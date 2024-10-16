package milky_way.utils;

import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {
    private String jwtSecret = "";
    private long jwtExpirationInMs = 3600000;

    public String generateToken(String email){
        return null;
    }

    public String getEmailFromJWT(String token) {
        return null;
    }

    public boolean validateToken(String authToken) {
        return true;
    }

}
