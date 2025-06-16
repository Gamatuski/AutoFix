package com.example.autofix.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.autofix.MainActivity;
import com.example.autofix.R;

public class NotificationReceiver extends BroadcastReceiver {
    private static final String TAG = "NotificationReceiver";
    private static final String CHANNEL_ID = "booking_reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Notification received");

        String appointmentId = intent.getStringExtra("appointmentId");
        String serviceName = intent.getStringExtra("serviceName");
        String stationAddress = intent.getStringExtra("stationAddress");
        String bookingTime = intent.getStringExtra("bookingTime");

        if (appointmentId == null || serviceName == null) {
            Log.w(TAG, "Missing required data for notification");
            return;
        }

        showNotification(context, appointmentId, serviceName, stationAddress, bookingTime);
    }

    private void showNotification(Context context, String appointmentId, String serviceName,
                                  String stationAddress, String bookingTime) {

        // Intent для открытия приложения при нажатии на уведомление
        Intent mainIntent = new Intent(context, MainActivity.class);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        mainIntent.putExtra("openBookings", true);
        mainIntent.putExtra("appointmentId", appointmentId);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                appointmentId.hashCode(),
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Создаем уведомление
        String title = "Напоминание о записи";
        String content = String.format("Через 2 часа у вас запись на %s", serviceName);
        String bigText = String.format("Через 2 часа у вас запись:\n\n" +
                        "🔧 Услуга: %s\n" +
                        "📍 Адрес: %s\n" +
                        "🕐 Время: %s",
                serviceName,
                stationAddress != null ? stationAddress : "Не указан",
                bookingTime);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.fix_logo) // Создайте эту иконку
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(bigText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(new long[]{0, 500, 250, 500})
                .setLights(0xFF0000FF, 1000, 1000);

        // Показываем уведомление
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            notificationManager.notify(appointmentId.hashCode(), builder.build());
            Log.d(TAG, "Notification shown for appointment: " + appointmentId);
        }
    }
}