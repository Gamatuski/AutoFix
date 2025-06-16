package com.example.autofix.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.autofix.data.entities.Cart;
import com.example.autofix.data.entities.CartItem;

import java.util.List;

@Dao
public interface CartDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertCart(Cart cart);

    @Update
    void updateCart(Cart cart);


    @Query("SELECT * FROM cart WHERE id = 1")
    LiveData<Cart> getCart();

    @Query("SELECT * FROM cart WHERE id = 1")
    Cart getCartSync();

    @Insert
    long insertCartItem(CartItem cartItem);

    @Delete
    void deleteCartItem(CartItem cartItem);

    @Query("SELECT * FROM cart_items")
    LiveData<List<CartItem>> getAllCartItems();

    @Query("SELECT * FROM cart_items")
    List<CartItem> getAllCartItemsSync();

    @Query("SELECT * FROM cart_items WHERE serviceId = :serviceId")
    CartItem getCartItemByServiceId(String serviceId);

    @Query("DELETE FROM cart_items")
    void clearCart();

    @Query("DELETE FROM cart WHERE id = 1")
    void deleteCart();

    @Query("SELECT COUNT(*) FROM cart_items")
    LiveData<Integer> getCartItemCount();

    @Query("SELECT SUM(servicePrice) FROM cart_items")
    LiveData<Integer> getTotalPrice();

    @Query("SELECT SUM(serviceDuration) FROM cart_items")
    LiveData<Integer> getTotalDuration();

    // Новые методы для работы с заказом
    @Query("UPDATE cart SET stationId = NULL, stationAddress = NULL, bookingDate = NULL, bookingTime = NULL, comment = NULL WHERE id = 1")
    void clearBookingInfo();

    @Query("UPDATE cart SET orderId = :orderId, orderStatus = :status WHERE id = 1")
    void updateOrderInfo(String orderId, String status);

    @Query("SELECT * FROM cart_items WHERE serviceId IN (:serviceIds)")
    List<CartItem> getCartItemsByServiceIds(List<String> serviceIds);
    @Query("UPDATE cart SET discount = :discount WHERE id = 1")
    void updateDiscount(int discount);
}