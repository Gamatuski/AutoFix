package com.example.autofix.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.example.autofix.data.entities.Cart;
import com.example.autofix.data.entities.CartItem;
import com.example.autofix.data.repository.CartRepository;
import com.example.autofix.models.Service;

import java.util.List;

public class CartViewModel extends AndroidViewModel {
    private CartRepository repository;
    private LiveData<Cart> cart;
    private LiveData<List<CartItem>> allCartItems;
    private LiveData<Integer> cartItemCount;
    private LiveData<Integer> totalPrice;
    private LiveData<Integer> totalDuration;
    private int currentUserDiscount = 0;
    private boolean discountObserverActive = false;

    // Для отслеживания статуса бронирования
    private MutableLiveData<Boolean> bookingInProgress = new MutableLiveData<>(false);
    private MutableLiveData<Boolean> bookingSuccess = new MutableLiveData<>(false);

    public CartViewModel(@NonNull Application application) {
        super(application);
        repository = new CartRepository(application);
        cart = repository.getCart();
        allCartItems = repository.getAllCartItems();
        cartItemCount = repository.getCartItemCount();
        totalPrice = repository.getTotalPrice();
        totalDuration = repository.getTotalDuration();
    }

    public LiveData<Cart> getCart() {
        return cart;
    }

    public LiveData<List<CartItem>> getAllCartItems() {
        return allCartItems;
    }

    public LiveData<Integer> getCartItemCount() {
        return cartItemCount;
    }

    public LiveData<Integer> getTotalPrice() {
        Log.d("CartViewModel", "Getting total price LiveData");
        return repository.getTotalPrice();
    }

    public LiveData<Integer> getTotalDuration() {
        return totalDuration;
    }

    public LiveData<Boolean> getBookingInProgress() {
        return bookingInProgress;
    }

    public LiveData<Boolean> getBookingSuccess() {
        return bookingSuccess;
    }

    public void addServiceToCart(Service service, String carId, String carName) {
        repository.addServiceToCart(service, carId, carName);
    }

    public void removeServiceFromCart(CartItem cartItem, String carId, String carName) {
        repository.removeServiceFromCart(cartItem, carId, carName);
    }

    public void clearCart() {
        repository.clearCart();
    }

    public void updateCart(Cart cart) {
        repository.updateCart(cart);
    }

    public void insertCart(Cart cart) {
        repository.insertCart(cart);
    }

    // Новые методы для работы с бронированием

    public void setStationInfo(String stationId, String stationAddress) {
        Cart currentCart = cart.getValue();
        if (currentCart != null) {
            currentCart.setStationId(stationId);
            currentCart.setStationAddress(stationAddress);
            updateCart(currentCart);
        }
    }

    public void setBookingInfo(String date, String time, String comment) {
        Cart currentCart = cart.getValue();
        if (currentCart != null) {
            currentCart.setBookingDate(date);
            currentCart.setBookingTime(time);
            currentCart.setComment(comment);
            updateCart(currentCart);
        }
    }

    public void clearBookingInfo() {
        Cart currentCart = cart.getValue();
        if (currentCart != null) {
            currentCart.clearBookingInfo();
            updateCart(currentCart);
        }
    }

    public void startBookingProcess() {
        bookingInProgress.setValue(true);
        bookingSuccess.setValue(false);
    }

    public void finishBookingProcess(boolean success, String orderId) {
        bookingInProgress.setValue(false);
        bookingSuccess.setValue(success);

        if (success && orderId != null) {
            Cart currentCart = cart.getValue();
            if (currentCart != null) {
                currentCart.setOrderId(orderId);
                currentCart.setOrderStatus("pending");
                updateCart(currentCart);
            }
        }
    }

    // Метод для создания заказа в Firestore
    public void createOrder(String userId, OnOrderCreatedListener listener) {
        repository.createOrder(userId, listener);
    }
    public void applyDiscount(int discountPercent) {
        this.currentUserDiscount = discountPercent; // Сохраняем текущую скидку
        repository.applyDiscount(discountPercent);
        Log.d("CartViewModel", "User discount saved: " + discountPercent + "%");
    }
    // Добавьте метод для настройки observer'а для новых корзин
    public void setupDiscountObserver() {
        if (discountObserverActive) {
            return; // Избегаем множественных observer'ов
        }

        discountObserverActive = true;

        // Наблюдаем за изменениями корзины
        cart.observeForever(new Observer<Cart>() {
            @Override
            public void onChanged(Cart currentCart) {
                if (currentCart != null &&
                        (currentCart.getDiscount() == null || currentCart.getDiscount() == 0) &&
                        currentUserDiscount > 0) {

                    Log.d("CartViewModel", "New cart detected, applying saved discount: " + currentUserDiscount + "%");

                    // Применяем сохраненную скидку
                    currentCart.setDiscount(currentUserDiscount);
                    updateCart(currentCart);

                    // Удаляем observer после применения скидки
                    cart.removeObserver(this);
                    discountObserverActive = false;

                    Log.d("CartViewModel", "Discount applied to new cart: " + currentUserDiscount + "%");
                }
            }
        });
    }

    // Метод для получения текущей скидки
    public int getCurrentUserDiscount() {
        return currentUserDiscount;
    }

    // Метод для очистки observer'а
    public void clearDiscountObserver() {
        discountObserverActive = false;
    }

    public void deleteCart() {
        repository.deleteCart();
    }



    public interface OnOrderCreatedListener {
        void onOrderCreated(boolean success, String orderId);
    }
}