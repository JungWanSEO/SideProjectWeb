package com.commerce.api.member.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.commerce.api.global.config.JpaConfig;
import com.commerce.api.global.config.QuerydslConfig;
import com.commerce.api.member.entity.Member;
import com.commerce.api.member.entity.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * MemberRepository 슬라이스 테스트.
 * - @DataJpaTest: JPA 관련 빈만 로딩, 임베디드 H2 사용, 각 테스트 후 자동 롤백.
 * - @Import(JpaConfig): @EnableJpaAuditing을 끌어와 createdAt 자동 기록까지 검증.
 */
@DataJpaTest
// QuerydslConfig: @DataJpaTest가 모든 리포지토리를 로드하므로 ProductRepository(QueryDSL)의
// JPAQueryFactory 빈이 슬라이스에도 필요하다.
@Import({JpaConfig.class, QuerydslConfig.class})
class MemberRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    private Member newMember(String email, String nickname) {
        return Member.builder()
                .email(email)
                .password("password123")
                .nickname(nickname)
                .role(Role.USER)
                .build();
    }

    @Test
    @DisplayName("existsByEmail - 가입된 이메일이면 true")
    void existsByEmail_true() {
        memberRepository.save(newMember("alice@commerce.com", "alice"));
        assertThat(memberRepository.existsByEmail("alice@commerce.com")).isTrue();
    }

    @Test
    @DisplayName("existsByEmail - 없는 이메일이면 false")
    void existsByEmail_false() {
        assertThat(memberRepository.existsByEmail("nobody@commerce.com")).isFalse();
    }

    @Test
    @DisplayName("저장 시 id와 createdAt이 자동으로 채워진다 (JPA Auditing)")
    void save_autoFields() {
        Member saved = memberRepository.save(newMember("bob@commerce.com", "bob"));
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("findById - 저장한 회원을 조회한다")
    void findById_success() {
        Member saved = memberRepository.save(newMember("carol@commerce.com", "carol"));
        assertThat(memberRepository.findById(saved.getId()))
                .isPresent()
                .get()
                .extracting(Member::getEmail)
                .isEqualTo("carol@commerce.com");
    }
}