

package com.smartservice.management.security;

import com.smartservice.management.model.User;
import com.smartservice.management.repository.UserRepository;

import io.jsonwebtoken.Claims;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    private static final Set<String> VALID_ROLES =
            Set.of("USER", "STAFF", "ADMIN");

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserRepository userRepository) {

        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {

            String token = authHeader.substring(7).trim();

            try {

                if (!token.isEmpty() && jwtService.isTokenValid(token)) {

                    Claims claims = jwtService.extractAllClaims(token);

                    int userId = Integer.parseInt(claims.getSubject());

                    if (userId > 0 &&
                            SecurityContextHolder.getContext()
                                    .getAuthentication() == null) {

                        User user = userRepository.findById(userId);

                        if (user != null && user.isActive()) {

                            String role = user.getRole();

                            if (role != null && VALID_ROLES.contains(role)) {

                                UsernamePasswordAuthenticationToken authentication =
                                        new UsernamePasswordAuthenticationToken(
                                                String.valueOf(userId),
                                                null,
                                                List.of(
                                                        new SimpleGrantedAuthority(
                                                                "ROLE_" + role
                                                        )
                                                )
                                        );

                                SecurityContextHolder.getContext()
                                        .setAuthentication(authentication);
                            }
                        }
                    }
                }

            } catch (Exception exception) {

                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
