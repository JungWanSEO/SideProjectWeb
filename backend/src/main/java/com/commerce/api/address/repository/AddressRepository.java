package com.commerce.api.address.repository;

import com.commerce.api.address.entity.Address;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 배송지 리포지토리. 메서드 이름 규칙으로 쿼리 자동 생성.
 */
public interface AddressRepository extends JpaRepository<Address, Long> {

    /** 내 주소 목록 — 기본배송지 먼저, 그다음 최신순. */
    List<Address> findByMemberIdOrderByIsDefaultDescCreatedAtDesc(Long memberId);

    /** 현재 기본배송지(없을 수도 있음). set-default 시 기존 기본 해제용. */
    Optional<Address> findByMemberIdAndIsDefaultTrue(Long memberId);

    /** 첫 주소 판별용(0이면 이번이 첫 주소 → 자동 기본). */
    long countByMemberId(Long memberId);

    /** 기본배송지 삭제 시 승격 후보 — 남은 주소 중 최신. */
    Optional<Address> findFirstByMemberIdOrderByCreatedAtDesc(Long memberId);
}
