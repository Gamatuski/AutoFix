package com.example.autofix.adapters;

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

import java.util.ArrayList;
import java.util.List;

public class ServiceItemAdapter extends RecyclerView.Adapter<ServiceItemAdapter.ViewHolder>
        implements Filterable {

    public enum ItemType {
        CATEGORY,
        SUBCATEGORY
    }

    private final ItemType itemType;
    private List<?> items; // Может содержать ServiceCategory или ServiceSubcategory
    private List<?> filteredItems;
    private final OnItemClickListener listener;

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

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_service, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Object item = filteredItems.get(position);

        if (itemType == ItemType.CATEGORY && item instanceof ServiceCategory) {
            ServiceCategory category = (ServiceCategory) item;
            holder.bind(category.getName(), category.getImageUrl());
            holder.itemView.setOnClickListener(v -> listener.onCategoryClick(category));
        } else if (itemType == ItemType.SUBCATEGORY && item instanceof ServiceSubcategory) {
            ServiceSubcategory subcategory = (ServiceSubcategory) item;
            holder.bind(subcategory.getName(), subcategory.getImageUrl());
            holder.itemView.setOnClickListener(v -> listener.onSubcategoryClick(subcategory));
        }
    }

    @Override
    public int getItemCount() {
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

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView serviceImage;
        private final TextView serviceName;

        ViewHolder(View itemView) {
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