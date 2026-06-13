package com.commerce.api.address.entity;

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
 * 배송지(주소록 항목). 회원당 여러 개를 저장하고 그중 하나를 기본배송지로 둔다.
 *
 * <p>회원은 다른 애그리거트라 <b>ID 참조</b>(memberId) — @ManyToOne 아님(architecture.md §11).
 * "회원당 기본배송지 1개" 불변식은 여러 행에 걸친 규칙이라 엔티티가 아니라 {@code AddressService}가 보장한다.
 * (.NET 비유: 엔티티는 한 행의 상태/행위만, 행 사이의 규칙은 도메인 서비스에서.)
 */
@Getter
@Entity
@Table(name = "address")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Address extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long memberId;      // 소유 회원 → ID 참조

    @Column(nullable = false, length = 50)
    private String recipient;   // 수령인

    @Column(nullable = false, length = 30)
    private String phone;       // 연락처

    @Column(nullable = false, length = 10)
    private String zipcode;     // 우편번호

    @Column(nullable = false, length = 200)
    private String address1;    // 기본주소(도로명/지번)

    @Column(length = 200)
    private String address2;    // 상세주소(선택)

    /** 기본배송지 여부. 회원당 1개만 true (= AddressService가 단일화 보장). */
    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    private Address(Long memberId, String recipient, String phone, String zipcode,
                    String address1, String address2) {
        this.memberId = memberId;
        this.recipient = recipient;
        this.phone = phone;
        this.zipcode = zipcode;
        this.address1 = address1;
        this.address2 = address2;
        this.isDefault = false;
    }

    /** 정적 팩토리. 기본배송지 지정은 service가 markDefault로 별도 수행(첫 주소 자동 기본). */
    public static Address create(Long memberId, String recipient, String phone, String zipcode,
                                 String address1, String address2) {
        return new Address(memberId, recipient, phone, zipcode, address1, address2);
    }

    /** 주소 내용 수정. 기본배송지 여부는 건드리지 않는다(그건 setDefault의 책임). */
    public void update(String recipient, String phone, String zipcode,
                       String address1, String address2) {
        this.recipient = recipient;
        this.phone = phone;
        this.zipcode = zipcode;
        this.address1 = address1;
        this.address2 = address2;
    }

    public void markDefault() {
        this.isDefault = true;
    }

    public void releaseDefault() {
        this.isDefault = false;
    }

    /** 소유자 확인(IDOR 차단은 service에서 이 결과로 403 처리). */
    public boolean isOwnedBy(Long memberId) {
        return this.memberId.equals(memberId);
    }
}
