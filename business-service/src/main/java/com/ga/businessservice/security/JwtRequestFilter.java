
package com.ga.businessservice.security;

import com.ga.businessservice.dto.response.InternalUserResponse;
import com.ga.businessservice.service.UserServiceClient;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtRequestFilter extends OncePerRequestFilter {

    private final JWTUtils jwtUtils;
    private final UserServiceClient userServiceClient;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String headerAuth = request.getHeader("Authorization");

        if (StringUtils.hasText(headerAuth)
                && headerAuth.startsWith("Bearer ")) {

            String jwt = headerAuth.substring(7);

            if (jwtUtils.validateJwtToken(jwt)) {
                try {
                    String email = jwtUtils.getUserNameFromJwtToken(jwt);

                    logger.info("JWT valid. Resolving user through User Service.");

                    InternalUserResponse user =
                            userServiceClient.findByEmail(email);

                    BusinessUserDetails userDetails =
                            new BusinessUserDetails(user);

                    if (userDetails.isEnabled()) {
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

                        SecurityContextHolder.getContext()
                                .setAuthentication(authentication);

                        logger.info("Business Service authentication successful.");
                    } else {
                        logger.warn("User account is not enabled in Business Service.");
                    }

                } catch (Exception e) {
                    SecurityContextHolder.clearContext();
                    logger.error("Business Service JWT authentication failed during user lookup.", e);
                }
            } else {
                logger.warn("Business Service rejected JWT validation.");
            }
        }

        filterChain.doFilter(request, response);
    }
}
