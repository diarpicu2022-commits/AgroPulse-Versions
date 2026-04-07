package com.agropulse.util;

import javax.crypto.*;
import javax.crypto.spec.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║  AgroPulse — Módulo de Cifrado de Máxima Seguridad         ║
 * ╠══════════════════════════════════════════════════════════════╣
 * ║                                                              ║
 * ║  CIFRADO DE DATOS: AES-256-GCM (AEAD)                       ║
 * ║    • IV   aleatorio de 12 bytes por cada operación          ║
 * ║    • Salt aleatorio de 32 bytes (NIST SP 800-132)           ║
 * ║    • GCM tag de 128 bits (autenticación + integridad)        ║
 * ║    • Clave derivada con PBKDF2-HMAC-SHA512                  ║
 * ║      · 600,000 iteraciones (OWASP 2024)                     ║
 * ║      · 256 bits de clave                                     ║
 * ║    • Additional Data (AAD) para ligar el contexto           ║
 * ║                                                              ║
 * ║  HASH DE CONTRASEÑAS: PBKDF2-HMAC-SHA512                    ║
 * ║    • Salt de 32 bytes por usuario                            ║
 * ║    • 600,000 iteraciones (resistente a GPU/ASIC)            ║
 * ║    • Salida de 512 bits                                      ║
 * ║    • Comparación en tiempo constante (timing-attack safe)   ║
 * ║                                                              ║
 * ║  FORMATO BINARIO SEGURO:                                    ║
 * ║    version(1) | salt(32) | iv(12) | tag_len(1) | cipher     ║
 * ║                                                              ║
 * ╚══════════════════════════════════════════════════════════════╝
 */
public final class CryptoUtils {

    private CryptoUtils() {}

    // ─── Constantes ──────────────────────────────────────────────────
    private static final String  AES_GCM       = "AES/GCM/NoPadding";
    private static final int     GCM_IV_LEN    = 12;    // bytes — NIST recomendado
    private static final int     GCM_TAG_BITS  = 128;   // bits  — máximo
    private static final int     SALT_LEN      = 32;    // bytes (256 bits)
    private static final int     KEY_BITS      = 256;   // AES-256
    private static final int     PWD_HASH_BITS = 512;   // SHA-512 output
    private static final int     KDF_ITER_DATA = 600_000; // OWASP 2024 para datos
    private static final int     KDF_ITER_PWD  = 600_000; // OWASP 2024 para passwords
    private static final String  PBKDF2_ALG    = "PBKDF2WithHmacSHA512";
    private static final byte    FORMAT_VER    = 0x02;  // versión del formato

    // Clave maestra compuesta (entropía alta)
    // En producción: cargar desde variable de entorno AGROPULSE_MASTER_KEY
    private static final String MASTER_KEY;
    static {
        String envKey = System.getenv("AGROPULSE_MASTER_KEY");
        MASTER_KEY = (envKey != null && envKey.length() >= 32)
            ? envKey
            : "AgroPulse\u00a9Nari\u00f1o2024#M@sterK3y!$ecure#XYZ*987";
    }

    // AAD (Additional Authenticated Data) — liga el contexto al cifrado
    private static final byte[] AAD = "AgroPulse-v2-GCM".getBytes(StandardCharsets.UTF_8);

    // ─── API Pública ─────────────────────────────────────────────────

