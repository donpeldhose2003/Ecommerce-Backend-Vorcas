package com.don.ecommerce.controller;

import com.don.ecommerce.model.Product;
import com.don.ecommerce.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequestMapping({"/api/admin/products", "/admin/products"})
public class ProductAdminController {

    private static final Logger logger = LoggerFactory.getLogger(ProductAdminController.class);

    private final ProductService productService;

    public ProductAdminController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping(consumes = {"multipart/form-data"})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createProduct(
            @RequestPart("product") Product product,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        logger.debug("ProductAdminController.createProduct - auth={}", auth);
        try {
            Product saved = productService.createProduct(product, image);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (IllegalArgumentException e) {
            logger.debug("ProductAdminController.createProduct - bad request: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            logger.error("ProductAdminController.createProduct - exception while creating product", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to save product: " + e.getMessage());
        }
    }

    // allow JSON-only creation (no image)
    @PostMapping(path = "/json", consumes = {"application/json"})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createProductJson(@RequestBody Product product) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        logger.debug("ProductAdminController.createProductJson - auth={}", auth);
        try {
            Product saved = productService.createProduct(product, null);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (IllegalArgumentException e) {
            logger.debug("ProductAdminController.createProductJson - bad request: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            logger.error("ProductAdminController.createProductJson - exception while creating product", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to save product: " + e.getMessage());
        }
    }
}
