package com.example.restaurantapp.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.restaurantapp.models.Restaurant;

public class RestaurantSelectionViewModel extends ViewModel {
    private final MutableLiveData<Restaurant> selectedRestaurant = new MutableLiveData<>();

    public void selectRestaurant(Restaurant restaurant) {
        selectedRestaurant.setValue(restaurant);
    }

    public LiveData<Restaurant> getSelectedRestaurant() {
        return selectedRestaurant;
    }
}
