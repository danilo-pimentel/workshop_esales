package com.treinamento.ctf.middleware;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Component
public class RequestLogFilter implements Filter {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public static final ThreadLocal<String> CURRENT_SQL = new ThreadLocal<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!(request instanceof HttpServletRequest httpRequest)) {
            chain.doFilter(request, response);
            return;
        }

        String path = httpRequest.getRequestURI();
        if (path.startsWith("/monitor")) {
            chain.doFilter(request, response);
            return;
        }

        ContentCachingRequestWrapper wrappedRequest  = new ContentCachingRequestWrapper(httpRequest);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(
                (HttpServletResponse) response);

        CURRENT_SQL.remove();

        try {
            chain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            try {
                String method      = wrappedRequest.getMethod();
                String reqPath     = wrappedRequest.getRequestURI();
                String queryParams = wrappedRequest.getQueryString();
                String body        = new String(wrappedRequest.getContentAsByteArray(), StandardCharsets.UTF_8);
                int    statusCode  = wrappedResponse.getStatus();
                String sqlQuery    = CURRENT_SQL.get();

                byte[] responseBody = wrappedResponse.getContentAsByteArray();
                String responsePreview = responseBody.length > 0
                        ? new String(responseBody, StandardCharsets.UTF_8)
                                .substring(0, Math.min(500, responseBody.length))
                        : null;

                String ip = wrappedRequest.getRemoteAddr();

                // Console log
                String statusColor = statusCode >= 500 ? "\u001b[31m" : statusCode >= 400 ? "\u001b[33m" : "\u001b[32m";
                String reset = "\u001b[0m";
                String dim = "\u001b[2m";
                System.out.println(
                    dim + Instant.now().toString() + reset + "  " +
                    statusColor + statusCode + reset + "  " +
                    String.format("%-6s", method) + " " + reqPath +
                    (queryParams != null ? "?" + queryParams : "") +
                    (body.isEmpty() ? "" : dim + "  body=" + body.substring(0, Math.min(80, body.length())) + reset)
                );

                String insertSql = "INSERT INTO request_logs " +
                        "(method, path, query_params, body, status_code, sql_query, response_preview, ip) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

                jdbcTemplate.update(insertSql,
                        method,
                        reqPath,
                        queryParams,
                        body.isEmpty() ? null : body,
                        statusCode,
                        sqlQuery,
                        responsePreview,
                        ip);

            } catch (Exception ignored) {
            }

            wrappedResponse.copyBodyToResponse();
            CURRENT_SQL.remove();
        }
    }
}
