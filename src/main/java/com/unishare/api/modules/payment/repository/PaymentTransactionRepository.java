package com.unishare.api.modules.payment.repository;

import com.unishare.api.modules.payment.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {
    List<PaymentTransaction> findByOrderId(UUID orderId);

    List<PaymentTransaction> findByOrderIdOrderByCreatedAtDesc(UUID orderId);

    Optional<PaymentTransaction> findByProviderTransactionId(String providerTransactionId);
}
