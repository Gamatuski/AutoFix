package com.example.autofix.adapters;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.autofix.R;
import com.example.autofix.models.ServiceCategory;
import com.example.autofix.models.ServiceSubcategory;
import com.facebook.shimmer.ShimmerFrameLayout;

import java.util.ArrayList;
import java.util.List;

public class ServiceItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
        implements Filterable {

    private static final int TYPE_LOADING = 0;
    private static final int TYPE_ITEM = 1;

    public enum ItemType {
        CATEGORY,
        SUBCATEGORY
    }

    private final ItemType itemType;
    private List<?> items; // Может содержать ServiceCategory или ServiceSubcategory
    private List<?> filteredItems;
    private final OnItemClickListener listener;
    private boolean isLoading = false;

    public interface OnItemClickListener {
        void onCategoryClick(ServiceCategory category);
        void onSubcategoryClick(ServiceSubcategory subcategory);
    }

    public ServiceItemAdapter(List<?> items, ItemType itemType, OnItemClickListener listener) {
        this.items = items;
        this.filteredItems = new ArrayList<>(items);
        this.itemType = itemType;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        if (isLoading && filteredItems.isEmpty()) {
            return TYPE_LOADING;
        }
        return TYPE_ITEM;
    }

    // Методы для управления состоянием загрузки
    public void showLoading() {
        isLoading = true;
        filteredItems = new ArrayList<>();
        notifyDataSetChanged();
    }

    public void hideLoading() {
        isLoading = false;
        notifyDataSetChanged();
    }

    // Метод для обновления данных с анимацией
    public void updateItems(List<?> newItems) {
        if (isLoading) {
            // Показываем skeleton минимум 2 секунды
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                isLoading = false;
                this.items = newItems;
                this.filteredItems = new ArrayList<>(newItems);
                notifyDataSetChanged();
            }, 1000);
        } else {
            this.items = newItems;
            this.filteredItems = new ArrayList<>(newItems);
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_LOADING) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_service_skeleton, parent, false);
            return new LoadingViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_service, parent, false);
            return new ItemViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof LoadingViewHolder) {
            // Skeleton элементы не требуют привязки данных
            return;
        }

        ItemViewHolder itemHolder = (ItemViewHolder) holder;
        Object item = filteredItems.get(position);

        if (itemType == ItemType.CATEGORY && item instanceof ServiceCategory) {
            ServiceCategory category = (ServiceCategory) item;
            itemHolder.bind(category.getName(), category.getImageUrl());
            itemHolder.itemView.setOnClickListener(v -> listener.onCategoryClick(category));
        } else if (itemType == ItemType.SUBCATEGORY && item instanceof ServiceSubcategory) {
            ServiceSubcategory subcategory = (ServiceSubcategory) item;
            itemHolder.bind(subcategory.getName(), subcategory.getImageUrl());
            itemHolder.itemView.setOnClickListener(v -> listener.onSubcategoryClick(subcategory));
        }
    }

    @Override
    public int getItemCount() {
        if (isLoading && filteredItems.isEmpty()) {
            return 6; // Показываем 4 skeleton элемента
        }
        return filteredItems.size();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<Object> filtered = new ArrayList<>();
                if (constraint == null || constraint.length() == 0) {
                    filtered.addAll(items);
                } else {
                    String pattern = constraint.toString().toLowerCase().trim();
                    for (Object item : items) {
                        String name = "";
                        if (itemType == ItemType.CATEGORY && item instanceof ServiceCategory) {
                            name = ((ServiceCategory) item).getName();
                        } else if (itemType == ItemType.SUBCATEGORY && item instanceof ServiceSubcategory) {
                            name = ((ServiceSubcategory) item).getName();
                        }
                        if (name.toLowerCase().contains(pattern)) {
                            filtered.add(item);
                        }
                    }
                }
                FilterResults results = new FilterResults();
                results.values = filtered;
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                filteredItems = (List<?>) results.values;
                notifyDataSetChanged();
            }
        };
    }

    // ViewHolder для skeleton загрузки
    static class LoadingViewHolder extends RecyclerView.ViewHolder {
        ShimmerFrameLayout shimmerLayout;

        public LoadingViewHolder(@NonNull View itemView) {
            super(itemView);
            shimmerLayout = itemView.findViewById(R.id.shimmer_layout);
        }
    }

    // ViewHolder для обычных элементов
    static class ItemViewHolder extends RecyclerView.ViewHolder {
        private final ImageView serviceImage;
        private final TextView serviceName;

        ItemViewHolder(View itemView) {
            super(itemView);
            serviceImage = itemView.findViewById(R.id.service_image);
            serviceName = itemView.findViewById(R.id.service_name);
        }

        void bind(String name, String imageUrl) {
            serviceName.setText(name);
            // Загрузка изображения с помощью Glide
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(imageUrl)
                        .centerCrop()
                        .placeholder(R.drawable.placeholder_image) // Замените на ваш placeholder
                        .into(serviceImage);
            } else {
                // Установка изображения по умолчанию, если URL отсутствует
                serviceImage.setImageResource(R.drawable.placeholder_image); // Замените на ваш placeholder
            }
        }
    }
}