package com.example.autofix.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.autofix.R;
import com.example.autofix.data.entities.Station;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.List;

public class StationAdapter extends RecyclerView.Adapter<StationAdapter.ViewHolder> {

    private List<Station> originalStations; // Полный неизменяемый список
    private List<Station> filteredStations; // Отфильтрованный список
    private StationClickListener listener;
    public StationAdapter(List<Station> stations, StationClickListener listener) {
        this.originalStations = new ArrayList<>(stations);
        this.filteredStations = new ArrayList<>(stations);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_station, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Station station = filteredStations.get(position);

        // Добавим логирование для отладки
        Log.d("StationAdapter", "Binding station: " + station.getStreet() + ", " + station.getBuilding());

        // Проверка на null
        holder.streetText.setText(station.getStreet() != null ? station.getStreet() : "");
        holder.houseText.setText(station.getBuilding() != null ? station.getBuilding() : "");

        // Настройка кнопки - используем отдельный обработчик для кнопки
        holder.bookButton.setOnClickListener(v -> {
            if (listener != null) {
                // Можно добавить другую логику для кнопки, например, переход к экрану бронирования
                listener.onBookButtonClick(station);
            }
        });

        // Настройка клика на весь элемент - для открытия BottomSheet
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onStationClick(station);
            }
        });
    }


    public void filter(String query) {
        filteredStations.clear();
        if (query.isEmpty()) {
            filteredStations.addAll(originalStations);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (Station station : originalStations) {
                if ((station.getStreet() != null && station.getStreet().toLowerCase().contains(lowerCaseQuery)) ||
                        (station.getCity() != null && station.getCity().toLowerCase().contains(lowerCaseQuery)) ||
                        (station.getBuilding() != null && station.getBuilding().toLowerCase().contains(lowerCaseQuery))) {
                    filteredStations.add(station);
                }
            }
        }
        notifyDataSetChanged();
    }

    // Добавим метод для получения текущего отфильтрованного списка
    public List<Station> getCurrentStations() {
        return filteredStations;
    }

    @Override
    public int getItemCount() {
        return filteredStations.size();
    }

    public void updateStations(List<Station> newStations) {
        originalStations = new ArrayList<>(newStations);
        filteredStations = new ArrayList<>(newStations);
        notifyDataSetChanged();
    }

    public Station getStationAt(int position) {
        if (position >= 0 && position < filteredStations.size()) {
            return filteredStations.get(position);
        }
        return null;
    }

    public List<Station> getOriginalStations() {
        return originalStations;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView streetText;
        TextView houseText;
        TextView bookButton;

        ViewHolder(View itemView) {
            super(itemView);
            streetText = itemView.findViewById(R.id.street_text);
            houseText = itemView.findViewById(R.id.house_text);
            bookButton = itemView.findViewById(R.id.book_button);
        }
    }

    public interface StationClickListener {
        void onStationClick(Station station);
        void onBookButtonClick(Station station);
    }
}