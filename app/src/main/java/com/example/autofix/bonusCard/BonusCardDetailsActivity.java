package com.example.autofix.bonusCard;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.autofix.R;
import com.example.autofix.models.BonusCard;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class BonusCardDetailsActivity extends AppCompatActivity {
    private TextView bonusPointsText;
    private TextView cardStatusText;
    private TextView totalSpentText;
    private TextView statusProgressText;
    private TextView cashbackPercentText;
    private TextView discountPercentText;
    private ProgressBar statusProgressBar;
    private ImageView silverDot, goldDot, rubyDot;
    private CardView cardHistory, infoCard;
    private ImageView btnBack;
    private FirebaseFirestore db;
    private String userId;
    private BonusCard bonusCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bonus_card_details);

        // Инициализация Firebase
        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        // Инициализация View элементов
        initViews();
        // Настройка обработчиков
        setupClickListeners();
        // Загрузка данных
        loadBonusCardData();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        bonusPointsText = findViewById(R.id.bonus_points_text);
        cardStatusText = findViewById(R.id.card_status_text);
        totalSpentText = findViewById(R.id.total_spent_text);
        statusProgressText = findViewById(R.id.status_progress_text);
        cashbackPercentText = findViewById(R.id.cashback_percent_text);
        discountPercentText = findViewById(R.id.discount_percent_text);
        statusProgressBar = findViewById(R.id.status_progress);
        silverDot = findViewById(R.id.silver_dot);
        goldDot = findViewById(R.id.gold_dot);
        rubyDot = findViewById(R.id.ruby_dot);
        cardHistory = findViewById(R.id.card_history);
        infoCard = findViewById(R.id.info_card);

    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        cardHistory.setOnClickListener(v -> {
            Intent intent = new Intent(this, BonusCardHistoryActivity.class);
            startActivity(intent);
        });
        infoCard.setOnClickListener(v -> {
            Intent intent = new Intent(this, BonusCardInfoActivity.class);
            startActivity(intent);
        });
    }

    private void loadBonusCardData() {
        if (userId == null) {
            Log.e("BonusCardDetails", "User not authenticated");
            return;
        }

        // Загружаем данные из двух источников: bonus_card для баллов и users для totalSpent
        loadBonusPoints();
    }

    private void loadBonusPoints() {
        db.collection("users")
                .document(userId)
                .collection("bonus_card")
                .document("card_info")
                .get()
                .addOnSuccessListener(document -> {
                    long points = 0;
                    if (document.exists() && document.contains("points")) {
                        points = document.getLong("points");
                    }
                    // После получения баллов загружаем totalSpent из основного документа пользователя
                    loadTotalSpent(points);
                })
                .addOnFailureListener(e -> {
                    Log.e("BonusCardDetails", "Error loading bonus points", e);
                    // Загружаем totalSpent с нулевыми баллами
                    loadTotalSpent(0);
                });
    }

    private void loadTotalSpent(long points) {
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    int totalSpent = 0;
                    if (document.exists() && document.contains("totalSpent")) {
                        totalSpent = document.getLong("totalSpent").intValue();
                    }
                    // Создаем объект BonusCard с полученными данными
                    bonusCard = new BonusCard(points, totalSpent);
                    updateUI();
                })
                .addOnFailureListener(e -> {
                    Log.e("BonusCardDetails", "Error loading total spent", e);
                    // Показываем данные по умолчанию
                    bonusCard = new BonusCard(points, 0);
                    updateUI();
                });
    }

    private void updateUI() {
        if (bonusCard == null) return;

        // Обновляем текстовые поля
        bonusPointsText.setText(String.valueOf(bonusCard.getPoints()));

        // Устанавливаем текст и фон для статуса карты
        cardStatusText.setText(String.format(
                bonusCard.getCurrentStatus().getDisplayName()));
        cardStatusText.setBackgroundResource(bonusCard.getCurrentStatus().getBackgroundResource());

        totalSpentText.setText(String.format("%,d ₽", bonusCard.getTotalSpent()));
        statusProgressText.setText(bonusCard.getProgressText());
        cashbackPercentText.setText(String.format("%d%%",
                bonusCard.getCurrentStatus().getCashbackPercent()));
        discountPercentText.setText(String.format("%d%%",
                bonusCard.getCurrentStatus().getDiscountPercent()));

        // Обновляем прогресс-бар
        updateProgressBar();
        // Обновляем точки прогресса
        updateProgressDots();
    }

    private void updateProgressBar() {
        int progress = bonusCard.getProgressPercent();
        statusProgressBar.setProgress(progress);
        statusProgressBar.setMax(100);
    }

    private void updateProgressDots() {
        // Серебро всегда активно
        silverDot.setImageResource(bonusCard.isStatusActive(BonusCard.CardStatus.SILVER) ?
                R.drawable.progress_dot_silver : R.drawable.progress_dot_inactive);

        // Золото активно если потрачено >= 40000
        goldDot.setImageResource(bonusCard.isStatusActive(BonusCard.CardStatus.GOLD) ?
                R.drawable.progress_dot_gold : R.drawable.progress_dot_inactive);

        // Рубин активен если потрачено >= 200000
        rubyDot.setImageResource(bonusCard.isStatusActive(BonusCard.CardStatus.RUBY) ?
                R.drawable.progress_dot_ruby : R.drawable.progress_dot_inactive);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Перезагружаем данные при возврате на экран
        loadBonusCardData();
    }
}