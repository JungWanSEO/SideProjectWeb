package com.commerce.api.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.commerce.api.brand.repository.BrandRepository;
import com.commerce.api.category.repository.CategoryRepository;
import com.commerce.api.global.common.PageResponse;
import com.commerce.api.global.exception.BusinessException;
import com.commerce.api.product.dto.ProductCreateRequest;
import com.commerce.api.product.dto.ProductResponse;
import com.commerce.api.product.dto.ProductSearchCondition;
import com.commerce.api.product.dto.ProductCreateRequest.ProductOptionRequest;
import com.commerce.api.product.entity.Product;
import com.commerce.api.product.entity.ProductOption;
import com.commerce.api.product.entity.ProductStatus;
import com.commerce.api.product.repository.ProductRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * ProductService 단위 테스트 (Mockito).
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryRepository categoryRepository;   // enrich/검증에서 사용 (이름 조회·존재검증)
    @Mock
    private BrandRepository brandRepository;

    @InjectMocks
    private ProductService productService;

    private Product productWithId(Long id) {
        Product product = Product.builder()
                .name("반팔티셔츠")
                .price(29000L)
                .description("면 100%")
                .status(ProductStatus.ON_SALE)
                .build();
        ReflectionTestUtils.setField(product, "id", id);
        product.addOption(ProductOption.create("M", 100));   // 재고는 옵션에
        return product;
    }

    @Test
    @DisplayName("상품 등록 성공 - 신규 상품은 ON_SALE 상태로 저장된다")
    void create_success() {
        // given
        ProductCreateRequest request = new ProductCreateRequest(
                "반팔티셔츠", 29000L, "면 100%", null, null, null,
                List.of(new ProductOptionRequest("M", 100)));
        given(productRepository.save(any(Product.class))).willReturn(productWithId(1L));

        // when
        ProductResponse response = productService.create(request);

        // then
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.price()).isEqualTo(29000L);
        assertThat(response.status()).isEqualTo(ProductStatus.ON_SALE);
        assertThat(response.options()).hasSize(1);
        assertThat(response.options().get(0).size()).isEqualTo("M");
        assertThat(response.options().get(0).stock()).isEqualTo(100);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("상품 조회 성공")
    void getProduct_success() {
        // given
        given(productRepository.findById(1L)).willReturn(Optional.of(productWithId(1L)));

        // when
        ProductResponse response = productService.getProduct(1L);

        // then
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("반팔티셔츠");
    }

    @Test
    @DisplayName("상품 목록/검색 - 가시 상태 + 검색조건으로 search 호출해 PageResponse로 변환한다")
    void getProducts_success() {
        // given
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        ProductSearchCondition condition = new ProductSearchCondition("티", 1000L, 5000L, null, null, null);
        Page<Product> page =
                new PageImpl<>(List.of(productWithId(1L), productWithId(2L)), pageable, 2);
        given(productRepository.search(any(), any(), any())).willReturn(page);

        // when
        PageResponse<ProductResponse> response = productService.getProducts(condition, pageable);

        // then - 페이지 메타가 그대로 옮겨지고 엔티티가 DTO로 매핑된다
        assertThat(response.content()).hasSize(2);
        assertThat(response.content().get(0).id()).isEqualTo(1L);
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.totalPages()).isEqualTo(1);
        assertThat(response.hasNext()).isFalse();

        // 가시 상태(ON_SALE·SOLD_OUT) + 받은 조건/페이지를 그대로 리포지토리에 전달한다 (DISCONTINUED 제외)
        verify(productRepository).search(
                List.of(ProductStatus.ON_SALE, ProductStatus.SOLD_OUT), condition, pageable);
    }

    @Test
    @DisplayName("상품 조회 실패 - 없는 상품이면 예외")
    void getProduct_notFound() {
        // given
        given(productRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productService.getProduct(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("상품을 찾을 수 없습니다");
    }
}