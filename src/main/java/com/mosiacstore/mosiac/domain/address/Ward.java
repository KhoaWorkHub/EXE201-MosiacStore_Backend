package com.mosiacstore.mosiac.domain.address;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "wards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Ward {

    @Id
    @Column(name = "ward_code", length = 10)
    private String wardCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_code", referencedColumnName = "district_code", nullable = false)
    private District district;

    @Column(name = "name", nullable = false, length = 100)
    private String name;
}