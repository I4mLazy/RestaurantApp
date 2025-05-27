package com.example.restaurantapp.adapters;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.restaurantapp.R;
import com.example.restaurantapp.models.Restaurant;
import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.GeoPoint;

import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying a list of {@link Restaurant} objects in a RecyclerView,
 * typically used to show search results.
 * Each item displays the restaurant's image, name, average rating, tags, and calculated distance
 * from the user's current location. It also provides a navigation button and handles click events
 * on both the item itself and the navigation button.
 */
public class RestaurantSearchResultsAdapter
        extends RecyclerView.Adapter<RestaurantSearchResultsAdapter.ViewHolder>
{

    /**
     * Interface definition for callbacks to be invoked when an item or its navigation button is clicked.
     */
    public interface OnItemClickListener
    {
        /**
         * Called when a restaurant item has been clicked.
         *
         * @param restaurant The {@link Restaurant} object that was clicked.
         */
        void onItemClick(Restaurant restaurant);

        /**
         * Called when the navigation button of a restaurant item has been clicked.
         *
         * @param restaurant The {@link Restaurant} object associated with the clicked navigation button.
         */
        void onNavigateClick(Restaurant restaurant);
    }

    /**
     * The list of {@link Restaurant} objects to be displayed.
     */
    private List<Restaurant> restaurants;
    /**
     * The context in which the adapter is operating.
     */
    private final Context context;
    /**
     * Listener for click events on items and navigation buttons.
     */
    private final OnItemClickListener listener;

    /**
     * Constructs a new {@code RestaurantSearchResultsAdapter}.
     *
     * @param restaurants The initial list of {@link Restaurant} objects to display.
     * @param context     The current context.
     * @param listener    The listener that will handle item and navigation clicks.
     */
    public RestaurantSearchResultsAdapter(
            List<Restaurant> restaurants,
            Context context,
            OnItemClickListener listener)
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
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_restaurant, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Called by RecyclerView to display the data at the specified position. This method should
     * update the contents of the {@link ViewHolder#itemView} to reflect the item at the given
     * position.
     * It sets the restaurant's name (defaulting to "Unknown" if null), average rating (formatted to
     * one decimal place, or "N/A" if not positive), and tags (using {@code toString()} on the list,
     * or "No tags" if null). The restaurant's image is loaded using Glide.
     * The distance to the restaurant is calculated asynchronously via {@link #getDistance(Restaurant, DistanceCallback)}
     * and displayed as "X km" or "N/A".
     * Click listeners are set for the entire item view (triggering {@link OnItemClickListener#onItemClick(Restaurant)})
     * and the navigation button (triggering {@link OnItemClickListener#onNavigateClick(Restaurant)}).
     *
     * @param holder   The ViewHolder which should be updated to represent the contents of the
     *                 item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position)
    {
        Restaurant restaurant = restaurants.get(position);

        // Bind the fields
        holder.nameTextView.setText(restaurant.getName() != null ? restaurant.getName() : "Unknown");
        Double rating = restaurant.getAverageRating();
        holder.ratingTextView.setText(rating > 0 ? String.format(Locale.getDefault(), "%.1f", rating) : "N/A");

        holder.tagsTextView.setText(restaurant.getTags() != null
                ? restaurant.getTags().toString() // Note: This will include brackets and commas from List.toString()
                : "No tags");

        Glide.with(context)
                .load(restaurant.getImageURL())
                .placeholder(R.drawable.image_placeholder)
                .error(R.drawable.image_placeholder)
                .into(holder.restaurantImage);

        // Bind distance (if needed)
        if(restaurant.getLocation() != null)
        {
            getDistance(restaurant, distance ->
            {
                if(distance != -1)
                {
                    holder.distanceTextView.setText(distance + " km");
                } else
                {
                    holder.distanceTextView.setText("N/A");
                }
            });
        } else
        {
            holder.distanceTextView.setText("N/A"); // If restaurant location is null
        }

        // Item click listener
        holder.itemView.setOnClickListener(v -> listener.onItemClick(restaurant));

        // Navigation button click listener
        holder.navButton.setOnClickListener(v -> listener.onNavigateClick(restaurant));
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
     * ViewHolder class for displaying a single {@link Restaurant} item in the search results.
     * It holds references to the UI components within the item's layout,
     * such as ImageView for the restaurant image, TextViews for name, rating, tags, distance,
     * and an ImageButton for navigation.
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
        TextView nameTextView;
        /**
         * TextView to display the average rating of the restaurant.
         */
        TextView ratingTextView;
        /**
         * TextView to display the tags associated with the restaurant.
         */
        TextView tagsTextView;
        /**
         * TextView to display the calculated distance to the restaurant.
         */
        TextView distanceTextView;
        /**
         * ImageButton to initiate navigation to the restaurant.
         */
        ImageButton navButton;

        /**
         * Constructs a new {@code ViewHolder}.
         * Initializes the UI components by finding them in the itemView.
         *
         * @param itemView The view that this ViewHolder will manage, representing a single search result item.
         */
        public ViewHolder(@NonNull View itemView)
        {
            super(itemView);
            restaurantImage = itemView.findViewById(R.id.restaurantImage);
            nameTextView = itemView.findViewById(R.id.restaurantName);
            ratingTextView = itemView.findViewById(R.id.restaurantRating);
            tagsTextView = itemView.findViewById(R.id.restaurantTags);
            distanceTextView = itemView.findViewById(R.id.restaurantDistance);
            navButton = itemView.findViewById(R.id.navButton);
        }
    }

    /**
     * Asynchronously calculates the distance from the user's current location to the given restaurant.
     * It uses {@link FusedLocationProviderClient} to get the current location.
     * Location permissions ({@link Manifest.permission#ACCESS_FINE_LOCATION} and
     * {@link Manifest.permission#ACCESS_COARSE_LOCATION}) are checked before attempting to get the location.
     * If permissions are granted and the current location is successfully obtained, it calculates
     * the distance to the restaurant's {@link GeoPoint} location. The distance is converted to kilometers,
     * rounded to one decimal place, and passed to the {@link DistanceCallback}.
     * If permissions are not granted, or if fetching the current location fails, or if the restaurant's
     * location is null, the callback receives -1.
     *
     * @param restaurant The {@link Restaurant} to calculate the distance to.
     * @param callback   The {@link DistanceCallback} to be invoked with the calculated distance or -1 on failure.
     */
    public void getDistance(Restaurant restaurant, DistanceCallback callback)
    {
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);

        if(ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {

            CurrentLocationRequest currentLocationRequest = new CurrentLocationRequest.Builder()
                    .setDurationMillis(5000) // Request location for up to 5 seconds
                    .setMaxUpdateAgeMillis(0)  // Request a fresh location
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    .build();

            Task<Location> locationTask = fusedLocationClient.getCurrentLocation(currentLocationRequest, null);

            locationTask.addOnCompleteListener(task ->
            {
                if(task.isSuccessful() && task.getResult() != null)
                {
                    Location location = task.getResult();
                    LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    GeoPoint geoPoint = restaurant.getLocation(); // Assumes restaurant.getLocation() is not null here, checked in onBindViewHolder
                    if(geoPoint != null)
                    { // Defensive check
                        LatLng restaurantLatLng = new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude());

                        float[] results = new float[1];
                        Location.distanceBetween(
                                currentLatLng.latitude, currentLatLng.longitude,
                                restaurantLatLng.latitude, restaurantLatLng.longitude,
                                results
                        );
                        // Convert meters to kilometers and round to one decimal place
                        double distance = Math.round((results[0] / 1000.0) * 10.0) / 10.0;
                        callback.onDistanceCalculated(distance);
                    } else
                    {
                        callback.onDistanceCalculated(-1); // Restaurant GeoPoint is null
                    }
                } else
                {
                    // Handle failure to get current location
                    callback.onDistanceCalculated(-1);
                }
            });
        } else
        {
            // Handle permission not granted case
            callback.onDistanceCalculated(-1);
        }
    }

    /**
     * Interface definition for a callback to be invoked when the distance calculation is complete.
     */
    public interface DistanceCallback
    {
        /**
         * Called when the distance to a restaurant has been calculated or if calculation failed.
         *
         * @param distance The calculated distance in kilometers, or -1 if the calculation failed
         *                 (e.g., permissions denied, location unavailable, restaurant location null).
         */
        void onDistanceCalculated(double distance);
    }
}