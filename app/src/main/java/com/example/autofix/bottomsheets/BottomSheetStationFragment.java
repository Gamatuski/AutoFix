package com.example.autofix.bottomsheets;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.autofix.R;
import com.example.autofix.adapters.PhotoAdapter;
import com.example.autofix.data.entities.Station;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

public class BottomSheetStationFragment extends BottomSheetDialogFragment {

    private static final String ARG_STATION = "station";

    private Station station;
    private PhotoAdapter photoAdapter;

    public static BottomSheetStationFragment newInstance(Station station) {
        BottomSheetStationFragment fragment = new BottomSheetStationFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_STATION, station);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            station = getArguments().getParcelable(ARG_STATION);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_station, container, false);

        // Инициализация элементов UI
        TextView streetText = view.findViewById(R.id.street_text);
        TextView buildingText = view.findViewById(R.id.building_text);
        TextView workingHoursText = view.findViewById(R.id.working_hours_text);
        RecyclerView photosRecycler = view.findViewById(R.id.photos_recycler);
        TextView servicesText = view.findViewById(R.id.services_text);
        TextView amenitiesText = view.findViewById(R.id.amenities_text);
        Button bookButton = view.findViewById(R.id.book_button);

        // Установка данных станции
        if (station != null) {
            streetText.setText(station.getStreet());
            buildingText.setText(station.getBuilding());
            workingHoursText.setText(station.getWorkingHours());

            // Настройка RecyclerView для фотографий
            List<String> photos = station.getPhotos() != null ? station.getPhotos() : new ArrayList<>();
            photoAdapter = new PhotoAdapter(photos);
            photosRecycler.setLayoutManager(new LinearLayoutManager(
                    getContext(), LinearLayoutManager.HORIZONTAL, false));
            photosRecycler.setAdapter(photoAdapter);

            // Форматирование списков услуг и удобств с разделителем " • "
            if (station.getServices() != null && !station.getServices().isEmpty()) {
                servicesText.setText(android.text.TextUtils.join(" • ", station.getServices()));
            } else {
                servicesText.setText(R.string.no_services_available);
            }

            if (station.getAmenities() != null && !station.getAmenities().isEmpty()) {
                amenitiesText.setText(android.text.TextUtils.join(" • ", station.getAmenities()));
            } else {
                amenitiesText.setText(R.string.no_amenities_available);
            }

            // Обработчик кнопки записи
            bookButton.setOnClickListener(v -> {
                // Здесь можно добавить логику записи
                dismiss();
            });
        }

        return view;
    }
}