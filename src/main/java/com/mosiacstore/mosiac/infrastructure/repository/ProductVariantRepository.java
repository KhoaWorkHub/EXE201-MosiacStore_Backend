package com.mosiacstore.mosiac.infrastructure.repository;

import com.mosiacstore.mosiac.domain.product.ProductSize;
import com.mosiacstore.mosiac.domain.product.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {

    List<ProductVariant> findByProductId(UUID productId);

    @Query("SELECT v FROM ProductVariant v WHERE v.product.id = :productId AND v.active = true")
    List<ProductVariant> findActiveVariantsByProductId(@Param("productId") UUID productId);

    void deleteByProductId(UUID productId);

    // Check if a combination of product, size, and color already exists
    boolean existsByProductIdAndSizeAndColor(UUID productId, ProductSize size, String color);
}