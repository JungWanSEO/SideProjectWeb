package com.commerce.api.notification.entity;

import com.commerce.api.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 알림 발송 기록 — 아웃박스 이벤트를 소비한 결과(모의: 실제 발송 대신 로그로 남김).
 *
 * <p>{@code eventId}(= 아웃박스 OutboxEvent.id)에 <b>UNIQUE</b>를 둬서 <b>멱등 소비</b>를 보장한다:
 * 발행이 at-least-once라 같은 이벤트가 두 번 와도, 두 번째 INSERT는 유니크 위반으로 막혀 중복 발송이 안 된다.
 */
@Getter
@Entity
@Table(name = "notification_log")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long eventId;          // 소비한 아웃박스 이벤트 id (멱등 키)

    @Column(nullable = false, length = 50)
    private String type;           // 예: "PAYMENT_COMPLETED"

    @Column(nullable = false, length = 255)
    private String message;        // 알림 내용

    private NotificationLog(Long eventId, String type, String message) {
        this.eventId = eventId;
        this.type = type;
        this.message = message;
    }

    public static NotificationLog of(Long eventId, String type, String message) {
        return new NotificationLog(eventId, type, message);
    }
}
