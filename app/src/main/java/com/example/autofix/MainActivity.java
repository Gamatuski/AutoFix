package com.example.autofix;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.autofix.fragments.GarageFragment;
import com.example.autofix.fragments.HomeFragment;
import com.example.autofix.fragments.MapFragment;
import com.example.autofix.fragments.ProfileFragment;
import com.example.autofix.services.NotificationReminderService;
import com.example.autofix.sto.CartServiceActivity;
import com.example.autofix.viewmodels.CartViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private BottomNavigationView bottomNav;
    private CardView cartCard;
    private TextView cartCarName;
    private ImageView cartArrow, cartDelete;
    private CartViewModel cartViewModel;
    private int previousMenuItemId = R.id.nav_home;
    private static final int REQUEST_NOTIFICATION_PERMISSION = 1001;
    private RequestQueue requestQueue;
    // Кэши для данных
    public static List<String> cachedMakes = null;
    public static Map<String, List<String>> cachedModels = new HashMap<>();

    // Определим порядок вкладок для правильной анимации
    private final int[] tabOrder = {R.id.nav_home, R.id.nav_garage, R.id.nav_map, R.id.nav_profile};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bottomNav = findViewById(R.id.bottom_navigation);

        cartCard = findViewById(R.id.cart_card);
        cartCarName = findViewById(R.id.cart_car_name);
        cartArrow = findViewById(R.id.cart_arrow);
        cartDelete = findViewById(R.id.cart_delete);

        // Инициализация ViewModel
        cartViewModel = new ViewModelProvider(this).get(CartViewModel.class);

        // Наблюдаем за изменениями в корзине
        cartViewModel.getCart().observe(this, cart -> {
            if (cart != null && cart.getSelectedCarId() != null) {
                cartCard.setVisibility(View.VISIBLE);
                cartCarName.setText(cart.getSelectedCarName() != null ?
                        cart.getSelectedCarName() : "Выбранный автомобиль");
            } else {
                cartCard.setVisibility(View.GONE);
            }
        });

        // Обработка кликов по элементам карточки
        cartArrow.setOnClickListener(v -> {
            // Переход к корзине
            startActivity(new Intent(this, CartServiceActivity.class));
        });

        cartDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Удаление Записи")
                    .setMessage("Выдействительно хотите удалить текущую запись?")
                    .setPositiveButton("Да", (dialog, which) -> {
                        cartViewModel.deleteCart();
                    })
                    .setNegativeButton("Нет", null)
                    .show();
        });


        // Загружаем стартовый фрагмент
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }

        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == previousMenuItemId) {
                return true;
            }

            Fragment selectedFragment = getFragmentForMenuItem(item.getItemId());
            if (selectedFragment != null) {
                boolean isMovingRight = isMovingRight(previousMenuItemId, item.getItemId());

                getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(
                                isMovingRight ? R.anim.slide_in_right : R.anim.slide_in_left,
                                isMovingRight ? R.anim.slide_out_left : R.anim.slide_out_right,
                                isMovingRight ? R.anim.slide_in_left : R.anim.slide_in_right,
                                isMovingRight ? R.anim.slide_out_right : R.anim.slide_out_left
                        )
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
                previousMenuItemId = item.getItemId();
            }
            return true;
        });
        startNotificationService();


        setupRequestQueue();

        // Предзагрузка популярных марок
        if (cachedMakes == null) {
            loadCarMakes();
        } else {
            Log.d(TAG, "Кэшированные марки уже доступны");
        }

        // Пример использования: можно вызвать после загрузки марок
        new Handler(Looper.getMainLooper()).postDelayed(this::preloadPopularModels, 2000);
    }

    private void setupRequestQueue() {
        requestQueue = Volley.newRequestQueue(this);
    }

    // === Метод: Загрузка всех марок с фильтрацией популярных ===
    public void loadCarMakes() {
        String url = "https://vpic.nhtsa.dot.gov/api/vehicles/getallmakes?format=json";

        // Список популярных марок из AddCarActivity
        Set<String> popularMakes = new HashSet<>(Arrays.asList(
                "AUDI", "BMW", "FORD", "HONDA", "HYUNDAI", "INFINITI",
                "KIA", "LADA", "LAND ROVER", "LEXUS", "MAZDA",
                "MERCEDES-BENZ", "MITSUBISHI", "NISSAN", "OPEL",
                "SKODA", "SUBARU", "SUZUKI", "TOYOTA", "VOLKSWAGEN", "VOLVO"
        ));

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray results = response.getJSONArray("Results");
                        Set<String> uniqueMakes = new HashSet<>();
                        for (int i = 0; i < results.length(); i++) {
                            JSONObject makeObj = results.getJSONObject(i);
                            String makeName = makeObj.getString("Make_Name").toUpperCase().trim();
                            if (popularMakes.contains(makeName)) {
                                uniqueMakes.add(formatMakeName(makeName));
                            }
                        }

                        cachedMakes = new ArrayList<>(uniqueMakes);
                        Collections.sort(cachedMakes);
                        Log.d(TAG, "Загружено " + cachedMakes.size() + " популярных марок");

                    } catch (JSONException e) {
                        Log.e(TAG, "Ошибка парсинга JSON при загрузке марок", e);
                        useStaticMakes();
                    }
                },
                error -> {
                    Log.e(TAG, "Ошибка сети при загрузке марок", error);
                    useStaticMakes();
                });

        request.setRetryPolicy(new DefaultRetryPolicy(
                15000, // 15 секунд
                1,     // одна попытка
                1.0f
        ));
        request.setTag(TAG);
        requestQueue.add(request);
    }

    // === Резервные данные, если API не отвечает ===
    private void useStaticMakes() {
        cachedMakes = new ArrayList<>(Arrays.asList(
                "Audi", "BMW", "Ford", "Honda", "Hyundai", "Infiniti",
                "Kia", "Lada", "Land Rover", "Lexus", "Mazda",
                "Mercedes-Benz", "Mitsubishi", "Nissan", "Opel",
                "Skoda", "Subaru", "Suzuki", "Toyota", "Volkswagen", "Volvo"
        ));
        Log.d(TAG, "Используются статические марки");
    }

    // === Форматирование названия марки (копия из AddCarActivity) ===
    private String formatMakeName(String makeName) {
        String[] words = makeName.toLowerCase().split("\\s+");
        StringBuilder formatted = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                if (formatted.length() > 0) formatted.append(" ");
                if (word.equals("bmw") || word.equals("gmc")) {
                    formatted.append(word.toUpperCase());
                } else if (word.contains("-")) {
                    String[] parts = word.split("-");
                    for (int i = 0; i < parts.length; i++) {
                        if (i > 0) formatted.append("-");
                        formatted.append(Character.toUpperCase(parts[i].charAt(0)))
                                .append(parts[i].substring(1));
                    }
                } else {
                    formatted.append(Character.toUpperCase(word.charAt(0)))
                            .append(word.substring(1));
                }
            }
        }
        return formatted.toString();
    }

    // === Предзагрузка моделей для популярных марок ===
    private void preloadPopularModels() {
        if (cachedMakes == null || cachedMakes.isEmpty()) {
            Log.w(TAG, "Невозможно предзагрузить модели: марки ещё не загружены");
            return;
        }

        for (String make : cachedMakes) {
            if (!cachedModels.containsKey(make)) {
                loadCarModels(make);
            }
        }
    }

    // === Метод: Загрузка моделей для конкретной марки ===
    private void loadCarModels(String make) {
        if (cachedModels.containsKey(make)) {
            Log.d(TAG, "Модели для " + make + " уже закэшированы");
            return;
        }

        String encodedMake = Uri.encode(make);
        String url = "https://vpic.nhtsa.dot.gov/api/vehicles/getmodelsformake/" + encodedMake + "?format=json";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray results = response.getJSONArray("Results");
                        Set<String> uniqueModels = new HashSet<>();
                        for (int i = 0; i < results.length(); i++) {
                            JSONObject model = results.getJSONObject(i);
                            String modelName = model.getString("Model_Name").trim();
                            if (!modelName.isEmpty() && modelName.length() <= 30) {
                                uniqueModels.add(modelName);
                                if (uniqueModels.size() >= 80) break;
                            }
                        }
                        List<String> sortedModels = new ArrayList<>(uniqueModels);
                        Collections.sort(sortedModels);
                        cachedModels.put(make, sortedModels);
                        Log.d(TAG, "Загружено " + sortedModels.size() + " моделей для " + make);

                    } catch (JSONException e) {
                        Log.e(TAG, "Ошибка парсинга JSON при загрузке моделей для " + make, e);
                    }
                },
                error -> Log.e(TAG, "Ошибка сети при загрузке моделей для " + make, error));

        request.setRetryPolicy(new DefaultRetryPolicy(
                10000, // 10 секунд
                1,
                1.0f
        ));
        request.setTag(TAG + "_model_" + make);
        requestQueue.add(request);
    }

    // Новый метод для определения направления движения
    private boolean isMovingRight(int fromItemId, int toItemId) {
        int fromIndex = -1;
        int toIndex = -1;

        // Находим индексы вкладок в нашем порядке
        for (int i = 0; i < tabOrder.length; i++) {
            if (tabOrder[i] == fromItemId) {
                fromIndex = i;
            }
            if (tabOrder[i] == toItemId) {
                toIndex = i;
            }
        }

        // Если нашли оба индекса, сравниваем их
        if (fromIndex != -1 && toIndex != -1) {
            return toIndex > fromIndex;
        }

        // По умолчанию считаем, что движемся вправо
        return true;
    }

    private Fragment getFragmentForMenuItem(int itemId) {
        if(itemId == R.id.nav_home){
            return new HomeFragment();
        } else if(itemId == R.id.nav_garage) {
            return new GarageFragment();
        } else if(itemId == R.id.nav_map) {
            return new MapFragment();
        } else if(itemId == R.id.nav_profile) {
            return new ProfileFragment();
        }
        return null;
    }

    private void startNotificationService() {
        // Проверяем разрешение на уведомления (только для Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, "android.permission.POST_NOTIFICATIONS")
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{"android.permission.POST_NOTIFICATIONS"},
                        REQUEST_NOTIFICATION_PERMISSION);
                return;
            }
        }

        // Запускаем сервис уведомлений
        NotificationReminderService.startService(this);
        Log.d("MainActivity", "Notification service started");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startNotificationService();
            } else {
                Toast.makeText(this, "Разрешение на уведомления необходимо для напоминаний",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

}