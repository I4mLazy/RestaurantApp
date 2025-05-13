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
import androidx.fragment.app.FragmentTransaction;

import com.example.restaurantapp.R;
import com.example.restaurantapp.activities.AuthenticationActivity;
import com.example.restaurantapp.activities.EditInfoActivity;
import com.example.restaurantapp.activities.SettingsActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RestaurantSettingsFragment extends Fragment
{
    private Button logoutButton, changePasswordButton;
    private TextView profileEmail, profilePhone;
    private LinearLayout editProfileEmailContainer, editProfilePhoneContainer;
    private Switch darkModeSwitch;
    private ActivityResultLauncher<Intent> editInfoLauncher;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
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
        editProfileEmailContainer = view.findViewById(R.id.editProfileEmailContainer);
        editProfilePhoneContainer = view.findViewById(R.id.editProfilePhoneContainer);
        darkModeSwitch = view.findViewById(R.id.darkModeSwitch);

        // Set the current email and phone if user is authenticated
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = auth.getCurrentUser();

        if(currentUser != null)
        {
            userSettingsRef = db.collection("Users")
                    .document(auth.getCurrentUser().getUid())
                    .collection("Settings")
                    .document("preferences");

            profileEmail.setText(currentUser.getEmail());
        }

        loadDarkModeSetting();

        // Register the ActivityResultLauncher for profile edits
        editInfoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> handleEditInfoResults(result)
        );

        // Set listeners for the UI components
        logoutButton.setOnClickListener(v -> handleLogout());
        editProfileEmailContainer.setOnClickListener(v -> launchEditActivity("Email", profileEmail.getText().toString()));
        editProfilePhoneContainer.setOnClickListener(v -> launchEditActivity("Phone", profilePhone.getText().toString()));

        /*changePasswordButton.setOnClickListener(v ->
        {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragmentContainer, new ChangePasswordFragment());
            transaction.addToBackStack(null);
            transaction.commit();

            findViewById(R.id.fragmentContainer).setVisibility(View.VISIBLE);
        });*/

        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
        {
            // Only process if this change was user-initiated (not from loadUserSettings)
            if(buttonView.isPressed())
            {
                // Update dark mode preference in Firestore and locally
                setDarkModePreference(isChecked);

                // Apply dark mode change immediately
                if(isChecked)
                {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                } else
                {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                }

                // Show confirmation toast
                Toast.makeText(
                        requireContext(),
                        isChecked ? "Dark mode enabled" : "Dark mode disabled",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        return view;
    }

    private void launchEditActivity(String fieldType, String currentValue)
    {
        Intent intent = new Intent(getActivity(), EditInfoActivity.class);
        intent.putExtra("fieldType", fieldType);
        intent.putExtra("currentValue", currentValue);
        editInfoLauncher.launch(intent);
    }

    private void handleEditInfoResults(androidx.activity.result.ActivityResult result)
    {
        if(result.getResultCode() == Activity.RESULT_OK && result.getData() != null)
        {
            String updatedValue = result.getData().getStringExtra("updatedValue");
            String fieldType = result.getData().getStringExtra("fieldType");

            if(updatedValue != null && fieldType != null)
            {
                // Update UI based on field type
                if("Email".equals(fieldType))
                {
                    profileEmail.setText(updatedValue);
                } else if("Phone".equals(fieldType))
                {
                    profilePhone.setText("+" + updatedValue);
                }

                // Save to Firestore
                saveUserInfo(updatedValue, fieldType);
            }
        }
    }

    private void saveUserInfo(String updatedValue, String fieldType)
    {
        FirebaseUser currentUser = auth.getCurrentUser();
        if(currentUser != null)
        {
            DocumentReference userRef = db.collection("Users").document(currentUser.getUid());
            Map<String, Object> updatedData = new HashMap<>();

            // Handle different field types
            if("Name".equals(fieldType))
            {
                updatedData.put("name", updatedValue);
                updateFirestore(userRef, updatedData);
            } else if("Phone".equals(fieldType))
            {
                updatedData.put("phoneNumber", "+" + updatedValue);
                updateFirestore(userRef, updatedData);
            } else if("Email".equals(fieldType))
            {
                // For email updates, using verifyBeforeUpdateEmail
                //handleEmailUpdate(currentUser, updatedValue, userRef);
            }
        }
    }

    private void updateFirestore(DocumentReference userRef, Map<String, Object> data)
    {
        userRef.update(data)
                .addOnSuccessListener(aVoid ->
                {
                    if(isAdded())
                    {
                        Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                {
                    if(isAdded())
                    {
                        Toast.makeText(requireContext(), "Error updating profile", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error updating profile", e);
                    }
                });
    }

    // Log out functionality
    private void handleLogout()
    {
        if(auth.getCurrentUser() != null)
        {
            new androidx.appcompat.app.AlertDialog.Builder(requireActivity())
                    .setTitle("Log Out")
                    .setMessage("Are you sure you want to log out?")
                    .setPositiveButton("Yes", (dialog, which) ->
                    {
                        FirebaseAuth.getInstance().signOut();

                        // Clear shared preferences
                        SharedPreferences sharedPreferences = requireActivity()
                                .getSharedPreferences("FeedMe", FragmentActivity.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.clear();
                        editor.apply();

                        // Redirect to authentication screen
                        Intent intent = new Intent(getActivity(), AuthenticationActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                Intent.FLAG_ACTIVITY_NEW_TASK |
                                Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        requireActivity().finish();
                    })
                    .setNegativeButton("No", (dialog, id) -> dialog.dismiss())
                    .create()
                    .show();
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

    // Function to save the dark mode preference to Firestore and SharedPreferences
    private void setDarkModePreference(boolean isEnabled)
    {
        Log.d(TAG, "User settings reference: " + userSettingsRef.getPath());
        // Save to Firestore
        userSettingsRef.update("dark_mode", isEnabled)
                .addOnFailureListener(e ->
                {
                    // Check if the fragment is still attached to its context
                    if(isAdded())
                    {
                        requireContext();
                        Toast.makeText(requireContext(),
                                "Failed to update dark mode setting: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });

        // Save locally using SharedPreferences
        if(isAdded())
        {
            requireContext();
            SharedPreferences sharedPreferences = requireContext().getSharedPreferences("FeedMe", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("darkMode", isEnabled);
            editor.apply();
        }
    }
}
