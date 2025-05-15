package com.example.autofix;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.example.autofix.fragments.GarageFragment;
import com.example.autofix.fragments.HomeFragment;
import com.example.autofix.fragments.MapFragment;
import com.example.autofix.fragments.ProfileFragment;
import com.example.autofix.sto.CartServiceActivity;
import com.example.autofix.viewmodels.CartViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    private BottomNavigationView bottomNav;
    private CardView cartCard;
    private TextView cartCarName;
    private ImageView cartArrow, cartDelete;
    private CartViewModel cartViewModel;
    private int previousMenuItemId = R.id.nav_home;

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
}