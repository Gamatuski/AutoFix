package com.example.autofix.models;

import com.google.firebase.firestore.PropertyName;

import java.util.List;

public class ServiceCategory {
    private String id;
    private String name;
    private String imageUrl;
    private List<ServiceSubcategory> subcategories;

    // Пустой конструктор обязателен для Firestore
    public ServiceCategory() {}

    public ServiceCategory(String name, String imageUrl) {
        this.name = name;
        this.imageUrl = imageUrl;
    }

    @PropertyName("id")
    public String getId() {
        return id;
    }

    @PropertyName("id")
    public void setId(String id) {
        this.id = id;
    }

    @PropertyName("name")
    public String getName() {
        return name;
    }

    @PropertyName("name")
    public void setName(String name) {
        this.name = name;
    }

    @PropertyName("image")
    public String getImageUrl() {
        return imageUrl;
    }

    @PropertyName("image")
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @PropertyName("subcategories")
    public List<ServiceSubcategory> getSubcategories() {
        return subcategories;
    }

    @PropertyName("subcategories")
    public void setSubcategories(List<ServiceSubcategory> subcategories) {
        this.subcategories = subcategories;
    }
}