package com.mosiacstore.mosiac.infrastructure.repository;

import com.mosiacstore.mosiac.domain.campaign.CampaignRule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignRuleRepository extends JpaRepository<CampaignRule, Long> {
}
