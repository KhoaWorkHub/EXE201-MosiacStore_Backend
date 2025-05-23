package com.mosiacstore.mosiac.infrastructure.repository;

import com.mosiacstore.mosiac.domain.order.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

}
