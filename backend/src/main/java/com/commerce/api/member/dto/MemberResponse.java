package com.commerce.api.member.dto;

import com.commerce.api.member.entity.Member;
import com.commerce.api.member.entity.Role;
import java.time.LocalDateTime;

/**
 * 회원 응답 DTO.
 * 엔티티를 그대로 노출하지 않고 필요한 필드만 담는다. (password 제외)
 * 정적 팩토리 from(Member)으로 엔티티 → DTO 변환.
 */
public record MemberResponse(
        Long id,
        String email,
        String nickname,
        Role role,
        LocalDateTime createdAt
) {
    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getEmail(),
                member.getNickname(),
                member.getRole(),
                member.getCreatedAt()
        );
    }
}
