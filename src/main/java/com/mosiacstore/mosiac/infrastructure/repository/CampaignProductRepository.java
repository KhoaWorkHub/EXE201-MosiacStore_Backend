package com.mosiacstore.mosiac.infrastructure.repository;

import com.mosiacstore.mosiac.domain.campaign.CampaignProduct;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignProductRepository extends JpaRepository<CampaignProduct, Long> {
}
