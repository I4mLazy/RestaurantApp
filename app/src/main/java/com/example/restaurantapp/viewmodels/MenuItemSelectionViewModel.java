package com.example.restaurantapp.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.restaurantapp.models.MenuItem;

public class MenuItemSelectionViewModel extends ViewModel
{

    // LiveData to hold the selected MenuItem
    private final MutableLiveData<MenuItem> selectedMenuItem = new MutableLiveData<>();

    // Call this method when a menu item is selected
    public void selectMenuItem(MenuItem menuItem) {
        selectedMenuItem.setValue(menuItem);
    }

    // Other fragments can observe this LiveData to react when a menu item is selected
    public LiveData<MenuItem> getSelectedMenuItem() {
        return selectedMenuItem;
    }
}