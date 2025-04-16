package com.example.restaurantapp.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.restaurantapp.models.Restaurant;

public class RestaurantViewModel extends ViewModel
{

    private final MutableLiveData<Restaurant> currentRestaurant = new MutableLiveData<>();

    public void setCurrentRestaurant(Restaurant restaurant)
    {
        currentRestaurant.setValue(restaurant);
    }

    public LiveData<Restaurant> getCurrentRestaurant()
    {
        return currentRestaurant;
    }

    public void clear()
    {
        currentRestaurant.setValue(null);
    }
}

