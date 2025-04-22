package com.mosiacstore.mosiac.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegionRequest {
    @NotBlank(message = "Region name is required")
    private String name;

    private String slug;

    private String description;

    private String imageUrl;

    private Boolean active;
}