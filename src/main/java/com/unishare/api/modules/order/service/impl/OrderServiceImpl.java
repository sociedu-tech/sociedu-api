package com.unishare.api.modules.order.service.impl;

import com.unishare.api.common.constants.OrderStatuses;
import com.unishare.api.common.dto.AppException;
import com.unishare.api.common.dto.PageResponse;
import com.unishare.api.common.event.OrderCheckoutCreatedEvent;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final CatalogReadService catalogReadService;
    private final DomainEventPublisher eventPublisher;
    private final PaymentService paymentService;

    @Value("${app.order.payment-expiration-minutes:15}")
    private long paymentExpirationMinutes;

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
    @Transactional
    public PageResponse<OrderResponse> getMyOrders(UUID buyerId, Pageable pageable) {
        expireStaleOrders(buyerId);
        return PageResponse.of(orderRepository.findByBuyerId(buyerId, pageable)
                .map(order -> enrichResponse(orderMapper.toResponse(order), order)));
    }

    @Override
    @Transactional
    public PageResponse<OrderResponse> getIncomingOrdersForMentor(UUID mentorId, Pageable pageable) {
        return PageResponse.of(orderRepository.findIncomingForMentor(mentorId, pageable)
                .map(order -> enrichResponse(orderMapper.toResponse(order), order)));
    }

    @Override
    @Transactional
    public OrderResponse getOrderById(UUID orderId, UUID viewerId) {
        expireStaleOrders(viewerId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));
        if (!canViewOrder(order, viewerId)) {
            throw new AppException(OrderErrorCode.ORDER_NOT_FOUND);
        }
        return enrichResponse(orderMapper.toResponse(order), order);
    }

    private boolean canViewOrder(Order order, UUID viewerId) {
        if (order.getBuyerId().equals(viewerId)) {
            return true;
        }
        try {
            var ctx = catalogReadService.resolvePurchaseContext(order.getServiceId());
            return ctx.mentorId().equals(viewerId);
        } catch (AppException e) {
            return false;
        }
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

        var purchaseCtx = catalogReadService.resolvePurchaseContext(ver.getId());
        eventPublisher.publish(new OrderCheckoutCreatedEvent(
                order.getId(),
                buyerId,
                purchaseCtx.mentorId(),
                ver.getId(),
                ver.getPrice()));

        String orderInfo = request.getOrderInfo() != null && !request.getOrderInfo().isBlank()
                ? request.getOrderInfo()
                : "Unishare order #" + order.getId();
        PaymentResponse pay = paymentService.createPayment(order.getId(), ver.getPrice(), orderInfo, clientIp);

        OrderResponse resp = enrichResponse(orderMapper.toResponse(order), order);
        resp.setPaymentUrl(pay.getPaymentUrl());
        return resp;
    }

    @Override
    @Transactional
    public OrderResponse repay(UUID orderId, UUID buyerId, String clientIp) {
        expireStaleOrders(buyerId);
        Order order = orderRepository.findById(orderId)
                .filter(o -> o.getBuyerId().equals(buyerId))
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));

        if (!isPayableStatus(order.getStatus())) {
            throw new AppException(OrderErrorCode.ORDER_NOT_PAYABLE, "Đơn không thể thanh toán ở trạng thái hiện tại");
        }

        if (OrderStatuses.FAILED.equals(order.getStatus()) || OrderStatuses.EXPIRED.equals(order.getStatus())) {
            order.setStatus(OrderStatuses.PENDING_PAYMENT);
        }

        order.setCreatedAt(Instant.now());
        order = orderRepository.save(order);

        String orderInfo = "Unishare order #" + order.getId();
        PaymentResponse pay = paymentService.createPayment(order.getId(), order.getTotalAmount(), orderInfo, clientIp);

        OrderResponse resp = enrichResponse(orderMapper.toResponse(order), order);
        resp.setPaymentUrl(pay.getPaymentUrl());
        log.info("Repay initiated for orderId={} buyerId={}", orderId, buyerId);
        return resp;
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public OrderSnapshot getOrderSnapshot(UUID orderId) {
        Order o = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));
        return new OrderSnapshot(o.getId(), o.getBuyerId(), o.getServiceId(), o.getStatus(), o.getTotalAmount());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean applyPaymentResult(UUID orderId, boolean success) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));
        if (OrderStatuses.PAID.equals(order.getStatus()) || OrderStatuses.REFUNDED.equals(order.getStatus())) {
            log.info("applyPaymentResult skipped orderId={} status={} (already settled)", orderId, order.getStatus());
            return false;
        }
        if (success) {
            order.setStatus(OrderStatuses.PAID);
            order.setPaidAt(Instant.now());
        } else if (OrderStatuses.PENDING_PAYMENT.equals(order.getStatus())) {
            order.setStatus(OrderStatuses.FAILED);
        }
        order = orderRepository.saveAndFlush(order);
        if (success) {
            log.info("applyPaymentResult marked orderId={} as paid (persisted status={})", orderId, order.getStatus());
            return true;
        }
        eventPublisher.publish(new OrderPaymentFailedEvent(orderId, order.getBuyerId()));
        return false;
    }

    private void expireStaleOrders(UUID buyerId) {
        Instant cutoff = Instant.now().minus(paymentExpirationMinutes, ChronoUnit.MINUTES);
        List<Order> stale = orderRepository.findByBuyerIdAndStatusAndCreatedAtBefore(
                buyerId, OrderStatuses.PENDING_PAYMENT, cutoff);
        if (stale.isEmpty()) {
            return;
        }
        for (Order order : stale) {
            order.setStatus(OrderStatuses.EXPIRED);
        }
        orderRepository.saveAll(stale);
        log.info("Expired {} stale pending order(s) for buyerId={}", stale.size(), buyerId);
    }

    private static boolean isPayableStatus(String status) {
        return OrderStatuses.PENDING_PAYMENT.equals(status)
                || OrderStatuses.FAILED.equals(status)
                || OrderStatuses.EXPIRED.equals(status);
    }

    private OrderResponse enrichResponse(OrderResponse response, Order order) {
        if (response == null || order == null) {
            return response;
        }
        boolean canPay = isPayableStatus(order.getStatus());
        response.setCanPay(canPay);
        if (OrderStatuses.PENDING_PAYMENT.equals(order.getStatus()) && order.getCreatedAt() != null) {
            response.setPaymentExpiresAt(order.getCreatedAt().plus(paymentExpirationMinutes, ChronoUnit.MINUTES));
        } else {
            response.setPaymentExpiresAt(null);
        }
        try {
            var ctx = catalogReadService.resolvePurchaseContext(order.getServiceId());
            var pkg = catalogReadService.getPackage(ctx.packageId());
            response.setPackageName(pkg.getName());
            response.setMentorId(ctx.mentorId());
        } catch (AppException ignored) {
            // keep partial response
        }
        if (order.getBuyerId() != null) {
            String id = order.getBuyerId().toString();
            response.setBuyerLabel("Học viên #" + id.substring(0, Math.min(8, id.length())));
        }
        return response;
    }
}
