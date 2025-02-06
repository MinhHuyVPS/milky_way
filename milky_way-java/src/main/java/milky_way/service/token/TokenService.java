package milky_way.service.token;

import jakarta.servlet.http.HttpServletRequest;
import milky_way.entity.Token;
import milky_way.entity.User;
import milky_way.exception.AppException;
import milky_way.exception.ErrorCode;
import milky_way.repository.TokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TokenService {

    @Autowired
    private TokenRepository tokenRepository;

    public void saveRefreshToken(User user, String refreshToken) {
        Token token = Token.builder()
                .refreshToken(refreshToken)
                .createDate(LocalDateTime.now())
                .updatedDate(LocalDateTime.now())
                .users(user)
                .build();
        tokenRepository.save(token);
    }

    public void deleteRefreshTokens(User user) {
        List<Token> tokens = tokenRepository.findByUsers(user);
        tokenRepository.deleteAll(tokens);
    }

    public String extractAccessToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new AppException(ErrorCode.INVALID_TOKEN_FORMAT);
        }
        return authorizationHeader.substring(7);
    }
}
