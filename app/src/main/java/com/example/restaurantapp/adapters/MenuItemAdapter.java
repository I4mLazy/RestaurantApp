package com.example.restaurantapp.adapters;

import android.content.Context;
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

import java.util.List;

public class MenuItemAdapter extends RecyclerView.Adapter<MenuItemAdapter.ViewHolder>
{

    // Callback interface for when a menu item is clicked
    public interface OnMenuItemClickListener
    {
        void onMenuItemClick(MenuItem menuItem);
    }

    private List<MenuItem> menuItems;
    private Context context;
    private OnMenuItemClickListener listener;

    // Updated constructor with listener
    public MenuItemAdapter(List<MenuItem> menuItems, Context context, OnMenuItemClickListener listener)
    {
        this.menuItems = menuItems;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(context).inflate(R.layout.item_menu_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position)
    {
        MenuItem item = menuItems.get(position);
        holder.itemName.setText(item.getName() != null ? item.getName() : "N/A");
        holder.itemPrice.setText(item.getPrice() > 0 ? String.valueOf(item.getPrice()) : "N/A");

        String imageUrl = item.getImageURL();
        if(imageUrl == null || imageUrl.isEmpty())
        {
            Glide.with(context)
                    .load(R.drawable.image_placeholder)
                    .into(holder.itemImage);
        } else
        {
            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.image_placeholder)
                    .error(R.drawable.image_placeholder)
                    .into(holder.itemImage);
        }

        // Set click listener on each menu item
        holder.itemView.setOnClickListener(v ->
        {
            if(listener != null)
            {
                listener.onMenuItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount()
    {
        return menuItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder
    {
        TextView itemName;
        TextView itemPrice;
        ImageView itemImage;

        public ViewHolder(View itemView)
        {
            super(itemView);
            itemName = itemView.findViewById(R.id.itemName);
            itemPrice = itemView.findViewById(R.id.itemPrice);
            itemImage = itemView.findViewById(R.id.itemImage);
        }
    }

    public void updateData(List<MenuItem> newItems)
    {
        this.menuItems.clear();
        this.menuItems.addAll(newItems);
        notifyDataSetChanged();
    }
}
