import { useEffect, useRef } from 'react';
import { useRequestStore } from '../store/requestStore';

let interceptorInstalled = false;

/**
 * Installs a global fetch interceptor that captures every HTTP request
 * and pushes it into the Zustand request store.
 *
 * Safe to call multiple times — installs exactly once per page lifecycle.
 */
export function useRequestLogger() {
  const addRequest = useRequestStore((s) => s.addRequest);
  const addRef = useRef(addRequest);
  addRef.current = addRequest;

  useEffect(() => {
    if (interceptorInstalled) return;
    interceptorInstalled = true;

    const originalFetch = window.fetch.bind(window);

    window.fetch = async (...args: Parameters<typeof fetch>): Promise<Response> => {
      const input = args[0];
      const init = args[1];

      const url = typeof input === 'string' ? input : input instanceof URL ? input.toString() : (input as Request).url;
      const method = (init?.method ?? (input instanceof Request ? input.method : 'GET')).toUpperCase();

      const requestHeaders: Record<string, string> = {};
      if (init?.headers) {
        const h = new Headers(init.headers as HeadersInit);
        h.forEach((v, k) => { requestHeaders[k] = v; });
      }

      const requestBody = init?.body != null
        ? typeof init.body === 'string' ? init.body : '[binary body]'
        : undefined;

      const id = `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
      const startTime = Date.now();

      try {
        const response = await originalFetch(...args);
        const duration = Date.now() - startTime;
        const clone = response.clone();

        // Read body asynchronously — do not block the caller
        clone.text().then((responseBody) => {
          const responseHeaders: Record<string, string> = {};
          response.headers.forEach((v, k) => { responseHeaders[k] = v; });

          addRef.current({
            id,
            timestamp: new Date(),
            method,
            url,
            requestHeaders,
            requestBody,
            status: response.status,
            statusText: response.statusText,
            responseBody,
            responseHeaders,
            duration,
          });
        }).catch(() => {
          addRef.current({
            id,
            timestamp: new Date(),
            method,
            url,
            requestHeaders,
            requestBody,
            status: response.status,
            statusText: response.statusText,
            responseBody: '[could not read body]',
            duration: Date.now() - startTime,
          });
        });

        return response;
      } catch (err) {
        addRef.current({
          id,
          timestamp: new Date(),
          method,
          url,
          requestHeaders,
          requestBody,
          error: err instanceof Error ? err.message : String(err),
          duration: Date.now() - startTime,
        });
        throw err;
      }
    };

  }, []);
}
