package milky_way.service;

import milky_way.entity.Token;
import milky_way.entity.User;
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
                .createAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .users(user)
                .build();
        tokenRepository.save(token);
    }

    public void deleteRefreshTokens(User user) {
        List<Token> tokens = tokenRepository.findByUsers(user);
        tokenRepository.deleteAll(tokens);
    }
}
