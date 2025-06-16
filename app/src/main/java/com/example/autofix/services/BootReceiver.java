package com.example.autofix.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


import com.google.firebase.auth.FirebaseAuth;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Received action: " + action);

        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
                Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {

            // Проверяем, авторизован ли пользователь
            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth.getCurrentUser() != null) {
                Log.d(TAG, "User is authenticated, starting notification service");
                NotificationReminderService.startService(context);
            } else {
                Log.d(TAG, "User not authenticated, skipping service start");
            }
        }
    }
}