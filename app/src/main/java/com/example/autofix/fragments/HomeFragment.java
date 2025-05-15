package com.example.autofix.fragments;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;



import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.example.autofix.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class HomeFragment extends Fragment {

    private FirebaseFirestore db;
    private TextView bonusPointsText, cardStatusText, statusProgressText;
    private ProgressBar statusProgressBar;
    private View silverDot, goldDot, rubyDot;
    private LinearLayout loadingView;
    private CardView bonus_card;
    private Button book_service_button;

    // Пороги для статусов карты
    private static final int SILVER_THRESHOLD = 0;
    private static final int GOLD_THRESHOLD = 40000;
    private static final int RUBY_THRESHOLD = 200000;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Инициализация Firebase
        db = FirebaseFirestore.getInstance();

        // Находим все View элементы
        bonusPointsText = view.findViewById(R.id.bonus_points);
        cardStatusText = view.findViewById(R.id.card_status);
        statusProgressText = view.findViewById(R.id.status_progress_text);
        statusProgressBar = view.findViewById(R.id.status_progress);
        loadingView = view.findViewById(R.id.loading_layout);
        bonus_card = view.findViewById(R.id.bonus_card);
        book_service_button = view.findViewById(R.id.book_service_button);

        // Точки прогресса
        silverDot = view.findViewById(R.id.silver_dot);
        goldDot = view.findViewById(R.id.gold_dot);
        rubyDot = view.findViewById(R.id.ruby_dot);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Загружаем данные пользователя
        loadUserData();
    }

    private void loadUserData() {
        // Показываем индикатор загрузки
        showLoading();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Загружаем данные пользователя
        db.collection("users").document(userId).get()
                .addOnSuccessListener(userDocument -> {
                    if (userDocument.exists()) {
                        // Получаем общую сумму трат
                        Long totalSpentLong = userDocument.getLong("totalSpent");
                        int totalSpent = totalSpentLong != null ? totalSpentLong.intValue() : 0;

                        // Загружаем данные бонусной карты из подколлекции
                        db.collection("users").document(userId)
                                .collection("bonus_card").document("card_info").get()
                                .addOnSuccessListener(cardDocument -> {
                                    if (cardDocument.exists()) {
                                        // Получаем баллы
                                        Long points = cardDocument.getLong("points");
                                        if (points != null) {
                                            bonusPointsText.setText(String.valueOf(points));
                                        }

                                        // Обновляем UI в зависимости от totalSpent
                                        updateCardStatus(totalSpent);
                                    } else {
                                        // Если документ с бонусной картой не найден
                                        Toast.makeText(getContext(), "Информация о бонусной карте не найдена", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(getContext(), "Ошибка при загрузке данных бонусной карты: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }
                    showBonusCard();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Ошибка при загрузке данных пользователя: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateCardStatus(int totalSpent) {
        String currentStatus;
        int nextThreshold;
        int progressValue;
        int backgroundResId;

        // Определяем текущий статус и следующий порог
        if (totalSpent >= RUBY_THRESHOLD) {
            currentStatus = "Рубин";
            nextThreshold = RUBY_THRESHOLD;
            progressValue = 100; // Максимальный прогресс (достигает третьей точки)
            backgroundResId = R.drawable.status_background_ruby;
        } else if (totalSpent >= GOLD_THRESHOLD) {
            currentStatus = "Золото";
            nextThreshold = RUBY_THRESHOLD;
            // Вычисляем процент прогресса между золотом и рубином (от 50% до 100%)
            progressValue = 50 + (int) (((float)(totalSpent - GOLD_THRESHOLD) / (RUBY_THRESHOLD - GOLD_THRESHOLD)) * 50);
            backgroundResId = R.drawable.status_background_gold;
        } else {
            currentStatus = "Серебро";
            nextThreshold = GOLD_THRESHOLD;
            // Вычисляем процент прогресса между серебром и золотом (от 0% до 50%)
            progressValue = (int) (((float)totalSpent / GOLD_THRESHOLD) * 50);
            backgroundResId = R.drawable.status_background_silver;
        }

        // Устанавливаем статус карты
        cardStatusText.setText(currentStatus);
        cardStatusText.setBackgroundResource(backgroundResId);

        // Обновляем прогресс бар (устанавливаем максимум 100 для процентного отображения)
        statusProgressBar.setMax(100);
        statusProgressBar.setProgress(progressValue);

        // Устанавливаем текст с оставшейся суммой
        int remaining = nextThreshold - totalSpent;
        if (totalSpent < RUBY_THRESHOLD) {
            String nextStatus = currentStatus.equals("Серебро") ? "Золото" : "Рубин";
            statusProgressText.setText(String.format("%,d ₽ до статуса «%s»", remaining, nextStatus));
        } else {
            statusProgressText.setText("Максимальный статус достигнут");
        }

        // Обновляем видимость точек прогресса
        updateProgressDots(totalSpent);
    }

    private void updateProgressDots(int totalSpent) {
        // Все точки неактивны по умолчанию
        silverDot.setBackgroundResource(R.drawable.progress_dot_active);
        goldDot.setBackgroundResource(R.drawable.progress_dot_inactive);
        rubyDot.setBackgroundResource(R.drawable.progress_dot_inactive);

        if (totalSpent >= GOLD_THRESHOLD) {
            goldDot.setBackgroundResource(R.drawable.progress_dot_active);
        }
        if (totalSpent >= RUBY_THRESHOLD) {
            rubyDot.setBackgroundResource(R.drawable.progress_dot_active);
        }
    }

    private void showBonusCard() {
        loadingView.setVisibility(View.GONE);
        bonus_card.setVisibility(View.VISIBLE);
        book_service_button.setVisibility(View.VISIBLE);
    }
    private void showLoading() {
        loadingView.setVisibility(View.VISIBLE);
        bonus_card.setVisibility(View.GONE);
        book_service_button.setVisibility(View.GONE);
    }
}