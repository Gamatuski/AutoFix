package com.example.autofix.data.entities;

import android.content.Intent;
import android.util.Log;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.ArrayList;
import java.util.List;

@Entity(tableName = "cart")
public class Cart {
    @PrimaryKey
    private int id = 1; // Всегда будет только одна корзина

    private String selectedCarId;
    private String selectedCarName;
    private int totalPrice;
    private Integer originalPrice;
    private int totalDuration;
    private Integer discount;

    // Поля для СТО и записи
    private String stationId;
    private String stationAddress;
    private String bookingDate;
    private String bookingTime;
    private String comment;

    // Поля для статуса заказа
    private String orderId;
    private String orderStatus;



    public Cart(String selectedCarId, String selectedCarName, int totalPrice, int totalDuration) {
        this.selectedCarId = selectedCarId;
        this.selectedCarName = selectedCarName;
        this.totalPrice = totalPrice;
        this.totalDuration = totalDuration;
    }

    public Cart() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSelectedCarId() {
        return selectedCarId;
    }

    public void setSelectedCarId(String selectedCarId) {
        this.selectedCarId = selectedCarId;
    }

    public String getSelectedCarName() {
        return selectedCarName;
    }

    public void setSelectedCarName(String selectedCarName) {
        this.selectedCarName = selectedCarName;
    }

    public int getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(int totalPrice) {
        this.totalPrice = totalPrice;
    }

    public int getTotalDuration() {
        return totalDuration;
    }

    public void setTotalDuration(int totalDuration) {
        this.totalDuration = totalDuration;
    }

    // Геттеры и сеттеры для СТО и записи
    public String getStationId() {
        return stationId;
    }

    public void setStationId(String stationId) {
        this.stationId = stationId;
    }

    public String getStationAddress() {
        return stationAddress;
    }

    public void setStationAddress(String stationAddress) {
        this.stationAddress = stationAddress;
    }

    public String getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(String bookingDate) {
        this.bookingDate = bookingDate;
    }

    public String getBookingTime() {
        return bookingTime;
    }

    public void setBookingTime(String bookingTime) {
        this.bookingTime = bookingTime;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    // Геттеры и сеттеры для статуса заказа
    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    // Вспомогательные методы
    public boolean isBookingComplete() {
        return stationId != null && bookingDate != null && bookingTime != null;
    }
    public Integer getDiscount() {
        return discount;
    }

    public void setDiscount(Integer discount) {
        this.discount = discount;
    }

    public Integer getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(Integer originalPrice) {
        this.originalPrice = originalPrice;
    }

    // Применяем скидку в процентах
    // Применяем скидку в процентах
    public void applyDiscount(int discountPercent) {
        // Если нет оригинальной цены, используем текущую цену как оригинальную
        if (originalPrice == null) {
            originalPrice = totalPrice;
        }

        // Устанавливаем процент скидки
        this.discount = discountPercent;

        // Рассчитываем новую цену со скидкой от оригинальной цены
        if (originalPrice > 0) {
            int discountAmount = (originalPrice * discountPercent) / 100;
            this.totalPrice = originalPrice - discountAmount;
        }

        Log.d("Cart", "Discount applied: " + discountPercent + "%, Original: " + originalPrice +
                ", Discount amount: " + getDiscountAmount() + ", New price: " + totalPrice);
    }

    // Обновляем базовую цену (когда добавляются/удаляются услуги)
    public void updateBasePrice(int newPrice) {
        if (discount != null && discount > 0) {
            // Если есть активная скидка, обновляем оригинальную цену и пересчитываем со скидкой
            this.originalPrice = newPrice;
            int discountAmount = (newPrice * discount) / 100;
            this.totalPrice = newPrice - discountAmount;
        } else {
            // Если скидки нет, просто обновляем цену
            this.totalPrice = newPrice;
            this.originalPrice = newPrice;
        }
        Log.d("Cart", "Base price updated to: " + newPrice + ", Final price: " + totalPrice + ", Discount: " + discount + "%");
    }

    // Получаем размер скидки в рублях
    public int getDiscountAmount() {
        if (originalPrice != null && discount != null && discount > 0) {
            return originalPrice - totalPrice;
        }
        return 0;
    }

    // Получаем цену для отображения (с учетом скидки)
    public int getDisplayPrice() {
        return totalPrice;
    }

    // Получаем оригинальную цену для отображения зачеркнутой
    public int getOriginalPriceForDisplay() {
        return originalPrice != null ? originalPrice : totalPrice;
    }

    // Проверяем, есть ли активная скидка
    public boolean hasActiveDiscount() {
        return discount != null && discount > 0 && originalPrice != null;
    }





    public void clearBookingInfo() {
        stationId = null;
        stationAddress = null;
        bookingDate = null;
        bookingTime = null;
        comment = null;
        orderId = null;
        orderStatus = null;
    }
}