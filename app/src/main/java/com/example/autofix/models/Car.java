package com.example.autofix.models;

public class Car {
    private String id;
    private String make;  // марка
    private String model; // модель
    private String generation;
    private String imageUrl;
    private String licensePlate;
    private String vinNumber;

    public Car() {
        // Default constructor required for calls to DataSnapshot.getValue(Car.class)
    }

    public Car(String make, String model, String generation, String imageUrl) {
        this.make = make;
        this.model = model;
        this.generation = generation;
        this.imageUrl = imageUrl;
    }

    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }

    public String getVinNumber() { return vinNumber; }
    public void setVinNumber(String vinNumber) { this.vinNumber = vinNumber; }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getMake() { return make; }
    public void setMake(String make) { this.make = make; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    // Для обратной совместимости с существующим кодом
    public String getBrand() { return make; }
    public void setBrand(String brand) { this.make = brand; }

    public String getGeneration() { return generation; }
    public void setGeneration(String generation) { this.generation = generation; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    // Метод для получения полного названия автомобиля
    public String getFullName() {
        StringBuilder fullName = new StringBuilder();
        if (make != null && !make.isEmpty()) {
            fullName.append(make);
        }
        if (model != null && !model.isEmpty()) {
            if (fullName.length() > 0) fullName.append(" ");
            fullName.append(model);
        }
        if (generation != null && !generation.isEmpty()) {
            if (fullName.length() > 0) fullName.append(" ");
            fullName.append(generation);
        }
        return fullName.toString();
    }
}