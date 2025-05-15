package com.example.autofix.bottomsheets;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.autofix.R;
import com.example.autofix.adapters.StationsListAdapter;
import com.example.autofix.data.entities.Station;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import java.util.List;

public class BottomSheetStationsList extends BottomSheetDialogFragment {

    private List<Station> stations;
    private OnStationSelectedListener listener;

    public interface OnStationSelectedListener {
        void onStationSelected(Station station);
        void onBookButtonClicked(Station station);
    }

    public static BottomSheetStationsList newInstance(List<Station> stations, OnStationSelectedListener listener) {
        BottomSheetStationsList fragment = new BottomSheetStationsList();
        fragment.stations = stations;
        fragment.listener = listener;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_stations_list, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.stations_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        StationsListAdapter adapter = new StationsListAdapter(stations, new StationsListAdapter.StationClickListener() {
            @Override
            public void onStationClick(Station station) {
                if (listener != null) {
                    listener.onStationSelected(station);
                }
                dismiss();
            }

            @Override
            public void onBookButtonClick(Station station) {
                if (listener != null) {
                    listener.onBookButtonClicked(station);
                }
                dismiss();
            }
        });

        recyclerView.setAdapter(adapter);

        return view;
    }
}