package com.mosiacstore.mosiac.domain.address;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "administrative_units")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdministrativeUnit {

    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "full_name_en")
    private String fullNameEn;

    @Column(name = "short_name")
    private String shortName;

    @Column(name = "short_name_en")
    private String shortNameEn;

    @Column(name = "code_name")
    private String codeName;

    @Column(name = "code_name_en")
    private String codeNameEn;

    @OneToMany(mappedBy = "administrativeUnit")
    private Set<Province> provinces = new HashSet<>();

    @OneToMany(mappedBy = "administrativeUnit")
    private Set<District> districts = new HashSet<>();

    @OneToMany(mappedBy = "administrativeUnit")
    private Set<Ward> wards = new HashSet<>();
}