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
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

import android.Manifest;
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

    // For Android 13+ permissions
    private ActivityResultLauncher<String[]> requestMultiplePermissionsLauncher;
    private ActivityResultLauncher<String> requestSinglePermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
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
            // Restore current fragment from saved state
            currentFragmentTag = savedInstanceState.getString("currentFragmentTag", DISCOVERY_FRAGMENT_TAG);
        }

        // Now set the correct item in bottom nav to match the current fragment
        updateBottomNavFromTag(currentFragmentTag);

        // Set up navigation listener AFTER initializing the first fragment
        setupBottomNavigation();

        // Request permissions with optimized approach
        requestRequiredPermissions();
    }

    private void setupPermissionLaunchers()
    {
        // For multiple permissions (Android 13+)
        requestMultiplePermissionsLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result ->
                {
                    StringBuilder grantedPermissions = new StringBuilder();
                    StringBuilder deniedPermissions = new StringBuilder();

                    for(Map.Entry<String, Boolean> entry : result.entrySet())
                    {
                        if(entry.getValue())
                        {
                            if(grantedPermissions.length() > 0) grantedPermissions.append(", ");
                            grantedPermissions.append(getPermissionFriendlyName(entry.getKey()));
                        } else
                        {
                            if(deniedPermissions.length() > 0) deniedPermissions.append(", ");
                            deniedPermissions.append(getPermissionFriendlyName(entry.getKey()));
                        }
                    }

                    // Show appropriate feedback based on results
                    if(deniedPermissions.length() > 0)
                    {
                        showPermissionFeedback(deniedPermissions.toString());
                    }

                    // Notify appropriate fragments about permission changes
                    notifyFragmentsAboutPermissionChanges(result);
                }
        );

        // For single permission requests
        requestSinglePermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted ->
                {
                    Log.d(TAG, "Single permission result: " + isGranted);
                    // Handle specific permission result if needed
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

        // Add location permissions
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED)
        {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        // Add notification permission for Android 13+
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        {
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED)
            {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        // If we have permissions to request, check if rationale is needed
        if(!permissionsToRequest.isEmpty())
        {
            boolean shouldShowRationale = false;

            for(String permission : permissionsToRequest)
            {
                if(ActivityCompat.shouldShowRequestPermissionRationale(this, permission))
                {
                    shouldShowRationale = true;
                    break;
                }
            }

            if(shouldShowRationale)
            {
                showPermissionRationale(permissionsToRequest);
            } else
            {
                // Request permissions directly
                requestMultiplePermissionsLauncher.launch(
                        permissionsToRequest.toArray(new String[0])
                );
            }
        }
    }

    private void showPermissionRationale(List<String> permissions)
    {
        boolean needsLocation = permissions.contains(Manifest.permission.ACCESS_FINE_LOCATION);
        boolean needsNotification = permissions.contains(Manifest.permission.POST_NOTIFICATIONS);

        StringBuilder message = new StringBuilder("This app needs ");

        if(needsLocation && needsNotification)
        {
            message.append("location permission to show nearby restaurants and notification permission for updates and promotions");
        } else if(needsLocation)
        {
            message.append("location permission to show nearby restaurants and provide navigation features");
        } else if(needsNotification)
        {
            message.append("notification permission to alert you about restaurant promotions, order updates, and reservation confirmations");
        }

        new AlertDialog.Builder(this)
                .setTitle("Permissions Needed")
                .setMessage(message.toString())
                .setPositiveButton("OK", (dialog, which) ->
                        requestMultiplePermissionsLauncher.launch(permissions.toArray(new String[0]))
                )
                .setNegativeButton("Cancel", (dialog, which) ->
                        showPermissionFeedback(permissions.stream()
                                .map(this::getPermissionFriendlyName)
                                .reduce((a, b) -> a + ", " + b)
                                .orElse("requested permissions"))
                )
                .create()
                .show();
    }

    private void showPermissionFeedback(String deniedPermissions)
    {
        Snackbar.make(
                rootView,
                "Limited functionality: " + deniedPermissions + " not available",
                Snackbar.LENGTH_LONG
        ).setAction("Settings", v ->
        {
            // Open app settings
            // Add intent to open settings if needed
        }).show();
    }

    private String getPermissionFriendlyName(String permission)
    {
        switch(permission)
        {
            case Manifest.permission.ACCESS_FINE_LOCATION:
            case Manifest.permission.ACCESS_COARSE_LOCATION:
                return "Location";
            case Manifest.permission.POST_NOTIFICATIONS:
                return "Notifications";
            default:
                return permission.substring(permission.lastIndexOf('.') + 1);
        }
    }

    private void notifyFragmentsAboutPermissionChanges(Map<String, Boolean> results)
    {
        // Get current fragment
        Fragment currentFragment = getSupportFragmentManager().findFragmentByTag(currentFragmentTag);

        // Check if the current fragment might need to know about permission changes
        if(currentFragment instanceof GmapsFragment &&
                (results.containsKey(Manifest.permission.ACCESS_FINE_LOCATION) ||
                        results.containsKey(Manifest.permission.ACCESS_COARSE_LOCATION)))
        {
            // Notify the fragment of permission changes
            // This would require implementing a method in the GmapsFragment
            // For example: ((GmapsFragment)currentFragment).onLocationPermissionResult(results);
        }
    }
}