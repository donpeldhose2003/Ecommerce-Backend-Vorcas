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
import java.util.List;
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

    // Return all stored products
    public List<Product> getAllProducts() {
        return productRepository.findAll();
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

    // New: get single product by id
    public Product getProductById(String id) {
        return productRepository.findById(id).orElse(null);
    }

    // New: update existing product; image optional (multipart)
    public Product updateProduct(String id, Product updated, MultipartFile image) throws IOException {
        Product existing = productRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Product not found"));

        // update simple fields when provided (null-check)
        if (updated.getName() != null) existing.setName(updated.getName());
        if (updated.getCategory() != null) existing.setCategory(updated.getCategory());
        if (updated.getSubCategory() != null) existing.setSubCategory(updated.getSubCategory());
        if (updated.getCollectionName() != null) existing.setCollectionName(updated.getCollectionName());
        if (updated.getGender() != null) existing.setGender(updated.getGender());
        if (updated.getFit() != null) existing.setFit(updated.getFit());
        if (updated.getDescription() != null) existing.setDescription(updated.getDescription());
        if (updated.getMaterials() != null) existing.setMaterials(updated.getMaterials());
        if (updated.getCareInstructions() != null) existing.setCareInstructions(updated.getCareInstructions());

        if (updated.getSizes() != null) existing.setSizes(updated.getSizes());
        if (updated.getColors() != null) existing.setColors(updated.getColors());

        if (updated.getStockQuantity() != null) existing.setStockQuantity(updated.getStockQuantity());

        // pricing: if originalPrice or discount provided, recompute
        if (updated.getOriginalPrice() != null) {
            existing.setOriginalPrice(updated.getOriginalPrice());
        }
        if (updated.getDiscountPercent() != null) {
            existing.setDiscountPercent(updated.getDiscountPercent());
        }
        // compute finalPrice using current values
        BigDecimal original = existing.getOriginalPrice();
        Integer discount = existing.getDiscountPercent();
        if (original != null && discount != null && discount >= 0 && discount <= 100) {
            BigDecimal discountAmount = original.multiply(BigDecimal.valueOf(discount)).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            existing.setFinalPrice(original.subtract(discountAmount));
        } else if (original != null) {
            existing.setFinalPrice(original);
        }

        // update booleans (no null in primitive types in model, but updated may have defaults)
        existing.setBestseller(updated.isBestseller());
        existing.setFeatured(updated.isFeatured());
        existing.setNewArrivals(updated.isNewArrivals());
        existing.setLimitedEdition(updated.isLimitedEdition());
        existing.setSale(updated.isSale());

        // handle image replacement
        if (image != null && !image.isEmpty()) {
            validateImage(image);
            // delete existing file if present
            String oldPath = existing.getImagePath();
            if (oldPath != null && oldPath.startsWith("/uploads/")) {
                try {
                    Path oldFile = this.uploadDir.resolve(oldPath.substring("/uploads/".length()));
                    Files.deleteIfExists(oldFile);
                } catch (IOException e) {
                    // log and continue
                }
            }
            String filename = System.currentTimeMillis() + "_" + UUID.randomUUID() + "_" + sanitizeFilename(image.getOriginalFilename());
            Path target = this.uploadDir.resolve(filename);
            Files.copy(image.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            existing.setImagePath("/uploads/" + filename);
        }

        return productRepository.save(existing);
    }

    // New: delete product and its image file if exists
    public void deleteProduct(String id) throws IOException {
        Product existing = productRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Product not found"));
        String oldPath = existing.getImagePath();
        if (oldPath != null && oldPath.startsWith("/uploads/")) {
            Path oldFile = this.uploadDir.resolve(oldPath.substring("/uploads/".length()));
            Files.deleteIfExists(oldFile);
        }
        productRepository.deleteById(id);
    }
}
