package com.mosiacstore.mosiac.infrastructure.repository;

import com.mosiacstore.mosiac.domain.tourguide.TourGuideImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TourGuideImageRepository extends JpaRepository<TourGuideImage, Long> {
}
