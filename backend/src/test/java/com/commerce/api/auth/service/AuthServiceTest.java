package com.commerce.api.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.commerce.api.auth.dto.AuthResult;
import com.commerce.api.auth.dto.LoginRequest;
import com.commerce.api.auth.entity.RefreshToken;
import com.commerce.api.auth.repository.RefreshTokenRepository;
import com.commerce.api.global.exception.BusinessException;
import com.commerce.api.global.security.JwtTokenProvider;
import com.commerce.api.member.entity.Member;
import com.commerce.api.member.entity.Role;
import com.commerce.api.member.repository.MemberRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * AuthService 단위 테스트 (로그인 / 토큰 재발급 / 로그아웃).
 * 토큰은 컨트롤러가 쿠키로 굽고, 서비스는 토큰 문자열 + 유저 정보를 AuthResult로 돌려준다.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    private Member member() {
        Member member = Member.builder()
                .email("alice@commerce.com").password("ENCODED").nickname("alice")
                .role(Role.USER).build();
        ReflectionTestUtils.setField(member, "id", 1L);
        return member;
    }

    @Test
    @DisplayName("로그인 성공 - 토큰 발급 + 유저 정보 반환")
    void login_success() {
        given(memberRepository.findByEmail("alice@commerce.com")).willReturn(Optional.of(member()));
        given(passwordEncoder.matches("password123", "ENCODED")).willReturn(true);
        given(jwtTokenProvider.createAccessToken(1L, Role.USER)).willReturn("access.token");
        given(jwtTokenProvider.createRefreshToken(1L)).willReturn("refresh.token");
        given(refreshTokenRepository.findByMemberId(1L)).willReturn(Optional.empty());

        AuthResult result = authService.login(new LoginRequest("alice@commerce.com", "password123"));

        assertThat(result.accessToken()).isEqualTo("access.token");
        assertThat(result.refreshToken()).isEqualTo("refresh.token");
        assertThat(result.user().id()).isEqualTo(1L);
        assertThat(result.user().email()).isEqualTo("alice@commerce.com");
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치면 401")
    void login_wrongPassword() {
        given(memberRepository.findByEmail("alice@commerce.com")).willReturn(Optional.of(member()));
        given(passwordEncoder.matches("wrong", "ENCODED")).willReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice@commerce.com", "wrong")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("이메일 또는 비밀번호");
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 이메일이면 401")
    void login_noUser() {
        given(memberRepository.findByEmail("none@commerce.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("none@commerce.com", "password123")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("이메일 또는 비밀번호");
    }

    @Test
    @DisplayName("토큰 재발급 성공 - 저장된 리프레시와 일치하면 새 토큰을 발급(회전)")
    void refresh_success() {
        RefreshToken stored = RefreshToken.builder().memberId(1L).token("old.refresh").build();
        given(jwtTokenProvider.validate("old.refresh")).willReturn(true);
        given(jwtTokenProvider.getMemberId("old.refresh")).willReturn(1L);
        given(refreshTokenRepository.findByMemberId(1L)).willReturn(Optional.of(stored));
        given(memberRepository.findById(1L)).willReturn(Optional.of(member()));
        given(jwtTokenProvider.createAccessToken(1L, Role.USER)).willReturn("new.access");
        given(jwtTokenProvider.createRefreshToken(1L)).willReturn("new.refresh");

        AuthResult result = authService.refresh("old.refresh");

        assertThat(result.accessToken()).isEqualTo("new.access");
        assertThat(result.refreshToken()).isEqualTo("new.refresh");
        assertThat(stored.getToken()).isEqualTo("new.refresh");   // 회전되어 저장 토큰도 갱신
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 서명/만료가 유효하지 않으면 401")
    void refresh_invalidToken() {
        given(jwtTokenProvider.validate("bad")).willReturn(false);

        assertThatThrownBy(() -> authService.refresh("bad"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("유효하지 않은 리프레시 토큰");
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 저장된 토큰과 다르면(회전됨/재사용) 401")
    void refresh_mismatch() {
        RefreshToken stored = RefreshToken.builder().memberId(1L).token("current.refresh").build();
        given(jwtTokenProvider.validate("old.refresh")).willReturn(true);
        given(jwtTokenProvider.getMemberId("old.refresh")).willReturn(1L);
        given(refreshTokenRepository.findByMemberId(1L)).willReturn(Optional.of(stored));

        assertThatThrownBy(() -> authService.refresh("old.refresh"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("유효하지 않은 리프레시 토큰");
    }

    @Test
    @DisplayName("로그아웃 - 저장된 리프레시 토큰을 폐기한다")
    void logout_deletesStored() {
        RefreshToken stored = RefreshToken.builder().memberId(1L).token("r.token").build();
        given(jwtTokenProvider.validate("r.token")).willReturn(true);
        given(jwtTokenProvider.getMemberId("r.token")).willReturn(1L);
        given(refreshTokenRepository.findByMemberId(1L)).willReturn(Optional.of(stored));

        authService.logout("r.token");

        verify(refreshTokenRepository).delete(stored);
    }
}
