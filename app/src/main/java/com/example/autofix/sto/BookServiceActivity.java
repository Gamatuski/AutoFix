package com.example.autofix.sto;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.autofix.MainActivity;
import com.example.autofix.R;
import com.example.autofix.adapters.CarItemAdapter;
import com.example.autofix.data.entities.Cart;
import com.example.autofix.viewmodels.CartViewModel;
import com.example.autofix.viewmodels.CartViewModelFactory;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class BookServiceActivity extends AppCompatActivity
        implements CarItemAdapter.OnItemClickListener {

    private AutoCompleteTextView brandInput, modelInput;
    private RecyclerView itemsRecycler;
    private Button continueButton;
    private CarItemAdapter itemAdapter;
    private List<String> allBrands = new ArrayList<>();
    private Map<String, List<String>> brandToModels = new HashMap<>();
    private String currentMode = "brand"; // "brand" или "model"
    private String selectedBrand = "";
    private String selectedModel = "";
    private boolean brandFieldClicked = false;
    private boolean modelFieldClicked = false;
    private CartViewModel cartViewModel;
    private ImageView cancelButton, backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_service);

        // Находим элементы
        View progressFill = findViewById(R.id.progress_fill);
        View firstDot = findViewById(R.id.first_dot);

        // Устанавливаем ширину прогресс-бара до первой точки
        firstDot.post(() -> {
            int dotPosition = (int) firstDot.getX() + firstDot.getWidth() / 2;
            ViewGroup.LayoutParams params = progressFill.getLayoutParams();
            params.width = dotPosition;
            progressFill.setLayoutParams(params);
        });

        // Инициализация ViewModel с фабрикой
        CartViewModelFactory factory = new CartViewModelFactory(getApplication());
        cartViewModel = new ViewModelProvider(this, factory).get(CartViewModel.class);

        initViews();
        setupInputs();
        loadCarData();

        // Инициализируем адаптер с пустым списком
        itemAdapter = new CarItemAdapter(new ArrayList<>(), this);
        itemsRecycler.setAdapter(itemAdapter);
    }

    private void initViews() {
        brandInput = findViewById(R.id.brand_input);
        modelInput = findViewById(R.id.model_input);
        itemsRecycler = findViewById(R.id.models_recycler);
        continueButton = findViewById(R.id.continue_button);

        continueButton.setOnClickListener(v -> {
            selectedBrand = brandInput.getText().toString();
            selectedModel = modelInput.getText().toString();
            // Проверка на пустые поля
            boolean hasErrors = false;

            // Проверка марки автомобиля
            if (selectedBrand.isEmpty()) {
                brandInput.setError("Заполните поле");
                hasErrors = true;
            } else if (!allBrands.contains(selectedBrand)) {
                brandInput.setError("Выберите марку из списка");
                hasErrors = true;
            } else {
                brandInput.setError(null);
            }

            // Проверка модели автомобиля
            if (selectedModel.isEmpty()) {
                modelInput.setError("Заполните поле");
                hasErrors = true;
            } else if (!brandToModels.containsKey(selectedBrand) ||
                    !brandToModels.get(selectedBrand).contains(selectedModel)) {
                modelInput.setError("Выберите модель из списка");
                hasErrors = true;
            } else {
                modelInput.setError(null);
            }

            if (hasErrors) {
                return;
            }

            String carId = selectedBrand + "_" + selectedModel;
            String carName = selectedBrand + " " + selectedModel;

            // Создаем новую корзину
            Cart newCart = new Cart(carId, carName, 0, 0);
            newCart.setId(1); // Важно установить ID = 1

            // Сохраняем новую корзину и ждем завершения операции
            cartViewModel.insertCart(newCart);

            // Добавим небольшую задержку, чтобы база данных успела обновиться
            new Handler().postDelayed(() -> {
                startActivity(new Intent(this, ServiceSelectionActivity.class));
            }, 100);
        });

        cancelButton = findViewById(R.id.cancel_button_top);
        cancelButton.setOnClickListener(l -> {
            // Возврат на главный экран
            cartViewModel.deleteCart();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        backButton = findViewById(R.id.back_button_top);
        backButton.setOnClickListener(v -> onBackPressed());


        modelInput.setEnabled(false);
        itemsRecycler.setLayoutManager(new LinearLayoutManager(this));
        itemsRecycler.setVisibility(View.GONE);
    }

    private void setupInputs() {
        brandInput.setOnClickListener(v -> {
            currentMode = "brand";
            // Показываем список только при первом клике
            if (!brandFieldClicked) {
                brandFieldClicked = true;
                itemAdapter.updateItems(allBrands);
                itemsRecycler.setVisibility(View.VISIBLE);
            } else {
                // При последующих кликах переключаем видимость
                toggleItemsListVisibility();
            }
        });

        brandInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Сбрасываем ошибку при изменении текста
                brandInput.setError(null);

                if (currentMode.equals("brand")) {
                    List<String> filteredBrands = new ArrayList<>();
                    String query = s.toString().toLowerCase();
                    for (String brand : allBrands) {
                        if (brand.toLowerCase().contains(query)) {
                            filteredBrands.add(brand);
                        }
                    }
                    itemAdapter.updateItems(filteredBrands);
                    // Если есть результаты фильтрации и список скрыт, показываем его
                    if (!filteredBrands.isEmpty() && itemsRecycler.getVisibility() != View.VISIBLE) {
                        itemsRecycler.setVisibility(View.VISIBLE);
                    } else if (filteredBrands.isEmpty() && itemsRecycler.getVisibility() == View.VISIBLE) {
                        // Если результатов нет, скрываем список
                        itemsRecycler.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        modelInput.setOnClickListener(v -> {
            if (!selectedBrand.isEmpty()) {
                currentMode = "model";
                List<String> models = brandToModels.getOrDefault(selectedBrand, new ArrayList<>());
                // Показываем список только при первом клике
                if (!modelFieldClicked) {
                    modelFieldClicked = true;
                    itemAdapter.updateItems(models);
                    itemsRecycler.setVisibility(View.VISIBLE);
                } else {
                    // При последующих кликах переключаем видимость
                    toggleItemsListVisibility();
                }
            }
        });

        modelInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Сбрасываем ошибку при изменении текста
                modelInput.setError(null);

                if (currentMode.equals("model") && !selectedBrand.isEmpty()) {
                    List<String> allModels = brandToModels.getOrDefault(selectedBrand, new ArrayList<>());
                    List<String> filteredModels = new ArrayList<>();
                    String query = s.toString().toLowerCase();
                    for (String model : allModels) {
                        if (model.toLowerCase().contains(query)) {
                            filteredModels.add(model);
                        }
                    }
                    itemAdapter.updateItems(filteredModels);
                    // Если есть результаты фильтрации и список скрыт, показываем его
                    if (!filteredModels.isEmpty() && itemsRecycler.getVisibility() != View.VISIBLE) {
                        itemsRecycler.setVisibility(View.VISIBLE);
                    } else if (filteredModels.isEmpty() && itemsRecycler.getVisibility() == View.VISIBLE) {
                        // Если результатов нет, скрываем список
                        itemsRecycler.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void toggleItemsListVisibility() {
        if (itemsRecycler.getVisibility() == View.VISIBLE) {
            itemsRecycler.setVisibility(View.GONE);
        } else if (!itemAdapter.getItems().isEmpty()) {
            itemsRecycler.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onItemClick(String item) {
        if (currentMode.equals("brand")) {
            selectedBrand = item;
            brandInput.setText(item);
            brandInput.setError(null); // Сбрасываем ошибку
            modelInput.setEnabled(true);
            modelInput.setText("");
            // Сбрасываем флаг клика для поля модели при выборе нового бренда
            modelFieldClicked = false;
        } else if (currentMode.equals("model")) {
            selectedModel = item;
            modelInput.setText(item);
            modelInput.setError(null); // Сбрасываем ошибку
        }
        itemsRecycler.setVisibility(View.GONE);
    }

    private void loadCarData() {
        // Здесь должна быть загрузка из API или локального JSON
        // Пример статических данных:
        allBrands.addAll(Arrays.asList("Audi", "BMW", "Ford", "Toyota", "Volkswagen"));
        brandToModels.put("Audi", Arrays.asList("A3", "A4", "A6", "Q5", "Q7"));
        brandToModels.put("BMW", Arrays.asList("3 Series", "5 Series", "X3", "X5"));
        brandToModels.put("Ford", Arrays.asList("Focus", "Fiesta", "Mondeo", "Kuga"));
        brandToModels.put("Toyota", Arrays.asList("Camry", "Corolla", "RAV4", "Land Cruiser"));
        brandToModels.put("Volkswagen", Arrays.asList("Golf", "Passat", "Tiguan", "Touareg"));
    }

    @Override
    public void onBackPressed() {
        // Просто возвращаемся назад
        super.onBackPressed();
    }

}