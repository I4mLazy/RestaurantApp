package com.example.restaurantapp.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch; // Standard Android Switch
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment; // Standard Fragment import, @NonNull not needed here for class declaration

import com.example.restaurantapp.R;
import com.example.restaurantapp.utils.SettingsUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
// import java.util.Objects; // Unused import in the provided code for this fragment

/**
 * A {@link Fragment} subclass that provides a user interface for restaurant users to manage their account settings.
 * This includes options to log out, change password, edit email and phone number, and toggle dark mode.
 * User data (email, phone) is loaded from Firestore. Dark mode settings are loaded from SharedPreferences,
 * with a fallback to Firestore, and then cached locally.
 * It utilizes {@link SettingsUtils} for common actions like logout, launching edit activities,
 * and handling dark mode switch logic.
 */
public class RestaurantSettingsFragment extends Fragment
{
    /**
     * Button to log out the current user.
     */
    private Button logoutButton;
    /**
     * Button to navigate to the change password screen.
     */
    private Button changePasswordButton;
    /**
     * TextView displaying the user's email address.
     */
    private TextView profileEmail;
    /**
     * TextView displaying the user's phone number.
     */
    private TextView profilePhone;
    /**
     * Switch for toggling dark mode preference.
     */
    private Switch darkModeSwitch; // Standard Android Switch
    /**
     * ActivityResultLauncher for handling results from the EditInfoActivity.
     */
    private ActivityResultLauncher<Intent> editInfoLauncher;

    /**
     * Instance of FirebaseFirestore for database operations.
     */
    private FirebaseFirestore db;
    /**
     * Instance of FirebaseAuth for user authentication.
     */
    private FirebaseAuth auth;
    /**
     * The currently authenticated FirebaseUser.
     */
    private FirebaseUser currentUser;
    /**
     * DocumentReference to the user's main document in Firestore (under "Users/{uid}").
     */
    private DocumentReference userRef;
    /**
     * DocumentReference to the user's settings document in Firestore (under "Users/{uid}/Settings/preferences").
     */
    private DocumentReference userSettingsRef;


    /**
     * Required empty public constructor for Fragment instantiation.
     */
    public RestaurantSettingsFragment()
    {
        // Required empty public constructor
    }

    /**
     * Called to have the fragment instantiate its user interface view.
     * Inflates the layout, initializes UI components (buttons, TextViews, Switch) and Firebase services.
     * If a user is authenticated, it fetches and displays their email and phone number from Firestore.
     * Calls {@link #loadDarkModeSetting()} to apply the dark mode preference.
     * Registers an {@link ActivityResultLauncher} to handle results from profile edit activities.
     * Sets up click listeners for logout, edit email, edit phone, and change password,
     * delegating actions to methods in {@link SettingsUtils}.
     * Also sets up the dark mode switch using {@link SettingsUtils#setupDarkModeSwitch(Fragment, Switch, DocumentReference)}.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate
     *                           any views in the fragment.
     * @param container          If non-null, this is the parent view that the fragment's
     *                           UI should be attached to. The fragment should not add the view itself,
     *                           but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     *                           from a previous saved state as given here.
     * @return Return the View for the fragment's UI.
     */
    @Override
    public View onCreateView(@androidx.annotation.NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) // Added @NonNull for inflater
    {
        View view = inflater.inflate(R.layout.fragment_restaurant_settings, container, false);


        // Initializing UI components
        logoutButton = view.findViewById(R.id.logoutButton);
        changePasswordButton = view.findViewById(R.id.changePasswordButton);
        profileEmail = view.findViewById(R.id.profileEmail);
        profilePhone = view.findViewById(R.id.profilePhone);
        darkModeSwitch = view.findViewById(R.id.darkModeSwitch);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = auth.getCurrentUser();

        if(currentUser != null)
        {
            // userRef = db.collection("Users").document(Objects.requireNonNull(auth.getCurrentUser()).getUid()); // Original code uses auth.getCurrentUser() again
            userRef = db.collection("Users").document(currentUser.getUid()); // Using the already fetched currentUser
            userSettingsRef = userRef.collection("Settings").document("preferences");

            userRef.get().addOnSuccessListener(documentSnapshot ->
            {
                if(isAdded() && documentSnapshot.exists())
                { // Check if fragment is added and document exists
                    profileEmail.setText(documentSnapshot.getString("email"));
                    profilePhone.setText(documentSnapshot.getString("phoneNumber"));
                } else if(isAdded())
                {
                    Log.w("RestaurantSettings", "User document does not exist for UID: " + currentUser.getUid());
                    // Optionally set default text or handle error
                }
            }).addOnFailureListener(e ->
            {
                if(isAdded()) Log.e("RestaurantSettings", "Error fetching user document.", e);
            });
        } else
        {
            Log.e("RestaurantSettings", "Current user is null in onCreateView.");
            // Handle UI for non-logged-in state if necessary
        }

        loadDarkModeSetting(); // Load dark mode setting

        // Register the ActivityResultLauncher for profile edits
        editInfoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result ->
                {
                    if(userRef != null && auth != null)
                    { // Ensure references are not null
                        SettingsUtils.handleEditInfoResult(requireContext(), result, userRef, auth, null, profilePhone);
                    } else
                    {
                        Log.e("RestaurantSettings", "userRef or auth is null in editInfoLauncher callback.");
                    }
                });

