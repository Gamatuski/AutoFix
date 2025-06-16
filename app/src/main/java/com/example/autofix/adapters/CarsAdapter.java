package com.example.autofix.adapters;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.autofix.R;
import com.example.autofix.addCar.CarDetailsActivity;
import com.example.autofix.models.Appointment;
import com.example.autofix.models.Car;
import com.facebook.shimmer.ShimmerFrameLayout;

import java.util.ArrayList;
import java.util.List;

public class CarsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_LOADING = 0;
    private static final int TYPE_CAR = 1;

    private List<Car> carsList;
    private Context context;
    private OnItemClickListener listener;
    private boolean isLoading = false;

    public interface OnItemClickListener {
        void onCarClick(Car car);
    }

    public CarsAdapter(List<Car> carsList, Context context, OnItemClickListener listener) {
        this.carsList = carsList != null ? carsList : new ArrayList<>();
        this.context = context;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        if (isLoading && carsList.isEmpty()) {
            return TYPE_LOADING;
        }
        return TYPE_CAR;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_LOADING) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_car_skeleton, parent, false);
            return new LoadingViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_car, parent, false);
            return new CarViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof CarViewHolder) {
            Car car = carsList.get(position);
            CarViewHolder carHolder = (CarViewHolder) holder;

            carHolder.brandTextView.setText(car.getBrand());
            carHolder.modelTextView.setText(car.getModel());
            carHolder.generationTextView.setText(car.getGeneration());

            // Загрузка изображения
            if (car.getImageUrl() != null && !car.getImageUrl().isEmpty()) {
                Glide.with(context)
                        .load(car.getImageUrl())
                        .placeholder(R.drawable.ic_car_placeholder)
                        .error(R.drawable.ic_car_placeholder)
                        .into(carHolder.carImageView);
            } else {
                carHolder.carImageView.setImageResource(R.drawable.ic_car_placeholder);
            }

            carHolder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCarClick(car);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        if (isLoading && carsList.isEmpty()) {
            return 3; // показываем 3 skeleton элемента
        }
        return carsList.size();
    }

    public void showLoading() {
        isLoading = true;
        notifyDataSetChanged();
    }

    public void hideLoading() {
        isLoading = false;
        notifyDataSetChanged();
    }

    public void updateCars(List<Car> newCars) {
        if (isLoading) {
            // Показываем skeleton минимум 2 секунды
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                isLoading = false;
                this.carsList = newCars;
                notifyDataSetChanged();
            }, 2000);
        } else {
            this.carsList = newCars;
            notifyDataSetChanged();
        }
    }

    // --- ViewHolder для обычного элемента ---
    public static class CarViewHolder extends RecyclerView.ViewHolder {
        ImageView carImageView;
        TextView brandTextView, modelTextView, generationTextView;

        public CarViewHolder(@NonNull View itemView) {
            super(itemView);
            carImageView = itemView.findViewById(R.id.iv_car_image);
            brandTextView = itemView.findViewById(R.id.tv_car_brand);
            modelTextView = itemView.findViewById(R.id.tv_car_model);
            generationTextView = itemView.findViewById(R.id.tv_car_generation);
        }
    }

    // --- ViewHolder для skeleton ---
    static class LoadingViewHolder extends RecyclerView.ViewHolder {
        ShimmerFrameLayout shimmerLayout;

        public LoadingViewHolder(@NonNull View itemView) {
            super(itemView);
            shimmerLayout = itemView.findViewById(R.id.shimmer_layout);
            shimmerLayout.startShimmer();
        }
    }
}