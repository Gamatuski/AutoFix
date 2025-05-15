package com.example.autofix.models;

import com.google.firebase.firestore.PropertyName;

import java.io.Serializable;

public class Service implements Serializable {
    private String id;
    private String name;
    private String description;
    private String imageUrl;
    private int price;
    private int durationMinutes;


    public Service() {}

    public Service(String name, String description, int price, int durationMinutes) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.durationMinutes = durationMinutes;
    }

    @PropertyName("id")
    public String getId() {
        return id;
    }

    @PropertyName("id")
    public void setId(String id) {
        this.id = id;
    }

    @PropertyName("Name")
    public String getName() {
        return name;
    }

    @PropertyName("Name")
    public void setName(String name) {
        this.name = name;
    }

    @PropertyName("Description")
    public String getDescription() {
        return description;
    }

    @PropertyName("Description")
    public void setDescription(String description) {
        this.description = description;
    }

    @PropertyName("Image")
    public String getImageUrl() {
        return imageUrl;
    }

    @PropertyName("Image")
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @PropertyName("Price")
    public int getPrice() {
        return price;
    }

    @PropertyName("Price")
    public void setPrice(int price) {
        this.price = price;
    }

    @PropertyName("Duration")
    public int getDurationMinutes() {
        return durationMinutes;
    }

    @PropertyName("Duration")
    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }


}