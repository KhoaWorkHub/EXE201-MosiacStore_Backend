package com.mosiacstore.mosiac.application.service.Impl;

import com.mosiacstore.mosiac.application.dto.request.ProductRequest;
import com.mosiacstore.mosiac.application.dto.request.ProductVariantRequest;
import com.mosiacstore.mosiac.application.dto.response.PageResponse;
import com.mosiacstore.mosiac.application.dto.response.ProductImageResponse;
import com.mosiacstore.mosiac.application.dto.response.ProductResponse;
import com.mosiacstore.mosiac.application.dto.response.ProductVariantResponse;
import com.mosiacstore.mosiac.application.exception.EntityNotFoundException;
import com.mosiacstore.mosiac.application.exception.ResourceConflictException;
import com.mosiacstore.mosiac.application.mapper.ProductMapper;
import com.mosiacstore.mosiac.application.service.ProductService;
import com.mosiacstore.mosiac.application.service.SlugService;
import com.mosiacstore.mosiac.domain.product.Product;
import com.mosiacstore.mosiac.domain.product.ProductCategory;
import com.mosiacstore.mosiac.domain.product.ProductImage;
import com.mosiacstore.mosiac.domain.product.ProductVariant;
import com.mosiacstore.mosiac.domain.region.Region;
import com.mosiacstore.mosiac.infrastructure.repository.ProductCategoryRepository;
import com.mosiacstore.mosiac.infrastructure.repository.ProductImageRepository;
import com.mosiacstore.mosiac.infrastructure.repository.ProductRepository;
import com.mosiacstore.mosiac.infrastructure.repository.ProductVariantRepository;
import com.mosiacstore.mosiac.infrastructure.repository.RegionRepository;
import com.mosiacstore.mosiac.infrastructure.service.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;
    private final RegionRepository regionRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;
    private final ProductMapper productMapper;
    private final MinioService minioService;
    private final SlugService slugService;

    @Override
    public PageResponse<ProductResponse> getProducts(
            String keyword, UUID categoryId, UUID regionId, Double minPrice, Double maxPrice,
            Boolean featured, Boolean active, int page, int size, String sort) {
        Specification<Product> spec = Specification.where(null);
        if (keyword != null && !keyword.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), "%" + keyword.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("description")), "%" + keyword.toLowerCase() + "%")));
        }
        if (categoryId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId));
        }
        if (regionId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("region").get("id"), regionId));
        }
        if (minPrice != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), minPrice));
        }
        if (maxPrice != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), maxPrice));
        }
        if (featured != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("featured"), featured));
        }
        if (active != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("active"), active));
        }
        Sort sorting = Sort.by(Sort.Direction.DESC, "createdAt");
        if (sort != null && !sort.isEmpty()) {
            String[] sortParams = sort.split(",");
            Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc")
                    ? Sort.Direction.ASC : Sort.Direction.DESC;
            sorting = Sort.by(direction, sortParams[0]);
        }
        Pageable pageable = PageRequest.of(page, size, sorting);
        Page<Product> productPage = productRepository.findAll(spec, pageable);
        List<ProductResponse> productResponses = productMapper.toProductResponseList(productPage.getContent());
        return new PageResponse<>(
                productResponses,
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalElements(),
                productPage.getTotalPages(),
                productPage.isFirst(),
                productPage.isLast()
        );
    }

    @Override
    public ProductResponse getProductById(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + id));
        product.setViewCount(product.getViewCount() + 1);
        productRepository.save(product);
        return productMapper.toProductResponse(product);
    }

    @Override
    public ProductResponse getProductBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with slug: " + slug));
        product.setViewCount(product.getViewCount() + 1);
        productRepository.save(product);
        return productMapper.toProductResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        if (request.getSlug() == null || request.getSlug().isEmpty()) {
            request.setSlug(slugService.generateSlug(request.getName()));
        } else if (productRepository.findBySlug(request.getSlug()).isPresent()) {
            throw new ResourceConflictException("Product with slug '" + request.getSlug() + "' already exists");
        }
        Product product = productMapper.toProduct(request);
        if (request.getCategoryId() != null) {
            ProductCategory category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("Category not found with ID: " + request.getCategoryId()));
            product.setCategory(category);
        }
        if (request.getRegionId() != null) {
            Region region = regionRepository.findById(request.getRegionId())
                    .orElseThrow(() -> new EntityNotFoundException("Region not found with ID: " + request.getRegionId()));
            product.setRegion(region);
        }
        product.setViewCount(0);
        if (product.getActive() == null) product.setActive(true);
        if (product.getFeatured() == null) product.setFeatured(false);
        Product savedProduct = productRepository.save(product);
        return productMapper.toProductResponse(savedProduct);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(UUID id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + id));
        if (request.getSlug() != null && !request.getSlug().equals(product.getSlug())) {
            if (productRepository.existsBySlugAndIdNot(request.getSlug(), id)) {
                throw new ResourceConflictException("Product with slug '" + request.getSlug() + "' already exists");
            }
        }
        productMapper.updateProductFromRequest(request, product);
        if (request.getCategoryId() != null) {
            ProductCategory category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("Category not found with ID: " + request.getCategoryId()));
            product.setCategory(category);
        }
        if (request.getRegionId() != null) {
            Region region = regionRepository.findById(request.getRegionId())
                    .orElseThrow(() -> new EntityNotFoundException("Region not found with ID: " + request.getRegionId()));
            product.setRegion(region);
        }
        product.setUpdatedAt(LocalDateTime.now());
        Product updatedProduct = productRepository.save(product);
        return productMapper.toProductResponse(updatedProduct);
    }

    @Override
    @Transactional
    public void deleteProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + id));
        List<ProductImage> images = imageRepository.findByProductId(id);
        for (ProductImage image : images) {
            if (image.getImageUrl() != null && !image.getImageUrl().isEmpty()) {
                try {
                    minioService.deleteFile(image.getImageUrl());
                } catch (Exception e) {
                    log.warn("Failed to delete image file: {}", image.getImageUrl(), e);
                }
            }
        }
        productRepository.delete(product);
    }

    @Override
    @Transactional
    public List<ProductImageResponse> uploadProductImages(
            UUID productId,
            List<MultipartFile> files,
            String altText,
            Boolean isPrimary) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + productId));
        List<ProductImage> savedImages = new ArrayList<>();
        for (MultipartFile file : files) {
            String imageUrl = minioService.uploadFile(file, "products");
            ProductImage image = new ProductImage();
            image.setProduct(product);
            image.setImageUrl(imageUrl);
            image.setAltText(altText);
            List<ProductImage> existingImages = imageRepository.findByProductIdOrderByDisplayOrderAsc(productId);
            int displayOrder = existingImages.isEmpty() ? 0 : existingImages.get(existingImages.size() - 1).getDisplayOrder() + 1;
            image.setDisplayOrder(displayOrder);
            if (Boolean.TRUE.equals(isPrimary)) {
                imageRepository.updateNonPrimaryImages(productId, null);
                image.setIsPrimary(true);
            } else if (existingImages.isEmpty() || !existingImages.stream().anyMatch(ProductImage::getIsPrimary)) {
                image.setIsPrimary(true);
            } else {
                image.setIsPrimary(false);
            }
            savedImages.add(imageRepository.save(image));
        }
        return savedImages.stream()
                .map(productMapper::toProductImageResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteProductImage(UUID imageId) {
        ProductImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new EntityNotFoundException("Product image not found with ID: " + imageId));
        if (image.getImageUrl() != null && !image.getImageUrl().isEmpty()) {
            try {
                minioService.deleteFile(image.getImageUrl());
            } catch (Exception e) {
                log.warn("Failed to delete image file: {}", image.getImageUrl(), e);
            }
        }
        if (Boolean.TRUE.equals(image.getIsPrimary())) {
            List<ProductImage> otherImages = imageRepository.findByProductId(image.getProduct().getId()).stream()
                    .filter(img -> !img.getId().equals(imageId))
                    .collect(Collectors.toList());
            if (!otherImages.isEmpty()) {
                ProductImage newPrimary = otherImages.get(0);
                newPrimary.setIsPrimary(true);
                imageRepository.save(newPrimary);
            }
        }
        imageRepository.delete(image);
    }

    @Override
    @Transactional
    public ProductResponse setProductFeatured(UUID id, boolean featured) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + id));
        product.setFeatured(featured);
        product.setUpdatedAt(LocalDateTime.now());
        Product updatedProduct = productRepository.save(product);
        return productMapper.toProductResponse(updatedProduct);
    }

    @Override
    @Transactional
    public ProductVariantResponse addProductVariant(UUID productId, ProductVariantRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + productId));
        if (variantRepository.existsByProductIdAndSizeAndColor(
                productId, request.getSize().name(), request.getColor())) {
            throw new ResourceConflictException("Variant with size " + request.getSize() +
                    " and color " + request.getColor() + " already exists for this product");
        }
        ProductVariant variant = productMapper.toProductVariant(request);
        variant.setProduct(product);
        if (variant.getActive() == null) variant.setActive(true);
        if (variant.getPriceAdjustment() == null) variant.setPriceAdjustment(java.math.BigDecimal.ZERO);
        if (variant.getSkuVariant() == null || variant.getSkuVariant().isEmpty()) {
            String baseSkuSku = product.getSku() != null ? product.getSku() :
                    "P" + product.getId().toString().substring(0, 8).toUpperCase();
            variant.setSkuVariant(baseSkuSku + "-" + variant.getSize() +
                    (variant.getColor() != null ? "-" + variant.getColor() : ""));
        }
        ProductVariant savedVariant = variantRepository.save(variant);
        return productMapper.toProductVariantResponse(savedVariant);
    }

    @Override
    @Transactional
    public ProductVariantResponse updateProductVariant(UUID variantId, ProductVariantRequest request) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new EntityNotFoundException("Product variant not found with ID: " + variantId));
        boolean sizeChanged = request.getSize() != null && request.getSize() != variant.getSize();
        boolean colorChanged = request.getColor() != null && !request.getColor().equals(variant.getColor());
        if (sizeChanged || colorChanged) {
            String newSize = sizeChanged ? request.getSize().name() : variant.getSize().name();
            String newColor = colorChanged ? request.getColor() : variant.getColor();
            if (variantRepository.existsByProductIdAndSizeAndColor(
                    variant.getProduct().getId(), newSize, newColor)) {
                throw new ResourceConflictException("Variant with size " + newSize +
                        " and color " + newColor + " already exists for this product");
            }
        }
        if (request.getSize() != null) variant.setSize(request.getSize());
        if (request.getColor() != null) variant.setColor(request.getColor());
        if (request.getPriceAdjustment() != null) variant.setPriceAdjustment(request.getPriceAdjustment());
        if (request.getStockQuantity() != null) variant.setStockQuantity(request.getStockQuantity());
        if (request.getSkuVariant() != null) variant.setSkuVariant(request.getSkuVariant());
        if (request.getActive() != null) variant.setActive(request.getActive());
        variant.setUpdatedAt(LocalDateTime.now());
        ProductVariant updatedVariant = variantRepository.save(variant);
        return productMapper.toProductVariantResponse(updatedVariant);
    }

    @Override
    @Transactional
    public void deleteProductVariant(UUID variantId) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new EntityNotFoundException("Product variant not found with ID: " + variantId));
        variantRepository.delete(variant);
    }

    @Override
    public PageResponse<ProductResponse> getFeaturedProducts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> productPage = productRepository.findByFeaturedTrueAndActiveTrue(pageable);
        List<ProductResponse> productResponses = productMapper.toProductResponseList(productPage.getContent());
        return new PageResponse<>(
                productResponses,
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalElements(),
                productPage.getTotalPages(),
                productPage.isFirst(),
                productPage.isLast()
        );
    }
}
