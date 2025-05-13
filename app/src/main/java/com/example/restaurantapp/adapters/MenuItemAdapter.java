package com.example.restaurantapp.adapters;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.restaurantapp.R;
import com.example.restaurantapp.models.MenuItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;

import com.example.restaurantapp.utils.DiscountUtils;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.Timestamp;

public class MenuItemAdapter extends RecyclerView.Adapter<MenuItemAdapter.ItemViewHolder>
{
    private List<MenuItem> menuItemList;
    private OnItemClickListener onItemClickListener;

    public interface OnItemClickListener
    {
        void onItemClick(MenuItem item);
    }

    public MenuItemAdapter(List<MenuItem> menuItemList, OnItemClickListener listener)
    {
        this.menuItemList = menuItemList;
        this.onItemClickListener = listener;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_menu_item, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position)
    {
        holder.bind(menuItemList.get(position));
    }

    @Override
    public int getItemCount()
    {
        return menuItemList.size();
    }

    class ItemViewHolder extends RecyclerView.ViewHolder
    {
        ImageView itemImage;
        TextView itemName, itemPrice, discountBadge, oldPrice;

        public ItemViewHolder(@NonNull View itemView)
        {
            super(itemView);
            itemImage = itemView.findViewById(R.id.itemImage);
            itemName = itemView.findViewById(R.id.itemName);
            itemPrice = itemView.findViewById(R.id.itemPrice);
            discountBadge = itemView.findViewById(R.id.discountBadge);
            oldPrice = itemView.findViewById(R.id.oldPrice);

        }

        public void bind(MenuItem item)
        {
            itemName.setText(item.getName());
            itemPrice.setText(String.format("$%.2f", item.getPrice())); // Show original price by default

            if(item.getImageURL() != null)
            {
                Glide.with(itemView.getContext())
                        .load(item.getImageURL())
                        .into(itemImage);
            } else
            {
                itemImage.setImageResource(R.drawable.image_placeholder);
            }

            // Apply discount using utility
            DiscountUtils.applyActiveDiscounts(item, itemView.getContext(), (original, current, hasDiscount, isFree, badgeText) ->
            {
                if(hasDiscount)
                {
                    itemPrice.setText(String.format("$%.2f", current));
                    oldPrice.setVisibility(View.VISIBLE);
                    oldPrice.setText(String.format("$%.2f", original));
                    oldPrice.setPaintFlags(oldPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG); // Strikethrough effect
                    discountBadge.setVisibility(View.VISIBLE);
                    discountBadge.setText(badgeText);
                }

                if(isFree)
                {
                    discountBadge.setVisibility(View.VISIBLE);
                    discountBadge.setText(itemView.getContext().getString(R.string.free));
                }
            });

            itemView.setOnClickListener(v ->
            {
                if(onItemClickListener != null)
                {
                    onItemClickListener.onItemClick(item);
                }
            });
        }
    }
}
