package com.example.restaurantapp.adapters;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

public class RestaurantSearchResultsAdapter extends RecyclerView.Adapter<RestaurantSearchResultsAdapter.ViewHolder>
{

    private List<Restaurant> restaurants;
    private Context context;

    public RestaurantSearchResultsAdapter(List<Restaurant> restaurants, Context context)
    {
        this.restaurants = restaurants;
        this.context = context;
    }

    // Method to update data
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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_restaurant, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position)
    {
        Restaurant restaurant = restaurants.get(position);

        holder.nameTextView.setText(restaurant.getName() != null ? restaurant.getName() : "Unknown");
        Double rating = restaurant.getRating();
        holder.ratingTextView.setText(rating != null ? String.valueOf(restaurant.getRating()) : "N/A");
        holder.tagsTextView.setText(restaurant.getTags() != null ? restaurant.getTags().toString() : "No tags");
        if (restaurant.getLocation() != null) {
            getDistance(restaurant, distance -> {
                holder.distanceTextView.setText(distance != -1 ? distance + " km" : "N/A");
            });
        } else {
            holder.distanceTextView.setText("N/A");
        }
        // Load restaurant image from URL using Glide
        Glide.with(context).load(restaurant.getImageURL())
                .placeholder(R.drawable.image_placeholder)
                .error(R.drawable.image_placeholder)
                .into(holder.restaurantImage);
    }

    @Override
    public int getItemCount()
    {
        return restaurants.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder
    {
        ImageView restaurantImage, starIcon;
        TextView nameTextView, ratingTextView, tagsTextView, distanceTextView;

        public ViewHolder(@NonNull View itemView)
        {
            super(itemView);
            restaurantImage = itemView.findViewById(R.id.restaurantImage);
            nameTextView = itemView.findViewById(R.id.restaurantName);
            ratingTextView = itemView.findViewById(R.id.restaurantRating);
            tagsTextView = itemView.findViewById(R.id.restaurantTags);
            distanceTextView = itemView.findViewById(R.id.restaurantDistance);
            starIcon = itemView.findViewById(R.id.starIcon);
        }
    }

    public void getDistance(Restaurant restaurant, DistanceCallback callback)
    {
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {

            CurrentLocationRequest currentLocationRequest = new CurrentLocationRequest.Builder()
                    .setDurationMillis(5000)
                    .setMaxUpdateAgeMillis(0)
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    .build();

            Task<Location> locationTask = fusedLocationClient.getCurrentLocation(currentLocationRequest, null);

            locationTask.addOnCompleteListener(task ->
            {
                if (task.isSuccessful() && task.getResult() != null)
                {
                    Location location = task.getResult();
                    LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    GeoPoint geoPoint = restaurant.getLocation();
                    LatLng restaurantLatLng = new LatLng(geoPoint.getLatitude(),geoPoint.getLongitude());

                    float[] results = new float[1];
                    Location.distanceBetween(
                            currentLatLng.latitude, currentLatLng.longitude,
                            restaurantLatLng.latitude, restaurantLatLng.longitude,
                            results
                    );
                    double distance = Math.round((results[0] / 1000.0) * 10.0) / 10.0;
                    callback.onDistanceCalculated(distance);
                } else
                {
                    // Handle failure case
                    callback.onDistanceCalculated(-1);
                }
            });
        } else
        {
            // Handle permission not granted case
            callback.onDistanceCalculated(-1);
        }
    }


    public interface DistanceCallback
    {
        void onDistanceCalculated(double distance);
    }

}