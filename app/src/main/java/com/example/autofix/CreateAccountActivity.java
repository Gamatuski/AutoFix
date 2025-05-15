package com.example.autofix;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.autofix.adapters.DatePickerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CreateAccountActivity extends AppCompatActivity {
    private EditText etLastName, etFirstName, etMiddleName, etEmail, etBirthDate, etPassword, etConfirmPassword;
    private RadioGroup rgGender;
    private ProgressBar progressBar;
    private Button btnContinue;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etLastName = findViewById(R.id.etLastName);
        etFirstName = findViewById(R.id.etFirstName);
        etMiddleName = findViewById(R.id.etMiddleName);
        etEmail = findViewById(R.id.etEmail);
        etBirthDate = findViewById(R.id.etBirthDate);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        rgGender = findViewById(R.id.rgGender);
        btnContinue = findViewById(R.id.btnContinue);
        progressBar = findViewById(R.id.progress_bar);

        etBirthDate.setOnClickListener(v -> showDatePickerBottomSheet());
        btnContinue.setOnClickListener(v -> createAccount());
    }

    private void createAccount() {
        changeInProgress(true);
        String lastName = etLastName.getText().toString().trim();
        String firstName = etFirstName.getText().toString().trim();
        String middleName = etMiddleName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String birthDate = etBirthDate.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        int selectedGenderId = rgGender.getCheckedRadioButtonId();
        RadioButton selectedGender = findViewById(selectedGenderId);
        String gender = selectedGender != null ? selectedGender.getText().toString() : "";

        if (!validateData(lastName, firstName, email, birthDate, password, confirmPassword)) {
            changeInProgress(false);
            return;
        }

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String uid = Objects.requireNonNull(auth.getCurrentUser()).getUid();

                        // Создаем объект пользователя
                        Map<String, Object> user = new HashMap<>();
                        user.put("lastName", lastName);
                        user.put("firstName", firstName);
                        user.put("middleName", middleName);
                        user.put("email", email);
                        user.put("birthDate", birthDate);
                        user.put("gender", gender);
                        user.put("createdAt", FieldValue.serverTimestamp());
                        user.put("totalSpent", 0); // Добавляем общую сумму трат

                        // Создаем объект бонусной карты
                        Map<String, Object> bonusCard = new HashMap<>();
                        bonusCard.put("points", 500); // Начальный баланс 500 баллов
                        bonusCard.put("status", "Серебро"); // Начальный статус
                        bonusCard.put("discount", 0); // Скидка для серебра
                        bonusCard.put("cashback", 4); // Кешбек для серебра

                        // Сохраняем пользователя
                        db.collection("users").document(uid)
                                .set(user)
                                .addOnSuccessListener(aVoid -> {
                                    // Создаем подколлекцию bonus_card внутри документа пользователя
                                    db.collection("users").document(uid)
                                            .collection("bonus_card")
                                            .document("card_info") // Документ с информацией о карте
                                            .set(bonusCard)
                                            .addOnSuccessListener(aVoid1 -> {
                                                Toast.makeText(CreateAccountActivity.this, "Регистрация успешна!", Toast.LENGTH_SHORT).show();
                                                startActivity(new Intent(CreateAccountActivity.this, MainActivity.class));
                                                finish();
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(CreateAccountActivity.this, "Ошибка создания бонусной карты: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                Log.e("Firestore", "Ошибка создания бонусной карты", e);
                                                changeInProgress(false);
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    changeInProgress(false);
                                    Toast.makeText(CreateAccountActivity.this, "Ошибка сохранения данных: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    Log.e("Firestore", "Ошибка сохранения данных пользователя", e);
                                });
                    } else {
                        changeInProgress(false);
                        Toast.makeText(CreateAccountActivity.this, "Ошибка регистрации: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showDatePickerBottomSheet() {
        DatePickerAdapter datePickerAdapter = new DatePickerAdapter();
        datePickerAdapter.setOnDateSelectedListener(selectedDate -> {
            etBirthDate.setText(selectedDate);
        });
        datePickerAdapter.show(getSupportFragmentManager(), "DatePickerBottomSheet");
    }

    void changeInProgress(boolean inProgress) {
        if (inProgress) {
            progressBar.setVisibility(View.VISIBLE);
            btnContinue.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
            btnContinue.setVisibility(View.VISIBLE);
        }
    }

    boolean validateData(String lastName, String firstName, String email, String birthDate, String password, String confirmPassword) {
        if (TextUtils.isEmpty(lastName)) {
            etLastName.setError("Введите фамилию");
            return false;
        }
        if (TextUtils.isEmpty(firstName)) {
            etFirstName.setError("Введите имя");
            return false;
        }
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Введите email");
            return false;
        }
        if (TextUtils.isEmpty(birthDate)) {
            etBirthDate.setError("Укажите дату рождения");
            return false;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Введите пароль");
            return false;
        }
        if (password.length() < 6) {
            etPassword.setError("Пароль должен быть больше 6 символов");
            return false;
        }
        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Пароли не совпадают");
            return false;
        }
        return true;
    }
}