import { useState } from 'react';
import { useRequestStore } from '../store/requestStore';
import type { LoggedRequest, SuspiciousFinding } from '../types';

// ─── Syntax highlighter for JSON ─────────────────────────────────────────────

function highlightJson(raw: string): string {
  // Simple token-by-token highlight
  try {
    // Re-format if valid JSON
    const parsed = JSON.parse(raw);
    raw = JSON.stringify(parsed, null, 2);
  } catch {
    // Leave as-is if not valid JSON
  }

  return raw
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    // strings
    .replace(/"([^"\\]*(\\.[^"\\]*)*)"/g, (match) => `<span class="token-string">${match}</span>`)
    // numbers
    .replace(/\b(-?\d+\.?\d*(?:[eE][+-]?\d+)?)\b/g, '<span class="token-number">$1</span>')
    // booleans
    .replace(/\b(true|false)\b/g, '<span class="token-bool">$1</span>')
    // null
    .replace(/\bnull\b/g, '<span class="token-null">null</span>');
}

function highlightSuspicious(html: string): string {
  const SQL_KW = /\b(SELECT|FROM|WHERE|UNION|INSERT|UPDATE|DELETE|DROP|TABLE|DATABASE|ORDER BY|GROUP BY|HAVING|JOIN)\b/gi;
  const CRED_FIELDS = /"(password|senha|passwd|secret)":\s*"[^"]+"/gi;
  const ERR_PATTERNS = /(sqlite|mysql|postgres|ORA-\d+|syntax error|near &quot;|unterminated|no such (column|table))/gi;

  return html
    .replace(SQL_KW, (m) => `<span class="token-sql">${m}</span>`)
    .replace(CRED_FIELDS, (m) => `<span class="token-cred">${m}</span>`)
    .replace(ERR_PATTERNS, (m) => `<span class="token-error">${m}</span>`);
}

// ─── Status badge ─────────────────────────────────────────────────────────────

function StatusBadge({ status }: { status?: number }) {
  if (status === undefined) {
    return <span className="text-red-400 font-mono text-xs">ERR</span>;
  }
  const color =
    status >= 500 ? 'text-red-400' :
    status >= 400 ? 'text-yellow-400' :
    status >= 200 ? 'text-green-400' :
    'text-gray-400';
  return <span className={`font-mono text-xs font-bold ${color}`}>{status}</span>;
}

// ─── Finding badges ───────────────────────────────────────────────────────────

const FINDING_STYLES: Record<SuspiciousFinding['type'], string> = {
  sql: 'bg-orange-900/60 text-orange-300 border border-orange-700',
  credential: 'bg-red-900/60 text-red-300 border border-red-700',
  error: 'bg-yellow-900/60 text-yellow-300 border border-yellow-700',
  pii: 'bg-purple-900/60 text-purple-300 border border-purple-700',
};

function FindingBadge({ finding }: { finding: SuspiciousFinding }) {
  return (
    <span className={`text-[10px] px-1.5 py-0.5 rounded font-mono ${FINDING_STYLES[finding.type]}`}>
      {finding.label}
    </span>
  );
}

// ─── Single request row ───────────────────────────────────────────────────────

