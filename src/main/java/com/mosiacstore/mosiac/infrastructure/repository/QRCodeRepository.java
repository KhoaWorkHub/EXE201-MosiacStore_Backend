package com.mosiacstore.mosiac.infrastructure.repository;

import com.mosiacstore.mosiac.domain.qrcode.QRCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QRCodeRepository extends JpaRepository<QRCode, Long> {
}
