package com.mosiacstore.mosiac.infrastructure.repository;

import com.mosiacstore.mosiac.domain.product.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

}
