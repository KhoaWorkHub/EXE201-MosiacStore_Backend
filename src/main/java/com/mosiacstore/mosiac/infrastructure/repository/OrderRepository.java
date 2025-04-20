package com.mosiacstore.mosiac.infrastructure.repository;

import com.mosiacstore.mosiac.domain.order.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

}
