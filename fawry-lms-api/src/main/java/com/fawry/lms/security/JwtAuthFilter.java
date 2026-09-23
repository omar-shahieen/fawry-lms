package com.fawry.lms.security;

import com.fawry.lms.user.User;
import com.fawry.lms.user.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;

    public JwtAuthFilter(JwtTokenProvider tokenProvider, UserRepository userRepository) {
        this.tokenProvider = tokenProvider;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticate(authorization.substring(BEARER_PREFIX.length()));
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(String token) {
        try {
            JwtTokenProvider.TokenClaims claims = tokenProvider.parseToken(token);
            if (claims.tokenType() != JwtTokenProvider.TokenType.ACCESS) {
                return;
            }

            Optional<User> userResult = userRepository.findById(claims.userId());
            if (userResult.isEmpty()) {
                return;
            }
            User user = userResult.get();
            if (!user.isActive() || user.getRole() != claims.role()
                    || !token.equals(user.getAccessToken())) {
                return;
            }

            var authority = new SimpleGrantedAuthority("ROLE_" + claims.role().name());
            var authentication = new UsernamePasswordAuthenticationToken(user, null, java.util.List.of(authority));
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
        } catch (JwtException | IllegalArgumentException exception) {
            // Invalid tokens are left unauthenticated and rejected by the security entry point.
        }
    }
}
