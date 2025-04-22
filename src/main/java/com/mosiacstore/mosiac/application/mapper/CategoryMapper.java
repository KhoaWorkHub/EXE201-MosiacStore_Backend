package com.mosiacstore.mosiac.application.mapper;

import com.mosiacstore.mosiac.application.dto.request.CategoryRequest;
import com.mosiacstore.mosiac.application.dto.response.CategoryResponse;
import com.mosiacstore.mosiac.domain.product.ProductCategory;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CategoryMapper {

    @Mapping(target = "children", ignore = true)
    @Mapping(target = "parent", ignore = true)
    CategoryResponse toCategoryResponse(ProductCategory category);

    List<CategoryResponse> toCategoryResponseList(List<ProductCategory> categories);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "children", ignore = true)
    ProductCategory toCategory(CategoryRequest request);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateCategoryFromRequest(CategoryRequest request, @MappingTarget ProductCategory category);
}