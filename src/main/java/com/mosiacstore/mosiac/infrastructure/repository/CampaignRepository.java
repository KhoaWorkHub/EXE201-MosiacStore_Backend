package com.mosiacstore.mosiac.infrastructure.repository;

import com.mosiacstore.mosiac.domain.campaign.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignRepository extends JpaRepository<Campaign, Long> {

}
