package com.mosiacstore.mosiac.domain.campaign;

import com.mosiacstore.mosiac.domain.common.BaseEntity;
import com.mosiacstore.mosiac.domain.user.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "campaign_usages")
public class CampaignUsage extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "usage_date", nullable = false)
    private LocalDateTime usageDate;

    @Column(name = "discount_amount")
    private BigDecimal discountAmount;
}
