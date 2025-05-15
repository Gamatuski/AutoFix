package com.example.autofix.sto;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.autofix.MainActivity;
import com.example.autofix.R;
import com.example.autofix.adapters.CartServiceAdapter;
import com.example.autofix.data.entities.Cart;
import com.example.autofix.data.entities.CartItem;
import com.example.autofix.viewmodels.CartViewModel;
import java.util.ArrayList;
import java.util.List;

public class CartServiceActivity extends AppCompatActivity
        implements CartServiceAdapter.OnItemDeleteListener {
    private CartViewModel cartViewModel;
    private CartServiceAdapter adapter;
    private TextView totalPriceText;
    private TextView carNameText;
    private TextView clearCartText;
    private Button proceedButton, addServiceButton;
    private String carId;
    private String carName;
    private ImageView cancelButton, backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart_service);

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

        // Инициализация views
        totalPriceText = findViewById(R.id.total_price_text);
        carNameText = findViewById(R.id.car_name_text);
        proceedButton = findViewById(R.id.proceed_to_booking_button);
        clearCartText = findViewById(R.id.Clear_cart_text);
        addServiceButton = findViewById(R.id.add_service_button);
        View emptyCartContainer = findViewById(R.id.empty_cart_container);


        // Добавляем обработчик нажатия для очистки корзины
        clearCartText.setOnClickListener(v -> {
            // Показываем диалог подтверждения перед очисткой корзины
            showClearCartConfirmationDialog();
        });

        addServiceButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ServiceSelectionActivity.class);
            startActivity(intent);
        });

        cancelButton = findViewById(R.id.cancel_button_top);
        cancelButton.setOnClickListener(v -> showCancelConfirmationDialog());

        backButton = findViewById(R.id.back_button_top);
        backButton.setOnClickListener(v -> onBackPressed());


        // Настройка RecyclerView
        RecyclerView recyclerView = findViewById(R.id.services_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CartServiceAdapter(new ArrayList<>(), this);
        recyclerView.setAdapter(adapter);

        // Настройка ItemTouchHelper для свайпа
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

// Создаем и прикрепляем ItemTouchHelper с нашим SwipeToDeleteCallback
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(adapter.getSwipeToDeleteCallback(vibrator));
        itemTouchHelper.attachToRecyclerView(recyclerView);

        // Наблюдаем за изменениями в корзине
        cartViewModel.getCart().observe(this, cart -> {
            if (cart != null) {
                if (cart.getSelectedCarName() != null) {
                    carNameText.setText(cart.getSelectedCarName());
                } else {
                    carNameText.setText("Автомобиль не выбран");
                }
                // Сохраняем ID и имя автомобиля
                carId = cart.getSelectedCarId();
                carName = cart.getSelectedCarName();
            }
        });


        cartViewModel.getTotalPrice().observe(this, total -> {
            if (total != null) {
                totalPriceText.setText(String.format("%d ₽", total));
            }
        });

        cartViewModel.getAllCartItems().observe(this, cartItems -> {
            if (cartItems != null && !cartItems.isEmpty()) {
                // Есть товары в корзине - показываем список
                emptyCartContainer.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                adapter.updateItems(cartItems);

                // Активируем кнопку "Далее"
                proceedButton.setEnabled(true);
            } else {
                // Корзина пуста - показываем пустое состояние
                emptyCartContainer.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
                adapter.updateItems(new ArrayList<>());

                // Деактивируем кнопку "Далее"
                proceedButton.setEnabled(false);
            }
        });

        // Обработка кнопки "Далее"
        proceedButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, BookingActivity.class);
            startActivity(intent);
        });
    }

    private void showClearCartConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Очистить корзину");
        builder.setMessage("Вы уверены, что хотите удалить все услуги из корзины?");
        builder.setPositiveButton("Да", (dialog, which) -> {
            // Очищаем корзину
            clearCart();
        });
        builder.setNegativeButton("Отмена", (dialog, which) -> {
            dialog.dismiss();
        });
        builder.show();
    }

    /**
     * Очищает корзину и обновляет UI
     */
    private void clearCart() {
        // Сохраняем ID и имя автомобиля перед очисткой
        String savedCarId = carId;
        String savedCarName = carName;

        // Очищаем корзину через ViewModel
        cartViewModel.clearCart();

        // Обновляем UI
        adapter.updateItems(new ArrayList<>());
        totalPriceText.setText("0 ₽");

        // Показываем сообщение об успешной очистке
        Toast.makeText(this, "Корзина очищена", Toast.LENGTH_SHORT).show();

        // Обновляем информацию о корзине, сохраняя выбранный автомобиль
        if (savedCarId != null && savedCarName != null) {
            Cart updatedCart = new Cart(savedCarId, savedCarName, 0, 0);
            cartViewModel.updateCart(updatedCart);
        }
    }



    @Override
    public void onItemDeleted(int position) {
        if (position < adapter.getCartItems().size()) {
            CartItem item = adapter.getCartItems().get(position);

            // Используем сохраненные ID и имя автомобиля вместо null
            cartViewModel.removeServiceFromCart(item, carId, carName);

            // Показываем сообщение об удалении
            Toast.makeText(this,
                    "Услуга \"" + item.getServiceName() + "\" удалена из корзины",
                    Toast.LENGTH_SHORT).show();

            // Получаем текущий список элементов
            List<CartItem> currentItems = new ArrayList<>(adapter.getCartItems());

            // Удаляем элемент из локального списка
            currentItems.remove(position);

            // Обновляем адаптер с новым списком
            adapter.updateItems(currentItems);

            // Обновляем общую стоимость
            int newTotal = 0;
            for (CartItem cartItem : currentItems) {
                newTotal += cartItem.getServicePrice();
            }

            // Если корзина пуста после удаления, обновляем UI соответствующим образом
            if (currentItems.isEmpty()) {
                totalPriceText.setText("0 ₽");
            }
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
        // Возвращаемся к выбору категории
        super.onBackPressed();

    }

}