package com.example.autofix.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

// Создайте класс CartViewModelFactory
public class CartViewModelFactory implements ViewModelProvider.Factory {
    private Application application;

    public CartViewModelFactory(Application application) {
        this.application = application;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new CartViewModel(application);
    }
}