package com.example.restaurantapp.activities;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Build;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.restaurantapp.R;
import com.example.restaurantapp.fragments.DiscoveryFragment;
import com.example.restaurantapp.fragments.GmapsFragment;
import com.example.restaurantapp.fragments.ProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import android.Manifest;
import java.util.ArrayList;
import java.util.List;

public class Main extends AppCompatActivity
{

    BottomNavigationView bottomNavMenu;
    private static final int PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_NOTIFICATION = 2;
    private final String DISCOVERY_FRAGMENT_TAG = "discovery_fragment";
    private final String GMAPS_FRAGMENT_TAG = "gmaps_fragment";
    private final String PROFILE_FRAGMENT_TAG = "profile_fragment";
    private String currentFragmentTag = DISCOVERY_FRAGMENT_TAG;

    // For Android 13+ notification permission
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Initialize permission launcher for Android 13+
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        // Notification permission granted
                    } else {
                        // Notification permission denied
                    }
                });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) ->
        {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        bottomNavMenu = findViewById(R.id.bottom_nav_menu);
        bottomNavMenu.setOnItemSelectedListener(item ->
        {
            int itemId = item.getItemId();
            if (itemId == R.id.Gmaps)
            {
                switchFragment(GMAPS_FRAGMENT_TAG);
            } else if (itemId == R.id.Profile)
            {
                switchFragment(PROFILE_FRAGMENT_TAG);
            } else if (itemId == R.id.Discovery)
            {
                switchFragment(DISCOVERY_FRAGMENT_TAG);
            }
            return true;
        });

        // Handle back press with the new API
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true)
        {
            @Override
            public void handleOnBackPressed()
            {
                // Always go to Discovery screen when back button is pressed
                if (!(getSupportFragmentManager().findFragmentById(R.id.fragmentContainer) instanceof DiscoveryFragment))
                {
                    // Set the bottom navigation selection to Discovery
                    bottomNavMenu.setSelectedItemId(R.id.Discovery);
                    // No need to call switchFragment here since setSelectedItemId will trigger
                    // the listener which will call switchFragment
                } else
                {
                    // If already on Discovery, exit the app
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
        // Initialize the first fragment if this is the first creation
        if (savedInstanceState == null)
        {
            // Load the default fragment
            switchFragment(DISCOVERY_FRAGMENT_TAG);
            bottomNavMenu.setSelectedItemId(R.id.Discovery);
        } else
        {
            // Restore current fragment from saved state
            currentFragmentTag = savedInstanceState.getString("currentFragmentTag", DISCOVERY_FRAGMENT_TAG);
            // Update the bottom navigation to match
            updateBottomNavFromTag(currentFragmentTag);
        }

        // Request permissions
        requestLocationPermission();
        requestNotificationPermission();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putString("currentFragmentTag", currentFragmentTag);
    }

    private void updateBottomNavFromTag(String tag)
    {
        if (GMAPS_FRAGMENT_TAG.equals(tag))
        {
            bottomNavMenu.setSelectedItemId(R.id.Gmaps);
        } else if (PROFILE_FRAGMENT_TAG.equals(tag))
        {
            bottomNavMenu.setSelectedItemId(R.id.Profile);
        } else
        {
            bottomNavMenu.setSelectedItemId(R.id.Discovery);
        }
    }

    private void switchFragment(String fragmentTag) {
        FragmentManager fragmentManager = getSupportFragmentManager();

        // Check if we're already on this fragment
        if (fragmentTag.equals(currentFragmentTag)) {
            return;
        }

        // Update current fragment tag
        currentFragmentTag = fragmentTag;

        // Begin transaction
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);

        // Create the fragment based on the tag
        Fragment fragment;
        switch (fragmentTag) {
            case GMAPS_FRAGMENT_TAG:
                fragment = new GmapsFragment();
                break;
            case PROFILE_FRAGMENT_TAG:
                fragment = new ProfileFragment();
                break;
            case DISCOVERY_FRAGMENT_TAG:
            default:
                fragment = new DiscoveryFragment();
                break;
        }

        // Replace without adding to back stack
        transaction.replace(R.id.fragmentContainer, fragment, fragmentTag)
                .commit();
    }

    private void requestLocationPermission()
    {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            List<String> permissions = new ArrayList<>();
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            // Also request coarse location as a fallback
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION))
            {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle("Location Permission Needed")
                        .setMessage("This app needs the Location permission to show nearby restaurants and provide navigation features.")
                        .setPositiveButton("OK", (dialogInterface, i) -> ActivityCompat.requestPermissions(
                                this, permissions.toArray(new String[0]),
                                PERMISSION_REQUEST_ACCESS_FINE_LOCATION))
                        .setNegativeButton("Cancel", null)
                        .create()
                        .show();
            } else
            {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]),
                        PERMISSION_REQUEST_ACCESS_FINE_LOCATION);
            }
        }
    }

    private void requestNotificationPermission() {
        // For Android 13+ (API level 33 and above)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {

                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.POST_NOTIFICATIONS)) {

                    // Show explanation dialog
                    new AlertDialog.Builder(this)
                            .setTitle("Notification Permission Needed")
                            .setMessage("This app needs notification permission to alert you about restaurant promotions, " +
                                    "order updates, and reservation confirmations.")
                            .setPositiveButton("OK", (dialog, which) ->
                                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS))
                            .setNegativeButton("Cancel", null)
                            .create()
                            .show();
                } else {
                    // Request directly
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                }
            }
        }
        // For Android 12 and below, notification permissions are granted by default
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_ACCESS_FINE_LOCATION)
        {
            boolean locationPermissionGranted = false;

            // Check if any location permission was granted
            for (int i = 0; i < permissions.length; i++) {
                if ((permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION) ||
                        permissions[i].equals(Manifest.permission.ACCESS_COARSE_LOCATION)) &&
                        grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    locationPermissionGranted = true;
                    break;
                }
            }

            if (locationPermissionGranted) {
                // Permission was granted.
                // You can now use location-related features.
            } else {
                // Permission denied.
                // You could disable location-related features, show an error message, etc.
                new AlertDialog.Builder(this)
                        .setTitle("Limited Functionality")
                        .setMessage("Without location permission, some features like finding nearby restaurants will not work.")
                        .setPositiveButton("OK", null)
                        .create()
                        .show();
            }
        }
    }
}