package com.mosiacstore.mosiac.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistrictResponse {
    private String code;
    private String name;
    private String nameEn;
    private String fullName;
    private String fullNameEn;
    private String provinceCode;
}