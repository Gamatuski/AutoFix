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
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.autofix.MainActivity;
import com.example.autofix.R;
import com.example.autofix.adapters.ServiceListAdapter;
import com.example.autofix.bottomsheets.ServiceDetailsBottomSheet;
import com.example.autofix.data.entities.Cart;
import com.example.autofix.models.Service;
import com.example.autofix.viewmodels.CartViewModel;
import com.example.autofix.viewmodels.CartViewModelFactory;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ServiceListActivity extends AppCompatActivity implements ServiceListAdapter.OnServiceActionListener {

    private RecyclerView servicesRecyclerView;
    private ServiceListAdapter serviceAdapter;
    private FirebaseFirestore db;

    private String categoryId;
    private String subcategoryId;
    private String subcategoryName;
    private CartViewModel cartViewModel;

    private CardView summaryContainer;
    private TextView totalPriceText;
    private Button proccedToCart;
    private  ImageView cancelButton, backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_list);

        // Инициализируем ViewModel
        CartViewModelFactory factory = new CartViewModelFactory(getApplication());
        cartViewModel = new ViewModelProvider(this, factory).get(CartViewModel.class);

        // Получаем данные из Intent
        categoryId = getIntent().getStringExtra("category_id");
        subcategoryId = getIntent().getStringExtra("subcategory_id");
        subcategoryName = getIntent().getStringExtra("subcategory_name");

        if (categoryId == null || subcategoryId == null) {
            Toast.makeText(this, "Ошибка: ID категории или подкатегории не найден", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Инициализация Firebase
        db = FirebaseFirestore.getInstance();

        // Настройка шапки
        setupHeader();

        summaryContainer = findViewById(R.id.summary_card);
        totalPriceText = findViewById(R.id.total_price_text);
        proccedToCart = findViewById(R.id.proceed_to_booking_button);
        servicesRecyclerView = findViewById(R.id.services_recycler);

        proccedToCart.setOnClickListener(v -> {
            Intent intent = new Intent(this, CartServiceActivity.class);
            startActivity(intent);
        });

        cancelButton = findViewById(R.id.cancel_button_top);
        cancelButton.setOnClickListener(v -> showCancelConfirmationDialog());

        backButton = findViewById(R.id.back_button_top);
        backButton.setOnClickListener(v -> onBackPressed());

        // Настройка RecyclerView в виде сетки (2 колонки)
        servicesRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        // Передаем ViewModel и FragmentManager в адаптер
        serviceAdapter = new ServiceListAdapter(
                new ArrayList<>(),
                this,
                cartViewModel,
                this
        );
        servicesRecyclerView.setAdapter(serviceAdapter);

        // Наблюдаем за изменениями в корзине
        setupCartObservers();

        // Загрузка данных
        loadServices();

    }

    private void setupHeader() {

        TextView titleText = findViewById(R.id.title_text);
        titleText.setText(subcategoryName != null ? subcategoryName : "Услуги");

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

    private void loadServices() {

        db.collection("service_categories")
                .document(categoryId)
                .collection("subcategories")
                .document(subcategoryId)
                .collection("services")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Service> services = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Service service = document.toObject(Service.class);
                            service.setId(document.getId());
                            services.add(service);
                        }

                        // Обновляем адаптер с полученными услугами
                        serviceAdapter.updateServices(services);


                    } else {
                        Toast.makeText(this, "Ошибка загрузки услуг", Toast.LENGTH_SHORT).show();
                    }

                });
    }

    @Override
    public void onServiceSelected(Service service) {
        Cart currentCart = cartViewModel.getCart().getValue();

        if (currentCart == null || currentCart.getSelectedCarId() == null) {
            Toast.makeText(this, "Пожалуйста, сначала выберите автомобиль", Toast.LENGTH_SHORT).show();
            return;
        }

        if (getSupportFragmentManager().findFragmentByTag("ServiceDetails") == null) {
            ServiceDetailsBottomSheet bottomSheet = ServiceDetailsBottomSheet.newInstance(service);

            // Упрощаем слушатель - он будет срабатывать только при реальном добавлении
            bottomSheet.setOnServiceAddedListener(service1 -> {
                // Toast показываем только если действительно было добавление
                Toast.makeText(this, "Услуга добавлена: " + service1.getName(),
                        Toast.LENGTH_SHORT).show();
            });

            bottomSheet.show(getSupportFragmentManager(), "ServiceDetails");
        }
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