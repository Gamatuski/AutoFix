package com.example.autofix.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.autofix.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DateAdapter extends RecyclerView.Adapter<DateAdapter.DateViewHolder> {

    private List<String> dates = new ArrayList<>();
    private OnDateSelectedListener listener;
    private int selectedPosition = -1;

    public interface OnDateSelectedListener {
        void onDateSelected(String date);
    }

    public DateAdapter(OnDateSelectedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public DateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_date, parent, false);
        return new DateViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DateViewHolder holder, int position) {
        final int adapterPosition = holder.getAdapterPosition();
        if (adapterPosition == RecyclerView.NO_POSITION || adapterPosition >= dates.size()) {
            return;
        }

        String date = dates.get(adapterPosition); // ← исправлено: использовать adapterPosition
        String[] parts = date.split("\\.");
        String day = parts[0];
        String month = parts[1];
        String year = parts[2];

        holder.weekDayTextView.setText(getWeekDay(date));
        holder.dayTextView.setText(day);
        holder.monthTextView.setText(getMonthName(month));
        holder.yearTextView.setText(year);

        // Устанавливаем стиль в зависимости от выбранного элемента
        holder.itemView.setBackgroundResource(
                adapterPosition == selectedPosition
                        ? R.drawable.selected_item_border
                        : R.drawable.unselected_item_border
        );

        holder.itemView.setOnClickListener(v -> {
            int clickedPosition = holder.getAdapterPosition();
            if (clickedPosition != RecyclerView.NO_POSITION && clickedPosition < dates.size()) {
                if (selectedPosition != clickedPosition) {
                    int previousSelected = selectedPosition;
                    selectedPosition = clickedPosition;

                    notifyItemChanged(previousSelected); // Старая позиция
                    notifyItemChanged(selectedPosition);  // Новая позиция

                    if (listener != null) {
                        listener.onDateSelected(dates.get(clickedPosition));
                    }
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return dates.size();
    }

    public void updateDates(List<String> newDates) {
        this.dates = newDates != null ? new ArrayList<>(newDates) : new ArrayList<>();
        notifyDataSetChanged();
    }

    private String getMonthName(String month) {
        String[] months = {"Января", "Февраля", "Марта", "Апрля", "Мая", "Июня", "Июля", "Августа", "Сентября", "Октября", "Ноября", "Декабря"};
        try {
            return months[Integer.parseInt(month) - 1];
        } catch (Exception e) {
            return month;
        }
    }

    private String getWeekDay(String date) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
            Date parsedDate = sdf.parse(date);
            Calendar cal = Calendar.getInstance();
            cal.setTime(parsedDate);

            String[] days = {"Воскресенье", "Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота"};
            return days[cal.get(Calendar.DAY_OF_WEEK) - 1];
        } catch (Exception e) {
            return "Дн";
        }
    }

    static class DateViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView weekDayTextView;
        TextView dayTextView;
        TextView monthTextView;
        TextView yearTextView;

        public DateViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_view);
            weekDayTextView = itemView.findViewById(R.id.weekday_text);
            dayTextView = itemView.findViewById(R.id.day_text);
            monthTextView = itemView.findViewById(R.id.month_text);
            yearTextView = itemView.findViewById(R.id.year_text);
        }
    }
}