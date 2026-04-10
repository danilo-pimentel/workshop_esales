import { useState } from 'react';
import type { HttpMethod, HeaderPair } from '../types';
import { getToken } from '../api/client';

const BASE_URL = 'http://localhost:4000';

const METHODS: HttpMethod[] = ['GET', 'POST', 'PUT', 'PATCH', 'DELETE'];

// Quick-add header templates
const QUICK_HEADERS: { label: string; key: string; value: string }[] = [
  { label: 'Content-Type JSON', key: 'Content-Type', value: 'application/json' },
  { label: 'Authorization (auto)', key: 'Authorization', value: `Bearer ${getToken() ?? '<token>'}` },
  { label: 'Accept JSON', key: 'Accept', value: 'application/json' },
];

function syntaxHighlight(raw: string): string {
  if (!raw) return '';
  let html = raw
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');

  try {
    const parsed = JSON.parse(raw);
    html = JSON.stringify(parsed, null, 2)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;');
  } catch {
    // leave as-is
  }

  return html
    .replace(/"([^"\\]*(\\.[^"\\]*)*)"/g, (m) => `<span class="token-string">${m}</span>`)
    .replace(/\b(-?\d+\.?\d*)\b/g, '<span class="token-number">$1</span>')
    .replace(/\b(true|false)\b/g, '<span class="token-bool">$1</span>')
    .replace(/\bnull\b/g, '<span class="token-null">null</span>');
}

interface ResponseState {
  status: number;
  statusText: string;
  body: string;
  headers: Record<string, string>;
  duration: number;
}

export default function CustomRequest() {
  const [method, setMethod] = useState<HttpMethod>('GET');
  const [url, setUrl] = useState(`${BASE_URL}/api/`);
  const [headers, setHeaders] = useState<HeaderPair[]>([
    { key: 'Content-Type', value: 'application/json' },
  ]);
  const [body, setBody] = useState('');
  const [loading, setLoading] = useState(false);
  const [response, setResponse] = useState<ResponseState | null>(null);
  const [sendError, setSendError] = useState<string | null>(null);

  // ── Header management ────────────────────────────────────────────────────

  function addHeader() {
    setHeaders((h) => [...h, { key: '', value: '' }]);
  }

  function updateHeader(i: number, field: 'key' | 'value', val: string) {
    setHeaders((h) => h.map((row, idx) => idx === i ? { ...row, [field]: val } : row));
  }

  function removeHeader(i: number) {
    setHeaders((h) => h.filter((_, idx) => idx !== i));
  }

  function addQuickHeader(qh: typeof QUICK_HEADERS[0]) {
    setHeaders((h) => {
      const exists = h.findIndex((r) => r.key.toLowerCase() === qh.key.toLowerCase());
      if (exists >= 0) {
        return h.map((r, i) => i === exists ? { key: qh.key, value: qh.value } : r);
      }
      return [...h, { key: qh.key, value: qh.value }];
    });
  }

  // ── Send ─────────────────────────────────────────────────────────────────

  async function handleSend() {
    setLoading(true);
    setSendError(null);
    setResponse(null);

    const headerRecord: Record<string, string> = {};
    headers.forEach(({ key, value }) => {
      if (key.trim()) headerRecord[key.trim()] = value;
    });

    const init: RequestInit = {
      method,
      headers: headerRecord,
    };

    if (method !== 'GET' && method !== 'DELETE' && body.trim()) {
      init.body = body.trim();
    }

    const start = Date.now();
    try {
      const res = await fetch(url, init);
      const duration = Date.now() - start;
      const text = await res.text();
      const resHeaders: Record<string, string> = {};
      res.headers.forEach((v, k) => { resHeaders[k] = v; });
      setResponse({ status: res.status, statusText: res.statusText, body: text, headers: resHeaders, duration });
    } catch (err) {
      setSendError(err instanceof Error ? err.message : String(err));
    } finally {
      setLoading(false);
    }
  }

  const statusColor =
    response === null ? '' :
    response.status >= 500 ? 'text-red-400' :
    response.status >= 400 ? 'text-yellow-400' :
    'text-green-400';

  return (
    <div className="flex flex-col h-full overflow-y-auto devtools-scrollbar p-3 space-y-4">
      {/* Method + URL */}
      <div className="flex gap-2">
        <select
          value={method}
          onChange={(e) => setMethod(e.target.value as HttpMethod)}
          className="bg-gray-800 text-blue-400 font-mono text-sm border border-gray-700 rounded px-2 py-1.5 focus:outline-none focus:ring-1 focus:ring-blue-600"
        >
          {METHODS.map((m) => (
            <option key={m} value={m}>{m}</option>
          ))}
        </select>

        <input
          type="text"
          value={url}
          onChange={(e) => setUrl(e.target.value)}
          className="flex-1 bg-gray-800 text-gray-200 font-mono text-sm border border-gray-700 rounded px-3 py-1.5 focus:outline-none focus:ring-1 focus:ring-blue-600"
          placeholder="http://localhost:4000/api/..."
        />

        <button
          onClick={handleSend}
          disabled={loading}
          className="bg-blue-600 hover:bg-blue-500 disabled:bg-blue-900 text-white font-bold text-sm px-4 py-1.5 rounded transition-colors"
        >
          {loading ? '…' : 'Send'}
        </button>
      </div>

      {/* Quick header buttons */}
      <div>
        <p className="text-[10px] text-gray-500 uppercase tracking-widest mb-1.5">Quick Headers</p>
        <div className="flex flex-wrap gap-1.5">
          {QUICK_HEADERS.map((qh) => (
            <button
              key={qh.label}
              onClick={() => addQuickHeader(qh)}
              className="text-[10px] bg-gray-800 hover:bg-gray-700 border border-gray-700 text-gray-300 px-2 py-1 rounded font-mono transition-colors"
            >
              + {qh.label}
            </button>
          ))}
        </div>
      </div>

      {/* Headers editor */}
      <div>
        <div className="flex items-center justify-between mb-1.5">
          <p className="text-[10px] text-gray-500 uppercase tracking-widest">Headers</p>
          <button
            onClick={addHeader}
            className="text-[10px] text-blue-400 hover:text-blue-300"
          >
            + Adicionar
          </button>
        </div>
        <div className="space-y-1.5">
          {headers.map((h, i) => (
            <div key={i} className="flex gap-1.5">
              <input
                type="text"
                value={h.key}
                onChange={(e) => updateHeader(i, 'key', e.target.value)}
                placeholder="Header-Name"
                className="w-40 bg-gray-800 text-purple-300 font-mono text-xs border border-gray-700 rounded px-2 py-1 focus:outline-none focus:ring-1 focus:ring-blue-600"
              />
              <input
                type="text"
                value={h.value}
                onChange={(e) => updateHeader(i, 'value', e.target.value)}
                placeholder="value"
                className="flex-1 bg-gray-800 text-gray-300 font-mono text-xs border border-gray-700 rounded px-2 py-1 focus:outline-none focus:ring-1 focus:ring-blue-600"
              />
              <button
                onClick={() => removeHeader(i)}
                className="text-gray-600 hover:text-red-400 text-xs px-1"
              >
                ✕
              </button>
            </div>
          ))}
        </div>
      </div>

      {/* Body editor */}
      {method !== 'GET' && method !== 'DELETE' && (
        <div>
          <p className="text-[10px] text-gray-500 uppercase tracking-widest mb-1.5">Body (JSON)</p>
          <textarea
            value={body}
            onChange={(e) => setBody(e.target.value)}
            rows={6}
            placeholder='{"key": "value"}'
            className="w-full bg-gray-800 text-green-300 font-mono text-xs border border-gray-700 rounded px-3 py-2 focus:outline-none focus:ring-1 focus:ring-blue-600 resize-y"
          />
        </div>
      )}

      {/* Response */}
      {sendError && (
        <div className="bg-red-900/40 border border-red-700 rounded p-3">
          <p className="text-xs text-red-400 font-mono">{sendError}</p>
        </div>
      )}

      {response && (
        <div className="space-y-2">
          <div className="flex items-center gap-3">
            <span className={`font-mono text-sm font-bold ${statusColor}`}>
              {response.status} {response.statusText}
            </span>
            <span className="text-xs text-gray-500">{response.duration}ms</span>
          </div>

          <div>
            <p className="text-[10px] text-gray-500 uppercase tracking-widest mb-1">Response Headers</p>
            <pre className="text-xs font-mono text-gray-400 bg-gray-950 rounded p-2 overflow-x-auto devtools-scrollbar max-h-24">
              {Object.entries(response.headers).map(([k, v]) => `${k}: ${v}`).join('\n')}
            </pre>
          </div>

          <div>
            <p className="text-[10px] text-gray-500 uppercase tracking-widest mb-1">Response Body</p>
            <pre
              className="text-xs font-mono bg-gray-950 rounded p-3 overflow-x-auto devtools-scrollbar max-h-72"
              dangerouslySetInnerHTML={{ __html: syntaxHighlight(response.body) }}
            />
          </div>
        </div>
      )}
    </div>
  );
}
