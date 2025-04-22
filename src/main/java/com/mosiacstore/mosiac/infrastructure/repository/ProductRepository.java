package com.mosiacstore.mosiac.infrastructure.repository;

import com.mosiacstore.mosiac.domain.product.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    Optional<Product> findBySlug(String slug);

    Page<Product> findByActiveTrue(Pageable pageable);

    Page<Product> findByCategoryIdAndActiveTrue(UUID categoryId, Pageable pageable);

    Page<Product> findByRegionIdAndActiveTrue(UUID regionId, Pageable pageable);

    Page<Product> findByFeaturedTrueAndActiveTrue(Pageable pageable);

    // Query with price range
    @Query("SELECT p FROM Product p WHERE p.active = true AND p.price BETWEEN :minPrice AND :maxPrice")
    Page<Product> findByPriceRange(@Param("minPrice") double minPrice, @Param("maxPrice") double maxPrice, Pageable pageable);

    // Query for search by keyword in name or description
    @Query("SELECT p FROM Product p WHERE p.active = true AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Product> searchProducts(@Param("keyword") String keyword, Pageable pageable);

    // Check if slug exists for another product (for validation)
    boolean existsBySlugAndIdNot(String slug, UUID id);
}