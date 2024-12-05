package milky_way.config;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import milky_way.entity.Role;
import milky_way.service.CustomUserDetailsService;
import milky_way.utils.CustomUserDetails;
import milky_way.utils.JwtTokenProvider;
import milky_way.utils.Notification;
import milky_way.utils.cookie.JwtAuthenticationUtils;
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

    @Autowired
    private JwtAuthenticationUtils jwtAuthenticationUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {
        String requestUri = request.getRequestURI();
        if (isPublicEndpoint(requestUri)) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = getJwtFromRequest(request);
        if (token != null && jwtTokenProvider.validateToken(token)) {
            Claims claims = jwtTokenProvider.getClaimsFromJWT(token);
            String email = claims.getSubject();
            handleToken(email, token);
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(Notification.TOKEN_INVALID_BLACKLIST.getValue());
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isPublicEndpoint(String requestUri) {
        AntPathMatcher pathMatcher = new AntPathMatcher();
        return requestUri.equals("/api/v1/auth/register") ||
                requestUri.equals("/api/v1/auth/login") ||
                requestUri.equals("/api/v1/auth/send-reset-password-email") ||
                requestUri.equals("/api/v1/auth/verify-otp") ||
                requestUri.equals("/api/v1/auth/reset-password") ||
                pathMatcher.match("/api/v1/auth/oauth2/**", requestUri);
    }

    private void handleToken(String email, String token) {
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
        return jwtAuthenticationUtils.extractRefreshTokenFromCookie(request);
    }

}