package com.mindcompass.api.auth.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 회원가입 요청 본문을 검증하는 DTO입니다.
public record SignUpRequest(
        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(max = 50, message = "닉네임은 50자 이하여야 합니다.")
        String nickname,

        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 255, message = "이메일은 255자 이하여야 합니다.")
        String email,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하여야 합니다.")
        String password,

        @AssertTrue(message = "개인정보 수집 및 이용 동의는 필수입니다.")
        boolean personalInfoConsentAgreed,

        @AssertTrue(message = "서비스 이용약관 동의는 필수입니다.")
        boolean serviceTermsAgreed
) {
}
