package com.example.autofix.adapters;

import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.autofix.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class TimeSlotAdapter extends RecyclerView.Adapter<TimeSlotAdapter.TimeSlotViewHolder> {
    private List<String> timeSlots = new ArrayList<>();
    private List<Boolean> availability = new ArrayList<>();
    private OnTimeSlotSelectedListener listener;
    private int selectedPosition = -1;
    private String selectedDate;

    public interface OnTimeSlotSelectedListener {
        void onTimeSlotSelected(String timeSlot);
    }

    public TimeSlotAdapter(OnTimeSlotSelectedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public TimeSlotViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_time_slot, parent, false);
        return new TimeSlotViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TimeSlotViewHolder holder, int position) {
        final int adapterPosition = holder.getAdapterPosition();
        if (adapterPosition == RecyclerView.NO_POSITION || adapterPosition >= timeSlots.size()) {
            return;
        }

        String timeSlot = timeSlots.get(adapterPosition);
        boolean isAvailable = adapterPosition < availability.size() ? availability.get(adapterPosition) : false;

        holder.timeTextView.setText(timeSlot);

        // Логируем для отладки
        Log.d("TimeSlotAdapter", "Binding slot " + adapterPosition + ": " + timeSlot + " = " + isAvailable);

        if (!isAvailable) {
            // Слот недоступен (забронирован или прошедшее время)
            holder.cardView.setCardBackgroundColor(Color.parseColor("#CCCCCC"));
            holder.timeTextView.setTextColor(Color.parseColor("#666666"));
            holder.itemView.setEnabled(false);
            holder.itemView.setOnClickListener(null);
            holder.itemView.setBackgroundResource(R.drawable.unselected_item_border);
            holder.itemView.setAlpha(0.5f); // Делаем полупрозрачным
        } else {
            // Слот доступен
            holder.itemView.setBackgroundResource(
                    adapterPosition == selectedPosition
                            ? R.drawable.selected_item_border
                            : R.drawable.unselected_item_border
            );
            holder.cardView.setCardBackgroundColor(Color.WHITE);
            holder.timeTextView.setTextColor(Color.BLACK);
            holder.itemView.setEnabled(true);
            holder.itemView.setAlpha(1.0f); // Полная непрозрачность

            holder.itemView.setOnClickListener(v -> {
                int clickedPosition = holder.getAdapterPosition();
                if (clickedPosition != RecyclerView.NO_POSITION && clickedPosition < timeSlots.size()) {
                    if (selectedPosition != clickedPosition) {
                        int previousSelected = selectedPosition;
                        selectedPosition = clickedPosition;

                        if (previousSelected != -1) {
                            notifyItemChanged(previousSelected);
                        }
                        notifyItemChanged(selectedPosition);

                        if (listener != null) {
                            listener.onTimeSlotSelected(timeSlots.get(clickedPosition));
                        }
                    }
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return timeSlots.size();
    }

    // Метод для полной очистки данных
    public void clearTimeSlots() {
        this.timeSlots.clear();
        this.availability.clear();
        this.selectedPosition = -1;
        this.selectedDate = null;
        notifyDataSetChanged();
    }

    // Основной метод для обновления слотов с учетом даты
    public void updateTimeSlots(Map<String, Boolean> timesMap, String selectedDate) {
        // Очищаем предыдущие данные
        this.timeSlots.clear();
        this.availability.clear();
        this.selectedPosition = -1;
        this.selectedDate = selectedDate;

        if (timesMap == null || timesMap.isEmpty()) {
            notifyDataSetChanged();
            return;
        }

        // Фильтруем слоты если выбран сегодняшний день
        Map<String, Boolean> filteredMap = filterTimeSlotsForToday(timesMap, selectedDate);

        // Сортируем по времени
        TreeMap<String, Boolean> sortedMap = new TreeMap<>(new TimeComparator());
        sortedMap.putAll(filteredMap);

        for (Map.Entry<String, Boolean> entry : sortedMap.entrySet()) {
            this.timeSlots.add(entry.getKey());
            this.availability.add(entry.getValue());
        }

        notifyDataSetChanged();
    }

    // Перегруженный метод для обратной совместимости
    public void updateTimeSlots(Map<String, Boolean> timesMap) {
        updateTimeSlots(timesMap, getCurrentDate());
    }

    public void updateTimeSlots(List<String> availableSlots, String selectedDate) {
        if (availableSlots == null) {
            clearTimeSlots();
            return;
        }

        // Создаем Map где все слоты доступны
        Map<String, Boolean> timesMap = new HashMap<>();
        for (String slot : availableSlots) {
            timesMap.put(slot, true);
        }

        updateTimeSlots(timesMap, selectedDate);
    }

    // Метод для фильтрации слотов времени для сегодняшнего дня
    private Map<String, Boolean> filterTimeSlotsForToday(Map<String, Boolean> timesMap, String selectedDate) {
        String today = getCurrentDate();

        // Если выбранная дата не сегодня, возвращаем все слоты
        if (!today.equals(selectedDate)) {
            return new HashMap<>(timesMap);
        }

        // Получаем текущее время
        Calendar now = Calendar.getInstance();
        int currentHour = now.get(Calendar.HOUR_OF_DAY);
        int currentMinute = now.get(Calendar.MINUTE);

        // Вычисляем следующий доступный час
        int nextAvailableHour = currentHour + 1;
        if (currentMinute == 0) {
            nextAvailableHour = currentHour + 1;
        }

        Map<String, Boolean> filteredMap = new HashMap<>();

        for (Map.Entry<String, Boolean> entry : timesMap.entrySet()) {
            String timeSlot = entry.getKey();
            Boolean isAvailable = entry.getValue();

            try {
                // Парсим время слота
                int slotHour = parseHourFromTimeSlot(timeSlot);

                if (slotHour != -1) {
                    // Если слот в прошлом, делаем его недоступным
                    if (slotHour < nextAvailableHour) {
                        filteredMap.put(timeSlot, false);
                    } else {
                        // Сохраняем оригинальную доступность
                        filteredMap.put(timeSlot, isAvailable);
                    }
                } else {
                    // Если не можем распарсить время, сохраняем как есть
                    filteredMap.put(timeSlot, isAvailable);
                }
            } catch (Exception e) {
                // Если ошибка парсинга, сохраняем слот как есть
                filteredMap.put(timeSlot, isAvailable);
            }
        }

        return filteredMap;
    }

    // Вспомогательный метод для парсинга часа из строки времени
    private int parseHourFromTimeSlot(String timeSlot) {
        try {
            String[] timeParts = timeSlot.split(":");
            if (timeParts.length >= 2) {
                return Integer.parseInt(timeParts[0].trim());
            }
        } catch (NumberFormatException e) {
            // Игнорируем ошибку парсинга
        }
        return -1;
    }

    // Метод для получения текущей даты в нужном формате
    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }

    // Добавьте этот метод в класс TimeSlotAdapter
    public void markSlotAsBooked(String timeSlot) {
        for (int i = 0; i < timeSlots.size(); i++) {
            if (timeSlots.get(i).equals(timeSlot)) {
                if (i < availability.size()) {
                    availability.set(i, false);
                    notifyItemChanged(i);
                    Log.d("TimeSlotAdapter", "Marked slot as booked: " + timeSlot);
                }
                break;
            }
        }
    }


    // Компаратор для сортировки времени
    private static class TimeComparator implements Comparator<String> {
        @Override
        public int compare(String time1, String time2) {
            try {
                String[] parts1 = time1.split(":");
                String[] parts2 = time2.split(":");

                if (parts1.length >= 2 && parts2.length >= 2) {
                    int hour1 = Integer.parseInt(parts1[0].trim());
                    int minute1 = Integer.parseInt(parts1[1].trim());
                    int hour2 = Integer.parseInt(parts2[0].trim());
                    int minute2 = Integer.parseInt(parts2[1].trim());

                    if (hour1 != hour2) {
                        return Integer.compare(hour1, hour2);
                    }
                    return Integer.compare(minute1, minute2);
                }
            } catch (NumberFormatException e) {
                // Если не можем распарсить, используем обычное сравнение строк
            }
            return time1.compareTo(time2);
        }
    }

    public String getSelectedTimeSlot() {
        if (selectedPosition >= 0 && selectedPosition < timeSlots.size()) {
            return timeSlots.get(selectedPosition);
        }
        return null;
    }

    public List<String> getTimeSlots() {
        return new ArrayList<>(timeSlots);
    }

    public void setSelectedDate(String selectedDate) {
        this.selectedDate = selectedDate;
    }

    public String getSelectedDate() {
        return selectedDate;
    }

    // Метод для проверки наличия данных
    public boolean hasData() {
        return !timeSlots.isEmpty();
    }

    // Метод для сброса выбора
    public void clearSelection() {
        if (selectedPosition != -1) {
            int oldPosition = selectedPosition;
            selectedPosition = -1;
            notifyItemChanged(oldPosition);
        }
    }

    static class TimeSlotViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView timeTextView;

        public TimeSlotViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_view);
            timeTextView = itemView.findViewById(R.id.time_text);
        }
    }
}