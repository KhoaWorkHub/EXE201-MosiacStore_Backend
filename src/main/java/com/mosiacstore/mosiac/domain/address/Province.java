package com.mosiacstore.mosiac.domain.address;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "provinces")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Province {

    @Id
    @Column(name = "province_code", length = 10)
    private String provinceCode;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "region", length = 50)
    private String region;

    @OneToMany(mappedBy = "province")
    private Set<District> districts = new HashSet<>();
}