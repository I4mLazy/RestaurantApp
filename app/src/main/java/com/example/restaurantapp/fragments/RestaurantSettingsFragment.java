package com.example.restaurantapp.fragments;

import static android.content.ContentValues.TAG;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
                    SettingsUtils.syncPendingEmailIfNeeded(requireContext(), userRef, auth, profileEmail);
                } else
                {
                    Log.e("RestaurantSettingsFragment", "User reload failed", task.getException());
                }
            });
        }
    }

    // Load user settings from Firestore (only for dark mode)
    private void loadDarkModeSetting()
    {
        userSettingsRef.get()
                .addOnSuccessListener(documentSnapshot ->
                {
                    if(isAdded() && documentSnapshot.exists())
                    {  // Check if the fragment is still added
                        // Retrieve the dark_mode setting
                        Boolean darkMode = documentSnapshot.getBoolean("dark_mode");

                        if(darkMode != null)
                        {
                            // Ensure the fragment is still attached before interacting with the UI
                            if(isAdded())
                            {
                                darkModeSwitch.setChecked(darkMode);
                                // Apply the dark mode setting
                                AppCompatDelegate.setDefaultNightMode(darkMode ?
                                        AppCompatDelegate.MODE_NIGHT_YES :
                                        AppCompatDelegate.MODE_NIGHT_NO);
                            }
                        }
                    } else
                    {
                        // Document doesn't exist, create it with the default value for dark mode
                        Map<String, Object> defaultSettings = new HashMap<>();
                        defaultSettings.put("dark_mode", false); // default value for dark mode

                        userSettingsRef.set(defaultSettings)
                                .addOnSuccessListener(aVoid ->
                                {
                                    if(isAdded())
                                    {  // Ensure fragment is still added before modifying UI
                                        darkModeSwitch.setChecked(false);
                                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                                    }
                                })
                                .addOnFailureListener(e ->
                                {
                                    if(isAdded())
                                    {
                                        Toast.makeText(requireContext(),
                                                "Failed to create settings: " + e.getMessage(),
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .addOnFailureListener(e ->
                {
                    if(isAdded())
                    {
                        Toast.makeText(requireContext(),
                                "Failed to load settings: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
