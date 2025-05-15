package com.example.autofix.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.autofix.R;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class TimeSlotAdapter extends RecyclerView.Adapter<TimeSlotAdapter.TimeSlotViewHolder> {
    private List<String> timeSlots = new ArrayList<>();
    private List<Boolean> availability = new ArrayList<>();
    private OnTimeSlotSelectedListener listener;
    private int selectedPosition = -1;

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

        if (adapterPosition == RecyclerView.NO_POSITION ||
                adapterPosition >= timeSlots.size()) {
            return;
        }

        String timeSlot = timeSlots.get(adapterPosition);
        boolean isAvailable = adapterPosition < availability.size() ?
                availability.get(adapterPosition) : false;

        holder.timeTextView.setText(timeSlot);

        if (!isAvailable) {
            holder.cardView.setCardBackgroundColor(Color.LTGRAY);
            holder.itemView.setEnabled(false);
            holder.itemView.setOnClickListener(null);
        } else {
            // Устанавливаем стиль в зависимости от выбранного элемента
            holder.itemView.setBackgroundResource(
                    adapterPosition == selectedPosition
                            ? R.drawable.selected_item_border
                            : R.drawable.unselected_item_border
            );

            holder.cardView.setCardBackgroundColor(Color.WHITE);
            holder.itemView.setEnabled(true);
            holder.itemView.setOnClickListener(v -> {
                int clickedPosition = holder.getAdapterPosition();
                if (clickedPosition != RecyclerView.NO_POSITION &&
                        clickedPosition < timeSlots.size()) {

                    if (selectedPosition != clickedPosition) {
                        int previousSelected = selectedPosition;
                        selectedPosition = clickedPosition;

                        notifyItemChanged(previousSelected); // Старая позиция
                        notifyItemChanged(selectedPosition);  // Новая позиция

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

    public void updateTimeSlots(Map<String, Boolean> timesMap) {
        TreeMap<String, Boolean> sortedMap = new TreeMap<>(timesMap);

        this.timeSlots.clear();
        this.availability.clear();

        for (Map.Entry<String, Boolean> entry : sortedMap.entrySet()) {
            this.timeSlots.add(entry.getKey());
            this.availability.add(entry.getValue());
        }

        selectedPosition = -1; // Сбрасываем выбор
        notifyDataSetChanged();
    }

    public void updateTimeSlots(List<String> availableSlots) {
        this.timeSlots.clear();
        this.availability.clear();

        Collections.sort(availableSlots);

        for (String slot : availableSlots) {
            this.timeSlots.add(slot);
            this.availability.add(true);
        }

        selectedPosition = -1; // Сбрасываем выбор
        notifyDataSetChanged();
    }

    public String getSelectedTimeSlot() {
        if (selectedPosition >= 0 && selectedPosition < timeSlots.size()) {
            return timeSlots.get(selectedPosition);
        }
        return null;
    }

    public List<String> getTimeSlots() {
        return timeSlots;
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