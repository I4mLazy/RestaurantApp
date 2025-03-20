package com.example.restaurantapp.activities;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.restaurantapp.R;
import com.example.restaurantapp.fragments.ManageMenuFragment;
import com.example.restaurantapp.fragments.RestaurantSettingsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class RestaurantMainActivity extends AppCompatActivity
{
    private BottomNavigationView bottomNavMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant_main);

        bottomNavMenu = findViewById(R.id.bottom_navigation);

        // Set default fragment
        if(savedInstanceState == null)
        {
            //getSupportFragmentManager().beginTransaction()
            //.replace(R.id.fragment_container, new RestaurantMenuFragment())
            //.commit();
        }

        bottomNavMenu.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener()
        {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item)
            {
                Fragment selectedFragment = null;

                int itemId = item.getItemId();
                if(itemId == R.id.nav_settings)
                {
                    selectedFragment = new RestaurantSettingsFragment();
                }/* else if(itemId == R.id.nav_orders)
                {
                    //selectedFragment = new RestaurantOrdersFragment();
                } */else if(itemId == R.id.nav_menu)
                {
                    selectedFragment = new ManageMenuFragment();
                }/* else if(itemId == R.id.nav_reservations)
                {
                    //selectedFragment = new RestaurantReservationsFragment();
                }*/

                if(selectedFragment != null)
                {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, selectedFragment)
                            .commit();
                }

                return true;
            }
        });
    }
}
