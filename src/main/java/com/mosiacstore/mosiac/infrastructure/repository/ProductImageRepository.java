package com.mosiacstore.mosiac.infrastructure.repository;

import com.mosiacstore.mosiac.domain.product.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {

    List<ProductImage> findByProductId(UUID productId);

    List<ProductImage> findByProductIdOrderByDisplayOrderAsc(UUID productId);

    Optional<ProductImage> findByProductIdAndIsPrimaryTrue(UUID productId);

    @Modifying
    @Query("UPDATE ProductImage pi SET pi.isPrimary = false WHERE pi.product.id = :productId AND pi.id != :imageId")
    void updateNonPrimaryImages(@Param("productId") UUID productId, @Param("imageId") UUID imageId);

    void deleteByProductId(UUID productId);
}