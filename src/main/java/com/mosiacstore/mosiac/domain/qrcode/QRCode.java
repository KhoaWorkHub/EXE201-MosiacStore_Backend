package com.mosiacstore.mosiac.domain.qrcode;

import com.mosiacstore.mosiac.domain.common.BaseEntity;
import com.mosiacstore.mosiac.domain.product.Product;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "qr_codes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@AttributeOverride(name = "id", column = @Column(name = "qr_id", updatable = false, nullable = false, columnDefinition = "UUID"))
public class QRCode extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "qr_image_url", length = 255)
    private String qrImageUrl;

    @Column(name = "qr_data", length = 500)
    private String qrData;

    @Column(name = "redirect_url", length = 500)
    private String redirectUrl;

    @Column(name = "scan_count")
    private Integer scanCount = 0;

    @Column(name = "active")
    private Boolean active = true;

    @OneToMany(mappedBy = "qrCode", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<QRScan> scans = new HashSet<>();
}