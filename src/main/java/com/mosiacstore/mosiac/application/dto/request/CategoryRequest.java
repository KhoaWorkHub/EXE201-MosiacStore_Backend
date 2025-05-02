package com.mosiacstore.mosiac.application.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRequest {
    @NotBlank(message = "Category name is required")
    private String name;

    private String slug;

    private String description;

    private UUID parentId;

    private String imageUrl;

    private Integer displayOrder;

    private Boolean active;

    @JsonIgnore
    private MultipartFile file;
}