package com.example.restaurantapp.activities;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentTransaction;

import com.example.restaurantapp.fragments.ChangePasswordFragment;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.example.restaurantapp.R;

import java.util.HashMap;
import java.util.Map;

public class SettingsActivity extends AppCompatActivity
{
    private Toolbar toolbar;
    private Switch darkModeSwitch;
    private Switch notificationsSwitch;
    private Button changePasswordButton;

    // Firebase instances
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private DocumentReference userSettingsRef;

    // For notification permission
    private static final String PREFS_NAME = "RestaurantAppPrefs";
    private static final String PREF_PERMISSION_REQUESTED = "notification_permission_requested";
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize Firebase instances
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize permission launcher for Android 13+
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted ->
                {
                    if (isGranted)
                    {
                        // Permission granted, update UI and Firestore
                        notificationsSwitch.setChecked(true);
                        setNotificationPreference(true);
                        Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show();
                    } else
                    {
                        // Permission denied, update UI to reflect this
                        notificationsSwitch.setChecked(false);
                        setNotificationPreference(false);
                        Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show();
                    }
                });

        // Check if user is logged in
        if (mAuth.getCurrentUser() == null)
        {
            // Redirect to login screen if not logged in
            Toast.makeText(this, "You must be logged in to access settings", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, AuthenticationActivity.class));
            finish();
            return;
        }

        // Get reference to user's settings document
        userSettingsRef = db.collection("Users")
                .document(mAuth.getCurrentUser().getUid())
                .collection("Settings")
                .document("preferences");

        toolbar = findViewById(R.id.toolbar);
        darkModeSwitch = findViewById(R.id.darkModeSwitch);
        notificationsSwitch = findViewById(R.id.notificationsSwitch);
        changePasswordButton = findViewById(R.id.changePasswordButton);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Enable back button
        getSupportActionBar().setTitle("Settings");

        toolbar.setNavigationOnClickListener(v -> finish());

        // Load settings from Firestore
        loadUserSettings();

        // Set up the Dark Mode Switch
        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
        {
            // Only process if this change was user-initiated (not from loadUserSettings)
            if (buttonView.isPressed())
            {
                // Update dark mode preference in Firestore
                setDarkModePreference(isChecked);

                // Apply dark mode change immediately
                if (isChecked)
                {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                } else
                {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                }

                // Show confirmation toast
                Toast.makeText(
                        SettingsActivity.this,
                        isChecked ? "Dark mode enabled" : "Dark mode disabled",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        // Set up the Notifications Switch
        notificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
        {
            // Only process if this change was user-initiated (not from loadUserSettings)
            if (buttonView.isPressed())
            {
                if (isChecked)
                {
                    // Check if we need to request permission for Android 13+
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                                android.content.pm.PackageManager.PERMISSION_GRANTED)
                        {

                            // Request permission instead of enabling notifications
                            requestNotificationPermission();

                            // Don't update UI yet - wait for permission result
                            buttonView.setChecked(false);
                            return;
                        }
                    }
                }

                // Update notification preference in Firestore
                setNotificationPreference(isChecked);

                // Show confirmation toast
                Toast.makeText(
                        SettingsActivity.this,
                        isChecked ? "Notifications enabled" : "Notifications disabled",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getProviderData().size() > 1)
        {
            boolean hasPassword = false;
            for (UserInfo userInfo : user.getProviderData())
            {
                if (EmailAuthProvider.PROVIDER_ID.equals(userInfo.getProviderId()))
                {
                    hasPassword = true;
                    break;
                }
            }
            if (hasPassword)
            {
                changePasswordButton.setVisibility(View.VISIBLE);
                changePasswordButton.setOnClickListener(v ->
                {
                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    transaction.replace(R.id.fragmentContainer, new ChangePasswordFragment());
                    transaction.addToBackStack(null);
                    transaction.commit();

                    findViewById(R.id.settingsScrollView).setVisibility(View.GONE);
                    findViewById(R.id.fragmentContainer).setVisibility(View.VISIBLE);
                    toolbar.setVisibility(View.GONE);
                });
            } else
            {
                changePasswordButton.setVisibility(View.GONE);
            }

            getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true)
            {
                @Override
                public void handleOnBackPressed()
                {
                    if (getSupportFragmentManager().getBackStackEntryCount() > 0)
                    {
                        getSupportFragmentManager().popBackStack();
                        findViewById(R.id.settingsScrollView).setVisibility(View.VISIBLE);
                        findViewById(R.id.fragmentContainer).setVisibility(View.GONE);
                        toolbar.setVisibility(View.VISIBLE);
                    } else
                    {
                        setEnabled(false);
                        getOnBackPressedDispatcher().onBackPressed();
                    }
                }
            });
        }
    }

    // Load user settings from Firestore
    private void loadUserSettings()
    {
        userSettingsRef.get()
                .addOnSuccessListener(documentSnapshot ->
                {
                    if (documentSnapshot.exists())
                    {
                        // Retrieve settings from document
                        Boolean darkMode = documentSnapshot.getBoolean("dark_mode");
                        Boolean notifications = documentSnapshot.getBoolean("notifications");

                        // Set switches without triggering listeners
                        if (darkMode != null)
                        {
                            darkModeSwitch.setChecked(darkMode);
                            // Apply dark mode setting
                            AppCompatDelegate.setDefaultNightMode(darkMode ?
                                    AppCompatDelegate.MODE_NIGHT_YES :
                                    AppCompatDelegate.MODE_NIGHT_NO);
                        }

                        if (notifications != null)
                        {
                            // For Android 13+, check if we have permission before setting checked state
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                            {
                                boolean hasPermission = ContextCompat.checkSelfPermission(this,
                                        Manifest.permission.POST_NOTIFICATIONS) ==
                                        android.content.pm.PackageManager.PERMISSION_GRANTED;

                                // Only allow notifications to be on if we have permission
                                notificationsSwitch.setChecked(notifications && hasPermission);
                            } else
                            {
                                notificationsSwitch.setChecked(notifications);
                            }
                        }
                    } else
                    {
                        // Document doesn't exist, create it with default values
                        boolean defaultNotifications;

                        // For Android 13+, check permission status for default value
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                        {
                            defaultNotifications = ContextCompat.checkSelfPermission(this,
                                    Manifest.permission.POST_NOTIFICATIONS) ==
                                    android.content.pm.PackageManager.PERMISSION_GRANTED;
                        } else
                        {
                            defaultNotifications = true;
                        }

                        Map<String, Object> defaultSettings = new HashMap<>();
                        defaultSettings.put("dark_mode", false);
                        defaultSettings.put("notifications", defaultNotifications);

                        userSettingsRef.set(defaultSettings)
                                .addOnSuccessListener(aVoid ->
                                {
                                    // Set default values on switches
                                    darkModeSwitch.setChecked(false);
                                    notificationsSwitch.setChecked(defaultNotifications);
                                })
                                .addOnFailureListener(e ->
                                {
                                    Toast.makeText(SettingsActivity.this,
                                            "Failed to create settings: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e ->
                {
                    Toast.makeText(SettingsActivity.this,
                            "Failed to load settings: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    // Function to save the dark mode preference to Firestore
    private void setDarkModePreference(boolean isEnabled)
    {
        userSettingsRef.update("dark_mode", isEnabled)
                .addOnFailureListener(e ->
                {
                    Toast.makeText(SettingsActivity.this,
                            "Failed to update dark mode setting: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    // Function to save the notification preference to Firestore
    private void setNotificationPreference(boolean isEnabled)
    {
        userSettingsRef.update("notifications", isEnabled)
                .addOnFailureListener(e ->
                {
                    Toast.makeText(SettingsActivity.this,
                            "Failed to update notification setting: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });

        // Update Firebase Cloud Messaging subscription
        updateNotificationSubscription(isEnabled);
    }

    // Function to request notification permission
    private void requestNotificationPermission()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.POST_NOTIFICATIONS))
            {

                // Show explanation dialog
                new AlertDialog.Builder(this)
                        .setTitle("Notification Permission Needed")
                        .setMessage("To receive restaurant promotions, order updates, and reservation confirmations, " +
                                "please enable notifications.")
                        .setPositiveButton("OK", (dialog, which) ->
                                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS))
                        .setNegativeButton("Cancel", (dialog, which) ->
                        {
                            // User declined explanation, keep notifications disabled
                            notificationsSwitch.setChecked(false);
                            setNotificationPreference(false);
                        })
                        .create()
                        .show();
            } else
            {
                // Check if we've requested permission before but user denied and checked "Don't ask again"
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                boolean permissionRequested = prefs.getBoolean(PREF_PERMISSION_REQUESTED, false);

                if (permissionRequested)
                {
                    // User likely checked "Don't ask again", direct them to Settings
                    new AlertDialog.Builder(this)
                            .setTitle("Permission Required")
                            .setMessage("Notifications permission is required for this feature. " +
                                    "Please enable it in app settings.")
                            .setPositiveButton("Settings", (dialog, which) ->
                            {
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                intent.setData(android.net.Uri.parse("package:" + getPackageName()));
                                startActivity(intent);
                            })
                            .setNegativeButton("Cancel", (dialog, which) ->
                            {
                                // Keep notifications disabled
                                notificationsSwitch.setChecked(false);
                                setNotificationPreference(false);
                            })
                            .create()
                            .show();
                } else
                {
                    // First time requesting, or user didn't check "Don't ask again"
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);

                    // Mark that we've requested permission
                    SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                    editor.putBoolean(PREF_PERMISSION_REQUESTED, true);
                    editor.apply();
                }
            }
        }
    }

    // Function to update notification subscription with Firebase Cloud Messaging
    private void updateNotificationSubscription(boolean isSubscribed)
    {
        if (isSubscribed)
        {
            FirebaseMessaging.getInstance().subscribeToTopic("app_notifications")
                    .addOnCompleteListener(task ->
                    {
                        if (!task.isSuccessful())
                        {
                            Toast.makeText(SettingsActivity.this,
                                    "Failed to enable notifications",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        } else
        {
            FirebaseMessaging.getInstance().unsubscribeFromTopic("app_notifications")
                    .addOnCompleteListener(task ->
                    {
                        if (!task.isSuccessful())
                        {
                            Toast.makeText(SettingsActivity.this,
                                    "Failed to disable notifications",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}