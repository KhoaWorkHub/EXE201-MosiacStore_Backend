package com.mosiacstore.mosiac.domain.qrcode;

import com.mosiacstore.mosiac.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "qr_scans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@AttributeOverride(name = "id", column = @Column(name = "scan_id", updatable = false, nullable = false, columnDefinition = "UUID"))
public class QRScan extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qr_id", nullable = false)
    private QRCode qrCode;

    @Column(name = "scan_date", nullable = false)
    private LocalDateTime scanDate;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "geo_location", length = 100)
    private String geoLocation;
}