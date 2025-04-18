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
import com.example.restaurantapp.fragments.ManageMenuFragment;
import com.example.restaurantapp.fragments.ReservationsTabLayoutFragment;
import com.example.restaurantapp.fragments.RestaurantSettingsFragment;
import com.example.restaurantapp.fragments.RestaurantInfoFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

import android.Manifest;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RestaurantMainActivity extends AppCompatActivity
{
    private static final String TAG = "RestaurantMainActivity";
    private BottomNavigationView bottomNavMenu;
    private final String MANAGE_MENU_FRAGMENT_TAG = "manage_menu_fragment";
    private final String RESTAURANT_SETTINGS_FRAGMENT_TAG = "restaurant_settings_fragment";
    private final String RESTAURANT_INFO_FRAGMENT_TAG = "restaurant_info_fragment";
    private final String RESERVATIONS_TAB_LAYOUT_FRAGMENT_TAG = "reservations_tab_layout_fragment";
    private String currentFragmentTag = MANAGE_MENU_FRAGMENT_TAG;

    private View rootView;

    // For Android 13+ permissions
    private ActivityResultLauncher<String[]> requestMultiplePermissionsLauncher;
    private ActivityResultLauncher<String> requestSinglePermissionLauncher;
    private boolean isTransactionInProgress = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private String pendingFragmentTag = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_restaurant_main);

        rootView = findViewById(R.id.fragment_container);
        bottomNavMenu = findViewById(R.id.bottom_navigation);

        // Initialize permission launchers
        setupPermissionLaunchers();

        // Handle back press with the new API
        setupBackPressHandler();

        if(savedInstanceState == null)
        {
            // First time initialization - load the default fragment directly
            loadFragment(new ManageMenuFragment(), MANAGE_MENU_FRAGMENT_TAG);
            currentFragmentTag = MANAGE_MENU_FRAGMENT_TAG;
        } else
        {
            // Restore current fragment from saved state
            currentFragmentTag = savedInstanceState.getString("currentFragmentTag", MANAGE_MENU_FRAGMENT_TAG);
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
                // Always go to Manage Menu screen when back button is pressed
                if(!(getSupportFragmentManager().findFragmentById(R.id.fragment_container) instanceof ManageMenuFragment))
                {
                    // Set the bottom navigation selection to Manage Menu
                    bottomNavMenu.setSelectedItemId(R.id.nav_menu);
                } else
                {
                    // If already on Manage Menu, exit the app
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
            if(itemId == R.id.nav_settings)
            {
                switchFragment(RESTAURANT_SETTINGS_FRAGMENT_TAG);
                return true;
            } else if(itemId == R.id.nav_reservations)
            {
                switchFragment(RESERVATIONS_TAB_LAYOUT_FRAGMENT_TAG);
                return true;
            } else if(itemId == R.id.nav_menu)
            {
                switchFragment(MANAGE_MENU_FRAGMENT_TAG);
                return true;
            } else if(itemId == R.id.nav_info)
            {
                switchFragment(RESTAURANT_INFO_FRAGMENT_TAG);
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
        if(RESTAURANT_SETTINGS_FRAGMENT_TAG.equals(tag))
        {
            bottomNavMenu.setSelectedItemId(R.id.nav_settings);
        } else if(RESERVATIONS_TAB_LAYOUT_FRAGMENT_TAG.equals(tag))
        {
            bottomNavMenu.setSelectedItemId(R.id.nav_reservations);
        }else if(RESTAURANT_INFO_FRAGMENT_TAG.equals(tag))
        {
            bottomNavMenu.setSelectedItemId(R.id.nav_info);
        } else
        {
            bottomNavMenu.setSelectedItemId(R.id.nav_menu);
        }
    }

    private void switchFragment(String fragmentTag)
    {
        // Check if we're already on this fragment
        if(fragmentTag.equals(currentFragmentTag))
        {
            return;
        }

        // If a transaction is in progress, store this as the pending request
        if(isTransactionInProgress)
        {
            pendingFragmentTag = fragmentTag;
            return;
        }

        // Set flag to prevent concurrent transactions
        isTransactionInProgress = true;

        // Create the fragment based on the tag
        Fragment fragment;
        switch(fragmentTag)
        {
            case RESTAURANT_SETTINGS_FRAGMENT_TAG:
                fragment = new RestaurantSettingsFragment();
                break;
            case RESERVATIONS_TAB_LAYOUT_FRAGMENT_TAG:
                fragment = new ReservationsTabLayoutFragment();
                break;
            case RESTAURANT_INFO_FRAGMENT_TAG:
                fragment = new RestaurantInfoFragment();
                break;
            case MANAGE_MENU_FRAGMENT_TAG:
            default:
                fragment = new ManageMenuFragment();
                break;
        }

        final Fragment finalFragment = fragment;
        final String finalTag = fragmentTag;

        // Update the UI to reflect the pending change immediately
        updateBottomNavFromTag(finalTag);

        // Use post to ensure it runs on the UI thread at an appropriate time
        handler.post(() ->
        {
            try
            {
                if(!isFinishing() && !isDestroyed())
                {
                    // Update current fragment tag before the transaction
                    currentFragmentTag = finalTag;

                    // Begin transaction
                    FragmentManager fragmentManager = getSupportFragmentManager();
                    FragmentTransaction transaction = fragmentManager.beginTransaction();
                    transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);

                    // Replace without adding to back stack
                    transaction.replace(R.id.fragment_container, finalFragment, finalTag)
                            .commitAllowingStateLoss();
                }
            } catch(Exception e)
            {
                Log.e(TAG, "Error switching fragments: " + e.getMessage());
            } finally
            {
                // Reset the flag after a small delay to ensure the transaction completes
                handler.postDelayed(() ->
                {
                    isTransactionInProgress = false;

                    // Check if there's a pending fragment switch request
                    if(pendingFragmentTag != null)
                    {
                        String nextTag = pendingFragmentTag;
                        pendingFragmentTag = null;
                        switchFragment(nextTag); // Process the pending request
                    }
                }, 250);
            }
        });
    }

    private void loadFragment(Fragment fragment, String tag)
    {
        try
        {
            // Update current fragment tag
            currentFragmentTag = tag;

            // Begin transaction
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);

            // Replace without adding to back stack and use commitAllowingStateLoss
            transaction.replace(R.id.fragment_container, fragment, tag)
                    .commitAllowingStateLoss();  // This is key for preventing IllegalStateException
        } catch(Exception e)
        {
            Log.e(TAG, "Error switching fragments: " + e.getMessage());
            isTransactionInProgress = false;  // Reset in case of exception
        }
    }

    private void requestRequiredPermissions()
    {
        List<String> permissionsToRequest = new ArrayList<>();

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
        boolean needsNotification = permissions.contains(Manifest.permission.POST_NOTIFICATIONS);

        StringBuilder message = new StringBuilder("This app needs ");

        if(needsNotification)
        {
            message.append("notification permission to alert you about new orders, customer messages, and important restaurant updates");
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

        // Add specific fragment handling code if needed
        // For example, if a restaurant fragment needs to know about permission changes
    }
}