package com.example.autofix.sto;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.autofix.MainActivity;
import com.example.autofix.R;
import com.example.autofix.adapters.ServiceItemAdapter;
import com.example.autofix.models.ServiceCategory;
import com.example.autofix.models.ServiceSubcategory;
import com.example.autofix.viewmodels.CartViewModel;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ServiceSelectionActivity extends AppCompatActivity implements ServiceItemAdapter.OnItemClickListener {

    private RecyclerView servicesRecyclerView;
    private ServiceItemAdapter serviceAdapter;
    private FirebaseFirestore db;
    private LinearLayout loading_layout;
    private TextInputEditText searchInput;

    private CardView summaryContainer;
    private TextView totalPriceText;
    private CartViewModel cartViewModel;
    private Button proceedToBookingButton;
    private  ImageView cancelButton, backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_selection);

        View progressFill = findViewById(R.id.progress_fill);
        View secondDot = findViewById(R.id.second_dot);

        // Устанавливаем ширину прогресс-бара до второй точки
        secondDot.post(() -> {
            int dotPosition = (int) secondDot.getX() + secondDot.getWidth() / 2;
            ViewGroup.LayoutParams params = progressFill.getLayoutParams();
            params.width = dotPosition;
            progressFill.setLayoutParams(params);
        });

        // Инициализация ViewModel
        cartViewModel = new ViewModelProvider(this).get(CartViewModel.class);

        cartViewModel.getCart().observe(this, cart -> {
            if (cart == null) {
                Log.e("ServiceSelection", "Cart is null!");
                Toast.makeText(this, "Корзина не загружена", Toast.LENGTH_SHORT).show();
            } else {
                Log.d("ServiceSelection", "Cart loaded: " + cart.getSelectedCarId());
            }
        });

        // Инициализация Firebase
        db = FirebaseFirestore.getInstance();

        // Настройка шапки
        setupHeader();

        // Инициализация views
        loading_layout = findViewById(R.id.loading_layout);
        searchInput = findViewById(R.id.search_input);
        servicesRecyclerView = findViewById(R.id.services_recycler);

        // Настройка RecyclerView в виде сетки (2 колонки)
        servicesRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        // Загрузка данных
        loadCategories();

        // Инициализация summary
        summaryContainer = findViewById(R.id.summary_card);
        totalPriceText = findViewById(R.id.total_price_text);

        // Наблюдаем за изменениями в корзине
        setupCartObservers();

        // Поиск
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (serviceAdapter != null) {
                    serviceAdapter.getFilter().filter(s);
                }
            }
        });
        // Инициализация кнопки
        proceedToBookingButton = findViewById(R.id.proceed_to_booking_button);
        proceedToBookingButton.setOnClickListener(v -> openCartServiceActivity());

        cancelButton = findViewById(R.id.cancel_button_top);
        cancelButton.setOnClickListener(v -> showCancelConfirmationDialog());

        backButton = findViewById(R.id.back_button_top);
        backButton.setOnClickListener(v -> onBackPressed());
    }

    private void setupCartObservers() {
        cartViewModel.getAllCartItems().observe(this, cartItems -> {
            if (cartItems != null && !cartItems.isEmpty()) {
                summaryContainer.setVisibility(View.VISIBLE);
            } else {
                summaryContainer.setVisibility(View.GONE);
            }
        });

        cartViewModel.getTotalPrice().observe(this, totalPrice -> {
            if (totalPrice != null) {
                totalPriceText.setText(String.format("%d ₽", totalPrice));
            }
        });
    }

    private void openCartServiceActivity() {
        Intent intent = new Intent(this, CartServiceActivity.class);
        startActivity(intent);
    }
    private void setupHeader() {
        ImageView backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        TextView stepText = findViewById(R.id.textView);
        stepText.setText("Шаг 2/3");

        TextView titleText = findViewById(R.id.title_text);
        titleText.setText("Выбор категории услуг");

    }

    private void loadCategories() {
        loading_layout.setVisibility(View.VISIBLE);

        db.collection("service_categories")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<ServiceCategory> categories = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            ServiceCategory category = document.toObject(ServiceCategory.class);
                            category.setId(document.getId());
                            categories.add(category);
                        }

                        // Настройка адаптера для категорий
                        serviceAdapter = new ServiceItemAdapter(
                                categories,
                                ServiceItemAdapter.ItemType.CATEGORY,
                                this);
                        servicesRecyclerView.setAdapter(serviceAdapter);

                    } else {
                        Toast.makeText(this, "Ошибка загрузки категорий", Toast.LENGTH_SHORT).show();
                    }

                    loading_layout.setVisibility(View.GONE);
                });
    }

    @Override
    public void onCategoryClick(ServiceCategory category) {
        // Переход к активности с подкатегориями
        Intent intent = new Intent(this, SubcategoryActivity.class);
        intent.putExtra("category_id", category.getId());
        intent.putExtra("category_name", category.getName());
        startActivity(intent);
    }

    @Override
    public void onSubcategoryClick(ServiceSubcategory subcategory) {
        // Этот метод не будет вызываться в данной активности,
        // но он должен быть реализован из-за интерфейса
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
