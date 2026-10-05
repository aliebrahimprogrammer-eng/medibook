package com.ga.medibook.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();

        if (isRateLimitedEndpoint(request, path)) {

            String clientIp = getClientIp(request);

            String key = clientIp + ":" + path;

            if (!rateLimitService.isAllowed(key)) {

                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("""
                    {
                        "status": 429,
                        "message": "Too many requests. Please try again later."
                    }
                    """);

                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isRateLimitedEndpoint(
            HttpServletRequest request,
            String path
    ) {
        return request.getMethod().equalsIgnoreCase("POST")
                && (
                path.equals("/api/auth/login")
                        || path.equals("/api/auth/register")
                        || path.equals("/api/auth/forgot-password")
                        || path.equals("/api/auth/reset-password")
        );
    }

    private String getClientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}