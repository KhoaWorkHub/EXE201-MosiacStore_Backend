package com.mosiacstore.mosiac.infrastructure.repository;

import com.mosiacstore.mosiac.domain.cart.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartRepository extends JpaRepository<Cart, UUID> {
    Optional<Cart> findByUserId(UUID userId);

    Optional<Cart> findByGuestId(String guestId);

    List<Cart> findAllByUserIdOrderByUpdatedAtDesc(UUID userId);

    @Query("SELECT c FROM Cart c WHERE c.expiredAt < :now")
    List<Cart> findExpiredCarts(@Param("now") LocalDateTime now);

    @Query("SELECT c FROM Cart c WHERE c.updatedAt < :timestamp AND SIZE(c.items) > 0")
    List<Cart> findCartsWithItemsNotUpdatedSince(@Param("timestamp") LocalDateTime timestamp);
}
