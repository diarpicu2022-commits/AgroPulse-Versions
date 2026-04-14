import { useState, useEffect, createContext, useContext } from 'react'
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts'
import {
  Thermometer, Droplets, Leaf, Bell, Bot, Settings, LogOut,
  Home, Activity, MessageCircle, Send, Cpu, Zap, RefreshCw,
  CheckCircle, XCircle, Info, Key, Wifi, WifiOff,
  Menu, X, ToggleLeft, ToggleRight, Sprout, BarChart3, ChevronRight
} from 'lucide-react'
import { createClient } from '@supabase/supabase-js'
import api from './services/api-client'

// ── Supabase Client ────────────────────────────────────────────────
const supabaseUrl = import.meta.env.VITE_SUPABASE_URL || ''
const supabaseKey = import.meta.env.VITE_SUPABASE_ANON_KEY || ''
const supabase = supabaseUrl && supabaseKey ? createClient(supabaseUrl, supabaseKey) : null

// ── API URL para REST (backend Java) ────────────────────────
const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

// ── IA Services (Groq, GitHub, Ollama) ────────────────────────────────────────────────
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

const getOpenRouterKey = () => {
  return safeGet('agropulse_openrouter_key')
    || import.meta.env.VITE_OPENROUTER_KEY
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

  // Intentar Ollama (local)
  try {
    const ollamaHost = import.meta.env.VITE_OLLAMA_HOST || 'http://localhost:11434'
    const res = await fetch(`${ollamaHost}/api/chat`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        model: 'llama3.1',
        messages: [
          { role: 'system', content: systemPrompt },
          { role: 'user', content: prompt }
        ],
        stream: false
      })
    })
    const data = await res.json()
    if (data.message && data.message.content) {
      return data.message.content
    }
  } catch (err) {
    console.error('Ollama error:', err)
  }

  return '⚠️ No hay servicio de IA disponible. Configura Groq o GitHub en Configuración.'
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

  return (
    <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-4 flex flex-col gap-3">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <div className={`p-2 rounded-xl ${color}`}>
            <Icon size={20} className="text-white" />
          </div>
          <span className="text-sm font-medium text-gray-600">{label}</span>
        </div>
        <span className={`text-xs px-2 py-1 rounded-full font-medium ${
          isOk ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'
        }`}>
          {isOk ? 'Óptimo' : 'Fuera de rango'}
        </span>
      </div>
      <div className="flex items-end gap-1">
        <span className="text-3xl font-bold text-gray-800">
          {value != null ? value.toFixed(1) : '--'}
        </span>
        <span className="text-lg text-gray-500 mb-1">{unit}</span>
      </div>
      {pct != null && (
        <div className="space-y-1">
          <div className="h-2 bg-gray-100 rounded-full overflow-hidden">
            <div
              className={`h-full rounded-full transition-all duration-500 ${
                isOk ? 'bg-green-500' : pct < 0 ? 'bg-blue-400' : 'bg-red-400'
              }`}
              style={{ width: `${Math.max(0, Math.min(100, pct))}%` }}
            />
          </div>
          <div className="flex justify-between text-xs text-gray-400">
            <span>Min: {min}{unit}</span>
            <span>Max: {max}{unit}</span>
          </div>
        </div>
      )}
    </div>
  )
}

