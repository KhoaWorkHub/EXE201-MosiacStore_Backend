package com.mosiacstore.mosiac.domain.campaign;

import com.mosiacstore.mosiac.domain.common.BaseEntity;
import com.mosiacstore.mosiac.domain.product.Product;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "campaign_products")
public class CampaignProduct extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "discount_value")
    private BigDecimal discountValue;

    @Column(name = "is_gift")
    private Boolean isGift = false;
}
