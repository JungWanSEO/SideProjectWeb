package com.commerce.api.auth.entity;

import com.commerce.api.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 리프레시 토큰 저장 (회원당 1개).
 * 서버가 상태를 가짐으로써 회전(rotation)·폐기(revocation)가 가능하다.
 * (액세스 토큰은 무상태지만, 리프레시는 재발급 통제를 위해 저장한다.)
 */
@Getter
@Entity
@Table(name = "refresh_token")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long memberId;

    @Column(nullable = false, length = 512)
    private String token;

    @Builder
    private RefreshToken(Long memberId, String token) {
        this.memberId = memberId;
        this.token = token;
    }

    /** 회전: 새 리프레시 토큰으로 교체 */
    public void update(String token) {
        this.token = token;
    }
}
