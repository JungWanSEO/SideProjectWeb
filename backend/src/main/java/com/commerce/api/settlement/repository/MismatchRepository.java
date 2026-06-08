package com.commerce.api.settlement.repository;

import com.commerce.api.settlement.entity.Mismatch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MismatchRepository extends JpaRepository<Mismatch, Long> {
    // 목록 조회는 JpaRepository.findAll(Pageable), 스냅샷 재생성은 deleteAllInBatch() 사용.
}
