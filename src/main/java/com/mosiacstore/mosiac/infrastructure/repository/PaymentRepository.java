package com.mosiacstore.mosiac.infrastructure.repository;

import com.mosiacstore.mosiac.domain.payment.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByOrderId(java.util.UUID orderId);
}
