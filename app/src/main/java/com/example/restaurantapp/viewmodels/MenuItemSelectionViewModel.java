package com.example.restaurantapp.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.restaurantapp.models.MenuItem;

/**
 * A {@link ViewModel} subclass designed to hold and manage the currently selected {@link MenuItem}.
 * This allows different fragments or UI components to share and observe the selected menu item data
 * in a lifecycle-aware manner. When a menu item is selected in one part of the UI, other parts
 * observing this ViewModel can react to the change.
 */
public class MenuItemSelectionViewModel extends ViewModel
{

    /**
     * {@link MutableLiveData} that holds the currently selected {@link MenuItem}.
     * This is private to ensure that only this ViewModel can modify its value,
     * while external components can observe it via the exposed {@link LiveData}.
     */
    private final MutableLiveData<MenuItem> selectedMenuItem = new MutableLiveData<>();

    /**
     * Sets the currently selected menu item.
     * This method should be called when a user selects a menu item in the UI.
     * It updates the value of the internal {@link MutableLiveData}, which will in turn
     * notify any active observers.
     *
     * @param menuItem The {@link MenuItem} object that has been selected.
     */
    public void selectMenuItem(MenuItem menuItem)
    {
        selectedMenuItem.setValue(menuItem);
    }

    /**
     * Returns a {@link LiveData} object that can be observed to get updates
     * whenever the selected menu item changes.
     * This allows UI components (like Fragments or Activities) to react to selections
     * made elsewhere in the application.
     *
     * @return A {@link LiveData} instance containing the currently selected {@link MenuItem}.
     * The value can be null if no item has been selected yet or if it has been cleared.
     */
    public LiveData<MenuItem> getSelectedMenuItem()
    {
        return selectedMenuItem;
    }
}