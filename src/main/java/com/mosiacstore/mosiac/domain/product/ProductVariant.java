package com.mosiacstore.mosiac.domain.product;

import com.mosiacstore.mosiac.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "product_variants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@AttributeOverride(name = "id", column = @Column(name = "variant_id", updatable = false, nullable = false, columnDefinition = "UUID"))
public class ProductVariant extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "size", length = 10)
    private ProductSize size;

    @Column(name = "color", length = 50)
    private String color;

    @Column(name = "price_adjustment", precision = 10, scale = 2)
    private BigDecimal priceAdjustment = BigDecimal.ZERO;

    @Column(name = "stock_quantity")
    private Integer stockQuantity;

    @Column(name = "sku_variant", length = 60)
    private String skuVariant;

    @Column(name = "active")
    private Boolean active = true;
}