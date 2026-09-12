import type { OllamaMessage, CopilotStatusResponse } from './types';

const OLLAMA_BASE_URL = process.env.OLLAMA_BASE_URL || 'http://localhost:11434';
const OLLAMA_MODEL = process.env.OLLAMA_MODEL || 'llama3.2';

/**
 * Checks the status of the local Ollama service and checks if the configured model is installed.
 */
export async function checkOllamaHealth(): Promise<CopilotStatusResponse> {
  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 3500);

    const res = await fetch(`${OLLAMA_BASE_URL}/api/tags`, {
      method: 'GET',
      headers: { 'Content-Type': 'application/json' },
      signal: controller.signal,
    });

    clearTimeout(timeoutId);

    if (!res.ok) {
      return {
        online: false,
        model: OLLAMA_MODEL,
        availableModels: [],
        error: `Ollama returned HTTP ${res.status}`,
      };
    }

    const data = await res.json();
    const models: string[] = (data?.models || []).map((m: { name: string }) => m.name);
    const modelFound = models.some(
      (m) => m === OLLAMA_MODEL || m.startsWith(`${OLLAMA_MODEL}:`)
    );

    return {
      online: true,
      model: OLLAMA_MODEL,
      availableModels: models,
      error: modelFound ? undefined : `Model '${OLLAMA_MODEL}' not found on local Ollama server.`,
    };
  } catch (err: unknown) {
    const errorMessage = err instanceof Error ? err.message : 'Connection failed';
    return {
      online: false,
      model: OLLAMA_MODEL,
      availableModels: [],
      error: errorMessage,
    };
  }
}

/**
 * Sends a chat completion request to the local Ollama API.
 */
export async function chatWithOllama(params: {
  systemPrompt: string;
  conversation?: OllamaMessage[];
  userMessage: string;
}): Promise<{ content: string; success: boolean; error?: string }> {
  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 45000); // 45s timeout for local LLM inference

    // Prepare message history
    const messages: OllamaMessage[] = [
      { role: 'system', content: params.systemPrompt },
    ];

    if (params.conversation && params.conversation.length > 0) {
      // Keep last 6 turns to manage context window
      const recent = params.conversation.slice(-6);
      for (const msg of recent) {
        if (msg.role === 'user' || msg.role === 'assistant') {
          messages.push({ role: msg.role, content: msg.content });
        }
      }
    }

    messages.push({ role: 'user', content: params.userMessage });

    const res = await fetch(`${OLLAMA_BASE_URL}/api/chat`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        model: OLLAMA_MODEL,
        messages,
        stream: false,
        options: {
          temperature: 0.2, // Low temperature for factual emergency reasoning
        },
      }),
      signal: controller.signal,
    });

    clearTimeout(timeoutId);

    if (!res.ok) {
      const errorJson = await res.json().catch(() => ({}));
      throw new Error(
        errorJson.error || `Ollama responded with status code ${res.status}`
      );
    }

    const data = await res.json();
    const reply = data?.message?.content?.trim() || '';

    if (!reply) {
      throw new Error('Received empty response from local Ollama model');
    }

    return { content: reply, success: true };
  } catch (err: unknown) {
    const errorMessage = err instanceof Error ? err.message : 'Unknown Ollama error';
    console.error('Ollama Chat Error:', errorMessage);
    return {
      content: '',
      success: false,
      error: errorMessage,
    };
  }
}
