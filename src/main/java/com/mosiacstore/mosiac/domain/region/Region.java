package com.mosiacstore.mosiac.domain.region;

import com.mosiacstore.mosiac.domain.common.BaseEntity;
import com.mosiacstore.mosiac.domain.product.Product;
import com.mosiacstore.mosiac.domain.tourguide.TourGuide;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "regions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@AttributeOverride(name = "id", column = @Column(name = "region_id", updatable = false, nullable = false, columnDefinition = "UUID"))
public class Region extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "slug", unique = true, nullable = false, length = 150)
    private String slug;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    @Column(name = "active")
    private Boolean active = true;

    @OneToMany(mappedBy = "region")
    private Set<Product> products = new HashSet<>();

    @OneToMany(mappedBy = "region")
    private Set<TourGuide> tourGuides = new HashSet<>();
}