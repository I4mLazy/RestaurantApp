package com.example.restaurantapp.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.restaurantapp.models.Restaurant;

/**
 * A {@link ViewModel} subclass designed to hold and manage the currently selected or active {@link Restaurant}.
 * This allows different fragments or UI components to share and observe the current restaurant data
 * in a lifecycle-aware manner. For example, when a restaurant is selected from a list, its details
 * can be displayed in another fragment by observing this ViewModel.
 * It also provides a method to clear the current restaurant selection.
 */
public class RestaurantViewModel extends ViewModel
{

    /**
     * {@link MutableLiveData} that holds the currently selected or active {@link Restaurant}.
     * This is private to ensure that only this ViewModel can modify its value,
     * while external components can observe it via the exposed {@link LiveData}.
     */
    private final MutableLiveData<Restaurant> currentRestaurant = new MutableLiveData<>();

    /**
     * Sets the currently active or selected restaurant.
     * This method should be called when a user selects a restaurant in the UI or when
     * the context of the current restaurant changes.
     * It updates the value of the internal {@link MutableLiveData}, which will in turn
     * notify any active observers.
     *
     * @param restaurant The {@link Restaurant} object that is now current.
     */
    public void setCurrentRestaurant(Restaurant restaurant)
    {
        currentRestaurant.setValue(restaurant);
    }

    /**
     * Returns a {@link LiveData} object that can be observed to get updates
     * whenever the current restaurant changes.
     * This allows UI components (like Fragments or Activities) to react to changes
     * in the currently active restaurant.
     *
     * @return A {@link LiveData} instance containing the current {@link Restaurant}.
     * The value can be null if no restaurant is currently set or if it has been cleared.
     */
    public LiveData<Restaurant> getCurrentRestaurant()
    {
        return currentRestaurant;
    }

    /**
     * Clears the currently selected or active restaurant by setting its value to null.
     * This will notify any observers that there is no longer a current restaurant selected.
     */
    public void clear()
    {
        currentRestaurant.setValue(null);
    }
}