package milky_way.controller;

import jakarta.servlet.http.Cookie;
import milky_way.dto.response.auth.RefreshResponse;
import milky_way.dto.response.session.SessionResponse;
import milky_way.exception.AppException;
import milky_way.exception.ErrorCode;
import milky_way.service.auth.AuthService;
import milky_way.utils.cookie.JwtAuthenticationUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerRefreshTokenTest {

    private AuthService authService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        AuthController authController = new AuthController();
        ReflectionTestUtils.setField(authController, "authService", authService);
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    @Test
    void refresh_onSuccess_returnsAccessTokenAndRotatedRefreshTokenCookie() throws Exception {
        when(authService.refreshToken(any(), any())).thenAnswer(inv -> {
            new JwtAuthenticationUtils().addRefreshTokenCookie("new-refresh-token", inv.getArgument(1));
            LocalDateTime now = LocalDateTime.now();
            return new RefreshResponse("new-access-token", new SessionResponse("session-1", now, now.plusHours(1)));
        });

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie("refreshToken", "old-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.sessionResponse.id").value("session-1"))
                .andExpect(cookie().value("refreshToken", "new-refresh-token"))
                .andExpect(cookie().httpOnly("refreshToken", true))
                .andExpect(cookie().secure("refreshToken", true))
                .andExpect(cookie().path("refreshToken", "/api/v1/auth/"));
    }

    @Test
    void refresh_onInvalidRefreshToken_returnsErrorWithoutCookie() throws Exception {
        when(authService.refreshToken(any(), any())).thenThrow(new AppException(ErrorCode.INVALID_REFRESH_TOKEN));

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie("refreshToken", "old-refresh-token")))
                .andExpect(status().is(ErrorCode.INVALID_REFRESH_TOKEN.getHttpStatus().value()))
                .andExpect(content().string(containsString(ErrorCode.INVALID_REFRESH_TOKEN.getMessage())))
                .andExpect(header().doesNotExist("Set-Cookie"));
    }
}
