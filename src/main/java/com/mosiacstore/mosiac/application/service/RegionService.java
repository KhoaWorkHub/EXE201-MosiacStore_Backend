package com.mosiacstore.mosiac.application.service;

import com.mosiacstore.mosiac.application.dto.request.RegionRequest;
import com.mosiacstore.mosiac.application.dto.response.PageResponse;
import com.mosiacstore.mosiac.application.dto.response.RegionResponse;

import java.util.List;
import java.util.UUID;

/**
 * Application service contract for managing regions
 */
public interface RegionService {

    /**
     * Retrieve paginated regions
     */
    PageResponse<RegionResponse> getRegions(int page, int size, String sort);

    /**
     * Retrieve all regions without pagination
     */
    List<RegionResponse> getAllRegions();

    /**
     * Retrieve a single region by its ID
     */
    RegionResponse getRegionById(UUID id);

    /**
     * Create a new region
     */
    RegionResponse createRegion(RegionRequest request);

    /**
     * Update an existing region
     */
    RegionResponse updateRegion(UUID id, RegionRequest request);

    /**
     * Delete a region by its ID
     */
    void deleteRegion(UUID id);
}