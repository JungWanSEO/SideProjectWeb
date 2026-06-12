package com.commerce.api.address.dto;

import com.commerce.api.address.entity.Address;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 배송지 응답. memberId는 노출하지 않는다(항상 본인 것).
 */
@Schema(description = "배송지 응답")
public record AddressResponse(
        Long id,
        String recipient,
        String phone,
        String zipcode,
        String address1,
        String address2,
        boolean isDefault,
        LocalDateTime createdAt
) {
    public static AddressResponse from(Address a) {
        return new AddressResponse(
                a.getId(), a.getRecipient(), a.getPhone(), a.getZipcode(),
                a.getAddress1(), a.getAddress2(), a.isDefault(), a.getCreatedAt());
    }
}
