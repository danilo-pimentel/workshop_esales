import { create } from 'zustand';
import type { LoggedRequest, SuspiciousFinding } from '../types';

// ─── Response analyzer ──────────────────────────────────────────────────────

const SQL_KEYWORDS = /\b(SELECT|FROM|WHERE|UNION|INSERT|UPDATE|DELETE|DROP|TABLE|DATABASE|ORDER BY|GROUP BY|HAVING|JOIN|INNER|OUTER|LEFT|RIGHT)\b/gi;
const CREDENTIAL_FIELDS = /"(password|senha|passwd|secret|token|api_key|apikey)":\s*"[^"]{2,}"/gi;
const SQL_ERROR_PATTERNS = /(sqlite|mysql|postgres|ORA-|syntax error|near ".+?"|unterminated quoted|no such column|no such table)/gi;
const PII_PATTERNS = /"(email|cpf|telefone|endereco)":/gi;

function detectFindings(body: string): SuspiciousFinding[] {
  const findings: SuspiciousFinding[] = [];

  if (!body || body.length === 0) return findings;

  const sqlMatches = body.match(SQL_KEYWORDS);
  if (sqlMatches) {
    findings.push({
      type: 'sql',
      label: 'SQL keywords detected',
      excerpt: [...new Set(sqlMatches.map((m) => m.toUpperCase()))].join(', '),
    });
  }

  const credMatches = body.match(CREDENTIAL_FIELDS);
  if (credMatches) {
    findings.push({
      type: 'credential',
      label: 'Credential field(s) exposed',
      excerpt: credMatches.slice(0, 3).join(' | '),
    });
  }

  const errMatches = body.match(SQL_ERROR_PATTERNS);
  if (errMatches) {
    findings.push({
      type: 'error',
      label: 'Database error message',
      excerpt: errMatches.slice(0, 2).join(' | '),
    });
  }

  const piiMatches = body.match(PII_PATTERNS);
  if (piiMatches && piiMatches.length >= 2) {
    findings.push({
      type: 'pii',
      label: 'Personal data detected',
      excerpt: [...new Set(piiMatches)].join(', '),
    });
  }

  return findings;
}

// ─── Store ────────────────────────────────────────────────────────────────────

interface RequestStore {
  requests: LoggedRequest[];
  addRequest: (req: Omit<LoggedRequest, 'findings'>) => void;
  clearRequests: () => void;
}

export const useRequestStore = create<RequestStore>((set) => ({
  requests: [],

  addRequest: (req) => {
    const findings = detectFindings(req.responseBody ?? '');
    set((state) => ({
      requests: [{ ...req, findings }, ...state.requests].slice(0, 200), // keep last 200
    }));
  },

  clearRequests: () => set({ requests: [] }),
}));
