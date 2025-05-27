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
import com.example.restaurantapp.fragments.ManageMenuFragment;
import com.example.restaurantapp.fragments.ReservationsTabLayoutFragment;
import com.example.restaurantapp.fragments.RestaurantSettingsFragment;
import com.example.restaurantapp.fragments.RestaurantInfoFragment;
import com.example.restaurantapp.utils.SettingsUtils;
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

/**
 * The main activity for the restaurant-side of the application.
 * This activity hosts various fragments for managing restaurant operations,
 * such as menu management, reservations, settings, and restaurant information.
 * It uses a {@link BottomNavigationView} for navigation between these sections.
 * The activity also handles custom back press behavior and manages fragment transactions
 * to prevent issues with rapid switching.
 */
public class RestaurantMainActivity extends AppCompatActivity
{
    /**
     * Tag for logging purposes.
     */
    private static final String TAG = "RestaurantMainActivity";
    /**
     * The bottom navigation view for switching between main sections of the restaurant app.
     */
    private BottomNavigationView bottomNavMenu;
    /**
     * Tag for the {@link ManageMenuFragment}.
     */
    private final String MANAGE_MENU_FRAGMENT_TAG = "manage_menu_fragment";
    /**
     * Tag for the {@link RestaurantSettingsFragment}.
     */
    private final String RESTAURANT_SETTINGS_FRAGMENT_TAG = "restaurant_settings_fragment";
    /**
     * Tag for the {@link RestaurantInfoFragment}.
     */
    private final String RESTAURANT_INFO_FRAGMENT_TAG = "restaurant_info_fragment";
    /**
     * Tag for the {@link ReservationsTabLayoutFragment}.
     */
    private final String RESERVATIONS_TAB_LAYOUT_FRAGMENT_TAG = "reservations_tab_layout_fragment";
    /**
     * Stores the tag of the currently displayed fragment. Defaults to {@link #MANAGE_MENU_FRAGMENT_TAG}.
     */
    private String currentFragmentTag = MANAGE_MENU_FRAGMENT_TAG;

    /**
     * Flag to indicate if a fragment transaction is currently in progress. Used to prevent concurrent transactions.
     */
    private boolean isTransactionInProgress = false;
    /**
     * Handler associated with the main looper, used for posting UI updates and delayed operations.
     */
    private final Handler handler = new Handler(Looper.getMainLooper());
    /**
     * Stores the tag of a fragment that is pending to be switched to if a transaction is already in progress.
     */
    private String pendingFragmentTag = null;

    /**
     * Called when the activity is first created.
     * Initializes user settings, enables edge-to-edge display, sets the content view,
     * and initializes the bottom navigation menu. It also sets up a custom back press handler.
     * If {@code savedInstanceState} is null, it loads the default {@link ManageMenuFragment}.
     * Otherwise, it restores the previously active fragment tag from {@code savedInstanceState}.
     * Finally, it updates the bottom navigation view to reflect the current fragment and sets up its listener.
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
        setContentView(R.layout.activity_restaurant_main);

        bottomNavMenu = findViewById(R.id.bottom_navigation);

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
    }

    /**
     * Sets up a custom back press handler for the activity.
     * When the back button is pressed:
     * <ul>
     *     <li>If the current fragment is not the {@link ManageMenuFragment}, it navigates to the
     *         {@link ManageMenuFragment} and updates the bottom navigation selection.</li>
     *     <li>If the current fragment is already the {@link ManageMenuFragment}, it allows the default
     *         back press behavior (exiting the app).</li>
     * </ul>
     */
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
                    setEnabled(false); // Disable this callback to allow default behavior
                    getOnBackPressedDispatcher().onBackPressed(); // Trigger default back press
                }
            }
        });
    }

    /**
     * Sets up the listener for the {@link BottomNavigationView}.
     * When an item is selected, it calls {@link #switchFragment(String)} with the corresponding
     * fragment tag to navigate to the selected section.
     */
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
     *            Defaults to {@link R.id#nav_menu} if the tag does not match other specific sections.
     */
    private void updateBottomNavFromTag(String tag)
    {
        if(RESTAURANT_SETTINGS_FRAGMENT_TAG.equals(tag))
        {
            bottomNavMenu.setSelectedItemId(R.id.nav_settings);
        } else if(RESERVATIONS_TAB_LAYOUT_FRAGMENT_TAG.equals(tag))
        {
            bottomNavMenu.setSelectedItemId(R.id.nav_reservations);
        } else if(RESTAURANT_INFO_FRAGMENT_TAG.equals(tag))
        {
            bottomNavMenu.setSelectedItemId(R.id.nav_info);
        } else // Default to MANAGE_MENU_FRAGMENT_TAG or any other
        {
            bottomNavMenu.setSelectedItemId(R.id.nav_menu);
        }
    }

    /**
     * Switches the currently displayed fragment in the {@code R.id.fragment_container}.
     * If the requested fragment is already the current one, no action is taken.
     * If a fragment transaction is already in progress ({@link #isTransactionInProgress} is true),
     * the requested {@code fragmentTag} is stored in {@link #pendingFragmentTag} to be processed later.
     * Otherwise, it instantiates the new fragment based on {@code fragmentTag}, updates the
     * {@link #bottomNavMenu} selection, and performs the fragment replacement.
     * The transaction is posted to the main thread handler and uses {@code commitAllowingStateLoss}
     * to prevent {@link IllegalStateException}.
     * After the transaction is posted, {@link #isTransactionInProgress} is set to false after a short delay,
     * and any {@link #pendingFragmentTag} is then processed.
     *
     * @param fragmentTag The tag identifying the fragment to switch to.
     *                    Supported tags are {@link #MANAGE_MENU_FRAGMENT_TAG},
     *                    {@link #RESTAURANT_SETTINGS_FRAGMENT_TAG},
     *                    {@link #RESERVATIONS_TAB_LAYOUT_FRAGMENT_TAG}, and
     *                    {@link #RESTAURANT_INFO_FRAGMENT_TAG}.
     *                    Defaults to {@link ManageMenuFragment} if an unrecognized tag is provided.
     */
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
            default: // Default to ManageMenuFragment
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
                            .commitAllowingStateLoss(); // Use commitAllowingStateLoss to prevent crashes
                }
            } catch(Exception e)
            {
                Log.e(TAG, "Error switching fragments: " + e.getMessage());
            } finally
            {
                // Reset the flag after a small delay to ensure the transaction completes
                // and to allow the UI to settle.
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
                }, 250); // Delay might need adjustment based on transaction complexity
            }
        });
    }

    /**
     * Loads the specified fragment into the {@code R.id.fragment_container}.
     * This method updates the {@link #currentFragmentTag} and performs a fragment replacement
     * transaction using {@code commitAllowingStateLoss}. It is typically used for the initial
     * loading of a fragment.
     *
     * @param fragment The {@link Fragment} instance to load.
     * @param tag      The tag to associate with the fragment. This tag will also become the {@link #currentFragmentTag}.
     */
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
            Log.e(TAG, "Error loading fragment: " + e.getMessage());
            // In case of an error during initial load, the isTransactionInProgress flag might need handling
            // if this method were to be used in contexts similar to switchFragment.
            // For current usage (initial load), this is less critical.
        }
    }
}