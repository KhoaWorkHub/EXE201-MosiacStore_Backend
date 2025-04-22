package com.mosiacstore.mosiac.application.service.Impl;

import com.mosiacstore.mosiac.application.service.SlugService;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class SlugServiceImpl implements SlugService {

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final Pattern EDGESDHASHES = Pattern.compile("(^-|-$)");

    /**
     * Generate URL-friendly slug from text
     * This handles Vietnamese characters and special symbols
     */
    @Override
    public String generateSlug(String input) {
        if (input == null) {
            return "";
        }

        String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        slug = EDGESDHASHES.matcher(slug).replaceAll("");

        return slug.toLowerCase(Locale.ENGLISH);
    }
}