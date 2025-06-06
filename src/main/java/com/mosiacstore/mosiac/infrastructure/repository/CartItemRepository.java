package com.mosiacstore.mosiac.infrastructure.repository;

import com.mosiacstore.mosiac.domain.cart.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

}
