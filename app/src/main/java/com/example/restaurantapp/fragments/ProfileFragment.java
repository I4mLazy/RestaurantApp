package com.example.restaurantapp.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
// import android.graphics.Paint; // Unused import
import android.media.ExifInterface; // Used for image orientation
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Switch; // Standard Switch, not SwitchMaterial from Material library
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.restaurantapp.R;
import com.example.restaurantapp.utils.SettingsUtils;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * A {@link Fragment} subclass that displays and manages the user's profile information and application settings.
 * Users can view their name, email, and phone number, and initiate edits for these fields.
 * It allows users to change their profile picture by taking a new photo or selecting from the gallery.
 * Application settings such as dark mode and notification preferences can also be managed here.
 * User data is loaded from and saved to Firestore, with profile pictures handled by Firebase Storage.
 * Settings are cached in SharedPreferences for quicker access.
 */
public class ProfileFragment extends Fragment
{
    /**
     * ImageButton for displaying and initiating edits to the user's profile picture.
     */
    private ImageButton editProfilePictureImageButton;
    /**
     * TextView displaying the user's name.
     */
    private TextView profileName;
    /**
     * TextView displaying the user's email address.
     */
    private TextView profileEmail;
    /**
     * TextView displaying the user's phone number.
     */
    private TextView profilePhone;
    /**
     * TextView acting as an additional click target to edit the profile picture.
     */
    private TextView editProfilePictureTextView;
    /**
     * Switch for toggling dark mode preference.
     */
    private Switch darkModeSwitch; // Standard Android Switch
    /**
     * Switch for toggling notification preference.
     */
    private Switch notificationsSwitch; // Standard Android Switch
    /**
     * ActivityResultLauncher for handling results from the EditInfoActivity.
     */
    private ActivityResultLauncher<Intent> editInfoLauncher;
    /**
     * ActivityResultLauncher for requesting runtime permissions (e.g., notifications on Android 13+).
     */
    private ActivityResultLauncher<String> requestPermissionLauncher;
    /**
     * BottomSheetDialog for presenting image source options (camera/gallery) for profile picture.
     */
    private BottomSheetDialog bottomSheetDialog;
    /**
     * Reference to the current Firebase Storage upload task for the profile picture.
     */
    private UploadTask currentUploadTask;
    /**
     * Flag indicating whether a profile picture upload is currently in progress.
     */
    private boolean isUploading = false;
    /**
     * Uri of the photo taken by the camera or selected from the gallery for the profile picture.
     */
    private Uri photoUri;

    /**
     * Instance of FirebaseFirestore for database operations.
     */
    private FirebaseFirestore db;
    /**
     * Instance of FirebaseAuth for user authentication.
     */
    private FirebaseAuth auth;
    /**
     * Instance of FirebaseStorage for image storage.
     */
    private FirebaseStorage storage;
    /**
     * Reference to the root of Firebase Storage.
     */
    private StorageReference storageRef;
    /**
     * DocumentReference to the user's settings document in Firestore (under "Users/{uid}/Settings/preferences").
     */
    private DocumentReference userSettingsRef;
    /**
     * DocumentReference to the user's main document in Firestore (under "Users/{uid}").
     */
    private DocumentReference userRef;
    /**
     * Tag for logging purposes.
     */
    private static final String TAG = "ProfileFragment";

    /**
     * Required empty public constructor for Fragment instantiation.
     */
    public ProfileFragment()
    {
        // Required empty public constructor
    }


