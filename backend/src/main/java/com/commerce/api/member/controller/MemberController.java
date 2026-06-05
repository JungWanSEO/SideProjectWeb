package com.commerce.api.member.controller;

import com.commerce.api.global.common.ApiResponse;
import com.commerce.api.member.dto.MemberResponse;
import com.commerce.api.member.dto.MemberSignupRequest;
import com.commerce.api.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회원 API.
 * - POST /api/members      회원가입
 * - GET  /api/members/{id} 단건 조회
 */
@Tag(name = "회원(Member)", description = "회원가입 / 조회 API")
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "회원가입", description = "이메일/비밀번호/닉네임으로 회원을 등록한다. 이메일 중복 시 409.")
    @PostMapping
    public ResponseEntity<ApiResponse<MemberResponse>> signup(
            @Valid @RequestBody MemberSignupRequest request) {
        MemberResponse response = memberService.signup(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("회원가입이 완료되었습니다.", response));
    }

    @Operation(summary = "회원 단건 조회", description = "회원 ID로 회원 정보를 조회한다. 없으면 404.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MemberResponse>> getMember(@PathVariable Long id) {
        MemberResponse response = memberService.getMember(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}