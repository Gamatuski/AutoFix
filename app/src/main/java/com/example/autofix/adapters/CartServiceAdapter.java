package com.example.autofix.adapters;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.autofix.R;
import com.example.autofix.data.entities.CartItem;
import java.util.ArrayList;
import java.util.List;

public class CartServiceAdapter extends RecyclerView.Adapter<CartServiceAdapter.ViewHolder> {
    private List<CartItem> cartItems;
    private OnItemDeleteListener deleteListener;

    public CartServiceAdapter(List<CartItem> cartItems, OnItemDeleteListener listener) {
        this.cartItems = cartItems != null ? cartItems : new ArrayList<>();
        this.deleteListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_service_cart, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CartItem item = cartItems.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    public void updateItems(List<CartItem> newItems) {
        this.cartItems = newItems != null ? newItems : new ArrayList<>();
        notifyDataSetChanged();
    }

    public List<CartItem> getCartItems() {
        return cartItems;
    }

    public ItemTouchHelper.Callback getSwipeToDeleteCallback(Vibrator vibrator, Context context) {
        return new SwipeToDeleteCallback(this, vibrator, context);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView serviceImage;
        private final TextView serviceName;
        private final TextView servicePrice;
        private final View cardView;

        ViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_view);
            serviceImage = itemView.findViewById(R.id.service_image);
            serviceName = itemView.findViewById(R.id.service_name);
            servicePrice = itemView.findViewById(R.id.service_price);
        }

        void bind(CartItem item) {
            serviceName.setText(item.getServiceName());
            servicePrice.setText(String.format("%d ₽", item.getServicePrice()));
            Glide.with(itemView.getContext())
                    .load(item.getServiceImageUrl())
                    .placeholder(R.drawable.placeholder_image)
                    .into(serviceImage);
        }
    }

    public interface OnItemDeleteListener {
        void onItemDeleted(int position);
    }

    private static class SwipeToDeleteCallback extends ItemTouchHelper.SimpleCallback {
        private final CartServiceAdapter adapter;
        private final Paint backgroundPaint;
        private final Paint textPaint;
        private final Drawable deleteIcon;
        private final Vibrator vibrator;
        private boolean isSwipedBeyondThreshold = false;
        private final int cornerRadius;
        private final RectF backgroundRect;

        public SwipeToDeleteCallback(CartServiceAdapter adapter, Vibrator vibrator, Context context) {
            super(0, ItemTouchHelper.LEFT);
            this.adapter = adapter;
            this.vibrator = vibrator;
            this.cornerRadius = 24;
            this.backgroundRect = new RectF();

            // Настройка краски для фона
            backgroundPaint = new Paint();
            backgroundPaint.setColor(Color.parseColor("#F44336"));
            backgroundPaint.setAntiAlias(true);

            // Настройка краски для текста
            textPaint = new Paint();
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(48);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setAntiAlias(true);
            textPaint.setTypeface(Typeface.DEFAULT_BOLD);

            // Получение иконки удаления с переданным контекстом
            deleteIcon = ContextCompat.getDrawable(context, R.drawable.ic_delete_white);
            if (deleteIcon != null) {
                deleteIcon.setTint(Color.WHITE);
                deleteIcon.setBounds(0, 0, 72, 72);
            }
        }


        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView,
                              @NonNull RecyclerView.ViewHolder viewHolder,
                              @NonNull RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                adapter.deleteListener.onItemDeleted(position);
            }
        }

        @Override
        public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                @NonNull RecyclerView.ViewHolder viewHolder,
                                float dX, float dY, int actionState, boolean isCurrentlyActive) {

            View itemView = viewHolder.itemView;
            int itemHeight = itemView.getHeight();
            int itemWidth = itemView.getWidth();

            // Добавляем отступы для скругления
            int margin = 16;

            // Настраиваем прямоугольник для фона со скругленными углами
            backgroundRect.set(
                    itemView.getRight() + dX + margin,
                    itemView.getTop() + margin,
                    itemView.getRight() - margin,
                    itemView.getBottom() - margin
            );

            // Рисуем скругленный фон
            c.drawRoundRect(backgroundRect, cornerRadius, cornerRadius, backgroundPaint);

            float threshold = itemWidth * 0.8f;
            boolean showDeleteElements = Math.abs(dX) > itemWidth * 0.3f;

            if (showDeleteElements) {
                // Позиция для иконки и текста
                float centerX = itemView.getRight() - itemWidth / 2;
                float centerY = itemView.getTop() + itemHeight / 2;

                // Рисуем иконку удаления
                if (deleteIcon != null) {
                    int iconSize = 72;
                    int iconLeft = (int) (centerX - iconSize / 2);
                    int iconTop = (int) (centerY - iconSize / 2 - 30); // Смещаем вверх для текста
                    int iconRight = iconLeft + iconSize;
                    int iconBottom = iconTop + iconSize;

                    deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                    deleteIcon.draw(c);

                    // Рисуем текст под иконкой
                    c.drawText("Удалить", centerX, centerY + 40, textPaint);
                } else {
                    // Если иконки нет, рисуем только текст
                    c.drawText("Удалить", centerX, centerY + 20, textPaint);
                }

                // Вибрация при достижении порога
                if (Math.abs(dX) > threshold) {
                    if (!isSwipedBeyondThreshold) {
                        vibrator.vibrate(50);
                        isSwipedBeyondThreshold = true;
                    }
                } else {
                    isSwipedBeyondThreshold = false;
                }
            }

            // Зависание элемента при появлении элементов удаления
            if (Math.abs(dX) > itemWidth * 0.5f && Math.abs(dX) < threshold) {
                if (dX < 0) {
                    dX = -itemWidth * 0.5f;
                }
            }

            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }
    }
}