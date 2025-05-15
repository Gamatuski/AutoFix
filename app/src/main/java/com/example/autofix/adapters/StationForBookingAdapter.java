package com.example.autofix.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.autofix.R;
import com.example.autofix.data.entities.Station;

import java.util.ArrayList;
import java.util.List;

public class StationForBookingAdapter extends RecyclerView.Adapter<StationForBookingAdapter.StationViewHolder> {

    private List<Station> stations;
    private OnStationSelectedListener listener;
    private int selectedPosition = -1;

    public interface OnStationSelectedListener {
        void onStationSelected(Station station);
    }

    public StationForBookingAdapter(List<Station> stations, OnStationSelectedListener listener) {
        this.stations = stations != null ? stations : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public StationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_station_booking, parent, false);
        return new StationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StationViewHolder holder, int position) {
        // Получаем актуальную позицию из holder
        final int adapterPosition = holder.getAdapterPosition();

        // Проверяем, что позиция действительна
        if (adapterPosition == RecyclerView.NO_POSITION || adapterPosition >= stations.size()) {
            return;
        }

        // Используем adapterPosition вместо position
        Station station = stations.get(adapterPosition);
        holder.nameTextView.setText(station.getStreet() + " " + station.getBuilding());
        holder.distanceTextView.setText("1.5 км"); // Временное значение

        // Устанавливаем стиль в зависимости от выбранного элемента
        holder.itemView.setBackgroundResource(
                adapterPosition == selectedPosition
                        ? R.drawable.selected_item_border
                        : R.drawable.unselected_item_border
        );

        holder.itemView.setOnClickListener(v -> {
            // Получаем актуальную позицию при клике
            int clickPosition = holder.getAdapterPosition();
            if (clickPosition != RecyclerView.NO_POSITION && listener != null) {
                // Обновляем выбранную позицию
                int previousSelected = selectedPosition;
                selectedPosition = clickPosition;

                // Уведомляем об изменениях для обновления стилей
                notifyItemChanged(previousSelected);
                notifyItemChanged(selectedPosition);

                listener.onStationSelected(stations.get(clickPosition));
            }
        });
    }

    public void setSelectedStationId(String stationId) {
        if (stationId == null) {
            selectedPosition = -1;
            return;
        }

        for (int i = 0; i < stations.size(); i++) {
            if (stationId.equals(stations.get(i).getId())) {
                selectedPosition = i;
                notifyDataSetChanged();
                return;
            }
        }
        selectedPosition = -1;
    }

    @Override
    public int getItemCount() {
        return stations.size();
    }

    public void updateStations(List<Station> newStations) {
        this.stations = newStations != null ? new ArrayList<>(newStations) : new ArrayList<>();
        notifyDataSetChanged();
    }

    static class StationViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        TextView distanceTextView;

        public StationViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.station_name_text);
            distanceTextView = itemView.findViewById(R.id.station_distance_text);
        }
    }
}