package com.example.autofix.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.autofix.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CarListAdapter extends RecyclerView.Adapter<CarListAdapter.ViewHolder> {

    private List<String> items;
    private final OnItemClickListener listener;
    private Set<String> filteredItems = new HashSet<>();

    public interface OnItemClickListener {
        void onItemClick(String item);
    }

    public CarListAdapter(List<String> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    public void updateItems(List<String> newItems) {
        this.items = newItems;
        filteredItems.clear();
        notifyDataSetChanged();
    }

    // Добавляем фильтрацию
    public void filter(String query) {
        filteredItems.clear();
        if (query.isEmpty()) {
            notifyDataSetChanged();
            return;
        }

        String lowerCaseQuery = query.toLowerCase();
        for (String item : items) {
            if (item.toLowerCase().contains(lowerCaseQuery)) {
                filteredItems.add(item);
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_car_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String item = filteredItems.isEmpty() ?
                items.get(position) :
                new ArrayList<>(filteredItems).get(position);

        holder.textView.setText(item);
        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.item_text);
        }
    }
}