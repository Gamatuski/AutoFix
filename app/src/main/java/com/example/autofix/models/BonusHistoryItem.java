package com.example.autofix.models;

import java.util.Date;

public class BonusHistoryItem {
    private long points;
    private String message;
    private String type; // "EARNED", "SPENT", etc.
    private Date timestamp;
    private long currentBalance;

    // Конструктор по умолчанию для Firebase
    public BonusHistoryItem() {}

    public BonusHistoryItem(long points, String message, String type, Date timestamp, long currentBalance) {
        this.points = points;
        this.message = message;
        this.type = type;
        this.timestamp = timestamp;
        this.currentBalance = currentBalance;
    }

    // Геттеры и сеттеры
    public long getPoints() { return points; }
    public void setPoints(long points) { this.points = points; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    public long getCurrentBalance() { return currentBalance; }
    public void setCurrentBalance(long currentBalance) { this.currentBalance = currentBalance; }
}