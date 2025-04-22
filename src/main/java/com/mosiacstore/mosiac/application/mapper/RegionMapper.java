package com.mosiacstore.mosiac.application.mapper;

import com.mosiacstore.mosiac.application.dto.request.RegionRequest;
import com.mosiacstore.mosiac.application.dto.response.RegionResponse;
import com.mosiacstore.mosiac.domain.region.Region;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RegionMapper {

    RegionResponse toRegionResponse(Region region);

    List<RegionResponse> toRegionResponseList(List<Region> regions);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "products", ignore = true)
    @Mapping(target = "tourGuides", ignore = true)
    Region toRegion(RegionRequest request);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "products", ignore = true)
    @Mapping(target = "tourGuides", ignore = true)
    void updateRegionFromRequest(RegionRequest request, @MappingTarget Region region);
}