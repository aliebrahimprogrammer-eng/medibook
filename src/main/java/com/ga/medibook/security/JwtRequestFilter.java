package com.ga.medibook.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtRequestFilter extends OncePerRequestFilter {

    private final MyUserDetailsService myUserDetailsService;
    private final JWTUtils jwtUtils;

    private String parseJwt(HttpServletRequest request) {

        String headerAuth = request.getHeader("Authorization");

        if (StringUtils.hasText(headerAuth)
                && headerAuth.startsWith("Bearer ")) {

            return headerAuth.substring(7);
        }

        return null;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {

            String jwt = parseJwt(request);

            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {

                String username =
                        jwtUtils.getUserNameFromJwtToken(jwt);

                UserDetails userDetails =
                        myUserDetailsService.loadUserByUsername(username);

                if (!userDetails.isEnabled()) {
                    filterChain.doFilter(request, response);
                    return;
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request)
                );

                SecurityContextHolder
                        .getContext()
                        .setAuthentication(authentication);
            }

        } catch (Exception e) {
            logger.warn("JWT authentication failed: {}");
        }

        filterChain.doFilter(request, response);
    }
}