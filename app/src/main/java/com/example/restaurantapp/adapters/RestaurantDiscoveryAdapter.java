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
import com.example.restaurantapp.models.Restaurant;

import java.util.List;
import java.util.Locale;

public class RestaurantDiscoveryAdapter extends RecyclerView.Adapter<RestaurantDiscoveryAdapter.ViewHolder> {

    private List<Restaurant> restaurants;
    private Context context;
    private OnRestaurantClickListener listener;

    // Interface for item click callback
    public interface OnRestaurantClickListener {
        void onRestaurantClick(Restaurant restaurant);
    }

    public RestaurantDiscoveryAdapter(List<Restaurant> restaurants, Context context, OnRestaurantClickListener listener) {
        this.restaurants = restaurants;
        this.context = context;
        this.listener = listener;
    }

    // Update adapter data
    public void updateData(List<Restaurant> newRestaurants) {
        this.restaurants.clear();
        this.restaurants.addAll(newRestaurants);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_discovery_restaurant, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Restaurant restaurant = restaurants.get(position);

        holder.restaurantName.setText(restaurant.getName() != null ? restaurant.getName() : "Unknown");
        holder.restaurantAddress.setText(restaurant.getAddress() != null ? restaurant.getAddress() : "");

        double rating = restaurant.getAverageRating();
        holder.restaurantRating.setText(rating > 0 ? String.format(Locale.getDefault(), "%.1f", rating) : "N/A");

        Glide.with(context)
                .load(restaurant.getImageURL())
                .placeholder(R.drawable.image_placeholder)
                .error(R.drawable.image_placeholder)
                .into(holder.restaurantImage);

        // Bind price level
        String priceLevelText = getPriceLevelText(restaurant.getPriceLevel());
        holder.restaurantPriceLevel.setText(priceLevelText);

        // Bind tags
        List<String> tags = restaurant.getTags();
        if (tags != null && !tags.isEmpty()) {
            holder.restaurantTags.setText(joinTags(tags));
        } else {
            holder.restaurantTags.setText("");
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRestaurantClick(restaurant);
            }
        });
    }

    @Override
    public int getItemCount() {
        return restaurants.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView restaurantImage;
        TextView restaurantName, restaurantAddress, restaurantRating, restaurantPriceLevel, restaurantTags;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            restaurantImage = itemView.findViewById(R.id.restaurantImage);
            restaurantName = itemView.findViewById(R.id.restaurantName);
            restaurantAddress = itemView.findViewById(R.id.restaurantAddress);
            restaurantRating = itemView.findViewById(R.id.restaurantRating);
            restaurantPriceLevel = itemView.findViewById(R.id.restaurantPriceLevel);
            restaurantTags = itemView.findViewById(R.id.restaurantTags);
        }
    }

    // Helper to format price level
    private String getPriceLevelText(int priceLevel) {
        if (priceLevel <= 0) return "Price: N/A";
        StringBuilder sb = new StringBuilder("Price: ");
        for (int i = 0; i < priceLevel; i++) {
            sb.append("$");
        }
        return sb.toString();
    }

    // Helper to join tags
    private String joinTags(List<String> tags) {
        return String.join(", ", tags);
    }
}
