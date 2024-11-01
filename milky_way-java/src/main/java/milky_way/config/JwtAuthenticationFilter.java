package milky_way.config;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import milky_way.entity.Role;
import milky_way.service.CustomUserDetailsService;
import milky_way.utils.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {
        AntPathMatcher pathMatcher = new AntPathMatcher();
        String requestUri = request.getRequestURI();
        if (requestUri.equals("/api/v1/auth/register") ||
                requestUri.equals("/api/v1/auth/login") ||
                pathMatcher.match("/api/v1/auth/oauth2/**", requestUri)) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = getJwtFromRequest(request);
        if (token != null && jwtTokenProvider.validateToken(token)) {
            Claims claims = jwtTokenProvider.getClaimsFromJWT(token);
            String tokenType = claims.get("type", String.class);
            String email = claims.getSubject();
            if ("access".equals(tokenType)) {
                handleAccessToken(email, claims);
            } else if ("refresh".equals(tokenType)) {
                handleRefreshToken(token, email, response);
                return;
            }
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Token is invalid or blacklisted.");
            return;
        }
        filterChain.doFilter(request, response);
    }


    private void handleAccessToken(String email, Claims claims) {
        List<GrantedAuthority> authorities = jwtTokenProvider.getAuthoritiesFromJWT(String.valueOf(claims));
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
    }

    private void handleRefreshToken(String token, String email, HttpServletResponse response) throws IOException {
        boolean isRefreshTokenValid = checkRefreshTokenValidity(token);
        if (isRefreshTokenValid) {
            Set<Role> roles = jwtTokenProvider.getAuthoritiesFromJWT(token).stream()
                    .map(GrantedAuthority::getAuthority)
                    .map(Role::new)
                    .collect(Collectors.toSet());
            String newAccessToken = jwtTokenProvider.generateToken(email, roles);
            response.setHeader("Authorization", "Bearer " + newAccessToken);
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write("{\"accessToken\": \"" + newAccessToken + "\"}");
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Refresh token is invalid or expired.\"}");
            response.setContentType("application/json");
        }
    }


    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    public boolean checkRefreshTokenValidity(String token) {
        try {
            var claims = jwtTokenProvider.getClaimsFromJWT(token);
            Date expirationDate = claims.getExpiration();
            return expirationDate != null && expirationDate.after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

}