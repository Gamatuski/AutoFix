package com.example.autofix.services;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.autofix.MainActivity;
import com.example.autofix.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class NotificationReminderService extends Service {
    private static final String TAG = "NotificationReminder";
    private static final String CHANNEL_ID = "booking_reminders";
    private static final String FOREGROUND_CHANNEL_ID = "service_channel";
    private static final int FOREGROUND_NOTIFICATION_ID = 1000;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private Handler notificationHandler;
    private Map<String, Runnable> scheduledNotifications = new HashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        notificationHandler = new Handler(Looper.getMainLooper());
        createNotificationChannels();
        Log.d(TAG, "NotificationReminderService created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "NotificationReminderService started");

        // Запускаем сервис как foreground
        startForegroundService();

        // Проверяем, есть ли данные для добавления нового уведомления
        if (intent != null && intent.hasExtra("appointmentId")) {
            String appointmentId = intent.getStringExtra("appointmentId");
            String bookingDateTime = intent.getStringExtra("bookingDateTime");
            String stationAddress = intent.getStringExtra("stationAddress");

            // Добавляем новое уведомление
            scheduleNotificationForAppointment(appointmentId, bookingDateTime, stationAddress);
            Log.d(TAG, "Added new notification for appointment: " + appointmentId);
        } else {
            // Если нет конкретного уведомления, загружаем все
            scheduleNotificationChecks();
        }

        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Отменяем все запланированные уведомления
        for (Runnable runnable : scheduledNotifications.values()) {
            notificationHandler.removeCallbacks(runnable);
        }
        scheduledNotifications.clear();
        Log.d(TAG, "NotificationReminderService destroyed");
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);

            // Канал для напоминаний о записях
            CharSequence name = "Напоминания о записи";
            String description = "Уведомления о предстоящих записях на обслуживание";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.enableVibration(true);
            channel.enableLights(true);
            notificationManager.createNotificationChannel(channel);

            // Канал для foreground service
            CharSequence serviceName = "Сервис уведомлений";
            String serviceDescription = "Сервис для планирования уведомлений о записях";
            int serviceImportance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel serviceChannel = new NotificationChannel(FOREGROUND_CHANNEL_ID, serviceName, serviceImportance);
            serviceChannel.setDescription(serviceDescription);
            serviceChannel.setShowBadge(false);
            notificationManager.createNotificationChannel(serviceChannel);
        }
    }

    private void startForegroundService() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, FOREGROUND_CHANNEL_ID)
                .setContentTitle("Сервис уведомлений активен")
                .setContentText("Отслеживание напоминаний о записях")
                .setSmallIcon(R.drawable.fix_logo)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        startForeground(FOREGROUND_NOTIFICATION_ID, notification);
    }

    private void scheduleNotificationChecks() {
        if (auth.getCurrentUser() == null) {
            Log.w(TAG, "User not authenticated, stopping service");
            stopSelf();
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        Log.d(TAG, "Scheduling notification checks for user: " + userId);

        db.collection("users")
                .document(userId)
                .collection("appointments")
                .whereGreaterThan("bookingTime", new Date())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Found " + queryDocumentSnapshots.size() + " future appointments");
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            String bookingTimeStr = document.getString("bookingTime");
                            String stationAddress = document.getString("stationAddress");
                            if (bookingTimeStr != null) {
                                scheduleNotificationForAppointment(
                                        document.getId(),
                                        bookingTimeStr,
                                        stationAddress
                                );
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing appointment: " + document.getId(), e);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching appointments", e);
                    stopSelf();
                });
    }

    private void scheduleNotificationForAppointment(String appointmentId, String bookingTimeStr,
                                                    String stationAddress) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
            Date bookingTime = sdf.parse(bookingTimeStr);
            if (bookingTime == null) {
                Log.w(TAG, "Could not parse booking time: " + bookingTimeStr);
                return;
            }

            // Вычисляем время уведомления (за 2 часа)
            Calendar notificationTime = Calendar.getInstance();
            notificationTime.setTime(bookingTime);
            notificationTime.add(Calendar.HOUR_OF_DAY, -2);

            long currentTime = System.currentTimeMillis();
            long notificationTimeMillis = notificationTime.getTimeInMillis();

            if (notificationTimeMillis <= currentTime) {
                Log.d(TAG, "Notification time has passed for appointment: " + appointmentId);
                return;
            }

            // Вычисляем задержку
            long delay = notificationTimeMillis - currentTime;

            // Создаем Runnable для показа уведомления
            Runnable notificationRunnable = () -> {
                showNotification(appointmentId, stationAddress, bookingTimeStr);
                scheduledNotifications.remove(appointmentId);
            };

            // Отменяем предыдущее уведомление для этой записи, если оно есть
            cancelNotificationForAppointment(appointmentId);

            // Планируем новое уведомление
            notificationHandler.postDelayed(notificationRunnable, delay);
            scheduledNotifications.put(appointmentId, notificationRunnable);

            Log.d(TAG, "Scheduled notification for appointment " + appointmentId +
                    " in " + (delay / 1000 / 60) + " minutes");

        } catch (ParseException e) {
            Log.e(TAG, "Error parsing booking time: " + bookingTimeStr, e);
        }
    }

    private void showNotification(String appointmentId,
                                  String stationAddress, String bookingTime) {
        // Intent для открытия приложения при нажатии на уведомление
        Intent mainIntent = new Intent(this, MainActivity.class);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        mainIntent.putExtra("openBookings", true);
        mainIntent.putExtra("appointmentId", appointmentId);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                appointmentId.hashCode(),
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Создаем уведомление
        String title = "Напоминание о записи";
        String content = "Через 2 часа у вас запись на %s";
        String bigText = String.format("Через 2 часа у вас запись:\n\n" +
                        "📍 Адрес: %s\n" +
                        "🕐 Время: %s",
                stationAddress != null ? stationAddress : "Не указан",
                bookingTime);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.fix_logo)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(bigText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(new long[]{0, 500, 250, 500})
                .setLights(0xFF0000FF, 1000, 1000);

        // Показываем уведомление
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.notify(appointmentId.hashCode(), builder.build());
            Log.d(TAG, "Notification shown for appointment: " + appointmentId);
        }
    }

    // Метод для добавления нового уведомления без перезапуска сервиса
    public static void addNotificationForBooking(Context context, String appointmentId,
                                                 String bookingDateTime,
                                                 String stationAddress) {
        Intent intent = new Intent(context, NotificationReminderService.class);
        intent.putExtra("appointmentId", appointmentId);
        intent.putExtra("bookingDateTime", bookingDateTime);
        intent.putExtra("stationAddress", stationAddress);

        context.startService(intent);

        Log.d(TAG, "Added new notification for booking: " + appointmentId);
    }

    public static void startService(Context context) {
        Intent intent = new Intent(context, NotificationReminderService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static void stopService(Context context) {
        Intent intent = new Intent(context, NotificationReminderService.class);
        context.stopService(intent);
    }

    // Метод для отмены конкретного уведомления
    public static void cancelNotificationForAppointment(Context context, String appointmentId) {
        // Отправляем команду сервису для отмены уведомления
        Intent intent = new Intent(context, NotificationReminderService.class);
        intent.putExtra("cancelAppointment", appointmentId);
        context.startService(intent);

        Log.d(TAG, "Requested cancellation for appointment: " + appointmentId);
    }

    private void cancelNotificationForAppointment(String appointmentId) {
        Runnable runnable = scheduledNotifications.get(appointmentId);
        if (runnable != null) {
            notificationHandler.removeCallbacks(runnable);
            scheduledNotifications.remove(appointmentId);
            Log.d(TAG, "Cancelled notification for appointment: " + appointmentId);
        }
    }
}