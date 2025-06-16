package com.example.autofix.sto;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.autofix.MainActivity;
import com.example.autofix.R;
import com.example.autofix.adapters.CarItemAdapter;
import com.example.autofix.adapters.CarSelectionAdapter;
import com.example.autofix.data.entities.Cart;
import com.example.autofix.models.Car;
import com.example.autofix.viewmodels.CartViewModel;
import com.example.autofix.viewmodels.CartViewModelFactory;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class BookServiceActivity extends AppCompatActivity implements CarSelectionAdapter.OnCarSelectedListener {
    private RecyclerView carsRecyclerView;
    private Button continueButton;
    private CarSelectionAdapter carAdapter;
    private List<Car> allCars = new ArrayList<>();
    private Car selectedCar = null;
    private CartViewModel cartViewModel;
    private ImageView cancelButton, backButton;
    private TextView stepCount;
    private boolean isQuickBooking = false;

    // UI элементы для разных состояний
    private LinearLayout loadingContainer;
    private LinearLayout emptyStateContainer;
    private LinearLayout errorStateContainer;
    private Button retryButton;
    private TextView errorText;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_service);

        // Получаем флаг из Intent
        isQuickBooking = getIntent().getBooleanExtra("IS_QUICK_BOOKING", false);

        // Находим элементы
        View progressFill = findViewById(R.id.progress_fill);
        View firstDot = findViewById(R.id.first_dot);
        View secondDot = findViewById(R.id.second_dot);
        RelativeLayout thirdDotContainer = findViewById(R.id.third_dot_container);
        stepCount = findViewById(R.id.step_conut);

        // Устанавливаем текст шага и скрываем третью точку в зависимости от типа записи
        if (isQuickBooking) {
            stepCount.setText("Шаг 1/2");
            thirdDotContainer.setVisibility(View.GONE);

            // Устанавливаем ширину прогресс-бара до второй точки для быстрой записи
            secondDot.post(() -> {
                int dotPosition = (int) secondDot.getX() + secondDot.getWidth() / 2;
                ViewGroup.LayoutParams params = progressFill.getLayoutParams();
                params.width = dotPosition;
                progressFill.setLayoutParams(params);
            });
        } else {
            stepCount.setText("Шаг 1/3");
            thirdDotContainer.setVisibility(View.VISIBLE);

            // Устанавливаем ширину прогресс-бара до первой точки для обычной записи
            firstDot.post(() -> {
                int dotPosition = (int) firstDot.getX() + firstDot.getWidth() / 2;
                ViewGroup.LayoutParams params = progressFill.getLayoutParams();
                params.width = dotPosition;
                progressFill.setLayoutParams(params);
            });
        }

        // Инициализация ViewModel с фабрикой
        CartViewModelFactory factory = new CartViewModelFactory(getApplication());
        cartViewModel = new ViewModelProvider(this, factory).get(CartViewModel.class);

        initViews();
        setupCarAdapter();
        loadCarDataFromFirebase();
    }

    private void initViews() {
        carsRecyclerView = findViewById(R.id.cars_recycler_view);
        continueButton = findViewById(R.id.continue_button);
        // Контейнеры для разных состояний
        loadingContainer = findViewById(R.id.loading_container);
        emptyStateContainer = findViewById(R.id.empty_state_container);
        errorStateContainer = findViewById(R.id.error_state_container);
        retryButton = findViewById(R.id.retry_button);
        errorText = findViewById(R.id.error_text);

        // Изначально кнопка неактивна
        continueButton.setEnabled(false);
        continueButton.setAlpha(0.5f);
        continueButton.setOnClickListener(v -> {
            if (selectedCar == null) {
                Toast.makeText(this, "Выберите автомобиль", Toast.LENGTH_SHORT).show();
                return;
            }
            String carId = selectedCar.getId() != null ? selectedCar.getId() :
                    (selectedCar.getMake() + "_" + selectedCar.getModel());
            String carName = selectedCar.getFullName();

            if (isQuickBooking) {
                Intent intent = new Intent(this, BookingActivity.class);
                intent.putExtra("CAR_ID", carId);
                intent.putExtra("CAR_NAME", carName);
                intent.putExtra("IS_QUICK_BOOKING", true);
                startActivity(intent);
            } else {
                // Получаем текущую корзину или создаем новую
                Cart existingCart = cartViewModel.getCart().getValue();

                if (existingCart != null) {
                    // Обновляем существующую корзину
                    existingCart.setSelectedCarId(carId);
                    existingCart.setSelectedCarName(carName);
                    cartViewModel.updateCart(existingCart);
                    Log.d("BookServiceActivity", "Updated existing cart with discount: " + existingCart.getDiscount() + "%");
                } else {
                    // Создаем новую корзину - observer автоматически применит сохраненную скидку
                    Cart newCart = new Cart(carId, carName, 0, 0);
                    newCart.setId(1);
                    cartViewModel.insertCart(newCart);
                    Log.d("BookServiceActivity", "Created new cart, observer will apply saved discount automatically");
                }

                new Handler().postDelayed(() -> {
                    startActivity(new Intent(this, ServiceSelectionActivity.class));
                }, 100);
            }
        });

        cancelButton = findViewById(R.id.cancel_button_top);
        cancelButton.setOnClickListener(l -> {
            cartViewModel.deleteCart();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        backButton = findViewById(R.id.back_button_top);
        backButton.setOnClickListener(v -> onBackPressed());

        // Настройка кнопки повтора
        if (retryButton != null) {
            retryButton.setOnClickListener(v -> loadCarDataFromFirebase());
        }

        carsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupCarAdapter() {
        carAdapter = new CarSelectionAdapter(allCars, this);
        carsRecyclerView.setAdapter(carAdapter);
    }

    private void loadCarDataFromFirebase() {
        Log.d("BookServiceActivity", "Начинаем загрузку данных из Firebase");

        // Показываем индикатор загрузки
        showLoading();

        // Получаем текущего пользователя
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e("BookServiceActivity", "Пользователь не авторизован");
            showError("Необходимо войти в систему");
            return;
        }

        String userId = currentUser.getUid();
        Log.d("BookServiceActivity", "User ID: " + userId);

        // Загружаем автомобили пользователя из Firestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(userId)
                .collection("cars").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d("BookServiceActivity", "Данные успешно загружены из Firestore");
                    Log.d("BookServiceActivity", "Количество документов: " + queryDocumentSnapshots.size());

                    allCars.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Log.d("BookServiceActivity", "Обрабатываем документ: " + document.getId());
                            Log.d("BookServiceActivity", "Данные документа: " + document.getData());

                            Car car = new Car();
                            car.setId(document.getId());

                            // Безопасно извлекаем данные
                            if (document.contains("make")) {
                                car.setMake(document.getString("make"));
                            }
                            if (document.contains("model")) {
                                car.setModel(document.getString("model"));
                            }
                            if (document.contains("generation")) {
                                car.setGeneration(document.getString("generation"));
                            }

                            allCars.add(car);
                            Log.d("BookServiceActivity", "Добавлен автомобиль: " + car.getMake() + " " + car.getModel());

                        } catch (Exception e) {
                            Log.e("BookServiceActivity", "Ошибка при парсинге автомобиля: " + e.getMessage(), e);
                        }
                    }

                    Log.d("BookServiceActivity", "Всего загружено автомобилей: " + allCars.size());

                    // Сортируем автомобили
                    Collections.sort(allCars, (car1, car2) -> {
                        String make1 = car1.getMake() != null ? car1.getMake() : "";
                        String make2 = car2.getMake() != null ? car2.getMake() : "";
                        int makeComparison = make1.compareToIgnoreCase(make2);
                        if (makeComparison != 0) {
                            return makeComparison;
                        }
                        String model1 = car1.getModel() != null ? car1.getModel() : "";
                        String model2 = car2.getModel() != null ? car2.getModel() : "";
                        return model1.compareToIgnoreCase(model2);
                    });

                    // Обновляем UI
                    carAdapter.updateCars(allCars);

                    if (allCars.isEmpty()) {
                        showEmptyState();
                    } else {
                        showContent();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("BookServiceActivity", "Ошибка при загрузке автомобилей: " + e.getMessage(), e);
                    showError("Ошибка при загрузке данных автомобилей: " + e.getMessage());
                });
    }

    private void showLoading() {
        if (loadingContainer != null) {
            loadingContainer.setVisibility(View.VISIBLE);
        }
        carsRecyclerView.setVisibility(View.GONE);
        if (emptyStateContainer != null) {
            emptyStateContainer.setVisibility(View.GONE);
        }
        if (errorStateContainer != null) {
            errorStateContainer.setVisibility(View.GONE);
        }
        continueButton.setEnabled(false);
        continueButton.setAlpha(0.5f);
    }

    private void showContent() {
        if (loadingContainer != null) {
            loadingContainer.setVisibility(View.GONE);
        }
        carsRecyclerView.setVisibility(View.VISIBLE);
        if (emptyStateContainer != null) {
            emptyStateContainer.setVisibility(View.GONE);
        }
        if (errorStateContainer != null) {
            errorStateContainer.setVisibility(View.GONE);
        }
    }

    private void showEmptyState() {
        if (loadingContainer != null) {
            loadingContainer.setVisibility(View.GONE);
        }
        carsRecyclerView.setVisibility(View.GONE);
        if (emptyStateContainer != null) {
            emptyStateContainer.setVisibility(View.VISIBLE);
        }
        if (errorStateContainer != null) {
            errorStateContainer.setVisibility(View.GONE);
        }
        continueButton.setEnabled(false);
        continueButton.setAlpha(0.5f);
    }

    private void showError(String message) {
        if (loadingContainer != null) {
            loadingContainer.setVisibility(View.GONE);
        }
        carsRecyclerView.setVisibility(View.GONE);
        if (emptyStateContainer != null) {
            emptyStateContainer.setVisibility(View.GONE);
        }
        if (errorStateContainer != null) {
            errorStateContainer.setVisibility(View.VISIBLE);
        }
        if (errorText != null) {
            errorText.setText(message);
        }
        continueButton.setEnabled(false);
        continueButton.setAlpha(0.5f);

        // Также показываем Toast
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onCarSelected(Car car) {
        selectedCar = car;
        continueButton.setEnabled(true);
        continueButton.setAlpha(1.0f);
        Log.d("BookServiceActivity", "Выбран автомобиль: " + car.getFullName());
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }
}