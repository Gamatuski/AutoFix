package com.example.autofix.bottomsheets;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.autofix.R;
import com.example.autofix.data.entities.Cart;
import com.example.autofix.data.entities.CartItem;
import com.example.autofix.models.Service;
import com.example.autofix.sto.CartServiceActivity;
import com.example.autofix.viewmodels.CartViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;

public class ServiceDetailsBottomSheet extends BottomSheetDialogFragment {

    private static final String TAG = "ServiceDetailsBottomSheet";
    private static final String ARG_SERVICE = "service";

    private Service service;
    private OnServiceAddedListener listener;
    private CartViewModel cartViewModel;
    private TextView totalPriceText;

    Button addServiceButton;
    LinearLayout summaryContainer;
    private boolean isServiceInCart = false;
    private CartItem existingCartItem = null;


    public interface OnServiceAddedListener {
        void onServiceAdded(Service service);
    }

    public static ServiceDetailsBottomSheet newInstance(Service service) {
        ServiceDetailsBottomSheet fragment = new ServiceDetailsBottomSheet();
        Bundle args = new Bundle();
        args.putSerializable(ARG_SERVICE, service);
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnServiceAddedListener(OnServiceAddedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            service = (Service) getArguments().getSerializable(ARG_SERVICE);
        }
        // Инициализируем ViewModel
        cartViewModel = new ViewModelProvider(requireActivity()).get(CartViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_service_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (service == null) {
            dismiss();
            return;
        }

        // Инициализация views
        ImageView serviceImage = view.findViewById(R.id.service_image);
        TextView serviceName = view.findViewById(R.id.service_name);
        TextView servicePrice = view.findViewById(R.id.service_price);
        TextView serviceDuration = view.findViewById(R.id.service_duration);
        TextView serviceDescription = view.findViewById(R.id.service_description);
        addServiceButton = view.findViewById(R.id.add_service_button);

        summaryContainer = view.findViewById(R.id.summary_container);
        totalPriceText = view.findViewById(R.id.total_price_text);
        ImageView clearCartButton = view.findViewById(R.id.clear_cart_button);
        Button proceedToCartButton = view.findViewById(R.id.proceed_to_booking_button);

        // Заполнение данными
        serviceName.setText(service.getName());
        servicePrice.setText(String.format("%d ₽", service.getPrice()));

        // Установка длительности (если есть)
        if (service.getDurationMinutes() > 0) {
            serviceDuration.setText(String.format("%d минут", service.getDurationMinutes()));
        } else {
            serviceDuration.setText("Время не указано");
        }

        // Установка описания
        if (service.getDescription() != null && !service.getDescription().isEmpty()) {
            serviceDescription.setText(service.getDescription());
        } else {
            serviceDescription.setText("Описание отсутствует");
        }

        // Загрузка изображения
        String imageUrl = service.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .centerCrop()
                    .placeholder(R.drawable.placeholder_image)

                    .listener(new RequestListener<android.graphics.drawable.Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                    Target<android.graphics.drawable.Drawable> target,
                                                    boolean isFirstResource) {
                            Log.e(TAG, "Image load failed: " + (e != null ? e.getMessage() : "unknown error"));
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(android.graphics.drawable.Drawable resource,
                                                       Object model,
                                                       Target<android.graphics.drawable.Drawable> target,
                                                       DataSource dataSource,
                                                       boolean isFirstResource) {
                            Log.d(TAG, "Image loaded successfully");
                            return false;
                        }
                    })
                    .into(serviceImage);
        } else {
            serviceImage.setImageResource(R.drawable.placeholder_image);
        }

        // Проверяем, есть ли уже эта услуга в корзине
        checkIfServiceInCart();

        // Обработчик кнопки добавления
        addServiceButton.setOnClickListener(v -> {
            if (!isServiceInCart) {
                addServiceToCart();
            }
        });

        // Обработчик кнопки удаления
        clearCartButton.setOnClickListener(v -> {
            if (isServiceInCart) {
                removeServiceFromCart();
            }
        });




        // Обработчик кнопки перехода к записи
        proceedToCartButton.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), CartServiceActivity.class);
            startActivity(intent);
            dismiss();
        });
    }

    private void checkIfServiceInCart() {
        cartViewModel.getAllCartItems().observe(getViewLifecycleOwner(), cartItems -> {
            if (cartItems != null) {
                for (CartItem item : cartItems) {
                    if (item.getServiceId().equals(service.getId())) {
                        existingCartItem = item;
                        isServiceInCart = true;
                        break;
                    }
                }
                updateUI();
            }
        });
    }


    private void updateSummaryInfo() {
        cartViewModel.getTotalPrice().observe(getViewLifecycleOwner(), totalPrice -> {
            if (totalPrice != null) {
                totalPriceText.setText(String.format("%d ₽", totalPrice));
            } else {
                totalPriceText.setText("0 ₽");
            }
        });
    }
    public boolean isServiceWasAdded() {
        return isServiceInCart;
    }

    private void addServiceToCart() {
        Cart currentCart = cartViewModel.getCart().getValue();
        if (currentCart != null && currentCart.getSelectedCarId() != null) {
            cartViewModel.addServiceToCart(
                    service,
                    currentCart.getSelectedCarId(),
                    currentCart.getSelectedCarName()
            );
            isServiceInCart = true;
            updateUI();

            if (listener != null) {
                listener.onServiceAdded(service);
            }
        }
    }

    private void removeServiceFromCart() {
        Cart currentCart = cartViewModel.getCart().getValue();
        if (currentCart != null && existingCartItem != null) {
            cartViewModel.removeServiceFromCart(
                    existingCartItem,
                    currentCart.getSelectedCarId(),
                    currentCart.getSelectedCarName()
            );
            isServiceInCart = false;
            existingCartItem = null;
            updateUI();
        }
    }

    private void updateUI() {
        addServiceButton.setVisibility(isServiceInCart ? View.GONE : View.VISIBLE);
        summaryContainer.setVisibility(isServiceInCart ? View.VISIBLE : View.GONE);

        if (isServiceInCart) {
            updateSummaryInfo();
        }
    }
}
