package com.example.autofix.data.repository;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.example.autofix.data.AppDatabase;
import com.example.autofix.data.dao.CartDao;
import com.example.autofix.data.entities.Cart;
import com.example.autofix.data.entities.CartItem;
import com.example.autofix.models.Service;
import com.example.autofix.viewmodels.CartViewModel;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CartRepository {
    private CartDao cartDao;
    private LiveData<Cart> cart;
    private LiveData<List<CartItem>> allCartItems;
    private LiveData<Integer> cartItemCount;
    private LiveData<Integer> totalPrice;
    private LiveData<Integer> totalDuration;
    private ExecutorService executor;
    private FirebaseFirestore db;

    public CartRepository(Application application) {
        AppDatabase database = AppDatabase.getInstance(application);
        cartDao = database.cartDao();
        cart = cartDao.getCart();
        allCartItems = cartDao.getAllCartItems();
        cartItemCount = cartDao.getCartItemCount();
        totalPrice = cartDao.getTotalPrice();
        totalDuration = cartDao.getTotalDuration();
        executor = Executors.newSingleThreadExecutor();
        db = FirebaseFirestore.getInstance();
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
        Log.d("CartRepository", "Getting total price from DAO");
        return totalPrice;
    }

    public LiveData<Integer> getTotalDuration() {
        return totalDuration;
    }

    // В CartRepository
    public void addServiceToCart(Service service, String carId, String carName) {
        executor.execute(() -> {
            // Check if service already exists
            CartItem existingItem = cartDao.getCartItemByServiceId(service.getId());
            if (existingItem != null) return;

            // Insert new cart item
            CartItem cartItem = new CartItem(
                    service.getId(),
                    service.getName(),
                    service.getPrice(),
                    service.getDurationMinutes(),
                    service.getImageUrl()
            );
            cartDao.insertCartItem(cartItem);

            // Update cart
            Cart existingCart = cartDao.getCartSync();
            if (existingCart == null) {
                Cart newCart = new Cart(carId, carName, service.getPrice(), service.getDurationMinutes());
                cartDao.insertCart(newCart);
            } else {
                // Calculate new totals
                List<CartItem> allItems = cartDao.getAllCartItemsSync();
                int newTotalPrice = allItems.stream().mapToInt(CartItem::getServicePrice).sum();
                int newTotalDuration = allItems.stream().mapToInt(CartItem::getServiceDuration).sum();

                existingCart.setTotalPrice(newTotalPrice);
                existingCart.setTotalDuration(newTotalDuration);
                existingCart.setSelectedCarId(carId);
                existingCart.setSelectedCarName(carName);
                cartDao.updateCart(existingCart);

                Log.d("CartRepository", "Updated cart price to: " + newTotalPrice);
            }
        });
    }

    public void removeServiceFromCart(CartItem cartItem, String carId, String carName) {
        executor.execute(() -> {
            cartDao.deleteCartItem(cartItem);

            // Обновляем корзину
            Cart existingCart = cartDao.getCartSync();
            if (existingCart != null) {
                existingCart.setSelectedCarId(carId);
                existingCart.setSelectedCarName(carName);
                existingCart.setTotalPrice(existingCart.getTotalPrice()-cartItem.getServicePrice());
                // Общая цена и продолжительность обновляются через LiveData
                cartDao.updateCart(existingCart);
            }
        });
    }

    public void clearCart() {
        executor.execute(() -> {
            cartDao.clearCart();

            // Очищаем информацию о корзине, но сохраняем объект
            Cart existingCart = cartDao.getCartSync();
            if (existingCart != null) {
                existingCart.setTotalPrice(0);
                existingCart.setTotalDuration(0);
                cartDao.updateCart(existingCart);
            }
        });
    }

    public void deleteCart() {
        executor.execute(() -> {
            cartDao.clearCart();
            cartDao.deleteCart();
        });
    }

    public void updateCart(Cart cart) {
        executor.execute(() -> {
            Log.d("CartRepository", "Updating cart: " + cart.toString());
            cartDao.updateCart(cart);
        });
    }

    public void insertCart(Cart cart) {
        executor.execute(() -> {
            Log.d("CartRepository", "Inserting cart: " + cart.toString());
            cartDao.insertCart(cart);
        });
    }

    public void clearBookingInfo() {
        executor.execute(() -> {
            cartDao.clearBookingInfo();
        });
    }

    public void updateOrderInfo(String orderId, String status) {
        executor.execute(() -> {
            cartDao.updateOrderInfo(orderId, status);
        });
    }

    /**
     * Создает заказ в Firestore на основе текущей корзины
     */
    public void createOrder(String userId, CartViewModel.OnOrderCreatedListener listener) {
        executor.execute(() -> {
            try {
                Cart currentCart = cartDao.getCartSync();
                List<CartItem> cartItems = cartDao.getAllCartItemsSync();

                if (currentCart == null || cartItems == null || cartItems.isEmpty() ||
                        currentCart.getStationId() == null ||
                        currentCart.getBookingDate() == null ||
                        currentCart.getBookingTime() == null) {

                    // Недостаточно информации для создания заказа
                    if (listener != null) {
                        listener.onOrderCreated(false, null);
                    }
                    return;
                }

                // Создаем уникальный ID для заказа
                String orderId = UUID.randomUUID().toString();

                // Подготавливаем данные заказа
                Map<String, Object> orderData = new HashMap<>();
                orderData.put("userId", userId);
                orderData.put("orderId", orderId);
                orderData.put("stationId", currentCart.getStationId());
                orderData.put("stationAddress", currentCart.getStationAddress());
                orderData.put("carId", currentCart.getSelectedCarId());
                orderData.put("carName", currentCart.getSelectedCarName());
                orderData.put("bookingDate", currentCart.getBookingDate());
                orderData.put("bookingTime", currentCart.getBookingTime());
                orderData.put("totalPrice", currentCart.getTotalPrice());
                orderData.put("totalDuration", currentCart.getTotalDuration());
                orderData.put("status", "pending");
                orderData.put("createdAt", System.currentTimeMillis());

                if (currentCart.getComment() != null && !currentCart.getComment().isEmpty()) {
                    orderData.put("comment", currentCart.getComment());
                }

                // Подготавливаем данные услуг
                List<Map<String, Object>> servicesData = new ArrayList<>();
                for (CartItem item : cartItems) {
                    Map<String, Object> serviceData = new HashMap<>();
                    serviceData.put("serviceId", item.getServiceId());
                    serviceData.put("serviceName", item.getServiceName());
                    serviceData.put("servicePrice", item.getServicePrice());
                    serviceData.put("serviceDuration", item.getServiceDuration());
                    servicesData.add(serviceData);
                }
                orderData.put("services", servicesData);

                // Сохраняем заказ в Firestore
                db.collection("orders")
                        .document(orderId)
                        .set(orderData, SetOptions.merge())
                        .addOnSuccessListener(aVoid -> {
                            // Обновляем информацию о заказе в локальной БД
                            updateOrderInfo(orderId, "pending");

                            // Добавляем заказ в коллекцию пользователя
                            Map<String, Object> userOrderData = new HashMap<>();
                            userOrderData.put("orderId", orderId);
                            userOrderData.put("stationAddress", currentCart.getStationAddress());
                            userOrderData.put("bookingDate", currentCart.getBookingDate());
                            userOrderData.put("bookingTime", currentCart.getBookingTime());
                            userOrderData.put("totalPrice", currentCart.getTotalPrice());
                            userOrderData.put("status", "pending");
                            userOrderData.put("createdAt", System.currentTimeMillis());

                            db.collection("users")
                                    .document(userId)
                                    .collection("orders")
                                    .document(orderId)
                                    .set(userOrderData, SetOptions.merge())
                                    .addOnSuccessListener(aVoid2 -> {
                                        if (listener != null) {
                                            listener.onOrderCreated(true, orderId);
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        if (listener != null) {
                                            listener.onOrderCreated(false, null);
                                        }
                                    });
                        })
                        .addOnFailureListener(e -> {
                            if (listener != null) {
                                listener.onOrderCreated(false, null);
                            }
                        });
            } catch (Exception e) {
                if (listener != null) {
                    listener.onOrderCreated(false, null);
                }
            }
        });
    }

    /**
     * Получает заказ из Firestore по ID
     */
    public void getOrderById(String orderId, OnOrderLoadedListener listener) {
        db.collection("orders")
                .document(orderId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> orderData = documentSnapshot.getData();
                        if (listener != null) {
                            listener.onOrderLoaded(true, orderData);
                        }
                    } else {
                        if (listener != null) {
                            listener.onOrderLoaded(false, null);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (listener != null) {
                        listener.onOrderLoaded(false, null);
                    }
                });
    }

    /**
     * Обновляет статус заказа в Firestore
     */
    public void updateOrderStatus(String orderId, String userId, String status, OnOrderStatusUpdatedListener listener) {
        // Обновляем в основной коллекции заказов
        db.collection("orders")
                .document(orderId)
                .update("status", status)
                .addOnSuccessListener(aVoid -> {
                    // Обновляем в коллекции пользователя
                    db.collection("users")
                            .document(userId)
                            .collection("orders")
                            .document(orderId)
                            .update("status", status)
                            .addOnSuccessListener(aVoid2 -> {
                                // Обновляем локальную БД
                                updateOrderInfo(orderId, status);

                                if (listener != null) {
                                    listener.onOrderStatusUpdated(true);
                                }
                            })
                            .addOnFailureListener(e -> {
                                if (listener != null) {
                                    listener.onOrderStatusUpdated(false);
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    if (listener != null) {
                        listener.onOrderStatusUpdated(false);
                    }
                });
    }



    public interface OnOrderLoadedListener {
        void onOrderLoaded(boolean success, Map<String, Object> orderData);
    }

    public interface OnOrderStatusUpdatedListener {
        void onOrderStatusUpdated(boolean success);
    }
}