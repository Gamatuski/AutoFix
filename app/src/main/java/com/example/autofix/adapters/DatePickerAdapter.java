package com.example.autofix.adapters;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.autofix.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DatePickerAdapter extends BottomSheetDialogFragment {
    private OnDateSelectedListener listener;

    // Интерфейс для передачи выбранной даты
    public interface OnDateSelectedListener {
        void onDateSelected(String selectedDate);
    }

    // Установка слушателя
    public void setOnDateSelectedListener(OnDateSelectedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        // Создаем BottomSheetDialog
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_scrolling_date_picker, null);
        dialog.setContentView(view);

        // Инициализация NumberPicker
        NumberPicker numPickerDay = view.findViewById(R.id.numPickerDay);
        NumberPicker numPickerMonth = view.findViewById(R.id.numPickerMonth);
        NumberPicker numPickerYear = view.findViewById(R.id.numPickerYear);

        // Установка минимальных и максимальных значений для NumberPicker
        Calendar calendar = Calendar.getInstance();
        numPickerDay.setMinValue(1);
        numPickerDay.setMaxValue(31);
        numPickerDay.setValue(calendar.get(Calendar.DAY_OF_MONTH));

        numPickerMonth.setMinValue(1);
        numPickerMonth.setMaxValue(12);
        numPickerMonth.setValue(calendar.get(Calendar.MONTH) + 1); // Месяцы начинаются с 0

        numPickerYear.setMinValue(2000);
        numPickerYear.setMaxValue(2030);
        numPickerYear.setValue(calendar.get(Calendar.YEAR));

        // Обработка нажатия на кнопку "Готово"
        Button btnDone = view.findViewById(R.id.btnDone);
        btnDone.setOnClickListener(v -> {
            // Получаем выбранные значения
            int day = numPickerDay.getValue();
            int month = numPickerMonth.getValue() - 1; // Месяцы начинаются с 0
            int year = numPickerYear.getValue();

            // Форматируем дату в строку
            calendar.set(year, month, day);
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
            String selectedDate = dateFormat.format(calendar.getTime());

            // Передаем выбранную дату через интерфейс
            if (listener != null) {
                listener.onDateSelected(selectedDate);
            }

            // Закрываем BottomSheetDialog
            dismiss();
        });

        return dialog;
    }
}
