package com.unishare.api.modules.order.service.impl;

import com.unishare.api.common.constants.OrderStatuses;
import com.unishare.api.common.dto.AppException;
import com.unishare.api.infrastructure.event.DomainEventPublisher;
import com.unishare.api.modules.order.dto.OrderResponse;
import com.unishare.api.modules.order.entity.Order;
import com.unishare.api.modules.order.exception.OrderErrorCode;
import com.unishare.api.modules.order.mapper.OrderMapper;
import com.unishare.api.modules.order.repository.OrderRepository;
import com.unishare.api.modules.payment.dto.PaymentResponse;
import com.unishare.api.modules.payment.service.PaymentService;
import com.unishare.api.modules.service.service.CatalogReadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CatalogReadService catalogReadService;
    @Mock
    private DomainEventPublisher eventPublisher;
    @Mock
    private PaymentService paymentService;

    private OrderServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OrderServiceImpl(
                orderRepository,
                new OrderMapper(),
                catalogReadService,
                eventPublisher,
                paymentService);
        ReflectionTestUtils.setField(service, "paymentExpirationMinutes", 15L);
    }

    @Test
    void getMyOrders_ShouldExpireStalePendingOrders() {
        UUID buyerId = UUID.randomUUID();
        Order stale = order(buyerId, OrderStatuses.PENDING_PAYMENT, Instant.now().minus(20, ChronoUnit.MINUTES));
        when(orderRepository.findByBuyerIdAndStatusAndCreatedAtBefore(
                        eq(buyerId), eq(OrderStatuses.PENDING_PAYMENT), any(Instant.class)))
                .thenReturn(List.of(stale));
        when(orderRepository.findByBuyerId(eq(buyerId), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(stale)));

        service.getMyOrders(buyerId, org.springframework.data.domain.PageRequest.of(0, 20));

        assertEquals(OrderStatuses.EXPIRED, stale.getStatus());
        verify(orderRepository).saveAll(List.of(stale));
    }

    @Test
    void repay_ShouldCreateNewPaymentUrl_ForFailedOrder() {
        UUID buyerId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Order failed = order(buyerId, OrderStatuses.FAILED, Instant.now().minus(1, ChronoUnit.HOURS));
        failed.setId(orderId);
        failed.setTotalAmount(new BigDecimal("150000"));

        when(orderRepository.findByBuyerIdAndStatusAndCreatedAtBefore(any(), any(), any()))
                .thenReturn(List.of());
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(failed));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentResponse pay = new PaymentResponse();
        pay.setPaymentUrl("https://pay.example/vnpay");
        when(paymentService.createPayment(eq(orderId), any(), anyString(), anyString())).thenReturn(pay);

        OrderResponse resp = service.repay(orderId, buyerId, "127.0.0.1");

        assertEquals(OrderStatuses.PENDING_PAYMENT, failed.getStatus());
        assertEquals("https://pay.example/vnpay", resp.getPaymentUrl());
        assertTrue(Boolean.TRUE.equals(resp.getCanPay()));
    }

    @Test
    void repay_ShouldRejectPaidOrder() {
        UUID buyerId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Order paid = order(buyerId, OrderStatuses.PAID, Instant.now());
        paid.setId(orderId);

        when(orderRepository.findByBuyerIdAndStatusAndCreatedAtBefore(any(), any(), any()))
                .thenReturn(List.of());
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(paid));

        AppException ex = assertThrows(AppException.class, () -> service.repay(orderId, buyerId, "127.0.0.1"));
        assertEquals(OrderErrorCode.ORDER_NOT_PAYABLE, ex.getExceptionCode());
        verify(paymentService, never()).createPayment(any(), any(), any(), any());
    }

    @Test
    void applyPaymentResult_ShouldMarkFailed_WhenPendingPayment() {
        UUID orderId = UUID.randomUUID();
        Order pending = order(UUID.randomUUID(), OrderStatuses.PENDING_PAYMENT, Instant.now());
        pending.setId(orderId);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(pending));
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        service.applyPaymentResult(orderId, false);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).saveAndFlush(captor.capture());
        assertEquals(OrderStatuses.FAILED, captor.getValue().getStatus());
    }

    private static Order order(UUID buyerId, String status, Instant createdAt) {
        Order o = new Order();
        o.setBuyerId(buyerId);
        o.setServiceId(UUID.randomUUID());
        o.setStatus(status);
        o.setTotalAmount(new BigDecimal("100000"));
        o.setCreatedAt(createdAt);
        return o;
    }
}
