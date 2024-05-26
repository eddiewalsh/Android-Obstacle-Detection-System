package com.example.odsk00238061.utils;

import java.util.List;

/**
 * Represents a product with various attributes such as title, description, category, ingredients, nutrition facts, and images.
 */
public class Product {
    // Attributes of the product
    private String title;
    private String description;
    private String category;
    private String ingredients;
    private String nutritionFacts;
    private List<String> images;

    // Constructors
    public Product () {}


    // Getters and Setters

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title.trim();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        if(category.toLowerCase().contains("food")){
            this.category = "Consumable";
        } else {
            this.category = category;
        }
    }

    public String getIngredients() {
        return ingredients;
    }

    public void setIngredients(String ingredients) {
        this.ingredients =  ingredients;
    }

    public String getNutritionFacts() {
        return nutritionFacts;
    }

    public void setNutritionFacts(String nutritionFacts) {
        this.nutritionFacts =  nutritionFacts;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }


    // Other methods

    /**
     * Checks if the product belongs to the category of food.
     *
     * @return True if the category contains "food" (case insensitive), false otherwise.
     */
    public boolean isFood(){
        return category.toLowerCase().contains("food");
    }

    /**
     * Checks if ingredients information is available for the product.
     *
     * @return True if ingredients information is not null and not empty, false otherwise.
     */
    public boolean isIngredientsAvailable() {
        return ingredients != null && !ingredients.isEmpty();
    }

    /**
     * Checks if nutrition facts information is available for the product.
     *
     * @return True if nutrition facts information is not null and not empty, false otherwise.
     */
    public boolean isNutritionFactsAvailable() {
        return nutritionFacts != null && !nutritionFacts.isEmpty();
    }

    /**
     * Checks if the title of the product is available.
     *
     * @return True if the title is not null and not empty, false otherwise.
     */
    public boolean isTitleAvailable() {
        return title != null && !title.isEmpty();
    }

    /**
     * Checks if the description of the product is available.
     *
     * @return True if the description is not null and not empty, false otherwise.
     */
    public boolean isDescriptionAvailable() {
        return description != null && !description.isEmpty();
    }
}

