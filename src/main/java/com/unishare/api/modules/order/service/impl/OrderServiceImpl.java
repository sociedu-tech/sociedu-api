package com.unishare.api.modules.order.service.impl;

import com.unishare.api.common.constants.OrderStatuses;
import com.unishare.api.common.dto.AppException;
import com.unishare.api.common.dto.PageResponse;
import com.unishare.api.common.event.OrderPaidEvent;
import com.unishare.api.common.event.OrderPaymentFailedEvent;
import com.unishare.api.infrastructure.event.DomainEventPublisher;
import com.unishare.api.modules.order.dto.CheckoutRequest;
import com.unishare.api.modules.order.dto.OrderResponse;
import com.unishare.api.modules.order.dto.OrderSnapshot;
import com.unishare.api.modules.order.entity.Order;
import com.unishare.api.modules.order.exception.OrderErrorCode;
import com.unishare.api.modules.order.mapper.OrderMapper;
import com.unishare.api.modules.order.repository.OrderRepository;
import com.unishare.api.modules.order.service.OrderService;
import com.unishare.api.modules.payment.dto.PaymentResponse;
import com.unishare.api.modules.payment.service.PaymentService;
import com.unishare.api.modules.service.entity.ServicePackageVersion;
import com.unishare.api.modules.service.service.CatalogReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final CatalogReadService catalogReadService;
    private final DomainEventPublisher eventPublisher;
    private final PaymentService paymentService;

    public OrderServiceImpl(
            OrderRepository orderRepository,
            OrderMapper orderMapper,
            CatalogReadService catalogReadService,
            DomainEventPublisher eventPublisher,
            @Lazy PaymentService paymentService) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
        this.catalogReadService = catalogReadService;
        this.eventPublisher = eventPublisher;
        this.paymentService = paymentService;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getMyOrders(UUID buyerId, Pageable pageable) {
        return PageResponse.of(orderRepository.findByBuyerId(buyerId, pageable).map(orderMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID orderId, UUID buyerId) {
        Order order = orderRepository.findById(orderId)
                .filter(o -> o.getBuyerId().equals(buyerId))
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse checkout(UUID buyerId, CheckoutRequest request, String clientIp) {
        ServicePackageVersion ver = catalogReadService.requireActiveVersion(request.getServicePackageVersionId());
        Order order = new Order();
        order.setBuyerId(buyerId);
        order.setServiceId(ver.getId());
        order.setTotalAmount(ver.getPrice());
        order.setStatus(OrderStatuses.PENDING_PAYMENT);
        order = orderRepository.save(order);

        String orderInfo = request.getOrderInfo() != null && !request.getOrderInfo().isBlank()
                ? request.getOrderInfo()
                : "Unishare order #" + order.getId();
        PaymentResponse pay = paymentService.createPayment(order.getId(), ver.getPrice(), orderInfo, clientIp);

        OrderResponse resp = orderMapper.toResponse(order);
        resp.setPaymentUrl(pay.getPaymentUrl());
        resp.setMockPayment(pay.getMockPayment());
        return resp;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderSnapshot getOrderSnapshot(UUID orderId) {
        Order o = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));
        return new OrderSnapshot(o.getId(), o.getBuyerId(), o.getServiceId(), o.getStatus(), o.getTotalAmount());
    }

    @Override
    @Transactional
    public void applyPaymentResult(UUID orderId, boolean success) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));
        if (success) {
            order.setStatus(OrderStatuses.PAID);
            order.setPaidAt(Instant.now());
        } else {
            order.setStatus(OrderStatuses.FAILED);
        }
        orderRepository.save(order);
        if (success) {
            eventPublisher.publish(new OrderPaidEvent(orderId, order.getBuyerId()));
        } else {
            eventPublisher.publish(new OrderPaymentFailedEvent(orderId, order.getBuyerId()));
        }
    }
}
