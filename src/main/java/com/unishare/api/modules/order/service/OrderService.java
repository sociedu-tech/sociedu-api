package com.unishare.api.modules.order.service;

import com.unishare.api.common.dto.PageResponse;
import com.unishare.api.modules.order.dto.CheckoutRequest;
import com.unishare.api.modules.order.dto.OrderResponse;
import com.unishare.api.modules.order.dto.OrderSnapshot;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrderService {

    PageResponse<OrderResponse> getMyOrders(UUID buyerId, Pageable pageable);

    PageResponse<OrderResponse> getIncomingOrdersForMentor(UUID mentorId, Pageable pageable);

    OrderResponse getOrderById(UUID orderId, UUID viewerId);

    /** Tạo đơn + URL thanh toán VNPay. */
    OrderResponse checkout(UUID buyerId, CheckoutRequest request, String clientIp);

    /** Tạo URL thanh toán mới cho đơn pending / failed / expired. */
    OrderResponse repay(UUID orderId, UUID buyerId, String clientIp);

    OrderSnapshot getOrderSnapshot(UUID orderId);

    void applyPaymentResult(UUID orderId, boolean success);
}
