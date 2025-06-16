package com.example.autofix.bonusCard;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.autofix.R;
import com.example.autofix.adapters.BonusHistoryAdapter;
import com.example.autofix.models.BonusHistoryItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class BonusCardHistoryActivity extends AppCompatActivity {

    private RecyclerView historyRecyclerView;
    private BonusHistoryAdapter historyAdapter;
    private List<BonusHistoryItem> historyItems;
    private ImageView btnBack;
    private TextView emptyStateText;

    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bonus_card_history);

        // Инициализация Firebase
        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        initViews();
        setupRecyclerView();
        loadHistory();

        btnBack.setOnClickListener(v -> finish());
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        historyRecyclerView = findViewById(R.id.history_recycler_view);
        emptyStateText = findViewById(R.id.empty_state_text);
    }

    private void setupRecyclerView() {
        historyItems = new ArrayList<>();
        historyAdapter = new BonusHistoryAdapter(historyItems);
        historyRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        historyRecyclerView.setAdapter(historyAdapter);
    }

    private void loadHistory() {
        if (userId == null) {
            Log.e("BonusHistory", "User not authenticated");
            return;
        }

        db.collection("users")
                .document(userId)
                .collection("bonus_card")
                .document("card_history")
                .collection("history")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    historyItems.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        BonusHistoryItem item = document.toObject(BonusHistoryItem.class);
                        historyItems.add(item);
                    }
                    historyAdapter.notifyDataSetChanged();

                    // Показываем/скрываем пустое состояние
                    if (historyItems.isEmpty()) {
                        emptyStateText.setVisibility(android.view.View.VISIBLE);
                        historyRecyclerView.setVisibility(android.view.View.GONE);
                    } else {
                        emptyStateText.setVisibility(android.view.View.GONE);
                        historyRecyclerView.setVisibility(android.view.View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("BonusHistory", "Error loading history", e);
                });
    }
}