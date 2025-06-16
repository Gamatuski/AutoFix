package com.example.autofix.adapters;

import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
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
import com.facebook.shimmer.ShimmerFrameLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServiceListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
        implements Filterable {

    private static final int TYPE_LOADING = 0;
    private static final int TYPE_SERVICE = 1;

    private List<Service> services;
    private List<Service> filteredServices;
    private final OnServiceActionListener listener;
    private final CartViewModel cartViewModel;
    private final LifecycleOwner lifecycleOwner;
    private final Map<String, Boolean> serviceInCartMap = new HashMap<>();
    private boolean isLoading = false;

    public interface OnServiceActionListener {
        void onServiceSelected(Service service);
        void onServiceAction(Service service); // Добавляем этот метод
    }

    public ServiceListAdapter(List<Service> services,
                              OnServiceActionListener listener,
                              CartViewModel cartViewModel,
                              LifecycleOwner lifecycleOwner) {
        Log.d("ServiceListAdapter", "Constructor called");
        Log.d("ServiceListAdapter", "Services count: " + (services != null ? services.size() : 0));
        Log.d("ServiceListAdapter", "Listener: " + (listener != null ? "not null" : "null"));
        Log.d("ServiceListAdapter", "CartViewModel: " + (cartViewModel != null ? "not null" : "null"));

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

    @Override
    public int getItemViewType(int position) {
        if (isLoading && filteredServices.isEmpty()) {
            return TYPE_LOADING;
        }
        return TYPE_SERVICE;
    }

    public void updateServices(List<Service> services) {
        if (isLoading) {
            // Показываем skeleton минимум 2 секунды
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                isLoading = false;
                this.services = services;
                this.filteredServices = new ArrayList<>(services);
                notifyDataSetChanged();
            }, 1000);
        } else {
            this.services = services;
            this.filteredServices = new ArrayList<>(services);
            notifyDataSetChanged();
        }
    }

    // Методы для управления состоянием загрузки
    public void showLoading() {
        isLoading = true;
        filteredServices.clear();
        notifyDataSetChanged();
    }

    public void hideLoading() {
        isLoading = false;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_LOADING) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_service_cart_skeleton, parent, false);
            return new LoadingViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_service_card, parent, false);
            return new ServiceViewHolder(view, cartViewModel, lifecycleOwner);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Log.d("ServiceListAdapter", "onBindViewHolder called for position: " + position);

        if (holder instanceof LoadingViewHolder) {
            Log.d("ServiceListAdapter", "Binding LoadingViewHolder");
            return;
        }

        if (position >= filteredServices.size()) {
            Log.e("ServiceListAdapter", "Position " + position + " is out of bounds, size: " + filteredServices.size());
            return;
        }

        ServiceViewHolder serviceHolder = (ServiceViewHolder) holder;
        Service service = filteredServices.get(position);
        boolean isInCart = serviceInCartMap.containsKey(service.getId());

        Log.d("ServiceListAdapter", "Binding service: " + service.getName() + " at position: " + position);
        Log.d("ServiceListAdapter", "Listener is: " + (listener != null ? "not null" : "null"));

        serviceHolder.bind(service, listener, isInCart);
    }

    @Override
    public int getItemCount() {
        if (isLoading && filteredServices.isEmpty()) {
            return 6; // Показываем 6 skeleton элементов
        }
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

    // ViewHolder для skeleton загрузки
    static class LoadingViewHolder extends RecyclerView.ViewHolder {
        ShimmerFrameLayout shimmerLayout;

        public LoadingViewHolder(@NonNull View itemView) {
            super(itemView);
            shimmerLayout = itemView.findViewById(R.id.shimmer_layout);
        }
    }

    // Переименованный ViewHolder для сервисов
    static class ServiceViewHolder extends RecyclerView.ViewHolder {
        private final ImageView serviceImage;
        private final TextView serviceName;
        private final TextView servicePrice;
        private final TextView serviceOldPrice;
        private final ImageView actionButton;
        private final CartViewModel cartViewModel;
        private final LifecycleOwner lifecycleOwner;
        private boolean isInCart;
        private Cart currentCart;


        ServiceViewHolder(View itemView, CartViewModel cartViewModel, LifecycleOwner lifecycleOwner) {
            super(itemView);
            Log.d("ServiceListAdapter", "ServiceViewHolder constructor called");

            this.cartViewModel = cartViewModel;
            this.lifecycleOwner = lifecycleOwner;
            serviceImage = itemView.findViewById(R.id.service_image);
            serviceName = itemView.findViewById(R.id.service_name);
            servicePrice = itemView.findViewById(R.id.service_price);
            serviceOldPrice = itemView.findViewById(R.id.service_old_price);
            actionButton = itemView.findViewById(R.id.add_button);

            // Проверяем, что все views найдены
            Log.d("ServiceListAdapter", "Views found - serviceName: " + (serviceName != null) +
                    ", actionButton: " + (actionButton != null));

            // Получаем текущую корзину один раз и храним ее
            cartViewModel.getCart().observe(lifecycleOwner, cart -> {
                this.currentCart = cart;
            });
        }


        void bind(Service service, OnServiceActionListener listener, boolean isInCart) {
            Log.d("ServiceListAdapter", "bind() called for service: " + service.getName() + ", isInCart: " + isInCart);

            this.isInCart = isInCart;

            if (serviceName != null) {
                serviceName.setText(service.getName());
                Log.d("ServiceListAdapter", "Service name set: " + service.getName());
            } else {
                Log.e("ServiceListAdapter", "serviceName is null!");
            }

            // Получаем скидку из корзины с дополнительной проверкой
            int discount = 0;
            Cart cart = cartViewModel.getCart().getValue();
            if (cart != null && cart.getDiscount() != null) {
                discount = cart.getDiscount();
                Log.d("Discount", "discount: " + discount);
            } else {
                Log.d("Discount", "discount: 0 (cart is null or discount is null)");
            }

            int originalPrice = service.getPrice();
            if (discount > 0) {
                // Рассчитываем цену со скидкой
                int discountedPrice = originalPrice - (originalPrice * discount / 100);
                // Показываем старую цену зачеркнутой
                if (serviceOldPrice != null) {
                    serviceOldPrice.setText(String.format("%d ₽", originalPrice));
                    serviceOldPrice.setVisibility(View.VISIBLE);
                    serviceOldPrice.setPaintFlags(serviceOldPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                }
                // Показываем новую цену
                if (servicePrice != null) {
                    servicePrice.setText(String.format("%d ₽", discountedPrice));
                }
            } else {
                // Скрываем старую цену и показываем только обычную
                if (serviceOldPrice != null) {
                    serviceOldPrice.setVisibility(View.GONE);
                }
                if (servicePrice != null) {
                    servicePrice.setText(String.format("%d ₽", originalPrice));
                }
            }

            if (serviceImage != null) {
                Glide.with(itemView.getContext())
                        .load(service.getImageUrl())
                        .into(serviceImage);
            }

            updateButtonState();

            // КРИТИЧЕСКИ ВАЖНО: Устанавливаем обработчики кликов
            setupClickListeners(service, listener);
        }

        private void setupClickListeners(Service service, OnServiceActionListener listener) {
            Log.d("ServiceListAdapter", "setupClickListeners called for: " + service.getName());

            // Сначала удаляем старые обработчики
            if (actionButton != null) {
                actionButton.setOnClickListener(null);
            }
            itemView.setOnClickListener(null);

            // Устанавливаем новые обработчики
            if (actionButton != null) {
                actionButton.setOnClickListener(v -> {
                    Log.d("ServiceListAdapter", "Action button clicked for: " + service.getName());
                    handleServiceAction(service);
                    if (listener != null) {
                        listener.onServiceAction(service);
                    } else {
                        Log.e("ServiceListAdapter", "Listener is null!");
                    }
                });
                Log.d("ServiceListAdapter", "Action button listener set successfully");
            } else {
                Log.e("ServiceListAdapter", "actionButton is null, cannot set listener!");
            }

            itemView.setOnClickListener(v -> {
                Log.d("ServiceListAdapter", "Item clicked for: " + service.getName());
                if (listener != null) {
                    listener.onServiceSelected(service);
                } else {
                    Log.e("ServiceListAdapter", "Listener is null!");
                }
            });
            Log.d("ServiceListAdapter", "Item click listener set successfully");
        }

        private int getDiscountFromCart() {
            Cart cart = cartViewModel.getCart().getValue();
            if (cart != null && cart.getDiscount() != null) {
                return cart.getDiscount();
            }
            return 0;
        }

        private void updateButtonState() {
            Log.d("ServiceListAdapter", "updateButtonState called, isInCart: " + isInCart);

            if (actionButton != null) {
                if (isInCart) {
                    actionButton.setImageResource(R.drawable.delete_ic);
                    actionButton.setBackgroundResource(R.drawable.delete_button);
                    Log.d("ServiceListAdapter", "Button set to DELETE state");
                } else {
                    actionButton.setImageResource(R.drawable.plus_ic);
                    actionButton.setBackgroundResource(R.drawable.rounded_button);
                    Log.d("ServiceListAdapter", "Button set to ADD state");
                }
            } else {
                Log.e("ServiceListAdapter", "actionButton is null in updateButtonState!");
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
            // Рассчитываем цену со скидкой если она есть
            int servicePrice = service.getPrice();
            if (cart.getDiscount() != null && cart.getDiscount() > 0) {
                servicePrice = servicePrice - (servicePrice * cart.getDiscount() / 100);
            }

            // Создаем копию сервиса с новой ценой
            Service discountedService = new Service();
            discountedService.setId(service.getId());
            discountedService.setName(service.getName());
            discountedService.setPrice(servicePrice);
            discountedService.setDurationMinutes(service.getDurationMinutes());
            discountedService.setDescription(service.getDescription());
            discountedService.setImageUrl(service.getImageUrl());

            cartViewModel.addServiceToCart(
                    discountedService,
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
