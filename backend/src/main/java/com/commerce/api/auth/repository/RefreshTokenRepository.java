package com.commerce.api.auth.repository;

import com.commerce.api.auth.entity.RefreshToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 리프레시 토큰 DB 접근. 회원당 1개(memberId unique).
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByMemberId(Long memberId);
}
