package com.commerce.api.notification.repository;

import com.commerce.api.notification.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<NotificationLog, Long> {

    /** 이미 이 이벤트로 알림을 만들었는지 — 멱등 소비(중복 디스패치 스킵)용. */
    boolean existsByEventId(Long eventId);
}
