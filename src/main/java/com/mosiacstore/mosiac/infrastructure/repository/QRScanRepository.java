package com.mosiacstore.mosiac.infrastructure.repository;

import com.mosiacstore.mosiac.domain.qrcode.QRScan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QRScanRepository  extends JpaRepository<QRScan, Long> {

}