    /**
     * Called to have the fragment instantiate its user interface view.
     * Inflates the layout, initializes Firebase services, and UI components.
     * Sets up references to the user's Firestore documents (main profile and settings).
     * Calls {@link #loadUserProfile()} to display user data and {@link #loadUserSettings()} for preferences.
     * Configures click listeners for editing profile fields (name, email, phone, password),
     * changing the profile picture, and logging out, utilizing {@link SettingsUtils} for some actions.
     * Registers {@link ActivityResultLauncher}s for handling edit results and permission requests.
     * Sets up listeners for dark mode and notification switches.
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
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        profileName = view.findViewById(R.id.profileName);
        profileEmail = view.findViewById(R.id.profileEmail);
        profilePhone = view.findViewById(R.id.profilePhone);

        editProfilePictureImageButton = view.findViewById(R.id.editProfilePictureImageButton);
        editProfilePictureTextView = view.findViewById(R.id.editProfilePictureTextView);

        darkModeSwitch = view.findViewById(R.id.darkModeSwitch);
        notificationsSwitch = view.findViewById(R.id.notificationsSwitch);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        FirebaseUser currentUser = auth.getCurrentUser();
        if(currentUser != null)
        {
            userRef = db.collection("Users").document(currentUser.getUid());
            userSettingsRef = userRef.collection("Settings").document("preferences");
        } else
        {
            // Handle case where user is null, perhaps navigate to login or show error
            Log.e(TAG, "Current user is null in onCreateView. Cannot initialize Firestore references.");
            // Consider returning an error view or navigating away.
            // For now, subsequent calls might fail if userRef/userSettingsRef are null.
        }


        // Load user profile data
        loadUserProfile();

        // Set click listeners for profile fields
        view.findViewById(R.id.editProfileNameContainer).setOnClickListener(v -> SettingsUtils.launchEditActivity(requireContext(), editInfoLauncher, "Name", profileName.getText().toString()));
        view.findViewById(R.id.editProfileEmailContainer).setOnClickListener(v -> SettingsUtils.launchEditActivity(requireContext(), editInfoLauncher, "Email", profileEmail.getText().toString()));
        view.findViewById(R.id.editProfilePhoneContainer).setOnClickListener(v -> SettingsUtils.launchEditActivity(requireContext(), editInfoLauncher, "Phone", profilePhone.getText().toString()));
        view.findViewById(R.id.changePasswordButton).setOnClickListener(v -> SettingsUtils.launchEditActivity(requireContext(), editInfoLauncher, "Password", null));

        // Set click listeners for profile picture
        editProfilePictureImageButton.setOnClickListener(v -> selectProfilePicture());
        editProfilePictureTextView.setOnClickListener(v -> selectProfilePicture());

        // Logout button
        view.findViewById(R.id.logoutButton).setOnClickListener(v -> SettingsUtils.handleLogout(requireActivity(), auth));

        // Register the ActivityResultLauncher for profile edits
        editInfoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result ->
                {
                    if(userRef != null && auth != null)
                    { // Ensure references are not null
                        SettingsUtils.handleEditInfoResult(requireContext(), result, userRef, auth, profileName, profilePhone);
                    } else
                    {
                        Log.e(TAG, "userRef or auth is null in editInfoLauncher callback.");
                    }
                });

        // Initialize permission launcher for Android 13+ notifications
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted ->
                {
                    if(isGranted)
                    {
                        notificationsSwitch.setChecked(true);
                        setNotificationPreference(true); // Update Firestore
                        Toast.makeText(requireContext(), "Notifications enabled.", Toast.LENGTH_SHORT).show();
                    } else
                    {
                        notificationsSwitch.setChecked(false); // Keep it unchecked
                        setNotificationPreference(false); // Update Firestore
                        Toast.makeText(requireContext(), "Notification permission denied.", Toast.LENGTH_SHORT).show();
                    }
                });

        loadUserSettings(); // Load and apply settings

        if(userSettingsRef != null)
        { // Ensure userSettingsRef is initialized
            SettingsUtils.setupDarkModeSwitch(this, darkModeSwitch, userSettingsRef);
        } else
        {
            Log.e(TAG, "userSettingsRef is null. Cannot setup dark mode switch.");
        }


        notificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
        {
            if(buttonView.isPressed()) // Process only user-initiated changes
            {
                if(isChecked)
                {
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    {
                        if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) !=
                                PackageManager.PERMISSION_GRANTED)
                        {
                            requestNotificationPermission(); // Request permission
                            buttonView.setChecked(false); // Revert switch until permission result
                            return; // Wait for permission result
                        }
                    }
                }
                // If permission already granted or not needed, or if unchecking
                setNotificationPreference(isChecked);
                Toast.makeText(requireContext(),
                        isChecked ? "Notifications enabled" : "Notifications disabled",
                        Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    /**
     * Called when the fragment is visible to the user and actively running.
     * Reloads the current Firebase user's data to ensure it's up-to-date,
     * then calls {@link SettingsUtils#syncPendingEmailIfNeeded(DocumentReference, FirebaseAuth, TextView)}
     * to handle any pending email verification states.
     */
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
                    Log.d(TAG, "User reloaded successfully in onResume.");
                    if(userRef != null && auth != null && profileEmail != null)
                    { // Check for nulls
                        SettingsUtils.syncPendingEmailIfNeeded(userRef, auth, profileEmail);
                    } else
                    {
                        Log.w(TAG, "Cannot sync pending email: one or more required objects are null.");
                    }
                } else
                {
                    Log.e(TAG, "User reload failed in onResume.", task.getException());
                }
            });
        }
    }


    /**
     * Called when the view previously created by {@link #onCreateView} has
     * been detached from the fragment.
     * Dismisses any showing {@link #bottomSheetDialog} to prevent window leaks.
     * Cancels any ongoing Firebase Storage upload task ({@link #currentUploadTask}).
     */
    @Override
    public void onDestroyView()
    {
        super.onDestroyView();

        if(bottomSheetDialog != null && bottomSheetDialog.isShowing())
        {
            bottomSheetDialog.dismiss();
        }
        bottomSheetDialog = null; // Clear reference

        if(currentUploadTask != null && isUploading) // isUploading flag is important here
        {
            currentUploadTask.cancel();
            Log.d(TAG, "Cancelled ongoing image upload task.");
        }
        currentUploadTask = null;
        isUploading = false;
    }

    /**
     * Loads user settings (dark mode, notifications) first from SharedPreferences.
     * If settings are not found locally, it falls back to fetching them from Firestore
     * via {@link #fetchFromFirestoreAndStoreLocally(SharedPreferences)} and then stores them locally.
     * Updates the UI switches (dark mode, notifications) accordingly.
     * For notifications on Android 13+, it also considers the current permission status.
     */
    private void loadUserSettings()
    {
        if(getContext() == null)
        {
            Log.e(TAG, "Context is null in loadUserSettings.");
            return;
        }
        SharedPreferences prefs = requireContext().getSharedPreferences("FeedMe", Context.MODE_PRIVATE);
        boolean hasLocalSettings = prefs.contains("dark_mode") && prefs.contains("notifications");

        if(hasLocalSettings)
        {
            boolean darkMode = prefs.getBoolean("dark_mode", false);
            boolean notificationsEnabledPref = prefs.getBoolean("notifications", true);

            // Apply dark mode
            darkModeSwitch.setChecked(darkMode);
            AppCompatDelegate.setDefaultNightMode(darkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

            // Apply notification setting, considering permission for Android 13+
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            {
                boolean hasPermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
                notificationsSwitch.setChecked(notificationsEnabledPref && hasPermission);
            } else
            {
                notificationsSwitch.setChecked(notificationsEnabledPref);
            }
        } else
        {
            // Fallback to Firestore if no local settings
            if(userSettingsRef != null)
            { // Ensure userSettingsRef is initialized
                fetchFromFirestoreAndStoreLocally(prefs);
            } else
            {
                Log.e(TAG, "userSettingsRef is null. Cannot fetch settings from Firestore.");
                // Set default UI state for switches if Firestore fetch is not possible
                darkModeSwitch.setChecked(false); // Default dark mode off
                notificationsSwitch.setChecked(true); // Default notifications on (pre-Tiramisu)
            }
        }
    }

    /**
     * Fetches user settings from Firestore if they are not available in SharedPreferences.
     * If the settings document exists in Firestore, it retrieves "dark_mode" and "notifications"
     * preferences, stores them in the provided SharedPreferences, and updates the UI switches.
     * If the document doesn't exist, it creates one with default values (dark mode off,
     * notifications on by default, or based on permission status for Android 13+),
     * stores these defaults locally, and updates the UI.
     *
     * @param prefs The {@link SharedPreferences} instance to store fetched settings.
     */
    private void fetchFromFirestoreAndStoreLocally(SharedPreferences prefs)
    {
        if(userSettingsRef == null || getContext() == null)
        {
            Log.e(TAG, "userSettingsRef or context is null in fetchFromFirestoreAndStoreLocally.");
            return;
        }
        userSettingsRef.get()
                .addOnSuccessListener(documentSnapshot ->
                {
                    if(!isAdded() || getContext() == null) return; // Check fragment state

                    boolean darkModeValue = false; // Default
                    boolean notificationsValue = true; // Default (pre-Tiramisu)

                    if(documentSnapshot.exists())
                    {
                        darkModeValue = Boolean.TRUE.equals(documentSnapshot.getBoolean("dark_mode"));
                        notificationsValue = Boolean.TRUE.equals(documentSnapshot.getBoolean("notifications"));
                    } else
                    {
                        // Document doesn't exist, create with defaults
                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                        {
                            notificationsValue = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
                        }
                        Map<String, Object> defaultSettings = new HashMap<>();
                        defaultSettings.put("dark_mode", darkModeValue);
                        defaultSettings.put("notifications", notificationsValue);
                        userSettingsRef.set(defaultSettings)
                                .addOnFailureListener(e -> Log.e(TAG, "Failed to create default settings in Firestore.", e));
                    }

                    // Store and apply fetched/default values
                    prefs.edit()
                            .putBoolean("dark_mode", darkModeValue)
                            .putBoolean("notifications", notificationsValue)
                            .apply();

                    darkModeSwitch.setChecked(darkModeValue);
                    AppCompatDelegate.setDefaultNightMode(darkModeValue ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    {
                        boolean hasPermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
                        notificationsSwitch.setChecked(notificationsValue && hasPermission);
                    } else
                    {
                        notificationsSwitch.setChecked(notificationsValue);
                    }
                })
                .addOnFailureListener(e ->
                {
                    Log.e(TAG, "Failed to load settings from Firestore.", e);
                    if(getContext() != null)
                    {
                        Toast.makeText(requireContext(), "Failed to load settings: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Requests the {@link Manifest.permission#POST_NOTIFICATIONS} permission from the user.
     * This is specifically for Android 13 (TIRAMISU) and above.
     * If the SDK version is below Tiramisu, it directly enables the notification preference
     * by calling {@link #setNotificationPreference(boolean)} with true and checks the switch.
     */
    private void requestNotificationPermission()
    {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        {
            if(requestPermissionLauncher != null)
            { // Ensure launcher is initialized
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else
            {
                Log.e(TAG, "requestPermissionLauncher is null. Cannot request notification permission.");
            }
        } else
        {
            // For pre-Tiramisu, permission is implicitly granted or not needed.
            setNotificationPreference(true); // Update Firestore
            notificationsSwitch.setChecked(true); // Update UI
            Toast.makeText(requireContext(), "Notifications enabled (pre-Android 13).", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Updates the "notifications" preference in the user's Firestore settings document.
     * Also calls {@link #updateNotificationSubscription(boolean)} to subscribe/unsubscribe
     * from Firebase Cloud Messaging topics accordingly.
     *
     * @param isEnabled True to enable notifications, false to disable.
     */
    private void setNotificationPreference(boolean isEnabled)
    {
        if(userSettingsRef == null)
        {
            Log.e(TAG, "userSettingsRef is null in setNotificationPreference. Cannot update Firestore.");
            return;
        }
        userSettingsRef.update("notifications", isEnabled)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Notification preference updated in Firestore to: " + isEnabled))
                .addOnFailureListener(e ->
                {
                    Log.e(TAG, "Failed to update notification setting in Firestore.", e);
                    if(getContext() != null)
                    {
                        Toast.makeText(requireContext(), "Failed to update notification setting: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

        updateNotificationSubscription(isEnabled); // Update FCM subscription
    }

    /**
     * Subscribes or unsubscribes the user from the "app_notifications" Firebase Cloud Messaging topic.
     *
     * @param isSubscribed True to subscribe to the topic, false to unsubscribe.
     */
    private void updateNotificationSubscription(boolean isSubscribed)
    {
        String topic = "app_notifications";
        if(isSubscribed)
        {
            FirebaseMessaging.getInstance().subscribeToTopic(topic)
                    .addOnCompleteListener(task ->
                    {
                        String msg = task.isSuccessful() ? "Subscribed to notifications topic." : "Failed to subscribe to notifications topic.";
                        Log.d(TAG, msg);
                        if(!task.isSuccessful() && getContext() != null)
                        {
                            Toast.makeText(requireContext(), "Subscription failed.", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else
        {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
                    .addOnCompleteListener(task ->
                    {
                        String msg = task.isSuccessful() ? "Unsubscribed from notifications topic." : "Failed to unsubscribe from notifications topic.";
                        Log.d(TAG, msg);
                        if(!task.isSuccessful() && getContext() != null)
                        {
                            Toast.makeText(requireContext(), "Unsubscription failed.", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    /**
     * Loads the current user's profile data (name, email, phone, profile image URL) from Firestore
     * and populates the corresponding TextViews and ImageView.
     * It fetches the user document based on the current user's UID.
     * If the document exists, it retrieves the "name", "phoneNumber", "email", and "profileImageURL" fields.
     * The {@link #profileName}, {@link #profilePhone}, and {@link #profileEmail} TextViews are updated
     * with these values if they are not null.
     * The user's profile image is loaded into {@link #editProfilePictureImageButton} using Glide
     * if {@code imageUrl} is not null or empty; otherwise, a placeholder image is set.
     * If the document does not exist or if fetching fails, a placeholder image is set for the profile picture,
     * and an error is logged.
     * The method ensures the fragment is still attached and active before attempting UI updates.
     */
    private void loadUserProfile()
    {
        FirebaseUser currentUser = auth.getCurrentUser(); // Local variable, not shadowing if class member is also named currentUser
        if(currentUser != null && userRef != null) // Check userRef as well
        {
            userRef.get().addOnSuccessListener(documentSnapshot ->
            {
                if(!isAdded() || getActivity() == null || getActivity().isFinishing() || getContext() == null) // Added getContext() null check
                    return;

                if(documentSnapshot.exists())
                {
                    String name = documentSnapshot.getString("name");
                    String phone = documentSnapshot.getString("phoneNumber");
                    String email = documentSnapshot.getString("email"); // This is from Firestore
                    String imageUrl = documentSnapshot.getString("profileImageURL");

                    if(name != null) profileName.setText(name);

                    if(phone != null) profilePhone.setText(phone);

                    // Email from Firestore is used directly
                    if(email != null) profileEmail.setText(email);


                    if(imageUrl != null && !imageUrl.isEmpty())
                    {
                        Glide.with(requireContext())
                                .load(imageUrl)
                                .placeholder(R.drawable.image_placeholder)
                                .error(R.drawable.image_placeholder)
                                .into(editProfilePictureImageButton);
                    } else
                    {
                        editProfilePictureImageButton.setImageResource(R.drawable.image_placeholder);
                    }
                } else
                {
                    Log.w(TAG, "User document does not exist for UID: " + currentUser.getUid());
                    editProfilePictureImageButton.setImageResource(R.drawable.image_placeholder); // Default if no doc
                }
            }).addOnFailureListener(e ->
            {
                if(isAdded() && getContext() != null) // Added getContext() null check
                {
                    Log.e(TAG, "Error loading user profile from Firestore.", e);
                    Toast.makeText(requireContext(), "Failed to load profile.", Toast.LENGTH_SHORT).show();
                    editProfilePictureImageButton.setImageResource(R.drawable.image_placeholder); // Default on error
                }
            });
        } else
        {
            Log.w(TAG, "Cannot load user profile: current user or userRef is null.");
            // Set UI to a default/logged-out state if necessary
            if(editProfilePictureImageButton != null) // Check if view is initialized
                editProfilePictureImageButton.setImageResource(R.drawable.image_placeholder);
            if(profileName != null) profileName.setText("Not Logged In");
            if(profilePhone != null) profilePhone.setText("");
            if(profileEmail != null) profileEmail.setText("");
        }
    }

    /**
     * Initiates the process of selecting a new profile picture.
     * Checks for camera and storage permissions. If not granted, requests them.
     * If granted, shows a {@link BottomSheetDialog} to choose image source (camera/gallery).
     * This method is a duplicate of methods found in other fragments and should be refactored.
     */
    private void selectProfilePicture() // This is identical to methods in EditRestaurantInfoFragment and ManageMenuFragment
    {
        if(getContext() == null) return;
        boolean cameraPermissionGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        boolean storagePermissionGranted = checkStoragePermission();
        Log.d(TAG, "selectProfilePicture - CameraP: " + cameraPermissionGranted + ", StorageP: " + storagePermissionGranted);
        if(!cameraPermissionGranted || !storagePermissionGranted) requestPermissions();
        else showBottomSheetDialog();
    }

    /**
     * Checks for necessary storage permissions based on Android SDK version.
     * This method is a duplicate of methods in other fragments.
     *
     * @return True if storage permissions are granted, false otherwise.
     */
    private boolean checkStoragePermission() // Identical to methods in other fragments
    {
        if(getContext() == null) return false;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) return true;
        else
            return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Requests necessary permissions (camera and storage).
     * This method is a duplicate of methods in other fragments.
     */
    private void requestPermissions() // Identical to methods in other fragments
    {
        if(getContext() == null) return;
        List<String> permissionsToRequest = new ArrayList<>();
        if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            permissionsToRequest.add(Manifest.permission.CAMERA);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        {
            if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED)
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES);
        } else if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
        {
            if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if(!permissionsToRequest.isEmpty())
            permissionLauncher.launch(permissionsToRequest.toArray(new String[0]));
        else showBottomSheetDialog();
    }

    /**
     * ActivityResultLauncher for handling multiple permission requests.
     * This is largely a duplicate of launchers in other fragments.
     */
    private final ActivityResultLauncher<String[]> permissionLauncher = // Identical to launchers in other fragments
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result ->
            {
                if(getContext() == null || !isAdded()) return;
                boolean cameraGranted = Boolean.TRUE.equals(result.get(Manifest.permission.CAMERA));
                boolean storageGranted = false;
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    storageGranted = Boolean.TRUE.equals(result.get(Manifest.permission.READ_MEDIA_IMAGES));
                else if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
                    storageGranted = Boolean.TRUE.equals(result.get(Manifest.permission.WRITE_EXTERNAL_STORAGE)) && Boolean.TRUE.equals(result.get(Manifest.permission.READ_EXTERNAL_STORAGE));
                else storageGranted = true;
                if(cameraGranted && storageGranted) showBottomSheetDialog();
                else
                    Toast.makeText(requireContext(), "Permissions are required to change profile picture.", Toast.LENGTH_SHORT).show();
            });

    /**
     * Displays a {@link BottomSheetDialog} allowing the user to choose an image source
     * (camera or gallery) for their profile picture.
     * If an existing {@code bottomSheetDialog} is already showing, it is dismissed first.
     * A new dialog is created using the layout {@code R.layout.bottom_sheet_image}.
     * The title TextView within the bottom sheet ({@code R.id.SelectImageTextView}) is set to
     * "Select Profile Picture".
     * Click listeners are set for the "Take Photo" button (calls {@link #takePhoto()}),
     * "Choose from Gallery" button (calls {@link #pickFromGallery()}), and "Cancel" button,
     * all of which also dismiss the dialog.
     * An {@code OnDismissListener} is set to nullify the {@link #bottomSheetDialog} reference
     * when it's dismissed. Finally, the dialog is shown.
     * The method includes a null check for {@link #getContext()} at the beginning.
     * Note: This method is marked as identical to methods in other fragments in the original code comments.
     */
    private void showBottomSheetDialog() // Identical to methods in other fragments
    {
        if(getContext() == null) return; // Null check for context
        if(bottomSheetDialog != null && bottomSheetDialog.isShowing()) bottomSheetDialog.dismiss();
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_image, null);
        TextView selectImageTextview = bottomSheetView.findViewById(R.id.SelectImageTextView);
        selectImageTextview.setText("Select Profile Picture");
        bottomSheetDialog = new BottomSheetDialog(requireContext());
        bottomSheetDialog.setContentView(bottomSheetView);
        Button btnTakePhoto = bottomSheetView.findViewById(R.id.btn_take_photo);
        Button btnChooseGallery = bottomSheetView.findViewById(R.id.btn_choose_gallery);
        Button btnCancel = bottomSheetView.findViewById(R.id.btn_cancel);
        btnTakePhoto.setOnClickListener(v ->
        {
            Log.d(TAG, "Taking photo...");
            takePhoto();
            bottomSheetDialog.dismiss();
        });
        btnChooseGallery.setOnClickListener(v ->
        {
            Log.d(TAG, "Choosing from gallery...");
            pickFromGallery();
            bottomSheetDialog.dismiss();
        });
        btnCancel.setOnClickListener(v -> bottomSheetDialog.dismiss());
        bottomSheetDialog.setOnDismissListener(dialog -> bottomSheetDialog = null);
        bottomSheetDialog.show();
    }

    /**
     * Initiates taking a photo using the camera.
     * This method is a duplicate of methods in other fragments.
     */
    private void takePhoto() // Identical to methods in other fragments
    {
        if(getContext() == null || getActivity() == null) return;
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(intent.resolveActivity(requireActivity().getPackageManager()) != null)
        {
            File photoFile = createImageFile();
            if(photoFile != null)
            {
                try
                {
                    photoUri = FileProvider.getUriForFile(requireContext(), "com.example.restaurantapp.fileprovider", photoFile);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                    cameraLauncher.launch(intent);
                } catch(IllegalArgumentException e)
                {
                    Log.e(TAG, "Error creating file URI for camera.", e);
                    Toast.makeText(requireContext(), "Unable to create file for photo.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    /**
     * Initiates picking an image from the gallery.
     * This method is a duplicate of methods in other fragments.
     */
    private void pickFromGallery() // Identical to methods in other fragments
    {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    /**
     * ActivityResultLauncher for handling camera result.
     * If successful, loads image, fixes orientation, displays it, and calls {@link #uploadImageToFirebase(Bitmap)}.
     * This is similar to launchers in other fragments.
     */
    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult( // Similar to launchers in other fragments
            new ActivityResultContracts.StartActivityForResult(),
            result ->
            {
                if(result.getResultCode() == Activity.RESULT_OK)
                {
                    if(!isAdded() || getActivity() == null || getActivity().isFinishing() || getContext() == null)
                        return;
                    boolean hasCameraP = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
                    boolean hasStorageP = checkStoragePermission();
                    if(hasCameraP && hasStorageP && photoUri != null)
                    {
                        try
                        {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), photoUri);
                            bitmap = fixImageOrientation(photoUri, bitmap); // Use correct fixImageOrientation
                            editProfilePictureImageButton.setImageBitmap(bitmap);
                            uploadImageToFirebase(bitmap);
                        } catch(IOException e)
                        {
                            Log.e(TAG, "Error processing camera image.", e);
                            Toast.makeText(requireContext(), "Error processing image.", Toast.LENGTH_SHORT).show();
                        }
                    } else
                    {
                        Toast.makeText(requireContext(), "Permissions required or photo URI null.", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    /**
     * ActivityResultLauncher for handling gallery result.
     * If successful, loads image, fixes orientation, displays it, and calls {@link #uploadImageToFirebase(Bitmap)}.
     * This is similar to launchers in other fragments.
     */
    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult( // Similar to launchers in other fragments
            new ActivityResultContracts.StartActivityForResult(),
            result ->
            {
                if(result.getResultCode() == Activity.RESULT_OK && result.getData() != null)
                {
                    if(!isAdded() || getActivity() == null || getActivity().isFinishing() || getContext() == null)
                        return;
                    boolean hasStorageP = checkStoragePermission();
                    if(hasStorageP)
                    {
                        Uri imageUri = result.getData().getData();
                        if(imageUri != null)
                        {
                            try
                            {
                                Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), imageUri);
                                bitmap = fixImageOrientation(imageUri, bitmap); // Use correct fixImageOrientation
                                editProfilePictureImageButton.setImageBitmap(bitmap);
                                uploadImageToFirebase(bitmap);
                            } catch(IOException e)
                            {
                                Log.e(TAG, "Error loading image from gallery.", e);
                                Toast.makeText(requireContext(), "Failed to load image.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } else
                    {
                        Toast.makeText(requireContext(), "Storage permissions required.", Toast.LENGTH_SHORT).show();
                        requestPermissions();
                    }
                }
            });

    /**
     * Creates a temporary image file for profile picture.
     * This method is a duplicate of methods in other fragments.
     *
     * @return The created {@link File}, or null on error.
     */
    private File createImageFile() // Identical to methods in other fragments
    {
        if(getContext() == null) return null;
        File storageDir = requireContext().getExternalFilesDir(null);
        try
        {
            return File.createTempFile("profile_pic_", ".jpg", storageDir);
        } catch(IOException e)
        {
            Log.e(TAG, "Error creating image file.", e);
            return null;
        }
    }

    /**
     * Uploads the provided bitmap image to Firebase Storage as the user's profile picture.
     * Generates a unique filename under "profile_images/{userID}/".
     * If successful, calls {@link #updateUserProfileWithImageUrl(String)} to save the new image URL to Firestore.
     * This method is similar to upload methods in other fragments but specific to profile pictures.
     *
     * @param bitmap The {@link Bitmap} image to upload.
     */
    private void uploadImageToFirebase(Bitmap bitmap) // Similar to upload methods in other fragments
    {
        if(bitmap == null)
        {
            Log.e(TAG, "Cannot upload null bitmap for profile.");
            return;
        }
        if(!isAdded() || getActivity() == null || getActivity().isFinishing() || getContext() == null)
            return;

        Toast.makeText(requireContext(), "Uploading profile picture...", Toast.LENGTH_SHORT).show();
        // Consider showing a progress bar specific to image upload

        FirebaseUser firebaseCurrentUser = auth.getCurrentUser();
        if(firebaseCurrentUser == null)
        {
            Log.e(TAG, "No user logged in for profile picture upload.");
            Toast.makeText(requireContext(), "Login required to upload picture.", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = firebaseCurrentUser.getUid();
        String filename = "profile_images/" + userId + "/" + UUID.randomUUID().toString() + ".jpg";
        StorageReference imageRef = storageRef.child(filename);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        byte[] imageData = baos.toByteArray();

        isUploading = true;
        currentUploadTask = imageRef.putBytes(imageData);
        currentUploadTask.addOnSuccessListener(taskSnapshot ->
        {
            isUploading = false;
            currentUploadTask = null;
            if(!isAdded() || getActivity() == null || getActivity().isFinishing()) return;
            imageRef.getDownloadUrl().addOnSuccessListener(downloadUri ->
            {
                if(!isAdded() || getActivity() == null || getActivity().isFinishing()) return;
                Log.d(TAG, "Profile picture upload successful. URL: " + downloadUri.toString());
                updateUserProfileWithImageUrl(downloadUri.toString()); // This will also handle deleting old image
            }).addOnFailureListener(e ->
            {
                if(!isAdded() || getActivity() == null || getActivity().isFinishing()) return;
                Log.e(TAG, "Failed to get download URL for profile picture.", e);
                Toast.makeText(requireContext(), "Failed to get image URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }).addOnFailureListener(e ->
        {
            isUploading = false;
            currentUploadTask = null;
            if(!isAdded() || getActivity() == null || getActivity().isFinishing()) return;
            Log.e(TAG, "Profile picture upload failed.", e);
            Toast.makeText(requireContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }).addOnProgressListener(taskSnapshot ->
        {
            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
            Log.d(TAG, "Profile picture upload progress: " + progress + "%");
        });
    }

    /**
     * Corrects image orientation using EXIF data.
     * This method is a duplicate of methods in other fragments, using {@code android.media.ExifInterface}.
     *
     * @param imageUri The URI of the image.
     * @param bitmap   The original bitmap.
     * @return The orientation-corrected bitmap, or original if an error occurs.
     */
    private Bitmap fixImageOrientation(Uri imageUri, Bitmap bitmap) // Uses android.media.ExifInterface
    {
        if(getContext() == null || imageUri == null || bitmap == null) return bitmap;
        try
        {
            ExifInterface exif = null;
            try(InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri))
            {
                if(inputStream != null) exif = new ExifInterface(inputStream);
            }
            int orientation = ExifInterface.ORIENTATION_NORMAL;
            if(exif != null)
                orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            Matrix matrix = new Matrix();
            switch(orientation)
            {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.postRotate(90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.postRotate(180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.postRotate(270);
                    break;
            }
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        } catch(Exception e)
        {
            Log.e(TAG, "Error fixing image orientation (using android.media.ExifInterface).", e);
            return bitmap;
        }
    }

    /**
     * Updates the "profileImageURL" field in the user's Firestore document with the new image URL.
     * Before updating, it calls {@link #deleteOldProfileImage(String)} to remove the previous profile picture
     * from Firebase Storage if one existed and is different from the new one.
     *
     * @param imageUrl The new profile image URL to save.
     */
    private void updateUserProfileWithImageUrl(String imageUrl)
    {
        FirebaseUser firebaseCurrentUser = auth.getCurrentUser(); // Shadowing
        if(firebaseCurrentUser != null && userRef != null)
        {
            // Call deleteOldProfileImage first. It will handle the logic of checking old URL.
            // The actual update to Firestore with the new URL should happen *after* old image deletion (or if no old image).
            // This method's current structure calls deleteOldProfileImage, which is good.
            // Then it proceeds to update Firestore.
            deleteOldProfileImage(imageUrl); // This will attempt to delete the old one.

            // The update to Firestore should ideally be chained after deleteOldProfileImage completes,
            // or deleteOldProfileImage should take a callback to then update Firestore.
            // For now, following the existing structure:
            Map<String, Object> updates = new HashMap<>();
            updates.put("profileImageURL", imageUrl);

            userRef.update(updates)
                    .addOnSuccessListener(aVoid ->
                    {
                        if(isAdded() && getActivity() != null && !getActivity().isFinishing())
                        {
                            Log.d(TAG, "User profile successfully updated with new image URL in Firestore.");
                            Toast.makeText(requireContext(), "Profile picture updated.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e ->
                    {
                        if(isAdded() && getActivity() != null && !getActivity().isFinishing())
                        {
                            Log.e(TAG, "Error updating user profileImageURL in Firestore.", e);
                            Toast.makeText(requireContext(), "Failed to save profile picture: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } else
        {
            Log.e(TAG, "Cannot update profile image URL: current user or userRef is null.");
        }
    }

    /**
     * Deletes the user's old profile image from Firebase Storage if it exists and is different from {@code newImageUrl}.
     * Fetches the current {@code profileImageURL} from the user's Firestore document.
     * If an old URL is found and differs, it parses the URL to get the storage path and attempts deletion.
     * This method is called by {@link #updateUserProfileWithImageUrl(String)} before setting the new URL.
     *
     * @param newImageUrl The URL of the new profile image. The old image will only be deleted if it's different.
     */
    private void deleteOldProfileImage(String newImageUrl)
    {
        FirebaseUser firebaseCurrentUser = auth.getCurrentUser(); // Shadowing
        if(firebaseCurrentUser != null && userRef != null)
        {
            userRef.get().addOnSuccessListener(documentSnapshot ->
            {
                if(documentSnapshot.exists())
                {
                    String oldImageUrl = documentSnapshot.getString("profileImageURL");
                    if(oldImageUrl != null && !oldImageUrl.isEmpty() && !oldImageUrl.equals(newImageUrl))
                    {
                        try
                        {
                            if(oldImageUrl.contains("/o/"))
                            { // Check for typical Firebase Storage URL structure
                                String urlPath = oldImageUrl.split("/o/")[1].split("\\?")[0];
                                String decodedPath = java.net.URLDecoder.decode(urlPath, "UTF-8");
                                StorageReference oldImageRef = storage.getReference().child(decodedPath);
                                oldImageRef.delete()
                                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Old profile image deleted successfully: " + oldImageUrl))
                                        .addOnFailureListener(e -> Log.e(TAG, "Error deleting old profile image: " + oldImageUrl, e));
                            } else
                            {
                                Log.w(TAG, "Old profile image URL format not recognized for deletion: " + oldImageUrl);
                            }
                        } catch(Exception e)
                        {
                            Log.e(TAG, "Error parsing or deleting old profile image URL: " + oldImageUrl, e);
                        }
                    }
                }
            }).addOnFailureListener(e -> Log.e(TAG, "Error fetching user document to delete old profile image.", e));
        } else
        {
            Log.w(TAG, "Cannot delete old profile image: current user or userRef is null.");
        }
    }
}