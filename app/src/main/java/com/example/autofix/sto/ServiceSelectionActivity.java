package com.example.autofix.sto;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
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
import com.example.autofix.data.entities.Cart;
import com.example.autofix.models.ServiceCategory;
import com.example.autofix.models.ServiceSubcategory;
import com.example.autofix.search.SearchActivity;
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
    private TextView totalPriceText,totalOldPriceText;
    private CartViewModel cartViewModel;
    private Button proceedToBookingButton;
    private  ImageView cancelButton, backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_selection);

        View progressFill = findViewById(R.id.progress_fill);
        View secondDot = findViewById(R.id.second_dot);

        ViewTreeObserver vto = secondDot.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                secondDot.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                // Получаем ширину родительского LinearLayout
                LinearLayout dotsContainer = (LinearLayout) secondDot.getParent().getParent();
                int containerWidth = dotsContainer.getWidth();

                // Вторая точка находится на 2/3 от общей ширины
                int progressWidth = (int) (containerWidth * 0.5f); // 2/3 от ширины

                ViewGroup.LayoutParams params = progressFill.getLayoutParams();
                params.width = progressWidth;
                progressFill.setLayoutParams(params);
            }
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


        // Инициализация summary
        summaryContainer = findViewById(R.id.summary_card);
        totalPriceText = findViewById(R.id.total_price_text);
        totalOldPriceText = findViewById(R.id.total_old_price_text);

        // Наблюдаем за изменениями в корзине
        setupCartObservers();

        // Поиск
        searchInput.setOnClickListener(v -> openSearchActivity());
        searchInput.setFocusable(false);
        searchInput.setClickable(true);


        // Инициализация кнопки
        proceedToBookingButton = findViewById(R.id.proceed_to_booking_button);
        proceedToBookingButton.setOnClickListener(v -> openCartServiceActivity());

        cancelButton = findViewById(R.id.cancel_button_top);
        cancelButton.setOnClickListener(v -> showCancelConfirmationDialog());

        backButton = findViewById(R.id.back_button_top);
        backButton.setOnClickListener(v -> onBackPressed());

        loadCategories();
    }

    private void openSearchActivity() {
        Intent intent = new Intent(this, SearchActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_bottom, R.anim.slide_out_top);
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
            updateTotalPriceDisplay(totalPrice);
        });

        // Добавляем наблюдение за изменениями корзины
        cartViewModel.getCart().observe(this, cart -> {
            // Обновляем отображение цены при изменении скидки
            Integer totalPrice = cartViewModel.getTotalPrice().getValue();
            if (totalPrice != null) {
                updateTotalPriceDisplay(totalPrice);
            }
        });
    }

    private void updateTotalPriceDisplay(Integer totalPrice) {
        if (totalPrice == null) return;

        Cart cart = cartViewModel.getCart().getValue();
        if (cart != null && cart.getDiscount() != null && cart.getDiscount() > 0) {
            // Рассчитываем оригинальную цену без скидки
            int discount = cart.getDiscount();
            int originalPrice = totalPrice * 100 / (100 - discount);

            // Показываем старую цену зачеркнутой
            totalOldPriceText.setText(String.format("%d ₽", originalPrice));
            totalOldPriceText.setVisibility(View.VISIBLE);
            totalOldPriceText.setPaintFlags(totalOldPriceText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

            // Показываем новую цену со скидкой
            totalPriceText.setText(String.format("%d ₽", totalPrice));
        } else {
            // Скрываем старую цену и показываем только обычную
            totalOldPriceText.setVisibility(View.GONE);
            totalPriceText.setText(String.format("%d ₽", totalPrice));
        }
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
        // Проверяем, что адаптер инициализирован
        if (serviceAdapter == null) {
            serviceAdapter = new ServiceItemAdapter(
                    new ArrayList<>(),
                    ServiceItemAdapter.ItemType.CATEGORY,
                    this);
            servicesRecyclerView.setAdapter(serviceAdapter);
        }

        serviceAdapter.showLoading();

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
                        // Обновляем данные в адаптере
                        serviceAdapter.updateItems(categories);
                    } else {
                        Toast.makeText(this, "Ошибка загрузки категорий", Toast.LENGTH_SHORT).show();
                        serviceAdapter.hideLoading();
                    }
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
