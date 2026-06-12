package com.commerce.api.cart.service;

import com.commerce.api.cart.dto.CartItemAddRequest;
import com.commerce.api.cart.dto.CartItemUpdateRequest;
import com.commerce.api.cart.dto.CartResponse;
import com.commerce.api.cart.dto.CartResponse.CartItemResponse;
import com.commerce.api.cart.entity.Cart;
import com.commerce.api.cart.entity.CartItem;
import com.commerce.api.cart.repository.CartRepository;
import com.commerce.api.global.exception.BusinessException;
import com.commerce.api.product.entity.Product;
import com.commerce.api.product.entity.ProductOption;
import com.commerce.api.product.repository.ProductRepository;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 장바구니 비즈니스 로직.
 * 항목은 상품·옵션(사이즈)을 ID로 참조하고, 조회 시 현재 상품/옵션 정보(이름·가격·사이즈·재고)를
 * 일괄 조회해 채운다(라이브 참조 — 주문의 스냅샷과 대비).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    /**
     * 옵션(사이즈) 담기 (장바구니 없으면 생성). 같은 옵션이면 수량 증가.
     * 재고는 여기서 막지 않는다(라이브/위시 성격) — 부족 검증은 주문 시점. 옵션 존재만 검증(없으면 404).
     */
    @Transactional
    public CartResponse addItem(Long memberId, CartItemAddRequest request) {
        // 옵션 ID로 소속 상품(애그리거트 루트)을 조회 — 옵션 존재 검증 + productId 확보 (주문과 동일 패턴)
        Product product = productRepository.findByOptionId(request.optionId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "옵션을 찾을 수 없습니다. (id: " + request.optionId() + ")"));

        Cart cart = cartRepository.findByMemberId(memberId)
                .orElseGet(() -> Cart.create(memberId));
        cart.addItem(product.getId(), request.optionId(), request.quantity());

        return toResponse(cartRepository.save(cart));
    }

    /** 장바구니 조회 (없으면 빈 장바구니로 응답) */
    public CartResponse getCart(Long memberId) {
        return cartRepository.findByMemberId(memberId)
                .map(this::toResponse)
                .orElseGet(() -> new CartResponse(memberId, List.of(), 0));
    }

    /**
     * 항목 수량 변경 (옵션 단위, 절대값). 담기(가산)와 달리 덮어쓴다.
     * 재고는 여기서 막지 않는다(담기와 동일한 라이브 성격) — 부족 검증은 주문/결제 시점.
     * cart는 이미 영속 상태라 dirty checking으로 flush된다(save 불필요 — removeItem과 동일).
     */
    @Transactional
    public CartResponse changeQuantity(Long memberId, Long optionId, CartItemUpdateRequest request) {
        Cart cart = cartRepository.findByMemberId(memberId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "장바구니가 없습니다."));
        cart.updateItemQuantity(optionId, request.quantity());
        return toResponse(cart);
    }

    /** 항목 제거 (옵션 단위) */
    @Transactional
    public CartResponse removeItem(Long memberId, Long optionId) {
        Cart cart = cartRepository.findByMemberId(memberId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "장바구니가 없습니다."));
        cart.removeItem(optionId);
        return toResponse(cart);
    }

    /** 항목들을 현재 상품/옵션 정보로 채워 응답 생성. 상품은 한 번에 조회(N+1 회피). */
    private CartResponse toResponse(Cart cart) {
        List<CartItem> items = cart.getCartItems();

        // 현재 상품 정보를 productId로 한 번에 조회(배치). 옵션(사이즈·재고)은 각 상품의 options에서 찾는다
        // — product.getOptions()는 지연 로딩이지만 default_batch_fetch_size로 IN 쿼리 한 방에 묶인다.
        List<Long> productIds = items.stream().map(CartItem::getProductId).toList();
        Map<Long, Product> productMap = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        List<CartItemResponse> itemResponses = items.stream()
                .map(item -> toItemResponse(item, productMap.get(item.getProductId())))
                .toList();

        int totalQuantity = items.stream().mapToInt(CartItem::getQuantity).sum();
        return new CartResponse(cart.getMemberId(), itemResponses, totalQuantity);
    }

    /** 항목 1건을 현재 상품/옵션 정보로 채운다. 상품·옵션이 사라졌어도 안전한 기본값으로 응답. */
    private CartItemResponse toItemResponse(CartItem item, Product product) {
        if (product == null) {
            // 상품 자체가 삭제됨 → 이름만 placeholder, 나머지는 0/품절 처리
            return new CartItemResponse(item.getProductId(), item.getOptionId(),
                    "(삭제된 상품)", "-", 0L, item.getQuantity(), 0L, 0, true);
        }
        // 상품의 옵션들 중 이 항목의 옵션(사이즈)을 찾는다
        ProductOption option = product.getOptions().stream()
                .filter(o -> o.getId().equals(item.getOptionId()))
                .findFirst().orElse(null);

        String size = option != null ? option.getSize() : "(삭제된 옵션)";
        int stock = option != null ? option.getStock() : 0;
        boolean soldOut = option == null || option.isSoldOut();  // 옵션이 사라졌으면 품절로 간주
        long price = product.getPrice();

        return new CartItemResponse(
                item.getProductId(), item.getOptionId(),
                product.getName(), size, price,
                item.getQuantity(), price * item.getQuantity(),
                stock, soldOut);
    }
}