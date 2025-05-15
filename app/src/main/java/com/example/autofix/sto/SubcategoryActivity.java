package com.example.autofix.sto;


import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class SubcategoryActivity extends AppCompatActivity implements ServiceItemAdapter.OnItemClickListener {

    private RecyclerView subcategoriesRecyclerView;
    private ServiceItemAdapter serviceAdapter;
    private FirebaseFirestore db;

    private String categoryId;
    private String categoryName;

    private CardView summaryContainer;
    private TextView totalPriceText;
    private CartViewModel cartViewModel;
    private Button proceedToBookingButton;
    private  ImageView cancelButton, backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subcategiry_selection);

        // Инициализация ViewModel
        cartViewModel = new ViewModelProvider(this).get(CartViewModel.class);

        // Получаем данные из Intent
        categoryId = getIntent().getStringExtra("category_id");
        categoryName = getIntent().getStringExtra("category_name");

        if (categoryId == null) {
            Toast.makeText(this, "Ошибка: ID категории не найден", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Инициализация Firebase
        db = FirebaseFirestore.getInstance();

        // Настройка шапки
        setupHeader();

        subcategoriesRecyclerView = findViewById(R.id.services_recycler);

        // Настройка RecyclerView в виде сетки (2 колонки)
        subcategoriesRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        // Инициализация summary
        summaryContainer = findViewById(R.id.summary_card);
        totalPriceText = findViewById(R.id.total_price_text);

        // Наблюдаем за изменениями в корзине
        setupCartObservers();

        // Загрузка данных
        loadSubcategories();

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

        TextView titleText = findViewById(R.id.title_text);
        titleText.setText(categoryName); // Используем название категории в заголовке

    }

    private void loadSubcategories() {

        db.collection("service_categories")
                .document(categoryId)
                .collection("subcategories")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<ServiceSubcategory> subcategories = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            ServiceSubcategory subcategory = document.toObject(ServiceSubcategory.class);
                            subcategory.setId(document.getId());
                            subcategories.add(subcategory);
                        }

                        // Настройка адаптера для подкатегорий
                        serviceAdapter = new ServiceItemAdapter(
                                subcategories,
                                ServiceItemAdapter.ItemType.SUBCATEGORY,
                                this);
                        subcategoriesRecyclerView.setAdapter(serviceAdapter);

                    } else {
                        Toast.makeText(this, "Ошибка загрузки подкатегорий", Toast.LENGTH_SHORT).show();
                    }

                });
    }

    @Override
    public void onCategoryClick(ServiceCategory category) {
        // Этот метод не будет вызываться в данной активности,
        // но он должен быть реализован из-за интерфейса
    }

    @Override
    public void onSubcategoryClick(ServiceSubcategory subcategory) {
        // Переход к активности со списком услуг
        Intent intent = new Intent(this, ServiceListActivity.class);
        intent.putExtra("category_id", categoryId);
        intent.putExtra("subcategory_id", subcategory.getId());
        intent.putExtra("subcategory_name", subcategory.getName());
        startActivity(intent);
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