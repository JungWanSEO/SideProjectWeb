package com.commerce.api.brand.service;

import com.commerce.api.brand.dto.BrandCreateRequest;
import com.commerce.api.brand.dto.BrandResponse;
import com.commerce.api.brand.entity.Brand;
import com.commerce.api.brand.repository.BrandRepository;
import com.commerce.api.global.exception.BusinessException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 브랜드 비즈니스 로직 — 목록 조회 / 등록.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BrandService {

    private final BrandRepository brandRepository;

    /** 전체 브랜드 목록. */
    public List<BrandResponse> getBrands() {
        return brandRepository.findAll().stream().map(BrandResponse::from).toList();
    }

    /** 브랜드 등록(ADMIN). 이름이 이미 있으면 409. */
    @Transactional
    public BrandResponse create(BrandCreateRequest request) {
        if (brandRepository.existsByName(request.name())) {
            throw new BusinessException(HttpStatus.CONFLICT, "이미 존재하는 브랜드입니다.");
        }
        return BrandResponse.from(brandRepository.save(Brand.create(request.name())));
    }
}