// ── Página de Login ──────────────────────────────────────────────
function LoginPage({ onLogin }) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError]       = useState('')
  const [loading, setLoading]   = useState(false)

  const handleLogin = async (e) => {
    e.preventDefault()
    setLoading(true)
    setError('')
    try {
      // Always use REST API first (Supabase is optional)
      const data = await api.auth.login(username, password)
      const email = data.username
      // Use role directly from API - ADMIN gets admin panel, others get user panel
      const role = (data.role === 'ADMIN') ? 'admin' : 'user'
      onLogin({ ...data, email: email, role: role })
    } catch (err) {
      setError('Credenciales incorrectas')
    } finally {
      setLoading(false)
    }
  }

  const handleGoogleLogin = async () => {
    if (!supabase) {
      setError('Supabase no configurado. Usa login con usuario y contraseña.')
      return
    }
    try {
      const { error } = await supabase.auth.signInWithOAuth({
        provider: 'google',
        options: {
          redirectTo: window.location.origin
        }
      })
      if (error) setError('Error con Google: ' + error.message)
    } catch (err) {
      setError('Error de conexión: ' + err.message)
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-green-800 to-green-600 flex items-center justify-center p-4">
      <div className="bg-white rounded-3xl shadow-2xl p-8 w-full max-w-sm">
        <div className="text-center mb-8">
          <div className="text-5xl mb-3">🌿</div>
          <h1 className="text-2xl font-bold text-green-800">AgroPulse</h1>
          <p className="text-gray-500 text-sm mt-1">Sistema de Monitoreo de Invernadero</p>
        </div>
        <form onSubmit={handleLogin} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Usuario</label>
            <input
              type="text" value={username}
              onChange={e => setUsername(e.target.value)}
              className="w-full border border-gray-200 rounded-xl px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-green-500"
              placeholder="admin"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Contraseña</label>
            <input
              type="password" value={password}
              onChange={e => setPassword(e.target.value)}
              className="w-full border border-gray-200 rounded-xl px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-green-500"
              placeholder="••••••••"
            />
          </div>
          {error && <p className="text-red-500 text-sm">{error}</p>}
          <button
            type="submit" disabled={loading}
            className="w-full bg-green-600 hover:bg-green-700 text-white font-semibold py-3 rounded-xl transition-colors disabled:opacity-50"
          >
            {loading ? 'Verificando...' : 'Ingresar'}
          </button>
        </form>

        {/* Divider */}
        <div className="flex items-center gap-3 my-5">
          <div className="flex-1 h-px bg-gray-200" />
          <span className="text-sm text-gray-400">ó</span>
          <div className="flex-1 h-px bg-gray-200" />
        </div>

        {/* Google Login */}
        <button
          onClick={handleGoogleLogin}
          disabled={loading}
          className="w-full flex items-center justify-center gap-3 border-2 border-gray-200 hover:border-green-400 bg-white hover:bg-green-50 text-gray-700 font-medium py-3 rounded-xl transition-all disabled:opacity-50"
        >
          <svg width="20" height="20" viewBox="0 0 48 48">
            <path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"/>
            <path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z"/>
            <path fill="#FBBC05" d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z"/>
            <path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"/>
          </svg>
          Continuar con Google
        </button>

        <p className="text-center text-xs text-gray-400 mt-6">
          Universidad Cooperativa de Colombia · Nariño
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
  const [crop,     setCrop]       = useState(null)
  const [loading,  setLoading]    = useState(true)
  const [lastUpdate, setLastUpdate] = useState(null)

  useEffect(() => {
    loadData()
    const interval = setInterval(loadData, 10000)
    return () => clearInterval(interval)
  }, [])

  const loadData = async () => {
    if (!supabase) { setLoading(false); return }
    try {
      const { data: raw } = await supabase
        .from('sensor_readings')
        .select('*')
        .order('timestamp', { ascending: false })
        .limit(200)

      if (raw) {
        setReadings(raw)
        const tempData = raw
          .filter(r => r.sensor_type === 'TEMPERATURE_INTERNAL')
          .slice(0, 20)
          .reverse()
          .map(r => ({
            time: new Date(r.timestamp).toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit' }),
            temp: parseFloat(r.value.toFixed(1))
          }))
        setHistory(tempData)
      }

      const { data: al } = await supabase
        .from('alerts')
        .select('*')
        .order('created_at', { ascending: false })
        .limit(5)
      if (al) setAlerts(al)

      const { data: crops } = await supabase
        .from('crops')
        .select('*')
        .eq('active', 1)
        .limit(1)
      if (crops && crops.length > 0) setCrop(crops[0])

      setLastUpdate(new Date().toLocaleTimeString('es-CO'))
    } catch (err) {
      console.error('Error cargando datos:', err)
    } finally {
      setLoading(false)
    }
  }

  const latest = (type) => {
    const r = readings.find(r => r.sensor_type === type)
    return r ? r.value : null
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold text-gray-800">
            Hola, {user?.full_name || user?.username} 👋
          </h2>
          <p className="text-sm text-gray-500">
            {crop ? `🌿 Cultivo activo: ${crop.name}` : 'Sin cultivo activo'}
            {lastUpdate && ` · Actualizado ${lastUpdate}`}
          </p>
        </div>
        <button onClick={loadData}
          className="bg-green-100 text-green-700 px-3 py-2 rounded-xl text-sm font-medium">
          🔄 Actualizar
        </button>
      </div>

      {!supabase ? (
        <div className="bg-yellow-50 border border-yellow-200 rounded-2xl p-4 text-sm text-yellow-800">
          <p className="font-semibold">⚠️ Supabase no configurado</p>
          <p className="mt-1">Configura VITE_SUPABASE_ANON_KEY en tu archivo .env para conectar con la base de datos.</p>
        </div>
      ) : loading ? (
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
  const [loading, setLoading]     = useState(true)
  const [showForm, setShowForm]   = useState(false)
  const [form, setForm] = useState({ name: '', type: 'EXTRACTOR', greenhouseId: 1 })

  useEffect(() => { loadActuators() }, [])

  const loadActuators = async () => {
    try {
      const data = await api.actuators.list()
      setActuators(data.actuators || [])
    } catch (err) { console.error(err) }
    setLoading(false)
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    try {
      await api.actuators.create(form)
      setShowForm(false)
      setForm({ name: '', type: 'EXTRACTOR', greenhouseId: 1 })
      loadActuators()
    } catch (err) { alert('Error: ' + err.message) }
  }

  const toggleEnabled = async (id, currentEnabled) => {
    try {
      await api.actuators.update(id, { enabled: !currentEnabled })
      loadActuators()
    } catch (err) { alert('Error: ' + err.message) }
  }

  const handleDelete = async (id) => {
    if (confirm('¿Eliminar?')) {
      try { await api.actuators.delete(id); loadActuators() }
      catch (err) { alert('Error: ' + err.message) }
    }
  }

  const typeInfo = {
    EXTRACTOR:      { emoji: '🌀', label: 'Extractor de Aire' },
    DOOR:           { emoji: '🚪', label: 'Puerta' },
    HEAT_GENERATOR: { emoji: '🔥', label: 'Generador de Calor' },
    WATER_PUMP:     { emoji: '💧', label: 'Bomba de Agua' },
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-bold text-gray-800">⚡ Control de Actuadores</h2>
        <button onClick={() => setShowForm(!showForm)} className="bg-primary text-white px-4 py-2 rounded-xl text-sm font-medium">
          {showForm ? 'Cancelar' : '+ Nuevo'}
        </button>
      </div>

      {showForm && (
        <form onSubmit={handleSubmit} className="bg-white rounded-2xl shadow-sm border border-gray-100 p-4 space-y-3">
          <input type="text" placeholder="Nombre" value={form.name} onChange={e => setForm({...form, name: e.target.value})} className="w-full border rounded-xl px-4 py-2 text-sm" required />
          <select value={form.type} onChange={e => setForm({...form, type: e.target.value})} className="w-full border rounded-xl px-4 py-2 text-sm">
            <option value="EXTRACTOR">Extractor de Aire</option>
            <option value="DOOR">Puerta</option>
            <option value="HEAT_GENERATOR">Generador de Calor</option>
            <option value="WATER_PUMP">Bomba de Agua</option>
          </select>
          <button type="submit" className="w-full bg-primary text-white py-2 rounded-xl font-medium">Guardar</button>
        </form>
      )}

      {loading ? (
        <div className="flex items-center justify-center h-40">
          <div className="animate-spin text-4xl">🌿</div>
        </div>
      ) : actuators.length === 0 ? (
        <div className="text-center py-16 text-gray-400">
          <Zap size={48} className="mx-auto mb-3 opacity-40" />
          <p>No hay actuadores</p>
          <button onClick={() => setShowForm(true)} className="text-primary text-sm mt-2">Crear uno nuevo</button>
        </div>
      ) : (
        <div className="space-y-3">
          {actuators.map(a => {
            const info = typeInfo[a.type] || { emoji: '⚙️', label: a.type }
            const isEnabled = a.enabled === 1 || a.enabled === true
            return (
              <div key={a.id}
                className={`bg-white rounded-2xl shadow-sm border p-4 transition-all ${
                  isEnabled ? 'border-green-200' : 'border-gray-100'
                }`}>
                <div className="flex items-center justify-between mb-3">
                  <div className="flex items-center gap-3">
                    <span className="text-2xl">{info.emoji}</span>
                    <div>
                      <p className="text-sm font-semibold text-gray-800">{a.name || info.label}</p>
                      <p className="text-xs text-gray-500">{info.label}</p>
                    </div>
                  </div>
                  <span className={`text-xs px-2 py-1 rounded-full font-medium ${
                    isEnabled ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'
                  }`}>
                    {isEnabled ? 'Encendido' : 'Apagado'}
                  </span>
                </div>

                <div className="flex gap-2">
                  <button
                    onClick={() => toggleField(a.id, 'enabled', a.enabled)}
                    className={`flex-1 flex items-center justify-center gap-2 py-2 rounded-xl text-xs font-medium transition-all ${
                      isEnabled
                        ? 'bg-green-600 text-white hover:bg-green-700'
                        : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                    }`}>
                    {isEnabled ? <ToggleRight size={16} /> : <ToggleLeft size={16} />}
                    {isEnabled ? 'Encendido' : 'Apagado'}
                  </button>
                  <button
                    onClick={() => toggleField(a.id, 'auto_mode', a.auto_mode)}
                    className={`flex-1 flex items-center justify-center gap-2 py-2 rounded-xl text-xs font-medium transition-all ${
                      a.auto_mode
                        ? 'bg-blue-600 text-white hover:bg-blue-700'
                        : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                    }`}>
                    <Cpu size={14} />
                    {a.auto_mode ? 'Automático' : 'Manual'}
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
  const [form, setForm] = useState({
    name: '', variety: '',
    temp_min: 15, temp_max: 25,
    humidity_min: 50, humidity_max: 70,
    soil_moisture_min: 40, soil_moisture_max: 60
  })

  useEffect(() => { loadCrops() }, [])

  const loadCrops = async () => {
    try {
      const data = await api.crops.list()
      setCrops(data.crops || [])
    } catch (err) { console.error(err) }
    setLoading(false)
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    try {
      await api.crops.create(form)
      setShowForm(false)
      setForm({ name: '', variety: '', temp_min: 15, temp_max: 25, humidity_min: 50, humidity_max: 70, soil_moisture_min: 40, soil_moisture_max: 60 })
      loadCrops()
    } catch (err) { alert('Error: ' + err.message) }
  }

  const handleDelete = async (id) => {
    if (confirm('¿Eliminar?')) {
      try { await api.crops.delete(id); loadCrops() }
      catch (err) { alert('Error: ' + err.message) }
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
        <h2 className="text-xl font-bold text-gray-800">🌿 Cultivos</h2>
        <button onClick={() => setShowForm(!showForm)} className="bg-primary text-white px-4 py-2 rounded-xl text-sm font-medium">
          {showForm ? 'Cancelar' : '+ Nuevo'}
        </button>
      </div>

      {showForm && (
        <form onSubmit={handleSubmit} className="bg-white rounded-2xl shadow-sm border border-gray-100 p-4 space-y-3">
          <div className="grid grid-cols-2 gap-3">
            <input type="text" placeholder="Nombre" value={form.name} onChange={e => setForm({...form, name: e.target.value})} className="border rounded-xl px-4 py-2 text-sm" required />
            <input type="text" placeholder="Variedad" value={form.variety} onChange={e => setForm({...form, variety: e.target.value})} className="border rounded-xl px-4 py-2 text-sm" />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <input type="number" placeholder="Temp min" value={form.temp_min} onChange={e => setForm({...form, temp_min: +e.target.value})} className="border rounded-xl px-4 py-2 text-sm" />
            <input type="number" placeholder="Temp max" value={form.temp_max} onChange={e => setForm({...form, temp_max: +e.target.value})} className="border rounded-xl px-4 py-2 text-sm" />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <input type="number" placeholder="Humedad min" value={form.humidity_min} onChange={e => setForm({...form, humidity_min: +e.target.value})} className="border rounded-xl px-4 py-2 text-sm" />
            <input type="number" placeholder="Humedad max" value={form.humidity_max} onChange={e => setForm({...form, humidity_max: +e.target.value})} className="border rounded-xl px-4 py-2 text-sm" />
          </div>
          <button type="submit" className="w-full bg-primary text-white py-2 rounded-xl font-medium">Guardar</button>
        </form>
      )}

      {loading ? (
        <div className="flex items-center justify-center h-40">
          <div className="animate-spin text-4xl">🌿</div>
        </div>
      ) : crops.length === 0 ? (
        <div className="text-center py-16 text-gray-400">
          <Sprout size={48} className="mx-auto mb-3 opacity-40" />
          <p>No hay cultivos</p>
          <button onClick={() => setShowForm(true)} className="text-primary text-sm mt-2">Crear uno nuevo</button>
        </div>
      ) : (
        <div className="space-y-4">
          {crops.map(c => {
            const isActive = c.active === 1 || c.active === true
            return (
              <div key={c.id}
                className={`bg-white rounded-2xl shadow-sm border p-4 ${
                  isActive ? 'border-green-200' : 'border-gray-100'
                }`}>
                <div className="flex items-center justify-between mb-4">
                  <div className="flex items-center gap-2">
                    <span className="text-xl">{isActive ? '🌿' : '🍂'}</span>
                    <div>
                      <p className="text-sm font-semibold text-gray-800">{c.name}</p>
                      {c.variety && <p className="text-xs text-gray-500">{c.variety}</p>}
                    </div>
                  </div>
                  <span className={`text-xs px-2 py-1 rounded-full font-medium ${
                    isActive ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'
                  }`}>
                    {isActive ? 'Activo' : 'Inactivo'}
                  </span>
                </div>

                <div className="space-y-3">
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
  const [readings, setReadings]   = useState([])

  useEffect(() => {
    loadLatestReadings()
  }, [])

  const loadLatestReadings = async () => {
    try {
      const data = await api.readings.list(1, 20)
      setReadings(data.readings || [])
    } catch (err) {
      console.error('Error cargando lecturas:', err)
      setReadings([])
    }
  }

  const buildSensorContext = () => {
    if (readings.length === 0) return ''
    const byType = {}
    readings.forEach(r => {
      if (!byType[r.sensor_type]) byType[r.sensor_type] = r
    })
    let ctx = 'Lecturas actuales de sensores:\n'
    for (const [type, r] of Object.entries(byType)) {
      ctx += `- ${type}: ${r.value.toFixed(1)} (${new Date(r.timestamp).toLocaleTimeString('es-CO')})\n`
    }
    return ctx
  }

  const sendPrompt = async (text) => {
    const question = text || prompt
    if (!question.trim()) return
    setLoading(true)
    setResponse('')
    const result = await callAI(question, buildSensorContext())
    setResponse(result)
    setLoading(false)
  }

  const presets = [
    { label: '📊 Analizar estado', prompt: 'Analiza el estado actual del invernadero basándote en las lecturas de sensores. Identifica problemas y sugiere acciones correctivas.' },
    { label: '🌱 Recomendar cultivo', prompt: 'Dame recomendaciones para mejorar el cuidado del cultivo actual basándote en las condiciones del invernadero.' },
    { label: '⚙️ Predecir actuadores', prompt: 'Basándote en las lecturas actuales, predice qué actuadores será necesario activar en las próximas horas y por qué.' },
  ]

  return (
    <div className="space-y-4">
      <h2 className="text-xl font-bold text-gray-800">🤖 Consultar IA</h2>

      {/* Estado de conexión */}
      <div className={`flex items-center gap-2 text-xs px-3 py-2 rounded-xl ${
        getOpenRouterKey() ? 'bg-green-50 text-green-700' : 'bg-yellow-50 text-yellow-700'
      }`}>
        {getOpenRouterKey() ? (
          <><CheckCircle size={14} /> IA conectada (OpenRouter - Gemini Experimental Gratis)</>
        ) : (
          <><XCircle size={14} /> Sin clave de IA. Ve a Configuración para ingresarla.</>
        )}
      </div>

      {/* Botones predefinidos */}
      <div className="grid grid-cols-3 gap-2">
        {presets.map((p, i) => (
          <button key={i}
            onClick={() => sendPrompt(p.prompt)}
            disabled={loading}
            className="bg-white border border-gray-200 hover:border-green-400 hover:bg-green-50 rounded-xl p-3 text-xs font-medium text-gray-700 transition-all disabled:opacity-50"
          >
            {p.label}
          </button>
        ))}
      </div>

      {/* Prompt libre */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-4">
        <textarea
          value={prompt}
          onChange={e => setPrompt(e.target.value)}
          rows={3}
          className="w-full border border-gray-200 rounded-xl px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-green-500 resize-none"
          placeholder="Escribe tu pregunta sobre el invernadero..."
        />
        <button
          onClick={() => sendPrompt()}
          disabled={loading || !prompt.trim()}
          className="mt-2 w-full bg-green-600 hover:bg-green-700 text-white py-2.5 rounded-xl text-sm font-medium flex items-center justify-center gap-2 disabled:opacity-50 transition-colors"
        >
          {loading ? (
            <><div className="animate-spin text-lg">🌿</div> Consultando IA...</>
          ) : (
            <><Send size={16} /> Enviar consulta</>
          )}
        </button>
      </div>

      {/* Respuesta de la IA */}
      {(response || loading) && (
        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-4">
          <div className="flex items-center gap-2 mb-3">
            <div className="p-1.5 bg-green-100 rounded-lg">
              <Bot size={16} className="text-green-700" />
            </div>
            <span className="text-sm font-semibold text-gray-700">Respuesta de AgroPulse IA</span>
          </div>
          {loading ? (
            <div className="flex items-center gap-2 text-sm text-gray-500">
              <div className="animate-spin text-xl">🌿</div>
              Analizando datos del invernadero...
            </div>
          ) : (
            <div className="text-sm text-gray-700 whitespace-pre-wrap leading-relaxed">
              {response}
            </div>
          )}
        </div>
      )}

      {/* Info de contexto */}
      {readings.length > 0 && (
        <p className="text-xs text-gray-400 text-center">
          La IA utiliza datos de {readings.length} lecturas recientes de sensores como contexto.
        </p>
      )}
    </div>
  )
}

// ── Página ML Predicciones ───────────────────────────────────────
function MLPage() {
  const [prediction, setPrediction] = useState('')
  const [loading, setLoading]       = useState(false)
  const [readings, setReadings]     = useState([])

  useEffect(() => { loadReadings() }, [])

  const loadReadings = async () => {
    try {
      const data = await api.readings.list(1, 30)
      setReadings(data.readings || [])
    } catch (err) { 
      console.error(err)
      setReadings([])
    }
  }

  const buildContext = () => {
    if (readings.length === 0) return ''
    const byType = {}
    readings.forEach(r => {
      if (!byType[r.sensor_type]) byType[r.sensor_type] = []
      byType[r.sensor_type].push(r)
    })
    let ctx = 'Historial reciente de sensores (últimas lecturas):\n'
    for (const [type, items] of Object.entries(byType)) {
      const values = items.slice(0, 10).map(r => r.value.toFixed(1)).join(', ')
      ctx += `- ${type}: [${values}]\n`
    }
    return ctx
  }

  const predict = async () => {
    setLoading(true)
    setPrediction('')
    const ctx = buildContext()
    const prompt = `Basándote en el historial reciente de sensores del invernadero, predice los valores de cada sensor para las próximas 6 horas (en intervalos de 1 hora).

Presenta los resultados en un formato claro con:
- Hora estimada
- Valores predichos para cada sensor
- Tendencia (subiendo, bajando, estable)
- Acciones recomendadas si algún valor saldrá de rango

Sé conciso y práctico.`

    const result = await callAI(prompt, ctx)
    setPrediction(result)
    setLoading(false)
  }

  return (
    <div className="space-y-4">
      <h2 className="text-xl font-bold text-gray-800">📈 ML — Predicciones</h2>

      <div className={`flex items-center gap-2 text-xs px-3 py-2 rounded-xl ${
        getOpenRouterKey() ? 'bg-green-50 text-green-700' : 'bg-yellow-50 text-yellow-700'
      }`}>
        {getOpenRouterKey() ? (
          <><CheckCircle size={14} /> Motor de predicción activo (Gemini Experimental)</>
        ) : (
          <><XCircle size={14} /> Configura la clave de IA para usar predicciones.</>
        )}
      </div>

      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-4">
        <p className="text-sm text-gray-600 mb-3">
          Utiliza inteligencia artificial para predecir los valores futuros de los sensores basándose en el historial reciente.
        </p>
        <button
          onClick={predict}
          disabled={loading}
          className="w-full bg-green-600 hover:bg-green-700 text-white py-3 rounded-xl text-sm font-medium flex items-center justify-center gap-2 disabled:opacity-50 transition-colors"
        >
          {loading ? (
            <><div className="animate-spin text-lg">🌿</div> Calculando predicción...</>
          ) : (
            <><BarChart3 size={16} /> Predecir próximas 6 horas</>
          )}
        </button>
      </div>

      {readings.length > 0 && !prediction && !loading && (
        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-4">
          <h3 className="text-sm font-semibold text-gray-700 mb-2">📡 Datos disponibles para predicción</h3>
          <p className="text-xs text-gray-500">{readings.length} lecturas recientes cargadas como contexto.</p>
        </div>
      )}

      {(prediction || loading) && (
        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-4">
          <div className="flex items-center gap-2 mb-3">
            <div className="p-1.5 bg-purple-100 rounded-lg">
              <Cpu size={16} className="text-purple-700" />
            </div>
            <span className="text-sm font-semibold text-gray-700">Predicción AgroPulse ML</span>
          </div>
          {loading ? (
            <div className="flex items-center gap-2 text-sm text-gray-500">
              <div className="animate-spin text-xl">🌿</div>
              Analizando tendencias y calculando predicción...
            </div>
          ) : (
            <div className="text-sm text-gray-700 whitespace-pre-wrap leading-relaxed">
              {prediction}
            </div>
          )}
        </div>
      )}
    </div>
  )
}

// ── Página de Tickets de Soporte ──────────────────────────────────
function SupportPage() {
  const [subject, setSubject] = useState('')
  const [description, setDesc] = useState('')
  const [loading, setLoading] = useState(false)
  const [sent, setSent] = useState(false)

  const submitTicket = async (e) => {
    e.preventDefault()
    if (!subject.trim() || !description.trim()) return
    setLoading(true)
    setTimeout(() => {
      setSubject(''); setDesc('')
      setLoading(false)
      setSent(true)
      setTimeout(() => setSent(false), 3000)
    }, 1000)
  }

  return (
    <div className="space-y-6">
      <h2 className="text-xl font-bold text-gray-800">🎧 Soporte Técnico</h2>

      {sent && (
        <div className="bg-green-100 border border-green-200 text-green-700 px-4 py-3 rounded-xl">
          ✅ Mensaje enviado. Te contactaremos pronto.
        </div>
      )}

      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-4">
        <h3 className="font-semibold text-gray-700 mb-3">📩 Contactar Soporte</h3>
        <form onSubmit={submitTicket} className="space-y-3">
          <input
            value={subject} onChange={e => setSubject(e.target.value)}
            className="w-full border border-gray-200 rounded-xl px-3 py-2 text-sm"
            placeholder="Asunto del problema..."
          />
          <textarea
            value={description} onChange={e => setDesc(e.target.value)}
            rows={3}
            className="w-full border border-gray-200 rounded-xl px-3 py-2 text-sm"
            placeholder="Describe el problema en detalle..."
          />
          <button type="submit" disabled={loading}
            className="w-full bg-green-600 text-white py-2 rounded-xl text-sm font-medium disabled:opacity-50">
            {loading ? 'Enviando...' : '📨 Enviar Ticket'}
          </button>
        </form>
      </div>

      <div className="space-y-3">
        {tickets.map(t => (
          <div key={t.id} className="bg-white rounded-2xl shadow-sm border border-gray-100 p-4">
            <div className="flex items-center justify-between mb-2">
              <span className="font-medium text-sm text-gray-800">{t.subject}</span>
              <span className={`text-xs px-2 py-1 rounded-full font-medium ${statusColors[t.status] || 'bg-gray-100'}`}>
                {t.status}
              </span>
            </div>
            <p className="text-xs text-gray-500">{t.description}</p>
            {t.admin_response && (
              <div className="mt-2 p-2 bg-green-50 rounded-xl">
                <p className="text-xs font-medium text-green-700">Respuesta del admin:</p>
                <p className="text-xs text-green-600">{t.admin_response}</p>
              </div>
            )}
          </div>
        ))}
        {tickets.length === 0 && (
          <p className="text-center text-gray-400 text-sm py-8">No tienes tickets aún.</p>
        )}
      </div>
    </div>
  )
}

// ── Página de Configuración ──────────────────────────────────────
function SettingsPage() {
  const [orKey, setOrKey]           = useState(safeGet('agropulse_openrouter_key') || '')
  const [saved, setSaved]           = useState(false)

  const saveKey = () => {
    if (orKey.trim()) {
      safeSet('agropulse_openrouter_key', orKey.trim())
    } else {
      safeRemove('agropulse_openrouter_key')
    }
    setSaved(true)
    setTimeout(() => setSaved(false), 2000)
  }

  const hasEnvKey = !!import.meta.env.VITE_OPENROUTER_KEY
  const activeKey = getOpenRouterKey()

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
            <span className="font-medium text-gray-800">2025.03</span>
          </div>
          <div className="flex justify-between">
            <span className="text-gray-500">WebApp</span>
            <span className="font-medium text-gray-800">React + Vite</span>
          </div>
          <div className="flex justify-between">
            <span className="text-gray-500">Usuario</span>
            <span className="font-medium text-gray-800">{user?.full_name || user?.username}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-gray-500">Rol</span>
            <span className="font-medium text-gray-800">{user?.role || 'OPERATOR'}</span>
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
            <span className="text-gray-500">IA (OpenRouter)</span>
            <span className={`flex items-center gap-1 text-xs font-medium px-2 py-0.5 rounded-full ${
              activeKey ? 'bg-green-100 text-green-700' : 'bg-yellow-100 text-yellow-700'
            }`}>
              {activeKey ? <><CheckCircle size={12} /> Configurada</> : <><XCircle size={12} /> Sin clave</>}
            </span>
          </div>
        </div>
      </div>

      {/* Configurar API Key */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-4">
        <h3 className="text-sm font-semibold text-gray-700 mb-3">
          <Key size={14} className="inline mr-1" /> Clave de IA (OpenRouter)
        </h3>
        <p className="text-xs text-gray-500 mb-3">
          Obtén tu clave gratuita en <span className="font-semibold">openrouter.ai</span> → Sign up → API Keys.
          Incluye acceso a Gemini 2.0 Flash y LLaMA-70B gratis.
        </p>
        {hasEnvKey && (
          <p className="text-xs text-green-600 mb-2">
            ✅ Se detectó una clave en las variables de entorno (.env).
          </p>
        )}
        <input
          type="password"
          value={orKey}
          onChange={e => setOrKey(e.target.value)}
          className="w-full border border-gray-200 rounded-xl px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-green-500"
          placeholder="sk-or-v1-..."
        />
        <button
          onClick={saveKey}
          className="mt-2 w-full bg-green-600 hover:bg-green-700 text-white py-2 rounded-xl text-sm font-medium transition-colors"
        >
          {saved ? '✅ Guardada' : '💾 Guardar clave'}
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
            <span className="font-medium text-gray-800">Leider Cadena</span>
          </div>
          <div className="flex justify-between">
            <span className="text-gray-500">Universidad</span>
            <span className="font-medium text-gray-800 text-right">Cooperativa de Colombia - Nariño</span>
          </div>
          <div className="flex justify-between">
            <span className="text-gray-500">Proyecto</span>
            <span className="font-medium text-gray-800">Grado 2025</span>
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
        const isAdmin = userEmail === 'diarpicu2025@gmail.com' || 
                       userEmail === 'diarpicu2022@gmail.com' ||
                       userEmail.includes('admin')
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
    { id: 'sensors',    label: 'Sensores',     icon: Activity },
    { id: 'actuators', label: 'Actuadores',   icon: Zap },
    { id: 'greenhouses',label: 'Invernadero', icon: Sprout },
    { id: 'crops',     label: 'Cultivos',     icon: Leaf },
    { id: 'simulate',  label: 'Simular',     icon: RefreshCw },
    { id: 'ai',       label: 'IA',         icon: Bot },
    { id: 'ml',       label: 'ML',         icon: Cpu },
    { id: 'alerts',   label: 'Alertas',     icon: Bell },
    { id: 'logs',     label: 'Logs',        icon: Activity },
    { id: 'users',    label: 'Usuarios',   icon: Key },
    { id: 'support',  label: 'Soporte',    icon: MessageCircle },
    { id: 'settings', label: 'Config',     icon: Settings },
  ] : [
    { id: 'dashboard', label: 'Inicio',    icon: Home },
    { id: 'sensors',   label: 'Sensores',  icon: Activity },
    { id: 'actuators', label: 'Actuadores',icon: Zap },
    { id: 'crops',     label: 'Cultivos',  icon: Leaf },
    { id: 'alerts',   label: 'Alertas',  icon: Bell },
    { id: 'support',  label: 'Soporte',  icon: MessageCircle },
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

        {/* Sidebar Menu */}
        <div className={`fixed top-0 left-0 h-full w-64 bg-white shadow-xl z-40 transform transition-transform duration-300 ease-in-out ${
          sidebarOpen ? 'translate-x-0' : '-translate-x-full'
        } lg:block lg:translate-x-0`}>
          {/* Sidebar Header */}
          <div className="bg-green-700 text-white px-5 py-4 flex items-center justify-between">
            <div>
              <div className="flex items-center gap-2">
                <span className="text-lg font-bold">🌿 AgroPulse</span>
                <span className="text-xs opacity-60 bg-green-600 px-1.5 py-0.5 rounded">v6.0</span>
              </div>
              <p className="text-xs opacity-70 mt-1">{user.full_name || user.username}</p>
            </div>
            <button onClick={() => setSidebarOpen(false)}
              className="p-1 hover:bg-green-600 rounded-lg transition-colors lg:hidden">
              <X size={20} />
            </button>
          </div>

          {/* Sidebar Nav Items */}
          <div className="py-2 overflow-y-auto" style={{ maxHeight: 'calc(100vh - 72px)' }}>
            {navItems.map(item => {
              const Icon = item.icon
              const active = page === item.id
              return (
                <button key={item.id}
                  onClick={() => navigate(item.id)}
                  className={`w-full flex items-center gap-3 px-5 py-3 text-sm transition-colors ${
                    active
                      ? 'bg-green-50 text-green-700 font-semibold border-r-4 border-green-600'
                      : 'text-gray-600 hover:bg-gray-50'
                  }`}>
                  <Icon size={20} />
                  <span className="flex-1 text-left">{item.label}</span>
                  {active && <ChevronRight size={16} className="text-green-400" />}
                </button>
              )
            })}

            {/* Logout in sidebar */}
            <div className="border-t border-gray-100 mt-2 pt-2">
              <button
                onClick={handleLogout}
                className="w-full flex items-center gap-3 px-5 py-3 text-sm text-red-600 hover:bg-red-50 transition-colors">
                <LogOut size={20} />
                <span>Cerrar sesión</span>
              </button>
            </div>
          </div>
        </div>

        {/* Header */}
        <header className="bg-green-700 text-white px-4 py-3 flex items-center justify-between sticky top-0 z-20 shadow lg:ml-64">
          <div className="flex items-center gap-3">
            <button onClick={() => setSidebarOpen(true)}
              className="p-1.5 hover:bg-green-600 rounded-lg transition-colors lg:hidden">
              <Menu size={20} />
            </button>
            <span className="text-lg font-bold">🌿 AgroPulse</span>
            <span className="text-xs opacity-60 bg-green-600 px-1.5 py-0.5 rounded">v6.0</span>
          </div>
          <div className="flex items-center gap-3">
            <span className="text-xs opacity-80">{user.full_name || user.username}</span>
          </div>
        </header>

        {/* Contenido */}
        <main className="px-4 py-4 pb-6 lg:px-8 lg:ml-64">
          {page === 'dashboard'   && <Dashboard />}
          {page === 'sensors'    && <SensorsPage />}
          {page === 'actuators'  && <ActuatorsPage />}
          {page === 'greenhouses' && <GreenhousePage />}
          {page === 'crops'      && <CropsPage />}
          {page === 'simulate'   && <SimulationPage />}
          {page === 'ai'         && <AIPage />}
          {page === 'ml'        && <MLPage />}
          {page === 'alerts'    && <AlertsPage />}
          {page === 'logs'       && <LogsPage />}
          {page === 'users'      && <UsersPage />}
          {page === 'support'    && <SupportPage />}
          {page === 'settings'   && <SettingsPage />}
        </main>
      </div>
    </AuthContext.Provider>
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

  useEffect(() => { loadLogs() }, [])

  const loadLogs = async () => {
    try {
      const data = await api.logs.list(50)
      setLogs(data.logs || [])
    } catch (err) { console.error(err) }
    setLoading(false)
  }

  return (
    <div className="space-y-4">
      <h2 className="text-xl font-bold text-gray-800">📋 Logs del Sistema</h2>
      {loading ? <div className="text-center py-8">Cargando...</div> : logs.length === 0 ? (
        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-8 text-center">
          <p className="text-gray-500">No hay logs</p>
        </div>
      ) : (
        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 divide-y max-h-96 overflow-y-auto">
          {logs.map(l => (
            <div key={l.id} className="p-3 text-sm">
              <span className="text-gray-400 text-xs">{l.timestamp}</span>
              <p className="text-gray-800"><span className="font-medium">{l.action}</span>: {l.details}</p>
              <p className="text-gray-400 text-xs">Por: {l.performedBy}</p>
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
