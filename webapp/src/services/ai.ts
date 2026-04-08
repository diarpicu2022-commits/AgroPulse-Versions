export type AIProvider = 'openrouter' | 'groq' | 'mistral' | 'ollama';

export interface AIResponse {
  success: boolean;
  content: string;
  error?: string;
  provider: string;
  model?: string;
}

export class AIService {
  private openRouterKey: string;
  private groqKey: string;
  private mistralKey: string;
  private ollamaHost: string;

  constructor() {
    this.openRouterKey = import.meta.env.VITE_OPENROUTER_KEY || '';
    this.groqKey = import.meta.env.VITE_GROQ_KEY || '';
    this.mistralKey = import.meta.env.VITE_MISTRAL_KEY || '';
    this.ollamaHost = import.meta.env.VITE_OLLAMA_HOST || 'http://localhost:11434';
  }

  private async callOpenRouter(prompt: string, systemPrompt?: string): Promise<AIResponse> {
    if (!this.openRouterKey) {
      return { success: false, content: '', error: 'API key de OpenRouter no configurada', provider: 'OpenRouter' };
    }

    try {
      const response = await fetch('https://openrouter.ai/api/v1/chat/completions', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${this.openRouterKey}`,
          'Content-Type': 'application/json',
          'HTTP-Referer': window.location.origin,
          'X-Title': 'AgroPulse'
        },
        body: JSON.stringify({
          model: 'google/gemini-exp-1121:free',
          messages: [
            ...(systemPrompt ? [{ role: 'system' as const, content: systemPrompt }] : []),
            { role: 'user' as const, content: prompt }
          ],
          max_tokens: 800,
          temperature: 0.7
        })
      });

      if (response.status === 401) {
        return { success: false, content: '', error: 'API key de OpenRouter inválida', provider: 'OpenRouter' };
      }
      if (response.status === 404) {
        return { success: false, content: '', error: 'Modelo no disponible. Prueba con otro proveedor.', provider: 'OpenRouter' };
      }

      const data = await response.json();
      if (data.error) {
        return { success: false, content: '', error: data.error.message || 'Error desconocido', provider: 'OpenRouter' };
      }

      return {
        success: true,
        content: data.choices?.[0]?.message?.content || 'Sin respuesta',
        provider: 'OpenRouter',
        model: 'gemini-exp-1121:free'
      };
    } catch (error) {
      return { success: false, content: '', error: `Error de conexión: ${error}`, provider: 'OpenRouter' };
    }
  }

  private async callGroq(prompt: string, systemPrompt?: string): Promise<AIResponse> {
    if (!this.groqKey) {
      return { success: false, content: '', error: 'API key de Groq no configurada', provider: 'Groq' };
    }

    try {
      const response = await fetch('https://api.groq.com/openai/v1/chat/completions', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${this.groqKey}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          model: 'llama-3.1-8b-instant',
          messages: [
            ...(systemPrompt ? [{ role: 'system' as const, content: systemPrompt }] : []),
            { role: 'user' as const, content: prompt }
          ],
          max_tokens: 800
        })
      });

      if (response.status === 401) {
        return { success: false, content: '', error: 'API key de Groq inválida', provider: 'Groq' };
      }

      const data = await response.json();
      if (data.error) {
        return { success: false, content: '', error: data.error.message || 'Error desconocido', provider: 'Groq' };
      }

      return {
        success: true,
        content: data.choices?.[0]?.message?.content || 'Sin respuesta',
        provider: 'Groq',
        model: 'llama-3.1-8b-instant'
      };
    } catch (error) {
      return { success: false, content: '', error: `Error de conexión: ${error}`, provider: 'Groq' };
    }
  }

  private async callMistral(prompt: string, systemPrompt?: string): Promise<AIResponse> {
    if (!this.mistralKey) {
      return { success: false, content: '', error: 'API key de Mistral no configurada', provider: 'Mistral' };
    }

    try {
      const response = await fetch('https://api.mistral.ai/v1/chat/completions', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${this.mistralKey}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          model: 'mistral-small-latest',
          messages: [
            ...(systemPrompt ? [{ role: 'system' as const, content: systemPrompt }] : []),
            { role: 'user' as const, content: prompt }
          ],
          max_tokens: 800
        })
      });

      if (response.status === 401) {
        return { success: false, content: '', error: 'API key de Mistral inválida', provider: 'Mistral' };
      }
      if (response.status === 429) {
        return { success: false, content: '', error: 'Límite de solicitudes alcanzado en Mistral', provider: 'Mistral' };
      }

      const data = await response.json();
      if (data.error) {
        return { success: false, content: '', error: data.error.message || 'Error desconocido', provider: 'Mistral' };
      }

      return {
        success: true,
        content: data.choices?.[0]?.message?.content || 'Sin respuesta',
        provider: 'Mistral',
        model: 'mistral-small-latest'
      };
    } catch (error) {
      return { success: false, content: '', error: `Error de conexión: ${error}`, provider: 'Mistral' };
    }
  }

  private async callOllama(prompt: string, systemPrompt?: string): Promise<AIResponse> {
    try {
      const response = await fetch(`${this.ollamaHost}/api/chat`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          model: 'llama3.1',
          messages: [
            ...(systemPrompt ? [{ role: 'system' as const, content: systemPrompt }] : []),
            { role: 'user' as const, content: prompt }
          ],
          stream: false
        })
      });

      if (!response.ok) {
        return {
          success: false,
          content: '',
          error: `Ollama no disponible en ${this.ollamaHost}.\nInstala desde ollama.com`,
          provider: 'Ollama'
        };
      }

      const data = await response.json();
      return {
        success: true,
        content: data.message?.content || 'Sin respuesta',
        provider: 'Ollama',
        model: 'llama3.1'
      };
    } catch (error) {
      return {
        success: false,
        content: '',
        error: `Ollama no disponible. Instala desde ollama.com`,
        provider: 'Ollama'
      };
    }
  }

  public async query(prompt: string, context?: Record<string, unknown>): Promise<AIResponse> {
    const contextStr = context
      ? Object.entries(context).map(([k, v]) => `- ${k}: ${v}`).join('\n')
      : '';

    const systemPrompt = `Eres AgroPulse IA, un asistente experto en agricultura e invernaderos inteligentes.
Respondes en español de forma clara, concisa y práctica.
${contextStr ? `\n📊 Datos actuales:\n${contextStr}` : ''}`;

    const providers: AIProvider[] = ['openrouter', 'groq', 'mistral', 'ollama'];
    const errors: string[] = [];

    for (const provider of providers) {
      let response: AIResponse;

      switch (provider) {
        case 'openrouter':
          response = await this.callOpenRouter(prompt, systemPrompt);
          break;
        case 'groq':
          response = await this.callGroq(prompt, systemPrompt);
          break;
        case 'mistral':
          response = await this.callMistral(prompt, systemPrompt);
          break;
        case 'ollama':
          response = await this.callOllama(prompt, systemPrompt);
          break;
      }

      if (response.success) {
        return response;
      }

      errors.push(`${provider}: ${response.error}`);
    }

    return {
      success: false,
      content: '',
      error: `Ninguna IA disponible:\n${errors.join('\n')}`,
      provider: 'Ninguna'
    };
  }

  public getConfiguredProviders(): AIProvider[] {
    const configured: AIProvider[] = [];
    if (this.openRouterKey) configured.push('openrouter');
    if (this.groqKey) configured.push('groq');
    if (this.mistralKey) configured.push('mistral');
    configured.push('ollama');
    return configured;
  }
}

export const aiService = new AIService();
