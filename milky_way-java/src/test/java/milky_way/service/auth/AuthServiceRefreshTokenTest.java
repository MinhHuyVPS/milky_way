package milky_way.service.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.http.Cookie;
import milky_way.dto.response.auth.RefreshResponse;
import milky_way.entity.Role;
import milky_way.entity.Session;
import milky_way.entity.TokenBlacklist;
import milky_way.entity.User;
import milky_way.exception.AppException;
import milky_way.exception.ErrorCode;
import milky_way.repository.TokenBlacklistRepository;
import milky_way.repository.UserRepository;
import milky_way.service.permission.PermissionService;
import milky_way.service.session.SessionService;
import milky_way.service.token.TokenBlacklistService;
import milky_way.utils.config.JwtTokenProvider;
import milky_way.utils.cookie.JwtAuthenticationUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceRefreshTokenTest {

    private static final String USER_ID = "user-1";
    private static final String EMAIL = "user@example.com";

    private String secret;
    private JwtTokenProvider jwtTokenProvider;
    private TokenBlacklistRepository tokenBlacklistRepository;
    private AuthService authService;
    private User user;
    private Session session;
    private final Set<String> blacklistedTokens = new HashSet<>();

    @BeforeEach
    void setUp() {
        secret = randomSecret();

        tokenBlacklistRepository = mock(TokenBlacklistRepository.class);
        when(tokenBlacklistRepository.findByToken(any())).thenAnswer(inv ->
                blacklistedTokens.contains(inv.<String>getArgument(0)) ? Optional.of(new TokenBlacklist()) : Optional.empty());
        when(tokenBlacklistRepository.save(any(TokenBlacklist.class))).thenAnswer(inv -> {
            TokenBlacklist entry = inv.getArgument(0);
            blacklistedTokens.add(entry.getToken());
            return entry;
        });

        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", secret);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationInSeconds", 3600L);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtRefreshExpirationInSeconds", 604800L);
        ReflectionTestUtils.setField(jwtTokenProvider, "tokenBlacklistRepository", tokenBlacklistRepository);

        TokenBlacklistService tokenBlacklistService = new TokenBlacklistService();
        ReflectionTestUtils.setField(tokenBlacklistService, "tokenBlacklistRepository", tokenBlacklistRepository);

        user = new User();
        user.setId(USER_ID);
        user.setEmail(EMAIL);
        user.setRoles(Set.of(Role.builder().id("role-1").name("USER").build()));

        UserRepository userRepository = mock(UserRepository.class);
        when(userRepository.findByEmailAndId(EMAIL, USER_ID)).thenAnswer(inv -> Optional.of(user));

        PermissionService permissionService = mock(PermissionService.class);
        when(permissionService.findPermissionsByRolesIn(any())).thenReturn(Set.of());

        session = Session.builder()
                .id("session-1")
                .createdDate(LocalDateTime.now())
                .expiresDate(LocalDateTime.now().plusHours(1))
                .users(user)
                .build();
        SessionService sessionService = mock(SessionService.class);
        when(sessionService.getSession(EMAIL)).thenReturn(Optional.of(session));

        authService = new AuthService();
        ReflectionTestUtils.setField(authService, "jwtTokenProvider", jwtTokenProvider);
        ReflectionTestUtils.setField(authService, "jwtAuthenticationUtils", new JwtAuthenticationUtils());
        ReflectionTestUtils.setField(authService, "tokenBlacklistService", tokenBlacklistService);
        ReflectionTestUtils.setField(authService, "userRepository", userRepository);
        ReflectionTestUtils.setField(authService, "permissionService", permissionService);
        ReflectionTestUtils.setField(authService, "sessionService", sessionService);
    }

    // A
    @Test
    void refreshToken_withValidRefreshToken_returnsNewAccessToken() {
        RefreshResponse refreshResponse = authService.refreshToken(requestWithCookie(newRefreshToken()), new MockHttpServletResponse());

        String accessToken = refreshResponse.getAccessToken();
        assertNotNull(accessToken);
        assertTrue(jwtTokenProvider.validateToken(accessToken));
        assertTrue(jwtTokenProvider.isAccessToken(accessToken));
        assertEquals(EMAIL, jwtTokenProvider.getEmailFromJWT(accessToken));
        assertEquals(USER_ID, jwtTokenProvider.getIdFromJWT(accessToken));
        assertEquals(session.getId(), refreshResponse.getSessionResponse().getId());
    }

    // B
    @Test
    void refreshToken_withValidRefreshToken_setsNewRefreshTokenCookie() {
        String oldRefreshToken = newRefreshToken();
        MockHttpServletResponse response = new MockHttpServletResponse();

        authService.refreshToken(requestWithCookie(oldRefreshToken), response);

        Cookie cookie = response.getCookie("refreshToken");
        assertNotNull(cookie);
        String newRefreshToken = cookie.getValue();
        assertNotEquals(oldRefreshToken, newRefreshToken);
        assertTrue(jwtTokenProvider.isRefreshToken(newRefreshToken));
        assertEquals(EMAIL, jwtTokenProvider.getEmailFromJWT(newRefreshToken));
        assertEquals(USER_ID, jwtTokenProvider.getIdFromJWT(newRefreshToken));
        assertNotEquals(claims(oldRefreshToken).getId(), claims(newRefreshToken).getId());
        assertTrue(cookie.isHttpOnly());
        assertTrue(cookie.getSecure());
        assertEquals("/api/v1/auth/", cookie.getPath());
        assertEquals(7 * 24 * 60 * 60, cookie.getMaxAge());
    }

    // C
    @Test
    void refreshToken_withValidRefreshToken_blacklistsOldRefreshToken() {
        String oldRefreshToken = newRefreshToken();

        authService.refreshToken(requestWithCookie(oldRefreshToken), new MockHttpServletResponse());

        ArgumentCaptor<TokenBlacklist> captor = ArgumentCaptor.forClass(TokenBlacklist.class);
        verify(tokenBlacklistRepository).save(captor.capture());
        assertEquals(oldRefreshToken, captor.getValue().getToken());
        assertEquals(jwtTokenProvider.getExpiryDateFromToken(oldRefreshToken), captor.getValue().getExpiryDate());
        assertTrue(jwtTokenProvider.isTokenBlacklisted(oldRefreshToken));
    }

    // D
    @Test
    void refreshToken_withAlreadyRotatedRefreshToken_isRejected() {
        String oldRefreshToken = newRefreshToken();
        authService.refreshToken(requestWithCookie(oldRefreshToken), new MockHttpServletResponse());

        assertRejected(oldRefreshToken);
    }

    // E
    @Test
    void refreshToken_withRotatedNewRefreshToken_canRefreshAgain() {
        String firstRefreshToken = newRefreshToken();
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        authService.refreshToken(requestWithCookie(firstRefreshToken), firstResponse);
        String secondRefreshToken = firstResponse.getCookie("refreshToken").getValue();

        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        RefreshResponse refreshResponse = authService.refreshToken(requestWithCookie(secondRefreshToken), secondResponse);

        assertTrue(jwtTokenProvider.isAccessToken(refreshResponse.getAccessToken()));
        String thirdRefreshToken = secondResponse.getCookie("refreshToken").getValue();
        assertNotEquals(firstRefreshToken, thirdRefreshToken);
        assertNotEquals(secondRefreshToken, thirdRefreshToken);
        assertTrue(jwtTokenProvider.isTokenBlacklisted(secondRefreshToken));
        assertFalse(jwtTokenProvider.isTokenBlacklisted(thirdRefreshToken));
    }

    // F
    @Test
    void refreshToken_withoutRefreshTokenCookie_isRejected() {
        assertRejected(new MockHttpServletRequest());

        MockHttpServletRequest requestWithOtherCookie = new MockHttpServletRequest();
        requestWithOtherCookie.setCookies(new Cookie("other", "value"));
        assertRejected(requestWithOtherCookie);
    }

    // G
    @Test
    void refreshToken_withAccessToken_isRejected() {
        String accessToken = jwtTokenProvider.generateToken(USER_ID, EMAIL, user.getRoles(), Set.of());

        assertRejected(accessToken);
    }

    // H
    @Test
    void refreshToken_withExpiredRefreshToken_isRejected() {
        String expiredRefreshToken = Jwts.builder()
                .setSubject(EMAIL)
                .claim("type", "refresh")
                .claim("id", USER_ID)
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(new Date(System.currentTimeMillis() - 120_000))
                .setExpiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();

        assertRejected(expiredRefreshToken);
    }

    // I
    @Test
    void refreshToken_withBlacklistedRefreshToken_isRejected() {
        String blacklistedRefreshToken = newRefreshToken();
        blacklistedTokens.add(blacklistedRefreshToken);

        assertRejected(blacklistedRefreshToken);
    }

    // J
    @ParameterizedTest
    @ValueSource(strings = {"", "not-a-jwt", "a.b.c", "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ4In0"})
    void refreshToken_withMalformedToken_isRejected(String malformedToken) {
        assertRejected(malformedToken);
    }

    // J
    @Test
    void refreshToken_withTokenSignedByAnotherKey_isRejected() {
        String foreignRefreshToken = Jwts.builder()
                .setSubject(EMAIL)
                .claim("type", "refresh")
                .claim("id", USER_ID)
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(SignatureAlgorithm.HS512, randomSecret())
                .compact();

        assertRejected(foreignRefreshToken);
    }

    private String newRefreshToken() {
        return jwtTokenProvider.generateRefreshToken(USER_ID, EMAIL, user.getRoles(), Set.of());
    }

    private Claims claims(String token) {
        return jwtTokenProvider.getClaimsFromJWT(token);
    }

    private static MockHttpServletRequest requestWithCookie(String refreshToken) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("refreshToken", refreshToken));
        return request;
    }

    private void assertRejected(String refreshToken) {
        assertRejected(requestWithCookie(refreshToken));
    }

    private void assertRejected(MockHttpServletRequest request) {
        clearInvocations(tokenBlacklistRepository);
        MockHttpServletResponse response = new MockHttpServletResponse();

        AppException exception = assertThrows(AppException.class, () -> authService.refreshToken(request, response));

        assertEquals(ErrorCode.INVALID_REFRESH_TOKEN, exception.getErrorCode());
        assertNull(response.getCookie("refreshToken"));
        verify(tokenBlacklistRepository, never()).save(any());
    }

    private static String randomSecret() {
        byte[] key = new byte[64];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
}
