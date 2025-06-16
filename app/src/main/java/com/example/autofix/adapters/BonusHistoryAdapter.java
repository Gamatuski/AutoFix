package com.example.autofix.adapters;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.autofix.R;
import com.example.autofix.models.BonusHistoryItem;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BonusHistoryAdapter extends RecyclerView.Adapter<BonusHistoryAdapter.HistoryViewHolder> {

    private List<BonusHistoryItem> historyItems;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat timeFormat;

    public BonusHistoryAdapter(List<BonusHistoryItem> historyItems) {
        this.historyItems = historyItems;
        this.dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        this.timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_bonus_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        BonusHistoryItem item = historyItems.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return historyItems.size();
    }

    class HistoryViewHolder extends RecyclerView.ViewHolder {
        private ImageView iconImageView;
        private TextView messageTextView;
        private TextView dateTextView;
        private TextView timeTextView;
        private TextView pointsTextView;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            iconImageView = itemView.findViewById(R.id.icon_image_view);
            messageTextView = itemView.findViewById(R.id.message_text_view);
            dateTextView = itemView.findViewById(R.id.date_text_view);
            timeTextView = itemView.findViewById(R.id.time_text_view);
            pointsTextView = itemView.findViewById(R.id.points_text_view);
        }

        public void bind(BonusHistoryItem item) {
            // Устанавливаем сообщение
            messageTextView.setText(item.getMessage());

            // Форматируем и устанавливаем дату и время
            if (item.getTimestamp() != null) {
                Date timestamp = item.getTimestamp();
                dateTextView.setText(dateFormat.format(timestamp));
                timeTextView.setText(timeFormat.format(timestamp));
            }

            // Устанавливаем баллы и стилизацию в зависимости от типа операции
            long points = item.getPoints();

            if ("EARNED".equals(item.getType())) {
                // Начисление баллов
                setupEarnedPoints(points);
            } else if ("SPENT".equals(item.getType())) {
                // Списание баллов
                setupSpentPoints(points);
            } else {
                // Другие операции - используем стиль начисления по умолчанию
                setupEarnedPoints(points);
            }
        }

        private void setupEarnedPoints(long points) {
            // Настройка для начисления баллов
            String pointsText = String.format("+%,d баллов", points);
            pointsTextView.setText(pointsText);
            pointsTextView.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.green_second));
            pointsTextView.setBackground(ContextCompat.getDrawable(itemView.getContext(), R.drawable.status_background_silver));
            pointsTextView.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(itemView.getContext(), R.color.gray_light)));

            // Настройка иконки для начисления
            iconImageView.setImageResource(R.drawable.plus_ic);
            iconImageView.setBackground(ContextCompat.getDrawable(itemView.getContext(), R.drawable.gradient_circle));
            iconImageView.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(itemView.getContext(), R.color.green)));
            iconImageView.setImageTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(itemView.getContext(), R.color.white)));
        }

        private void setupSpentPoints(long points) {
            // Настройка для списания баллов
            String pointsText = String.format("-%,d баллов", points);
            pointsTextView.setText(pointsText);
            pointsTextView.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.black));
            pointsTextView.setBackground(ContextCompat.getDrawable(itemView.getContext(), R.drawable.status_background_silver));
            pointsTextView.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(itemView.getContext(), R.color.silver_color)));

            // Настройка иконки для списания
            iconImageView.setImageResource(R.drawable.minus_ic);
            iconImageView.setBackground(ContextCompat.getDrawable(itemView.getContext(), R.drawable.gradient_circle));
            iconImageView.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(itemView.getContext(), R.color.secondary_color)));
            iconImageView.setImageTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(itemView.getContext(), R.color.black)));
        }
    }
}