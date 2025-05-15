package com.example.autofix.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.example.autofix.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private TextView nameTextView;
    private TextView phoneTextView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Инициализация Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Находим элементы интерфейса
        nameTextView = view.findViewById(R.id.name_text);
        phoneTextView = view.findViewById(R.id.phone_text);
        TextView editProfileButton = view.findViewById(R.id.edit_profile_button);
        ImageView notificationsSwitch = view.findViewById(R.id.notifications_switch);
        LinearLayout cityItem = view.findViewById(R.id.city_item);
        LinearLayout deleteProfileItem = view.findViewById(R.id.delete_profile_item);
        LinearLayout logoutItem = view.findViewById(R.id.logout_item);

        // Загружаем данные пользователя
        loadUserData();

        // Обработчики кликов
        editProfileButton.setOnClickListener(v -> editProfile());
        logoutItem.setOnClickListener(v -> logout());

        return view;
    }

    private void loadUserData() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            DocumentReference userRef = db.collection("users").document(currentUser.getUid());
            userRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        // Получаем данные из Firestore
                        String firstName = document.getString("firstName");
                        String lastName = document.getString("lastName");
                        String middleName = document.getString("middleName");
                        String phone = document.getString("phone");
                        String email = document.getString("email");

                        // Форматируем полное имя
                        String fullName = firstName + " " + middleName + "\n" + lastName;
                        nameTextView.setText(fullName);

                        // Устанавливаем телефон (или email, если телефона нет)
                        String contactInfo = phone != null ? phone : email;
                        phoneTextView.setText(contactInfo);
                    }
                } else {
                    Toast.makeText(getContext(), "Ошибка загрузки данных", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void editProfile() {
        // Реализация редактирования профиля
        Toast.makeText(getContext(), "Редактирование профиля", Toast.LENGTH_SHORT).show();
    }

    private void logout() {
        auth.signOut();
        // Здесь можно добавить переход на экран входа
        Toast.makeText(getContext(), "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show();
    }
}