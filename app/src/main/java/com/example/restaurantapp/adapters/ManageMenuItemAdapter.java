package com.example.restaurantapp.adapters;

import android.util.Log;
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

public class ManageMenuItemAdapter extends RecyclerView.Adapter<ManageMenuItemAdapter.ItemViewHolder>
{
    private List<MenuItem> menuItemList;
    private OnItemClickListener onItemClickListener;

    public interface OnItemClickListener
    {
        void onItemClick(MenuItem item);
    }

    public ManageMenuItemAdapter(List<MenuItem> menuItemList, OnItemClickListener listener)
    {
        this.menuItemList = menuItemList;
        this.onItemClickListener = listener;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_manage_menu_item, parent, false);
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
        TextView itemName, itemPrice;

        public ItemViewHolder(@NonNull View itemView)
        {
            super(itemView);
            itemImage = itemView.findViewById(R.id.itemImage);
            itemName = itemView.findViewById(R.id.itemName);
            itemPrice = itemView.findViewById(R.id.itemPrice);
        }

        public void bind(MenuItem item)
        {
            itemName.setText(item.getName());
            itemPrice.setText("$" + item.getPrice());

            if(item.getImageURL() != null)
            {
                Glide.with(itemView.getContext()).load(item.getImageURL()).into(itemImage);
            } else
            {
                itemImage.setImageResource(R.drawable.image_placeholder);
            }

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
