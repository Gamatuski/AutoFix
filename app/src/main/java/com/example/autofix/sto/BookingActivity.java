package com.example.autofix.sto;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.autofix.MainActivity;
import com.example.autofix.R;
import com.example.autofix.adapters.DateAdapter;
import com.example.autofix.adapters.StationForBookingAdapter;
import com.example.autofix.adapters.TimeSlotAdapter;
import com.example.autofix.data.entities.Cart;
import com.example.autofix.data.entities.CartItem;
import com.example.autofix.data.entities.Station;
import com.example.autofix.fragments.HomeFragment;
import com.example.autofix.viewmodels.CartViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class BookingActivity extends AppCompatActivity
        implements DateAdapter.OnDateSelectedListener,
        TimeSlotAdapter.OnTimeSlotSelectedListener,
        StationForBookingAdapter.OnStationSelectedListener {

    private static final String TAG = "BookingActivity";

    private CartViewModel cartViewModel;
    private RecyclerView stationsRecyclerView;
    private RecyclerView datesRecyclerView;
    private RecyclerView timeSlotsRecyclerView;
    private Button proceedButton;
    private TextView totalPriceText;
    private TextView commentText;
    private ImageView cancelButton, backButton;

    private StationForBookingAdapter stationAdapter;
    private DateAdapter dateAdapter;
    private TimeSlotAdapter timeSlotAdapter;

    private List<Station> stations = new ArrayList<>();
    private List<String> availableDates = new ArrayList<>();

    private Station selectedStation;
    private String selectedDate;
    private String selectedTime;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        cartViewModel = new ViewModelProvider(this).get(CartViewModel.class);
        db = FirebaseFirestore.getInstance();

        // Инициализация views
        stationsRecyclerView = findViewById(R.id.stations_recycler);
        datesRecyclerView = findViewById(R.id.dates_recycler);
        timeSlotsRecyclerView = findViewById(R.id.time_slots_recycler);
        proceedButton = findViewById(R.id.proceed_to_booking_button);
        totalPriceText = findViewById(R.id.total_price_text);
        commentText = findViewById(R.id.comment_text);

        // Настройка RecyclerView для станций
        setupStationsRecyclerView();

        // Настройка RecyclerView для дат
        setupDatesRecyclerView();

        // Настройка RecyclerView для временных слотов
        setupTimeSlotsRecyclerView();

        // Загрузка данных
        loadStationsFromFirestore();
        generateAvailableDates();


        // Наблюдаем за изменениями в корзине
        cartViewModel.getTotalPrice().observe(this, total -> {
            if (total != null) {
                totalPriceText.setText(String.format("%d ₽", total));
            }
        });

        // Обработка кнопки подтверждения записи
        proceedButton.setOnClickListener(v -> {
            if (selectedStation != null && selectedDate != null && selectedTime != null) {
                // Бронируем выбранный временной слот
                bookSelectedTimeSlot();
            } else {
                Toast.makeText(this, "Пожалуйста, выберите станцию, дату и время", Toast.LENGTH_SHORT).show();
            }
        });

        cancelButton = findViewById(R.id.cancel_button_top);
        cancelButton.setOnClickListener(l -> showCancelConfirmationDialog());

        backButton = findViewById(R.id.back_button_top);
        backButton.setOnClickListener(v -> onBackPressed());
    }

    private void setupStationsRecyclerView() {
        stationsRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        stationAdapter = new StationForBookingAdapter(stations, this);
        stationsRecyclerView.setAdapter(stationAdapter);
    }

    private void setupDatesRecyclerView() {
        datesRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        dateAdapter = new DateAdapter(this);
        datesRecyclerView.setAdapter(dateAdapter);
    }

    private void setupTimeSlotsRecyclerView() {
        timeSlotsRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        timeSlotAdapter = new TimeSlotAdapter(this);
        timeSlotsRecyclerView.setAdapter(timeSlotAdapter);
    }

    // В BookingActivity.java
    private void loadStationsFromFirestore() {
        db.collection("autoServiceCenters")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Station> loadedStations = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Station station = document.toObject(Station.class);
                            station.setId(document.getId());
                            loadedStations.add(station);
                        }
                        stations.clear();
                        stations.addAll(loadedStations);
                        stationAdapter.updateStations(stations);

                        // Проверяем, есть ли уже выбранная станция в корзине
                        observeOnce(cartViewModel.getCart(), this, cart -> {
                            if (cart != null && cart.getStationId() != null) {
                                // Находим станцию по ID и выбираем её
                                for (Station station : stations) {
                                    if (cart.getStationId().equals(station.getId())) {
                                        selectedStation = station;
                                        stationAdapter.setSelectedStationId(station.getId());
                                        onStationSelected(station);
                                        break;
                                    }
                                }
                            }
                        });
                    } else {
                        Toast.makeText(this, "Ошибка загрузки станций", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error loading stations", task.getException());
                    }
                });
    }

    private void generateAvailableDates() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        availableDates.clear();

        // Генерируем даты на 7 дней вперед
        for (int i = 0; i < 7; i++) {
            availableDates.add(sdf.format(calendar.getTime()));
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        dateAdapter.updateDates(availableDates);
    }

    @Override
    public void onStationSelected(Station station) {
        selectedStation = station;

        // Обновляем корзину
        Cart currentCart = cartViewModel.getCart().getValue();
        if (currentCart != null) {
            currentCart.setStationAddress(station.getAddress());
            currentCart.setStationId(station.getId());
            cartViewModel.updateCart(currentCart);
        }

        // Если дата уже выбрана, загружаем временные слоты для новой станции
        if (selectedDate != null) {
            loadTimeSlotsForDate(selectedDate);
        }
    }

    @Override
    public void onDateSelected(String date) {
        selectedDate = date;
        selectedTime = null; // Сбрасываем выбранное время при смене даты

        if (selectedStation != null) {
            loadTimeSlotsForDate(date);
        } else {
            Toast.makeText(this, "Сначала выберите станцию", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onTimeSlotSelected(String timeSlot) {
        selectedTime = timeSlot;
        Toast.makeText(this, "Выбрано время: " + timeSlot, Toast.LENGTH_SHORT).show();
    }

    private void loadTimeSlotsForDate(String date) {
        if (selectedStation == null || date == null) return;

        // Преобразуем дату из "dd.MM.yyyy" (например, "11.05.2025") в "yyyy-MM-dd"
        String[] dateParts = date.split("\\.");
        if (dateParts.length != 3) {
            Toast.makeText(this, "Неверный формат даты", Toast.LENGTH_SHORT).show();
            return;
        }

        String formattedDate = dateParts[2] + "-" + dateParts[1] + "-" + dateParts[0];
        Log.d(TAG, "Loading time slots for date: " + formattedDate);

        selectedStation.getAvailableTimeSlots(formattedDate, availableSlots -> {
            runOnUiThread(() -> {
                if (availableSlots.isEmpty()) {
                    Toast.makeText(this, "Нет доступных слотов на выбранную дату", Toast.LENGTH_SHORT).show();
                }
                timeSlotAdapter.updateTimeSlots(availableSlots);
                Log.d(TAG, "Loaded time slots: " + availableSlots.size());
            });
        });
    }

    private void bookSelectedTimeSlot() {
        Log.d(TAG, "bookSelectedTimeSlot: начало");
        if (selectedStation == null || selectedDate == null || selectedTime == null) {
            Toast.makeText(this, "Пожалуйста, выберите станцию, дату и время", Toast.LENGTH_SHORT).show();
            return;
        }



        // Преобразуем формат даты из "dd.MM.yyyy" в "yyyy-MM-dd" для Station.bookTimeSlot
        String[] dateParts = selectedDate.split("\\.");
        if (dateParts.length != 3) {
            Toast.makeText(this, "Неверный формат даты", Toast.LENGTH_SHORT).show();
            return;
        }

        // Удаляем ведущие нули из дня и месяца
        String formattedDate = dateParts[2] + "-" +
                Integer.parseInt(dateParts[1]) + "-" +
                Integer.parseInt(dateParts[0]);

        selectedStation.bookTimeSlot(formattedDate, selectedTime, success -> {
            runOnUiThread(() -> {
                if (success) {
                    Log.d(TAG, "bookSelectedTimeSlot: успешное бронирование");
                    saveBookingInfo();
                    Toast.makeText(this, "Запись успешно создана!", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e(TAG, "bookSelectedTimeSlot: бронирование провалено");
                    Toast.makeText(this, "Не удалось забронировать время", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void saveBookingInfo() {
        Log.d(TAG, "saveBookingInfo: вызван");
        observeOnce(cartViewModel.getCart(), this, currentCart -> {

            if (currentCart != null) {
                currentCart.setBookingDate(selectedDate);
                currentCart.setBookingTime(selectedTime);
                currentCart.setStationId(selectedStation.getId());
                currentCart.setStationAddress(selectedStation.getAddress());

                // Добавляем комментарий, если он есть
                String comment = commentText.getText().toString().trim();
                if (!comment.isEmpty()) {
                    currentCart.setComment(comment);
                }


                // Сохраняем данные бронирования в Firestore
                saveBookingToFirestore(currentCart);
            } else {
                Log.e(TAG, "saveBookingInfo: текущая корзина пуста!");
                Toast.makeText(this, "Корзина пуста", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void saveBookingToFirestore(Cart cart) {
        Log.d(TAG, "saveBookingToFirestore: сохраняем запись");
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Пользователь не авторизован", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = currentUser.getUid();

        // Создаем объект с данными бронирования
        Map<String, Object> bookingData = new HashMap<>();
        bookingData.put("bookingDate", cart.getBookingDate());
        bookingData.put("bookingTime", cart.getBookingTime());
        bookingData.put("stationId", cart.getStationId());
        bookingData.put("stationAddress", cart.getStationAddress());

        // Добавляем комментарий, если он есть
        if (cart.getComment() != null && !cart.getComment().isEmpty()) {
            bookingData.put("comment", cart.getComment());
        }

        // Добавляем информацию об автомобиле
        if (cart.getSelectedCarId() != null && !cart.getSelectedCarId().isEmpty()) {
            bookingData.put("carId", cart.getSelectedCarId());
            bookingData.put("carName", cart.getSelectedCarName());
        }

        // Добавляем общую стоимость и продолжительность
        bookingData.put("totalPrice", cart.getTotalPrice());
        bookingData.put("totalDuration", cart.getTotalDuration());

        // Добавляем timestamp создания записи
        bookingData.put("createdAt", new Date());
        bookingData.put("status", "pending"); // Начальный статус заказа

        // Сохраняем в Firestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Создаем новую запись в коллекции appointments
        db.collection("users")
                .document(userId)
                .collection("appointments")
                .add(bookingData)
                .addOnSuccessListener(documentReference -> {
                    String appointmentId = documentReference.getId();
                    Log.d("BookingActivity", "Booking saved with ID: " + appointmentId);

                    // Обновляем orderId и orderStatus в корзине
                    Cart currentCart = cartViewModel.getCart().getValue();
                    if (currentCart != null) {
                        currentCart.setOrderId(appointmentId);
                        currentCart.setOrderStatus("pending");
                        cartViewModel.updateCart(currentCart);
                    }

                    // Добавляем услуги в подколлекцию
                    observeOnce(cartViewModel.getAllCartItems(), this, items -> {
                        addServicesToAppointment(userId, appointmentId, items);
                    });

                    // Обновляем статус временного слота
                    updateTimeSlotStatus(currentCart.getStationId(), currentCart.getBookingDate(), currentCart.getBookingTime());

                    // Показываем Toast и переходим на MainActivity
                    Toast.makeText(this, "Запись успешно сохранена", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, MainActivity.class));
                    finish(); // Закрываем текущую активити, чтобы нельзя было вернуться назад
                    cartViewModel.deleteCart();
                })
                .addOnFailureListener(e -> {
                    Log.e("BookingActivity", "Error saving booking", e);
                    Toast.makeText(this, "Ошибка при сохранении записи", Toast.LENGTH_SHORT).show();
                });
    }


    // Метод для добавления услуг в подколлекцию services
    private void addServicesToAppointment(String userId, String appointmentId, List<CartItem> cartItems) {
        if (cartItems == null || cartItems.isEmpty()) {
            Log.d("BookingActivity", "No cart items to add to services collection");
            return;
        }

        Log.d("BookingActivity", "Adding " + cartItems.size() + " services to appointment: " + appointmentId);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        WriteBatch batch = db.batch();

        for (int i = 0; i < cartItems.size(); i++) {
            CartItem item = cartItems.get(i);

            DocumentReference serviceRef = db.collection("users")
                    .document(userId)
                    .collection("appointments")
                    .document(appointmentId)
                    .collection("services")
                    .document("service_" + (i + 1));

            Map<String, Object> serviceData = new HashMap<>();
            serviceData.put("id", item.getServiceId());
            serviceData.put("name", item.getServiceName());
            serviceData.put("price", item.getServicePrice());
            serviceData.put("duration", item.getServiceDuration());
            if (item.getServiceImageUrl() != null) {
                serviceData.put("imageUrl", item.getServiceImageUrl());
            }

            batch.set(serviceRef, serviceData);
            Log.d("BookingActivity", "Added service to batch: " + item.getServiceName());
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.d("BookingActivity", "Services successfully added to appointment: " + appointmentId);
                })
                .addOnFailureListener(e -> {
                    Log.e("BookingActivity", "Error adding services to appointment", e);
                });
    }

    // Метод для обновления статуса временного слота
    private void updateTimeSlotStatus(String stationId, String bookingDate, String bookingTime) {
        // Преобразуем дату из формата "dd.MM.yyyy" в отдельные компоненты для пути в Firestore
        String[] dateParts = bookingDate.split("\\.");
        if (dateParts.length != 3) {
            Log.e("BookingActivity", "Неверный формат даты: " + bookingDate);
            return;
        }

        // Удаляем ведущие нули из дня и месяца
        String day = String.valueOf(Integer.parseInt(dateParts[0]));
        String month = String.valueOf(Integer.parseInt(dateParts[1]));
        String year = dateParts[2];

        Log.d("BookingActivity", "Обновление статуса временного слота для: " + stationId + ", дата: " + year + "-" + month + "-" + day + ", время: " + bookingTime);

        // Путь к документу с временными слотами
        DocumentReference timeSlotRef = FirebaseFirestore.getInstance()
                .collection("autoServiceCenters")
                .document(stationId)
                .collection("appointments")
                .document(year)
                .collection("months")
                .document(month)
                .collection("days")
                .document(day);

        // Сначала проверяем, существует ли документ
        timeSlotRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();

                if (document.exists()) {
                    // Документ существует, обновляем только конкретное время в Map times
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("times." + bookingTime, false);

                    timeSlotRef.update(updates)
                            .addOnSuccessListener(aVoid -> {
                                Log.d("BookingActivity", "Статус временного слота успешно обновлен");
                            })
                            .addOnFailureListener(e -> {
                                Log.e("BookingActivity", "Ошибка при обновлении статуса временного слота", e);
                            });
                } else {
                    // Документ не существует, создаем его с правильной структурой
                    Map<String, Boolean> times = new HashMap<>();
                    times.put(bookingTime, false);

                    Map<String, Object> data = new HashMap<>();
                    data.put("times", times);

                    timeSlotRef.set(data)
                            .addOnSuccessListener(aVoid -> {
                                Log.d("BookingActivity", "Документ временного слота успешно создан");
                            })
                            .addOnFailureListener(e -> {
                                Log.e("BookingActivity", "Ошибка при создании документа временного слота", e);
                            });
                }
            } else {
                Log.e("BookingActivity", "Ошибка при проверке документа временного слота", task.getException());
            }
        });
    }

    public static <T> void observeOnce(@NonNull LiveData<T> liveData, @NonNull LifecycleOwner owner, @NonNull Observer<T> observer) {
        liveData.observe(owner, new Observer<T>() {
            @Override
            public void onChanged(T t) {
                observer.onChanged(t);
                liveData.removeObserver(this);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Очистка ресурсов, если необходимо
    }

    private void showCancelConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Отмена записи")
                .setMessage("Вы действительно хотите отменить запись? Все данные будут потеряны.")
                .setPositiveButton("Да", (dialog, which) -> {
                    CartViewModel cartViewModel = new ViewModelProvider(this).get(CartViewModel.class);
                    cartViewModel.deleteCart();

                    // Создаем интент с флагами для очистки стека активностей
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();

                    // Анимация закрытия
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                })
                .setNegativeButton("Нет", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

    }
}