package com.example.autofix.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.autofix.CreateAccountActivity;


import com.example.autofix.MainActivity;
import com.example.autofix.R;
import com.example.autofix.adapters.CarsAdapter;
import com.example.autofix.addCar.AddCarActivity;
import com.example.autofix.addCar.CarDetailsActivity;
import com.example.autofix.models.Car;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class GarageFragment extends Fragment {
    private View emptyStateView, emptyUserView;

    private RecyclerView recyclerView;
    private CarsAdapter carsAdapter;
    private List<Car> carsList;
    private FirebaseUser currentUser;
    private Button btnAddCar;

    public GarageFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_garage, container, false);

        // Инициализация views
        emptyStateView = view.findViewById(R.id.empty_state_layout);
        emptyUserView = view.findViewById(R.id.empty_user_layout);

        recyclerView = view.findViewById(R.id.recycler_view_cars);
        btnAddCar = view.findViewById(R.id.btn_add_car);

        // Настройка RecyclerView
        carsList = new ArrayList<>();
        carsAdapter = new CarsAdapter(carsList, getContext(), car -> {
            // Обработка клика по автомобилю
            Intent intent = new Intent(getContext(), CarDetailsActivity.class);
            intent.putExtra("car_id", car.getId());
            startActivity(intent);
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(carsAdapter);

        // Настройка кнопки добавления авто
        btnAddCar.setOnClickListener(v -> {
            openAddCarActivity();
        });

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            // Пользователь авторизован, загружаем данные
            loadCarsFromFirebase();
        } else {
            // Пользователь не авторизован
            showGuestView();
        }

        return view;
    }

    private void loadCarsFromFirebase() {
        carsAdapter.showLoading();

        String userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        Log.d("GarageFragment", "Loading cars for user: " + userId);

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .collection("cars")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Car> carsList = new ArrayList<>();
                        Log.d("GarageFragment", "Found " + task.getResult().size() + " cars");

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                Car car = new Car();
                                car.setId(document.getId());

                                if (document.contains("make"))
                                    car.setBrand(document.getString("make"));
                                if (document.contains("model"))
                                    car.setModel(document.getString("model"));
                                if (document.contains("generation"))
                                    car.setGeneration(document.getString("generation"));
                                if (document.contains("image"))
                                    car.setImageUrl(document.getString("image"));
                                carsList.add(car);
                                Log.d("GarageFragment", "Added car: " + car.getBrand() + " " + car.getModel());

                            } catch (Exception e) {
                                Log.e("GarageFragment", "Error parsing car: " + e.getMessage());
                            }
                        }

                        // ВАЖНО: Обновляем список с полученными данными
                        updateCarsList(carsList);

                    } else {
                        Log.e("GarageFragment", "Error getting cars", task.getException());
                        // Показываем пустой список в случае ошибки
                        updateCarsList(new ArrayList<>());
                    }
                });
    }

    private void updateCarsList(List<Car> carsList) {
        this.carsList.clear();
        this.carsList.addAll(carsList);

        if (carsList.isEmpty()) {
            showEmptyState();
        } else {
            showCarsRecyclerView();
        }

        if (carsAdapter != null) {
            carsAdapter.updateCars(carsList);
            carsAdapter.hideLoading();
        }


        Log.d("GarageFragment", "Updated cars list with " + carsList.size() + " items");
    }

    private void openAddCarActivity() {
        Intent intent = new Intent(getActivity(), AddCarActivity.class);

        if (MainActivity.cachedMakes != null) {
            intent.putStringArrayListExtra("makes_list", new ArrayList<>(MainActivity.cachedMakes));
        }

        // Передаем модели популярных марок
        Bundle modelsBundle = new Bundle();
        for (Map.Entry<String, List<String>> entry : MainActivity.cachedModels.entrySet()) {
            modelsBundle.putStringArrayList(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        intent.putExtra("models_cache", modelsBundle);

        startActivity(intent);
    }

    private void showGuestView() {
        emptyUserView.setVisibility(View.VISIBLE);
        emptyStateView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);

        btnAddCar.setEnabled(false);

        Button registerButton = emptyUserView.findViewById(R.id.btn_register);
        registerButton.setVisibility(View.VISIBLE);
        registerButton.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), CreateAccountActivity.class));
        });
    }

    private void showEmptyState() {
        emptyStateView.setVisibility(View.VISIBLE);
        emptyUserView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);

        btnAddCar.setEnabled(true);
    }

    private void showCarsRecyclerView() {
        recyclerView.setVisibility(View.VISIBLE);
        emptyStateView.setVisibility(View.GONE);
        emptyUserView.setVisibility(View.GONE);

        btnAddCar.setEnabled(true);
    }



    @Override
    public void onResume() {
        super.onResume();

        // Обновляем данные при возвращении на фрагмент
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            loadCarsFromFirebase();
        } else {
            showGuestView();
        }
    }

}