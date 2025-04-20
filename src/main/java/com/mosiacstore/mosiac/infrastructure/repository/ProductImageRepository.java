package com.mosiacstore.mosiac.infrastructure.repository;

import com.mosiacstore.mosiac.domain.product.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

}
