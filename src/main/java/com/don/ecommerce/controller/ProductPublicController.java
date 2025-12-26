package com.don.ecommerce.controller;

import com.don.ecommerce.model.Product;
import com.don.ecommerce.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/products", "/products"})
public class ProductPublicController {

    private static final Logger logger = LoggerFactory.getLogger(ProductPublicController.class);

    private final ProductService productService;

    public ProductPublicController(ProductService productService) {
        this.productService = productService;
    }

    // Public endpoint for customers to list products as JSON
    @GetMapping(path = "/json", produces = {"application/json"})
    public ResponseEntity<?> listProductsJson() {
        logger.debug("ProductPublicController.listProductsJson - public request");
        try {
            List<Product> products = productService.getAllProducts();
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            logger.error("ProductPublicController.listProductsJson - failed to fetch products", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch products: " + e.getMessage());
        }
    }
}
