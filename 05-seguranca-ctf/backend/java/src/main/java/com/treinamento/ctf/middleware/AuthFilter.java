package com.treinamento.ctf.middleware;

import io.jsonwebtoken.Claims;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
@Order(2)
public class AuthFilter implements Filter {

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/forgot-password",
            "/api/products",
            "/monitor"
    );

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  httpRequest  = (HttpServletRequest)  request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path   = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();

        // Allow OPTIONS for CORS preflight
        if ("OPTIONS".equalsIgnoreCase(method)) {
            chain.doFilter(request, response);
            return;
        }

        // Allow root health check
        if ("/".equals(path)) {
            chain.doFilter(request, response);
            return;
        }

        // Allow reset-db (uses its own key-based auth)
        if ("/api/admin/reset-db".equals(path)) {
            chain.doFilter(request, response);
            return;
        }

        // Check if path is public
        boolean isPublic = PUBLIC_PATHS.stream().anyMatch(path::startsWith);

        // Try to extract JWT if present (even on public paths)
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Claims claims = jwtUtil.parseToken(token);
                httpRequest.setAttribute("userId", claims.getSubject());
                httpRequest.setAttribute("userEmail", claims.get("email", String.class));
                httpRequest.setAttribute("userRole", claims.get("role", String.class));
            } catch (Exception ex) {
                if (!isPublic) {
                    httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    httpResponse.setContentType("application/json");
                    httpResponse.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Token invalido ou expirado\"}");
                    return;
                }
            }
            chain.doFilter(request, response);
            return;
        }

        // No Authorization header
        if (isPublic) {
            chain.doFilter(request, response);
            return;
        }

        // Protected path without token
        httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        httpResponse.setContentType("application/json");
        httpResponse.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Token nao fornecido\"}");
    }
}
