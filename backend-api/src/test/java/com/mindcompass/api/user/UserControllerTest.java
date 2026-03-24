package com.mindcompass.api.user;

// User 내 정보 조회 API의 HTTP 계약을 검증하는 WebMvc 테스트다.

import com.mindcompass.api.auth.domain.ResponseMode;
import com.mindcompass.api.auth.domain.UserStatus;
import com.mindcompass.api.auth.security.JwtAuthenticationFilter;
import com.mindcompass.api.common.exception.GlobalExceptionHandler;
import com.mindcompass.api.user.controller.UserController;
import com.mindcompass.api.user.dto.response.UserMeResponse;
import com.mindcompass.api.user.service.UserService;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void getMeReturnsUserProfile() throws Exception {
        when(userService.getMe(isNull()))
                .thenReturn(new UserMeResponse(
                        11L,
                        "me@example.com",
                        "tester",
                        UserStatus.ACTIVE,
                        LocalDateTime.of(2026, 3, 24, 9, 0),
                        new UserMeResponse.Settings(
                                false,
                                true,
                                LocalTime.of(22, 0),
                                ResponseMode.EMPATHETIC
                        )
                ));

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(11))
                .andExpect(jsonPath("$.email").value("me@example.com"))
                .andExpect(jsonPath("$.nickname").value("tester"))
                .andExpect(jsonPath("$.settings.notificationEnabled").value(true))
                .andExpect(jsonPath("$.settings.responseMode").value("EMPATHETIC"));
    }
}
