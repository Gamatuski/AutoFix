package com.example.autofix.models;

import com.google.firebase.firestore.PropertyName;

import java.util.List;

public class ServiceSubcategory {
    private String id;
    private String name;
    private String imageUrl;
    private List<Service> services;

    public ServiceSubcategory() {}

    public ServiceSubcategory(String name, String imageUrl) {
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

    @PropertyName("services")
    public List<Service> getServices() {
        return services;
    }

    @PropertyName("services")
    public void setServices(List<Service> services) {
        this.services = services;
    }
}