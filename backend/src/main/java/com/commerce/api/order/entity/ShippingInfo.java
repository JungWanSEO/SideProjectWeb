package com.commerce.api.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 배송 정보 — 주문(Order)에 임베드되는 값 객체(스냅샷).
 *
 * <p>주소록(Address)에서 고른 주소를 주문 시점에 <b>복사해 보존</b>한다(가격/이름 스냅샷과 같은 이유 —
 * 나중에 주소록을 수정·삭제해도 과거 주문의 배송지는 그대로). 그래서 주문은 addressId를 들고 있지 않다.
 * 모든 컬럼은 nullable: 배송지 없이 만든 주문(명시적 POST /api/orders)도 허용하기 위함.
 *
 * <p>@Embeddable: 별도 테이블이 아니라 orders 테이블의 컬럼들로 펼쳐진다(.NET의 owned/value object와 유사).
 */
@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShippingInfo {

    @Column(length = 50)
    private String recipient;

    @Column(length = 30)
    private String phone;

    @Column(length = 10)
    private String zipcode;

    @Column(length = 200)
    private String address1;

    @Column(length = 200)
    private String address2;

    @Column(name = "delivery_memo", length = 200)
    private String deliveryMemo;   // 배송 요청사항(선택)

    private ShippingInfo(String recipient, String phone, String zipcode,
                         String address1, String address2, String deliveryMemo) {
        this.recipient = recipient;
        this.phone = phone;
        this.zipcode = zipcode;
        this.address1 = address1;
        this.address2 = address2;
        this.deliveryMemo = deliveryMemo;
    }

    public static ShippingInfo of(String recipient, String phone, String zipcode,
                                  String address1, String address2, String deliveryMemo) {
        return new ShippingInfo(recipient, phone, zipcode, address1, address2, deliveryMemo);
    }
}
