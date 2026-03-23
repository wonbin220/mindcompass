package com.mindcompass.api.user;

import com.mindcompass.api.auth.domain.User;
import com.mindcompass.api.auth.domain.UserSettings;
import com.mindcompass.api.auth.repository.UserRepository;
import com.mindcompass.api.auth.repository.UserSettingsRepository;
import com.mindcompass.api.user.dto.response.UserMeResponse;
import com.mindcompass.api.user.service.UserService;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSettingsRepository userSettingsRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void getMeReturnsUserAndSettings() throws Exception {
        User user = User.create("me@example.com", "encoded-password", "나");
        setField(user, "id", 11L);
        setField(user, "createdAt", LocalDateTime.of(2026, 3, 18, 10, 0));

        UserSettings settings = UserSettings.createDefault(user);

        when(userRepository.findById(11L)).thenReturn(Optional.of(user));
        when(userSettingsRepository.findByUserId(11L)).thenReturn(Optional.of(settings));

        UserMeResponse response = userService.getMe(11L);

        assertThat(response.userId()).isEqualTo(11L);
        assertThat(response.email()).isEqualTo("me@example.com");
        assertThat(response.nickname()).isEqualTo("나");
        assertThat(response.settings().notificationEnabled()).isTrue();
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
