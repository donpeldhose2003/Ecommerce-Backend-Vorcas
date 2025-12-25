package com.don.ecommerce.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Document(collection = "products")
public class Product {

    @Id
    private String id;

    private String name;
    private String category;
    private String subCategory;
    private String collectionName;
    private String gender;
    private String fit;
    private String description;

    private BigDecimal originalPrice;
    private Integer discountPercent;
    private BigDecimal finalPrice;

    private List<String> sizes;
    private List<String> colors;

    private Integer stockQuantity;
    private String materials;
    private String careInstructions;

    private boolean bestseller;
    private boolean featured;
    private boolean newArrivals;
    private boolean limitedEdition;
    private boolean sale;

    private String imagePath;

    private Instant createdAt = Instant.now();

    public Product() {}

    // getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSubCategory() { return subCategory; }
    public void setSubCategory(String subCategory) { this.subCategory = subCategory; }

    public String getCollectionName() { return collectionName; }
    public void setCollectionName(String collectionName) { this.collectionName = collectionName; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getFit() { return fit; }
    public void setFit(String fit) { this.fit = fit; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(BigDecimal originalPrice) { this.originalPrice = originalPrice; }

    public Integer getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(Integer discountPercent) { this.discountPercent = discountPercent; }

    public BigDecimal getFinalPrice() { return finalPrice; }
    public void setFinalPrice(BigDecimal finalPrice) { this.finalPrice = finalPrice; }

    public List<String> getSizes() { return sizes; }
    public void setSizes(List<String> sizes) { this.sizes = sizes; }

    public List<String> getColors() { return colors; }
    public void setColors(List<String> colors) { this.colors = colors; }

    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }

    public String getMaterials() { return materials; }
    public void setMaterials(String materials) { this.materials = materials; }

    public String getCareInstructions() { return careInstructions; }
    public void setCareInstructions(String careInstructions) { this.careInstructions = careInstructions; }

    public boolean isBestseller() { return bestseller; }
    public void setBestseller(boolean bestseller) { this.bestseller = bestseller; }

    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }

    public boolean isNewArrivals() { return newArrivals; }
    public void setNewArrivals(boolean newArrivals) { this.newArrivals = newArrivals; }

    public boolean isLimitedEdition() { return limitedEdition; }
    public void setLimitedEdition(boolean limitedEdition) { this.limitedEdition = limitedEdition; }

    public boolean isSale() { return sale; }
    public void setSale(boolean sale) { this.sale = sale; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

