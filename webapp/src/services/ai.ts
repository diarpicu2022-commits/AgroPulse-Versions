export type AIProvider = 'groq' | 'github' | 'ollama';

export interface AIResponse {
  success: boolean;
  content: string;
  error?: string;
  provider: string;
  model?: string;
}

export class AIService {
  private groqKey: string;
  private githubToken: string;
  private ollamaHost: string;

  constructor() {
    this.groqKey = import.meta.env.VITE_GROQ_KEY || '';
    this.githubToken = import.meta.env.VITE_GITHUB_TOKEN || '';
    this.ollamaHost = import.meta.env.VITE_OLLAMA_HOST || 'http://localhost:11434';
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

  private async callGitHub(prompt: string, systemPrompt?: string): Promise<AIResponse> {
    if (!this.githubToken) {
      return { success: false, content: '', error: 'Token de GitHub no configurado', provider: 'GitHub' };
    }

    try {
      const response = await fetch('https://models.github.ai/inference/chat/completions', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${this.githubToken}`,
          'Content-Type': 'application/json',
          'Accept': 'application/vnd.github+json',
          'X-GitHub-Api-Version': '2022-11-28'
        },
        body: JSON.stringify({
          model: 'openai/gpt-4o-mini',
          messages: [
            ...(systemPrompt ? [{ role: 'system' as const, content: systemPrompt }] : []),
            { role: 'user' as const, content: prompt }
          ],
          max_tokens: 800
        })
      });

      if (response.status === 401) {
        return { success: false, content: '', error: 'Token de GitHub inválido', provider: 'GitHub' };
      }
      if (response.status === 404) {
        return { success: false, content: '', error: 'Modelo no disponible', provider: 'GitHub' };
      }

      const data = await response.json();
      if (data.error) {
        return { success: false, content: '', error: data.error.message || 'Error desconocido', provider: 'GitHub' };
      }

      return {
        success: true,
        content: data.choices?.[0]?.message?.content || 'Sin respuesta',
        provider: 'GitHub',
        model: 'gpt-4o-mini'
      };
    } catch (error) {
      return { success: false, content: '', error: `Error de conexión: ${error}`, provider: 'GitHub' };
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

    const providers: AIProvider[] = ['groq', 'github', 'ollama'];
    const errors: string[] = [];

    for (const provider of providers) {
      let response: AIResponse;

      switch (provider) {
        case 'groq':
          response = await this.callGroq(prompt, systemPrompt);
          break;
        case 'github':
          response = await this.callGitHub(prompt, systemPrompt);
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
    if (this.groqKey) configured.push('groq');
    if (this.githubToken) configured.push('github');
    configured.push('ollama');
    return configured;
  }
}

export const aiService = new AIService();
