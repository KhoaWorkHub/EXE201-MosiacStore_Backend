package com.mosiacstore.mosiac.application.service;

import com.mosiacstore.mosiac.application.dto.request.CategoryRequest;
import com.mosiacstore.mosiac.application.dto.response.CategoryResponse;
import com.mosiacstore.mosiac.application.dto.response.PageResponse;

import java.util.List;
import java.util.UUID;

/**
 * Application service contract for managing product categories
 */
public interface CategoryService {

    /**
     * Retrieve paginated categories
     */
    PageResponse<CategoryResponse> getCategories(int page, int size, String sort);

    /**
     * Retrieve all categories without pagination
     */
    List<CategoryResponse> getAllCategories();

    /**
     * Retrieve a single category by its ID
     */
    CategoryResponse getCategoryById(UUID id);

    /**
     * Create a new category
     */
    CategoryResponse createCategory(CategoryRequest request);

    /**
     * Update an existing category
     */
    CategoryResponse updateCategory(UUID id, CategoryRequest request);

    /**
     * Delete a category by its ID
     */
    void deleteCategory(UUID id);
}