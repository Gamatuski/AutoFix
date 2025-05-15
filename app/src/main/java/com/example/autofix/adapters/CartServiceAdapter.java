package com.example.autofix.adapters;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
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

    public ItemTouchHelper.Callback getSwipeToDeleteCallback(Vibrator vibrator) {
        return new SwipeToDeleteCallback(this, vibrator);
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
        private final ColorDrawable background;
        private final Vibrator vibrator;
        private boolean isSwipedBeyondThreshold = false;

        public SwipeToDeleteCallback(CartServiceAdapter adapter, Vibrator vibrator) {
            super(0, ItemTouchHelper.LEFT);
            this.adapter = adapter;
            this.background = new ColorDrawable(Color.RED);
            this.vibrator = vibrator;
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

            // Рисуем красный фон
            background.setBounds(itemView.getRight() + (int) dX, itemView.getTop(),
                    itemView.getRight(), itemView.getBottom());
            background.draw(c);

            // Рисуем текст "Удалить"
            Paint textPaint = new Paint();
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(48);
            textPaint.setTextAlign(Paint.Align.CENTER);

            float threshold = itemWidth * 0.8f;

            if (Math.abs(dX) > threshold) {
                if (!isSwipedBeyondThreshold) {
                    vibrator.vibrate(50);
                    isSwipedBeyondThreshold = true;
                }
                c.drawText("Удалить", itemView.getRight() - itemWidth / 2,
                        itemView.getTop() + itemHeight / 2 + 20, textPaint);
            } else {
                isSwipedBeyondThreshold = false;
            }

            // Зависание элемента при появлении надписи "Удалить"
            if (Math.abs(dX) > itemWidth * 0.5f && Math.abs(dX) < threshold) {
                if (dX < 0) {
                    dX = -itemWidth * 0.5f;
                }
            }

            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }
    }
}