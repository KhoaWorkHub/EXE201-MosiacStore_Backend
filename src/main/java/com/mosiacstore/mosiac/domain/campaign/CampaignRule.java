package com.mosiacstore.mosiac.domain.campaign;

import com.mosiacstore.mosiac.domain.common.BaseEntity;
import jakarta.persistence.*;


@Entity
@Table(name = "campaign_rules")
public class CampaignRule extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false)
    private RuleType ruleType;

    @Column(name = "value", nullable = false)
    private String value;

    @Column(name = "operator")
    private String operator;
}
