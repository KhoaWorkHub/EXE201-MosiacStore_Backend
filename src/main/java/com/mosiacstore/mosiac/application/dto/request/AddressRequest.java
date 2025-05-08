package com.mosiacstore.mosiac.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressRequest {
    @NotBlank(message = "Recipient name is required")
    private String recipientName;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9]{10,11}$", message = "Phone number must be 10-11 digits")
    private String phone;

    @NotBlank(message = "Province code is required")
    private String provinceCode;

    @NotBlank(message = "District code is required")
    private String districtCode;

    @NotBlank(message = "Ward code is required")
    private String wardCode;

    @NotBlank(message = "Street address is required")
    private String streetAddress;

    private Boolean isDefault = false;
}