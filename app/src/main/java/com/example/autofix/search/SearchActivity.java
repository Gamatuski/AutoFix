package com.example.autofix.search;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.autofix.R;
import com.example.autofix.adapters.ServiceListAdapter;
import com.example.autofix.bottomsheets.ServiceDetailsBottomSheet;
import com.example.autofix.data.entities.Cart;
import com.example.autofix.data.entities.CartItem;
import com.example.autofix.models.Service;
import com.example.autofix.sto.BookServiceActivity;
import com.example.autofix.viewmodels.CartViewModel;
import com.example.autofix.viewmodels.CartViewModelFactory;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SearchActivity extends AppCompatActivity implements ServiceListAdapter.OnServiceActionListener {
    private TextInputEditText searchInput;
    private TextInputLayout searchInputLayout;
    private RecyclerView searchResultsRecyclerView;
    private ServiceListAdapter searchAdapter;
    private FirebaseFirestore db;
    private CartViewModel cartViewModel;
    private LinearLayout bottomContainer;
    private Button quickBookingButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        // Инициализация ViewModel
        CartViewModelFactory factory = new CartViewModelFactory(getApplication());
        cartViewModel = new ViewModelProvider(this, factory).get(CartViewModel.class);

        // Инициализация Firebase
        db = FirebaseFirestore.getInstance();

        // Инициализация views
        initViews();
        setupCartObservers();
        // Настройка поиска
        setupSearch();

        // Автоматически показываем клавиатуру
        showKeyboard();
    }

    private void initViews() {
        searchInput = findViewById(R.id.search_input);
        searchInputLayout = (TextInputLayout) findViewById(R.id.search_input).getParent().getParent();
        searchResultsRecyclerView = findViewById(R.id.search_results_recycler);
        bottomContainer = findViewById(R.id.bottom_container);
        quickBookingButton = findViewById(R.id.quick_booking_button);

        // Настройка RecyclerView
        searchResultsRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        // Теперь передаем this как OnServiceActionListener
        searchAdapter = new ServiceListAdapter(
                new ArrayList<>(),
                this, // this реализует OnServiceActionListener
                cartViewModel,
                this
        );
        searchResultsRecyclerView.setAdapter(searchAdapter);

        // Обработчик кнопки закрытия в TextInputLayout
        searchInputLayout.setEndIconOnClickListener(v -> finish());

        // Обработчик быстрой записи
        quickBookingButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, BookServiceActivity.class);
            intent.putExtra("IS_QUICK_BOOKING", true);
            startActivity(intent);
        });
    }

    private void setupSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.length() >= 2) {
                    searchServices(query);
                } else {
                    clearResults();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupCartObservers() {
        // Добавляем наблюдение за изменениями корзины для обновления адаптера
        cartViewModel.getCart().observe(this, cart -> {
            Log.d("SearchActivity", "Cart updated: " + (cart != null ? cart.toString() : "null"));
            if (searchAdapter != null) {
                searchAdapter.notifyDataSetChanged();
            }
        });

        cartViewModel.getAllCartItems().observe(this, cartItems -> {
            Log.d("SearchActivity", "Cart items updated: " + (cartItems != null ? cartItems.size() : 0));
            if (searchAdapter != null) {
                searchAdapter.notifyDataSetChanged();
            }
        });
    }

    private void searchServices(String query) {
        // Показываем loading в адаптере
        searchAdapter.showLoading();
        hideAllViews();
        searchResultsRecyclerView.setVisibility(View.VISIBLE);

        // Поиск по всем подкатегориям во всех категориях
        searchInAllCategories(query);
    }

    private void searchInAllCategories(String query) {
        List<Service> allServices = new ArrayList<>();
        AtomicInteger completedRequests = new AtomicInteger(0);
        AtomicInteger totalRequests = new AtomicInteger(0);

        // Сначала получаем все категории
        db.collection("service_categories")
                .get()
                .addOnCompleteListener(categoriesTask -> {
                    if (categoriesTask.isSuccessful()) {
                        QuerySnapshot categoriesSnapshot = categoriesTask.getResult();
                        if (categoriesSnapshot.isEmpty()) {
                            displayResults(new ArrayList<>());
                            return;
                        }

                        // Для каждой категории получаем подкатегории
                        for (QueryDocumentSnapshot categoryDoc : categoriesSnapshot) {
                            String categoryId = categoryDoc.getId();
                            db.collection("service_categories")
                                    .document(categoryId)
                                    .collection("subcategories")
                                    .get()
                                    .addOnCompleteListener(subcategoriesTask -> {
                                        if (subcategoriesTask.isSuccessful()) {
                                            QuerySnapshot subcategoriesSnapshot = subcategoriesTask.getResult();
                                            // Увеличиваем счетчик общих запросов на количество подкатегорий
                                            int subcategoriesCount = subcategoriesSnapshot.size();
                                            totalRequests.addAndGet(subcategoriesCount);

                                            // Для каждой подкатегории ищем услуги
                                            for (QueryDocumentSnapshot subcategoryDoc : subcategoriesSnapshot) {
                                                String subcategoryId = subcategoryDoc.getId();
                                                searchInSubcategory(categoryId, subcategoryId, query, allServices,
                                                        completedRequests, totalRequests);
                                            }

                                            // Если нет подкатегорий, проверяем завершение
                                            if (subcategoriesCount == 0) {
                                                checkSearchCompletion(allServices, completedRequests, totalRequests);
                                            }
                                        } else {
                                            Log.e("SearchActivity", "Error getting subcategories for category: " + categoryId,
                                                    subcategoriesTask.getException());
                                            checkSearchCompletion(allServices, completedRequests, totalRequests);
                                        }
                                    });
                        }
                    } else {
                        Log.e("SearchActivity", "Error getting categories", categoriesTask.getException());
                        Toast.makeText(this, "Ошибка поиска", Toast.LENGTH_SHORT).show();
                        showNoResults();
                    }
                });
    }

    private void searchInSubcategory(String categoryId, String subcategoryId, String query,
                                     List<Service> allServices, AtomicInteger completedRequests,
                                     AtomicInteger totalRequests) {
        db.collection("service_categories")
                .document(categoryId)
                .collection("subcategories")
                .document(subcategoryId)
                .collection("services")
                .get()
                .addOnCompleteListener(servicesTask -> {
                    if (servicesTask.isSuccessful()) {
                        QuerySnapshot servicesSnapshot = servicesTask.getResult();
                        // Фильтруем услуги по запросу
                        for (QueryDocumentSnapshot serviceDoc : servicesSnapshot) {
                            Service service = serviceDoc.toObject(Service.class);
                            service.setId(serviceDoc.getId());
                            if (matchesQuery(service, query)) {
                                synchronized (allServices) {
                                    allServices.add(service);
                                }
                            }
                        }
                    } else {
                        Log.e("SearchActivity", "Error searching in subcategory: " + subcategoryId,
                                servicesTask.getException());
                    }

                    // Увеличиваем счетчик завершенных запросов
                    completedRequests.incrementAndGet();
                    checkSearchCompletion(allServices, completedRequests, totalRequests);
                });
    }

    private void checkSearchCompletion(List<Service> allServices, AtomicInteger completedRequests,
                                       AtomicInteger totalRequests) {
        // Проверяем, завершились ли все запросы
        if (completedRequests.get() >= totalRequests.get() && totalRequests.get() > 0) {
            // Сортируем результаты по релевантности (можно добавить свою логику)
            Collections.sort(allServices, (s1, s2) -> s1.getName().compareToIgnoreCase(s2.getName()));
            // Показываем результаты в UI потоке
            runOnUiThread(() -> displayResults(allServices));
        }
    }

    private boolean matchesQuery(Service service, String query) {
        String lowerQuery = query.toLowerCase();
        String serviceName = service.getName() != null ? service.getName().toLowerCase() : "";
        String serviceDescription = service.getDescription() != null ? service.getDescription().toLowerCase() : "";
        return serviceName.contains(lowerQuery) || serviceDescription.contains(lowerQuery);
    }

    private void displayResults(List<Service> services) {
        hideAllViews();
        if (services.isEmpty()) {
            showNoResults();
        } else {
            searchResultsRecyclerView.setVisibility(View.VISIBLE);
            searchAdapter.updateServices(services);
        }
    }

    private void showNoResults() {
        hideAllViews();
        bottomContainer.setVisibility(View.VISIBLE);
    }

    private void clearResults() {
        hideAllViews();
        searchAdapter.updateServices(new ArrayList<>());
    }

    private void hideAllViews() {
        searchResultsRecyclerView.setVisibility(View.GONE);
        bottomContainer.setVisibility(View.GONE);
    }

    private void showKeyboard() {
        searchInput.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    // Реализация интерфейса OnServiceActionListener
    // Реализация методов интерфейса OnServiceActionListener
    @Override
    public void onServiceSelected(Service service) {
        Log.d("SearchActivity", "onServiceSelected called for: " + service.getName());

        // Этот метод вызывается при клике на элемент списка (открытие BottomSheet)
        Cart currentCart = cartViewModel.getCart().getValue();
        if (currentCart == null || currentCart.getSelectedCarId() == null) {
            Log.d("SearchActivity", "Cart is null or no car selected");
            Toast.makeText(this, "Пожалуйста, сначала выберите автомобиль", Toast.LENGTH_SHORT).show();
            return;
        }

        if (getSupportFragmentManager().findFragmentByTag("ServiceDetails") == null) {
            Log.d("SearchActivity", "Opening ServiceDetailsBottomSheet");
            ServiceDetailsBottomSheet bottomSheet = ServiceDetailsBottomSheet.newInstance(service);
            bottomSheet.setOnServiceAddedListener(service1 -> {
                Toast.makeText(this, "Услуга добавлена: " + service1.getName(),
                        Toast.LENGTH_SHORT).show();
            });
            bottomSheet.show(getSupportFragmentManager(), "ServiceDetails");
        } else {
            Log.d("SearchActivity", "BottomSheet already open");
        }
    }
    @Override
    public void onServiceAction(Service service) {

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_top, R.anim.slide_out_bottom);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_top, R.anim.slide_out_bottom);
    }
}