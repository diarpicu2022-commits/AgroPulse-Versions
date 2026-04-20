import { useState, useEffect, createContext, useContext } from 'react'
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts'
import {
  Thermometer, Droplets, Leaf, Bell, Bot, Settings, LogOut,
  Home, Activity, MessageCircle, Send, Cpu, Zap, RefreshCw,
  CheckCircle, XCircle, Info, Key, Wifi, WifiOff,
  Menu, X, ToggleLeft, ToggleRight, Sprout, BarChart3, ChevronRight, Mail
} from 'lucide-react'
import { createClient } from '@supabase/supabase-js'
import api from './services/api-client'
import { DebugInfo } from './debug'

// ── Supabase Client ────────────────────────────────────────────────
const supabaseUrl = import.meta.env.VITE_SUPABASE_URL || ''
const supabaseKey = import.meta.env.VITE_SUPABASE_ANON_KEY || ''
const supabase = supabaseUrl && supabaseKey ? createClient(supabaseUrl, supabaseKey) : null

// ── API URL para REST (backend Java) ────────────────────────
const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

// ── LocalStorage Helpers ────────────────────────────────────
const safeGet = (key) => {
  try {
    return localStorage.getItem(key)
  } catch {
    return null
  }
}

const safeSet = (key, value) => {
  try {
    localStorage.setItem(key, value)
  } catch {
    console.error('No se pudo guardar en localStorage')
  }
}

const safeRemove = (key) => {
  try {
    localStorage.removeItem(key)
  } catch {
    console.error('No se pudo eliminar de localStorage')
  }
}

// ── IA Services (Groq, GitHub, Gemma) ────────────────────────────────────────────
const getGroqKey = () => {
  return safeGet('agropulse_groq_key')
    || import.meta.env.VITE_GROQ_KEY
    || ''
}

const getGitHubToken = () => {
  return safeGet('agropulse_github_token')
    || import.meta.env.VITE_GITHUB_TOKEN
    || ''
}

const getGemmaKey = () => {
  return safeGet('agropulse_gemma_key')
    || import.meta.env.VITE_GEMMA_KEY
    || ''
}

