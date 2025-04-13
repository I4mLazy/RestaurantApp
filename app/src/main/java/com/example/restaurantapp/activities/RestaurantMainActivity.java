package com.example.restaurantapp.activities;

import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.restaurantapp.R;
import com.example.restaurantapp.fragments.ManageMenuFragment;
import com.example.restaurantapp.fragments.RestaurantSettingsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class RestaurantMainActivity extends AppCompatActivity
{
    private BottomNavigationView bottomNavMenu;
    private Fragment currentFragment;
    private final String MANAGE_MENU_FRAGMENT_TAG = "manage_menu_fragment";
    private final String RESTAURANT_SETTINGS_FRAGMENT_TAG = "restaurant_settings_fragment";
    private String currentFragmentTag = MANAGE_MENU_FRAGMENT_TAG;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant_main);

        bottomNavMenu = findViewById(R.id.bottom_navigation);

        // Handle back press behavior
        setupBackPressHandler();

        // Set default fragment
        if(savedInstanceState == null)
        {
            currentFragment = new ManageMenuFragment();
            currentFragmentTag = MANAGE_MENU_FRAGMENT_TAG;
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, currentFragment)
                    .commit();
        } else
        {
            // Restore current fragment from saved state
            currentFragmentTag = savedInstanceState.getString("currentFragmentTag", MANAGE_MENU_FRAGMENT_TAG);
            currentFragment = getSupportFragmentManager().findFragmentByTag(currentFragmentTag);
        }

        // Set the default button as selected (ManageMenuFragment)
        updateBottomNavFromTag(currentFragmentTag);

        bottomNavMenu.setOnItemSelectedListener(item ->
        {
            Fragment selectedFragment = null;
            String tag = null;

            int itemId = item.getItemId();
            if(itemId == R.id.nav_settings)
            {
                selectedFragment = new RestaurantSettingsFragment();
                tag = RESTAURANT_SETTINGS_FRAGMENT_TAG;
            } else if(itemId == R.id.nav_menu)
            {
                selectedFragment = new ManageMenuFragment();
                tag = MANAGE_MENU_FRAGMENT_TAG;
            }

            // Prevent selecting the same fragment twice
            if(selectedFragment != null && !tag.equals(currentFragmentTag))
            {
                currentFragmentTag = tag;
                currentFragment = selectedFragment;
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment, tag)
                        .commit();
            }

            return true;
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putString("currentFragmentTag", currentFragmentTag);
    }

    private void setupBackPressHandler()
    {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true)
        {
            @Override
            public void handleOnBackPressed()
            {
                // If we're not already on the Manage Menu fragment, go back to it
                if(!(currentFragment instanceof ManageMenuFragment))
                {
                    // Set the bottom navigation selection to Manage Menu
                    bottomNavMenu.setSelectedItemId(R.id.nav_menu);
                } else
                {
                    // If already on Manage Menu, exit the app
                    setEnabled(false);
                    onBackPressed();
                }
            }
        });
    }

    private void updateBottomNavFromTag(String tag)
    {
        if(RESTAURANT_SETTINGS_FRAGMENT_TAG.equals(tag))
        {
            bottomNavMenu.setSelectedItemId(R.id.nav_settings);
        } else if(MANAGE_MENU_FRAGMENT_TAG.equals(tag))
        {
            bottomNavMenu.setSelectedItemId(R.id.nav_menu);
        }
    }
}
