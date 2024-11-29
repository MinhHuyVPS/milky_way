package milky_way.utils.cookie;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthenticationUtils {

    private static final int REFRESH_TOKEN_MAX_AGE = 7 * 24 * 60 * 60;
    private static final String REFRESH_TOKEN_PATH = "/api/v1/auth/";
    private static final boolean REFRESH_TOKEN_SECURE = true;
    private static final boolean REFRESH_TOKEN_HTTP_ONLY = true;

    public String extractRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("refreshToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    public void addRefreshTokenCookie(String refreshToken, HttpServletResponse response) {
        Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
        refreshTokenCookie.setHttpOnly(REFRESH_TOKEN_HTTP_ONLY);
        refreshTokenCookie.setSecure(REFRESH_TOKEN_SECURE);
        refreshTokenCookie.setPath(REFRESH_TOKEN_PATH);
        refreshTokenCookie.setMaxAge(REFRESH_TOKEN_MAX_AGE);
        response.addCookie(refreshTokenCookie);
    }

    public void deleteRefreshTokenCookie(HttpServletResponse response) {
        Cookie refreshTokenCookie = new Cookie("refreshToken", null);
        refreshTokenCookie.setHttpOnly(REFRESH_TOKEN_HTTP_ONLY);
        refreshTokenCookie.setSecure(REFRESH_TOKEN_SECURE);
        refreshTokenCookie.setPath(REFRESH_TOKEN_PATH);
        refreshTokenCookie.setMaxAge(0);
        response.addCookie(refreshTokenCookie);
    }
}
