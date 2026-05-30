package com.unishare.api.modules.order.service;

import com.unishare.api.common.dto.PageResponse;
import com.unishare.api.modules.order.dto.CheckoutRequest;
import com.unishare.api.modules.order.dto.OrderResponse;
import com.unishare.api.modules.order.dto.OrderSnapshot;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrderService {

    PageResponse<OrderResponse> getMyOrders(UUID buyerId, Pageable pageable);

    OrderResponse getOrderById(UUID orderId, UUID buyerId);

    /** Tạo đơn + URL thanh toán VNPay. */
    OrderResponse checkout(UUID buyerId, CheckoutRequest request, String clientIp);

    OrderSnapshot getOrderSnapshot(UUID orderId);

    void applyPaymentResult(UUID orderId, boolean success);
}
