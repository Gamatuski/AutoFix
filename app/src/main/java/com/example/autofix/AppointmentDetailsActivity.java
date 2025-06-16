package com.example.autofix;


import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.autofix.R;
import com.example.autofix.adapters.CartServiceAdapter;
import com.example.autofix.data.entities.Cart;
import com.example.autofix.data.entities.CartItem;
import com.example.autofix.models.Appointment;
import com.example.autofix.services.NotificationReminderService;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppointmentDetailsActivity extends AppCompatActivity implements OnMapReadyCallback {
    private TextView tvStationAddress, tvBookingDate, tvBookingTime, tvCarName, tvTotalPrice;
    private ImageView back_icon;
    private RecyclerView servicesRecyclerView;
    private CartServiceAdapter adapter;
    private List<CartItem> services = new ArrayList<>();
    private Appointment appointment;
    private MapView mapView;
    private GoogleMap googleMap;
    private LinearLayout quickBookingLayout;
    String appointmentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointment_details);

        // Инициализация views
        tvStationAddress = findViewById(R.id.tv_station_address);
        tvBookingDate = findViewById(R.id.tv_booking_date);
        tvBookingTime = findViewById(R.id.tv_booking_time);
        tvCarName = findViewById(R.id.tv_car_name);
        tvTotalPrice = findViewById(R.id.tv_total_price);
        servicesRecyclerView = findViewById(R.id.services_recycler);
        back_icon = findViewById(R.id.back_icon);
        mapView = findViewById(R.id.map_view);
        quickBookingLayout = findViewById(R.id.quick_booking_layout);

        // Инициализация карты
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        // Настройка RecyclerView
        adapter = new CartServiceAdapter(services, null);
        servicesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        servicesRecyclerView.setAdapter(adapter);

        Button cancelButton = findViewById(R.id.cancel_booking_button);
        cancelButton.setOnClickListener(v -> showCancelDialog());

        // Получаем ID записи из Intent
        appointmentId = getIntent().getStringExtra("APPOINTMENT_ID");
        if (appointmentId != null) {
            loadAppointmentDetails(appointmentId);
        }

        back_icon.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
        });
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setAllGesturesEnabled(false);
        googleMap.getUiSettings().setMapToolbarEnabled(false);
        if (appointment != null) {
            loadServiceLocation(appointment.getStationId());
        }
    }

    private void loadServiceLocation(String stationId) {
        FirebaseFirestore.getInstance()
                .collection("autoServiceCenters")
                .document(stationId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Получаем GeoPoint вместо Map
                        GeoPoint location = documentSnapshot.getGeoPoint("location");
                        if (location != null) {
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();
                            LatLng serviceLocation = new LatLng(latitude, longitude);
                            if (googleMap != null) {
                                googleMap.addMarker(new MarkerOptions()
                                        .position(serviceLocation)
                                        .title("Сервисный центр"));
                                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(serviceLocation, 15));
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("AppointmentDetailsActivity", "Ошибка загрузки местоположения", e);
                });
    }

    private void showCancelDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Отмена записи")
                .setMessage("Вы действительно хотите отменить запись?")
                .setPositiveButton("Да", (dialog, which) -> cancelAppointment())
                .setNegativeButton("Нет", null)
                .show();
    }

    private void cancelAppointment() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Пользователь не авторизован", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Удаление записи из /users/{userId}/appointments/{appointmentId}
        db.collection("users")
                .document(userId)
                .collection("appointments")
                .document(appointmentId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d("AppointmentDetailsActivity", "Запись успешно удалена");

                    NotificationReminderService.cancelNotificationForAppointment(this, appointmentId);
                    Log.d("AppointmentDetailsActivity", "Уведомление отменено для записи: " + appointmentId);
                    updateTimeSlotStatusToAvailable(
                            appointment.getStationId(),
                            appointment.getBookingDate(),
                            appointment.getBookingTime()
                    );
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e("AppointmentDetailsActivity", "Ошибка при удалении записи", e);
                    Toast.makeText(this, "Не удалось удалить запись", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateTimeSlotStatusToAvailable(String stationId, String bookingDate, String bookingTime) {
        String[] dateParts = bookingDate.split("\\.");
        if (dateParts.length != 3) {
            Log.e("BookingActivity", "Неверный формат даты: " + bookingDate);
            return;
        }

        String day = String.valueOf(Integer.parseInt(dateParts[0]));
        String month = String.valueOf(Integer.parseInt(dateParts[1]));
        String year = dateParts[2];

        Log.d("BookingActivity", "Освобождение временного слота для: " + stationId + ", дата: " + year + "-" + month + "-" + day + ", время: " + bookingTime);

        DocumentReference timeSlotRef = FirebaseFirestore.getInstance()
                .collection("autoServiceCenters")
                .document(stationId)
                .collection("appointments")
                .document(year)
                .collection("months")
                .document(month)
                .collection("days")
                .document(day);

        timeSlotRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("times." + bookingTime, true); // ← изменено на true
                    timeSlotRef.update(updates)
                            .addOnSuccessListener(aVoid -> Log.d("BookingActivity", "Временной слот успешно освобожден"))
                            .addOnFailureListener(e -> Log.e("BookingActivity", "Ошибка при освобождении слота", e));
                } else {
                    Map<String, Boolean> times = new HashMap<>();
                    times.put(bookingTime, true); // ← изменено на true
                    Map<String, Object> data = new HashMap<>();
                    data.put("times", times);
                    timeSlotRef.set(data)
                            .addOnSuccessListener(aVoid -> Log.d("BookingActivity", "Слот создан как доступный"))
                            .addOnFailureListener(e -> Log.e("BookingActivity", "Ошибка при создании слота", e));
                }
            } else {
                Log.e("BookingActivity", "Ошибка при проверке документа временного слота", task.getException());
            }
        });
    }

    private void loadAppointmentDetails(String appointmentId) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        // Путь к записи
        String path = "users/" + currentUser.getUid() + "/appointments/" + appointmentId;
        FirebaseFirestore.getInstance().document(path)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();
                        // Заполняем данные о записи
                        fillAppointmentData(document);
                        // Загружаем услуги
                        loadServices(appointmentId);
                    }
                });
    }

    private void fillAppointmentData(DocumentSnapshot document) {
        appointment = document.toObject(Appointment.class); //
        if (appointment != null) {
            tvStationAddress.setText(appointment.getStationAddress());
            tvBookingDate.setText(appointment.getBookingDate());
            tvBookingTime.setText(appointment.getBookingTime());
            tvCarName.setText(appointment.getCarName());
            tvTotalPrice.setText(String.format("%d ₽", appointment.getTotalPrice()));

            // Проверяем и отображаем индикатор быстрой записи
            boolean isQuickBooking = document.contains("isQuickBooking") &&
                    document.getBoolean("isQuickBooking");

            if (isQuickBooking) {
                quickBookingLayout.setVisibility(View.VISIBLE);
                // Скрываем блок стоимости для быстрой записи
                findViewById(R.id.total_price_layout).setVisibility(View.GONE);
            } else {
                quickBookingLayout.setVisibility(View.GONE);
                // Показываем блок стоимости для обычной записи
                findViewById(R.id.total_price_layout).setVisibility(View.VISIBLE);
            }

            // Загружаем местоположение если карта готова
            if (googleMap != null) {
                loadServiceLocation(appointment.getStationId());
            }
        }
    }

    private void loadServices(String appointmentId) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUser.getUid())
                .collection("appointments")
                .document(appointmentId)
                .collection("services")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<CartItem> loadedServices = new ArrayList<>();
                        for (DocumentSnapshot doc : task.getResult()) {
                            CartItem item = new CartItem(
                                    doc.getString("id"),
                                    doc.getString("name"),
                                    doc.getLong("price").intValue(),
                                    doc.getLong("duration").intValue(),
                                    doc.getString("imageUrl")
                            );
                            loadedServices.add(item);
                        }
                        services.clear();
                        services.addAll(loadedServices);
                        adapter.updateItems(services);
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}