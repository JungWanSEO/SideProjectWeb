package com.commerce.api.category.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.commerce.api.category.dto.CategoryCreateRequest;
import com.commerce.api.category.dto.CategoryResponse;
import com.commerce.api.category.entity.Category;
import com.commerce.api.category.repository.CategoryRepository;
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
 * CategoryService 단위 테스트 (Mockito).
 */
@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;
    @InjectMocks
    private CategoryService categoryService;

    private Category categoryWithId(Long id, String name) {
        Category c = Category.create(name);
        ReflectionTestUtils.setField(c, "id", id);   // DB가 채울 id를 테스트에서 강제 주입
        return c;
    }

    @Test
    @DisplayName("카테고리 등록 성공")
    void create_success() {
        given(categoryRepository.existsByName("상의")).willReturn(false);
        given(categoryRepository.save(any(Category.class))).willReturn(categoryWithId(1L, "상의"));

        CategoryResponse response = categoryService.create(new CategoryCreateRequest("상의"));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("상의");
    }

    @Test
    @DisplayName("카테고리 등록 실패 - 이름 중복이면 409, 저장하지 않음")
    void create_duplicate() {
        given(categoryRepository.existsByName("상의")).willReturn(true);

        assertThatThrownBy(() -> categoryService.create(new CategoryCreateRequest("상의")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("이미 존재");
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("카테고리 목록 조회 - DTO로 매핑")
    void getCategories_maps() {
        given(categoryRepository.findAll())
                .willReturn(List.of(categoryWithId(1L, "상의"), categoryWithId(2L, "하의")));

        List<CategoryResponse> result = categoryService.getCategories();

        assertThat(result).extracting(CategoryResponse::name).containsExactly("상의", "하의");
    }
}
