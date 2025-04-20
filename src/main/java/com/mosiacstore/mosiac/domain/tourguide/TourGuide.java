package com.mosiacstore.mosiac.domain.tourguide;

import com.mosiacstore.mosiac.domain.common.BaseEntity;
import com.mosiacstore.mosiac.domain.region.Region;
import com.mosiacstore.mosiac.domain.product.Product;
import com.mosiacstore.mosiac.domain.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tour_guides")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@AttributeOverride(name = "id", column = @Column(name = "guide_id", updatable = false, nullable = false, columnDefinition = "UUID"))
public class TourGuide extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id")
    private Region region;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "slug", unique = true, nullable = false, length = 250)
    private String slug;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "thumbnail_url", length = 255)
    private String thumbnailUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    @Column(name = "view_count")
    private Integer viewCount = 0;

    @Column(name = "published")
    private Boolean published = false;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "featured")
    private Boolean featured = false;

    @Column(name = "meta_title", length = 200)
    private String metaTitle;

    @Column(name = "meta_description", length = 500)
    private String metaDescription;

    @OneToMany(mappedBy = "tourGuide", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TourGuideImage> images = new HashSet<>();
}