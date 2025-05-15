package com.example.autofix.data.entities;

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
    private int totalDuration;

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





    // Метод для добавления элемента в корзину


    // Метод для удаления элемента из корзины


    // Метод для очистки корзины


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