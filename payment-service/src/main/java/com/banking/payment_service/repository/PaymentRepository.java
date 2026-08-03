package com.banking.payment_service.repository;

import com.banking.payment_service.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<PaymentEntity, String> {
    Optional<PaymentEntity> findByRazorpayOrderId(String orderId);
}
