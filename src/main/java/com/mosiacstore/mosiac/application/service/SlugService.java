package com.mosiacstore.mosiac.application.service;

/**
 * Service for generating URL-friendly slugs
 */
public interface SlugService {
    /**
     * Generate URL-friendly slug from input text
     * @param input the source text
     * @return normalized slug
     */
    String generateSlug(String input);
}