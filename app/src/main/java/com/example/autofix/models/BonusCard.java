package com.example.autofix.models;

import android.util.Log;

import com.example.autofix.R;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class BonusCard {
    // Пороги для статусов карты
    public static final int SILVER_THRESHOLD = 0;
    public static final int GOLD_THRESHOLD = 40000;
    public static final int RUBY_THRESHOLD = 200000;

    // Статусы карты
    public enum CardStatus {
        SILVER("Серебро", 4, 0, R.drawable.status_background_silver),
        GOLD("Золото", 8, 5, R.drawable.status_background_gold),
        RUBY("Рубин", 12, 10, R.drawable.status_background_ruby);

        private final String displayName;
        private final int cashbackPercent;
        private final int discountPercent;
        private final int backgroundResource;

        // Данные карты


        CardStatus(String displayName, int cashbackPercent, int discountPercent, int backgroundResource) {
            this.displayName = displayName;
            this.cashbackPercent = cashbackPercent;
            this.discountPercent = discountPercent;
            this.backgroundResource = backgroundResource;
        }

        public String getDisplayName() { return displayName; }
        public int getCashbackPercent() { return cashbackPercent; }
        public int getDiscountPercent() { return discountPercent; }
        public int getBackgroundResource() { return backgroundResource; }
    }

    // Данные карты
    private long points;
    private int totalSpent;
    private CardStatus currentStatus;

    private FirebaseFirestore db;
    private String userId;

    public BonusCard(long points, int totalSpent) {
        this.points = points;
        this.totalSpent = totalSpent;
        this.currentStatus = determineCardStatus(totalSpent);
    }

    // Определяем статус карты на основе потраченной суммы
    private CardStatus determineCardStatus(int totalSpent) {
        if (totalSpent >= RUBY_THRESHOLD) {
            return CardStatus.RUBY;
        } else if (totalSpent >= GOLD_THRESHOLD) {
            return CardStatus.GOLD;
        } else {
            return CardStatus.SILVER;
        }
    }

    // Получаем следующий статус
    public CardStatus getNextStatus() {
        switch (currentStatus) {
            case SILVER:
                return CardStatus.GOLD;
            case GOLD:
                return CardStatus.RUBY;
            case RUBY:
            default:
                return null; // Максимальный статус достигнут
        }
    }

    // Получаем следующий порог
    public int getNextThreshold() {
        switch (currentStatus) {
            case SILVER:
                return GOLD_THRESHOLD;
            case GOLD:
                return RUBY_THRESHOLD;
            case RUBY:
            default:
                return RUBY_THRESHOLD; // Максимальный порог
        }
    }

    // Получаем сумму до следующего статуса
    public int getAmountToNextStatus() {
        if (currentStatus == CardStatus.RUBY) {
            return 0; // Максимальный статус достигнут
        }
        return getNextThreshold() - totalSpent;
    }

    // Получаем прогресс в процентах (0-100)
    public int getProgressPercent() {
        if (totalSpent >= RUBY_THRESHOLD) {
            return 100; // Максимальный прогресс
        } else if (totalSpent >= GOLD_THRESHOLD) {
            // Прогресс между золотом и рубином (50% - 100%)
            return 50 + (int) (((float)(totalSpent - GOLD_THRESHOLD) / (RUBY_THRESHOLD - GOLD_THRESHOLD)) * 50);
        } else {
            // Прогресс между серебром и золотом (0% - 50%)
            return (int) (((float)totalSpent / GOLD_THRESHOLD) * 50);
        }
    }

    // Проверяем, активна ли точка прогресса для определенного статуса
    public boolean isStatusActive(CardStatus status) {
        Log.d("BonusCard", "Checking status: " + status + ", totalSpent: " + totalSpent);

        switch (status) {
            case SILVER:
                // Серебро всегда активно (начальный статус)
                boolean silverActive = totalSpent >= SILVER_THRESHOLD;
                Log.d("BonusCard", "Silver active: " + silverActive);
                return silverActive;

            case GOLD:
                // Золото активно, если потрачено >= 40000
                boolean goldActive = totalSpent >= GOLD_THRESHOLD;
                Log.d("BonusCard", "Gold active: " + goldActive + " (threshold: " + GOLD_THRESHOLD + ")");
                return goldActive;

            case RUBY:
                // Рубин активен, если потрачено >= 200000
                boolean rubyActive = totalSpent >= RUBY_THRESHOLD;
                Log.d("BonusCard", "Ruby active: " + rubyActive + " (threshold: " + RUBY_THRESHOLD + ")");
                return rubyActive;

            default:
                return false;
        }
    }

    // Рассчитываем кешбек для суммы
    public int calculateCashback(int amount) {
        return (int) (amount * currentStatus.getCashbackPercent() / 100.0);
    }

    // Рассчитываем скидку для суммы
    public int calculateDiscount(int amount) {
        return (int) (amount * currentStatus.getDiscountPercent() / 100.0);
    }

    // Применяем скидку к сумме
    // Применяем скидку к сумме и возвращаем новую сумму
    public int applyDiscount(int originalAmount) {
        int discountAmount = calculateDiscount(originalAmount);
        return originalAmount - discountAmount;
    }

    // Получаем размер скидки в рублях
    public int getDiscountAmount(int originalAmount) {
        return calculateDiscount(originalAmount);
    }

    // Получаем текст прогресса
    public String getProgressText() {
        if (currentStatus == CardStatus.RUBY) {
            return "Максимальный статус достигнут";
        } else {
            CardStatus nextStatus = getNextStatus();
            int remaining = getAmountToNextStatus();
            return String.format("%,d ₽ до статуса «%s»", remaining, nextStatus.getDisplayName());
        }
    }

    // Получаем описание преимуществ текущего статуса
    public String getStatusBenefits() {
        return String.format("Кешбек: %d%% • Скидка: %d%%",
                currentStatus.getCashbackPercent(),
                currentStatus.getDiscountPercent());
    }

    // Геттеры
    public long getPoints() { return points; }
    public int getTotalSpent() { return totalSpent; }
    public CardStatus getCurrentStatus() { return currentStatus; }

    // Сеттеры
    public void setPoints(long points) {
        this.points = points;
    }

    public void setTotalSpent(int totalSpent) {
        this.totalSpent = totalSpent;
        this.currentStatus = determineCardStatus(totalSpent);
    }

    // Добавляем баллы
    public void addPoints(long pointsToAdd) {
        this.points += pointsToAdd;
    }

    // Списываем баллы
    public boolean spendPoints(long pointsToSpend) {
        if (points >= pointsToSpend) {
            points -= pointsToSpend;
            return true;
        }
        return false;
    }

    // Добавляем к потраченной сумме
    public void addToTotalSpent(int amount) {
        this.totalSpent += amount;
        this.currentStatus = determineCardStatus(this.totalSpent);
    }

    public void addPointsToFirebase(long pointsToAdd, String message, BonusCardCallback callback) {
        if (userId == null) {
            Log.e("BonusCard", "User ID is null");
            if (callback != null) callback.onFailure("Пользователь не авторизован");
            return;
        }

        // Обновляем баллы в основном документе бонусной карты
        DocumentReference bonusCardRef = db.collection("users")
                .document(userId)
                .collection("bonus_card")
                .document("card_data");

        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(bonusCardRef);

            long currentPoints = 0;
            if (snapshot.exists() && snapshot.contains("points")) {
                currentPoints = snapshot.getLong("points");
            }

            long newPoints = currentPoints + pointsToAdd;

            // Обновляем или создаем документ с баллами
            Map<String, Object> cardData = new HashMap<>();
            cardData.put("points", newPoints);
            cardData.put("totalSpent", totalSpent);
            cardData.put("currentStatus", currentStatus.name());
            cardData.put("lastUpdated", new Date());

            transaction.set(bonusCardRef, cardData, SetOptions.merge());

            return newPoints;
        }).addOnSuccessListener(newPoints -> {
            // Обновляем локальные данные
            this.points = newPoints;

            // Добавляем запись в историю
            addToCardHistory(pointsToAdd, message, "EARNED", callback);

            Log.d("BonusCard", "Points successfully added: " + pointsToAdd);
        }).addOnFailureListener(e -> {
            Log.e("BonusCard", "Error adding points to Firebase", e);
            if (callback != null) callback.onFailure("Ошибка при добавлении баллов");
        });
    }

    private void addToCardHistory(long points, String message, String type, BonusCardCallback callback) {
        if (userId == null) {
            Log.e("BonusCard", "User ID is null");
            if (callback != null) callback.onFailure("Пользователь не авторизован");
            return;
        }

        Map<String, Object> historyData = new HashMap<>();
        historyData.put("points", points);
        historyData.put("message", message);
        historyData.put("type", type);
        historyData.put("timestamp", new Date());
        historyData.put("currentBalance", this.points);

        db.collection("users")
                .document(userId)
                .collection("bonus_card")
                .document("card_history")
                .collection("history")
                .add(historyData)
                .addOnSuccessListener(documentReference -> {
                    Log.d("BonusCard", "History record added with ID: " + documentReference.getId());
                    if (callback != null) callback.onSuccess("Операция выполнена успешно");
                })
                .addOnFailureListener(e -> {
                    Log.e("BonusCard", "Error adding history record");

                });
    }

    public void spendPointsToFirebase(long pointsToSpend, String message, BonusCardCallback callback) {
        if (userId == null) {
            Log.e("BonusCard", "User ID is null");
            if (callback != null) callback.onFailure("Пользователь не авторизован");
            return;
        }

        // Проверяем и списываем баллы в основном документе бонусной карты
        DocumentReference bonusCardRef = db.collection("users")
                .document(userId)
                .collection("bonus_card")
                .document("card_data");

        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(bonusCardRef);
            long currentPoints = 0;
            if (snapshot.exists() && snapshot.contains("points")) {
                currentPoints = snapshot.getLong("points");
            }

            if (currentPoints < pointsToSpend) {
                throw new RuntimeException("Недостаточно баллов для списания");
            }

            long newPoints = currentPoints - pointsToSpend;

            // Обновляем документ с баллами
            Map<String, Object> cardData = new HashMap<>();
            cardData.put("points", newPoints);
            cardData.put("totalSpent", totalSpent);
            cardData.put("currentStatus", currentStatus.name());
            cardData.put("lastUpdated", new Date());

            transaction.set(bonusCardRef, cardData, SetOptions.merge());
            return newPoints;
        }).addOnSuccessListener(newPoints -> {
            // Обновляем локальные данные
            this.points = newPoints;
            // Добавляем запись в историю
            addToCardHistory(pointsToSpend, message, "SPENT", callback);
            Log.d("BonusCard", "Points successfully spent: " + pointsToSpend);
        }).addOnFailureListener(e -> {
            Log.e("BonusCard", "Error spending points", e);
            if (callback != null) callback.onFailure(e.getMessage());
        });
    }

    public interface BonusCardCallback {
        void onSuccess(String message);
        void onFailure(String error);
    }

}