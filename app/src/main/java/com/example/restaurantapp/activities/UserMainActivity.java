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

/**
 * The main activity for the user-facing side of the application.
 * This activity serves as a container for various fragments that allow users to discover restaurants,
 * view them on a map, manage their profile, and handle their reservations.
 * Navigation between these sections is facilitated by a {@link BottomNavigationView}.
 * The activity also handles runtime permission requests for location and notifications.
 */
public class UserMainActivity extends AppCompatActivity
{

    /**
     * Tag for logging purposes.
     */
    private static final String TAG = "UserMainActivity";
    /**
     * The bottom navigation view for switching between main sections of the user app.
     */
    private BottomNavigationView bottomNavMenu;
    /**
     * Tag for the {@link DiscoveryFragment}.
     */
    private final String DISCOVERY_FRAGMENT_TAG = "discovery_fragment";
    /**
     * Tag for the {@link GmapsFragment}.
     */
    private final String GMAPS_FRAGMENT_TAG = "gmaps_fragment";
    /**
     * Tag for the {@link ProfileFragment}.
     */
    private final String PROFILE_FRAGMENT_TAG = "profile_fragment";
    /**
     * Tag for the {@link ReservationsTabLayoutFragment}.
     */
    private final String RESERVATIONS_TAB_LAYOUT_FRAGMENT_TAG = "reservations_tab_layout_fragment";
    /**
     * Stores the tag of the currently displayed fragment. Defaults to {@link #DISCOVERY_FRAGMENT_TAG}.
     */
    private String currentFragmentTag = DISCOVERY_FRAGMENT_TAG;

    /**
     * Launcher for requesting multiple permissions.
     * Handles the results of permission requests for location and notifications.
     */
    private ActivityResultLauncher<String[]> permissionLauncher;


    /**
     * Called when the activity is first created.
     * Initializes user settings, enables edge-to-edge display, sets the content view,
     * and initializes UI components like the {@link #bottomNavMenu}
     * It sets up permission launchers, a custom back press handler, and the bottom navigation listener.
     * If {@code savedInstanceState} is null, it loads the default {@link DiscoveryFragment}.
     * Otherwise, it restores the previously active fragment tag.
     * Finally, it requests necessary runtime permissions.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *                           previously being shut down then this Bundle contains the data it most
     *                           recently supplied in {@link #onSaveInstanceState}.
     *                           <b><i>Note: Otherwise it is null.</i></b>
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        SettingsUtils.loadUserSettings(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_main);

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

    /**
     * Initializes the {@link #permissionLauncher} for handling results of runtime permission requests.
     * It registers an {@link ActivityResultContracts.RequestMultiplePermissions} contract
     * to request location (fine or coarse) and notification (for Android Tiramisu and above) permissions.
     * The callback logs which permissions were denied.
     */
    private void setupPermissionLaunchers()
    {
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result ->
                {
                    boolean locationGranted = result.containsKey(Manifest.permission.ACCESS_FINE_LOCATION) && Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION))
                            || result.containsKey(Manifest.permission.ACCESS_COARSE_LOCATION) && Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));

                    boolean notificationsGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                            || Boolean.TRUE.equals(result.getOrDefault(Manifest.permission.POST_NOTIFICATIONS, false));

                    List<String> denied = new ArrayList<>();
                    if(!locationGranted) denied.add("Location");
                    if(!notificationsGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                        denied.add("Notifications");

                    if(!denied.isEmpty())
                    {
                        Log.w("Permissions", "Denied: " + TextUtils.join(", ", denied));
                    }
                }
        );
    }


    /**
     * Sets up a custom back press handler for the activity using {@link OnBackPressedCallback}.
     * When the back button is pressed:
     * <ul>
     *     <li>If the current fragment is not the {@link DiscoveryFragment}, it navigates to the
     *         {@link DiscoveryFragment} and updates the bottom navigation selection.</li>
     *     <li>If the current fragment is already the {@link DiscoveryFragment}, it allows the default
     *         back press behavior (typically exiting the app).</li>
     * </ul>
     */
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
                    // Note: The switchFragment will be called by the bottomNavMenu's listener
                } else
                {
                    // If already on Discovery, exit the app
                    setEnabled(false); // Disable this callback to allow default behavior
                    getOnBackPressedDispatcher().onBackPressed(); // Trigger default back press
                }
            }
        });
    }

    /**
     * Sets up the listener for the {@link #bottomNavMenu}.
     * When an item in the bottom navigation is selected, it calls {@link #switchFragment(String)}
     * with the corresponding fragment tag to display the selected section.
     */
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

    /**
     * Called to retrieve per-instance state from an activity before it is killed
     * so that the state can be restored in {@link #onCreate(Bundle)} or
     * {@link #onRestoreInstanceState(Bundle)}.
     * This implementation saves the {@link #currentFragmentTag}.
     *
     * @param outState Bundle in which to place your saved state.
     */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putString("currentFragmentTag", currentFragmentTag);
    }

    /**
     * Updates the selected item in the {@link #bottomNavMenu} based on the provided fragment tag.
     * This ensures the navigation view visually reflects the currently active fragment.
     *
     * @param tag The tag of the fragment to select in the bottom navigation view.
     *            Defaults to {@link R.id#Discovery} if the tag does not match other specific sections.
     */
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
        } else // Default to DISCOVERY_FRAGMENT_TAG or any other
        {
            bottomNavMenu.setSelectedItemId(R.id.Discovery);
        }
    }

    /**
     * Switches the currently displayed fragment in the {@code R.id.fragmentContainer}.
     * If the requested fragment is already the current one, no action is taken.
     * Otherwise, it instantiates the new fragment based on {@code fragmentTag} and
     * calls {@link #loadFragment(Fragment, String)} to perform the replacement.
     *
     * @param fragmentTag The tag identifying the fragment to switch to.
     *                    Supported tags are {@link #DISCOVERY_FRAGMENT_TAG},
     *                    {@link #GMAPS_FRAGMENT_TAG}, {@link #PROFILE_FRAGMENT_TAG}, and
     *                    {@link #RESERVATIONS_TAB_LAYOUT_FRAGMENT_TAG}.
     *                    Defaults to {@link DiscoveryFragment} if an unrecognized tag is provided.
     */
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
            default: // Default to DiscoveryFragment
                fragment = new DiscoveryFragment();
                break;
        }

        loadFragment(fragment, fragmentTag);
    }

    /**
     * Loads the specified fragment into the {@code R.id.fragmentContainer}.
     * This method updates the {@link #currentFragmentTag} and performs a fragment replacement
     * transaction. It sets a transition animation and commits the transaction.
     *
     * @param fragment The {@link Fragment} instance to load.
     * @param tag      The tag to associate with the fragment. This tag will also become the {@link #currentFragmentTag}.
     */
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

    /**
     * Requests necessary runtime permissions for the application.
     * It checks for location permissions ({@link Manifest.permission#ACCESS_FINE_LOCATION} and
     * {@link Manifest.permission#ACCESS_COARSE_LOCATION}) and notification permission
     * ({@link Manifest.permission#POST_NOTIFICATIONS} on Android Tiramisu and above).
     * If any of these permissions are not granted, it launches the {@link #permissionLauncher}
     * to request them from the user.
     */
    private void requestRequiredPermissions()
    {
        List<String> permissionsToRequest = new ArrayList<>();

        // Check for location permissions
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION); // Request both, user can choose one
        }

        // Check for notification permission on Android 13 (Tiramisu) and above
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