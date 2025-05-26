package com.example.restaurantapp.fragments;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.example.restaurantapp.R;
import com.example.restaurantapp.activities.AuthenticationActivity;
import com.example.restaurantapp.activities.EditInfoActivity;
import com.example.restaurantapp.utils.SettingsUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RestaurantSettingsFragment extends Fragment
{
    private Button logoutButton, changePasswordButton;
    private TextView profileEmail, profilePhone;
    private Switch darkModeSwitch;
    private ActivityResultLauncher<Intent> editInfoLauncher;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private DocumentReference userRef;
    private DocumentReference userSettingsRef;


    public RestaurantSettingsFragment()
    {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_restaurant_settings, container, false);


        // Initializing UI components
        logoutButton = view.findViewById(R.id.logoutButton);
        changePasswordButton = view.findViewById(R.id.changePasswordButton);
        profileEmail = view.findViewById(R.id.profileEmail);
        profilePhone = view.findViewById(R.id.profilePhone);

        darkModeSwitch = view.findViewById(R.id.darkModeSwitch);

        // Set the current email and phone if user is authenticated
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = auth.getCurrentUser();

        if(currentUser != null)
        {
            userRef = db.collection("Users").document(auth.getCurrentUser().getUid());
            userSettingsRef = userRef.collection("Settings").document("preferences");

            userRef.get().addOnSuccessListener(documentSnapshot ->
            {
                profileEmail.setText(documentSnapshot.getString("email"));
                profilePhone.setText(documentSnapshot.getString("phoneNumber"));
            });
        }

        loadDarkModeSetting();

        // Register the ActivityResultLauncher for profile edits
        editInfoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> SettingsUtils.handleEditInfoResult(requireContext(), result, userRef, auth, null, profilePhone));

        // Set listeners for the UI components
        logoutButton.setOnClickListener(v -> SettingsUtils.handleLogout(requireActivity(), auth));
        view.findViewById(R.id.editProfileEmailContainer).setOnClickListener(v -> SettingsUtils.launchEditActivity(getActivity(), editInfoLauncher, "Email", profileEmail.getText().toString()));
        view.findViewById(R.id.editProfilePhoneContainer).setOnClickListener(v -> SettingsUtils.launchEditActivity(getActivity(), editInfoLauncher, "Phone", profilePhone.getText().toString()));
        changePasswordButton.setOnClickListener(v -> SettingsUtils.launchEditActivity(getActivity(), editInfoLauncher, "Password", null));

        SettingsUtils.setupDarkModeSwitch(this, darkModeSwitch, userSettingsRef);

        return view;
    }

    @Override
    public void onResume()
    {
        super.onResume();

        FirebaseUser user = auth.getCurrentUser();
        if(user != null)
        {
            user.reload().addOnCompleteListener(task ->
            {
                if(task.isSuccessful())
                {
                    Log.d("RestaurantSettingsFragment", "User reloaded in onResume");
                    SettingsUtils.syncPendingEmailIfNeeded(userRef, auth, profileEmail);
                } else
                {
                    Log.e("RestaurantSettingsFragment", "User reload failed", task.getException());
                }
            });
        }
    }

    // Load user settings from preferences
    private void loadDarkModeSetting()
    {
        SharedPreferences prefs = requireContext().getSharedPreferences("FeedMe", Context.MODE_PRIVATE);
        boolean hasLocalSettings = prefs.contains("dark_mode");

        if(hasLocalSettings)
        {
            // Load from SharedPreferences
            boolean darkMode = prefs.getBoolean("dark_mode", false);

            darkModeSwitch.setChecked(darkMode);
            AppCompatDelegate.setDefaultNightMode(darkMode ?
                    AppCompatDelegate.MODE_NIGHT_YES :
                    AppCompatDelegate.MODE_NIGHT_NO);
        } else
        {
            // Fallback to Firestore
            fetchFromFirestoreAndStoreLocally(prefs);
        }
    }

    //In case load fails, load from firestore and store locally
    private void fetchFromFirestoreAndStoreLocally(SharedPreferences prefs)
    {
        userSettingsRef.get()
                .addOnSuccessListener(documentSnapshot ->
                {
                    if(documentSnapshot.exists())
                    {
                        // Retrieve setting from document
                        boolean darkMode = documentSnapshot.getBoolean("dark_mode") != null && Boolean.TRUE.equals(documentSnapshot.getBoolean("dark_mode"));

                        prefs.edit()
                                .putBoolean("dark_mode", darkMode)
                                .apply();

                        // Set switches without triggering listeners
                        darkModeSwitch.setChecked(darkMode);
                        // Apply dark mode setting
                        AppCompatDelegate.setDefaultNightMode(darkMode ?
                                AppCompatDelegate.MODE_NIGHT_YES :
                                AppCompatDelegate.MODE_NIGHT_NO);
                    } else
                    {
                        Map<String, Object> defaultSettings = new HashMap<>();
                        defaultSettings.put("dark_mode", false);

                        userSettingsRef.set(defaultSettings)
                                .addOnSuccessListener(aVoid ->
                                {
                                    prefs.edit()
                                            .putBoolean("dark_mode", false)
                                            .apply();
                                    // Set default values on switches
                                    darkModeSwitch.setChecked(false);
                                })
                                .addOnFailureListener(e ->
                                {
                                    Toast.makeText(requireContext(),
                                            "Failed to create settings: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e ->
                {
                    Toast.makeText(requireContext(),
                            "Failed to load settings: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }
}
