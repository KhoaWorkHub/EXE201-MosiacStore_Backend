package com.mosiacstore.mosiac.infrastructure.repository;

import com.mosiacstore.mosiac.domain.campaign.CampaignUsage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignUsageRepository extends JpaRepository<CampaignUsage, Long> {
}
