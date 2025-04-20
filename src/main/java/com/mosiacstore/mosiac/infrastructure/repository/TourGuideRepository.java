package com.mosiacstore.mosiac.infrastructure.repository;

import com.mosiacstore.mosiac.domain.tourguide.TourGuide;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TourGuideRepository extends JpaRepository<TourGuide, Long> {

}
