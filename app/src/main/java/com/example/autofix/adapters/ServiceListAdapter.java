package com.example.autofix.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.autofix.R;
import com.example.autofix.data.entities.Cart;
import com.example.autofix.data.entities.CartItem;
import com.example.autofix.models.Service;
import com.example.autofix.viewmodels.CartViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServiceListAdapter extends RecyclerView.Adapter<ServiceListAdapter.ViewHolder>
        implements Filterable {
    private List<Service> services;
    private List<Service> filteredServices;
    private final OnServiceActionListener listener;
    private final CartViewModel cartViewModel;
    private final LifecycleOwner lifecycleOwner;
    private final Map<String, Boolean> serviceInCartMap = new HashMap<>();

    public interface OnServiceActionListener {
        void onServiceSelected(Service service);
    }

    public ServiceListAdapter(List<Service> services,
                              OnServiceActionListener listener,
                              CartViewModel cartViewModel,
                              LifecycleOwner lifecycleOwner) {
        this.services = services;
        this.filteredServices = new ArrayList<>(services);
        this.listener = listener;
        this.cartViewModel = cartViewModel;
        this.lifecycleOwner = lifecycleOwner;
        setupCartObserver();
    }

    private void setupCartObserver() {
        cartViewModel.getAllCartItems().observe(lifecycleOwner, cartItems -> {
            serviceInCartMap.clear();
            for (CartItem item : cartItems) {
                serviceInCartMap.put(item.getServiceId(), true);
            }
            notifyDataSetChanged();
        });
    }

    public void updateServices(List<Service> services) {
        this.services = services;
        this.filteredServices = new ArrayList<>(services);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_service_card, parent, false);
        return new ViewHolder(view, cartViewModel, lifecycleOwner);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Service service = filteredServices.get(position);
        boolean isInCart = serviceInCartMap.containsKey(service.getId());
        holder.bind(service, listener, isInCart);
    }

    @Override
    public int getItemCount() {
        return filteredServices.size();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<Service> filtered = new ArrayList<>();
                if (constraint == null || constraint.length() == 0) {
                    filtered.addAll(services);
                } else {
                    String pattern = constraint.toString().toLowerCase().trim();
                    for (Service service : services) {
                        if (service.getName().toLowerCase().contains(pattern) ||
                                (service.getDescription() != null &&
                                        service.getDescription().toLowerCase().contains(pattern))) {
                            filtered.add(service);
                        }
                    }
                }
                FilterResults results = new FilterResults();
                results.values = filtered;
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                filteredServices.clear();
                filteredServices.addAll((List<Service>) results.values);
                notifyDataSetChanged();
            }
        };
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView serviceImage;
        private final TextView serviceName;
        private final TextView servicePrice;
        private final ImageView actionButton;
        private final CartViewModel cartViewModel;
        private final LifecycleOwner lifecycleOwner;
        private boolean isInCart;
        private Cart currentCart;

        ViewHolder(View itemView, CartViewModel cartViewModel, LifecycleOwner lifecycleOwner) {
            super(itemView);
            this.cartViewModel = cartViewModel;
            this.lifecycleOwner = lifecycleOwner;
            serviceImage = itemView.findViewById(R.id.service_image);
            serviceName = itemView.findViewById(R.id.service_name);
            servicePrice = itemView.findViewById(R.id.service_price);
            actionButton = itemView.findViewById(R.id.add_button);

            // Получаем текущую корзину один раз и храним ее
            cartViewModel.getCart().observe(lifecycleOwner, cart -> {
                this.currentCart = cart;
            });
        }

        void bind(Service service, OnServiceActionListener listener, boolean isInCart) {
            this.isInCart = isInCart;
            serviceName.setText(service.getName());
            servicePrice.setText(String.format("%d ₽", service.getPrice()));
            Glide.with(itemView.getContext())
                    .load(service.getImageUrl())
                    .into(serviceImage);
            updateButtonState();

            // Обработчик клика с фиксированной логикой
            actionButton.setOnClickListener(v -> handleServiceAction(service));
            itemView.setOnClickListener(v -> listener.onServiceSelected(service));
        }

        private void updateButtonState() {
            if (isInCart) {
                actionButton.setImageResource(R.drawable.delete_ic);
                actionButton.setBackgroundResource(R.drawable.delete_button);
            } else {
                actionButton.setImageResource(R.drawable.plus_ic);
                actionButton.setBackgroundResource(R.drawable.rounded_button);
            }
        }

        private void handleServiceAction(Service service) {
            Cart cart = cartViewModel.getCart().getValue();

            if (cart == null) {
                Log.d("ServiceListAdapter", "Cart is null");
                Toast.makeText(itemView.getContext(), "Корзина не создана", Toast.LENGTH_SHORT).show();
                return;
            }

            if (cart.getSelectedCarId() == null || cart.getSelectedCarId().isEmpty()) {
                Log.d("ServiceListAdapter", "SelectedCarId: " + cart.getSelectedCarId());
                Log.d("ServiceListAdapter", "SelectedCarName: " + cart.getSelectedCarName());
                Toast.makeText(itemView.getContext(), "Сначала выберите автомобиль", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isInCart) {
                removeServiceFromCart(service, cart);
            } else {
                addServiceToCart(service, cart);
            }
        }

        private void addServiceToCart(Service service, Cart cart) {
            cartViewModel.addServiceToCart(
                    service,
                    cart.getSelectedCarId(),
                    cart.getSelectedCarName()
            );
        }

        private void removeServiceFromCart(Service service, Cart cart) {
            List<CartItem> cartItems = cartViewModel.getAllCartItems().getValue();
            if (cartItems != null) {
                for (CartItem item : cartItems) {
                    if (item.getServiceId().equals(service.getId())) {
                        cartViewModel.removeServiceFromCart(
                                item,
                                cart.getSelectedCarId(),
                                cart.getSelectedCarName()
                        );
                        break;
                    }
                }
            }
        }
    }
}