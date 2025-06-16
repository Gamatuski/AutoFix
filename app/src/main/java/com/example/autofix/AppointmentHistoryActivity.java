package com.example.autofix;

import static androidx.core.content.ContextCompat.startActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.autofix.adapters.AppointmentsAdapter;
import com.example.autofix.models.Appointment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AppointmentHistoryActivity extends AppCompatActivity implements AppointmentsAdapter.OnAppointmentClickListener {

    private RecyclerView recyclerView;
    private AppointmentsAdapter appointmentsAdapter;
    private View emptyStateView;
    private View loadingView;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointment_history);

        // Настройка ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("История записей");
        }

        // Инициализация views
        recyclerView = findViewById(R.id.recycler_appointments);
        emptyStateView = findViewById(R.id.empty_state_layout);
        loadingView = findViewById(R.id.loading_layout);

        // Настройка RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        appointmentsAdapter = new AppointmentsAdapter(this, new ArrayList<>(), this);
        recyclerView.setAdapter(appointmentsAdapter);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            loadAppointmentsForUser(currentUser.getUid());
        } else {
            showEmptyState();
        }
    }

    private void loadAppointmentsForUser(String userId) {
        showLoading();
        appointmentsAdapter.showLoading();
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .collection("appointments")
                .whereEqualTo("status", "completed") // Только фильтрация, без сортировки
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Appointment> appointmentsList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                Appointment appointment = new Appointment();
                                appointment.setId(document.getId());

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
                                Log.e("AppointmentHistory", "Error parsing appointment: " + e.getMessage());
                            }
                        }

                        // Сортируем на клиенте
                        appointmentsList.sort((a1, a2) -> {
                            if (a1.getCreatedAt() == null && a2.getCreatedAt() == null) return 0;
                            if (a1.getCreatedAt() == null) return 1;
                            if (a2.getCreatedAt() == null) return -1;
                            return a2.getCreatedAt().compareTo(a1.getCreatedAt());
                        });

                        updateUI(appointmentsList);
                    } else {
                        Log.e("AppointmentHistory", "Error getting appointments", task.getException());
                        showEmptyState();
                    }
                });
    }
    private void updateUI(List<Appointment> appointments) {
        loadingView.setVisibility(View.GONE);
        if (appointments.isEmpty()) {
            showEmptyState();
        } else {
            showAppointmentsList(appointments);
        }
    }

    private void showEmptyState() {
        loadingView.setVisibility(View.GONE);
        emptyStateView.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }

    private void showAppointmentsList(List<Appointment> appointments) {
        emptyStateView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        appointmentsAdapter.updateAppointments(appointments);
    }

    private void showLoading() {
        loadingView.setVisibility(View.VISIBLE);
        emptyStateView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
    }

    @Override
    public void onAppointmentClick(Appointment appointment) {
        Intent intent = new Intent(this, AppointmentDetailsActivity.class);
        intent.putExtra("APPOINTMENT_ID", appointment.getId());
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}