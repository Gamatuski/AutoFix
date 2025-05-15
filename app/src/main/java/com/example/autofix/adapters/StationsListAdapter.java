package com.example.autofix.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.autofix.R;
import com.example.autofix.data.entities.Station;
import java.util.List;

public class StationsListAdapter extends RecyclerView.Adapter<StationsListAdapter.ViewHolder> {
    private final List<Station> stations;
    private final StationClickListener listener;

    public StationsListAdapter(List<Station> stations, StationClickListener listener) {
        this.stations = stations;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_station_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Station station = stations.get(position);

        holder.stationAddress.setText(station.getStreet() + ", " + station.getBuilding());
        holder.stationPhone.setText(station.getPhone());
        holder.stationHours.setText(station.getWorkingHours());

        holder.bookButton.setOnClickListener(v -> listener.onBookButtonClick(station));
        holder.itemView.setOnClickListener(v -> listener.onStationClick(station));
    }

    @Override
    public int getItemCount() {
        return stations.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView stationAddress, stationPhone, stationHours;
        final TextView bookButton;

        ViewHolder(View itemView) {
            super(itemView);
            stationAddress = itemView.findViewById(R.id.station_address);
            stationPhone = itemView.findViewById(R.id.station_phone);
            stationHours = itemView.findViewById(R.id.station_hours);
            bookButton = itemView.findViewById(R.id.book_button);
        }
    }

    public interface StationClickListener {
        void onStationClick(Station station);
        void onBookButtonClick(Station station);
    }
}