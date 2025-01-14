package milky_way.service.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class RedisService {

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
//        redisTemplate.delete("accessToken:" + accessToken);
//        redisTemplate.delete("refreshToken:" + refreshToken);
        Set<String> accessTokenKeys = redisTemplate.keys("accessToken:*");
        Set<String> refreshTokenKeys = redisTemplate.keys("refreshToken:*");
        if (accessTokenKeys != null && !accessTokenKeys.isEmpty()) {
            redisTemplate.delete(accessTokenKeys);
        }
        if (refreshTokenKeys != null && !refreshTokenKeys.isEmpty()) {
            redisTemplate.delete(refreshTokenKeys);
        }
    }

    public <T> void setList(String key, List<T> data, long ttlInSeconds) {
        redisTemplate.opsForValue().set(key, data, ttlInSeconds, TimeUnit.SECONDS);
    }

    public <T> List<T> getList(String key, Class<T> type) {
        Object data = redisTemplate.opsForValue().get(key);
        if (data != null) {
            return (List<T>) data;
        }
        return null;
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    public <T> T get(String key, Class<T> type) {
        Object data = redisTemplate.opsForValue().get(key);
        if (type.isInstance(data)) {
            return type.cast(data);
        }
        return null;
    }

    public <T> void set(String key, T data, long ttlInSeconds) {
        redisTemplate.opsForValue().set(key, data, ttlInSeconds, TimeUnit.SECONDS);
    }
}