        // Set listeners for the UI components
        logoutButton.setOnClickListener(v -> SettingsUtils.handleLogout(requireActivity(), auth));
        view.findViewById(R.id.editProfileEmailContainer).setOnClickListener(v -> SettingsUtils.launchEditActivity(getActivity(), editInfoLauncher, "Email", profileEmail.getText().toString()));
        view.findViewById(R.id.editProfilePhoneContainer).setOnClickListener(v -> SettingsUtils.launchEditActivity(getActivity(), editInfoLauncher, "Phone", profilePhone.getText().toString()));
        changePasswordButton.setOnClickListener(v -> SettingsUtils.launchEditActivity(getActivity(), editInfoLauncher, "Password", null));

        if(userSettingsRef != null)
        { // Ensure userSettingsRef is initialized before use
            SettingsUtils.setupDarkModeSwitch(this, darkModeSwitch, userSettingsRef);
        } else
        {
            Log.e("RestaurantSettings", "userSettingsRef is null. Cannot setup dark mode switch.");
            // Potentially set a default state for darkModeSwitch or disable it
        }

        return view;
    }

    /**
     * Called when the fragment is visible to the user and actively running.
     * Reloads the current Firebase user's data to ensure it's up-to-date,
     * then calls {@link SettingsUtils#syncPendingEmailIfNeeded(DocumentReference, FirebaseAuth, TextView)}
     * to handle any pending email verification states, updating the {@link #profileEmail} TextView.
     */
    @Override
    public void onResume()
    {
        super.onResume();
        FirebaseUser user = auth.getCurrentUser(); // Use local var or class member consistently
        if(user != null)
        {
            user.reload().addOnCompleteListener(task ->
            {
                if(task.isSuccessful())
                {
                    Log.d("RestaurantSettingsFragment", "User reloaded in onResume");
                    if(userRef != null && auth != null && profileEmail != null)
                    { // Check for nulls
                        SettingsUtils.syncPendingEmailIfNeeded(userRef, auth, profileEmail);
                    } else
                    {
                        Log.w("RestaurantSettingsFragment", "Cannot sync pending email: one or more required objects are null.");
                    }
                } else
                {
                    Log.e("RestaurantSettingsFragment", "User reload failed in onResume.", task.getException());
                }
            });
        }
    }

    /**
     * Loads the dark mode setting.
     * It first attempts to load the "dark_mode" preference from SharedPreferences ("FeedMe").
     * If found, it applies the setting to the {@link #darkModeSwitch} and {@link AppCompatDelegate}.
     * If not found locally, it calls {@link #fetchFromFirestoreAndStoreLocally(SharedPreferences)}
     * to get the setting from Firestore and then cache it.
     */
    private void loadDarkModeSetting()
    {
        if(getContext() == null)
        {
            Log.e("RestaurantSettings", "Context is null in loadDarkModeSetting.");
            return;
        }
        SharedPreferences prefs = requireContext().getSharedPreferences("FeedMe", Context.MODE_PRIVATE);
        boolean hasLocalSettings = prefs.contains("dark_mode");

        if(hasLocalSettings)
        {
            boolean darkMode = prefs.getBoolean("dark_mode", false); // Default to false
            darkModeSwitch.setChecked(darkMode);
            AppCompatDelegate.setDefaultNightMode(darkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        } else
        {
            // Fallback to Firestore if no local setting
            if(userSettingsRef != null)
            { // Ensure userSettingsRef is initialized
                fetchFromFirestoreAndStoreLocally(prefs);
            } else
            {
                Log.e("RestaurantSettings", "userSettingsRef is null. Cannot fetch dark mode setting from Firestore.");
                // Set a default UI state for darkModeSwitch if Firestore fetch is not possible
                darkModeSwitch.setChecked(false); // Default dark mode off
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        }
    }

    /**
     * Fetches the "dark_mode" setting from the user's Firestore settings document if it's not
     * found in SharedPreferences.
     * If the settings document exists in Firestore, it retrieves the "dark_mode" preference,
     * stores it in the provided SharedPreferences, and updates the {@link #darkModeSwitch} and
     * {@link AppCompatDelegate}.
     * If the document doesn't exist, it creates one with a default "dark_mode" value of false,
     * stores this default locally, and updates the UI.
     *
     * @param prefs The {@link SharedPreferences} instance to store the fetched setting.
     */
    private void fetchFromFirestoreAndStoreLocally(SharedPreferences prefs)
    {
        if(userSettingsRef == null || getContext() == null)
        {
            Log.e("RestaurantSettings", "userSettingsRef or context is null in fetchFromFirestoreAndStoreLocally.");
            return;
        }
        userSettingsRef.get()
                .addOnSuccessListener(documentSnapshot ->
                {
                    if(!isAdded() || getContext() == null) return; // Check fragment state

                    boolean darkModeValue = false; // Default value

                    if(documentSnapshot.exists())
                    {
                        // Retrieve setting from document, default to false if field is null or not boolean
                        darkModeValue = Boolean.TRUE.equals(documentSnapshot.getBoolean("dark_mode"));
                    } else
                    {
                        // Document doesn't exist, create it with default "dark_mode": false
                        Map<String, Object> defaultSettings = new HashMap<>();
                        defaultSettings.put("dark_mode", false); // Default is false
                        userSettingsRef.set(defaultSettings)
                                .addOnFailureListener(e ->
                                {
                                    if(isAdded())
                                        Log.e("RestaurantSettings", "Failed to create default settings in Firestore.", e);
                                });
                        // darkModeValue remains false (the default)
                    }

                    // Store and apply the fetched/default value
                    prefs.edit().putBoolean("dark_mode", darkModeValue).apply();
                    darkModeSwitch.setChecked(darkModeValue);
                    AppCompatDelegate.setDefaultNightMode(darkModeValue ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
                })
                .addOnFailureListener(e ->
                {
                    Log.e("RestaurantSettings", "Failed to load dark_mode setting from Firestore.", e);
                    if(getContext() != null)
                    {
                        Toast.makeText(requireContext(), "Failed to load dark mode setting: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        // Apply a default UI state if Firestore fetch fails
                        darkModeSwitch.setChecked(false);
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    }
                });
    }
}