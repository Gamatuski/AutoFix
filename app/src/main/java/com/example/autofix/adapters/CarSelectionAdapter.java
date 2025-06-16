package com.example.autofix.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.autofix.R;
import com.example.autofix.models.Car;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class CarSelectionAdapter extends RecyclerView.Adapter<CarSelectionAdapter.CarViewHolder> {
    private List<Car> cars;
    private int selectedPosition = -1;
    private OnCarSelectedListener listener;

    public interface OnCarSelectedListener {
        void onCarSelected(Car car);
    }

    public CarSelectionAdapter(List<Car> cars, OnCarSelectedListener listener) {
        this.cars = cars;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_car_selection, parent, false);
        return new CarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CarViewHolder holder, int position) {
        Car car = cars.get(position);
        holder.bind(car, position == selectedPosition);
    }

    @Override
    public int getItemCount() {
        return cars.size();
    }

    public void updateCars(List<Car> newCars) {
        this.cars = newCars;
        selectedPosition = -1;
        notifyDataSetChanged();
    }

    public Car getSelectedCar() {
        if (selectedPosition != -1 && selectedPosition < cars.size()) {
            return cars.get(selectedPosition);
        }
        return null;
    }

    class CarViewHolder extends RecyclerView.ViewHolder {
        private TextView brandText, modelText, generationText;
        private MaterialCardView cardView;

        public CarViewHolder(@NonNull View itemView) {
            super(itemView);
            brandText = itemView.findViewById(R.id.brand_text);
            modelText = itemView.findViewById(R.id.model_text);
            generationText = itemView.findViewById(R.id.generation_text);
            cardView = itemView.findViewById(R.id.car_card);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    int previousSelected = selectedPosition;
                    selectedPosition = position;

                    // Обновляем только измененные элементы
                    if (previousSelected != -1) {
                        notifyItemChanged(previousSelected);
                    }
                    notifyItemChanged(selectedPosition);

                    if (listener != null) {
                        listener.onCarSelected(cars.get(position));
                    }
                }
            });
        }

        public void bind(Car car, boolean isSelected) {
            brandText.setText(car.getBrand());
            modelText.setText(car.getModel());
            generationText.setText(car.getGeneration());

            // Применяем стиль в зависимости от выбора
            if (isSelected) {
                cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.selected_car_background));
                cardView.setStrokeColor(ContextCompat.getColor(itemView.getContext(), R.color.selected_car_stroke));
                cardView.setStrokeWidth(4);
            } else {
                cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.default_car_background));
                cardView.setStrokeColor(ContextCompat.getColor(itemView.getContext(), R.color.default_car_stroke));
                cardView.setStrokeWidth(1);
            }
        }
    }
}