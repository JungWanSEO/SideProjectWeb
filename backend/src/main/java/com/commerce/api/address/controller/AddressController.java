package com.commerce.api.address.controller;

import com.commerce.api.address.dto.AddressRequest;
import com.commerce.api.address.dto.AddressResponse;
import com.commerce.api.address.service.AddressService;
import com.commerce.api.global.common.ApiResponse;
import com.commerce.api.global.security.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 배송지(주소록) API. 대상 회원은 인증된 로그인 사용자(본인 것만).
 * - GET    /api/addresses                 내 주소 목록(기본배송지 먼저)
 * - POST   /api/addresses                 추가 (첫 주소는 자동 기본배송지)
 * - PUT    /api/addresses/{id}            수정 (주소 내용)
 * - PUT    /api/addresses/{id}/default    기본배송지 지정
 * - DELETE /api/addresses/{id}            삭제 (기본이면 남은 것 중 최신을 승격)
 *
 * 모든 응답 data = 변경 후의 내 주소 목록(FE가 그대로 다시 그리도록 — 장바구니 패턴).
 */
@Tag(name = "배송지(Address)", description = "주소록 CRUD + 기본배송지 지정 API")
@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @Operation(summary = "내 배송지 목록", description = "로그인 사용자의 주소를 기본배송지 먼저 정렬해 조회한다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getMyAddresses() {
        return ResponseEntity.ok(ApiResponse.success(
                addressService.getMyAddresses(SecurityUtil.getCurrentMemberId())));
    }

    @Operation(summary = "배송지 추가", description = "첫 주소는 자동으로 기본배송지가 된다.")
    @PostMapping
    public ResponseEntity<ApiResponse<List<AddressResponse>>> create(
            @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "배송지를 추가했습니다.",
                addressService.create(SecurityUtil.getCurrentMemberId(), request)));
    }

    @Operation(summary = "배송지 수정", description = "주소 내용을 수정한다(기본배송지 지정은 별도 API).")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> update(
            @PathVariable Long id, @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "배송지를 수정했습니다.",
                addressService.update(SecurityUtil.getCurrentMemberId(), id, request)));
    }

    @Operation(summary = "기본배송지 지정", description = "이 주소를 기본배송지로 설정한다(기존 기본은 해제).")
    @PutMapping("/{id}/default")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> setDefault(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "기본배송지로 설정했습니다.",
                addressService.setDefault(SecurityUtil.getCurrentMemberId(), id)));
    }

    @Operation(summary = "배송지 삭제", description = "기본배송지를 삭제하면 남은 주소 중 최신이 기본이 된다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> delete(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "배송지를 삭제했습니다.",
                addressService.delete(SecurityUtil.getCurrentMemberId(), id)));
    }
}
