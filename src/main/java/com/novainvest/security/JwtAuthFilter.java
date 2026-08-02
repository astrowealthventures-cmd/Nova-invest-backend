package com.novainvest.security;

import com.novainvest.model.User;
import com.novainvest.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Mirrors the FastAPI get_current_user dependency:
 * 1. Look for the "access_token" cookie
 * 2. Fall back to the Authorization: Bearer header
 * 3. Validate it's an "access" type token (not a refresh token)
 * 4. Load the user and put it in the SecurityContext
 *
 * Requests with no/invalid token simply proceed unauthenticated -
 * @PreAuthorize / SecurityConfig decide what's allowed.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = extractToken(request);

        if (token != null) {
            try {
                Claims claims = jwtService.parseClaims(token);
                if ("access".equals(claims.get("type", String.class))) {
                    String userId = claims.getSubject();
                    Optional<User> user = userRepository.findById(userId);
                    user.ifPresent(u -> {
                        if (!u.isEnabled()) {
                            return; // don't authenticate unverified users
                        }
                        var authority = new SimpleGrantedAuthority("ROLE_" + u.getRole().toUpperCase());
                        var authToken = new UsernamePasswordAuthenticationToken(u, null, List.of(authority));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    });
                }
            } catch (JwtException | IllegalArgumentException ignored) {
                // invalid/expired token -> request proceeds unauthenticated
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("access_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