function RequestRow({ req }: { req: LoggedRequest }) {
  const [expanded, setExpanded] = useState(false);

  const rowBg =
    req.error || (req.status !== undefined && req.status >= 500) ? 'hover:bg-red-950/40' :
    req.status !== undefined && req.status >= 400 ? 'hover:bg-yellow-950/40' :
    'hover:bg-gray-800/50';

  const highlightedBody = req.responseBody
    ? highlightSuspicious(highlightJson(req.responseBody))
    : null;

  return (
    <div className={`border-b border-gray-800 ${rowBg} transition-colors`}>
      {/* Summary row */}
      <button
        className="w-full text-left px-3 py-2 flex items-center gap-2"
        onClick={() => setExpanded((v) => !v)}
      >
        <span className="text-gray-500 text-xs font-mono w-4">{expanded ? '▾' : '▸'}</span>
        <StatusBadge status={req.status} />
        <span className="text-blue-400 font-mono text-xs w-14 shrink-0">{req.method}</span>
        <span className="text-gray-300 font-mono text-xs truncate flex-1">{req.url}</span>
        {req.duration !== undefined && (
          <span className="text-gray-600 font-mono text-[10px] shrink-0">{req.duration}ms</span>
        )}
        <span className="text-gray-600 font-mono text-[10px] shrink-0">
          {req.timestamp.toLocaleTimeString('pt-BR')}
        </span>
      </button>

      {/* Finding badges on the row */}
      {req.findings.length > 0 && (
        <div className="px-9 pb-1.5 flex flex-wrap gap-1">
          {req.findings.map((f, i) => <FindingBadge key={i} finding={f} />)}
        </div>
      )}

      {/* Expanded details */}
      {expanded && (
        <div className="px-4 pb-4 space-y-3 animate-fade-in">
          {/* Request section */}
          <div>
            <p className="text-[10px] text-gray-500 uppercase tracking-widest mb-1">Request Headers</p>
            <pre className="text-xs font-mono text-gray-400 bg-gray-950 rounded p-2 overflow-x-auto devtools-scrollbar">
              {Object.entries(req.requestHeaders).map(([k, v]) => `${k}: ${v}`).join('\n') || '(none)'}
            </pre>
          </div>

          {req.requestBody && (
            <div>
              <p className="text-[10px] text-gray-500 uppercase tracking-widest mb-1">Request Body</p>
              <pre
                className="text-xs font-mono bg-gray-950 rounded p-2 overflow-x-auto devtools-scrollbar max-h-40"
                dangerouslySetInnerHTML={{ __html: highlightJson(req.requestBody) }}
              />
            </div>
          )}

          {/* Response section */}
          <div>
            <p className="text-[10px] text-gray-500 uppercase tracking-widest mb-1">Response Body</p>
            {req.error ? (
              <pre className="text-xs font-mono text-red-400 bg-gray-950 rounded p-2 overflow-x-auto devtools-scrollbar">
                {req.error}
              </pre>
            ) : (
              <pre
                className="text-xs font-mono bg-gray-950 rounded p-2 overflow-x-auto devtools-scrollbar max-h-60"
                dangerouslySetInnerHTML={{ __html: highlightedBody ?? '(empty)' }}
              />
            )}
          </div>
        </div>
      )}
    </div>
  );
}

// ─── Request Log panel ────────────────────────────────────────────────────────

export default function RequestLog() {
  const requests = useRequestStore((s) => s.requests);
  const clearRequests = useRequestStore((s) => s.clearRequests);

  return (
    <div className="flex flex-col h-full">
      {/* Toolbar */}
      <div className="flex items-center justify-between px-3 py-2 border-b border-gray-700 shrink-0">
        <span className="text-xs text-gray-400">
          {requests.length} request{requests.length !== 1 ? 's' : ''} capturado{requests.length !== 1 ? 's' : ''}
        </span>
        <button
          onClick={clearRequests}
          className="text-xs text-gray-500 hover:text-red-400 transition-colors"
        >
          Limpar
        </button>
      </div>

      {/* Legend */}
      <div className="flex gap-3 px-3 py-1.5 border-b border-gray-800 shrink-0 text-[10px] text-gray-500">
        <span className="text-green-400">■ 2xx</span>
        <span className="text-yellow-400">■ 4xx</span>
        <span className="text-red-400">■ 5xx / ERR</span>
        <span className="text-orange-400">■ SQL</span>
        <span className="text-red-300">■ Credencial</span>
        <span className="text-yellow-300">■ Erro DB</span>
      </div>

      {/* Requests list */}
      <div className="flex-1 overflow-y-auto devtools-scrollbar">
        {requests.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-full text-gray-600 text-sm gap-2">
            <span className="text-3xl">📡</span>
            <p>Nenhum request capturado ainda.</p>
            <p className="text-xs">Interaja com a aplicação para ver os requests aqui.</p>
          </div>
        ) : (
          requests.map((req) => <RequestRow key={req.id} req={req} />)
        )}
      </div>
    </div>
  );
}
