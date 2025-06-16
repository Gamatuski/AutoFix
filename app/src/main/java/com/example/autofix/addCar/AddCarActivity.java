package com.example.autofix.addCar;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.autofix.R;
import com.example.autofix.adapters.CarListAdapter;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class AddCarActivity extends AppCompatActivity {
    private static final String TAG = "AddCarActivity";
    private ImageView backButton;
    private TextInputLayout makeInputLayout, modelInputLayout, generationInputLayout;
    private TextView makeTextView, modelTextView, generationTextView;
    private Button btnAddCar;
    private List<String> makesList = new ArrayList<>();
    private List<String> modelsList = new ArrayList<>();
    private List<String> generationsList = new ArrayList<>();
    private String selectedMake = "";
    private String selectedModel = "";
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private RecyclerView listRecyclerView;
    private CarListAdapter listAdapter;
    private String currentListType = ""; // "make", "model" или "generation"
    private Map<String, List<String>> modelsCache = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_car);

        initViews();
        setupClickListeners();
        setupRecyclerView();

        // Получаем данные из Intent
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            if (extras.containsKey("makes_list")) {
                makesList = extras.getStringArrayList("makes_list");
                updateMakeUI();
            }

            if (extras.containsKey("models_cache")) {
                Bundle modelsBundle = extras.getBundle("models_cache");
                for (String key : modelsBundle.keySet()) {
                    modelsCache.put(key, modelsBundle.getStringArrayList(key));
                }
            }
        }

        // Если данных нет — можно вызвать loadCarMakes() с ошибкой
        if (makesList == null || makesList.isEmpty()) {
            Toast.makeText(this, "Нет данных о марках", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void updateMakeUI() {
        runOnUiThread(() -> {
            makeInputLayout.setEnabled(true);
            if (!makesList.isEmpty()) {
                showList("make", makesList);
            }
        });
    }

    private void initViews() {
        backButton = findViewById(R.id.back_button_top);
        makeInputLayout = findViewById(R.id.make_input_layout);
        modelInputLayout = findViewById(R.id.model_input_layout);
        generationInputLayout = findViewById(R.id.generation_input_layout);
        makeTextView = findViewById(R.id.make_text_view);
        modelTextView = findViewById(R.id.model_text_view);
        generationTextView = findViewById(R.id.generation_text_view);
        btnAddCar = findViewById(R.id.btn_add_car);
        listRecyclerView = findViewById(R.id.list_recycler_view);

        modelInputLayout.setEnabled(false);
        generationInputLayout.setEnabled(false);
    }

    private void setupRecyclerView() {
        listAdapter = new CarListAdapter(new ArrayList<>(), item -> {
            switch (currentListType) {
                case "make":
                    handleMakeSelection(item);
                    break;
                case "model":
                    handleModelSelection(item);
                    break;
                case "generation":
                    handleGenerationSelection(item);
                    break;
            }
            hideList();
        });

        listRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        listRecyclerView.setAdapter(listAdapter);
        listRecyclerView.setVisibility(View.GONE);
    }

    private void handleMakeSelection(String make) {
        selectedMake = make;
        makeTextView.setText(make);
        modelInputLayout.setEnabled(true);
        modelTextView.setText("");
        generationTextView.setText("");
        generationInputLayout.setEnabled(false);

        if (modelsCache.containsKey(selectedMake)) {
            modelsList.clear();
            modelsList.addAll(modelsCache.get(selectedMake));
            showList("model", modelsList);
        } else {
            Toast.makeText(this, "Модели для этой марки недоступны", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleModelSelection(String model) {
        selectedModel = model;
        modelTextView.setText(model);
        generationInputLayout.setEnabled(true);
        generationTextView.setText("");

        loadDefaultGenerations();
        showList("generation", generationsList);
    }

    private void handleGenerationSelection(String generation) {
        generationTextView.setText(generation);
    }

    private void showList(String type, List<String> items) {
        currentListType = type;
        listAdapter.updateItems(items);
        listRecyclerView.setVisibility(View.VISIBLE);
    }

    private void hideList() {
        listRecyclerView.setVisibility(View.GONE);
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());

        makeInputLayout.setOnClickListener(v -> {
            if (!makesList.isEmpty()) {
                showList("make", makesList);
            }
        });

        modelInputLayout.setOnClickListener(v -> {
            if (modelInputLayout.isEnabled() && !modelsList.isEmpty()) {
                showList("model", modelsList);
            }
        });

        generationInputLayout.setOnClickListener(v -> {
            if (generationInputLayout.isEnabled() && !generationsList.isEmpty()) {
                showList("generation", generationsList);
            }
        });

        btnAddCar.setOnClickListener(v -> addCar());
    }

    private void loadDefaultGenerations() {
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        generationsList.clear();
        generationsList.add((currentYear - 2) + "-" + currentYear);
        generationsList.add((currentYear - 5) + "-" + (currentYear - 3));
        generationsList.add((currentYear - 8) + "-" + (currentYear - 6));
        generationsList.add("Текущее поколение");
        generationsList.add("Предыдущее поколение");
        generationsList.add("Рестайлинг");
    }

    private void addCar() {
        String make = makeTextView.getText().toString().trim();
        String model = modelTextView.getText().toString().trim();
        String generation = generationTextView.getText().toString().trim();

        if (make.isEmpty()) {
            Toast.makeText(this, "Выберите марку автомобиля", Toast.LENGTH_SHORT).show();
            return;
        }
        if (model.isEmpty()) {
            Toast.makeText(this, "Выберите модель автомобиля", Toast.LENGTH_SHORT).show();
            return;
        }
        if (generation.isEmpty()) {
            Toast.makeText(this, "Выберите поколение авто", Toast.LENGTH_SHORT).show();
            return;
        }

        saveCarToFirebase(make, model, generation);
    }

    private void saveCarToFirebase(String make, String model, String generation) {
        btnAddCar.setEnabled(false);
        btnAddCar.setText("Поиск изображения...");

        String carId = UUID.randomUUID().toString();
        Map<String, Object> carData = new HashMap<>();
        carData.put("id", carId);
        carData.put("make", make);
        carData.put("model", model);
        carData.put("generation", generation);
        carData.put("createdAt", com.google.firebase.Timestamp.now());
        carData.put("updatedAt", com.google.firebase.Timestamp.now());
        carData.put("image", "");

        saveCarDataToFirebase(carData);
    }

    private void saveCarDataToFirebase(Map<String, Object> carData) {
        String carId = (String) carData.get("id");
        String make = (String) carData.get("make");
        String model = (String) carData.get("model");
        String generation = (String) carData.get("generation");

        FirebaseUser currentUser = mAuth.getCurrentUser();
        String userId = currentUser.getUid();

        DocumentReference userDocRef = db.collection("users").document(userId);
        DocumentReference carDocRef = userDocRef.collection("cars").document(carId);

        btnAddCar.setText("Сохранение в базу...");

        carDocRef.set(carData)
                .addOnSuccessListener(aVoid -> {
                    btnAddCar.setEnabled(true);
                    btnAddCar.setText("Добавить автомобиль");
                    String successMessage = "Автомобиль " + make + " " + model + " " + generation + " успешно добавлен!";
                    Toast.makeText(AddCarActivity.this, successMessage, Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnAddCar.setEnabled(true);
                    btnAddCar.setText("Добавить автомобиль");
                    Toast.makeText(AddCarActivity.this,
                            "Ошибка сохранения автомобиля: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}