package com.example.autofix.sto;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.autofix.MainActivity;
import com.example.autofix.R;

public class ConfirmBookingActivity extends AppCompatActivity {

    private TextView bookingDateText;
    private TextView bookingTimeText;
    private TextView stationAddressText;
    private TextView carNameText;
    private Button returnToMainButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_booking);

        initializeViews();
        setupClickListeners();
        displayBookingInfo();
    }

    private void initializeViews() {
        bookingDateText = findViewById(R.id.booking_date_text);
        bookingTimeText = findViewById(R.id.booking_time_text);
        stationAddressText = findViewById(R.id.station_address_text);
        carNameText = findViewById(R.id.car_name_text);
        returnToMainButton = findViewById(R.id.return_to_main_button);
    }

    private void setupClickListeners() {
        returnToMainButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void displayBookingInfo() {
        // Получаем данные из Intent
        String bookingDate = getIntent().getStringExtra("BOOKING_DATE");
        String bookingTime = getIntent().getStringExtra("BOOKING_TIME");
        String stationAddress = getIntent().getStringExtra("STATION_ADDRESS");
        String carName = getIntent().getStringExtra("CAR_NAME");

        // Устанавливаем данные в TextView
        if (bookingDate != null) {
            bookingDateText.setText(bookingDate);
        }
        if (bookingTime != null) {
            bookingTimeText.setText(bookingTime);
        }

        if (stationAddress != null) {
            stationAddressText.setText(stationAddress);
        }

        if (carName != null) {
            carNameText.setText(carName);
        }
    }

    @Override
    public void onBackPressed() {
        // При нажатии кнопки "Назад" также возвращаемся на главный экран
        super.onBackPressed();
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}