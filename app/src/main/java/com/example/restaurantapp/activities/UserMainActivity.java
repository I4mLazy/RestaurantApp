package com.example.restaurantapp.activities;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Build;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.restaurantapp.R;
import com.example.restaurantapp.fragments.DiscoveryFragment;
import com.example.restaurantapp.fragments.GmapsFragment;
import com.example.restaurantapp.fragments.ProfileFragment;
import com.example.restaurantapp.fragments.ReservationsTabLayoutFragment;
import com.example.restaurantapp.fragments.UpcomingReservationsFragment;
import com.example.restaurantapp.utils.SettingsUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

import android.Manifest;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class UserMainActivity extends AppCompatActivity
{

    private static final String TAG = "UserMainActivity";
    private BottomNavigationView bottomNavMenu;
    private final String DISCOVERY_FRAGMENT_TAG = "discovery_fragment";
    private final String GMAPS_FRAGMENT_TAG = "gmaps_fragment";
    private final String PROFILE_FRAGMENT_TAG = "profile_fragment";
    private final String RESERVATIONS_TAB_LAYOUT_FRAGMENT_TAG = "reservations_tab_layout_fragment";
    private String currentFragmentTag = DISCOVERY_FRAGMENT_TAG;

    private View rootView;

    private ActivityResultLauncher<String[]> permissionLauncher;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        SettingsUtils.loadUserSettings(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_main);

        rootView = findViewById(R.id.fragmentContainer);
        bottomNavMenu = findViewById(R.id.bottom_nav_menu);

        // Initialize permission launchers
        setupPermissionLaunchers();

        // Handle back press with the new API
        setupBackPressHandler();

        if(savedInstanceState == null)
        {
            // First time initialization - load the default fragment directly
            loadFragment(new DiscoveryFragment(), DISCOVERY_FRAGMENT_TAG);
            currentFragmentTag = DISCOVERY_FRAGMENT_TAG;
        } else
        {
            currentFragmentTag = savedInstanceState.getString("currentFragmentTag", DISCOVERY_FRAGMENT_TAG);
        }

        updateBottomNavFromTag(currentFragmentTag);

        setupBottomNavigation();

        requestRequiredPermissions();
    }

    private void setupPermissionLaunchers()
    {
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result ->
                {
                    boolean locationGranted = result.containsKey(Manifest.permission.ACCESS_FINE_LOCATION) && result.get(Manifest.permission.ACCESS_FINE_LOCATION)
                            || result.containsKey(Manifest.permission.ACCESS_COARSE_LOCATION) && result.get(Manifest.permission.ACCESS_COARSE_LOCATION);

                    boolean notificationsGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                            || result.getOrDefault(Manifest.permission.POST_NOTIFICATIONS, false);

                    List<String> denied = new ArrayList<>();
                    if(!locationGranted) denied.add("Location");
                    if(!notificationsGranted) denied.add("Notifications");

                    if(!denied.isEmpty())
                    {
                        Log.w("Permissions", "Denied: " + TextUtils.join(", ", denied));
                    }
                }
        );
    }


    private void setupBackPressHandler()
    {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true)
        {
            @Override
            public void handleOnBackPressed()
            {
                // Always go to Discovery screen when back button is pressed
                if(!(getSupportFragmentManager().findFragmentById(R.id.fragmentContainer) instanceof DiscoveryFragment))
                {
                    // Set the bottom navigation selection to Discovery
                    bottomNavMenu.setSelectedItemId(R.id.Discovery);
                } else
                {
                    // If already on Discovery, exit the app
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void setupBottomNavigation()
    {
        bottomNavMenu.setOnItemSelectedListener(item ->
        {
            int itemId = item.getItemId();
            if(itemId == R.id.Gmaps)
            {
                switchFragment(GMAPS_FRAGMENT_TAG);
                return true;
            } else if(itemId == R.id.Profile)
            {
                switchFragment(PROFILE_FRAGMENT_TAG);
                return true;
            } else if(itemId == R.id.Discovery)
            {
                switchFragment(DISCOVERY_FRAGMENT_TAG);
                return true;
            } else if(itemId == R.id.Reservations)
            {
                switchFragment(RESERVATIONS_TAB_LAYOUT_FRAGMENT_TAG);
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putString("currentFragmentTag", currentFragmentTag);
    }

    private void updateBottomNavFromTag(String tag)
    {
        if(GMAPS_FRAGMENT_TAG.equals(tag))
        {
            bottomNavMenu.setSelectedItemId(R.id.Gmaps);
        } else if(PROFILE_FRAGMENT_TAG.equals(tag))
        {
            bottomNavMenu.setSelectedItemId(R.id.Profile);
        } else if(RESERVATIONS_TAB_LAYOUT_FRAGMENT_TAG.equals(tag))
        {
            bottomNavMenu.setSelectedItemId(R.id.Reservations);
        } else
        {
            bottomNavMenu.setSelectedItemId(R.id.Discovery);
        }
    }

    private void switchFragment(String fragmentTag)
    {
        // Check if we're already on this fragment
        if(fragmentTag.equals(currentFragmentTag))
        {
            return;
        }

        // Create the fragment based on the tag
        Fragment fragment;
        switch(fragmentTag)
        {
            case GMAPS_FRAGMENT_TAG:
                fragment = new GmapsFragment();
                break;
            case PROFILE_FRAGMENT_TAG:
                fragment = new ProfileFragment();
                break;
            case RESERVATIONS_TAB_LAYOUT_FRAGMENT_TAG:
                fragment = new ReservationsTabLayoutFragment();
                break;
            case DISCOVERY_FRAGMENT_TAG:
            default:
                fragment = new DiscoveryFragment();
                break;
        }

        loadFragment(fragment, fragmentTag);
    }

    private void loadFragment(Fragment fragment, String tag)
    {
        // Update current fragment tag
        currentFragmentTag = tag;

        // Begin transaction
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);

        // Replace without adding to back stack
        transaction.replace(R.id.fragmentContainer, fragment, tag)
                .commit();
    }

    private void requestRequiredPermissions()
    {
        List<String> permissionsToRequest = new ArrayList<>();

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
        {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        if(!permissionsToRequest.isEmpty())
        {
            permissionLauncher.launch(permissionsToRequest.toArray(new String[0]));
        }
    }
}