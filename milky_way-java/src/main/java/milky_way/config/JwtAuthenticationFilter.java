package milky_way.config;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import milky_way.entity.Role;
import milky_way.service.CustomUserDetailsService;
import milky_way.utils.CustomUserDetails;
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
        String requestUri = request.getRequestURI();
        if (isPublicEndpoint(requestUri)) {
            filterChain.doFilter(request, response);
            return;
        }
        logger.info("Processing request: {}", request.getRequestURI());
        String token = getJwtFromRequest(request);
        if (token != null && jwtTokenProvider.validateToken(token)) {
            Claims claims = jwtTokenProvider.getClaimsFromJWT(token);
            String tokenType = claims.get("type", String.class);
            String email = claims.getSubject();
            if ("access".equals(tokenType)) {
                handleAccessToken(email, claims, token);
            } else if ("refresh".equals(tokenType)) {
                handleRefreshToken(token, email, response);
            }
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Token is invalid or blacklisted.");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isPublicEndpoint(String requestUri) {
        AntPathMatcher pathMatcher = new AntPathMatcher();
        return requestUri.equals("/api/v1/auth/register") ||
                requestUri.equals("/api/v1/auth/login") ||
                requestUri.equals("/api/v1/auth/send-reset-password-email") ||
                pathMatcher.match("/api/v1/auth/oauth2/**", requestUri);
    }

    private void handleAccessToken(String email, Claims claims, String token) {
        List<GrantedAuthority> authorities = jwtTokenProvider.getAuthoritiesFromJWT(token);
        CustomUserDetails userDetails = new CustomUserDetails(email, "", authorities);
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
    }

    private void handleRefreshToken(String token, String email, HttpServletResponse response) throws IOException {
        List<GrantedAuthority> authorities = jwtTokenProvider.getAuthoritiesFromJWT(token);
        CustomUserDetails userDetails = new CustomUserDetails(email, "", authorities);
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

//    public boolean checkRefreshTokenValidity(String token) {
//        try {
//            var claims = jwtTokenProvider.getClaimsFromJWT(token);
//            Date expirationDate = claims.getExpiration();
//            return expirationDate != null && expirationDate.after(new Date());
//        } catch (Exception e) {
//            return false;
//        }
//    }

}