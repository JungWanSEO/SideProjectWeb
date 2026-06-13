package com.commerce.api.address.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 배송지 생성/수정 요청.
 * 생성과 수정의 입력 필드가 동일해 하나로 공유한다(기본배송지 지정은 별도 엔드포인트라 여기 없음).
 */
@Schema(description = "배송지 생성/수정 요청")
public record AddressRequest(

        @Schema(description = "수령인", example = "홍길동")
        @NotBlank(message = "수령인은 필수입니다.")
        @Size(max = 50, message = "수령인은 50자 이내여야 합니다.")
        String recipient,

        @Schema(description = "연락처", example = "010-1234-5678")
        @NotBlank(message = "연락처는 필수입니다.")
        @Size(max = 30, message = "연락처는 30자 이내여야 합니다.")
        String phone,

        @Schema(description = "우편번호", example = "06236")
        @NotBlank(message = "우편번호는 필수입니다.")
        @Size(max = 10, message = "우편번호는 10자 이내여야 합니다.")
        String zipcode,

        @Schema(description = "기본주소(도로명/지번)", example = "서울 강남구 테헤란로 123")
        @NotBlank(message = "주소는 필수입니다.")
        @Size(max = 200, message = "주소는 200자 이내여야 합니다.")
        String address1,

        @Schema(description = "상세주소(선택)", example = "4층 401호")
        @Size(max = 200, message = "상세주소는 200자 이내여야 합니다.")
        String address2
) {
}