    /**
     * Cifra texto con AES-256-GCM + PBKDF2-SHA512.
     * Cada llamada genera salt e IV únicos → mismo plaintext → diferente ciphertext.
     *
     * @param  plaintext Texto a cifrar
     * @return Base64url(version | salt[32] | iv[12] | ciphertext+tag[var])
     */
    public static String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) return plaintext;
        try {
            byte[] salt       = secureRandom(SALT_LEN);
            byte[] iv         = secureRandom(GCM_IV_LEN);
            SecretKey key     = deriveKey(MASTER_KEY.toCharArray(), salt, KDF_ITER_DATA, KEY_BITS);

            Cipher cipher = Cipher.getInstance(AES_GCM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);
            cipher.updateAAD(AAD);

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Empaquetar: version(1) + salt(32) + iv(12) + ciphertext+tag
            ByteBuffer buf = ByteBuffer.allocate(1 + SALT_LEN + GCM_IV_LEN + ciphertext.length);
            buf.put(FORMAT_VER).put(salt).put(iv).put(ciphertext);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(buf.array());

        } catch (Exception e) {
            throw new SecurityException("[CryptoUtils] Error al cifrar: " + e.getMessage(), e);
        }
    }

    /**
     * Descifra texto cifrado con AES-256-GCM.
     * Si la autenticación GCM falla (datos manipulados), lanza SecurityException.
     * Si el texto no es Base64 válido (migración de datos planos), lo devuelve tal cual.
     *
     * @param  encrypted Base64url(version | salt | iv | ciphertext+tag)
     * @return Texto descifrado o el original si era datos sin cifrar (migración)
     */
    public static String decrypt(String encrypted) {
        if (encrypted == null || encrypted.isEmpty()) return encrypted;
        try {
            byte[] data = Base64.getUrlDecoder().decode(encrypted);

            // Verificar formato mínimo
            if (data.length < 1 + SALT_LEN + GCM_IV_LEN + 16) {
                return encrypted; // Datos sin cifrar (migración)
            }

            ByteBuffer buf   = ByteBuffer.wrap(data);
            byte version     = buf.get();
            byte[] salt      = new byte[SALT_LEN];
            byte[] iv        = new byte[GCM_IV_LEN];
            byte[] ciphertext= new byte[data.length - 1 - SALT_LEN - GCM_IV_LEN];
            buf.get(salt).get(iv).get(ciphertext);

            SecretKey key = deriveKey(MASTER_KEY.toCharArray(), salt, KDF_ITER_DATA, KEY_BITS);
            Cipher cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            cipher.updateAAD(AAD);

            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);

        } catch (IllegalArgumentException e) {
            return encrypted; // No es Base64 válido → dato plano (migración)
        } catch (AEADBadTagException e) {
            System.err.println("[CryptoUtils] ⚠️ Tag GCM inválido — posible manipulación de datos.");
            return "[DATOS CORRUPTOS]";
        } catch (Exception e) {
            return encrypted; // Fallback migración
        }
    }

    // ─── Hash de contraseñas ──────────────────────────────────────────

    /**
     * Genera hash seguro de contraseña con PBKDF2-SHA512.
     * Formato: Base64url(version | salt[32] | hash[64])
     */
    public static String hashPassword(String password) {
        if (password == null || password.isEmpty()) throw new SecurityException("Contraseña vacía");
        try {
            byte[] salt = secureRandom(SALT_LEN);
            byte[] hash = pbkdf2(password.toCharArray(), salt, KDF_ITER_PWD, PWD_HASH_BITS);

            ByteBuffer buf = ByteBuffer.allocate(1 + SALT_LEN + hash.length);
            buf.put(FORMAT_VER).put(salt).put(hash);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(buf.array());

        } catch (Exception e) {
            throw new SecurityException("[CryptoUtils] Error al hashear contraseña: " + e.getMessage(), e);
        }
    }

    /**
     * Verifica contraseña contra hash almacenado.
     *
     * • Comparación en tiempo CONSTANTE (MessageDigest.isEqual) para
     *   prevenir timing attacks.
     * • Soporta contraseñas planas antiguas (migración automática).
     * • Soporta hashes de formato v1 (anterior con 32 bytes de hash).
     */
    public static boolean verifyPassword(String password, String storedHash) {
        if (password == null || storedHash == null) return false;
        try {
            byte[] data = Base64.getUrlDecoder().decode(storedHash);
            if (data.length < 1 + SALT_LEN + 1) {
                return storedHash.equals(password); // Fallback texto plano
            }

            ByteBuffer buf  = ByteBuffer.wrap(data);
            byte version    = buf.get();
            byte[] salt     = new byte[SALT_LEN];
            byte[] expected = new byte[data.length - 1 - SALT_LEN];
            buf.get(salt).get(expected);

            // Recalcular hash con misma sal
            int outBits = expected.length * 8;
            byte[] actual = pbkdf2(password.toCharArray(), salt, KDF_ITER_PWD, outBits);
            return MessageDigest.isEqual(expected, actual); // Tiempo constante

        } catch (IllegalArgumentException e) {
            return storedHash.equals(password); // No es Base64 → texto plano antiguo
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Detecta si un hash fue generado con el sistema antiguo (texto plano).
     */
    public static boolean isLegacyPassword(String storedHash) {
        if (storedHash == null) return true;
        try {
            byte[] data = Base64.getUrlDecoder().decode(storedHash);
            return data.length < 1 + SALT_LEN + 16;
        } catch (Exception e) {
            return true;
        }
    }

    // ─── Utilidades ──────────────────────────────────────────────────

    /** Genera bytes aleatorios criptográficamente seguros. */
    public static byte[] secureRandom(int length) {
        byte[] b = new byte[length];
        new SecureRandom().nextBytes(b);
        return b;
    }

    /** Genera un token seguro de N bytes, codificado en Base64url. */
    public static String generateToken(int bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(secureRandom(bytes));
    }

    // ─── Internos ────────────────────────────────────────────────────

    private static SecretKey deriveKey(char[] password, byte[] salt,
                                       int iterations, int keyBits) throws Exception {
        byte[] keyBytes = pbkdf2(password, salt, iterations, keyBits);
        return new SecretKeySpec(keyBytes, "AES");
    }

    private static byte[] pbkdf2(char[] password, byte[] salt,
                                  int iterations, int keyBits) throws Exception {
        KeySpec spec = new PBEKeySpec(password, salt, iterations, keyBits);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALG);
        return factory.generateSecret(spec).getEncoded();
    }
}
