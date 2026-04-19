// ═══════════════════════════════════════════════════════════════════
// 🐛 DEBUG - Componente para detectar errores de inicialización
// ═══════════════════════════════════════════════════════════════════

import { useEffect, useState } from 'react'

export function DebugInfo() {
  const [info, setInfo] = useState({
    env: {},
    supabase: null,
    api: null,
    errors: []
  })

  useEffect(() => {
    try {
      // 1. Verificar variables de entorno
      const env = {
        VITE_API_URL: import.meta.env.VITE_API_URL,
        VITE_SUPABASE_URL: import.meta.env.VITE_SUPABASE_URL,
        VITE_SUPABASE_ANON_KEY: import.meta.env.VITE_SUPABASE_ANON_KEY ? '✅ EXISTS' : '❌ MISSING',
        MODE: import.meta.env.MODE,
        DEV: import.meta.env.DEV
      }

      // 2. Verificar Supabase
      let supabaseStatus = '❌ No inicializado'
      try {
        const { createClient } = require('@supabase/supabase-js')
        const url = import.meta.env.VITE_SUPABASE_URL
        const key = import.meta.env.VITE_SUPABASE_ANON_KEY
        if (url && key) {
          const client = createClient(url, key)
          supabaseStatus = '✅ Inicializado'
        }
      } catch (e) {
        supabaseStatus = `❌ ${e.message}`
      }

      // 3. Verificar API Backend
      let apiStatus = '❌ No verificado'
      const apiUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080'
      fetch(apiUrl + '/api/auth/me', { method: 'GET' })
        .then(r => {
          apiStatus = `✅ Backend respondiendo (${r.status})`
          setInfo(prev => ({ ...prev, api: apiStatus }))
        })
        .catch(e => {
          apiStatus = `❌ ${e.message}`
          setInfo(prev => ({ ...prev, api: apiStatus }))
        })

      setInfo({
        env,
        supabase: supabaseStatus,
        api: apiStatus,
        errors: []
      })

      // 4. Capturar errores globales
      const handleError = (event) => {
        console.error('❌ ERROR GLOBAL:', event.error)
        setInfo(prev => ({
          ...prev,
          errors: [...prev.errors, {
            message: event.error?.message || String(event.error),
            stack: event.error?.stack || 'N/A'
          }]
        }))
      }

      window.addEventListener('error', handleError)
      return () => window.removeEventListener('error', handleError)
    } catch (e) {
      console.error('Debug error:', e)
    }
  }, [])

  return (
    <div className="fixed bottom-0 left-0 bg-gray-900 text-white p-4 text-xs max-w-md max-h-96 overflow-auto rounded-tr-lg font-mono z-50 border-t border-r border-green-500">
      <div className="font-bold mb-2 text-green-400">🐛 DEBUG INFO</div>
      
      <div className="mb-2">
        <div className="text-yellow-400">📦 Environment:</div>
        <pre className="bg-gray-800 p-2 rounded text-xs whitespace-pre-wrap">
          {JSON.stringify(info.env, null, 2)}
        </pre>
      </div>

      <div className="mb-2">
        <div className="text-yellow-400">🔷 Supabase:</div>
        <div className="bg-gray-800 p-2 rounded">{info.supabase}</div>
      </div>

      <div className="mb-2">
        <div className="text-yellow-400">🌐 API Backend:</div>
        <div className="bg-gray-800 p-2 rounded">{info.api}</div>
      </div>

      {info.errors.length > 0 && (
        <div className="mb-2">
          <div className="text-red-400">❌ Errors ({info.errors.length}):</div>
          <div className="bg-red-900 p-2 rounded text-red-200 text-xs">
            {info.errors.map((err, i) => (
              <div key={i} className="mb-2 border-b border-red-700 pb-1">
                <div className="font-bold">{err.message}</div>
                <div className="text-red-300 text-xs">{err.stack}</div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
