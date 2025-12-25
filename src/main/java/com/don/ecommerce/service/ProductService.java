package com.don.ecommerce.service;

import com.don.ecommerce.model.Product;
import com.don.ecommerce.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final Path uploadDir;
    private final long maxFileSizeBytes = 10L * 1024L * 1024L; // 10MB

    public ProductService(ProductRepository productRepository, @Value("${app.upload.dir:uploads}") String uploadDir) {
        this.productRepository = productRepository;
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory", e);
        }
    }

    public Product createProduct(Product product, MultipartFile image) throws IOException {
        if (product.getOriginalPrice() == null) {
            throw new IllegalArgumentException("originalPrice is required");
        }

        Integer discount = product.getDiscountPercent();
        BigDecimal original = product.getOriginalPrice();
        if (discount != null && discount >= 0 && discount <= 100) {
            // use scale and rounding mode to avoid non-terminating decimal errors
            BigDecimal discountAmount = original.multiply(BigDecimal.valueOf(discount)).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            product.setFinalPrice(original.subtract(discountAmount));
        } else {
            product.setFinalPrice(original);
        }

        if (image != null && !image.isEmpty()) {
            validateImage(image);
            String filename = System.currentTimeMillis() + "_" + UUID.randomUUID() + "_" + sanitizeFilename(image.getOriginalFilename());
            Path target = this.uploadDir.resolve(filename);
            Files.copy(image.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            product.setImagePath("/uploads/" + filename);
        }

        return productRepository.save(product);
    }

    private void validateImage(MultipartFile file) {
        if (file.getSize() > maxFileSizeBytes) {
            throw new IllegalArgumentException("Image exceeds maximum allowed size of 10MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !(contentType.equalsIgnoreCase("image/png") || contentType.equalsIgnoreCase("image/jpeg") || contentType.equalsIgnoreCase("image/jpg") || contentType.equalsIgnoreCase("image/gif"))) {
            throw new IllegalArgumentException("Unsupported image type; allowed types: PNG, JPG, GIF");
        }
    }

    private String sanitizeFilename(String original) {
        if (original == null) return "file";
        return original.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
