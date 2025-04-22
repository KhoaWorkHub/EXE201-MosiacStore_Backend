package com.mosiacstore.mosiac.application.service.Impl;

import com.mosiacstore.mosiac.application.dto.request.CategoryRequest;
import com.mosiacstore.mosiac.application.dto.response.CategoryResponse;
import com.mosiacstore.mosiac.application.dto.response.PageResponse;
import com.mosiacstore.mosiac.application.exception.EntityNotFoundException;
import com.mosiacstore.mosiac.application.exception.ResourceConflictException;
import com.mosiacstore.mosiac.application.mapper.CategoryMapper;
import com.mosiacstore.mosiac.application.service.CategoryService;
import com.mosiacstore.mosiac.application.service.SlugService;
import com.mosiacstore.mosiac.domain.product.ProductCategory;
import com.mosiacstore.mosiac.infrastructure.repository.ProductCategoryRepository;
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
public class CategoryServiceImpl implements CategoryService {
    private final ProductCategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final SlugService slugService;

    @Override
    public PageResponse<CategoryResponse> getCategories(int page, int size, String sort) {
        Sort sorting = Sort.by(Sort.Direction.ASC, "displayOrder", "name");
        if (sort != null && !sort.isEmpty()) {
            String[] params = sort.split(",");
            Sort.Direction dir = params.length>1 && params[1].equalsIgnoreCase("desc")
                    ? Sort.Direction.DESC : Sort.Direction.ASC;
            sorting = Sort.by(dir, params[0]);
        }
        Pageable pageable = PageRequest.of(page, size, sorting);
        Page<ProductCategory> pg = categoryRepository.findAll(pageable);
        List<CategoryResponse> list = pg.getContent().stream()
                .map(categoryMapper::toCategoryResponse)
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
    public List<CategoryResponse> getAllCategories() {
        List<ProductCategory> cats = categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "displayOrder", "name"));
        return cats.stream()
                .map(categoryMapper::toCategoryResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CategoryResponse getCategoryById(UUID id) {
        ProductCategory cat = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found with ID: " + id));
        return categoryMapper.toCategoryResponse(cat);
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        if (request.getSlug()==null || request.getSlug().isEmpty()) {
            request.setSlug(slugService.generateSlug(request.getName()));
        } else if (categoryRepository.existsBySlug(request.getSlug())) {
            throw new ResourceConflictException("Category with slug '"+request.getSlug()+"' already exists");
        }
        ProductCategory entity = categoryMapper.toCategory(request);
        if (request.getParentId()!=null) {
            ProductCategory parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new EntityNotFoundException("Parent category not found with ID: " + request.getParentId()));
            entity.setParent(parent);
        }
        if (entity.getActive()==null) entity.setActive(true);
        if (entity.getDisplayOrder()==null) entity.setDisplayOrder(0);
        ProductCategory saved = categoryRepository.save(entity);
        return categoryMapper.toCategoryResponse(saved);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(UUID id, CategoryRequest request) {
        ProductCategory cat = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found with ID: " + id));
        if (request.getSlug()!=null && !request.getSlug().equals(cat.getSlug()) &&
                categoryRepository.existsBySlugAndIdNot(request.getSlug(), id)) {
            throw new ResourceConflictException("Category with slug '"+request.getSlug()+"' already exists");
        }
        categoryMapper.updateCategoryFromRequest(request, cat);
        if (request.getParentId()!=null) {
            if (request.getParentId().equals(id)) {
                throw new ResourceConflictException("Category cannot be its own parent");
            }
            ProductCategory parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new EntityNotFoundException("Parent category not found with ID: " + request.getParentId()));
            cat.setParent(parent);
        } else if (request.getParentId()==null) {
            cat.setParent(null);
        }
        cat.setUpdatedAt(LocalDateTime.now());
        ProductCategory updated = categoryRepository.save(cat);
        return categoryMapper.toCategoryResponse(updated);
    }

    @Override
    @Transactional
    public void deleteCategory(UUID id) {
        ProductCategory cat = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found with ID: " + id));
        if (!cat.getChildren().isEmpty()) {
            throw new ResourceConflictException("Cannot delete category with children. Delete children first.");
        }
        if (categoryRepository.hasProducts(id)) {
            throw new ResourceConflictException("Cannot delete category with products. Remove products first.");
        }
        categoryRepository.delete(cat);
    }
}
