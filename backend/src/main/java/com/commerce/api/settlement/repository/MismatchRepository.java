package com.commerce.api.settlement.repository;

import com.commerce.api.settlement.entity.Mismatch;
import com.commerce.api.settlement.entity.MismatchStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MismatchRepository extends JpaRepository<Mismatch, Long> {

    /** 이미 처리된(RESOLVED/IGNORED) 불일치 — 재대사에서 같은 거래키를 다시 OPEN으로 만들지 않으려 조회. */
    List<Mismatch> findByStatusIn(Collection<MismatchStatus> statuses);

    /** 직전 OPEN 스냅샷만 비운다(처리된 건은 보존). */
    void deleteByStatus(MismatchStatus status);

    /** 상태별 목록(예: OPEN만). */
    Page<Mismatch> findByStatus(MismatchStatus status, Pageable pageable);
}
