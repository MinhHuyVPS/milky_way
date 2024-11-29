package milky_way.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RedisTokenService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private final long ACCESS_TOKEN_TTL = 3600;
    private final long REFRESH_TOKEN_TTL = 604800;

    public void saveAccessToken(String email, String token) {
        redisTemplate.opsForValue().set("accessToken:" + token, email, ACCESS_TOKEN_TTL, TimeUnit.SECONDS);
    }

    public void saveRefreshToken(String email, String token) {
        redisTemplate.opsForValue().set("refreshToken:" + token, email, REFRESH_TOKEN_TTL, TimeUnit.SECONDS);
    }

    public boolean isTokenValid(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("accessToken:" + token)) || Boolean.TRUE.equals(redisTemplate.hasKey("refreshToken:" + token));
    }

    public void invalidateToken(String accessToken, String refreshToken) {
        redisTemplate.delete("accessToken:" + accessToken);
        redisTemplate.delete("refreshToken:" + refreshToken);
    }
}
