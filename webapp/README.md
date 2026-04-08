# AgroPulse Frontend v6.0

WebApp de monitoreo de invernadero inteligente. React + Vite + Tailwind CSS + Supabase.

## Instalación

```bash
npm install
```

## Configuración

```bash
cp .env.example .env
```

Edita `.env` con tus claves reales:

| Variable | Descripción |
|----------|-------------|
| `VITE_SUPABASE_URL` | URL de tu proyecto Supabase |
| `VITE_SUPABASE_ANON_KEY` | Clave pública (anon) de Supabase |
| `VITE_API_URL` | URL del backend Java (por defecto `http://localhost:8080`) |
| `VITE_OPENROUTER_KEY` | Clave de OpenRouter (gratis en openrouter.ai) |

> **IMPORTANTE:** Nunca subas `.env` a GitHub. Ya está en `.gitignore`.

## Desarrollo

```bash
npm run dev
```

Abre http://localhost:3000 en el navegador.

## Build para producción

```bash
npm run build
```

Los archivos se generan en `dist/`.

## Funcionalidades

- **Dashboard** — Lecturas en tiempo real de sensores (temperatura, humedad, humedad del suelo)
- **Consultar IA** — Consultas a inteligencia artificial (OpenRouter/Gemini 2.0 Flash gratis) con contexto de sensores
- **Alertas** — Visualización de alertas críticas, warning e informativas
- **Soporte** — Sistema de tickets de soporte técnico
- **Configuración** — Info del sistema, clave de IA, equipo de desarrollo
- **Google Login** — Autenticación con Google OAuth via Supabase Auth
- **PWA** — Instalable como app en dispositivos móviles

## Tecnologías

- React 18 + Vite 5
- Tailwind CSS (CDN)
- Supabase (auth + base de datos)
- Recharts (gráficas)
- Lucide React (iconos)
- OpenRouter API (IA gratuita)

## Autor

Leider Cadena — Universidad Cooperativa de Colombia, Nariño — Proyecto de Grado 2025
