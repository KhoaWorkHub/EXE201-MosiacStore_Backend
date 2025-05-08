package com.mosiacstore.mosiac.infrastructure.repository;

import com.mosiacstore.mosiac.domain.order.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {
    Page<Order> findByUserId(UUID userId, Pageable pageable);
    Optional<Order> findByOrderNumber(String orderNumber);
}