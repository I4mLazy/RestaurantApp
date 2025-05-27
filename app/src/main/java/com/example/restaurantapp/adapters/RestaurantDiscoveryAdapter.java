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

/**
 * Adapter for displaying a list of {@link Restaurant} objects in a RecyclerView,
 * typically used in a discovery or browsing context.
 * Each item displays the restaurant's image, name, address, average rating,
 * price level, and tags. It also handles click events on items.
 */
public class RestaurantDiscoveryAdapter extends RecyclerView.Adapter<RestaurantDiscoveryAdapter.ViewHolder>
{

    /**
     * The list of {@link Restaurant} objects to be displayed.
     */
    private List<Restaurant> restaurants;
    /**
     * The context in which the adapter is operating.
     */
    private Context context;
    /**
     * Listener for click events on individual restaurant items.
     */
    private OnRestaurantClickListener listener;

    /**
     * Interface definition for a callback to be invoked when a restaurant item is clicked.
     */
    public interface OnRestaurantClickListener
    {
        /**
         * Called when a restaurant item has been clicked.
         *
         * @param restaurant The {@link Restaurant} object that was clicked.
         */
        void onRestaurantClick(Restaurant restaurant);
    }

    /**
     * Constructs a new {@code RestaurantDiscoveryAdapter}.
     *
     * @param restaurants The initial list of {@link Restaurant} objects to display.
     * @param context     The current context.
     * @param listener    The listener that will handle item clicks.
     */
    public RestaurantDiscoveryAdapter(List<Restaurant> restaurants, Context context, OnRestaurantClickListener listener)
    {
        this.restaurants = restaurants;
        this.context = context;
        this.listener = listener;
    }

    /**
     * Updates the data in the adapter with a new list of restaurants.
     * Clears the existing list and adds all items from the new list, then notifies
     * the adapter that the data set has changed.
     *
     * @param newRestaurants The new list of {@link Restaurant} objects to display.
     */
    public void updateData(List<Restaurant> newRestaurants)
    {
        this.restaurants.clear();
        this.restaurants.addAll(newRestaurants);
        notifyDataSetChanged();
    }

    /**
     * Called when RecyclerView needs a new {@link ViewHolder} of the given type to represent
     * an item.
     *
     * @param parent   The ViewGroup into which the new View will be added after it is bound to
     *                 an adapter position.
     * @param viewType The view type of the new View.
     * @return A new ViewHolder that holds a View of the given view type.
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(context).inflate(R.layout.item_discovery_restaurant, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Called by RecyclerView to display the data at the specified position. This method should
     * update the contents of the {@link ViewHolder#itemView} to reflect the item at the given
     * position.
     * It sets the restaurant's name (defaulting to "Unknown" if null), address (defaulting to empty if null),
     * average rating (formatted to one decimal place, or "N/A" if rating is not positive),
     * and loads the restaurant's image using Glide with placeholders.
     * It also formats and displays the price level and joins the tags into a comma-separated string.
     * An OnClickListener is set on the itemView to trigger {@link OnRestaurantClickListener#onRestaurantClick(Restaurant)}
     * if the listener is not null.
     *
     * @param holder   The ViewHolder which should be updated to represent the contents of the
     *                 item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position)
    {
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
        if(tags != null && !tags.isEmpty())
        {
            holder.restaurantTags.setText(joinTags(tags));
        } else
        {
            holder.restaurantTags.setText(""); // Set to empty if no tags
        }

        holder.itemView.setOnClickListener(v ->
        {
            if(listener != null)
            {
                listener.onRestaurantClick(restaurant);
            }
        });
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of items in this adapter.
     */
    @Override
    public int getItemCount()
    {
        return restaurants.size();
    }

    /**
     * ViewHolder class for displaying a single {@link Restaurant} item in the discovery list.
     * It holds references to the UI components within the item's layout,
     * such as ImageView for the restaurant image and TextViews for name, address, rating, etc.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder
    {
        /**
         * ImageView to display the image of the restaurant.
         */
        ImageView restaurantImage;
        /**
         * TextView to display the name of the restaurant.
         */
        TextView restaurantName;
        /**
         * TextView to display the address of the restaurant.
         */
        TextView restaurantAddress;
        /**
         * TextView to display the average rating of the restaurant.
         */
        TextView restaurantRating;
        /**
         * TextView to display the price level of the restaurant (e.g., "$", "$$").
         */
        TextView restaurantPriceLevel;
        /**
         * TextView to display the tags associated with the restaurant.
         */
        TextView restaurantTags;

        /**
         * Constructs a new {@code ViewHolder}.
         * Initializes the UI components by finding them in the itemView.
         *
         * @param itemView The view that this ViewHolder will manage, representing a single restaurant item.
         */
        public ViewHolder(@NonNull View itemView)
        {
            super(itemView);
            restaurantImage = itemView.findViewById(R.id.restaurantImage);
            restaurantName = itemView.findViewById(R.id.restaurantName);
            restaurantAddress = itemView.findViewById(R.id.restaurantAddress);
            restaurantRating = itemView.findViewById(R.id.restaurantRating);
            restaurantPriceLevel = itemView.findViewById(R.id.restaurantPriceLevel);
            restaurantTags = itemView.findViewById(R.id.restaurantTags);
        }
    }

    /**
     * Helper method to convert a numeric price level into a displayable string (e.g., "$", "$$", "$$$").
     * If the price level is zero or negative, it returns "Price: N/A".
     * Otherwise, it returns "Price: " followed by a number of "$" characters equal to the price level.
     *
     * @param priceLevel The numeric price level (e.g., 1, 2, 3).
     * @return A string representation of the price level.
     */
    private String getPriceLevelText(int priceLevel)
    {
        if(priceLevel <= 0) return "Price: N/A";
        StringBuilder sb = new StringBuilder("Price: ");
        for(int i = 0; i < priceLevel; i++)
        {
            sb.append("$");
        }
        return sb.toString();
    }

    /**
     * Helper method to join a list of tags into a single comma-separated string.
     *
     * @param tags A list of string tags.
     * @return A single string with tags joined by ", ", or an empty string if the list is null or empty.
     */
    private String joinTags(List<String> tags)
    {
        // The behavior of String.join for null or empty list is to return an empty string or just the delimiter if only one element.
        // This implementation is fine as is.
        return String.join(", ", tags);
    }
}