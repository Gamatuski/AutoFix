package com.example.autofix.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.example.autofix.LoginActivity;
import com.example.autofix.R;
import com.example.autofix.services.NotificationReminderService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private TextView nameTextView;
    private EditText nameEditText;
    private TextView phoneTextView;
    private ImageView checkIcon, cancelIcon;
    private boolean isEditMode = false;
    private String originalName;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Инициализация Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Находим элементы интерфейса
        nameTextView = view.findViewById(R.id.name_text);
        nameEditText = view.findViewById(R.id.name_edit_text);
        phoneTextView = view.findViewById(R.id.phone_text);
        TextView editProfileButton = view.findViewById(R.id.edit_profile_button);
        LinearLayout cityItem = view.findViewById(R.id.city_item);
        LinearLayout deleteProfileItem = view.findViewById(R.id.delete_profile_item);
        LinearLayout logoutItem = view.findViewById(R.id.logout_item);
        LinearLayout notificationsItem = view.findViewById(R.id.notifications_item);
        checkIcon = view.findViewById(R.id.check_icon);
        cancelIcon = view.findViewById(R.id.cancel_icon);

        // Загружаем данные пользователя
        loadUserData();

        // Обработчики кликов
        editProfileButton.setOnClickListener(v -> toggleEditMode());
        checkIcon.setOnClickListener(v -> saveProfile());
        cancelIcon.setOnClickListener(v -> cancelEdit());
        notificationsItem.setOnClickListener(v -> openNotificationSettings());
        logoutItem.setOnClickListener(v -> logout());
        deleteProfileItem.setOnClickListener(v -> showDeleteProfileDialog());


        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            showGuestView();
            return;
        }else {
            loadUserData();
        }

    }

    public void showGuestView() {
        // Устанавливаем текст "Гость" в поле имени
        nameTextView.setText("Гость");
        nameTextView.setVisibility(View.VISIBLE);

        // Скрываем поле редактирования имени
        nameEditText.setVisibility(View.GONE);

        // Очищаем поле телефона или устанавливаем текст для гостя
        phoneTextView.setText("Не авторизован");

        // Скрываем иконки редактирования
        checkIcon.setVisibility(View.GONE);
        cancelIcon.setVisibility(View.GONE);

        // Отключаем все интерактивные элементы
        View view = getView();
        if (view != null) {
            TextView editProfileButton = view.findViewById(R.id.edit_profile_button);
            LinearLayout cityItem = view.findViewById(R.id.city_item);
            LinearLayout deleteProfileItem = view.findViewById(R.id.delete_profile_item);
            LinearLayout logoutItem = view.findViewById(R.id.logout_item);
            LinearLayout notificationsItem = view.findViewById(R.id.notifications_item);

            // Отключаем кнопки
            if (editProfileButton != null) {
                editProfileButton.setEnabled(false);
                editProfileButton.setAlpha(0.5f);
                editProfileButton.setText("Войдите для редактирования");
            }

            if (cityItem != null) {
                cityItem.setEnabled(false);
                cityItem.setAlpha(0.5f);
            }

            if (deleteProfileItem != null) {
                deleteProfileItem.setEnabled(false);
                deleteProfileItem.setAlpha(0.5f);
            }

            if (notificationsItem != null) {
                notificationsItem.setEnabled(false);
                notificationsItem.setAlpha(0.5f);
            }

            // Оставляем кнопку выхода активной, но меняем её функционал
            if (logoutItem != null) {
                TextView logoutText = logoutItem.findViewById(android.R.id.text1);
                if (logoutText == null) {
                    // Если не нашли по ID, ищем TextView в LinearLayout
                    for (int i = 0; i < logoutItem.getChildCount(); i++) {
                        View child = logoutItem.getChildAt(i);
                        if (child instanceof TextView) {
                            ((TextView) child).setText("Войти в приложение");
                            break;
                        }
                    }
                }

                // Переопределяем обработчик клика для перехода к авторизации
                logoutItem.setOnClickListener(v -> {
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    startActivity(intent);
                });
            }
        }

        // Сбрасываем режим редактирования
        isEditMode = false;
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
                        nameEditText.setText(fullName);
                        originalName = fullName;

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

    private void toggleEditMode() {
        if (!isEditMode) {
            // Переходим в режим редактирования
            nameTextView.setVisibility(View.GONE);
            nameEditText.setVisibility(View.VISIBLE);
            checkIcon.setVisibility(View.VISIBLE);
            cancelIcon.setVisibility(View.VISIBLE);

            // Устанавливаем фокус на EditText
            nameEditText.requestFocus();
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(nameEditText, InputMethodManager.SHOW_IMPLICIT);

            isEditMode = true;
        }
    }

    private void saveProfile() {
        String newName = nameEditText.getText().toString().trim();
        if (newName.isEmpty()) {
            Toast.makeText(getContext(), "Имя не может быть пустым", Toast.LENGTH_SHORT).show();
            return;
        }

        // Парсим имя (предполагаем формат: "Имя Отчество\nФамилия")
        String[] nameParts = newName.split("\n");
        if (nameParts.length != 2) {
            Toast.makeText(getContext(), "Неверный формат имени", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] firstParts = nameParts[0].split(" ");
        String lastName = nameParts[1];

        if (firstParts.length < 2) {
            Toast.makeText(getContext(), "Введите имя и отчество", Toast.LENGTH_SHORT).show();
            return;
        }

        String firstName = firstParts[0];
        String middleName = firstParts[1];

        // Сохраняем в Firestore
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("firstName", firstName);
            updates.put("middleName", middleName);
            updates.put("lastName", lastName);

            db.collection("users").document(currentUser.getUid())
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        nameTextView.setText(newName);
                        originalName = newName;
                        exitEditMode();
                        Toast.makeText(getContext(), "Профиль обновлен", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Ошибка сохранения", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void cancelEdit() {
        nameEditText.setText(originalName);
        exitEditMode();
    }

    private void exitEditMode() {
        nameTextView.setVisibility(View.VISIBLE);
        nameEditText.setVisibility(View.GONE);
        checkIcon.setVisibility(View.GONE);
        cancelIcon.setVisibility(View.GONE);

        // Скрываем клавиатуру
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(nameEditText.getWindowToken(), 0);

        isEditMode = false;
    }

    private void openNotificationSettings() {
        try {
            Intent intent = new Intent();

            intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().getPackageName());

            startActivity(intent);
        } catch (Exception e) {
            // Если не удалось открыть настройки приложения, открываем общие настройки уведомлений
            try {
                Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
                startActivity(intent);
            } catch (Exception ex) {
                Toast.makeText(getContext(), "Не удалось открыть настройки", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showDeleteProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Удаление профиля");
        builder.setMessage("Вы уверены, что хотите удалить свой профиль? Это действие нельзя отменить.");
        builder.setPositiveButton("Удалить", (dialog, which) -> deleteProfile());
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private void deleteProfile() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            // Удаляем данные из Firestore
            db.collection("users").document(currentUser.getUid())
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        // Удаляем аккаунт пользователя
                        currentUser.delete()
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(getContext(), "Профиль удален", Toast.LENGTH_SHORT).show();
                                        // Переход на экран входа
                                        Intent intent = new Intent(getActivity(), LoginActivity.class );
                                        startActivity(intent);
                                    } else {
                                        Toast.makeText(getContext(), "Ошибка удаления профиля", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Ошибка удаления данных", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void logout() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Выход");
        builder.setMessage("Вы уверены, что хотите выйти из приложения?");
        builder.setPositiveButton("Выйти", (dialog, which) -> {
            auth.signOut();
            Toast.makeText(getContext(), "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getActivity(), LoginActivity.class );
            startActivity(intent);

            // Переход на экран входа
        });
        builder.setNegativeButton("Отмена", null);
        builder.show();
        NotificationReminderService.stopService(getActivity());
    }
}