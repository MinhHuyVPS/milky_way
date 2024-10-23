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
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JwtTokenProvider {
    @Value("${jwt.signerKey}")
    private String jwtSecret;
    @Value("${jwtExpirationInMs}")
    private long jwtExpirationInMs;
    @Value("${jwtRefreshExpirationInMs}")
    private long jwtRefreshExpirationInMs;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private TokenBlacklistRepository tokenBlacklistRepository;

    public String generateToken(String email, Set<Role> roles) {
        String rolesJson = null;
        try {
            rolesJson = objectMapper.writeValueAsString(roles.stream()
                    .map(Role::getName)
                    .collect(Collectors.toList()));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return Jwts.builder()
                .setSubject(email)
                .claim("roles", rolesJson)
                .claim("type", "access")
                .setIssuedAt(new Date())
                .setExpiration(new Date(new Date().getTime() + jwtExpirationInMs))
                .signWith(SignatureAlgorithm.HS512, jwtSecret)
                .compact();
    }

    public String generateRefreshToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .claim("type", "refresh")
                .setIssuedAt(new Date())
                .setExpiration(new Date(new Date().getTime() + jwtRefreshExpirationInMs))
                .signWith(SignatureAlgorithm.HS512, jwtSecret)
                .compact();
    }

    public List<GrantedAuthority> getAuthoritiesFromJWT(String token) {
        String rolesJson = getClaimsFromJWT(token).get("roles", String.class);
        List<String> roles = null;
        try {
            roles = objectMapper.readValue(rolesJson, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
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
        return Jwts.parser()
                .setSigningKey(jwtSecret)
                .parseClaimsJws(token)
                .getBody();
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

}
