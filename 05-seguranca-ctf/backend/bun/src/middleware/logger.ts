/**
 * Request logger middleware.
 */

import { db } from "../db";

// Global mutable slot so route handlers can attach the SQL they ran
// before the response is finalised.
export let currentRequestSql: string | null = null;

export function setCurrentSql(sql: string) {
  currentRequestSql = sql;
}

export function clearCurrentSql() {
  currentRequestSql = null;
}

/**
 * Call this AFTER a response is ready to persist the log entry.
 * Elysia lifecycle hooks make it easy to do this in onAfterHandle / onError.
 */
export function logRequest(opts: {
  method:          string;
  path:            string;
  queryParams:     string; // raw search string, e.g. "?search=foo"
  body:            string;
  statusCode:      number;
  sqlQuery:        string;
  responsePreview: string;
  ip:              string;
}) {
  // Console log
  const statusColor = opts.statusCode >= 500 ? "\x1b[31m" : opts.statusCode >= 400 ? "\x1b[33m" : "\x1b[32m";
  const reset = "\x1b[0m";
  const dim = "\x1b[2m";
  console.log(
    `${dim}${new Date().toISOString()}${reset}  ${statusColor}${opts.statusCode}${reset}  ${opts.method.padEnd(6)} ${opts.path}${opts.queryParams ? "?" + opts.queryParams : ""}${opts.body ? dim + "  body=" + opts.body.substring(0, 80) + reset : ""}`
  );

  // Persist to database
  try {
    db.run(
      `INSERT INTO request_logs
         (method, path, query_params, body, status_code, sql_query, response_preview, ip)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?)`,
      [
        opts.method,
        opts.path,
        opts.queryParams,
        opts.body,
        opts.statusCode,
        opts.sqlQuery,
        opts.responsePreview,
        opts.ip,
      ]
    );
  } catch {
    // Never crash the server because of a logging failure
  }
}
