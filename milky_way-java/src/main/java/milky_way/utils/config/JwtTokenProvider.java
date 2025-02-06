package milky_way.utils.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import milky_way.dto.response.permission.PermissionResponse;
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
import java.util.*;
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

    public String generateToken(String id ,String email, Set<Role> roles, Set<PermissionResponse> permissions) {
        String rolesJson;
        String permissionsJson;
        try {
            if (roles == null || roles.isEmpty()) {
                rolesJson = "[]";
            } else {
                rolesJson = objectMapper.writeValueAsString(roles.stream()
                        .map(Role::getName)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()));
            }
            if (permissions == null || permissions.isEmpty()) {
                permissionsJson = "[]";
            } else {
                permissionsJson = objectMapper.writeValueAsString(permissions.stream()
                        .map(PermissionResponse::getName)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()));
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return Jwts.builder()
                .setSubject(email)
                .claim("roles", rolesJson)
                .claim("permissions", permissionsJson)
                .claim("type", "access")
                .claim("id", id)
                .setIssuedAt(new Date())
                .setExpiration(new Date(new Date().getTime() + TimeUnit.SECONDS.toMillis(jwtExpirationInSeconds)))
                .signWith(SignatureAlgorithm.HS512, jwtSecret)
                .compact();
    }

    public String generateRefreshToken(String id ,String email, Set<Role> roles, Set<PermissionResponse> permissions) {
        String rolesJson;
        String permissionsJson;
        try {
            if (roles == null || roles.isEmpty()) {
                rolesJson = "[]";
            } else {
                rolesJson = objectMapper.writeValueAsString(roles.stream()
                        .map(Role::getName)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()));
            }
            if (permissions == null || permissions.isEmpty()) {
                permissionsJson = "[]";
            } else {
                permissionsJson = objectMapper.writeValueAsString(permissions.stream()
                        .map(PermissionResponse::getName)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()));
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return Jwts.builder()
                .setSubject(email)
                .claim("roles", rolesJson)
                .claim("permissions", permissionsJson)
                .claim("type", "refresh")
                .claim("id", id)
                .setIssuedAt(new Date())
                .setExpiration(new Date(new Date().getTime() + TimeUnit.SECONDS.toMillis(jwtRefreshExpirationInSeconds)))
                .signWith(SignatureAlgorithm.HS512, jwtSecret)
                .compact();
    }

    public List<GrantedAuthority> getAuthoritiesFromJWT(String token) {
        Claims claims = getClaimsFromJWT(token);
        String rolesJson = claims.get("roles", String.class);
        List<String> roles = new ArrayList<>();
        try {
            if (rolesJson != null) {
                roles = objectMapper.readValue(rolesJson, new TypeReference<List<String>>() {});
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse roles", e);
        }
        String permissionsJson = claims.get("permissions", String.class);
        List<String> permissions = new ArrayList<>();
        try {
            if (permissionsJson != null) {
                permissions = objectMapper.readValue(permissionsJson, new TypeReference<List<String>>() {});
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse permissions", e);
        }
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.addAll(roles.stream()
                .map(SimpleGrantedAuthority::new)
                .toList());
        authorities.addAll(permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .toList());
        return authorities;
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
        if (token.split("\\.").length != 3) {
            throw new IllegalArgumentException("Invalid JWT token format. Token must contain exactly 2 periods.");
        }
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(jwtSecret)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims;
        } catch (Exception e) {
            throw e;
        }
    }

    public boolean isTokenBlacklisted(String token) {
        return tokenBlacklistRepository.findByToken(token).isPresent();
    }

    public boolean validateToken(String authToken) {
        try {
            if (isTokenBlacklisted(authToken)) {
                return false;
            }
            Jwts.parser()
                    .setSigningKey(jwtSecret)
                    .parseClaimsJws(authToken);
            return true;
        } catch (Exception e) {
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
