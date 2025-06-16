package com.example.autofix.adapters;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.autofix.R;
import com.example.autofix.models.Appointment;
import com.facebook.shimmer.ShimmerFrameLayout;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class AppointmentsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_LOADING = 0;
    private static final int TYPE_APPOINTMENT = 1;

    private List<Appointment> appointments;
    private Context context;
    private OnAppointmentClickListener listener;
    private boolean isLoading = false;

    // Интерфейс для обработки кликов
    public interface OnAppointmentClickListener {
        void onAppointmentClick(Appointment appointment);
    }

    public AppointmentsAdapter(Context context, List<Appointment> appointments, OnAppointmentClickListener listener) {
        this.context = context;
        this.appointments = appointments;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        if (isLoading && appointments.isEmpty()) {
            return TYPE_LOADING;
        }
        return TYPE_APPOINTMENT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_LOADING) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_appointment_skeleton, parent, false);
            return new LoadingViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_appointment, parent, false);
            return new AppointmentViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof LoadingViewHolder) {
            // Skeleton элементы не требуют привязки данных
            return;
        }

        AppointmentViewHolder appointmentHolder = (AppointmentViewHolder) holder;
        Appointment appointment = appointments.get(position);

        // Устанавливаем данные в элементы интерфейса
        appointmentHolder.carNameTextView.setText(appointment.getCarName());
        appointmentHolder.stationAddressTextView.setText(appointment.getStationAddress());

        // Форматируем дату и время
        String dateTime = appointment.getBookingDate() + ", " + appointment.getBookingTime();
        appointmentHolder.bookingDateTimeTextView.setText(dateTime);

        // Проверяем тип записи и отображаем соответствующую информацию
        if (appointment.isQuickBooking()) {
            // Для быстрой записи показываем "Быстрая запись" с иконкой молнии
            appointmentHolder.priceLayout.setVisibility(View.GONE);
            appointmentHolder.quickBookingLayout.setVisibility(View.VISIBLE);
        } else {
            // Для обычной записи показываем цену
            appointmentHolder.priceLayout.setVisibility(View.VISIBLE);
            appointmentHolder.quickBookingLayout.setVisibility(View.GONE);
            // Форматируем цену
            NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("ru", "RU"));
            String price = format.format(appointment.getTotalPrice()).replace(",00", "");
            appointmentHolder.totalPriceTextView.setText(price);
        }

        // Проверяем статус и изменяем внешний вид для завершенных записей
        if ("completed".equals(appointment.getStatus())) {
            // Меняем фон карточки на серый
            appointmentHolder.itemView.setBackgroundResource(R.drawable.completed_appointment_background);
            // Показываем надпись "Выполнен"
            appointmentHolder.statusTextView.setVisibility(View.VISIBLE);
            appointmentHolder.statusTextView.setText("Выполнен");
        } else {
            // Обычный фон для других статусов
            appointmentHolder.itemView.setBackgroundResource(R.drawable.default_appointment_background);
            appointmentHolder.statusTextView.setVisibility(View.VISIBLE);
            appointmentHolder.statusTextView.setText("Подтверждён");
        }

        // Устанавливаем обработчик клика
        appointmentHolder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAppointmentClick(appointment);
            }
        });
    }

    @Override
    public int getItemCount() {
        if (isLoading && appointments.isEmpty()) {
            return 3; // Показываем 3 skeleton элемента
        }
        return appointments != null ? appointments.size() : 0;
    }

    // Методы для управления состоянием загрузки
    public void showLoading() {
        isLoading = true;
        appointments.clear();
        notifyDataSetChanged();
    }

    public void hideLoading() {
        isLoading = false;
        notifyDataSetChanged();
    }

    // Метод для обновления данных с анимацией
    public void updateAppointments(List<Appointment> newAppointments) {
        if (isLoading) {
            // Показываем skeleton минимум 2 секунды
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                isLoading = false;
                this.appointments = newAppointments;
                notifyDataSetChanged();
            }, 1000);
        } else {
            this.appointments = newAppointments;
            notifyDataSetChanged();
        }
    }

    // ViewHolder для skeleton загрузки
    static class LoadingViewHolder extends RecyclerView.ViewHolder {
        ShimmerFrameLayout shimmerLayout;

        public LoadingViewHolder(@NonNull View itemView) {
            super(itemView);
            shimmerLayout = itemView.findViewById(R.id.shimmer_layout);
        }
    }

    // ViewHolder для элемента списка
    static class AppointmentViewHolder extends RecyclerView.ViewHolder {
        TextView carNameTextView;
        TextView stationAddressTextView;
        TextView bookingDateTimeTextView;
        TextView totalPriceTextView;
        TextView statusTextView;
        LinearLayout priceLayout;
        LinearLayout quickBookingLayout;

        public AppointmentViewHolder(@NonNull View itemView) {
            super(itemView);
            carNameTextView = itemView.findViewById(R.id.tv_car_name);
            stationAddressTextView = itemView.findViewById(R.id.tv_station_address);
            bookingDateTimeTextView = itemView.findViewById(R.id.tv_booking_datetime);
            totalPriceTextView = itemView.findViewById(R.id.tv_total_price);
            statusTextView = itemView.findViewById(R.id.tv_status);
            priceLayout = itemView.findViewById(R.id.price_layout);
            quickBookingLayout = itemView.findViewById(R.id.quick_booking_layout);
        }
    }
}
