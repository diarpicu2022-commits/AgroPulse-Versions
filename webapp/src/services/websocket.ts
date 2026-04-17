/**
 * WebSocket Service para Sincronización en Tiempo Real
 * Fallback a polling si WebSocket no está disponible
 */

export interface SensorUpdate {
  sensorId: number
  sensorType: string
  value: number
  timestamp: string
  source: string
}

export interface ActuatorUpdate {
  actuatorId: number
  name: string
  enabled: boolean
  auto_mode: boolean
}

export interface AlertUpdate {
  id: number
  type: string
  message: string
  level: string
  timestamp: string
}

class WebSocketService {
  private ws: WebSocket | null = null
  private url: string
  private messageHandlers: Map<string, Function[]> = new Map()
  private reconnectAttempts = 0
  private maxReconnectAttempts = 5
  private reconnectDelay = 3000
  private isConnecting = false

  constructor(url: string = 'ws://localhost:8080/api/ws') {
    this.url = url
  }

  /**
   * Conectar al WebSocket con reintentos
   */
  connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      if (this.isConnecting) {
        reject(new Error('Connection already in progress'))
        return
      }

      this.isConnecting = true

      try {
        this.ws = new WebSocket(this.url)

        this.ws.onopen = () => {
          console.log('✅ WebSocket conectado')
          this.reconnectAttempts = 0
          this.isConnecting = false
          this.emit('connected')
          resolve()
        }

        this.ws.onmessage = (event) => {
          try {
            const data = JSON.parse(event.data)
            this.handleMessage(data)
          } catch (err) {
            console.error('Error parsing WebSocket message:', err)
          }
        }

        this.ws.onerror = (error) => {
          console.error('❌ WebSocket error:', error)
          this.isConnecting = false
          reject(error)
        }

        this.ws.onclose = () => {
          console.log('WebSocket desconectado, intentando reconectar...')
          this.isConnecting = false
          this.attemptReconnect()
        }
      } catch (err) {
        this.isConnecting = false
        reject(err)
      }
    })
  }

  /**
   * Manejar mensajes recibidos
   */
  private handleMessage(data: any) {
    const { type, payload } = data

    switch (type) {
      case 'sensor_update':
        this.emit('sensor_update', payload)
        break
      case 'actuator_update':
        this.emit('actuator_update', payload)
        break
      case 'alert':
        this.emit('alert', payload)
        break
      case 'reading':
        this.emit('reading', payload)
        break
      default:
        console.warn('Unknown message type:', type)
    }
  }

  /**
   * Enviar mensaje al servidor
   */
  send(type: string, payload: any) {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      console.warn('WebSocket no está conectado')
      return false
    }

    this.ws.send(JSON.stringify({ type, payload, timestamp: new Date().toISOString() }))
    return true
  }

  /**
   * Subscribe a eventos
   */
  on(event: string, callback: Function) {
    if (!this.messageHandlers.has(event)) {
      this.messageHandlers.set(event, [])
    }
    this.messageHandlers.get(event)!.push(callback)
  }

  /**
   * Unsubscribe de eventos
   */
  off(event: string, callback: Function) {
    const handlers = this.messageHandlers.get(event)
    if (handlers) {
      const index = handlers.indexOf(callback)
      if (index > -1) {
        handlers.splice(index, 1)
      }
    }
  }

  /**
   * Emitir evento
   */
  private emit(event: string, data?: any) {
    const handlers = this.messageHandlers.get(event) || []
    handlers.forEach(handler => {
      try {
        handler(data)
      } catch (err) {
        console.error(`Error en handler de ${event}:`, err)
      }
    })
  }

  /**
   * Reconectar con backoff exponencial
   */
  private attemptReconnect() {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('❌ Max reconnection attempts reached, falling back to polling')
      this.emit('fallback_to_polling')
      return
    }

    this.reconnectAttempts++
    const delay = this.reconnectDelay * Math.pow(2, this.reconnectAttempts - 1)
    console.log(`Reconectando en ${delay}ms... (intento ${this.reconnectAttempts}/${this.maxReconnectAttempts})`)

    setTimeout(() => {
      this.connect().catch(() => {
        // Reintento automático en handleClose
      })
    }, delay)
  }

  /**
   * Desconectar
   */
  disconnect() {
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
  }

  /**
   * Verificar si está conectado
   */
  isConnected(): boolean {
    return this.ws !== null && this.ws.readyState === WebSocket.OPEN
  }
}

// Instancia global
export const websocket = new WebSocketService()

/**
 * Service alternativo usando Server-Sent Events (SSE)
 * Más compatible que WebSocket pero unidireccional
 */
export class EventSourceService {
  private eventSource: EventSource | null = null
  private messageHandlers: Map<string, Function[]> = new Map()
  private url: string

  constructor(url: string = 'http://localhost:8080/api/events') {
    this.url = url
  }

  connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      try {
        this.eventSource = new EventSource(this.url)

        this.eventSource.onopen = () => {
          console.log('✅ EventSource conectado')
          this.emit('connected')
          resolve()
        }

        this.eventSource.onerror = (error) => {
          console.error('❌ EventSource error:', error)
          reject(error)
        }

        // Escuchar diferentes tipos de eventos
        this.eventSource.addEventListener('sensor_update', (event) => {
          this.emit('sensor_update', JSON.parse(event.data))
        })

        this.eventSource.addEventListener('actuator_update', (event) => {
          this.emit('actuator_update', JSON.parse(event.data))
        })

        this.eventSource.addEventListener('alert', (event) => {
          this.emit('alert', JSON.parse(event.data))
        })
      } catch (err) {
        reject(err)
      }
    })
  }

  on(event: string, callback: Function) {
    if (!this.messageHandlers.has(event)) {
      this.messageHandlers.set(event, [])
    }
    this.messageHandlers.get(event)!.push(callback)
  }

  off(event: string, callback: Function) {
    const handlers = this.messageHandlers.get(event)
    if (handlers) {
      const index = handlers.indexOf(callback)
      if (index > -1) {
        handlers.splice(index, 1)
      }
    }
  }

  private emit(event: string, data?: any) {
    const handlers = this.messageHandlers.get(event) || []
    handlers.forEach(handler => {
      try {
        handler(data)
      } catch (err) {
        console.error(`Error en handler de ${event}:`, err)
      }
    })
  }

  disconnect() {
    if (this.eventSource) {
      this.eventSource.close()
      this.eventSource = null
    }
  }

  isConnected(): boolean {
    return this.eventSource !== null && this.eventSource.readyState === EventSource.OPEN
  }
}

export const eventSource = new EventSourceService()
