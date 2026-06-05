package com.commerce.api.brand.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.commerce.api.brand.dto.BrandCreateRequest;
import com.commerce.api.brand.dto.BrandResponse;
import com.commerce.api.brand.entity.Brand;
import com.commerce.api.brand.repository.BrandRepository;
import com.commerce.api.global.exception.BusinessException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * BrandService 단위 테스트 (Mockito).
 */
@ExtendWith(MockitoExtension.class)
class BrandServiceTest {

    @Mock
    private BrandRepository brandRepository;
    @InjectMocks
    private BrandService brandService;

    private Brand brandWithId(Long id, String name) {
        Brand b = Brand.create(name);
        ReflectionTestUtils.setField(b, "id", id);
        return b;
    }

    @Test
    @DisplayName("브랜드 등록 성공")
    void create_success() {
        given(brandRepository.existsByName("Nike")).willReturn(false);
        given(brandRepository.save(any(Brand.class))).willReturn(brandWithId(1L, "Nike"));

        BrandResponse response = brandService.create(new BrandCreateRequest("Nike"));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Nike");
    }

    @Test
    @DisplayName("브랜드 등록 실패 - 이름 중복이면 409, 저장하지 않음")
    void create_duplicate() {
        given(brandRepository.existsByName("Nike")).willReturn(true);

        assertThatThrownBy(() -> brandService.create(new BrandCreateRequest("Nike")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("이미 존재");
        verify(brandRepository, never()).save(any());
    }

    @Test
    @DisplayName("브랜드 목록 조회 - DTO로 매핑")
    void getBrands_maps() {
        given(brandRepository.findAll())
                .willReturn(List.of(brandWithId(1L, "Nike"), brandWithId(2L, "Adidas")));

        List<BrandResponse> result = brandService.getBrands();

        assertThat(result).extracting(BrandResponse::name).containsExactly("Nike", "Adidas");
    }
}
