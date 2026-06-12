package com.commerce.api.address.service;

import com.commerce.api.address.dto.AddressRequest;
import com.commerce.api.address.dto.AddressResponse;
import com.commerce.api.address.entity.Address;
import com.commerce.api.address.repository.AddressRepository;
import com.commerce.api.global.exception.BusinessException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 배송지(주소록) 비즈니스 로직.
 *
 * <p>"회원당 기본배송지 1개" 불변식을 여기서 보장한다:
 * ① 첫 주소는 자동 기본 ② set-default 시 기존 기본 해제 후 단일화 ③ 기본 삭제 시 남은 것 중 최신을 승격.
 * 모든 단건 작업은 소유권(IDOR)을 검증한다 — 본인 것 아니면 403 (Order의 findOwnedOrder 패턴).
 * 변경 메서드는 변경 후 "내 주소 목록"을 돌려준다(FE가 그대로 다시 그리도록 — Cart 패턴).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AddressService {

    private final AddressRepository addressRepository;

    /**
     * 본인 소유 주소를 DTO로 조회 — 다른 도메인(주문 등)이 배송지 스냅샷을 만들 때 쓴다.
     * 엔티티 대신 DTO를 돌려줘 도메인 경계를 지킨다(settlement→payment와 같은 방식). 없으면 404·남의 것 403.
     */
    public AddressResponse getOwnedAddress(Long memberId, Long addressId) {
        return AddressResponse.from(findOwned(memberId, addressId));
    }

    /** 내 주소 목록(기본배송지 먼저, 그다음 최신순). */
    public List<AddressResponse> getMyAddresses(Long memberId) {
        return addressRepository.findByMemberIdOrderByIsDefaultDescCreatedAtDesc(memberId).stream()
                .map(AddressResponse::from)
                .toList();
    }

    /** 추가. 회원의 첫 주소면 자동으로 기본배송지가 된다. */
    @Transactional
    public List<AddressResponse> create(Long memberId, AddressRequest request) {
        Address address = Address.create(memberId, request.recipient(), request.phone(),
                request.zipcode(), request.address1(), request.address2());
        if (addressRepository.countByMemberId(memberId) == 0) {
            address.markDefault();   // 첫 주소 = 기본배송지
        }
        addressRepository.save(address);
        return getMyAddresses(memberId);
    }

    /** 수정(주소 내용만 — 기본배송지 설정은 setDefault). */
    @Transactional
    public List<AddressResponse> update(Long memberId, Long addressId, AddressRequest request) {
        Address address = findOwned(memberId, addressId);
        address.update(request.recipient(), request.phone(), request.zipcode(),
                request.address1(), request.address2());
        return getMyAddresses(memberId);   // 영속 엔티티라 dirty-checking으로 flush
    }

    /** 삭제. 기본배송지를 지우면 남은 주소 중 최신을 기본으로 승격한다. */
    @Transactional
    public List<AddressResponse> delete(Long memberId, Long addressId) {
        Address address = findOwned(memberId, addressId);
        boolean wasDefault = address.isDefault();
        addressRepository.delete(address);

        if (wasDefault) {
            // 삭제를 먼저 DB에 반영한 뒤 남은 것 중 최신을 조회(삭제 대상이 후보에 끼지 않도록).
            addressRepository.flush();
            addressRepository.findFirstByMemberIdOrderByCreatedAtDesc(memberId)
                    .ifPresent(Address::markDefault);
        }
        return getMyAddresses(memberId);
    }

    /** 기본배송지 지정. 기존 기본은 해제하고 이 주소를 기본으로(회원당 1개 불변식). */
    @Transactional
    public List<AddressResponse> setDefault(Long memberId, Long addressId) {
        Address target = findOwned(memberId, addressId);
        addressRepository.findByMemberIdAndIsDefaultTrue(memberId)
                .filter(current -> !current.getId().equals(target.getId()))
                .ifPresent(Address::releaseDefault);
        target.markDefault();
        return getMyAddresses(memberId);
    }

    /** 본인 소유 주소 조회 — 없으면 404, 남의 것이면 403(IDOR 차단). */
    private Address findOwned(Long memberId, Long addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "배송지를 찾을 수 없습니다. (id: " + addressId + ")"));
        if (!address.isOwnedBy(memberId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "본인의 배송지만 접근할 수 있습니다.");
        }
        return address;
    }
}
