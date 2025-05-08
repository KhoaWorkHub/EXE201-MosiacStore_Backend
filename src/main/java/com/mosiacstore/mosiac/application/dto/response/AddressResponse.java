package com.mosiacstore.mosiac.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressResponse {
    private String id;
    private String recipientName;
    private String phone;
    private ProvinceResponse province;
    private DistrictResponse district;
    private WardResponse ward;
    private String streetAddress;
    private Boolean isDefault;
}
