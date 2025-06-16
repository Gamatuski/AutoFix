package com.example.autofix.bonusCard;


import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.autofix.R;

public class BonusCardInfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bonus_card_info);

        // Кнопка назад
        ImageView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        CardView silverCard = findViewById(R.id.silver_card);
        CardView goldCard = findViewById(R.id.gold_card);
        CardView rubyCard = findViewById(R.id.ruby_card);


    }


}