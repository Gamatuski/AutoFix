package com.example.autofix.sto;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
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
import com.example.autofix.services.NotificationReminderService;
import com.example.autofix.viewmodels.CartViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
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
    private boolean isQuickBooking = false;
    private LinearLayout totalPriceLayout;
    private TextView disclaimerText;
    private String carId, carName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        // Получаем параметры из Intent
        isQuickBooking = getIntent().getBooleanExtra("IS_QUICK_BOOKING", false);
        carId = getIntent().getStringExtra("CAR_ID");
        carName = getIntent().getStringExtra("CAR_NAME");

        cartViewModel = new ViewModelProvider(this).get(CartViewModel.class);
        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupUIForBookingType();
        setupProgressBar(); // Новый метод для настройки прогресс-бара
        setupClickListeners();

        // Настройка RecyclerView для станций
        setupStationsRecyclerView();
        // Настройка RecyclerView для дат
        setupDatesRecyclerView();
        // Настройка RecyclerView для временных слотов
        setupTimeSlotsRecyclerView();

        // Загрузка данных
        loadStationsFromFirestore();
        generateAvailableDates();
        setupObservers();
    }

    private void setupProgressBar() {
        // Находим элементы прогресс-бара
        View progressFill = findViewById(R.id.progress_fill);
        View firstDot = findViewById(R.id.first_dot);
        View secondDot = findViewById(R.id.second_dot);
        RelativeLayout thirdDotContainer = findViewById(R.id.third_dot_container);
        TextView stepCount = findViewById(R.id.step_conut);

        if (isQuickBooking) {
            stepCount.setText("Шаг 2/2");
            thirdDotContainer.setVisibility(View.GONE);

            // Для быстрой записи заполняем прогресс до конца (вторая точка активна)
            secondDot.post(() -> {
                ViewGroup.LayoutParams params = progressFill.getLayoutParams();
                params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                progressFill.setLayoutParams(params);
            });

            // Делаем вторую точку активной
            secondDot.setBackground(ContextCompat.getDrawable(this, R.drawable.progress_dot_active));
        } else {
            stepCount.setText("Шаг 3/3");
            thirdDotContainer.setVisibility(View.VISIBLE);

            // Для обычной записи заполняем прогресс до второй точки
            secondDot.post(() -> {
                ViewGroup.LayoutParams params = progressFill.getLayoutParams();
                params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                progressFill.setLayoutParams(params);
            });

            // Делаем вторую точку активной
            secondDot.setBackground(ContextCompat.getDrawable(this, R.drawable.progress_dot_active));
        }
    }

    private void initializeViews() {
        stationsRecyclerView = findViewById(R.id.stations_recycler);
        datesRecyclerView = findViewById(R.id.dates_recycler);
        timeSlotsRecyclerView = findViewById(R.id.time_slots_recycler);
        proceedButton = findViewById(R.id.proceed_to_booking_button);
        totalPriceText = findViewById(R.id.total_price_text);
        commentText = findViewById(R.id.comment_text);
        cancelButton = findViewById(R.id.cancel_button_top);
        backButton = findViewById(R.id.back_button_top);

        // Инициализация элементов CardView
        totalPriceLayout = findViewById(R.id.total_price_layout);
        disclaimerText = findViewById(R.id.disclaimer_text);
    }

    private void setupClickListeners() {
        // Обработка кнопки подтверждения записи
        proceedButton.setOnClickListener(v -> {
            if (selectedStation != null && selectedDate != null && selectedTime != null) {
                if (isQuickBooking) {
                    // Для быстрого бронирования создаем минимальную корзину
                    createQuickBookingCart();
                } else {
                    // Обычное бронирование
                    bookSelectedTimeSlot();
                }
            } else {
                Toast.makeText(this, "Пожалуйста, выберите станцию, дату и время", Toast.LENGTH_SHORT).show();
            }
        });

        cancelButton.setOnClickListener(l -> showCancelConfirmationDialog());
        backButton.setOnClickListener(v -> onBackPressed());
    }

    private void createQuickBookingCart() {
        Log.d(TAG, "createQuickBookingCart: создание быстрой корзины");

        // Создаем новую корзину для быстрого бронирования
        Cart quickCart = new Cart();
        quickCart.setSelectedCarName(carName);
        quickCart.setSelectedCarId(carId);
        quickCart.setBookingDate(selectedDate);
        quickCart.setBookingTime(selectedTime);
        quickCart.setStationId(selectedStation.getId());
        quickCart.setStationAddress(selectedStation.getAddress());
        quickCart.setTotalPrice(0); // Цена будет определена на СТО
        quickCart.setTotalDuration(0);

        // Добавляем комментарий, если он есть
        String comment = commentText.getText().toString().trim();
        if (!comment.isEmpty()) {
            quickCart.setComment(comment);
        }
        else {
            Toast.makeText(this, "Опишите пробелму",Toast.LENGTH_SHORT).show();
            return;
        }

        // Сохраняем корзину в ViewModel
        cartViewModel.updateCart(quickCart);

        Log.d(TAG, "createQuickBookingCart: корзина создана и сохранена");

        // Бронируем временной слот
        bookSelectedTimeSlot();
    }

    private void setupUIForBookingType() {
        if (isQuickBooking) {
            // Скрываем поля итого и дисклеймер при быстром бронировании
            if (totalPriceLayout != null) {
                totalPriceLayout.setVisibility(View.GONE);
            }
            if (disclaimerText != null) {
                disclaimerText.setVisibility(View.GONE);
            }

            // Изменяем текст кнопки для быстрого бронирования
            proceedButton.setText("Быстрая запись");
            commentText.setHint("Опишите проблему");
        } else {
            // Показываем все поля для обычного бронирования
            if (totalPriceLayout != null) {
                totalPriceLayout.setVisibility(View.VISIBLE);
            }
            if (disclaimerText != null) {
                disclaimerText.setVisibility(View.VISIBLE);
            }

            proceedButton.setText("Записаться на СТО");
        }
    }
    private void setupObservers() {
        // Наблюдаем за изменениями в корзине только если это не быстрое бронирование
        if (!isQuickBooking) {
            cartViewModel.getTotalPrice().observe(this, total -> {
                if (total != null) {
                    totalPriceText.setText(String.format("%d ₽", total));
                }
            });
        }
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
        if (selectedStation == null || date == null) {
            Log.w(TAG, "loadTimeSlotsForDate: selectedStation or date is null");
            return;
        }

        // Преобразуем дату из "dd.MM.yyyy" в компоненты для Firebase
        String[] dateParts = date.split("\\.");
        if (dateParts.length != 3) {
            Toast.makeText(this, "Неверный формат даты", Toast.LENGTH_SHORT).show();
            return;
        }

        String day = String.valueOf(Integer.parseInt(dateParts[0]));
        String month = String.valueOf(Integer.parseInt(dateParts[1]));
        String year = dateParts[2];
        String formattedDate = year + "-" + String.format("%02d", Integer.parseInt(month)) + "-" + String.format("%02d", Integer.parseInt(day));

        Log.d(TAG, "Loading time slots for date: " + formattedDate + ", station: " + selectedStation.getAddress());

        // Очищаем старые слоты
        timeSlotAdapter.clearTimeSlots();

        // Путь к документу с временными слотами в Firebase
        DocumentReference timeSlotRef = db.collection("autoServiceCenters")
                .document(selectedStation.getId())
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
                    // Получаем Map с временными слотами из Firebase
                    Map<String, Object> timesData = (Map<String, Object>) document.get("times");
                    if (timesData != null && !timesData.isEmpty()) {
                        // Преобразуем Object в Boolean
                        Map<String, Boolean> timesMap = new HashMap<>();
                        for (Map.Entry<String, Object> entry : timesData.entrySet()) {
                            Boolean value = (Boolean) entry.getValue();
                            timesMap.put(entry.getKey(), value != null ? value : false);
                            Log.d(TAG, "Time slot: " + entry.getKey() + " = " + value);
                        }

                        // Передаем данные в адаптер
                        timeSlotAdapter.updateTimeSlots(timesMap, formattedDate);
                        Log.d(TAG, "Time slots loaded: " + timesMap.size());
                    } else {
                        Log.d(TAG, "No times data found, creating default slots");
                        createDefaultTimeSlots(timeSlotRef, formattedDate);
                    }
                } else {
                    Log.d(TAG, "Document doesn't exist, creating default slots");
                    createDefaultTimeSlots(timeSlotRef, formattedDate);
                }
            } else {
                Log.e(TAG, "Error loading time slots", task.getException());
                Toast.makeText(this, "Ошибка загрузки временных слотов", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Метод для создания слотов по умолчанию, если их нет в Firebase
    private void createDefaultTimeSlots(DocumentReference timeSlotRef, String formattedDate) {
        // Создаем стандартные временные слоты
        Map<String, Boolean> defaultSlots = new HashMap<>();
        String[] defaultTimes = {"09:00", "10:00", "11:00", "12:00", "13:00", "14:00",
                "15:00", "16:00", "17:00", "18:00", "19:00", "20:00", "21:00"};

        for (String time : defaultTimes) {
            defaultSlots.put(time, true); // По умолчанию все слоты доступны
        }

        // Сохраняем в Firebase
        Map<String, Object> data = new HashMap<>();
        data.put("times", defaultSlots);

        timeSlotRef.set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Default time slots created");
                    timeSlotAdapter.updateTimeSlots(defaultSlots, formattedDate);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating default time slots", e);
                    // Все равно показываем слоты пользователю
                    timeSlotAdapter.updateTimeSlots(defaultSlots, formattedDate);
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
            // Если корзина пуста, создаем новую (особенно важно для быстрого бронирования)
            if (currentCart == null) {
                Log.d(TAG, "saveBookingInfo: корзина пуста, создаем новую");
                currentCart = new Cart();
            }

            // Validate required fields
            if (selectedDate == null || selectedTime == null || selectedStation == null) {
                Toast.makeText(this, "Не все обязательные поля заполнены", Toast.LENGTH_SHORT).show();
                return;
            }

            // Получаем carId и carName из Intent или из корзины
            String finalCarId = carId;
            String finalCarName = carName;

            // Если данные не пришли через Intent, пытаемся получить из корзины
            if (finalCarId == null && currentCart.getSelectedCarId() != null) {
                finalCarId = currentCart.getSelectedCarId();
            }
            if (finalCarName == null && currentCart.getSelectedCarName() != null) {
                finalCarName = currentCart.getSelectedCarName();
            }

            // Логируем для отладки
            Log.d(TAG, "saveBookingInfo: carId=" + finalCarId + ", carName=" + finalCarName);

            currentCart.setSelectedCarId(finalCarId);
            currentCart.setSelectedCarName(finalCarName);
            currentCart.setBookingDate(selectedDate);
            currentCart.setBookingTime(selectedTime);
            currentCart.setStationId(selectedStation.getId());
            currentCart.setStationAddress(selectedStation.getAddress());

            // Добавляем комментарий, если он есть
            String comment = commentText.getText().toString().trim();
            if (!comment.isEmpty()) {
                currentCart.setComment(comment);
            }

            // Для быстрого бронирования устанавливаем значения по умолчанию
            if (isQuickBooking) {
                currentCart.setTotalPrice(0);
                currentCart.setTotalDuration(0);
            }

            // Обновляем корзину в ViewModel перед сохранением
            cartViewModel.updateCart(currentCart);
            // Планируем уведомление для новой записи

            // Сохраняем данные бронирования в Firestore
            saveBookingToFirestore(currentCart);
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
        bookingData.put("isQuickBooking", isQuickBooking);

        // Добавляем информацию об автомобиле только если она есть
        if (cart.getSelectedCarId() != null && !cart.getSelectedCarId().isEmpty()) {
            bookingData.put("carId", cart.getSelectedCarId());
            Log.d(TAG, "saveBookingToFirestore: добавлен carId=" + cart.getSelectedCarId());
        }
        if (cart.getSelectedCarName() != null && !cart.getSelectedCarName().isEmpty()) {
            bookingData.put("carName", cart.getSelectedCarName());
            Log.d(TAG, "saveBookingToFirestore: добавлен carName=" + cart.getSelectedCarName());
        }

        // Добавляем комментарий, если он есть
        if (cart.getComment() != null && !cart.getComment().isEmpty()) {
            bookingData.put("comment", cart.getComment());
        }

        // ВАЖНО: Используем цену напрямую из корзины без дополнительных пересчетов
        if (isQuickBooking) {
            bookingData.put("totalPrice", 0);
            bookingData.put("totalDuration", 0);
        } else {
            // Берем финальную цену из корзины (уже с учетом скидки)
            bookingData.put("totalPrice", cart.getTotalPrice());
            bookingData.put("totalDuration", cart.getTotalDuration());

            // Добавляем информацию о скидке, если она есть
            if (cart.hasActiveDiscount()) {
                bookingData.put("discountPercent", cart.getDiscount());
                bookingData.put("originalPrice", cart.getOriginalPriceForDisplay());
                bookingData.put("discountAmount", cart.getDiscountAmount());
            }
        }

        bookingData.put("createdAt", new Date());
        bookingData.put("status", "confirmed");

        // Логируем финальные данные
        Log.d(TAG, "saveBookingToFirestore: Final totalPrice=" + bookingData.get("totalPrice") +
                ", discount=" + cart.getDiscount() + "%, originalPrice=" + cart.getOriginalPriceForDisplay());

        // Остальной код сохранения остается без изменений...
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .document(userId)
                .collection("appointments")
                .add(bookingData)
                .addOnSuccessListener(documentReference -> {
                    String appointmentId = documentReference.getId();
                    Log.d("BookingActivity", "Booking saved with ID: " + appointmentId);

                    if (!isQuickBooking) {
                        observeOnce(cartViewModel.getAllCartItems(), this, items -> {
                            addServicesToAppointment(userId, appointmentId, items);
                        });
                    }

                    String bookingDateTime = selectedDate + " " + selectedTime;
                    scheduleNotificationForNewBooking(
                            appointmentId,
                            bookingDateTime,
                            selectedStation.getAddress()
                    );

                    updateTimeSlotStatus(cart.getStationId(), cart.getBookingDate(), cart.getBookingTime());

                    Intent confirmIntent = new Intent(this, ConfirmBookingActivity.class);
                    confirmIntent.putExtra("BOOKING_DATE", cart.getBookingDate());
                    confirmIntent.putExtra("BOOKING_TIME", cart.getBookingTime());
                    confirmIntent.putExtra("STATION_ADDRESS", cart.getStationAddress());
                    confirmIntent.putExtra("CAR_NAME", cart.getSelectedCarName());
                    startActivity(confirmIntent);
                    finish();
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
        if (stationId == null || bookingDate == null || bookingTime == null) {
            Log.e(TAG, "Invalid parameters for updateTimeSlotStatus");
            return;
        }

        // Преобразуем дату из формата "dd.MM.yyyy"
        String[] dateParts = bookingDate.split("\\.");
        if (dateParts.length != 3) {
            Log.e(TAG, "Неверный формат даты: " + bookingDate);
            return;
        }

        String day = String.valueOf(Integer.parseInt(dateParts[0]));
        String month = String.valueOf(Integer.parseInt(dateParts[1]));
        String year = dateParts[2];

        Log.d(TAG, "Updating time slot status:");
        Log.d(TAG, "Station ID: " + stationId);
        Log.d(TAG, "Date path: " + year + "/" + month + "/" + day);
        Log.d(TAG, "Time slot: " + bookingTime);

        DocumentReference timeSlotRef = db.collection("autoServiceCenters")
                .document(stationId)
                .collection("appointments")
                .document(year)
                .collection("months")
                .document(month)
                .collection("days")
                .document(day);

        // Обновляем статус временного слота на false (занято)
        Map<String, Object> updates = new HashMap<>();
        updates.put("times." + bookingTime, false);

        Log.d(TAG, "Updating field: times." + bookingTime + " = false");

        timeSlotRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Time slot status successfully updated to false");

                    // Обновляем адаптер локально, чтобы сразу показать изменения
                    runOnUiThread(() -> {
                        timeSlotAdapter.markSlotAsBooked(bookingTime);
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating time slot status", e);

                    // Если документ не существует, создаем его
                    Map<String, Boolean> times = new HashMap<>();
                    times.put(bookingTime, false);

                    Map<String, Object> data = new HashMap<>();
                    data.put("times", times);

                    timeSlotRef.set(data, SetOptions.merge())
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Time slot document created and status set to false");
                                runOnUiThread(() -> {
                                    timeSlotAdapter.markSlotAsBooked(bookingTime);
                                });
                            })
                            .addOnFailureListener(createError -> {
                                Log.e(TAG, "Error creating time slot document", createError);
                            });
                });
    }

    // Вспомогательный метод для создания документа временного слота
    private void createTimeSlotDocument(DocumentReference timeSlotRef, String bookingTime) {
        Map<String, Boolean> times = new HashMap<>();
        times.put(bookingTime, false); // Устанавливаем забронированное время как false

        Map<String, Object> data = new HashMap<>();
        data.put("times", times);

        timeSlotRef.set(data, SetOptions.merge()) // Используем merge для безопасного создания
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Документ временного слота успешно создан с times." + bookingTime + " = false");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Ошибка при создании документа временного слота", e);
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

    private void scheduleNotificationForNewBooking(String appointmentId, String bookingDateTime,
                                                   String stationAddress) {

        NotificationReminderService.addNotificationForBooking(
                this,
                appointmentId,
                bookingDateTime,
                stationAddress
        );

        Log.d("BookingActivity", "Added notification for new booking: " + appointmentId);
    }
}