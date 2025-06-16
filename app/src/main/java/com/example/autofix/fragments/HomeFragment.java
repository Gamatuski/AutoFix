package com.example.autofix.fragments;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;


import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.autofix.AppointmentDetailsActivity;
import com.example.autofix.AppointmentHistoryActivity;
import com.example.autofix.CreateAccountActivity;
import com.example.autofix.R;
import com.example.autofix.adapters.AppointmentsAdapter;
import com.example.autofix.addCar.AddCarActivity;
import com.example.autofix.bonusCard.BonusCardDetailsActivity;
import com.example.autofix.data.entities.Cart;
import com.example.autofix.models.Appointment;
import com.example.autofix.models.BonusCard;
import com.example.autofix.sto.BookServiceActivity;
import com.example.autofix.viewmodels.CartViewModel;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeFragment extends Fragment implements AppointmentsAdapter.OnAppointmentClickListener {
    private FirebaseFirestore db;
    private TextView bonusPointsText, cardStatusText, statusProgressText,titleText;
    private ProgressBar statusProgressBar;
    private ImageView silverDot, goldDot, rubyDot;
    private LinearLayout loadingView, card_info,historySection;
    private CardView bonus_card;
    private Button book_service_button, btn_register;

    // Добавляем поля для работы с записями
    private RecyclerView appointmentsRecyclerView;
    private AppointmentsAdapter appointmentsAdapter;
    private View emptyStateView;
    private boolean hasCars = false;

    private View bonusCardSkeleton;
    private ShimmerFrameLayout shimmerLayout;
    // Пороги для статусов карты

    private BonusCard bonusCard;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Инициализация Firebase
        db = FirebaseFirestore.getInstance();

        // Находим все View элементы
        titleText = view.findViewById(R.id.title_text);
        bonusPointsText = view.findViewById(R.id.bonus_points);
        cardStatusText = view.findViewById(R.id.card_status);
        statusProgressText = view.findViewById(R.id.status_progress_text);
        statusProgressBar = view.findViewById(R.id.status_progress);
        loadingView = view.findViewById(R.id.loading_layout);
        bonus_card = view.findViewById(R.id.bonus_card);
        book_service_button = view.findViewById(R.id.book_service_button);
        card_info = view.findViewById(R.id.card_info);
        btn_register = view.findViewById(R.id.btn_register);
        historySection = view.findViewById(R.id.history_section);
        // Точки прогресса
        silverDot = view.findViewById(R.id.silver_dot);
        goldDot = view.findViewById(R.id.gold_dot);
        rubyDot = view.findViewById(R.id.ruby_dot);

        // Инициализация элементов для записей (если они есть в layout)
        appointmentsRecyclerView = view.findViewById(R.id.recycler_appointments);

        bonusCardSkeleton = view.findViewById(R.id.bonus_card_skeleton);
        shimmerLayout = bonusCardSkeleton.findViewById(R.id.shimmer_layout);

        // Настройка RecyclerView для записей (если нужно показывать последние записи)
        if (appointmentsRecyclerView != null) {
            appointmentsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            appointmentsAdapter = new AppointmentsAdapter(getContext(), new ArrayList<>(), this);
            appointmentsRecyclerView.setAdapter(appointmentsAdapter);
        }

        return view;
    }

    // Модифицируйте метод onViewCreated
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            showGuestView();
            return;
        }
        else {
            // Загружаем данные пользователя
            loadUserData();
            // Проверяем наличие автомобилей
            checkUserCars();
            // Загружаем последние записи (если нужно)
            if (appointmentsRecyclerView != null) {
                loadAppointments();
            }
        }

        book_service_button.setOnClickListener(v -> {
            // Проверяем наличие автомобилей перед показом опций записи
            checkCarsBeforeBooking();
        });

        // Обработчик клика по секции истории записей
        historySection.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AppointmentHistoryActivity.class);
            startActivity(intent);
        });

        bonus_card.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), BonusCardDetailsActivity.class);
            startActivity(intent);
        });
    }

    // Добавьте метод для проверки наличия автомобилей
    private void checkUserCars() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .collection("cars")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        hasCars = !task.getResult().isEmpty();
                        Log.d("HomeFragment", "User has cars: " + hasCars);
                    } else {
                        Log.e("HomeFragment", "Error checking cars", task.getException());
                        hasCars = false;
                    }
                });
    }

    private void showAddCarDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Вы не добавили авто в гараж");
        builder.setMessage("Добавить авто?");
        builder.setPositiveButton("Добавить", (dialog, which) -> {
            // Переходим к добавлению автомобиля
            Intent intent = new Intent(getActivity(), AddCarActivity.class);
            startActivity(intent);
        });
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private void checkCarsBeforeBooking() {
        if (!hasCars) {
            showAddCarDialog();
        } else {
            showBookingOptionsBottomSheet();
        }
    }


    private void loadAppointments() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        appointmentsAdapter.showLoading();
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .collection("appointments")
                .whereEqualTo("status", "confirmed")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Appointment> appointmentsList = new ArrayList<>();
                        Log.d("HomeFragment", "Found " + task.getResult().size() + " appointments");

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                Appointment appointment = new Appointment();
                                appointment.setId(document.getId());

                                if (document.contains("stationId"))
                                    appointment.setStationId(document.getString("stationId"));
                                if (document.contains("stationAddress"))
                                    appointment.setStationAddress(document.getString("stationAddress"));
                                if (document.contains("bookingDate"))
                                    appointment.setBookingDate(document.getString("bookingDate"));
                                if (document.contains("bookingTime"))
                                    appointment.setBookingTime(document.getString("bookingTime"));
                                if (document.contains("carId"))
                                    appointment.setCarId(document.getString("carId"));
                                if (document.contains("carName"))
                                    appointment.setCarName(document.getString("carName"));
                                if (document.contains("totalPrice"))
                                    appointment.setTotalPrice(document.getLong("totalPrice").intValue());
                                if (document.contains("status"))
                                    appointment.setStatus(document.getString("status"));
                                if (document.contains("createdAt"))
                                    appointment.setCreatedAt(document.getDate("createdAt"));
                                if (document.contains("isQuickBooking"))
                                    appointment.setQuickBooking(Boolean.TRUE.equals(document.getBoolean("isQuickBooking")));

                                appointmentsList.add(appointment);
                                Log.d("HomeFragment", "Added appointment: " + appointment.getId());

                            } catch (Exception e) {
                                Log.e("HomeFragment", "Error parsing appointment: " + e.getMessage());
                            }
                        }

                        // Сортируем на клиенте
                        appointmentsList.sort((a1, a2) -> {
                            if (a1.getCreatedAt() == null && a2.getCreatedAt() == null) return 0;
                            if (a1.getCreatedAt() == null) return 1;
                            if (a2.getCreatedAt() == null) return -1;
                            return a2.getCreatedAt().compareTo(a1.getCreatedAt());
                        });

                        // ВАЖНО: Обновляем адаптер с полученными данными
                        appointmentsAdapter.updateAppointments(appointmentsList);

                    } else {
                        Log.e("HomeFragment", "Error getting appointments", task.getException());
                        // Показываем пустой список в случае ошибки
                        updateAppointmentsList(new ArrayList<>());
                    }
                });
    }

    // Добавьте этот метод для обновления списка записей
    private void updateAppointmentsList(List<Appointment> appointments) {
        if (appointmentsAdapter != null) {
            Log.d("HomeFragment", "Updating adapter with " + appointments.size() + " appointments");

            appointmentsAdapter.updateAppointments(appointments);

            // Управляем видимостью RecyclerView
            if (appointments.isEmpty()) {
                appointmentsRecyclerView.setVisibility(View.GONE);
                // Можно показать сообщение "Нет записей"
            } else {
                appointmentsRecyclerView.setVisibility(View.VISIBLE);
            }
        } else {
            Log.e("HomeFragment", "AppointmentsAdapter is null");
        }
    }

    // Реализация интерфейса OnAppointmentClickListener
    @Override
    public void onAppointmentClick(Appointment appointment) {
        // Открываем активность с деталями записи
        Intent intent = new Intent(getActivity(), AppointmentDetailsActivity.class);
        intent.putExtra("APPOINTMENT_ID", appointment.getId());
        startActivity(intent);
    }

    // Add this method
    private void showGuestView() {
        loadingView.setVisibility(View.GONE);
        bonus_card.setBackgroundResource(R.drawable.gradient_bonus_card);
        book_service_button.setEnabled(false);
        card_info.setVisibility(View.GONE);
        btn_register.setVisibility(View.VISIBLE);
        btn_register.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), CreateAccountActivity.class));
        });

        TextView guestMessage = getView().findViewById(R.id.guest_message);
        if (guestMessage != null) {
            guestMessage.setVisibility(View.VISIBLE);
            guestMessage.setText("Для просмотра бонусной карты необходимо зарегистрироваться");
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Останавливаем shimmer
            if (shimmerLayout != null) {
                shimmerLayout.stopShimmer();
            }

            // Скрываем skeleton и показываем основную карточку
            loadingView.setVisibility(View.GONE);
            bonus_card.setBackgroundResource(R.drawable.gradient_bonus_card);
            book_service_button.setEnabled(false);
            card_info.setVisibility(View.GONE);
            btn_register.setVisibility(View.VISIBLE);
            btn_register.setOnClickListener(v -> {
                startActivity(new Intent(getActivity(), CreateAccountActivity.class));
            });

            // Скрываем секцию с записями для гостя
            if (appointmentsRecyclerView != null) {
                appointmentsRecyclerView.setVisibility(View.GONE);
            }
            if (emptyStateView != null) {
                emptyStateView.setVisibility(View.GONE);
            }

        }, 1000);


    }

    private void loadUserData() {
        showLoading();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(userDocument -> {
                    if (userDocument.exists()) {
                        // Загружаем имя пользователя
                        String firstName = userDocument.getString("firstName");
                        if (firstName != null && !firstName.isEmpty()) {
                            titleText.setText("Привет,"+ firstName + "!");
                        } else {
                            titleText.setText("Профиль");
                        }

                        Long totalSpentLong = userDocument.getLong("totalSpent");
                        int totalSpent = totalSpentLong != null ? totalSpentLong.intValue() : 0;

                        // Загружаем данные бонусной карты
                        db.collection("users").document(userId)
                                .collection("bonus_card").document("card_info").get()
                                .addOnSuccessListener(cardDocument -> {
                                    if (cardDocument.exists()) {
                                        Long points = cardDocument.getLong("points");
                                        long cardPoints = points != null ? points : 0;
                                        bonusCard = new BonusCard(cardPoints, totalSpent);
                                        updateBonusCardUI();
                                    } else {
                                        Toast.makeText(getContext(), "Информация о бонусной карте не найдена", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(getContext(), "Ошибка при загрузке данных бонусной карты: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }
                    showBonusCard();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Ошибка при загрузке данных пользователя: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateBonusCardUI() {
        if (bonusCard == null) return;

        // Обновляем баллы
        bonusPointsText.setText(String.valueOf(bonusCard.getPoints()));

        // Обновляем статус карты
        BonusCard.CardStatus currentStatus = bonusCard.getCurrentStatus();
        cardStatusText.setText(currentStatus.getDisplayName());
        cardStatusText.setBackgroundResource(currentStatus.getBackgroundResource());

        // Обновляем прогресс бар
        statusProgressBar.setMax(100);
        statusProgressBar.setProgress(bonusCard.getProgressPercent());

        // Обновляем текст прогресса
        statusProgressText.setText(bonusCard.getProgressText());

        // Обновляем точки прогресса
        updateProgressDots();

        // Генерация штрихкода по email пользователя
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getEmail() != null) {
            String userEmail = user.getEmail();
            Bitmap barcode = generateBarcode(userEmail);
            ImageView barcodeImageView = getView().findViewById(R.id.barcode_image);
            if (barcode != null && barcodeImageView != null) {
                barcodeImageView.setImageBitmap(barcode);
                barcodeImageView.setVisibility(View.VISIBLE);
            }
        }

        // Применяем скидку к корзине и настраиваем observer для новых корзин
        int discountPercent = currentStatus.getDiscountPercent();
        CartViewModel cartViewModel = new ViewModelProvider(requireActivity()).get(CartViewModel.class);

        // Применяем скидку (это также сохранит её в CartViewModel)
        cartViewModel.applyDiscount(discountPercent);

        // Настраиваем observer для автоматического применения скидки к новым корзинам
        cartViewModel.setupDiscountObserver();
    }

    private void showBookingOptionsBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext());
        View bottomSheetView = LayoutInflater.from(getContext())
                .inflate(R.layout.bottom_sheet_booking_options, null);

        Button quickBookingOption = bottomSheetView.findViewById(R.id.option_quick_booking);
        Button catalogBookingOption = bottomSheetView.findViewById(R.id.option_catalog_booking);

        quickBookingOption.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            // Открываем быструю запись с флагом
            Intent intent = new Intent(getActivity(), BookServiceActivity.class);
            intent.putExtra("IS_QUICK_BOOKING", true);
            startActivity(intent);
        });

        catalogBookingOption.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            // Открываем обычную запись без флага
            Intent intent = new Intent(getActivity(), BookServiceActivity.class);
            intent.putExtra("IS_QUICK_BOOKING", false);
            startActivity(intent);
        });

        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.show();
    }


    private void updateProgressDots() {
        // Серебро всегда активно
        silverDot.setImageResource(bonusCard.isStatusActive(BonusCard.CardStatus.SILVER) ?
                R.drawable.progress_dot_silver : R.drawable.progress_dot_inactive);

        // Золото активно если потрачено >= 40000
        goldDot.setImageResource(bonusCard.isStatusActive(BonusCard.CardStatus.GOLD) ?
                R.drawable.progress_dot_gold : R.drawable.progress_dot_inactive);

        // Рубин активен если потрачено >= 200000
        rubyDot.setImageResource(bonusCard.isStatusActive(BonusCard.CardStatus.RUBY) ?
                R.drawable.progress_dot_ruby : R.drawable.progress_dot_inactive);
    }

    private void showBonusCard() {

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Останавливаем shimmer
            if (shimmerLayout != null) {
                shimmerLayout.stopShimmer();
            }

            // Скрываем skeleton и показываем основную карточку
            bonusCardSkeleton.setVisibility(View.GONE);
            bonus_card.setVisibility(View.VISIBLE);

        }, 1000);
    }

    private void showLoading() {
        bonusCardSkeleton.setVisibility(View.VISIBLE);
        bonus_card.setVisibility(View.GONE);


        // Запускаем shimmer анимацию
        if (shimmerLayout != null) {
            shimmerLayout.startShimmer();
        }
    }

    private Bitmap generateBarcode(String content) {
        int width = 1000;
        int height = 150;

        // Используем Code 128, можно использовать другие форматы (EAN13, UPC_A и т.д.)
        BarcodeFormat format = BarcodeFormat.CODE_128;

        Map<EncodeHintType, Object> hints = new HashMap<>();

        hints.put(EncodeHintType.MARGIN, 1); // уменьшаем отступы

        MultiFormatWriter writer = new MultiFormatWriter();
        try {
            BitMatrix matrix = writer.encode(content, format, width, height, null);
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint();
            paint.setColor(Color.BLACK);
            paint.setStyle(Paint.Style.FILL);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    if (matrix.get(x, y)) {
                        canvas.drawPoint(x, y, paint);
                    }
                }
            }
            return bitmap;
        } catch (WriterException e) {
            Log.e("HomeFragment", "Ошибка генерации штрихкода", e);
            return null;
        }
    }
    @Override
    public void onResume() {
        super.onResume();

        if (shimmerLayout != null && bonusCardSkeleton.getVisibility() == View.VISIBLE) {
            shimmerLayout.startShimmer();
        }
        // Обновляем данные при возвращении к фрагменту
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            loadUserData();
            // Проверяем наличие автомобилей
            checkUserCars();
            // Обновляем последние записи, если RecyclerView существует
            if (appointmentsRecyclerView != null) {
                loadAppointments();
            }
        }
    }


}