package com.commerce.api.address.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.commerce.api.address.dto.AddressRequest;
import com.commerce.api.address.entity.Address;
import com.commerce.api.address.repository.AddressRepository;
import com.commerce.api.global.exception.BusinessException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * AddressService 단위 테스트.
 * 핵심 = "회원당 기본배송지 1개" 불변식(첫 주소 자동 기본 · set-default 단일화 · 기본 삭제 시 승격) + 소유권(IDOR).
 */
@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    @Mock
    private AddressRepository addressRepository;

    @InjectMocks
    private AddressService addressService;

    private AddressRequest sampleRequest() {
        return new AddressRequest("홍길동", "010-1234-5678", "06236", "서울 강남구 테헤란로 123", "4층");
    }

    /** id를 가진 주소 생성(JPA 자동생성 id를 리플렉션으로 주입). */
    private Address addressWithId(Long id, Long memberId, boolean isDefault) {
        Address a = Address.create(memberId, "수령인", "010-0000-0000", "00000", "주소", "상세");
        ReflectionTestUtils.setField(a, "id", id);
        if (isDefault) {
            a.markDefault();
        }
        return a;
    }

    @Test
    @DisplayName("추가 - 회원의 첫 주소는 자동으로 기본배송지가 된다")
    void create_firstAddress_becomesDefault() {
        // given: 기존 주소 0개
        given(addressRepository.countByMemberId(100L)).willReturn(0L);
        given(addressRepository.save(any(Address.class))).willAnswer(inv -> inv.getArgument(0));
        given(addressRepository.findByMemberIdOrderByIsDefaultDescCreatedAtDesc(100L)).willReturn(List.of());

        // when
        addressService.create(100L, sampleRequest());

        // then: 저장된 엔티티가 기본배송지 + 입력 내용 반영
        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(addressRepository).save(captor.capture());
        assertThat(captor.getValue().isDefault()).isTrue();
        assertThat(captor.getValue().getRecipient()).isEqualTo("홍길동");
        assertThat(captor.getValue().getMemberId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("추가 - 두 번째 주소는 기본배송지가 아니다")
    void create_secondAddress_notDefault() {
        // given: 기존 주소 1개
        given(addressRepository.countByMemberId(100L)).willReturn(1L);
        given(addressRepository.save(any(Address.class))).willAnswer(inv -> inv.getArgument(0));
        given(addressRepository.findByMemberIdOrderByIsDefaultDescCreatedAtDesc(100L)).willReturn(List.of());

        // when
        addressService.create(100L, sampleRequest());

        // then
        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(addressRepository).save(captor.capture());
        assertThat(captor.getValue().isDefault()).isFalse();
    }

    @Test
    @DisplayName("수정 - 본인 주소면 내용이 바뀐다(기본배송지 여부는 불변)")
    void update_success() {
        Address mine = addressWithId(1L, 100L, true);
        given(addressRepository.findById(1L)).willReturn(Optional.of(mine));
        given(addressRepository.findByMemberIdOrderByIsDefaultDescCreatedAtDesc(100L)).willReturn(List.of(mine));

        addressService.update(100L, 1L, new AddressRequest("김철수", "010-9999-8888", "12345", "부산 해운대구", null));

        assertThat(mine.getRecipient()).isEqualTo("김철수");
        assertThat(mine.getZipcode()).isEqualTo("12345");
        assertThat(mine.getAddress2()).isNull();
        assertThat(mine.isDefault()).isTrue();   // 수정은 기본배송지 여부를 건드리지 않음
    }

    @Test
    @DisplayName("수정 실패 - 남의 주소면 403")
    void update_notOwner_forbidden() {
        Address others = addressWithId(1L, 999L, false);   // 다른 회원 소유
        given(addressRepository.findById(1L)).willReturn(Optional.of(others));

        assertThatThrownBy(() -> addressService.update(100L, 1L, sampleRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("본인의 배송지만");
    }

    @Test
    @DisplayName("수정 실패 - 없는 주소면 404")
    void update_notFound() {
        given(addressRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> addressService.update(100L, 99L, sampleRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("배송지를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("기본배송지 지정 - 기존 기본은 해제되고 대상이 기본이 된다")
    void setDefault_unsetsPreviousAndSetsTarget() {
        Address current = addressWithId(1L, 100L, true);    // 현재 기본
        Address target = addressWithId(2L, 100L, false);    // 새로 기본 지정할 대상
        given(addressRepository.findById(2L)).willReturn(Optional.of(target));
        given(addressRepository.findByMemberIdAndIsDefaultTrue(100L)).willReturn(Optional.of(current));
        given(addressRepository.findByMemberIdOrderByIsDefaultDescCreatedAtDesc(100L))
                .willReturn(List.of(target, current));

        addressService.setDefault(100L, 2L);

        assertThat(current.isDefault()).isFalse();
        assertThat(target.isDefault()).isTrue();
    }

    @Test
    @DisplayName("삭제 - 기본배송지를 지우면 남은 주소 중 최신이 기본으로 승격된다")
    void delete_defaultPromotesLatest() {
        Address toDelete = addressWithId(1L, 100L, true);   // 지울 기본배송지
        Address remaining = addressWithId(2L, 100L, false); // 남는 주소
        given(addressRepository.findById(1L)).willReturn(Optional.of(toDelete));
        given(addressRepository.findFirstByMemberIdOrderByCreatedAtDesc(100L)).willReturn(Optional.of(remaining));
        given(addressRepository.findByMemberIdOrderByIsDefaultDescCreatedAtDesc(100L)).willReturn(List.of(remaining));

        addressService.delete(100L, 1L);

        verify(addressRepository).delete(toDelete);
        assertThat(remaining.isDefault()).isTrue();   // 승격됨
    }

    @Test
    @DisplayName("삭제 실패 - 남의 주소면 403")
    void delete_notOwner_forbidden() {
        Address others = addressWithId(1L, 999L, true);
        given(addressRepository.findById(1L)).willReturn(Optional.of(others));

        assertThatThrownBy(() -> addressService.delete(100L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("본인의 배송지만");
    }
}
