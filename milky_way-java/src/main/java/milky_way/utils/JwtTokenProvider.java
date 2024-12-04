package milky_way.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import milky_way.repository.TokenBlacklistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import milky_way.entity.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class JwtTokenProvider {
    @Value("${jwt.signerKey}")
    private String jwtSecret;
    @Value("${jwtExpirationInSeconds}")
    private long jwtExpirationInSeconds;
    @Value("${jwtRefreshExpirationInSeconds}")
    private long jwtRefreshExpirationInSeconds;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private TokenBlacklistRepository tokenBlacklistRepository;

    public String generateToken(String id ,String email, Set<Role> roles) {
        String rolesJson;
        try {
            if (roles == null || roles.isEmpty()) {
                rolesJson = "[]";
            } else {
                rolesJson = objectMapper.writeValueAsString(roles.stream()
                        .map(Role::getName)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()));
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return Jwts.builder()
                .setSubject(email)
                .claim("roles", rolesJson)
                .claim("type", "access")
                .claim("id", id)
                .setIssuedAt(new Date())
                .setExpiration(new Date(new Date().getTime() + TimeUnit.SECONDS.toMillis(jwtExpirationInSeconds)))
                .signWith(SignatureAlgorithm.HS512, jwtSecret)
                .compact();
    }

    public String generateRefreshToken(String id ,String email, Set<Role> roles) {
        String rolesJson;
        try {
            if (roles == null || roles.isEmpty()) {
                rolesJson = "[]";
            } else {
                rolesJson = objectMapper.writeValueAsString(roles.stream()
                        .map(Role::getName)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()));
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return Jwts.builder()
                .setSubject(email)
                .claim("roles", rolesJson)
                .claim("type", "refresh")
                .claim("id", id)
                .setIssuedAt(new Date())
                .setExpiration(new Date(new Date().getTime() + TimeUnit.SECONDS.toMillis(jwtRefreshExpirationInSeconds)))
                .signWith(SignatureAlgorithm.HS512, jwtSecret)
                .compact();
    }

    public List<GrantedAuthority> getAuthoritiesFromJWT(String token) {
//        System.out.println("getAuthoritiesFromJWT - Token: " + token);
        Claims claims = getClaimsFromJWT(token);
//        System.out.println("getAuthoritiesFromJWT - Claims: " + claims);
        String rolesJson = claims.get("roles", String.class);
//        System.out.println("getAuthoritiesFromJWT - Roles JSON: " + rolesJson);
        List<String> roles = null;
        try {
            roles = objectMapper.readValue(rolesJson, new TypeReference<List<String>>() {});
//            System.out.println("getAuthoritiesFromJWT - Parsed Roles: " + roles);
        } catch (JsonProcessingException e) {
//            System.err.println("getAuthoritiesFromJWT - Failed to parse roles: " + e.getMessage());
            throw new RuntimeException(e);
        }
        return roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    public String getEmailFromJWT(String token) {
        return getClaimsFromJWT(token).getSubject();
    }

    public String getTypeFromJWT(String token) {
        return getClaimsFromJWT(token).get("type", String.class);
    }

    public boolean isTokenExpired(String token) {
        Date expirationDate = getClaimsFromJWT(token).getExpiration();
        return expirationDate.before(new Date());
    }

    public Claims getClaimsFromJWT(String token) {
//        System.out.println("getClaimsFromJWT - Token: " + token);
        if (token.split("\\.").length != 3) {
//            System.err.println("getClaimsFromJWT - Invalid token format. Token must contain exactly 2 periods.");
            throw new IllegalArgumentException("Invalid JWT token format. Token must contain exactly 2 periods.");
        }
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(jwtSecret)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
//            System.out.println("getClaimsFromJWT - Parsed Claims: " + claims);
            return claims;
        } catch (Exception e) {
//            System.out.println("Failed to get claims from token: " + e.getMessage());
            throw e;
        }
    }

    public boolean isTokenBlacklisted(String token) {
        return tokenBlacklistRepository.findByToken(token).isPresent();
    }

    public boolean validateToken(String authToken) {
        try {
            if (isTokenBlacklisted(authToken)) {
//                System.out.println("Token is blacklisted.");
                return false;
            }
            Jwts.parser()
                    .setSigningKey(jwtSecret)
                    .parseClaimsJws(authToken);
            return true;
        } catch (Exception e) {
//            System.out.println("Token validation failed: " + e.getMessage());
            return false;
        }
    }

    public boolean isRefreshToken(String token){
        try{
            Claims claims = getClaimsFromJWT(token);
            String tokenType = claims.get("type", String.class);
            return "refresh".equals(tokenType);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isAccessToken(String token){
        try{
            Claims claims = getClaimsFromJWT(token);
            String tokenType = claims.get("type", String.class);
            return "access".equals(tokenType);
        } catch (Exception e) {
            return false;
        }
    }

    public LocalDateTime getExpiryDateFromToken(String token) {
        Date expiration = getClaimsFromJWT(token).getExpiration();
        return expiration.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    public String getIdFromJWT(String token) {
        return getClaimsFromJWT(token).get("id", String.class);
    }
}