async function callAI(prompt, sensorContext = '') {
  const systemPrompt = `Eres un experto agrónomo e ingeniero de invernaderos. Tu nombre es AgroPulse IA.
Respondes en español, de forma concisa y práctica.
Siempre das recomendaciones basadas en datos reales de sensores cuando están disponibles.
${sensorContext ? `\nDatos actuales del invernadero:\n${sensorContext}` : ''}`

  // Intentar Groq primero
  const groqKey = getGroqKey()
  if (groqKey) {
    try {
      const res = await fetch('https://api.groq.com/openai/v1/chat/completions', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${groqKey}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          model: 'llama-3.1-8b-instant',
          messages: [
            { role: 'system', content: systemPrompt },
            { role: 'user', content: prompt }
          ],
          max_tokens: 600
        })
      })
      const data = await res.json()
      if (data.choices && data.choices[0]) {
        return data.choices[0].message.content
      }
    } catch (err) {
      console.error('Groq error:', err)
    }
  }

  // Intentar GitHub Models
  const githubToken = getGitHubToken()
  if (githubToken) {
    try {
      const res = await fetch('https://models.github.ai/inference/chat/completions', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${githubToken}`,
          'Content-Type': 'application/json',
          'Accept': 'application/vnd.github+json',
          'X-GitHub-Api-Version': '2022-11-28'
        },
        body: JSON.stringify({
          model: 'openai/gpt-4o-mini',
          messages: [
            { role: 'system', content: systemPrompt },
            { role: 'user', content: prompt }
          ],
          max_tokens: 600
        })
      })
      const data = await res.json()
      if (data.choices && data.choices[0]) {
        return data.choices[0].message.content
      }
    } catch (err) {
      console.error('GitHub error:', err)
    }
  }

  // Intentar Gemma 4 (Google AI Studio)
  const gemmaKey = getGemmaKey()
  if (gemmaKey) {
    try {
      const res = await fetch('https://generativelanguage.googleapis.com/v1beta/openai/chat/completions?key=' + gemmaKey, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          model: 'gemma-4-31b-it',
          messages: [
            { role: 'user', content: systemPrompt + '\n\nPregunta: ' + prompt }
          ],
          max_tokens: 600,
          temperature: 0.7
        })
      })
      const data = await res.json()
      if (data.choices && data.choices[0]) {
        return data.choices[0].message.content
      }
      if (data.error) {
        console.error('Gemma error:', data.error.message)
      }
    } catch (err) {
      console.error('Gemma error:', err)
    }
  }

  return '⚠️ No hay servicio de IA disponible. Configura Groq, GitHub o Gemma en Configuración.'
}

// ── Contexto de autenticación ────────────────────────────────────
const AuthContext = createContext(null)
const useAuth = () => useContext(AuthContext)

// ══════════════════════════════════════════════════════════════════
//  COMPONENTES
// ══════════════════════════════════════════════════════════════════

// ── Tarjeta de sensor ────────────────────────────────────────────
function SensorCard({ icon: Icon, label, value, unit, color, min, max }) {
  const pct = min != null && max != null
    ? Math.max(0, Math.min(100, ((value - min) / (max - min)) * 100))
    : null

  const isOk = pct != null && pct >= 0 && pct <= 100

  // Gradientes basados en tipo de sensor
  const gradients = {
    'bg-orange-500': 'from-orange-50 to-amber-50 border-orange-100',
    'bg-blue-500': 'from-blue-50 to-cyan-50 border-blue-100',
    'bg-cyan-500': 'from-cyan-50 to-sky-50 border-cyan-100',
    'bg-green-600': 'from-green-50 to-emerald-50 border-green-100',
  }

  const gradientClass = gradients[color] || 'from-gray-50 to-gray-50 border-gray-100'

  return (
    <div className={`bg-gradient-to-br ${gradientClass} rounded-2xl shadow-sm border p-4 flex flex-col gap-3 hover:shadow-md transition-all duration-300 hover:scale-[1.02]`}>
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className={`p-3 rounded-xl ${color} shadow-md`}>
            <Icon size={22} className="text-white" />
          </div>
          <span className="text-sm font-semibold text-gray-700">{label}</span>
        </div>
        <span className={`text-xs px-3 py-1.5 rounded-full font-medium transition-all ${
          isOk ? 'bg-green-100 text-green-700 shadow-sm' : 'bg-red-100 text-red-700 shadow-sm'
        }`}>
          {isOk ? '✓ Óptimo' : '⚠ Fuera'}
        </span>
      </div>
      <div className="flex items-end gap-2">
        <span className="text-4xl font-bold text-gray-800 font-mono">
          {value != null ? value.toFixed(1) : '—'}
        </span>
        <span className="text-sm text-gray-500 mb-1">{unit}</span>
      </div>
      {pct != null && (
        <div className="space-y-2 pt-1">
          <div className="h-2.5 bg-gray-200 rounded-full overflow-hidden shadow-inner">
            <div
              className={`h-full rounded-full transition-all duration-500 ${
                isOk ? 'bg-gradient-to-r from-green-400 to-emerald-500' : pct < 0 ? 'bg-gradient-to-r from-blue-400 to-cyan-500' : 'bg-gradient-to-r from-red-400 to-rose-500'
              }`}
              style={{ width: `${Math.max(0, Math.min(100, pct))}%` }}
            />
          </div>
          <div className="flex justify-between text-xs text-gray-500 font-medium">
            <span>{min}{unit}</span>
            <span className="text-center text-gray-600">{Math.round(pct)}%</span>
            <span>{max}{unit}</span>
          </div>
        </div>
      )}
    </div>
  )
}

// ── Componente AlertsBanner - Alertas en Tiempo Real ──────────────────
function AlertsBanner({ alerts, onDismiss }) {
  if (!alerts || alerts.length === 0) return null

  const getAlertColor = (type) => {
    const colors = {
      'TEMPERATURE': 'bg-orange-100 border-orange-300 text-orange-800',
      'HUMIDITY': 'bg-blue-100 border-blue-300 text-blue-800',
      'SOIL_MOISTURE': 'bg-green-100 border-green-300 text-green-800',
      'CRITICAL': 'bg-red-100 border-red-300 text-red-800'
    }
    return colors[type] || 'bg-yellow-100 border-yellow-300 text-yellow-800'
  }

  const getAlertIcon = (type) => {
    const icons = {
      'TEMPERATURE': '🌡️',
      'HUMIDITY': '💧',
      'SOIL_MOISTURE': '🌱',
      'CRITICAL': '🚨'
    }
    return icons[type] || '⚠️'
  }

  return (
    <div className="space-y-2 mb-4">
      {alerts.map((alert, idx) => (
        <div
          key={idx}
          className={`border-l-4 rounded-lg p-4 flex items-center justify-between animate-bounce ${
            getAlertColor(alert.type)
          }`}
          style={{
            animationDelay: `${idx * 100}ms`,
            animationDuration: '2s'
          }}
        >
          <div className="flex items-center gap-3 flex-1">
            <span className="text-2xl">{getAlertIcon(alert.type)}</span>
            <div>
              <p className="font-semibold text-sm">{alert.title}</p>
              <p className="text-xs opacity-90">{alert.message}</p>
              {alert.timestamp && (
                <p className="text-xs opacity-70 mt-1">
                  {new Date(alert.timestamp).toLocaleTimeString('es-CO')}
                </p>
              )}
            </div>
          </div>
          <button
            onClick={() => onDismiss(idx)}
            className="ml-4 text-lg hover:opacity-70 transition-opacity"
          >
            ✕
          </button>
        </div>
      ))}
    </div>
  )
}

// ── Página de Login ──────────────────────────────────────────────
function LoginPage({ onLogin }) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [tab, setTab] = useState('local') // 'local' or 'google'

  const handleLocalLogin = async (e) => {
    e.preventDefault()
    if (!username || !password) {
      setError('Por favor completa usuario y contraseña')
      return
    }
    setLoading(true)
    setError('')
    try {
      const response = await api.auth.login(username, password)
      const role = response.role === 'ADMIN' ? 'admin' : 'user'
      onLogin({ 
        ...response, 
        email: response.username, 
        role: role,
        provider: response.provider || 'LOCAL'
      })
    } catch (err) {
      setError('Credenciales incorrectas. Intenta de nuevo.')
      console.error('Login error:', err)
    } finally {
      setLoading(false)
    }
  }

  const handleGoogleLogin = async () => {
    if (!supabase) {
      setError('Google login no configurado. Usa credenciales locales.')
      return
    }
    
    setLoading(true)
    setError('')
    
    try {
      const { data, error } = await supabase.auth.signInWithOAuth({
        provider: 'google',
        options: { redirectTo: window.location.origin + '/' }
      })
      
      if (error) {
        setError('Error en Google: ' + error.message)
      }
    } catch (err) {
      setError('Error de conexión: ' + err.message)
    } finally {
      setLoading(false)
    }
  }

  // Interceptar sesión de Supabase y enviar al backend
  useEffect(() => {
    if (!supabase) return
    
    const { data: { subscription } } = supabase.auth.onAuthStateChange(async (event, session) => {
      if (event === 'SIGNED_IN' && session?.user) {
        setLoading(true)
        setError('')
        try {
          const user = session.user
          // Usar el nuevo método googleLogin
          const response = await api.auth.googleLogin(
            user.email,
            user.user_metadata?.full_name || user.email,
            user.id
          )
          
          const role = response.role === 'ADMIN' ? 'admin' : 'user'
          
          onLogin({
            ...response,
            email: user.email,
            role: role,
            provider: 'GOOGLE'
          })
        } catch (err) {
          setError('Error procesando login de Google')
          console.error('Google login backend error:', err)
          setLoading(false)
        }
      }
    })
    
    return () => subscription?.unsubscribe()
  }, [supabase, onLogin])

  return (
    <div className="min-h-screen bg-gradient-to-br from-green-900 via-green-800 to-emerald-700 flex items-center justify-center p-4 relative overflow-hidden">
      {/* Fondo animado con gradientes */}
      <div className="absolute inset-0 opacity-20">
        <div className="absolute top-20 right-20 w-72 h-72 bg-green-400 rounded-full blur-3xl animate-pulse" />
        <div className="absolute bottom-20 left-20 w-72 h-72 bg-emerald-400 rounded-full blur-3xl animate-pulse" style={{ animationDelay: '1s' }} />
      </div>

      {/* Login Card */}
      <div className="bg-white/95 backdrop-blur-md rounded-3xl shadow-2xl p-8 w-full max-w-md relative z-10 border border-white/20">
        {/* Header */}
        <div className="text-center mb-8">
          <div className="text-6xl mb-4 animate-bounce" style={{ animationDuration: '2s' }}>🌿</div>
          <h1 className="text-3xl font-bold bg-gradient-to-r from-green-600 to-emerald-600 bg-clip-text text-transparent mb-2">
            AgroPulse
          </h1>
          <p className="text-gray-600 text-sm">Sistema Inteligente de Monitoreo de Invernaderos</p>
        </div>

        {/* Tabs */}
        <div className="flex gap-3 mb-6 bg-gray-100 p-1 rounded-xl">
          <button
            onClick={() => { setTab('local'); setError('') }}
            className={`flex-1 py-2 px-3 rounded-lg font-medium text-sm transition-all ${
              tab === 'local'
                ? 'bg-white text-green-600 shadow-md'
                : 'text-gray-600 hover:text-gray-900'
            }`}
          >
            Usuario
          </button>
          <button
            onClick={() => { setTab('google'); setError('') }}
            className={`flex-1 py-2 px-3 rounded-lg font-medium text-sm transition-all ${
              tab === 'google'
                ? 'bg-white text-green-600 shadow-md'
                : 'text-gray-600 hover:text-gray-900'
            }`}
          >
            Google
          </button>
        </div>

        {/* Tab Content */}
        {tab === 'local' ? (
          // ── Local Login ──
          <form onSubmit={handleLocalLogin} className="space-y-4">
            <div>
              <label className="block text-sm font-semibold text-gray-700 mb-2">Usuario</label>
              <input
                type="text"
                value={username}
                onChange={e => setUsername(e.target.value)}
                placeholder="admin"
                disabled={loading}
                className="w-full border-2 border-gray-200 rounded-xl px-4 py-3 text-sm focus:border-green-500 focus:outline-none focus:ring-2 focus:ring-green-200 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
              />
            </div>
            <div>
              <label className="block text-sm font-semibold text-gray-700 mb-2">Contraseña</label>
              <input
                type="password"
                value={password}
                onChange={e => setPassword(e.target.value)}
                placeholder="••••••••"
                disabled={loading}
                className="w-full border-2 border-gray-200 rounded-xl px-4 py-3 text-sm focus:border-green-500 focus:outline-none focus:ring-2 focus:ring-green-200 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
              />
            </div>

            {error && (
              <div className="bg-red-50 border-2 border-red-200 text-red-700 px-4 py-3 rounded-xl text-sm animate-pulse">
                ⚠️ {error}
              </div>
            )}

            <button
              type="submit"
              disabled={loading}
              className="w-full bg-gradient-to-r from-green-500 to-emerald-500 hover:from-green-600 hover:to-emerald-600 disabled:from-gray-400 disabled:to-gray-400 text-white font-bold py-3 rounded-xl transition-all duration-200 transform hover:scale-105 disabled:scale-100 shadow-lg hover:shadow-xl flex items-center justify-center gap-2"
            >
              {loading ? (
                <>
                  <div className="w-5 h-5 border-3 border-white border-t-transparent rounded-full animate-spin" />
                  Verificando...
                </>
              ) : (
                <>
                  🔓 Ingresar
                </>
              )}
            </button>
          </form>
        ) : (
          // ── Google Login ──
          <div className="space-y-4">
            <p className="text-sm text-gray-600 text-center mb-4">
              Accede con tu cuenta de Google para continuar
            </p>

            {error && (
              <div className="bg-red-50 border-2 border-red-200 text-red-700 px-4 py-3 rounded-xl text-sm animate-pulse">
                ⚠️ {error}
              </div>
            )}

            <button
              onClick={handleGoogleLogin}
              disabled={loading}
              className="w-full flex items-center justify-center gap-3 border-2 border-gray-300 hover:border-green-400 bg-white hover:bg-green-50 text-gray-700 font-semibold py-3 rounded-xl transition-all duration-200 transform hover:scale-105 disabled:scale-100 disabled:opacity-50 shadow-md hover:shadow-lg"
            >
              {loading ? (
                <>
                  <div className="w-5 h-5 border-3 border-gray-400 border-t-transparent rounded-full animate-spin" />
                  Conectando...
                </>
              ) : (
                <>
                  <svg width="20" height="20" viewBox="0 0 48 48">
                    <path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"/>
                    <path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z"/>
                    <path fill="#FBBC05" d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z"/>
                    <path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"/>
                  </svg>
                  Continuar con Google
                </>
              )}
            </button>

            <p className="text-xs text-gray-500 text-center pt-2">
              Se abrirá una ventana para autenticarte con Google
            </p>
          </div>
        )}

        {/* Footer */}
        <p className="text-center text-xs text-gray-400 mt-8 pt-6 border-t border-gray-200">
          © Universidad Cooperativa de Colombia · Nariño
        </p>
      </div>
    </div>
  )
}

// ── Dashboard Principal ──────────────────────────────────────────
function Dashboard() {
  const { user }       = useAuth()
  const [readings, setReadings]   = useState([])
  const [history,  setHistory]    = useState([])
  const [alerts,   setAlerts]     = useState([])
  const [autoAlerts, setAutoAlerts] = useState([]) // Alertas generadas automáticamente
  const [crop,     setCrop]       = useState(null)
  const [loading,  setLoading]    = useState(true)
  const [lastUpdate, setLastUpdate] = useState(null)
  const [lastAlertCheck, setLastAlertCheck] = useState(null)

  useEffect(() => {
    loadData()
    const interval = setInterval(loadData, 5000)
    return () => clearInterval(interval)
  }, [])

  const generateAutoAlerts = (readingsData, cropData) => {
    if (!cropData || readingsData.length === 0) return []
    
    const newAlerts = []
    const now = new Date()
    
    // Verificar temperatura interior
    const tempInt = readingsData.find(r => r.sensorType === 'TEMPERATURE_INTERNAL')
    if (tempInt) {
      if (tempInt.value < cropData.temp_min) {
        newAlerts.push({
          type: 'TEMPERATURE',
          title: '❄️ Temperatura muy baja',
          message: `${tempInt.value.toFixed(1)}°C (mín: ${cropData.temp_min}°C)`,
          timestamp: now
        })
      } else if (tempInt.value > cropData.temp_max) {
        newAlerts.push({
          type: 'TEMPERATURE',
          title: '🔥 Temperatura muy alta',
          message: `${tempInt.value.toFixed(1)}°C (máx: ${cropData.temp_max}°C)`,
          timestamp: now
        })
      }
    }
    
    // Verificar humedad
    const humidity = readingsData.find(r => r.sensorType === 'HUMIDITY')
    if (humidity) {
      if (humidity.value < cropData.humidity_min) {
        newAlerts.push({
          type: 'HUMIDITY',
          title: '🏜️ Humedad muy baja',
          message: `${humidity.value.toFixed(1)}% (mín: ${cropData.humidity_min}%)`,
          timestamp: now
        })
      } else if (humidity.value > cropData.humidity_max) {
        newAlerts.push({
          type: 'HUMIDITY',
          title: '💦 Humedad muy alta',
          message: `${humidity.value.toFixed(1)}% (máx: ${cropData.humidity_max}%)`,
          timestamp: now
        })
      }
    }
    
    // Verificar humedad del suelo
    const soil = readingsData.find(r => r.sensorType === 'SOIL_MOISTURE')
    if (soil) {
      if (soil.value < cropData.soil_moisture_min) {
        newAlerts.push({
          type: 'SOIL_MOISTURE',
          title: '🏜️ Suelo muy seco',
          message: `${soil.value.toFixed(1)}% (mín: ${cropData.soil_moisture_min}%)`,
          timestamp: now
        })
      } else if (soil.value > cropData.soil_moisture_max) {
        newAlerts.push({
          type: 'SOIL_MOISTURE',
          title: '💧 Suelo muy húmedo',
          message: `${soil.value.toFixed(1)}% (máx: ${cropData.soil_moisture_max}%)`,
          timestamp: now
        })
      }
    }
    
    return newAlerts
  }

  const saveAutoAlert = async (alert) => {
    try {
      // Guardar en backend si el endpoint existe
      if (api.alerts && api.alerts.create) {
        await api.alerts.create({
          type: alert.type,
          level: 'WARNING',
          message: alert.message,
          title: alert.title
        })
      }
    } catch (err) {
      console.error('Error saving alert:', err)
    }
  }

  const loadData = async () => {
    try {
      // Cargar lecturas desde API REST (backend Java)
      const readingsData = await api.readings.list(null, 200)
      if (readingsData && readingsData.readings) {
        setReadings(readingsData.readings)
        
        // Generar historial de temperatura (últimas 20 lecturas)
        const tempData = readingsData.readings
          .filter(r => r.sensorType === 'TEMPERATURE_INTERNAL')
          .slice(0, 20)
          .reverse()
          .map(r => ({
            time: new Date(r.timestamp).toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit' }),
            temp: parseFloat(r.value.toFixed(1))
          }))
        setHistory(tempData)
        
        // Cargar cultivo activo
        const cropsData = await api.crops.list()
        if (cropsData && cropsData.crops && cropsData.crops.length > 0) {
          const activeCrop = cropsData.crops.find(c => c.active === 1 || c.active === true) || cropsData.crops[0]
          setCrop(activeCrop)
          
          // Generar alertas automáticas
          const newAutoAlerts = generateAutoAlerts(readingsData.readings, activeCrop)
          if (newAutoAlerts.length > 0) {
            setAutoAlerts(newAutoAlerts)
            // Guardar alertas
            newAutoAlerts.forEach(alert => saveAutoAlert(alert))
          } else {
            setAutoAlerts([])
          }
        }
      }

      // Cargar alertas del backend
      const alertsData = await api.alerts.list()
      if (alertsData && alertsData.alerts) {
        setAlerts(alertsData.alerts.slice(0, 5))
      }

      setLastUpdate(new Date().toLocaleTimeString('es-CO'))
    } catch (err) {
      console.error('Error cargando datos:', err)
    } finally {
      setLoading(false)
    }
  }

  const latest = (type) => {
    const r = readings.find(r => r.sensorType === type)
    return r ? r.value : null
  }

  const dismissAutoAlert = (idx) => {
    setAutoAlerts(autoAlerts.filter((_, i) => i !== idx))
  }

  return (
    <div className="space-y-6">
      {/* Mostrar alertas automáticas */}
      <AlertsBanner alerts={autoAlerts} onDismiss={dismissAutoAlert} />
      
      <div className="flex items-center justify-between mb-8">
        <div>
          <h2 className="text-3xl font-bold text-gray-800 mb-1">
            ¡Bienvenido! 👋
          </h2>
          <p className="text-sm text-gray-600">
            {crop ? `🌿 Cultivo activo: **${crop.name}**` : 'Sin cultivo activo'}
            {lastUpdate && ` · Actualizado hace momentos`}
          </p>
        </div>
        <button onClick={loadData}
          className="bg-gradient-to-r from-green-500 to-emerald-500 hover:from-green-600 hover:to-emerald-600 text-white px-5 py-2.5 rounded-xl text-sm font-medium shadow-md hover:shadow-lg transition-all duration-200 transform hover:scale-105 flex items-center gap-2">
          <RefreshCw size={16} className="animate-spin-slow" /> Actualizar
        </button>
      </div>

      {loading ? (
        <div className="flex items-center justify-center h-40">
          <div className="animate-spin text-4xl">🌿</div>
        </div>
      ) : (
        <>
          <div className="grid grid-cols-2 gap-3">
            <SensorCard
              icon={Thermometer} label="Temp. Interior" unit="°C"
              value={latest('TEMPERATURE_INTERNAL')}
              color="bg-orange-500"
              min={crop?.temp_min} max={crop?.temp_max}
            />
            <SensorCard
              icon={Thermometer} label="Temp. Exterior" unit="°C"
              value={latest('TEMPERATURE_EXTERNAL')}
              color="bg-blue-500"
              min={crop?.temp_min} max={crop?.temp_max}
            />
            <SensorCard
              icon={Droplets} label="Humedad" unit="%"
              value={latest('HUMIDITY')}
              color="bg-cyan-500"
              min={crop?.humidity_min} max={crop?.humidity_max}
            />
            <SensorCard
              icon={Leaf} label="Humedad Suelo" unit="%"
              value={latest('SOIL_MOISTURE')}
              color="bg-green-600"
              min={crop?.soil_moisture_min} max={crop?.soil_moisture_max}
            />
          </div>

          {history.length > 0 && (
            <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-4">
              <h3 className="text-sm font-semibold text-gray-700 mb-3">
                📈 Temperatura Interior (últimas lecturas)
              </h3>
              <ResponsiveContainer width="100%" height={160}>
                <LineChart data={history}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                  <XAxis dataKey="time" tick={{ fontSize: 10 }} />
                  <YAxis tick={{ fontSize: 10 }} domain={['auto', 'auto']} />
                  <Tooltip />
                  <Line type="monotone" dataKey="temp" stroke="#16a34a"
                    strokeWidth={2} dot={false} />
                </LineChart>
              </ResponsiveContainer>
            </div>
          )}

          {alerts.length > 0 && (
            <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-4">
              <h3 className="text-sm font-semibold text-gray-700 mb-3">
                🔔 Alertas Recientes
              </h3>
              <div className="space-y-2">
                {alerts.map(a => (
                  <div key={a.id}
                    className={`flex items-start gap-2 p-2 rounded-xl text-sm ${
                      a.level === 'CRITICAL' ? 'bg-red-50 text-red-700' :
                      a.level === 'WARNING'  ? 'bg-yellow-50 text-yellow-700' :
                      'bg-blue-50 text-blue-700'
                    }`}>
                    <Bell size={14} className="mt-0.5 shrink-0" />
                    <span>{a.message}</span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </>
      )}
    </div>
  )
}

// ── Página de Sensores (detalle) ─────────────────────────────────
function SensorsPage() {
  const [sensors, setSensors] = useState([])
  const [loading, setLoading] = useState(true)
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState({ name: '', type: 'TEMPERATURE_INTERNAL', location: '' })

  useEffect(() => { loadSensors() }, [])

  const loadSensors = async () => {
    try {
      const data = await api.sensors.list()
      setSensors(data.sensors || [])
    } catch (err) { console.error(err) }
    setLoading(false)
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    try {
      await api.sensors.create(form)
      setShowForm(false)
      setForm({ name: '', type: 'TEMPERATURE_INTERNAL', location: '' })
      loadSensors()
    } catch (err) { alert('Error: ' + err.message) }
  }

  const handleDelete = async (id) => {
    if (confirm('¿Eliminar?')) {
      try { await api.sensors.delete(id); loadSensors() }
      catch (err) { alert('Error: ' + err.message) }
    }
  }

  const typeLabel = {
    TEMPERATURE_INTERNAL: { label: '🌡️ Temp. Interior', unit: '°C' },
    TEMPERATURE_EXTERNAL: { label: '🌡️ Temp. Exterior', unit: '°C' },
    HUMIDITY:             { label: '💧 Humedad',         unit: '%' },
    SOIL_MOISTURE:        { label: '🌱 Humedad Suelo',   unit: '%' },
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-bold text-gray-800">📡 Sensores</h2>
        <button onClick={() => setShowForm(!showForm)} className="bg-primary text-white px-4 py-2 rounded-xl text-sm font-medium">
          {showForm ? 'Cancelar' : '+ Nuevo'}
        </button>
      </div>

      {showForm && (
        <form onSubmit={handleSubmit} className="bg-white rounded-2xl shadow-sm border border-gray-100 p-4 space-y-3">
          <input type="text" placeholder="Nombre del sensor" value={form.name} onChange={e => setForm({...form, name: e.target.value})} className="w-full border rounded-xl px-4 py-2 text-sm" required />
          <select value={form.type} onChange={e => setForm({...form, type: e.target.value})} className="w-full border rounded-xl px-4 py-2 text-sm">
            <option value="TEMPERATURE_INTERNAL">Temp. Interior</option>
            <option value="TEMPERATURE_EXTERNAL">Temp. Exterior</option>
            <option value="HUMIDITY">Humedad</option>
            <option value="SOIL_MOISTURE">Humedad Suelo</option>
          </select>
          <input type="text" placeholder="Ubicación" value={form.location} onChange={e => setForm({...form, location: e.target.value})} className="w-full border rounded-xl px-4 py-2 text-sm" />
          <button type="submit" className="w-full bg-primary text-white py-2 rounded-xl font-medium">Guardar</button>
        </form>
      )}

      {loading ? (
        <div className="flex items-center justify-center h-40">
          <div className="animate-spin text-4xl">🌿</div>
        </div>
      ) : sensors.length === 0 ? (
        <div className="text-center py-16 text-gray-400">
          <Activity size={48} className="mx-auto mb-3 opacity-40" />
          <p>No hay sensores</p>
          <button onClick={() => setShowForm(true)} className="text-primary text-sm mt-2">Crear uno nuevo</button>
        </div>
      ) : (
        <div className="grid gap-3">
          {sensors.map(s => {
            const info = typeLabel[s.type] || { label: s.type, unit: '' }
            return (
              <div key={s.id} className="bg-white rounded-2xl shadow-sm border border-gray-100 p-4">
                <div className="flex items-start justify-between">
                  <div>
                    <h3 className="font-semibold text-gray-800">{s.name}</h3>
                    <p className="text-sm text-gray-500">{info.label}</p>
                    <p className="text-xs text-gray-400">{s.location}</p>
                  </div>
                  <button onClick={() => handleDelete(s.id)} className="text-red-500 text-sm">Eliminar</button>
                </div>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}

// ── Página de Actuadores ─────────────────────────────────────────
function ActuatorsPage() {
  const [actuators, setActuators] = useState([])
  const [loading, setLoading] = useState(true)
  const [showForm, setShowForm] = useState(false)
  const [editingId, setEditingId] = useState(null)
  const [form, setForm] = useState({ name: '', type: 'EXTRACTOR', greenhouseId: 1 })
  const [showScheduling, setShowScheduling] = useState(null) // ID del actuador en scheduling
  const [schedule, setSchedule] = useState({
    enabled: false,
    onTime: '08:00',
    offTime: '18:00',
    daysOfWeek: [1, 2, 3, 4, 5] // Mon-Fri
  })

  useEffect(() => { loadActuators() }, [])

  const loadActuators = async () => {
    try {
      const data = await api.actuators.list()
      setActuators(data.actuators || [])
    } catch (err) { console.error(err) }
    setLoading(false)
  }

  const resetForm = () => {
    setForm({ name: '', type: 'EXTRACTOR', greenhouseId: 1 })
    setEditingId(null)
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    try {
      if (editingId) {
        await api.actuators.update(editingId, form)
      } else {
        await api.actuators.create(form)
      }
      setShowForm(false)
      resetForm()
      loadActuators()
    } catch (err) { alert('Error: ' + err.message) }
  }

  const handleEdit = (actuator) => {
    setEditingId(actuator.id)
    setForm({
      name: actuator.name,
      type: actuator.type || 'EXTRACTOR',
      greenhouseId: actuator.greenhouse_id || 1
    })
    setShowForm(true)
  }

  const handleDelete = async (id, name) => {
    if (confirm(`¿Eliminar el actuador "${name}"?`)) {
      try { 
        await api.actuators.delete(id)
        loadActuators()
      } catch (err) { alert('Error: ' + err.message) }
    }
  }

  const toggleEnabled = async (id, currentEnabled) => {
    try {
      await api.actuators.update(id, { enabled: !currentEnabled })
      loadActuators()
    } catch (err) { alert('Error: ' + err.message) }
  }

  const toggleField = async (id, field, currentValue) => {
    try {
      await api.actuators.update(id, { [field]: !currentValue })
      loadActuators()
    } catch (err) {
      alert('Error: ' + err.message)
    }
  }

  const toggleScheduleDay = (day) => {
    const newDays = schedule.daysOfWeek.includes(day)
      ? schedule.daysOfWeek.filter(d => d !== day)
      : [...schedule.daysOfWeek, day]
    setSchedule({ ...schedule, daysOfWeek: newDays })
  }

  const handleSaveSchedule = async (actuatorId) => {
    try {
      // Guardar horario como JSON en un campo personalizado
      await api.actuators.update(actuatorId, {
        schedule_config: JSON.stringify(schedule)
      })
      setShowScheduling(null)
      loadActuators()
    } catch (err) { alert('Error: ' + err.message) }
  }

  const typeInfo = {
    EXTRACTOR:      { emoji: '🌀', label: 'Extractor de Aire' },
    DOOR:           { emoji: '🚪', label: 'Puerta' },
    HEAT_GENERATOR: { emoji: '🔥', label: 'Generador de Calor' },
    WATER_PUMP:     { emoji: '💧', label: 'Bomba de Agua' },
  }

  const dayNames = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom']

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-3xl font-bold text-gray-800">⚡ Control de Actuadores</h2>
          <p className="text-sm text-gray-600 mt-1">Gestiona y programa los actuadores del invernadero</p>
        </div>
        {!showForm && !showScheduling && (
          <button 
            onClick={() => { setShowForm(true); resetForm() }}
            className="bg-gradient-to-r from-blue-500 to-cyan-500 hover:from-blue-600 hover:to-cyan-600 text-white px-5 py-2.5 rounded-xl text-sm font-medium shadow-md hover:shadow-lg transition-all duration-200 transform hover:scale-105 flex items-center gap-2">
            ✨ Nuevo Actuador
          </button>
        )}
      </div>

      {/* Formulario de Creación/Edición */}
      {showForm && (
        <form onSubmit={handleSubmit} className="bg-white rounded-2xl shadow-md border-2 border-blue-200 p-6 space-y-4">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-bold text-gray-800">
              {editingId ? '✏️ Editar Actuador' : '➕ Nuevo Actuador'}
            </h3>
            <button
              type="button"
              onClick={() => { setShowForm(false); resetForm() }}
              className="text-gray-500 hover:text-gray-700 text-2xl">✕</button>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Nombre del actuador *</label>
              <input 
                type="text" 
                placeholder="Extractor principal, Bomba de riego, etc." 
                value={form.name} 
                onChange={e => setForm({...form, name: e.target.value})} 
                className="w-full border-2 border-gray-200 rounded-xl px-4 py-2.5 text-sm focus:border-blue-500 focus:outline-none transition-colors" 
                required 
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Tipo de actuador *</label>
              <select 
                value={form.type} 
                onChange={e => setForm({...form, type: e.target.value})} 
                className="w-full border-2 border-gray-200 rounded-xl px-4 py-2.5 text-sm focus:border-blue-500 focus:outline-none transition-colors">
                <option value="EXTRACTOR">🌀 Extractor de Aire</option>
                <option value="DOOR">🚪 Puerta</option>
                <option value="HEAT_GENERATOR">🔥 Generador de Calor</option>
                <option value="WATER_PUMP">💧 Bomba de Agua</option>
              </select>
            </div>
          </div>

          <div className="flex gap-3 pt-2">
            <button 
              type="submit" 
              className="flex-1 bg-gradient-to-r from-blue-500 to-cyan-500 hover:from-blue-600 hover:to-cyan-600 text-white py-2.5 rounded-xl font-semibold transition-all duration-200 transform hover:scale-105">
              {editingId ? '💾 Actualizar' : '✨ Crear'}
            </button>
            <button 
              type="button"
              onClick={() => { setShowForm(false); resetForm() }}
              className="flex-1 border-2 border-gray-300 text-gray-700 hover:bg-gray-50 py-2.5 rounded-xl font-semibold transition-colors">
              Cancelar
            </button>
          </div>
        </form>
      )}

      {/* Formulario de Scheduling */}
      {showScheduling && (
        <div className="bg-white rounded-2xl shadow-md border-2 border-yellow-200 p-6 space-y-4">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-bold text-gray-800">
              ⏰ Programar {actuators.find(a => a.id === showScheduling)?.name}
            </h3>
            <button
              onClick={() => setShowScheduling(null)}
              className="text-gray-500 hover:text-gray-700 text-2xl">✕</button>
          </div>

          <div className="space-y-4">
            {/* Toggle para activar scheduling */}
            <label className="flex items-center gap-3 cursor-pointer p-3 bg-yellow-50 rounded-xl border-2 border-yellow-100">
              <input 
                type="checkbox"
                checked={schedule.enabled}
                onChange={e => setSchedule({...schedule, enabled: e.target.checked})}
                className="w-5 h-5 text-yellow-600 rounded cursor-pointer"
              />
              <span className="text-sm font-semibold text-gray-800">✅ Activar Programación Horaria</span>
            </label>

            {schedule.enabled && (
              <>
                {/* Horas */}
                <div className="bg-blue-50 rounded-xl p-4 border-2 border-blue-100 space-y-4">
                  <p className="text-sm font-semibold text-blue-900">🕐 Horario</p>
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-xs font-medium text-gray-700 mb-2">Hora de Encendido</label>
                      <input 
                        type="time"
                        value={schedule.onTime}
                        onChange={e => setSchedule({...schedule, onTime: e.target.value})}
                        className="w-full border-2 border-blue-200 rounded-lg px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-medium text-gray-700 mb-2">Hora de Apagado</label>
                      <input 
                        type="time"
                        value={schedule.offTime}
                        onChange={e => setSchedule({...schedule, offTime: e.target.value})}
                        className="w-full border-2 border-blue-200 rounded-lg px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
                      />
                    </div>
                  </div>
                </div>

                {/* Días de la semana */}
                <div className="bg-green-50 rounded-xl p-4 border-2 border-green-100 space-y-3">
                  <p className="text-sm font-semibold text-green-900">📅 Días de la Semana</p>
                  <div className="grid grid-cols-7 gap-2">
                    {dayNames.map((day, idx) => {
                      const dayNum = idx + 1 // 1=Monday, 7=Sunday
                      const isSelected = schedule.daysOfWeek.includes(dayNum)
                      return (
                        <button
                          key={dayNum}
                          onClick={() => toggleScheduleDay(dayNum)}
                          className={`py-2 rounded-lg font-semibold text-xs transition-all ${
                            isSelected
                              ? 'bg-green-600 text-white shadow-md'
                              : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                          }`}>
                          {day}
                        </button>
                      )
                    })}
                  </div>
                </div>
              </>
            )}
          </div>

          <div className="flex gap-3 pt-2">
            <button 
              onClick={() => handleSaveSchedule(showScheduling)}
              className="flex-1 bg-gradient-to-r from-yellow-500 to-orange-500 hover:from-yellow-600 hover:to-orange-600 text-white py-2.5 rounded-xl font-semibold transition-all duration-200 transform hover:scale-105">
              💾 Guardar Programación
            </button>
            <button 
              onClick={() => setShowScheduling(null)}
              className="flex-1 border-2 border-gray-300 text-gray-700 hover:bg-gray-50 py-2.5 rounded-xl font-semibold transition-colors">
              Cancelar
            </button>
          </div>
        </div>
      )}

      {loading ? (
        <div className="flex items-center justify-center h-40">
          <div className="animate-spin text-4xl">⚡</div>
        </div>
      ) : actuators.length === 0 ? (
        <div className="text-center py-16 text-gray-400">
          <Zap size={48} className="mx-auto mb-3 opacity-40" />
          <p className="text-lg font-medium">No hay actuadores</p>
          <button 
            onClick={() => { setShowForm(true); resetForm() }}
            className="text-blue-600 text-sm mt-3 hover:text-blue-700 font-semibold">
            ➕ Crear el primer actuador
          </button>
        </div>
      ) : (
        <div className="space-y-3">
          {actuators.map(a => {
            const info = typeInfo[a.type] || { emoji: '⚙️', label: a.type }
            const isEnabled = a.enabled === 1 || a.enabled === true
            const isAuto = a.auto_mode === 1 || a.auto_mode === true
            
            return (
              <div key={a.id}
                className={`bg-white rounded-2xl shadow-sm border-2 p-5 transition-all duration-200 hover:shadow-md ${
                  isEnabled ? 'border-green-200 bg-green-50/20' : 'border-gray-200'
                }`}>
                <div className="flex items-start justify-between mb-4">
                  <div className="flex items-start gap-3 flex-1">
                    <span className="text-3xl">{info.emoji}</span>
                    <div>
                      <p className="text-base font-bold text-gray-800">{a.name || info.label}</p>
                      <p className="text-xs text-gray-500">{info.label}</p>
                      <p className={`text-xs font-semibold mt-1 ${isAuto ? 'text-blue-600' : 'text-orange-600'}`}>
                        {isAuto ? '🤖 Modo Automático' : '👤 Modo Manual'}
                      </p>
                    </div>
                  </div>
                  <span className={`text-xs px-3 py-1 rounded-full font-semibold whitespace-nowrap ${
                    isEnabled ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600'
                  }`}>
                    {isEnabled ? '✅ Encendido' : '⏸️ Apagado'}
                  </span>
                </div>

                {/* Botones de control */}
                <div className="grid grid-cols-2 gap-2 mb-3">
                  <button
                    onClick={() => toggleField(a.id, 'enabled', a.enabled)}
                    className={`flex items-center justify-center gap-2 py-2 rounded-lg text-sm font-semibold transition-all duration-200 transform hover:scale-105 ${
                      isEnabled
                        ? 'bg-gradient-to-r from-green-500 to-emerald-500 text-white'
                        : 'bg-gray-200 text-gray-600 hover:bg-gray-300'
                    }`}>
                    {isEnabled ? '✅ Apagar' : '▶️ Encender'}
                  </button>
                  <button
                    onClick={() => toggleField(a.id, 'auto_mode', a.auto_mode)}
                    className={`flex items-center justify-center gap-2 py-2 rounded-lg text-sm font-semibold transition-all duration-200 transform hover:scale-105 ${
                      isAuto
                        ? 'bg-gradient-to-r from-blue-500 to-cyan-500 text-white'
                        : 'bg-gray-200 text-gray-600 hover:bg-gray-300'
                    }`}>
                    {isAuto ? '🤖 Auto' : '👤 Manual'}
                  </button>
                </div>

                {/* Botones de acciones */}
                <div className="flex gap-2 pt-3 border-t border-gray-200">
                  <button
                    onClick={() => { 
                      setShowScheduling(a.id)
                      // Cargar horario existente si existe
                      if (a.schedule_config) {
                        try {
                          setSchedule(JSON.parse(a.schedule_config))
                        } catch (e) {
                          setSchedule({ enabled: false, onTime: '08:00', offTime: '18:00', daysOfWeek: [1,2,3,4,5] })
                        }
                      }
                    }}
                    className="flex-1 bg-gradient-to-r from-yellow-500 to-orange-500 hover:from-yellow-600 hover:to-orange-600 text-white py-2 rounded-lg text-sm font-semibold transition-all duration-200 transform hover:scale-105 flex items-center justify-center gap-2">
                    ⏰ Programar
                  </button>
                  <button
                    onClick={() => handleEdit(a)}
                    className="flex-1 border-2 border-blue-300 text-blue-600 hover:bg-blue-50 py-2 rounded-lg text-sm font-semibold transition-colors flex items-center justify-center gap-2">
                    ✏️ Editar
                  </button>
                  <button
                    onClick={() => handleDelete(a.id, a.name)}
                    className="flex-1 border-2 border-red-300 text-red-600 hover:bg-red-50 py-2 rounded-lg text-sm font-semibold transition-colors flex items-center justify-center gap-2">
                    🗑️ Eliminar
                  </button>
                </div>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}

// ── Página de Cultivos ───────────────────────────────────────────
function CropsPage() {
  const [crops, setCrops] = useState([])
  const [loading, setLoading] = useState(true)
  const [showForm, setShowForm] = useState(false)
  const [editingId, setEditingId] = useState(null)
  const [form, setForm] = useState({
    name: '', variety: '',
    temp_min: 15, temp_max: 25,
    humidity_min: 50, humidity_max: 70,
    soil_moisture_min: 40, soil_moisture_max: 60,
    active: 0
  })

  useEffect(() => { loadCrops() }, [])

  const loadCrops = async () => {
    try {
      const data = await api.crops.list()
      setCrops(data.crops || [])
    } catch (err) { console.error(err) }
    setLoading(false)
  }

  const resetForm = () => {
    setForm({
      name: '', variety: '',
      temp_min: 15, temp_max: 25,
      humidity_min: 50, humidity_max: 70,
      soil_moisture_min: 40, soil_moisture_max: 60,
      active: 0
    })
    setEditingId(null)
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    try {
      if (editingId) {
        // Modo edición
        await api.crops.update(editingId, form)
      } else {
        // Modo creación
        await api.crops.create(form)
      }
      setShowForm(false)
      resetForm()
      loadCrops()
    } catch (err) { alert('Error: ' + err.message) }
  }

  const handleEdit = (crop) => {
    setEditingId(crop.id)
    setForm({
      name: crop.name,
      variety: crop.variety || '',
      temp_min: crop.temp_min || 15,
      temp_max: crop.temp_max || 25,
      humidity_min: crop.humidity_min || 50,
      humidity_max: crop.humidity_max || 70,
      soil_moisture_min: crop.soil_moisture_min || 40,
      soil_moisture_max: crop.soil_moisture_max || 60,
      active: crop.active ? 1 : 0
    })
    setShowForm(true)
  }

  const handleDelete = async (id, name) => {
    if (confirm(`¿Estás seguro de eliminar el cultivo "${name}"?`)) {
      try {
        await api.crops.delete(id)
        loadCrops()
      } catch (err) { alert('Error: ' + err.message) }
    }
  }

  const RangeBar = ({ label, min, max, unit, color }) => (
    <div className="space-y-1">
      <div className="flex justify-between text-xs">
        <span className="text-gray-500">{label}</span>
        <span className="font-medium text-gray-700">{min} - {max} {unit}</span>
      </div>
      <div className="h-2 bg-gray-100 rounded-full overflow-hidden">
        <div className={`h-full rounded-full ${color}`}
          style={{ marginLeft: `${(min / (max * 1.5)) * 100}%`, width: `${((max - min) / (max * 1.5)) * 100}%` }}
        />
      </div>
    </div>
  )

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-3xl font-bold text-gray-800">🌿 Cultivos</h2>
          <p className="text-sm text-gray-600 mt-1">Crear, editar y gestionar cultivos</p>
        </div>
        {!showForm && (
          <button 
            onClick={() => { setShowForm(true); resetForm() }}
            className="bg-gradient-to-r from-green-500 to-emerald-500 hover:from-green-600 hover:to-emerald-600 text-white px-5 py-2.5 rounded-xl text-sm font-medium shadow-md hover:shadow-lg transition-all duration-200 transform hover:scale-105 flex items-center gap-2">
            ✨ Nuevo Cultivo
          </button>
        )}
      </div>

      {showForm && (
        <form onSubmit={handleSubmit} className="bg-white rounded-2xl shadow-md border-2 border-green-200 p-6 space-y-4">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-bold text-gray-800">
              {editingId ? '✏️ Editar Cultivo' : '➕ Nuevo Cultivo'}
            </h3>
            <button
              type="button"
              onClick={() => { setShowForm(false); resetForm() }}
              className="text-gray-500 hover:text-gray-700 text-2xl">✕</button>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Nombre del cultivo *</label>
              <input 
                type="text" 
                placeholder="Tomate, Lechuga, etc." 
                value={form.name} 
                onChange={e => setForm({...form, name: e.target.value})} 
                className="w-full border-2 border-gray-200 rounded-xl px-4 py-2.5 text-sm focus:border-green-500 focus:outline-none transition-colors" 
                required 
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Variedad</label>
              <input 
                type="text" 
                placeholder="Variedad (opcional)" 
                value={form.variety} 
                onChange={e => setForm({...form, variety: e.target.value})} 
                className="w-full border-2 border-gray-200 rounded-xl px-4 py-2.5 text-sm focus:border-green-500 focus:outline-none transition-colors" 
              />
            </div>
          </div>

          {/* Temperatura */}
          <div className="bg-orange-50 rounded-xl p-4 border-2 border-orange-100">
            <p className="text-sm font-semibold text-orange-900 mb-3">🌡️ Rango de Temperatura</p>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">Mínima (°C)</label>
                <input 
                  type="number" 
                  step="0.1"
                  value={form.temp_min} 
                  onChange={e => setForm({...form, temp_min: parseFloat(e.target.value)})} 
                  className="w-full border-2 border-orange-200 rounded-lg px-3 py-2 text-sm focus:border-orange-500 focus:outline-none"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">Máxima (°C)</label>
                <input 
                  type="number" 
                  step="0.1"
                  value={form.temp_max} 
                  onChange={e => setForm({...form, temp_max: parseFloat(e.target.value)})} 
                  className="w-full border-2 border-orange-200 rounded-lg px-3 py-2 text-sm focus:border-orange-500 focus:outline-none"
                />
              </div>
            </div>
          </div>

          {/* Humedad */}
          <div className="bg-cyan-50 rounded-xl p-4 border-2 border-cyan-100">
            <p className="text-sm font-semibold text-cyan-900 mb-3">💧 Rango de Humedad</p>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">Mínima (%)</label>
                <input 
                  type="number" 
                  min="0" max="100"
                  value={form.humidity_min} 
                  onChange={e => setForm({...form, humidity_min: parseInt(e.target.value)})} 
                  className="w-full border-2 border-cyan-200 rounded-lg px-3 py-2 text-sm focus:border-cyan-500 focus:outline-none"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">Máxima (%)</label>
                <input 
                  type="number" 
                  min="0" max="100"
                  value={form.humidity_max} 
                  onChange={e => setForm({...form, humidity_max: parseInt(e.target.value)})} 
                  className="w-full border-2 border-cyan-200 rounded-lg px-3 py-2 text-sm focus:border-cyan-500 focus:outline-none"
                />
              </div>
            </div>
          </div>

          {/* Humedad Suelo */}
          <div className="bg-green-50 rounded-xl p-4 border-2 border-green-100">
            <p className="text-sm font-semibold text-green-900 mb-3">🌱 Rango de Humedad del Suelo</p>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">Mínima (%)</label>
                <input 
                  type="number" 
                  min="0" max="100"
                  value={form.soil_moisture_min} 
                  onChange={e => setForm({...form, soil_moisture_min: parseInt(e.target.value)})} 
                  className="w-full border-2 border-green-200 rounded-lg px-3 py-2 text-sm focus:border-green-500 focus:outline-none"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">Máxima (%)</label>
                <input 
                  type="number" 
                  min="0" max="100"
                  value={form.soil_moisture_max} 
                  onChange={e => setForm({...form, soil_moisture_max: parseInt(e.target.value)})} 
                  className="w-full border-2 border-green-200 rounded-lg px-3 py-2 text-sm focus:border-green-500 focus:outline-none"
                />
              </div>
            </div>
          </div>

          {/* Estado activo */}
          <div>
            <label className="flex items-center gap-3 cursor-pointer">
              <input 
                type="checkbox"
                checked={form.active === 1 || form.active === true}
                onChange={e => setForm({...form, active: e.target.checked ? 1 : 0})}
                className="w-4 h-4 text-green-600 rounded cursor-pointer"
              />
              <span className="text-sm font-medium text-gray-700">✅ Cultivo Activo</span>
            </label>
          </div>

          {/* Botones */}
          <div className="flex gap-3 pt-2">
            <button 
              type="submit" 
              className="flex-1 bg-gradient-to-r from-green-500 to-emerald-500 hover:from-green-600 hover:to-emerald-600 text-white py-2.5 rounded-xl font-semibold transition-all duration-200 transform hover:scale-105 flex items-center justify-center gap-2"
            >
              {editingId ? '💾 Actualizar' : '✨ Crear'}
            </button>
            <button 
              type="button"
              onClick={() => { setShowForm(false); resetForm() }}
              className="flex-1 border-2 border-gray-300 text-gray-700 hover:bg-gray-50 py-2.5 rounded-xl font-semibold transition-colors"
            >
              Cancelar
            </button>
          </div>
        </form>
      )}

      {loading ? (
        <div className="flex items-center justify-center h-40">
          <div className="animate-spin text-4xl">🌿</div>
        </div>
      ) : crops.length === 0 ? (
        <div className="text-center py-16 text-gray-400">
          <Sprout size={48} className="mx-auto mb-3 opacity-40" />
          <p className="text-lg font-medium">No hay cultivos</p>
          <button 
            onClick={() => { setShowForm(true); resetForm() }}
            className="text-green-600 text-sm mt-3 hover:text-green-700 font-semibold">
            ➕ Crear el primer cultivo
          </button>
        </div>
      ) : (
        <div className="space-y-3">
          {crops.map(c => {
            const isActive = c.active === 1 || c.active === true
            return (
              <div key={c.id}
                className={`bg-white rounded-2xl shadow-sm border-2 p-5 transition-all duration-200 hover:shadow-md ${
                  isActive ? 'border-green-200 bg-green-50/30' : 'border-gray-200'
                }`}>
                <div className="flex items-start justify-between mb-4">
                  <div className="flex-1">
                    <div className="flex items-center gap-2 mb-2">
                      <span className="text-2xl">{isActive ? '🌿' : '🍂'}</span>
                      <div>
                        <p className="text-base font-bold text-gray-800">{c.name}</p>
                        {c.variety && <p className="text-xs text-gray-500">{c.variety}</p>}
                      </div>
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className={`text-xs px-3 py-1 rounded-full font-semibold ${
                      isActive ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600'
                    }`}>
                      {isActive ? '✅ Activo' : '⏸️ Inactivo'}
                    </span>
                  </div>
                </div>

                {/* Rangos */}
                <div className="space-y-3 mb-4">
                  {c.temp_min != null && c.temp_max != null && (
                    <RangeBar label="🌡️ Temperatura" min={c.temp_min} max={c.temp_max} unit="°C" color="bg-orange-400" />
                  )}
                  {c.humidity_min != null && c.humidity_max != null && (
                    <RangeBar label="💧 Humedad" min={c.humidity_min} max={c.humidity_max} unit="%" color="bg-cyan-400" />
                  )}
                  {c.soil_moisture_min != null && c.soil_moisture_max != null && (
                    <RangeBar label="🌱 Humedad Suelo" min={c.soil_moisture_min} max={c.soil_moisture_max} unit="%" color="bg-green-400" />
                  )}
                </div>

                {/* Botones de acción */}
                <div className="flex gap-2 pt-3 border-t border-gray-200">
                  <button
                    onClick={() => handleEdit(c)}
                    className="flex-1 bg-gradient-to-r from-blue-500 to-cyan-500 hover:from-blue-600 hover:to-cyan-600 text-white py-2 rounded-lg text-sm font-semibold transition-all duration-200 transform hover:scale-105 flex items-center justify-center gap-2">
                    ✏️ Editar
                  </button>
                  <button
                    onClick={() => handleDelete(c.id, c.name)}
                    className="flex-1 bg-gradient-to-r from-red-500 to-rose-500 hover:from-red-600 hover:to-rose-600 text-white py-2 rounded-lg text-sm font-semibold transition-all duration-200 transform hover:scale-105 flex items-center justify-center gap-2">
                    🗑️ Eliminar
                  </button>
                </div>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}

// ── Página de Simulación ─────────────────────────────────────────
function SimulationPage() {
  const [generated, setGenerated] = useState(null)
  const [loading, setLoading]     = useState(false)
  const [history, setHistory]     = useState([])

  const randBetween = (min, max) => +(Math.random() * (max - min) + min).toFixed(1)

  const generateAndInsert = async () => {
    setLoading(true)
    const timestamp = new Date().toISOString()
    const readings = [
      { sensor_id: 1, sensor_type: 'TEMPERATURE_INTERNAL', value: randBetween(15, 40), timestamp, source: 'SIMULATION' },
      { sensor_id: 2, sensor_type: 'TEMPERATURE_EXTERNAL', value: randBetween(5, 35),  timestamp, source: 'SIMULATION' },
      { sensor_id: 3, sensor_type: 'HUMIDITY',             value: randBetween(30, 90),  timestamp, source: 'SIMULATION' },
      { sensor_id: 4, sensor_type: 'SOIL_MOISTURE',        value: randBetween(20, 80),  timestamp, source: 'SIMULATION' },
    ]

    setGenerated(readings)

    try {
      for (const r of readings) {
        await api.readings.create(r)
      }
      setHistory(prev => [{ time: timestamp, readings }, ...prev].slice(0, 10))
    } catch (err) {
      console.error('Error:', err)
    }
    setLoading(false)
  }

  const typeEmoji = {
    TEMPERATURE_INTERNAL: '🌡️',
    TEMPERATURE_EXTERNAL: '🌡️',
    HUMIDITY:             '💧',
    SOIL_MOISTURE:        '🌱',
  }

  const typeLabel = {
    TEMPERATURE_INTERNAL: 'Temp. Interior',
    TEMPERATURE_EXTERNAL: 'Temp. Exterior',
    HUMIDITY:             'Humedad',
    SOIL_MOISTURE:        'Humedad Suelo',
  }

  const typeUnit = {
    TEMPERATURE_INTERNAL: '°C',
    TEMPERATURE_EXTERNAL: '°C',
    HUMIDITY:             '%',
    SOIL_MOISTURE:        '%',
  }

  return (
    <div className="space-y-4">
      <h2 className="text-xl font-bold text-gray-800">🧪 Simular Lecturas</h2>

      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-4">
        <p className="text-sm text-gray-600 mb-3">
          Genera lecturas aleatorias de sensores y las inserta en la base de datos para probar el sistema.
        </p>
        <button
          onClick={generateAndInsert}
          disabled={loading}
          className="w-full bg-green-600 hover:bg-green-700 text-white py-3 rounded-xl text-sm font-medium flex items-center justify-center gap-2 disabled:opacity-50 transition-colors"
        >
          {loading ? (
            <><div className="animate-spin text-lg">🌿</div> Generando...</>
          ) : (
            <><RefreshCw size={16} /> Generar lecturas aleatorias</>
          )}
        </button>
      </div>

      {generated && (
        <div className="bg-white rounded-2xl shadow-sm border border-green-200 p-4">
          <h3 className="text-sm font-semibold text-green-700 mb-3">✅ Lecturas generadas</h3>
          <div className="space-y-2">
            {generated.map((r, i) => (
              <div key={i} className="flex items-center justify-between py-1.5 border-b border-gray-50 last:border-0">
                <span className="text-sm text-gray-600">
                  {typeEmoji[r.sensor_type]} {typeLabel[r.sensor_type]}
                </span>
                <span className="text-sm font-bold text-gray-800">
                  {r.value} {typeUnit[r.sensor_type]}
                </span>
              </div>
            ))}
          </div>
          <p className="text-xs text-gray-400 mt-2">
            {new Date(generated[0].timestamp).toLocaleString('es-CO')}
          </p>
        </div>
      )}

      {history.length > 1 && (
        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-4">
          <h3 className="text-sm font-semibold text-gray-700 mb-2">📋 Historial de simulaciones</h3>
          <div className="space-y-2 max-h-40 overflow-y-auto">
            {history.slice(1).map((h, i) => (
              <div key={i} className="text-xs text-gray-500 flex items-center gap-2">
                <span className="text-gray-300">•</span>
                {new Date(h.time).toLocaleTimeString('es-CO')} — {h.readings.length} lecturas
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}

// ── Página de Alertas ────────────────────────────────────────────
function AlertsPage() {
  const [alerts, setAlerts] = useState([])
  const [loading, setLoading] = useState(true)
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState({ message: '', level: 'INFO' })

  useEffect(() => { loadAlerts() }, [])

  const loadAlerts = async () => {
    try {
      const data = await api.alerts.list()
      setAlerts(data.alerts || [])
    } catch (err) { console.error(err) }
    setLoading(false)
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    try {
      await api.alerts.create(form)
      setShowForm(false)
      setForm({ message: '', level: 'INFO' })
      loadAlerts()
    } catch (err) { alert('Error: ' + err.message) }
  }

  const handleMarkRead = async (id) => {
    try { await api.alerts.markRead(id); loadAlerts() }
    catch (err) { alert('Error: ' + err.message) }
  }

  const handleDelete = async (id) => {
    if (confirm('¿Eliminar?')) {
      try { await api.alerts.delete(id); loadAlerts() }
      catch (err) { alert('Error: ' + err.message) }
    }
  }

  const levelStyle = {
    CRITICAL: 'bg-red-50 border-red-200 text-red-700',
    WARNING:  'bg-yellow-50 border-yellow-200 text-yellow-700',
    INFO:     'bg-blue-50 border-blue-200 text-blue-700'
  }

  const levelIcon = { CRITICAL: '🔴', WARNING: '🟡', INFO: '🔵' }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-bold text-gray-800">🔔 Alertas</h2>
        <button onClick={() => setShowForm(!showForm)} className="bg-primary text-white px-4 py-2 rounded-xl text-sm font-medium">
          {showForm ? 'Cancelar' : '+ Nueva'}
        </button>
      </div>

      {showForm && (
        <form onSubmit={handleSubmit} className="bg-white rounded-2xl shadow-sm border border-gray-100 p-4 space-y-3">
          <input type="text" placeholder="Mensaje de alerta" value={form.message} onChange={e => setForm({...form, message: e.target.value})} className="w-full border rounded-xl px-4 py-2 text-sm" required />
          <select value={form.level} onChange={e => setForm({...form, level: e.target.value})} className="w-full border rounded-xl px-4 py-2 text-sm">
            <option value="INFO">Info</option>
            <option value="WARNING">Warning</option>
            <option value="CRITICAL">Critical</option>
          </select>
          <button type="submit" className="w-full bg-primary text-white py-2 rounded-xl font-medium">Crear Alerta</button>
        </form>
      )}

      {loading ? (
        <div className="flex items-center justify-center h-40">
          <div className="animate-spin text-4xl">🌿</div>
        </div>
      ) : alerts.length === 0 ? (
        <div className="text-center py-16 text-gray-400">
          <Bell size={48} className="mx-auto mb-3 opacity-40" />
          <p>No hay alertas recientes</p>
          <p className="text-xs mt-1">El sistema monitoreará automáticamente</p>
        </div>
      ) : (
        <div className="space-y-3">
          {alerts.map(a => (
            <div key={a.id}
              className={`rounded-2xl border p-4 ${levelStyle[a.level] || 'bg-gray-50 border-gray-200 text-gray-700'}`}>
              <div className="flex items-start gap-2">
                <span className="text-lg">{levelIcon[a.level] || '⚪'}</span>
                <div className="flex-1">
                  <p className="text-sm font-medium">{a.message}</p>
                  <p className="text-xs opacity-70 mt-1">
                    {a.level} · {new Date(a.created_at).toLocaleString('es-CO')}
                  </p>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

// ── Página de Consultar IA ───────────────────────────────────────
function AIPage() {
  const [prompt, setPrompt]       = useState('')
  const [response, setResponse]   = useState('')
  const [loading, setLoading]     = useState(false)

  const sendToAI = async (type) => {
    setLoading(true)
    setResponse('')
    
    let promptText = ''
    switch(type) {
      case 'recommendation':
        promptText = 'Eres un agrónomo experto. Basándote en las condiciones actuales del invernadero, proporciona recomendaciones específicas para optimizar el cultivo. Considera temperatura, humedad y luminosidad.'
        break
      case 'prediction':
        promptText = 'Eres experto en invernaderos. Predice qué actuadores será necesario activar en las próximas horas y por qué. Considera las tendencias actuales de los sensores.'
        break
      case 'analysis':
        promptText = 'Analiza el estado completo del invernadero. Proporciona: 1) Estado general, 2) Problemas detectados, 3) Acciones recomendadas. Sé conciso y práctico.'
        break
      default:
        promptText = prompt
    }
    
    try {
      const result = await callAI(promptText, '')
      setResponse(result)
    } catch (err) {
      setResponse('Error: ' + err.message)
    }
    setLoading(false)
  }

  const sendCustom = async () => {
    if (!prompt.trim()) return
    setLoading(true)
    try {
      const result = await callAI(prompt, '')
      setResponse(result)
    } catch (err) {
      setResponse('Error: ' + err.message)
    }
    setLoading(false)
  }

  return (
    <div className="space-y-4">
      <h2 className="text-xl font-bold text-gray-800">🤖 AgroPulse IA</h2>

      {/* Estado de IAs */}
      <div className="flex gap-2 text-xs">
        {getGroqKey() && <span className="bg-green-100 text-green-700 px-2 py-1 rounded-full">⚡ Groq</span>}
        {getGitHubToken() && <span className="bg-green-100 text-green-700 px-2 py-1 rounded-full">🐙 GitHub</span>}
        {getGemmaKey() && <span className="bg-green-100 text-green-700 px-2 py-1 rounded-full">🧠 Gemma</span>}
        {!getGroqKey() && !getGitHubToken() && !getGemmaKey() && (
          <span className="bg-yellow-100 text-yellow-700 px-2 py-1 rounded-full">⚠️ Sin IA configurada</span>
        )}
      </div>

      {/* 3 botones principales */}
      <div className="grid grid-cols-3 gap-2">
        <button
          onClick={() => sendToAI('recommendation')}
          disabled={loading}
          className="bg-white border border-gray-200 hover:border-green-400 hover:bg-green-50 rounded-xl p-3 text-sm font-medium text-gray-700 transition-all disabled:opacity-50"
        >
          💡 Recomendación
        </button>
        <button
          onClick={() => sendToAI('prediction')}
          disabled={loading}
          className="bg-white border border-gray-200 hover:border-green-400 hover:bg-green-50 rounded-xl p-3 text-sm font-medium text-gray-700 transition-all disabled:opacity-50"
        >
          🔮 Predicción
        </button>
        <button
          onClick={() => sendToAI('analysis')}
          disabled={loading}
          className="bg-white border border-gray-200 hover:border-green-400 hover:bg-green-50 rounded-xl p-3 text-sm font-medium text-gray-700 transition-all disabled:opacity-50"
        >
          🔍 Análisis
        </button>
      </div>

      {/* Pregunta libre */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-4">
        <textarea
          value={prompt}
          onChange={e => setPrompt(e.target.value)}
          placeholder="Escribe tu pregunta..."
          className="w-full border rounded-xl px-3 py-2 text-sm"
          rows={3}
        />
        <button
          onClick={sendCustom}
          disabled={loading || !prompt.trim()}
          className="mt-2 w-full bg-green-600 text-white py-2 rounded-xl font-medium disabled:opacity-50"
        >
          {loading ? 'Consultando...' : '🤖 Enviar'}
        </button>
      </div>

      {/* Respuesta */}
      {response && (
        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-4">
          <div className="flex items-center gap-2 mb-2">
            <Bot size={16} className="text-green-600" />
            <span className="text-sm font-semibold text-gray-700">Respuesta</span>
          </div>
          <p className="text-sm text-gray-700 whitespace-pre-wrap">{response}</p>
        </div>
      )}
    </div>
  )
}

// ── Página de ML
function MLPage() {
  const [prediction, setPrediction] = useState('')
  const [loading, setLoading]       = useState(false)

  const predict = async () => {
    setLoading(true)
    setPrediction('')
    const prompt = `Basándote en el historial reciente de sensores del invernadero, predice los valores de cada sensor para las próximas 6 horas (en intervalos de 1 hora).

Presenta los resultados en un formato claro con:
- Hora estimada
- Valores predichos para cada sensor
- Tendencia (subiendo, bajando, estable)
- Acciones recomendadas si algún valor saldrá de rango

Sé conciso y práctico.`

    const result = await callAI(prompt, '')
    setPrediction(result)
    setLoading(false)
  }

  return (
    <div className="space-y-4">
      <h2 className="text-xl font-bold text-gray-800">📈 ML — Predicciones</h2>

      <div className="text-xs px-3 py-2 rounded-xl bg-green-50 text-green-700">
<Cpu size={14} className="inline mr-1" /> Predicciones de Machine Learning
      </div>

      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-4">
        <p className="text-sm text-gray-600 mb-3">
          Utiliza inteligencia artificial para predecir los valores futuros de los sensores.
        </p>
        <button
          onClick={predict}
          disabled={loading}
          className="w-full bg-green-600 hover:bg-green-700 text-white py-3 rounded-xl text-sm font-medium disabled:opacity-50"
        >
          {loading ? 'Calculando...' : '🔮 Predecir próximas 6 horas'}
        </button>
      </div>

      {prediction && (
        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-4">
          <p className="text-sm text-gray-700 whitespace-pre-wrap">{prediction}</p>
        </div>
      )}
    </div>
  )
}

// ── Página de Soporte ──────────────────────────────────
function SupportPage() {
  return (
    <div className="space-y-6">
      <h2 className="text-xl font-bold text-gray-800">🎧 Soporte Técnico</h2>

      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-4">
        <h3 className="font-semibold text-gray-700 mb-3">📩 Contactar al Equipo</h3>
        <p className="text-sm text-gray-500 mb-4">
          ¿Tienes problemas o sugerencias? Escríbenos a:
        </p>
        <a href="mailto:soporte@agropulse.com" 
           className="block bg-green-600 text-white text-center py-3 rounded-xl font-medium hover:bg-green-700">
          📧 soporte@agropulse.com
        </a>
      </div>

      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-4">
        <h3 className="font-semibold text-gray-700 mb-3">📋 Información del Sistema</h3>
        <div className="space-y-2 text-sm text-gray-600">
          <p>🌿 <strong>AgroPulse</strong></p>
          <p>📅 Versión 6.0</p>
          <p>🎓 Universidad Cooperativa de Colombia</p>
        </div>
      </div>
    </div>
  )
}

// ── Página de Configuración ──────────────────────────────────────
// ── Reportes Automáticos por Email ────────────────────────────
function ReportsPage() {
  const { user } = useAuth()
  const [schedules, setSchedules] = useState([])
  const [loading, setLoading] = useState(true)
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState({
    email: user?.email || '',
    frequency: 'daily'
  })

  useEffect(() => {
    loadSchedules()
  }, [])

  const loadSchedules = async () => {
    try {
      const data = await api.reports.history()
      setSchedules(data.history || [])
    } catch (err) { console.error('Error cargando reportes:', err) }
    setLoading(false)
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    try {
      await api.reports.schedule(form)
      setShowForm(false)
      setForm({ email: user?.email || '', frequency: 'daily' })
      loadSchedules()
    } catch (err) { alert('Error: ' + err.message) }
  }

  const generateDailyReport = async () => {
    try {
      const data = await api.reports.dailyCsv()
      if (data.success) {
        const link = document.createElement('a')
        const blob = new Blob([data.csv_content], { type: 'text/csv' })
        link.href = URL.createObjectURL(blob)
        link.download = `agropulse-report-${new Date().toISOString().split('T')[0]}.csv`
        link.click()
      }
    } catch (err) {
      alert('Error: ' + err.message)
    }
  }

  const sendReport = async (email, frequency) => {
    try {
      await api.reports.sendEmail({ email, type: frequency })
      alert('✅ Reporte enviado correctamente')
      loadSchedules()
    } catch (err) {
      alert('Error: ' + err.message)
    }
  }

  const deleteSchedule = async (id) => {
    try {
      setSchedules(schedules.filter(s => s.id !== id))
    } catch (err) {
      alert('Error: ' + err.message)
    }
  }


  const calculateNextRun = (frequency) => {
    const next = new Date()
    switch (frequency) {
      case 'daily':
        next.setDate(next.getDate() + 1)
        next.setHours(8, 0, 0, 0)
        break
      case 'weekly':
        next.setDate(next.getDate() + 7)
        next.setHours(8, 0, 0, 0)
        break
      case 'monthly':
        next.setMonth(next.getMonth() + 1)
        next.setHours(8, 0, 0, 0)
        break
    }
    return next.toISOString()
  }

  const frequencyLabels = {
    'daily': '📅 Diariamente',
    'weekly': '📆 Semanalmente',
    'monthly': '📊 Mensualmente'
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-3xl font-bold text-gray-800">📧 Reportes Automáticos</h2>
          <p className="text-sm text-gray-600 mt-1">Genera y recibe reportes por email automáticamente</p>
        </div>
        <div className="flex gap-2">
          <button 
            onClick={generateDailyReport}
            className="bg-gradient-to-r from-cyan-500 to-blue-500 hover:from-cyan-600 hover:to-blue-600 text-white px-5 py-2.5 rounded-xl text-sm font-medium shadow-md hover:shadow-lg transition-all duration-200 transform hover:scale-105 flex items-center gap-2">
            📥 Generar Ahora
          </button>
          {!showForm && (
            <button 
              onClick={() => setShowForm(true)}
              className="bg-gradient-to-r from-green-500 to-emerald-500 hover:from-green-600 hover:to-emerald-600 text-white px-5 py-2.5 rounded-xl text-sm font-medium shadow-md hover:shadow-lg transition-all duration-200 transform hover:scale-105 flex items-center gap-2">
              ➕ Agendar
            </button>
          )}
        </div>
      </div>

      {showForm && (
        <form onSubmit={handleSubmit} className="bg-white rounded-2xl shadow-md border-2 border-green-200 p-6 space-y-4">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-bold text-gray-800">📧 Agendar Nuevo Reporte</h3>
            <button
              type="button"
              onClick={() => setShowForm(false)}
              className="text-gray-500 hover:text-gray-700 text-2xl">✕</button>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Correo electrónico</label>
            <input 
              type="email" 
              value={form.email} 
              onChange={e => setForm({...form, email: e.target.value})} 
              className="w-full border-2 border-gray-200 rounded-xl px-4 py-2.5 text-sm focus:border-green-500 focus:outline-none transition-colors" 
              required 
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Frecuencia</label>
            <select 
              value={form.frequency} 
              onChange={e => setForm({...form, frequency: e.target.value})} 
              className="w-full border-2 border-gray-200 rounded-xl px-4 py-2.5 text-sm focus:border-green-500 focus:outline-none transition-colors">
              <option value="daily">📅 Diariamente (8 AM)</option>
              <option value="weekly">📆 Semanalmente (Lunes 8 AM)</option>
              <option value="monthly">📊 Mensualmente (1º 8 AM)</option>
            </select>
          </div>

          <div className="flex gap-3 pt-2">
            <button 
              type="submit" 
              className="flex-1 bg-gradient-to-r from-green-500 to-emerald-500 hover:from-green-600 hover:to-emerald-600 text-white py-2.5 rounded-xl font-semibold transition-all duration-200 transform hover:scale-105">
              ✨ Agendar
            </button>
            <button 
              type="button"
              onClick={() => setShowForm(false)}
              className="flex-1 border-2 border-gray-300 text-gray-700 hover:bg-gray-50 py-2.5 rounded-xl font-semibold transition-colors">
              Cancelar
            </button>
          </div>
        </form>
      )}

      {loading ? (
        <div className="flex items-center justify-center h-40"><div className="animate-spin text-4xl">📧</div></div>
      ) : schedules.length === 0 ? (
        <div className="text-center py-16 text-gray-400">
          <Mail size={48} className="mx-auto mb-3 opacity-40" />
          <p className="text-lg font-medium">No hay reportes agendados</p>
          <button 
            onClick={() => setShowForm(true)}
            className="text-green-600 text-sm mt-3 hover:text-green-700 font-semibold">
            ➕ Agendar el primer reporte
          </button>
        </div>
      ) : (
        <div className="space-y-3">
          {schedules.map(schedule => (
            <div key={schedule.id}
              className="bg-white rounded-2xl shadow-sm border-2 border-gray-200 p-4 transition-all">
              <div className="flex items-start justify-between mb-3">
                <div className="flex-1">
                  <p className="text-base font-bold text-gray-800">📧 {schedule.email}</p>
                  <p className="text-sm text-gray-600 mt-1">Frecuencia: {frequencyLabels[schedule.frequency] || schedule.frequency}</p>
                </div>
              </div>

              <div className="flex gap-2 pt-3 border-t border-gray-200">
                <button
                  onClick={() => sendReport(schedule.email, schedule.frequency)}
                  className="flex-1 bg-gradient-to-r from-cyan-500 to-blue-500 hover:from-cyan-600 hover:to-blue-600 text-white py-2 rounded-lg text-sm font-semibold transition-colors">
                  📤 Enviar Ahora
                </button>
                <button
                  onClick={() => deleteSchedule(schedule.id)}
                  className="flex-1 border-2 border-red-300 text-red-600 hover:bg-red-50 py-2 rounded-lg text-sm font-semibold transition-colors">
                  🗑️ Eliminar
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

// ── Automatización de Reglas (IF/THEN) ────────────────────────────
function RulesPage() {
  const [rules, setRules] = useState([])
  const [loading, setLoading] = useState(true)
  const [showForm, setShowForm] = useState(false)
  const [editingId, setEditingId] = useState(null)
  const [form, setForm] = useState({
    condition_type: 'temp_high',
    condition_value: 25,
    action_type: 'activate_extractor',
    enabled: true
  })

  useEffect(() => { loadRules() }, [])

  const loadRules = async () => {
    try {
      const data = await api.rules.list()
      setRules(data.rules || [])
    } catch (err) { console.error('Error cargando reglas:', err) }
    setLoading(false)
  }

  const resetForm = () => {
    setForm({
      condition_type: 'temp_high',
      condition_value: 25,
      action_type: 'activate_extractor',
      enabled: true
    })
    setEditingId(null)
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    try {
      if (editingId) {
        await api.rules.update(editingId, form)
      } else {
        await api.rules.create(form)
      }
      setShowForm(false)
      resetForm()
      loadRules()
    } catch (err) { alert('Error: ' + err.message) }
  }

  const handleEdit = (rule) => {
    setEditingId(rule.id)
    setForm({
      condition_type: rule.condition_type,
      condition_value: rule.condition_value,
      action_type: rule.action_type,
      enabled: rule.enabled
    })
    setShowForm(true)
  }

  const handleDelete = async (id) => {
    const rule = rules.find(r => r.id === id)
    if (confirm(`¿Eliminar esta regla?`)) {
      try {
        await api.rules.delete(id)
        loadRules()
      } catch (err) { alert('Error: ' + err.message) }
    }
  }

  const toggleEnabled = async (id) => {
    const rule = rules.find(r => r.id === id)
    try {
      await api.rules.update(id, {...rule, enabled: !rule.enabled})
      loadRules()
    } catch (err) { alert('Error: ' + err.message) }
  }

  const conditionLabels = {
    'temp_high': '🔥 Temperatura alta',
    'temp_low': '❄️ Temperatura baja',
    'humidity_high': '💦 Humedad alta',
    'humidity_low': '🏜️ Humedad baja',
    'soil_dry': '🏜️ Suelo seco'
  }

  const actionLabels = {
    'activate_extractor': 'Activar extractor',
    'activate_pump': 'Activar bomba',
    'close_door': 'Cerrar puerta',
    'open_door': 'Abrir puerta'
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-3xl font-bold text-gray-800">🤖 Automatización</h2>
          <p className="text-sm text-gray-600 mt-1">Crear reglas IF/THEN para automatizar actuadores</p>
        </div>
        {!showForm && (
          <button 
            onClick={() => { setShowForm(true); resetForm() }}
            className="bg-gradient-to-r from-purple-500 to-pink-500 hover:from-purple-600 hover:to-pink-600 text-white px-5 py-2.5 rounded-xl text-sm font-medium shadow-md hover:shadow-lg transition-all duration-200 transform hover:scale-105 flex items-center gap-2">
            ➕ Nueva Regla
          </button>
        )}
      </div>

      {showForm && (
        <form onSubmit={handleSubmit} className="bg-white rounded-2xl shadow-md border-2 border-purple-200 p-6 space-y-4">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-bold text-gray-800">
              {editingId ? '✏️ Editar Regla' : '➕ Nueva Regla'}
            </h3>
            <button
              type="button"
              onClick={() => { setShowForm(false); resetForm() }}
              className="text-gray-500 hover:text-gray-700 text-2xl">✕</button>
          </div>

          <div className="bg-purple-50 rounded-xl p-4 border-2 border-purple-100 space-y-3">
            <p className="text-sm font-semibold text-purple-900">IF (Condición)</p>
            <select 
              value={form.condition_type} 
              onChange={e => setForm({...form, condition_type: e.target.value})} 
              className="w-full border-2 border-purple-200 rounded-lg px-3 py-2 text-sm focus:border-purple-500 focus:outline-none">
              {Object.entries(conditionLabels).map(([key, label]) => (
                <option key={key} value={key}>{label}</option>
              ))}
            </select>
            
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">Valor umbral</label>
              <input 
                type="number" 
                step="0.1"
                value={form.condition_value} 
                onChange={e => setForm({...form, condition_value: parseFloat(e.target.value)})} 
                className="w-full border-2 border-purple-200 rounded-lg px-3 py-2 text-sm focus:border-purple-500 focus:outline-none"
              />
            </div>
          </div>

          <div className="bg-blue-50 rounded-xl p-4 border-2 border-blue-100 space-y-3">
            <p className="text-sm font-semibold text-blue-900">THEN (Acción)</p>
            <select 
              value={form.action_type} 
              onChange={e => setForm({...form, action_type: e.target.value})} 
              className="w-full border-2 border-blue-200 rounded-lg px-3 py-2 text-sm focus:border-blue-500 focus:outline-none">
              {Object.entries(actionLabels).map(([key, label]) => (
                <option key={key} value={key}>{label}</option>
              ))}
            </select>
          </div>

          <label className="flex items-center gap-3 cursor-pointer p-3 bg-green-50 rounded-xl border-2 border-green-100">
            <input 
              type="checkbox"
              checked={form.enabled}
              onChange={e => setForm({...form, enabled: e.target.checked})}
              className="w-5 h-5 text-green-600 rounded cursor-pointer"
            />
            <span className="text-sm font-semibold text-gray-800">✅ Regla activa</span>
          </label>

          <div className="flex gap-3 pt-2">
            <button 
              type="submit" 
              className="flex-1 bg-gradient-to-r from-purple-500 to-pink-500 hover:from-purple-600 hover:to-pink-600 text-white py-2.5 rounded-xl font-semibold transition-all duration-200 transform hover:scale-105">
              {editingId ? '💾 Actualizar' : '✨ Crear'}
            </button>
            <button 
              type="button"
              onClick={() => { setShowForm(false); resetForm() }}
              className="flex-1 border-2 border-gray-300 text-gray-700 hover:bg-gray-50 py-2.5 rounded-xl font-semibold transition-colors">
              Cancelar
            </button>
          </div>
        </form>
      )}

      {loading ? (
        <div className="flex items-center justify-center h-40"><div className="animate-spin text-4xl">🤖</div></div>
      ) : rules.length === 0 ? (
        <div className="text-center py-16 text-gray-400">
          <Cpu size={48} className="mx-auto mb-3 opacity-40" />
          <p className="text-lg font-medium">No hay reglas de automatización</p>
          <button 
            onClick={() => { setShowForm(true); resetForm() }}
            className="text-purple-600 text-sm mt-3 hover:text-purple-700 font-semibold">
            ➕ Crear la primera regla
          </button>
        </div>
      ) : (
        <div className="space-y-3">
          {rules.map(rule => (
            <div key={rule.id}
              className={`bg-white rounded-2xl shadow-sm border-2 p-4 transition-all ${
                rule.enabled ? 'border-green-200' : 'border-gray-200'
              }`}>
              <div className="flex items-start justify-between mb-3">
                <div className="flex-1">
                  <div className="mt-2 space-y-1 text-sm">
                    <p className="text-gray-600">
                      <span className="font-semibold">IF</span> {conditionLabels[rule.condition_type] || rule.condition_type} &gt; {rule.condition_value}
                    </p>
                    <p className="text-gray-600">
                      <span className="font-semibold">THEN</span> {actionLabels[rule.action_type] || rule.action_type}
                    </p>
                  </div>
                </div>
                <span className={`text-xs px-3 py-1 rounded-full font-semibold ${
                  rule.enabled ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600'
                }`}>
                  {rule.enabled ? '✅ Activa' : '⏸️ Inactiva'}
                </span>
              </div>

              <div className="flex gap-2 pt-3 border-t border-gray-200">
                <button
                  onClick={() => toggleEnabled(rule.id)}
                  className={`flex-1 py-2 rounded-lg text-sm font-semibold transition-all ${
                    rule.enabled
                      ? 'bg-green-100 text-green-700 hover:bg-green-200'
                      : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                  }`}>
                  {rule.enabled ? '⏸️ Desactivar' : '▶️ Activar'}
                </button>
                <button
                  onClick={() => handleEdit(rule)}
                  className="flex-1 border-2 border-blue-300 text-blue-600 hover:bg-blue-50 py-2 rounded-lg text-sm font-semibold transition-colors">
                  ✏️ Editar
                </button>
                <button
                  onClick={() => handleDelete(rule.id)}
                  className="flex-1 border-2 border-red-300 text-red-600 hover:bg-red-50 py-2 rounded-lg text-sm font-semibold transition-colors">
                  🗑️ Eliminar
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

// ── Página de Configuración ────────────────────────────────────────
function SettingsPage() {
  const { logout } = useAuth()
  const [groqKey, setGroqKey]       = useState(safeGet('agropulse_groq_key') || '')
  const [githubKey, setGithubKey]   = useState(safeGet('agropulse_github_token') || '')
  const [gemmaKey, setGemmaKey]     = useState(safeGet('agropulse_gemma_key') || '')
  const [saved, setSaved]           = useState(false)

  const saveKeys = () => {
    if (groqKey.trim()) safeSet('agropulse_groq_key', groqKey.trim())
    else safeRemove('agropulse_groq_key')
    
    if (githubKey.trim()) safeSet('agropulse_github_token', githubKey.trim())
    else safeRemove('agropulse_github_token')
    
    if (gemmaKey.trim()) safeSet('agropulse_gemma_key', gemmaKey.trim())
    else safeRemove('agropulse_gemma_key')
    
    setSaved(true)
    setTimeout(() => setSaved(false), 2000)
  }

  const groqActive = getGroqKey()
  const githubActive = getGitHubToken()
  const gemmaActive = getGemmaKey()
  const activeCount = [groqActive, githubActive, gemmaActive].filter(Boolean).length

  return (
    <div className="space-y-4">
      <h2 className="text-xl font-bold text-gray-800">⚙️ Configuración</h2>

      {/* Info del sistema */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-4">
        <h3 className="text-sm font-semibold text-gray-700 mb-3">
          <Info size={14} className="inline mr-1" /> Información del Sistema
        </h3>
        <div className="space-y-2 text-sm">
          <div className="flex justify-between">
            <span className="text-gray-500">Versión</span>
            <span className="font-medium text-gray-800">AgroPulse v6.0</span>
          </div>
          <div className="flex justify-between">
            <span className="text-gray-500">Build</span>
            <span className="font-medium text-gray-800">2025.04</span>
          </div>
          <div className="flex justify-between">
            <span className="text-gray-500">IAs Activas</span>
            <span className="font-medium text-green-600">{activeCount} de 3</span>
          </div>
          <div className="flex justify-between">
            <span className="text-gray-500">Usuario</span>
            <span className="font-medium text-gray-800">Usuario</span>
          </div>
          <div className="flex justify-between">
            <span className="text-gray-500">Rol</span>
            <span className="font-medium text-gray-800">Usuario</span>
          </div>
          <div className="flex justify-between items-center">
            <span className="text-gray-500">Supabase</span>
            <span className={`flex items-center gap-1 text-xs font-medium px-2 py-0.5 rounded-full ${
              supabase ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'
            }`}>
              {supabase ? <><Wifi size={12} /> Conectado</> : <><WifiOff size={12} /> No configurado</>}
            </span>
          </div>
          <div className="flex justify-between items-center">
            <span className="text-gray-500">IA Activas</span>
            <span className={`flex items-center gap-1 text-xs font-medium px-2 py-0.5 rounded-full ${
              activeCount > 0 ? 'bg-green-100 text-green-700' : 'bg-yellow-100 text-yellow-700'
            }`}>
              {activeCount > 0 ? <><CheckCircle size={12} /> {activeCount} configuradas</> : <><XCircle size={12} /> Sin configurar</>}
            </span>
          </div>
        </div>
      </div>

      {/* Configurar 3 IAs */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-4">
        <h3 className="text-sm font-semibold text-gray-700 mb-3">
          <Key size={14} className="inline mr-1" /> Inteligencias Artificiales
        </h3>
        <p className="text-xs text-gray-500 mb-3">
          Configura las IAs que quieres usar en la aplicación.
        </p>

        {/* Groq */}
        <div className="mb-4 p-3 border border-gray-100 rounded-xl">
          <div className="flex justify-between items-center mb-2">
            <span className="font-medium">⚡ Groq (LLaMA-3.3-70B)</span>
            <span className={`text-xs px-2 py-0.5 rounded-full ${groqActive ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'}`}>
              {groqActive ? 'Activa' : 'No configurada'}
            </span>
          </div>
          <input
            type="password"
            value={groqKey}
            onChange={e => setGroqKey(e.target.value)}
            className="w-full border border-gray-200 rounded-xl px-3 py-2 text-sm text-xs"
            placeholder="gsk_..."
          />
        </div>

        {/* GitHub */}
        <div className="mb-4 p-3 border border-gray-100 rounded-xl">
          <div className="flex justify-between items-center mb-2">
            <span className="font-medium">🐙 GitHub (phi-4-mini)</span>
            <span className={`text-xs px-2 py-0.5 rounded-full ${githubActive ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'}`}>
              {githubActive ? 'Activa' : 'No configurada'}
            </span>
          </div>
          <input
            type="password"
            value={githubKey}
            onChange={e => setGithubKey(e.target.value)}
            className="w-full border border-gray-200 rounded-xl px-3 py-2 text-sm text-xs"
            placeholder="ghp_... (GitHub PAT)"
          />
        </div>

        {/* Gemma */}
        <div className="mb-4 p-3 border border-gray-100 rounded-xl">
          <div className="flex justify-between items-center mb-2">
            <span className="font-medium">🧠 Gemma 4 (Google AI)</span>
            <span className={`text-xs px-2 py-0.5 rounded-full ${gemmaActive ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'}`}>
              {gemmaActive ? 'Activa' : 'No configurada'}
            </span>
          </div>
          <input
            type="password"
            value={gemmaKey}
            onChange={e => setGemmaKey(e.target.value)}
            className="w-full border border-gray-200 rounded-xl px-3 py-2 text-sm text-xs"
            placeholder="AIza... (Google AI Studio)"
          />
        </div>

        <button
          onClick={saveKeys}
          className="w-full bg-green-600 hover:bg-green-700 text-white py-2 rounded-xl text-sm font-medium transition-colors"
        >
          {saved ? '✅ Todas guardadas' : '💾 Guardar todas las claves'}
        </button>
      </div>

      {/* Equipo de desarrollo */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-4">
        <h3 className="text-sm font-semibold text-gray-700 mb-3">
          👥 Equipo de Desarrollo
        </h3>
        <div className="space-y-2 text-sm">
          <div className="flex justify-between">
            <span className="text-gray-500">Desarrollador</span>
            <span className="font-medium text-gray-800">Diego Armando Pinta Cuasquen</span>
          </div>
          <div className="flex justify-between">
            <span className="text-gray-500">Universidad</span>
            <span className="font-medium text-gray-800 text-right">Cooperativa de Colombia - Nariño</span>
          </div>
          <div className="flex justify-between">
            <span className="text-gray-500">Proyecto</span>
            <span className="font-medium text-gray-800">Semestre 2025</span>
          </div>
        </div>
      </div>

      {/* Cerrar sesión */}
      <button
        onClick={logout}
        className="w-full bg-red-50 border border-red-200 text-red-600 hover:bg-red-100 py-3 rounded-xl text-sm font-medium flex items-center justify-center gap-2 transition-colors"
      >
        <LogOut size={16} /> Cerrar sesión
      </button>
    </div>
  )
}

// ══════════════════════════════════════════════════════════════════
//  APP PRINCIPAL
// ══════════════════════════════════════════════════════════════════

export default function App() {
  const [user, setUser]               = useState(null)
  const [page, setPage]               = useState('dashboard')
  const [authLoading, setAuthLoading] = useState(true)
  const [sidebarOpen, setSidebarOpen] = useState(false)

  // Verificar sesión de Supabase Auth (Google OAuth redirect)
  useEffect(() => {
    if (!supabase) {
      setAuthLoading(false)
      return
    }

    const findOrCreateUser = async (authUser) => {
      try {
        // Buscar usuario existente por email
        const { data: existing } = await supabase
          .from('users')
          .select('*')
          .eq('email', authUser.email)
          .eq('active', 1)
          .single()

        if (existing) {
          setUser(existing)
          return
        }

        // Buscar por username (parte antes del @)
        const username = authUser.email.split('@')[0]
        const { data: byUsername } = await supabase
          .from('users')
          .select('*')
          .eq('username', username)
          .eq('active', 1)
          .single()

        if (byUsername) {
          setUser(byUsername)
          return
        }

        const userEmail = authUser.email?.toLowerCase() || ''
        // ADMIN: email específico o que contenga "admin"
        const isAdmin = userEmail === 'diarpicu2022@gmail.com' || userEmail.includes('admin')
        setUser({
          id: authUser.id,
          username: username,
          full_name: authUser.user_metadata?.full_name || authUser.email,
          email: authUser.email,
          role: isAdmin ? 'ADMIN' : 'OPERATOR',
          active: 1
        })
      } catch (err) {
        console.error('Error buscando usuario:', err)
        // Fallback: usar datos de Google directamente
        setUser({
          id: authUser.id,
          username: authUser.email.split('@')[0],
          full_name: authUser.user_metadata?.full_name || authUser.email,
          email: authUser.email,
          role: 'OPERATOR',
          active: 1
        })
      }
    }

    // Verificar sesión existente
    if (!supabase) {
      setAuthLoading(false)
      return
    }
    supabase.auth.getSession().then(({ data: { session } }) => {
      if (session?.user) {
        findOrCreateUser(session.user)
      }
      setAuthLoading(false)
    })

    // Escuchar cambios de auth
    const { data: { subscription } } = supabase.auth.onAuthStateChange((_event, session) => {
      if (session?.user) {
        findOrCreateUser(session.user)
      }
      setAuthLoading(false)
    })

    return () => subscription.unsubscribe()
  }, [])

  const handleLogin = (userData) => setUser(userData)
  const handleLogout = async () => {
    setUser(null)
    setPage('dashboard')
  }

  if (authLoading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-green-800 to-green-600 flex items-center justify-center">
        <div className="text-center text-white">
          <div className="animate-spin text-5xl mb-4">🌿</div>
          <p className="text-lg font-medium">Cargando AgroPulse...</p>
        </div>
      </div>
    )
  }

  if (!user) return <LoginPage onLogin={handleLogin} />

  // Use role directly from login response
  const userRole = user.role || 'user'
  const isAdmin = userRole === 'admin' || userRole === 'ADMIN'

  const navItems = isAdmin ? [
    { id: 'dashboard', label: 'Inicio',       icon: Home },
    { id: 'analytics', label: 'Analíticas',    icon: BarChart3 },
    { id: 'sensors',    label: 'Sensores',     icon: Activity },
    { id: 'actuators', label: 'Actuadores',   icon: Zap },
    { id: 'rules', label: 'Automatización', icon: Cpu },
    { id: 'reports', label: 'Reportes', icon: Mail },
    { id: 'greenhouses',label: 'Invernadero', icon: Sprout },
    { id: 'crops',     label: 'Cultivos',     icon: Leaf },
    { id: 'simulate',  label: 'Simular',     icon: RefreshCw },
    { id: 'ai',       label: 'IA',         icon: Bot },
    { id: 'ml',       label: 'ML',         icon: Cpu },
    { id: 'alerts',   label: 'Alertas',     icon: Bell },
    { id: 'logs',     label: 'Logs',        icon: Activity },
    { id: 'users',    label: 'Usuarios',   icon: Key },
    { id: 'admin',    label: 'Gestión Roles', icon: Settings },
    { id: 'support',  label: 'Soporte',    icon: MessageCircle },
    { id: 'settings', label: 'Config',     icon: Settings },
  ] : [
    { id: 'dashboard', label: 'Inicio',    icon: Home },
    { id: 'analytics', label: 'Analíticas', icon: BarChart3 },
    { id: 'sensors',   label: 'Sensores',  icon: Activity },
    { id: 'actuators', label: 'Actuadores',icon: Zap },
    { id: 'rules', label: 'Automatización', icon: Cpu },
    { id: 'reports', label: 'Reportes', icon: Mail },
    { id: 'crops',     label: 'Cultivos',  icon: Leaf },
    { id: 'simulate',  label: 'Simular',  icon: RefreshCw },
    { id: 'ai',       label: 'IA',      icon: Bot },
    { id: 'ml',      label: 'ML',     icon: Cpu },
    { id: 'alerts',   label: 'Alertas',  icon: Bell },
    { id: 'support',  label: 'Soporte',  icon: MessageCircle },
    { id: 'settings', label: 'Config',  icon: Settings },
  ]

  const navigate = (id) => {
    setPage(id)
    setSidebarOpen(false)
  }

  return (
    <AuthContext.Provider value={{ user, logout: handleLogout }}>
      <div className="min-h-screen bg-gray-50 lg:max-w-full mx-auto relative">

        {/* Sidebar Overlay */}
        {sidebarOpen && (
          <div className="fixed inset-0 bg-black bg-opacity-50 z-30 lg:hidden"
            onClick={() => setSidebarOpen(false)} />
        )}

        {/* Sidebar */}
        <div className={`fixed top-0 left-0 h-full w-64 bg-gradient-to-b from-green-700 via-green-600 to-emerald-700 text-white shadow-2xl z-40 transform transition-transform duration-300 ease-in-out ${
          sidebarOpen ? 'translate-x-0' : '-translate-x-full'
        } lg:block lg:translate-x-0`}>
          {/* Sidebar Header */}
          <div className="bg-gradient-to-r from-green-800 to-emerald-700 px-5 py-5 flex items-center justify-between border-b border-green-600">
            <div className="flex-1">
              <div className="flex items-center gap-2">
                <span className="text-2xl font-bold">🌿</span>
                <div>
                  <span className="text-lg font-bold">AgroPulse</span>
                  <span className="text-xs opacity-60 bg-green-500 px-2 py-0.5 rounded ml-1">v6.0</span>
                </div>
              </div>
              <p className="text-xs opacity-70 mt-2 text-gray-200">{user.full_name || user.username}</p>
            </div>
            <button onClick={() => setSidebarOpen(false)}
              className="p-1 hover:bg-green-700 rounded-lg transition-colors lg:hidden">
              <X size={20} />
            </button>
          </div>

          {/* Sidebar Nav Items */}
          <div className="py-3 overflow-y-auto" style={{ maxHeight: 'calc(100vh - 85px)' }}>
            {navItems.map(item => {
              const Icon = item.icon
              const active = page === item.id
              return (
                <button key={item.id}
                  onClick={() => navigate(item.id)}
                  className={`w-full flex items-center gap-3 px-5 py-3 text-sm transition-all duration-200 ${
                    active
                      ? 'bg-green-500 text-white font-semibold border-r-4 border-yellow-300 shadow-md'
                      : 'text-green-100 hover:bg-green-600 hover:text-white'
                  }`}>
                  <Icon size={20} className={active ? 'text-yellow-300' : ''} />
                  <span className="flex-1 text-left">{item.label}</span>
                  {active && <ChevronRight size={16} className="text-yellow-300" />}
                </button>
              )
            })}

            {/* Logout in sidebar */}
            <div className="border-t border-green-500 mt-3 pt-3">
              <button
                onClick={handleLogout}
                className="w-full flex items-center gap-3 px-5 py-3 text-sm text-red-200 hover:bg-red-600 hover:text-white transition-all duration-200">
                <LogOut size={20} />
                <span>Cerrar sesión</span>
              </button>
            </div>
          </div>
        </div>

        {/* Header */}
        <header className="bg-gradient-to-r from-green-700 via-green-600 to-emerald-600 text-white px-4 py-4 flex items-center justify-between sticky top-0 z-20 shadow-lg lg:ml-64">
          <div className="flex items-center gap-3">
            <button onClick={() => setSidebarOpen(true)}
              className="p-2 hover:bg-green-600 rounded-lg transition-colors lg:hidden">
              <Menu size={20} />
            </button>
            <div className="flex items-center gap-2">
              <span className="text-2xl font-bold">🌿</span>
              <div>
                <h1 className="text-lg font-bold">AgroPulse</h1>
                <p className="text-xs opacity-70">Sistema de Monitoreo Inteligente</p>
              </div>
            </div>
          </div>
          <div className="flex items-center gap-4">
            <div className="text-right hidden sm:block">
              <p className="text-xs opacity-70">Bienvenido</p>
              <p className="text-sm font-semibold">{user.full_name || user.username}</p>
            </div>
            <div className="w-10 h-10 bg-white bg-opacity-20 rounded-full flex items-center justify-center">
              <span className="text-sm font-bold">👤</span>
            </div>
          </div>
        </header>

        {/* Contenido */}
        <main className="px-4 py-4 pb-6 lg:px-8 lg:ml-64">
          {page === 'dashboard'   && <Dashboard />}
          {page === 'analytics'   && <AnalyticsPage />}
          {page === 'sensors'    && <SensorsPage />}
          {page === 'actuators'  && <ActuatorsPage />}
          {page === 'rules'      && <RulesPage />}
          {page === 'greenhouses' && <GreenhousePage />}
          {page === 'crops'      && <CropsPage />}
          {page === 'simulate'   && <SimulationPage />}
          {page === 'ai'         && <AIPage />}
          {page === 'ml'        && <MLPage />}
          {page === 'alerts'    && <AlertsPage />}
          {page === 'logs'       && <LogsPage />}
          {page === 'users'      && <UsersPage />}
          {page === 'admin'      && <AdminPanel user={user} />}
          {page === 'support'    && <SupportPage />}
          {page === 'settings'   && <SettingsPage />}
        </main>
      </div>
      <DebugInfo />
    </AuthContext.Provider>
  )
}

// ── Analytics Page (Gráficas e Históricos) ─────────────────────
function AnalyticsPage() {
  const [readings, setReadings] = useState([])
  const [loading, setLoading] = useState(true)
  const [range, setRange] = useState('24h') // '24h', '7d', '30d'
  const [showPrintPreview, setShowPrintPreview] = useState(false)

  useEffect(() => { loadReadings() }, [])

  const loadReadings = async () => {
    try {
      // Obtener muchas lecturas (últimas 500)
      const data = await api.readings.list(null, 500)
      setReadings(data.readings || [])
    } catch (err) { console.error('Error cargando lecturas:', err) }
    setLoading(false)
  }

  // Filtrar lecturas por rango de fecha
  const getFilteredReadings = () => {
    const now = new Date()
    const cutoff = new Date()
    
    switch(range) {
      case '24h': cutoff.setHours(now.getHours() - 24); break
      case '7d': cutoff.setDate(now.getDate() - 7); break
      case '30d': cutoff.setDate(now.getDate() - 30); break
      default: cutoff.setHours(now.getHours() - 24)
    }
    
    return readings.filter(r => {
      const readingDate = new Date(r.timestamp)
      return readingDate >= cutoff && readingDate <= now
    })
  }

  // Exportar CSV
  const exportToCSV = () => {
    const filtered = getFilteredReadings()
    if (filtered.length === 0) {
      alert('No hay datos para exportar')
      return
    }

    // Encabezados
    const headers = ['Fecha/Hora', 'Tipo de Sensor', 'Valor', 'Unidad', 'Origen']
    
    // Filas de datos
    const rows = filtered.map(r => {
      const date = new Date(r.timestamp).toLocaleString('es-CO')
      const unit = r.sensorType === 'SOIL_MOISTURE' || r.sensorType === 'HUMIDITY' ? '%' : '°C'
      return [date, r.sensorType, r.value, unit, r.source || 'ESP32']
    })
    
    // Crear CSV
    const csv = [
      headers.join(','),
      ...rows.map(row => row.map(cell => `"${cell}"`).join(','))
    ].join('\n')
    
    // Descargar
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
    const link = document.createElement('a')
    const url = URL.createObjectURL(blob)
    link.setAttribute('href', url)
    link.setAttribute('download', `agropulse-export-${new Date().toISOString().split('T')[0]}.csv`)
    link.style.visibility = 'hidden'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  }

  // Exportar PDF (mediante vista imprimible)
  const handlePrintPDF = () => {
    setShowPrintPreview(true)
    setTimeout(() => window.print(), 200)
  }

  const filteredReadings = getFilteredReadings()

  // Agrupar lecturas por tipo y hora
  const getChartData = (sensorType) => {
    const byTime = {}
    filteredReadings
      .filter(r => r.sensorType === sensorType)
      .forEach(r => {
        const time = new Date(r.timestamp)
        const key = time.toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit', hour12: false })
        if (!byTime[key]) byTime[key] = []
        byTime[key].push(r.value)
      })
    
    return Object.entries(byTime).map(([time, values]) => ({
      time,
      value: (values.reduce((a, b) => a + b, 0) / values.length).toFixed(1)
    })).slice(-24) // últimas 24 puntos
  }

  // Calcular estadísticas
  const getStats = (sensorType) => {
    const values = filteredReadings
      .filter(r => r.sensorType === sensorType)
      .map(r => r.value)
    
    if (values.length === 0) return { min: 0, max: 0, avg: 0, current: 0 }
    
    const current = values[0]
    const min = Math.min(...values)
    const max = Math.max(...values)
    const avg = (values.reduce((a, b) => a + b, 0) / values.length).toFixed(1)
    
    return { min: min.toFixed(1), max: max.toFixed(1), avg, current: current.toFixed(1) }
  }

  const tempStats = getStats('TEMPERATURE_INTERNAL')
  const humidStats = getStats('HUMIDITY')
  const soilStats = getStats('SOIL_MOISTURE')

  // Interfaz de vista previa para impresión
  if (showPrintPreview) {
    return (
      <div className="print-preview">
        <style>{`
          @media print {
            body * { visibility: hidden; }
            .print-preview, .print-preview * { visibility: visible; }
            .print-preview { position: absolute; left: 0; top: 0; width: 100%; }
            .print-header { page-break-after: avoid; }
            .print-table { width: 100%; border-collapse: collapse; margin-top: 20px; }
            .print-table th, .print-table td { border: 1px solid #999; padding: 8px; text-align: left; }
            .print-table th { background-color: #f0f0f0; font-weight: bold; }
            .print-stats { page-break-inside: avoid; margin: 20px 0; }
            .no-print { display: none; }
          }
        `}</style>
        
        <div className="print-content p-8">
          <div className="print-header mb-8">
            <h1 className="text-3xl font-bold text-gray-800 mb-2">📊 Reporte de Analíticas AgroPulse</h1>
            <p className="text-gray-600">Generado: {new Date().toLocaleString('es-CO')}</p>
            <p className="text-gray-600">Período: {range === '24h' ? 'Últimas 24 horas' : range === '7d' ? 'Últimos 7 días' : 'Últimos 30 días'}</p>
          </div>

          {/* Resumen ejecutivo */}
          <div className="print-stats">
            <h2 className="text-xl font-bold text-gray-800 mb-4">Resumen Ejecutivo</h2>
            <div className="grid grid-cols-4 gap-4 text-sm">
              <div className="border p-3 rounded">
                <p className="text-gray-600">🌡️ Temperatura</p>
                <p className="text-lg font-bold">Min: {tempStats.min}°C | Máx: {tempStats.max}°C | Promedio: {tempStats.avg}°C</p>
              </div>
              <div className="border p-3 rounded">
                <p className="text-gray-600">💧 Humedad</p>
                <p className="text-lg font-bold">Min: {humidStats.min}% | Máx: {humidStats.max}% | Promedio: {humidStats.avg}%</p>
              </div>
              <div className="border p-3 rounded">
                <p className="text-gray-600">🌱 Suelo</p>
                <p className="text-lg font-bold">Min: {soilStats.min}% | Máx: {soilStats.max}% | Promedio: {soilStats.avg}%</p>
              </div>
              <div className="border p-3 rounded">
                <p className="text-gray-600">📈 Total Lecturas</p>
                <p className="text-lg font-bold">{filteredReadings.length}</p>
              </div>
            </div>
          </div>

          {/* Tabla detallada */}
          <h2 className="text-xl font-bold text-gray-800 mt-8 mb-4">Datos Detallados</h2>
          <table className="print-table">
            <thead>
              <tr>
                <th>Fecha/Hora</th>
                <th>Tipo de Sensor</th>
                <th>Valor</th>
                <th>Unidad</th>
                <th>Origen</th>
              </tr>
            </thead>
            <tbody>
              {filteredReadings.slice(0, 500).map((r, idx) => (
                <tr key={idx}>
                  <td>{new Date(r.timestamp).toLocaleString('es-CO')}</td>
                  <td>{r.sensorType}</td>
                  <td>{r.value}</td>
                  <td>{r.sensorType === 'SOIL_MOISTURE' || r.sensorType === 'HUMIDITY' ? '%' : '°C'}</td>
                  <td>{r.source || 'ESP32'}</td>
                </tr>
              ))}
            </tbody>
          </table>

          <div className="no-print mt-8 flex gap-3">
            <button 
              onClick={() => setShowPrintPreview(false)}
              className="bg-gray-500 text-white px-6 py-2 rounded-lg font-semibold">
              Cerrar Vista Previa
            </button>
            <button 
              onClick={() => window.print()}
              className="bg-blue-500 text-white px-6 py-2 rounded-lg font-semibold">
              🖨️ Imprimir / Guardar como PDF
            </button>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between flex-wrap gap-4">
        <div>
          <h2 className="text-3xl font-bold text-gray-800">📊 Analíticas en Tiempo Real</h2>
          <p className="text-sm text-gray-600 mt-1">Gráficas e históricos de sensores</p>
        </div>
        <div className="flex gap-2">
          <button 
            onClick={loadReadings}
            className="bg-gradient-to-r from-green-500 to-emerald-500 hover:from-green-600 hover:to-emerald-600 text-white px-4 py-2.5 rounded-xl text-sm font-medium shadow-md hover:shadow-lg transition-all duration-200 transform hover:scale-105 flex items-center gap-2"
          >
            <RefreshCw size={16} /> Refrescar
          </button>
          <button 
            onClick={exportToCSV}
            className="bg-gradient-to-r from-cyan-500 to-blue-500 hover:from-cyan-600 hover:to-blue-600 text-white px-4 py-2.5 rounded-xl text-sm font-medium shadow-md hover:shadow-lg transition-all duration-200 transform hover:scale-105 flex items-center gap-2"
          >
            📥 Descargar CSV
          </button>
          <button 
            onClick={handlePrintPDF}
            className="bg-gradient-to-r from-purple-500 to-pink-500 hover:from-purple-600 hover:to-pink-600 text-white px-4 py-2.5 rounded-xl text-sm font-medium shadow-md hover:shadow-lg transition-all duration-200 transform hover:scale-105 flex items-center gap-2"
          >
            📄 Generar PDF
          </button>
        </div>
      </div>

      {/* Range Selector */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-4">
        <div className="flex gap-3 flex-wrap">
          {['24h', '7d', '30d'].map(r => (
            <button
              key={r}
              onClick={() => setRange(r)}
              className={`px-6 py-2 rounded-xl font-medium transition-all ${
                range === r
                  ? 'bg-gradient-to-r from-green-500 to-emerald-500 text-white shadow-md'
                  : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
              }`}
            >
              {r === '24h' ? 'Últimas 24h' : r === '7d' ? 'Últimos 7 días' : 'Últimos 30 días'}
            </button>
          ))}
        </div>
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-12">
          <div className="text-center">
            <div className="text-4xl mb-3 animate-bounce">📊</div>
            <p className="text-gray-600">Cargando gráficas...</p>
          </div>
        </div>
      ) : (
        <>
          {/* Gráficas */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
            {/* Temperatura */}
            <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-5">
              <h3 className="text-lg font-bold text-gray-800 mb-3 flex items-center gap-2">
                <Thermometer size={20} className="text-orange-500" /> Temperatura Interior
              </h3>
              <ResponsiveContainer width="100%" height={300}>
                <LineChart data={getChartData('TEMPERATURE_INTERNAL')}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#ddd" />
                  <XAxis dataKey="time" tick={{ fontSize: 12 }} />
                  <YAxis tick={{ fontSize: 12 }} domain={['dataMin - 5', 'dataMax + 5']} />
                  <Tooltip formatter={(v) => `${v}°C`} />
                  <Line type="monotone" dataKey="value" stroke="#f97316" strokeWidth={2} dot={{ r: 3 }} />
                </LineChart>
              </ResponsiveContainer>
              <div className="grid grid-cols-2 gap-2 mt-3 text-xs">
                <div className="bg-orange-50 p-2 rounded-lg">
                  <p className="text-gray-600">Mín</p>
                  <p className="text-lg font-bold text-orange-600">{tempStats.min}°C</p>
                </div>
                <div className="bg-orange-50 p-2 rounded-lg">
                  <p className="text-gray-600">Máx</p>
                  <p className="text-lg font-bold text-orange-600">{tempStats.max}°C</p>
                </div>
                <div className="bg-orange-50 p-2 rounded-lg">
                  <p className="text-gray-600">Promedio</p>
                  <p className="text-lg font-bold text-orange-600">{tempStats.avg}°C</p>
                </div>
                <div className="bg-orange-50 p-2 rounded-lg">
                  <p className="text-gray-600">Actual</p>
                  <p className="text-lg font-bold text-orange-600">{tempStats.current}°C</p>
                </div>
              </div>
            </div>

            {/* Humedad */}
            <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-5">
              <h3 className="text-lg font-bold text-gray-800 mb-3 flex items-center gap-2">
                <Droplets size={20} className="text-blue-500" /> Humedad Ambiente
              </h3>
              <ResponsiveContainer width="100%" height={300}>
                <LineChart data={getChartData('HUMIDITY')}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#ddd" />
                  <XAxis dataKey="time" tick={{ fontSize: 12 }} />
                  <YAxis tick={{ fontSize: 12 }} domain={[0, 100]} />
                  <Tooltip formatter={(v) => `${v}%`} />
                  <Line type="monotone" dataKey="value" stroke="#0ea5e9" strokeWidth={2} dot={{ r: 3 }} />
                </LineChart>
              </ResponsiveContainer>
              <div className="grid grid-cols-2 gap-2 mt-3 text-xs">
                <div className="bg-blue-50 p-2 rounded-lg">
                  <p className="text-gray-600">Mín</p>
                  <p className="text-lg font-bold text-blue-600">{humidStats.min}%</p>
                </div>
                <div className="bg-blue-50 p-2 rounded-lg">
                  <p className="text-gray-600">Máx</p>
                  <p className="text-lg font-bold text-blue-600">{humidStats.max}%</p>
                </div>
                <div className="bg-blue-50 p-2 rounded-lg">
                  <p className="text-gray-600">Promedio</p>
                  <p className="text-lg font-bold text-blue-600">{humidStats.avg}%</p>
                </div>
                <div className="bg-blue-50 p-2 rounded-lg">
                  <p className="text-gray-600">Actual</p>
                  <p className="text-lg font-bold text-blue-600">{humidStats.current}%</p>
                </div>
              </div>
            </div>

            {/* Suelo */}
            <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-5">
              <h3 className="text-lg font-bold text-gray-800 mb-3 flex items-center gap-2">
                <Leaf size={20} className="text-green-600" /> Humedad del Suelo
              </h3>
              <ResponsiveContainer width="100%" height={300}>
                <LineChart data={getChartData('SOIL_MOISTURE')}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#ddd" />
                  <XAxis dataKey="time" tick={{ fontSize: 12 }} />
                  <YAxis tick={{ fontSize: 12 }} domain={[0, 100]} />
                  <Tooltip formatter={(v) => `${v}%`} />
                  <Line type="monotone" dataKey="value" stroke="#22c55e" strokeWidth={2} dot={{ r: 3 }} />
                </LineChart>
              </ResponsiveContainer>
              <div className="grid grid-cols-2 gap-2 mt-3 text-xs">
                <div className="bg-green-50 p-2 rounded-lg">
                  <p className="text-gray-600">Mín</p>
                  <p className="text-lg font-bold text-green-600">{soilStats.min}%</p>
                </div>
                <div className="bg-green-50 p-2 rounded-lg">
                  <p className="text-gray-600">Máx</p>
                  <p className="text-lg font-bold text-green-600">{soilStats.max}%</p>
                </div>
                <div className="bg-green-50 p-2 rounded-lg">
                  <p className="text-gray-600">Promedio</p>
                  <p className="text-lg font-bold text-green-600">{soilStats.avg}%</p>
                </div>
                <div className="bg-green-50 p-2 rounded-lg">
                  <p className="text-gray-600">Actual</p>
                  <p className="text-lg font-bold text-green-600">{soilStats.current}%</p>
                </div>
              </div>
            </div>
          </div>

          {/* Resumen */}
          <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-5">
            <h3 className="text-lg font-bold text-gray-800 mb-3">📈 Resumen</h3>
            <div className="grid grid-cols-1 md:grid-cols-4 gap-4 text-sm">
              <div className="border-l-4 border-orange-500 pl-3">
                <p className="text-gray-600">Lecturas de temperatura</p>
                <p className="text-2xl font-bold text-orange-600">{filteredReadings.filter(r => r.sensorType === 'TEMPERATURE_INTERNAL').length}</p>
              </div>
              <div className="border-l-4 border-blue-500 pl-3">
                <p className="text-gray-600">Lecturas de humedad</p>
                <p className="text-2xl font-bold text-blue-600">{filteredReadings.filter(r => r.sensorType === 'HUMIDITY').length}</p>
              </div>
              <div className="border-l-4 border-green-600 pl-3">
                <p className="text-gray-600">Lecturas de suelo</p>
                <p className="text-2xl font-bold text-green-600">{filteredReadings.filter(r => r.sensorType === 'SOIL_MOISTURE').length}</p>
              </div>
              <div className="border-l-4 border-purple-500 pl-3">
                <p className="text-gray-600">Total de lecturas</p>
                <p className="text-2xl font-bold text-purple-600">{filteredReadings.length}</p>
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  )
}

// ── Greenhouse Page ─────────────────────────────────────────
function GreenhousePage() {
  const [greenhouses, setGreenhouses] = useState([])
  const [loading, setLoading] = useState(true)
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState({ name: '', location: '', description: '' })

  useEffect(() => { loadGreenhouses() }, [])

  const loadGreenhouses = async () => {
    try {
      const data = await api.greenhouses.list()
      setGreenhouses(data.greenhouses || [])
    } catch (err) { console.error(err) }
    setLoading(false)
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    try {
      await api.greenhouses.create(form)
      setShowForm(false)
      setForm({ name: '', location: '', description: '' })
      loadGreenhouses()
    } catch (err) { alert('Error: ' + err.message) }
  }

  const handleDelete = async (id) => {
    if (confirm('¿Eliminar?')) {
      try {
        await api.greenhouses.delete(id)
        loadGreenhouses()
      } catch (err) { alert('Error: ' + err.message) }
    }
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-bold text-gray-800">🏡 Invernaderos</h2>
        <button onClick={() => setShowForm(!showForm)} className="bg-primary text-white px-4 py-2 rounded-xl text-sm font-medium hover:bg-green-700">
          {showForm ? 'Cancelar' : '+ Nuevo'}
        </button>
      </div>

      {showForm && (
        <form onSubmit={handleSubmit} className="bg-white rounded-2xl shadow-sm border border-gray-100 p-4 space-y-3">
          <input type="text" placeholder="Nombre" value={form.name} onChange={e => setForm({...form, name: e.target.value})} className="w-full border rounded-xl px-4 py-2 text-sm" required />
          <input type="text" placeholder="Ubicación" value={form.location} onChange={e => setForm({...form, location: e.target.value})} className="w-full border rounded-xl px-4 py-2 text-sm" />
          <textarea placeholder="Descripción" value={form.description} onChange={e => setForm({...form, description: e.target.value})} className="w-full border rounded-xl px-4 py-2 text-sm" />
          <button type="submit" className="w-full bg-primary text-white py-2 rounded-xl font-medium">Guardar</button>
        </form>
      )}

      {loading ? <div className="text-center py-8">Cargando...</div> : greenhouses.length === 0 ? (
        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-8 text-center">
          <p className="text-gray-500">No hay invernaderos</p>
          <p className="text-sm text-gray-400 mt-1">Crea uno nuevo</p>
        </div>
      ) : (
        <div className="grid gap-3">
          {greenhouses.map(g => (
            <div key={g.id} className="bg-white rounded-2xl shadow-sm border border-gray-100 p-4">
              <div className="flex items-start justify-between">
                <div>
                  <h3 className="font-semibold text-gray-800">{g.name}</h3>
                  <p className="text-sm text-gray-500">{g.location}</p>
                  <p className="text-xs text-gray-400 mt-1">{g.description}</p>
                </div>
                <button onClick={() => handleDelete(g.id)} className="text-red-500 text-sm">Eliminar</button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

// ── Logs Page ─────────────────────────────────────────────
function LogsPage() {
  const [logs, setLogs] = useState([])
  const [loading, setLoading] = useState(true)
  const [filterAction, setFilterAction] = useState('')
  const [filterUser, setFilterUser] = useState('')
  const [searchText, setSearchText] = useState('')

  useEffect(() => { loadLogs() }, [])

  const loadLogs = async () => {
    try {
      const data = await api.logs.list(200)
      setLogs(data.logs || [])
    } catch (err) { console.error(err) }
    setLoading(false)
  }

  // Filtrar logs según criterios
  const filteredLogs = logs.filter(l => {
    const matchAction = !filterAction || l.action.includes(filterAction)
    const matchUser = !filterUser || l.performedBy.includes(filterUser)
    const matchSearch = !searchText || 
      l.action.toLowerCase().includes(searchText.toLowerCase()) ||
      l.details.toLowerCase().includes(searchText.toLowerCase()) ||
      l.performedBy.toLowerCase().includes(searchText.toLowerCase())
    return matchAction && matchUser && matchSearch
  })

  const actionTypes = [...new Set(logs.map(l => l.action))].sort()
  const userList = [...new Set(logs.map(l => l.performedBy))].sort()

  // Colores según tipo de acción
  const getActionColor = (action) => {
    if (action.includes('LOGIN')) return 'bg-blue-50 text-blue-700 border-blue-200'
    if (action.includes('DELETE')) return 'bg-red-50 text-red-700 border-red-200'
    if (action.includes('CREATE')) return 'bg-green-50 text-green-700 border-green-200'
    if (action.includes('UPDATE')) return 'bg-yellow-50 text-yellow-700 border-yellow-200'
    return 'bg-gray-50 text-gray-700 border-gray-200'
  }

  const getActionIcon = (action) => {
    if (action.includes('LOGIN')) return '🔐'
    if (action.includes('DELETE')) return '🗑️'
    if (action.includes('CREATE')) return '✨'
    if (action.includes('UPDATE')) return '✏️'
    return '📝'
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-3xl font-bold text-gray-800">📋 Logs del Sistema</h2>
          <p className="text-sm text-gray-600 mt-1">Total: <strong>{filteredLogs.length}</strong> registros</p>
        </div>
        <button 
          onClick={loadLogs}
          className="bg-gradient-to-r from-green-500 to-emerald-500 hover:from-green-600 hover:to-emerald-600 text-white px-4 py-2.5 rounded-xl text-sm font-medium shadow-md hover:shadow-lg transition-all duration-200 transform hover:scale-105 flex items-center gap-2"
        >
          <RefreshCw size={16} /> Refrescar
        </button>
      </div>

      {/* Filtros */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-4">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-3">
          {/* Búsqueda */}
          <input
            type="text"
            placeholder="🔍 Buscar en acciones, detalles..."
            value={searchText}
            onChange={e => setSearchText(e.target.value)}
            className="border-2 border-gray-200 rounded-xl px-3 py-2 text-sm focus:border-green-500 focus:outline-none transition-colors"
          />

          {/* Filtro por acción */}
          <select
            value={filterAction}
            onChange={e => setFilterAction(e.target.value)}
            className="border-2 border-gray-200 rounded-xl px-3 py-2 text-sm focus:border-green-500 focus:outline-none transition-colors"
          >
            <option value="">Todas las acciones</option>
            {actionTypes.map(a => (
              <option key={a} value={a}>{a}</option>
            ))}
          </select>

          {/* Filtro por usuario */}
          <select
            value={filterUser}
            onChange={e => setFilterUser(e.target.value)}
            className="border-2 border-gray-200 rounded-xl px-3 py-2 text-sm focus:border-green-500 focus:outline-none transition-colors"
          >
            <option value="">Todos los usuarios</option>
            {userList.map(u => (
              <option key={u} value={u}>{u}</option>
            ))}
          </select>

          {/* Limpiar filtros */}
          {(filterAction || filterUser || searchText) && (
            <button
              onClick={() => { setFilterAction(''); setFilterUser(''); setSearchText('') }}
              className="border-2 border-gray-300 text-gray-700 hover:bg-gray-50 rounded-xl px-3 py-2 text-sm font-medium transition-colors"
            >
              🔄 Limpiar
            </button>
          )}
        </div>
      </div>

      {/* Logs */}
      {loading ? (
        <div className="flex items-center justify-center py-12">
          <div className="text-center">
            <div className="text-4xl mb-3 animate-bounce">📋</div>
            <p className="text-gray-600">Cargando logs...</p>
          </div>
        </div>
      ) : filteredLogs.length === 0 ? (
        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-12 text-center">
          <p className="text-gray-500 text-lg">No hay registros que coincidan con los filtros</p>
        </div>
      ) : (
        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 divide-y max-h-[600px] overflow-y-auto">
          {filteredLogs.map(l => (
            <div key={l.id} className={`p-4 border-l-4 ${getActionColor(l.action)}`}>
              <div className="flex items-start justify-between gap-3">
                <div className="flex-1">
                  <div className="flex items-center gap-2 mb-2">
                    <span className="text-2xl">{getActionIcon(l.action)}</span>
                    <span className="font-bold text-sm">{l.action}</span>
                    <span className="text-xs text-gray-500 bg-gray-100 px-2 py-1 rounded">
                      {new Date(l.timestamp).toLocaleString('es-CO')}
                    </span>
                  </div>
                  <p className="text-sm text-gray-700 mb-1">{l.details}</p>
                  <p className="text-xs text-gray-500">Por: <strong>{l.performedBy}</strong></p>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

// ── Users Page ────────────────────────────────────────────
function UsersPage() {
  const [users, setUsers] = useState([])
  const [loading, setLoading] = useState(true)
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState({ username: '', password: '', fullName: '', role: 'USER' })

  useEffect(() => { loadUsers() }, [])

  const loadUsers = async () => {
    try {
      const data = await api.users.list()
      setUsers(data.users || [])
    } catch (err) { console.error(err) }
    setLoading(false)
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    try {
      await api.users.create(form)
      setShowForm(false)
      setForm({ username: '', password: '', fullName: '', role: 'USER' })
      loadUsers()
    } catch (err) { alert('Error: ' + err.message) }
  }

  const handleDelete = async (id) => {
    if (confirm('¿Eliminar usuario?')) {
      try {
        await api.users.delete(id)
        loadUsers()
      } catch (err) { alert('Error: ' + err.message) }
    }
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-bold text-gray-800">👥 Usuarios</h2>
        <button onClick={() => setShowForm(!showForm)} className="bg-primary text-white px-4 py-2 rounded-xl text-sm font-medium">
          {showForm ? 'Cancelar' : '+ Nuevo'}
        </button>
      </div>

      {showForm && (
        <form onSubmit={handleSubmit} className="bg-white rounded-2xl shadow-sm border border-gray-100 p-4 space-y-3">
          <input type="text" placeholder="Usuario" value={form.username} onChange={e => setForm({...form, username: e.target.value})} className="w-full border rounded-xl px-4 py-2 text-sm" required />
          <input type="password" placeholder="Contraseña" value={form.password} onChange={e => setForm({...form, password: e.target.value})} className="w-full border rounded-xl px-4 py-2 text-sm" required />
          <input type="text" placeholder="Nombre completo" value={form.fullName} onChange={e => setForm({...form, fullName: e.target.value})} className="w-full border rounded-xl px-4 py-2 text-sm" />
          <select value={form.role} onChange={e => setForm({...form, role: e.target.value})} className="w-full border rounded-xl px-4 py-2 text-sm">
            <option value="USER">Usuario</option>
            <option value="ADMIN">Administrador</option>
            <option value="OPERATOR">Operador</option>
          </select>
          <button type="submit" className="w-full bg-primary text-white py-2 rounded-xl font-medium">Crear Usuario</button>
        </form>
      )}

      {loading ? <div className="text-center py-8">Cargando...</div> : (
        <div className="grid gap-3">
          {users.map(u => (
            <div key={u.id} className="bg-white rounded-2xl shadow-sm border border-gray-100 p-4">
              <div className="flex items-start justify-between">
                <div>
                  <h3 className="font-semibold text-gray-800">{u.username}</h3>
                  <p className="text-sm text-gray-500">{u.fullName}</p>
                  <span className="text-xs bg-green-100 text-green-700 px-2 py-0.5 rounded">{u.role}</span>
                </div>
                <button onClick={() => handleDelete(u.id)} className="text-red-500 text-sm">Eliminar</button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

// ── Admin Panel (Gestión de Roles) ─────────────────────────────────
function AdminPanel({ user }) {
  const [users, setUsers] = useState([])
  const [loading, setLoading] = useState(true)
  const [changing, setChanging] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => { loadUsers() }, [])

  const loadUsers = async () => {
    try {
      setLoading(true)
      const response = await fetch(`${API_URL}/api/auth/users`, {
        headers: {
          'X-Admin-Email': user.username,
          'Content-Type': 'application/json'
        }
      })
      if (!response.ok) throw new Error('No tienes permisos para ver usuarios')
      const data = await response.json()
      setUsers(data.users || [])
      setError('')
    } catch (err) {
      setError(err.message || 'Error cargando usuarios')
      setUsers([])
    } finally {
      setLoading(false)
    }
  }

  const changeRole = async (userId, newRole) => {
    try {
      setChanging(userId)
      const response = await fetch(`${API_URL}/api/auth/users/${userId}/role`, {
        method: 'PUT',
        headers: {
          'X-Admin-Email': user.username,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ role: newRole })
      })
      if (!response.ok) {
        const errData = await response.json()
        throw new Error(errData.message || 'Error cambiando rol')
      }
      await loadUsers()
    } catch (err) {
      alert('Error: ' + err.message)
    } finally {
      setChanging(null)
    }
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-3">
        <Settings className="text-blue-600" size={24} />
        <h2 className="text-2xl font-bold text-gray-800">Gestión de Roles</h2>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg">
          ⚠️ {error}
        </div>
      )}

      {loading ? (
        <div className="text-center py-12">
          <div className="inline-block animate-spin">⏳</div>
          <p className="text-gray-500 mt-2">Cargando usuarios...</p>
        </div>
      ) : users.length === 0 ? (
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-6 text-center">
          <p className="text-gray-600">No hay usuarios registrados</p>
        </div>
      ) : (
        <div className="grid gap-3">
          {users.map(u => {
            const isCurrentUser = u.username === user.username
            return (
              <div key={u.id} className="bg-white rounded-lg shadow border border-gray-200 p-4 hover:shadow-md transition-shadow">
                <div className="flex items-center justify-between">
                  <div className="flex-1">
                    <h3 className="font-semibold text-gray-800">{u.username}</h3>
                    <p className="text-sm text-gray-500">{u.full_name || 'Sin nombre'}</p>
                  </div>

                  <div className="flex items-center gap-3">
                    <span className={`px-3 py-1 rounded-full text-sm font-medium ${
                      u.role === 'ADMIN' 
                        ? 'bg-red-100 text-red-700' 
                        : 'bg-blue-100 text-blue-700'
                    }`}>
                      {u.role === 'ADMIN' ? '🔴 Administrador' : '🔵 Usuario'}
                    </span>

                    {!isCurrentUser && (
                      <select
                        value={u.role}
                        onChange={(e) => changeRole(u.id, e.target.value)}
                        disabled={changing === u.id}
                        className="px-3 py-1 border border-gray-300 rounded text-sm font-medium cursor-pointer hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
                      >
                        <option value="USER">Usuario</option>
                        <option value="ADMIN">Administrador</option>
                      </select>
                    )}
                    {isCurrentUser && (
                      <span className="text-xs text-gray-500 px-2">Tú</span>
                    )}
                  </div>
                </div>
              </div>
            )
          })}
        </div>
      )}

      <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 text-sm text-gray-700">
        <p><strong>💡 Info:</strong> Cambios de rol toman efecto inmediatamente. Los usuarios con rol ADMIN pueden acceder al panel de administración.</p>
      </div>
    </div>
  )
}
