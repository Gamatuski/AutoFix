package com.example.autofix.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.autofix.R;
import com.example.autofix.models.Appointment;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class AppointmentsAdapter extends RecyclerView.Adapter<AppointmentsAdapter.AppointmentViewHolder> {

    private List<Appointment> appointments;
    private Context context;
    private OnAppointmentClickListener listener;

    // Интерфейс для обработки кликов
    public interface OnAppointmentClickListener {
        void onAppointmentClick(Appointment appointment);
    }

    public AppointmentsAdapter(Context context, List<Appointment> appointments, OnAppointmentClickListener listener) {
        this.context = context;
        this.appointments = appointments;
        this.listener = listener;
    }



    @NonNull
    @Override
    public AppointmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_appointment, parent, false);
        return new AppointmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppointmentViewHolder holder, int position) {
        Appointment appointment = appointments.get(position);

        // Устанавливаем данные в элементы интерфейса
        holder.carNameTextView.setText(appointment.getCarName());
        holder.stationAddressTextView.setText(appointment.getStationAddress());

        // Форматируем дату и время
        String dateTime = appointment.getBookingDate() + ", " + appointment.getBookingTime();
        holder.bookingDateTimeTextView.setText(dateTime);

        // Форматируем цену
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("ru", "RU"));
        String price = format.format(appointment.getTotalPrice()).replace(",00", "");
        holder.totalPriceTextView.setText(price);


        // Устанавливаем обработчик клика
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAppointmentClick(appointment);
            }
        });
    }

    @Override
    public int getItemCount() {
        return appointments != null ? appointments.size() : 0;
    }

    // Метод для обновления данных
    public void updateAppointments(List<Appointment> newAppointments) {
        this.appointments = newAppointments;
        notifyDataSetChanged();
    }


    // ViewHolder для элемента списка
    static class AppointmentViewHolder extends RecyclerView.ViewHolder {
        TextView carNameTextView;
        TextView stationAddressTextView;
        TextView bookingDateTimeTextView;
        TextView totalPriceTextView;

        public AppointmentViewHolder(@NonNull View itemView) {
            super(itemView);
            carNameTextView = itemView.findViewById(R.id.tv_car_name);
            stationAddressTextView = itemView.findViewById(R.id.tv_station_address);
            bookingDateTimeTextView = itemView.findViewById(R.id.tv_booking_datetime);
            totalPriceTextView = itemView.findViewById(R.id.tv_total_price);
        }
    }
}