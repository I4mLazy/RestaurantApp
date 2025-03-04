package com.example.restaurantapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.restaurantapp.R;
import com.example.restaurantapp.models.Restaurant;

import java.util.List;

public class RestaurantDiscoveryAdapter extends RecyclerView.Adapter<RestaurantDiscoveryAdapter.ViewHolder>
{

    private List<Restaurant> restaurants;
    private Context context;
    private OnRestaurantClickListener listener;

    // Interface for item click callback
    public interface OnRestaurantClickListener
    {
        void onRestaurantClick(Restaurant restaurant);
    }

    public RestaurantDiscoveryAdapter(List<Restaurant> restaurants, Context context, OnRestaurantClickListener listener)
    {
        this.restaurants = restaurants;
        this.context = context;
        this.listener = listener;
    }

    // Update adapter data
    public void updateData(List<Restaurant> newRestaurants)
    {
        this.restaurants.clear();
        this.restaurants.addAll(newRestaurants);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(context).inflate(R.layout.discovery_restaurant_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position)
    {
        Restaurant restaurant = restaurants.get(position);
        holder.restaurantName.setText(restaurant.getName() != null ? restaurant.getName() : "Unknown");
        holder.restaurantAddress.setText(restaurant.getAddress() != null ? restaurant.getAddress() : "");
        double rating = restaurant.getRating();
        holder.restaurantRating.setText(rating > 0 ? String.valueOf(rating) : "N/A");
        holder.itemView.setOnClickListener(v ->
        {
            if (listener != null)
            {
                listener.onRestaurantClick(restaurant);
            }
        });
    }

    @Override
    public int getItemCount()
    {
        return restaurants.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder
    {
        TextView restaurantName, restaurantAddress, restaurantRating;

        public ViewHolder(@NonNull View itemView)
        {
            super(itemView);
            restaurantName = itemView.findViewById(R.id.restaurantName);
            restaurantAddress = itemView.findViewById(R.id.restaurantAddress);
            restaurantRating = itemView.findViewById(R.id.restaurantRating);
        }
    }
}
