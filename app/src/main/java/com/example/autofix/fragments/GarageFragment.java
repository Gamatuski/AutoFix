package com.example.autofix.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.autofix.AppointmentDetailsActivity;
import com.example.autofix.R;

import com.example.autofix.adapters.AppointmentsAdapter;
import com.example.autofix.models.Appointment;
import com.example.autofix.sto.BookServiceActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class GarageFragment extends Fragment implements AppointmentsAdapter.OnAppointmentClickListener {

    private RecyclerView appointmentsRecyclerView;
    private AppointmentsAdapter appointmentsAdapter;
    private View emptyStateView;
    private View loadingView;
    FirebaseUser currentUser;

    public GarageFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_garage, container, false);

        // Инициализация views
        appointmentsRecyclerView = view.findViewById(R.id.recycler_appointments);
        emptyStateView = view.findViewById(R.id.empty_state_layout);
        loadingView = view.findViewById(R.id.loading_layout);
        Button btnBookService = view.findViewById(R.id.btn_book_service);

        // Настройка RecyclerView
        appointmentsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        appointmentsAdapter = new AppointmentsAdapter(getContext(), new ArrayList<>(), this);
        appointmentsRecyclerView.setAdapter(appointmentsAdapter);

        // Настройка кнопки
        btnBookService.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), BookServiceActivity.class));
        });

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            // Загружаем записи для текущего пользователя
            loadAppointmentsForUser(currentUser.getUid());
        } else {
            // Пользователь не авторизован, показываем пустое состояние
            showEmptyState();
        }
        return view;
    }

    private void loadAppointmentsForUser(String userId) {
        // Показываем индикатор загрузки
        showLoading();

        // Получаем записи для конкретного пользователя
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .collection("appointments")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Appointment> appointmentsList = new ArrayList<>();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                // Преобразуем документ в объект Appointment
                                Appointment appointment = new Appointment();
                                appointment.setId(document.getId());

                                // Получаем данные из документа
                                if (document.contains("stationId"))
                                    appointment.setStationId(document.getString("stationId"));

                                if (document.contains("stationAddress"))
                                    appointment.setStationAddress(document.getString("stationAddress"));

                                if (document.contains("bookingDate"))
                                    appointment.setBookingDate(document.getString("bookingDate"));

                                if (document.contains("bookingTime"))
                                    appointment.setBookingTime(document.getString("bookingTime"));

                                if (document.contains("carId"))
                                    appointment.setCarId(document.getString("carId"));

                                if (document.contains("carName"))
                                    appointment.setCarName(document.getString("carName"));

                                if (document.contains("totalPrice"))
                                    appointment.setTotalPrice(document.getLong("totalPrice").intValue());

                                if (document.contains("status"))
                                    appointment.setStatus(document.getString("status"));

                                if (document.contains("createdAt"))
                                    appointment.setCreatedAt(document.getDate("createdAt"));

                                appointmentsList.add(appointment);
                            } catch (Exception e) {
                                Log.e("GarageFragment", "Error parsing appointment: " + e.getMessage());
                            }
                        }

                        // Обновляем UI
                        updateUI(appointmentsList);

                    } else {
                        Log.e("GarageFragment", "Error getting appointments", task.getException());
                        // Показываем пустое состояние в случае ошибки
                        showEmptyState();
                    }
                });
    }

    private void updateUI(List<Appointment> appointments) {
        // Скрываем индикатор загрузки
        loadingView.setVisibility(View.GONE);

        if (appointments.isEmpty()) {
            // Если записей нет, показываем пустое состояние
            showEmptyState();
        } else {
            // Если записи есть, показываем список
            showAppointmentsList(appointments);
        }
    }

    private void showEmptyState() {
        emptyStateView.setVisibility(View.VISIBLE);
        appointmentsRecyclerView.setVisibility(View.GONE);
    }

    private void showAppointmentsList(List<Appointment> appointments) {
        emptyStateView.setVisibility(View.GONE);
        appointmentsRecyclerView.setVisibility(View.VISIBLE);
        appointmentsAdapter.updateAppointments(appointments);
    }

    private void showLoading() {
        loadingView.setVisibility(View.VISIBLE);
        emptyStateView.setVisibility(View.GONE);
        appointmentsRecyclerView.setVisibility(View.GONE);
    }

    @Override
    public void onAppointmentClick(Appointment appointment) {
        // Открываем активность с деталями записи
        Intent intent = new Intent(getActivity(), AppointmentDetailsActivity.class);
        intent.putExtra("APPOINTMENT_ID", appointment.getId());
        this.startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Обновляем данные при возвращении к фрагменту
        loadAppointmentsForUser(currentUser.getUid());
    }
}