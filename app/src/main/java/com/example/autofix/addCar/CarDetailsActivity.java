package com.example.autofix.addCar;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;


import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.autofix.MainActivity;
import com.example.autofix.R;
import com.example.autofix.adapters.AppointmentsAdapter;
import com.example.autofix.models.Appointment;
import com.example.autofix.models.Car;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CarDetailsActivity extends AppCompatActivity {

    private RecyclerView appointmentsRecyclerView;
    private AppointmentsAdapter appointmentsAdapter;
    private List<Appointment> allAppointments = new ArrayList<>();
    private List<Appointment> filteredAppointments = new ArrayList<>();

    private ImageView carImageView, editIcon, cancelButton;
    private TextView brandTextView, modelTextView, generationTextView;

    private Button saveButton, deleteButton;

    private Car currentCar;
    private String carId;
    private boolean isEditMode = false;

    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri selectedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_details);

        initViews();


        // Получаем ID автомобиля из Intent
        carId = getIntent().getStringExtra("car_id");
        if (carId != null) {
            loadCarDetails();
        } else {
            finish();
        }
    }

    private void initViews() {
        carImageView = findViewById(R.id.iv_car_image);
        editIcon = findViewById(R.id.iv_edit_icon);
        brandTextView = findViewById(R.id.tv_brand);
        modelTextView = findViewById(R.id.tv_model);
        generationTextView = findViewById(R.id.tv_generation);
        deleteButton = findViewById(R.id.btn_delete);
        saveButton = findViewById(R.id.btn_save);

        // Настройка слушателей
        editIcon.setOnClickListener(v -> toggleEditMode());
        carImageView.setOnClickListener(v -> {
            if (isEditMode) {
                selectImage();
            }
        });
        saveButton.setOnClickListener(v -> saveChanges());

        cancelButton = findViewById(R.id.back_button_top);
        cancelButton.setOnClickListener(l -> {
            // Возврат на главный экран
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        deleteButton.setOnClickListener(v -> showDeleteConfirmationDialog());

        appointmentsRecyclerView = findViewById(R.id.rv_appointments);
        appointmentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        appointmentsAdapter = new AppointmentsAdapter(this, filteredAppointments, appointment -> {
            // Обработка клика по записи
        });
        appointmentsRecyclerView.setAdapter(appointmentsAdapter);

        // Изначально поля недоступны для редактирования
        setEditMode(false);
    }
    private void loadAppointmentsForCar() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String currentCarName = currentCar.getBrand() + " " + currentCar.getModel() + " " + currentCar.getGeneration();

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .collection("appointments")
                .whereEqualTo("carName", currentCarName)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Appointment> appointmentsList = new ArrayList<>();
                        Log.d("HomeFragment", "Found " + task.getResult().size() + " appointments");

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                Appointment appointment = new Appointment();
                                appointment.setId(document.getId());

                                if (document.contains("stationId"))
                                    appointment.setStationId(document.getString("stationId"));
                                if (document.contains("stationAddress"))
                                    appointment.setStationAddress(document.getString("stationAddress"));
                                if (document.contains("bookingDate"))
                                    appointment.setBookingDate(document.getString("bookingDate"));
                                if (document.contains("bookingTime"))
                                    appointment.setBookingTime(document.getString("bookingTime"));
                                if (document.contains("carId"))
                                    appointment.setCarId(document.getString("carId"));
                                if (document.contains("carName"))
                                    appointment.setCarName(document.getString("carName"));
                                if (document.contains("totalPrice"))
                                    appointment.setTotalPrice(document.getLong("totalPrice").intValue());
                                if (document.contains("status"))
                                    appointment.setStatus(document.getString("status"));
                                if (document.contains("createdAt"))
                                    appointment.setCreatedAt(document.getDate("createdAt"));
                                if (document.contains("isQuickBooking"))
                                    appointment.setQuickBooking(Boolean.TRUE.equals(document.getBoolean("isQuickBooking")));

                                appointmentsList.add(appointment);
                                Log.d("HomeFragment", "Added appointment: " + appointment.getId());

                            } catch (Exception e) {
                                Log.e("HomeFragment", "Error parsing appointment: " + e.getMessage());
                            }
                        }

                        // Сортируем на клиенте
                        appointmentsList.sort((a1, a2) -> {
                            if (a1.getCreatedAt() == null && a2.getCreatedAt() == null) return 0;
                            if (a1.getCreatedAt() == null) return 1;
                            if (a2.getCreatedAt() == null) return -1;
                            return a2.getCreatedAt().compareTo(a1.getCreatedAt());
                        });

                        // ВАЖНО: Обновляем адаптер с полученными данными
                        appointmentsAdapter.updateAppointments(appointmentsList);

                    } else {
                        Log.e("HomeFragment", "Error getting appointments", task.getException());
                        // Показываем пустой список в случае ошибки
                        appointmentsAdapter.updateAppointments(new ArrayList<>());
                    }
                });
    }


    private void loadCarDetails() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        appointmentsAdapter.showLoading();
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .collection("cars")
                .document(carId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        currentCar = new Car();
                        currentCar.setId(document.getId());

                        if (document.contains("make"))
                            currentCar.setBrand(document.getString("make"));
                        if (document.contains("model"))
                            currentCar.setModel(document.getString("model"));
                        if (document.contains("generation"))
                            currentCar.setGeneration(document.getString("generation"));

                        if (document.contains("image"))
                            currentCar.setImageUrl(document.getString("image"));

                        loadAppointmentsForCar();
                        displayCarDetails();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void displayCarDetails() {
        if (currentCar == null) return;

        brandTextView.setText(currentCar.getBrand() != null ? currentCar.getBrand() : "Не указано");
        modelTextView.setText(currentCar.getModel() != null ? currentCar.getModel() : "Не указано");
        generationTextView.setText(currentCar.getGeneration() != null ? currentCar.getGeneration() : "Не указано");



        // Загрузка изображения
        if (currentCar.getImageUrl() != null && !currentCar.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(currentCar.getImageUrl())
                    .placeholder(R.drawable.ic_car_placeholder)
                    .error(R.drawable.ic_car_placeholder)
                    .into(carImageView);
        } else {
            carImageView.setImageResource(R.drawable.ic_car_placeholder);
        }
    }

    private void toggleEditMode() {
        isEditMode = !isEditMode;
        setEditMode(isEditMode);
    }

    private void setEditMode(boolean editMode) {
        isEditMode = editMode;

        saveButton.setVisibility(editMode ? View.VISIBLE : View.GONE);

        // Изменяем иконку
        editIcon.setImageResource(editMode ? R.drawable.close_ic : R.drawable.ic_edit);

        // Показываем подсказку для изображения
        if (editMode) {
            Toast.makeText(this, "Нажмите на изображение для его изменения", Toast.LENGTH_SHORT).show();
        }
    }

    private void selectImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Выберите изображение"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();

            // Показываем выбранное изображение
            Glide.with(this)
                    .load(selectedImageUri)
                    .placeholder(R.drawable.ic_car_placeholder)
                    .into(carImageView);
        }
    }

    private void saveChanges() {

        // Показываем прогресс
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Сохранение...");
        progressDialog.show();

        // Если выбрано новое изображение, конвертируем его в Base64
        if (selectedImageUri != null) {
            convertImageToBase64AndSave( progressDialog);
        } else {
            saveToFirestore( currentCar.getImageUrl(), progressDialog);
        }
    }

    private void convertImageToBase64AndSave(ProgressDialog progressDialog) {
        try {
            // Получаем InputStream из URI
            InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);

            // Конвертируем в Bitmap
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            // Сжимаем изображение для экономии места
            Bitmap compressedBitmap = compressBitmap(bitmap, 800, 600);

            // Конвертируем в Base64
            String base64Image = bitmapToBase64(compressedBitmap);

            // Сохраняем в Firestore
            saveToFirestore(base64Image, progressDialog);

        } catch (Exception e) {
            progressDialog.dismiss();
            Toast.makeText(this, "Ошибка обработки изображения", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private Bitmap compressBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        // Вычисляем коэффициент масштабирования
        float scaleWidth = ((float) maxWidth) / width;
        float scaleHeight = ((float) maxHeight) / height;
        float scale = Math.min(scaleWidth, scaleHeight);

        // Если изображение уже меньше максимальных размеров, возвращаем как есть
        if (scale >= 1.0f) {
            return bitmap;
        }

        // Создаем новый Bitmap с уменьшенными размерами
        int newWidth = Math.round(width * scale);
        int newHeight = Math.round(height * scale);

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        // Сжимаем в JPEG с качеством 80%
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);

        byte[] byteArray = byteArrayOutputStream.toByteArray();

        // Добавляем префикс для data URL
        return "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.DEFAULT);
    }



    private void saveToFirestore( String imageData, ProgressDialog progressDialog) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Map<String, Object> updates = new HashMap<>();
        if (imageData != null) {
            updates.put("image", imageData); // Сохраняем Base64 строку в поле image
        }
        updates.put("updatedAt", new Date());

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .collection("cars")
                .document(carId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Изменения сохранены", Toast.LENGTH_SHORT).show();

                    // Обновляем текущий объект
                    if (imageData != null) {
                        currentCar.setImageUrl(imageData);
                    }

                    // Выходим из режима редактирования
                    setEditMode(false);
                    selectedImageUri = null;
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Ошибка сохранения", Toast.LENGTH_SHORT).show();
                });
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Удаление автомобиля")
                .setMessage("Вы уверены, что хотите удалить этот автомобиль? Это действие нельзя отменить.")
                .setPositiveButton("Удалить", (dialog, which) -> deleteCar())
                .setNegativeButton("Отмена", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void deleteCar() {
        // Показываем прогресс
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Удаление автомобиля...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .collection("cars")
                .document(carId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Автомобиль успешно удален", Toast.LENGTH_SHORT).show();

                    // Возвращаемся на главный экран
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Ошибка при удалении автомобиля: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("CarDetailsActivity", "Error deleting car", e);
                });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (isEditMode) {
            // Если в режиме редактирования, спрашиваем о сохранении
            new AlertDialog.Builder(this)
                    .setTitle("Несохраненные изменения")
                    .setMessage("У вас есть несохраненные изменения. Сохранить их?")
                    .setPositiveButton("Сохранить", (dialog, which) -> saveChanges())
                    .setNegativeButton("Отменить", (dialog, which) -> {
                        setEditMode(false);
                        displayCarDetails(); // Восстанавливаем исходные данные
                        selectedImageUri = null;
                    })
                    .setNeutralButton("Выйти без сохранения", (dialog, which) -> super.onBackPressed())
                    .show();
        } else {
            super.onBackPressed();
        }
    }
}