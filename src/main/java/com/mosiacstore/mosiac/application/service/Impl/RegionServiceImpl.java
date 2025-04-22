package com.mosiacstore.mosiac.application.service.Impl;

import com.mosiacstore.mosiac.application.dto.request.RegionRequest;
import com.mosiacstore.mosiac.application.dto.response.PageResponse;
import com.mosiacstore.mosiac.application.dto.response.RegionResponse;
import com.mosiacstore.mosiac.application.exception.EntityNotFoundException;
import com.mosiacstore.mosiac.application.exception.ResourceConflictException;
import com.mosiacstore.mosiac.application.mapper.RegionMapper;
import com.mosiacstore.mosiac.application.service.RegionService;
import com.mosiacstore.mosiac.application.service.SlugService;
import com.mosiacstore.mosiac.domain.region.Region;
import com.mosiacstore.mosiac.infrastructure.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RegionServiceImpl implements RegionService {
    private final RegionRepository regionRepository;
    private final RegionMapper regionMapper;
    private final SlugService slugService;

    @Override
    public PageResponse<RegionResponse> getRegions(int page, int size, String sort) {
        Sort sorting = Sort.by(Sort.Direction.ASC, "name");
        if (sort != null && !sort.isEmpty()) {
            String[] params = sort.split(",");
            Sort.Direction dir = params.length > 1 && params[1].equalsIgnoreCase("desc")
                    ? Sort.Direction.DESC : Sort.Direction.ASC;
            sorting = Sort.by(dir, params[0]);
        }
        Pageable pageable = PageRequest.of(page, size, sorting);
        Page<Region> pg = regionRepository.findAll(pageable);
        List<RegionResponse> list = pg.getContent().stream()
                .map(regionMapper::toRegionResponse)
                .collect(Collectors.toList());
        return new PageResponse<>(
                list,
                pg.getNumber(),
                pg.getSize(),
                pg.getTotalElements(),
                pg.getTotalPages(),
                pg.isFirst(),
                pg.isLast()
        );
    }

    @Override
    public List<RegionResponse> getAllRegions() {
        List<Region> regs = regionRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
        return regs.stream()
                .map(regionMapper::toRegionResponse)
                .collect(Collectors.toList());
    }

    @Override
    public RegionResponse getRegionById(UUID id) {
        Region reg = regionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Region not found with ID: " + id));
        return regionMapper.toRegionResponse(reg);
    }

    @Override
    @Transactional
    public RegionResponse createRegion(RegionRequest request) {
        if (request.getSlug() == null || request.getSlug().isEmpty()) {
            request.setSlug(slugService.generateSlug(request.getName()));
        } else if (regionRepository.existsBySlug(request.getSlug())) {
            throw new ResourceConflictException("Region with slug '" + request.getSlug() + "' already exists");
        }
        Region entity = regionMapper.toRegion(request);
        if (entity.getActive() == null) entity.setActive(true);
        Region saved = regionRepository.save(entity);
        return regionMapper.toRegionResponse(saved);
    }

    @Override
    @Transactional
    public RegionResponse updateRegion(UUID id, RegionRequest request) {
        Region reg = regionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Region not found with ID: " + id));
        if (request.getSlug() != null && !request.getSlug().equals(reg.getSlug()) &&
                regionRepository.existsBySlugAndIdNot(request.getSlug(), id)) {
            throw new ResourceConflictException("Region with slug '" + request.getSlug() + "' already exists");
        }
        regionMapper.updateRegionFromRequest(request, reg);
        reg.setUpdatedAt(LocalDateTime.now());
        Region updated = regionRepository.save(reg);
        return regionMapper.toRegionResponse(updated);
    }

    @Override
    @Transactional
    public void deleteRegion(UUID id) {
        Region reg = regionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Region not found with ID: " + id));
        if (regionRepository.hasProducts(id)) {
            throw new ResourceConflictException("Cannot delete region with products. Remove products first.");
        }
        if (regionRepository.hasTourGuides(id)) {
            throw new ResourceConflictException("Cannot delete region with tour guides. Remove tour guides first.");
        }
        regionRepository.delete(reg);
    }
}
