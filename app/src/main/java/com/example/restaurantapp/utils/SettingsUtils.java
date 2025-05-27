package com.example.restaurantapp.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Switch; // Standard Android Switch
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.example.restaurantapp.activities.AuthenticationActivity;
import com.example.restaurantapp.activities.EditInfoActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class providing static methods for managing user settings and profile information.
 * This includes loading and applying dark mode settings, handling user logout,
 * launching activities to edit user information, processing results from these edit activities,
 * saving user info to Firestore, managing pending email changes, and setting up UI elements
 * like dark mode switches.
 */
public class SettingsUtils
{

    /** The name of the SharedPreferences file used for storing local settings. */
    private static final String PREF_NAME = "FeedMe";
    /** The key used to store the dark mode preference in SharedPreferences. */
    private static final String DARK_MODE_KEY = "darkMode";

    /**
     * Loads and applies the user's dark mode setting.
     * It first checks SharedPreferences for a locally stored dark mode preference.
     * If found, it applies this setting to {@link AppCompatDelegate}.
     * If not found in SharedPreferences, it attempts to load the "dark_mode" field
     * from the user's "preferences" document in their Firestore "Settings" subcollection.
     * If fetched successfully from Firestore, the value is saved to SharedPreferences
     * and then applied to {@link AppCompatDelegate}.
     * Requires the current user to be authenticated to fetch from Firestore.
     *
     * @param context The {@link Context} used to access SharedPreferences and show toasts.
     */
    public static void loadUserSettings(Context context)
    {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser(); // Get current user once
        if (currentUser == null) {
            Log.w("SettingsUtils", "loadUserSettings: No current user. Cannot load settings from Firestore.");
            // Apply default or only local settings if user is null
            SharedPreferences sharedPreferencesOnNullUser = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            boolean isDarkModeOnNullUser = sharedPreferencesOnNullUser.getBoolean(DARK_MODE_KEY, false);
            AppCompatDelegate.setDefaultNightMode(isDarkModeOnNullUser ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
            return;
        }
        String userID = currentUser.getUid();

        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean(DARK_MODE_KEY, false); // Default to false (light mode)

        // Apply dark mode based on shared preference initially
        AppCompatDelegate.setDefaultNightMode(isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        // If SharedPreferences doesn't have dark mode value, load from Firestore
        if(!sharedPreferences.contains(DARK_MODE_KEY))
        {
            db.collection("Users")
                    .document(userID)
                    .collection("Settings")
                    .document("preferences").get()
                    .addOnSuccessListener(documentSnapshot ->
                    {
                        if(documentSnapshot.exists())
                        {
                            Boolean darkModeFromFirestore = documentSnapshot.getBoolean("dark_mode");
                            if(darkModeFromFirestore != null)
                            {
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putBoolean(DARK_MODE_KEY, darkModeFromFirestore);
                                editor.apply();

                                // Re-apply if fetched value is different from initial local value (or if no local value)
                                if (isDarkMode != darkModeFromFirestore) { // Only re-apply if different
                                    AppCompatDelegate.setDefaultNightMode(darkModeFromFirestore ?
                                            AppCompatDelegate.MODE_NIGHT_YES :
                                            AppCompatDelegate.MODE_NIGHT_NO);
                                }
                            } else {
                                // Field doesn't exist or is null, can store default to Firestore
                                // For now, just relies on the default 'false' applied earlier if not found.
                            }
                        } else {
                            // Preferences document doesn't exist, can create with defaults
                            // For now, relies on the default 'false' applied earlier.
                        }
                    })
                    .addOnFailureListener(e ->
                    {
                        // Toast.makeText(context, "Failed to load settings: " + e.getMessage(), Toast.LENGTH_SHORT).show(); // Original code
                        Log.e("SettingsUtils", "Failed to load settings from Firestore: " + e.getMessage(), e);
                    });
        }
    }

    /**
     * Handles the user logout process.
     * Shows an {@link androidx.appcompat.app.AlertDialog} to confirm the logout action.
     * If confirmed, it signs out the user via {@link FirebaseAuth#signOut()},
     * clears all data from SharedPreferences ("FeedMe"), and navigates to
     * {@link AuthenticationActivity}, clearing the activity stack.
     *
     * @param activity The current {@link FragmentActivity} from which logout is initiated.
     * @param auth The {@link FirebaseAuth} instance.
     */
    public static void handleLogout(FragmentActivity activity, FirebaseAuth auth)
    {
        if(auth.getCurrentUser() != null)
        {
            new androidx.appcompat.app.AlertDialog.Builder(activity)
                    .setTitle("Log Out")
                    .setMessage("Are you sure you want to log out?")
                    .setPositiveButton("Yes", (dialog, which) ->
                    {
                        auth.signOut();

                        SharedPreferences sharedPreferences = activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE); // PREF_NAME used
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.clear();
                        editor.apply();

                        Intent intent = new Intent(activity, AuthenticationActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                Intent.FLAG_ACTIVITY_NEW_TASK |
                                Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        activity.startActivity(intent);
                        activity.finish();
                    })
                    .setNegativeButton("No", (dialog, id) -> dialog.dismiss())
                    .create()
                    .show();
        } else {
            Log.w("SettingsUtils", "handleLogout called but no user is currently signed in.");
            // Optionally, directly navigate to AuthenticationActivity if no user is signed in.
        }
    }

    /**
     * Launches the {@link EditInfoActivity} to edit a specific user profile field.
     *
     * @param context The {@link Context} from which to launch the activity.
     * @param launcher The {@link ActivityResultLauncher} registered to handle the result from {@link EditInfoActivity}.
     * @param fieldType A string identifying the type of field to edit (e.g., "Name", "Email", "Phone", "Password").
     * @param currentValue The current value of the field being edited (can be null, e.g., for password change).
     */
    public static void launchEditActivity(Context context, ActivityResultLauncher<Intent> launcher, String fieldType, String currentValue)
    {
        Intent intent = new Intent(context, EditInfoActivity.class);
        intent.putExtra("fieldType", fieldType);
        intent.putExtra("currentValue", currentValue);
        launcher.launch(intent);
    }

    /**
     * Handles the result returned from {@link EditInfoActivity}.
     * If the result is {@link Activity#RESULT_OK} and data is present, it extracts the
     * {@code updatedValue} and {@code fieldType}.
     * Based on {@code fieldType}:
     * <ul>
     *     <li>"Password": Shows a success toast.</li>
     *     <li>"Name": Updates the {@code profileName} TextView and calls {@link #saveUserInfo}.</li>
     *     <li>"Email": Calls {@link #savePendingEmail} to handle email change with verification.</li>
     *     <li>"Phone": Updates the {@code profilePhone} TextView (prefixing with "+") and calls {@link #saveUserInfo}.</li>
     * </ul>
     *
     * @param context The {@link Context} for showing toasts.
     * @param result The {@link androidx.activity.result.ActivityResult} received from {@link EditInfoActivity}.
     * @param userRef The {@link DocumentReference} to the user's document in Firestore.
     * @param auth The {@link FirebaseAuth} instance.
     * @param profileName The {@link TextView} displaying the user's name (can be null if not updating name).
     * @param profilePhone The {@link TextView} displaying the user's phone (can be null if not updating phone).
     */
    public static void handleEditInfoResult(Context context, androidx.activity.result.ActivityResult result, DocumentReference userRef, FirebaseAuth auth, TextView profileName, TextView profilePhone)
    {
        if(result.getResultCode() == Activity.RESULT_OK && result.getData() != null)
        {
            String updatedValue = result.getData().getStringExtra("updatedValue");
            String fieldType = result.getData().getStringExtra("fieldType");

            if(updatedValue != null && fieldType != null)
            {
                if("Password".equals(fieldType))
                {
                    Toast.makeText(context, "Password changed successfully", Toast.LENGTH_SHORT).show();
                } else if("Name".equals(fieldType))
                {
                    if (profileName != null) profileName.setText(updatedValue);
                    saveUserInfo(context, userRef, updatedValue, fieldType);
                } else if("Email".equals(fieldType))
                {
                    savePendingEmail(context, userRef, auth, updatedValue); // updatedValue is the new email
                } else if("Phone".equals(fieldType))
                {
                    if (profilePhone != null) profilePhone.setText("+" + updatedValue); // Assume updatedValue is without '+'
                    saveUserInfo(context, userRef, updatedValue, fieldType); // Pass updatedValue without '+'
                }
            } else {
                Log.w("SettingsUtils", "handleEditInfoResult: updatedValue or fieldType is null.");
            }
        } else {
            Log.d("SettingsUtils", "EditInfoActivity result was not OK or data was null. Result code: " + result.getResultCode());
        }
    }

    /**
     * Saves updated user information (name or phone number) to Firestore.
     * Constructs a data map based on {@code fieldType}.
     * If {@code fieldType} is "Name", it puts {"name": updatedValue}.
     * If {@code fieldType} is "Phone", it puts {"phoneNumber": "+" + updatedValue}.
     * Calls {@link #updateFirestore(Context, DocumentReference, Map)} to perform the update.
     *
     * @param context The {@link Context} for showing toasts.
     * @param userRef The {@link DocumentReference} to the user's document.
     * @param updatedValue The new value for the field.
     * @param fieldType The type of field being updated ("Name" or "Phone").
     */
    public static void saveUserInfo(Context context, DocumentReference userRef, String updatedValue, String fieldType)
    {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if(currentUser != null && userRef != null) // Check userRef as well
        {
            Map<String, Object> updatedData = new HashMap<>();

            if("Name".equals(fieldType))
            {
                updatedData.put("name", updatedValue);
            } else if("Phone".equals(fieldType))
            {
                updatedData.put("phoneNumber", "+" + updatedValue); // Add "+" prefix for phone
            }

            if(!updatedData.isEmpty())
            {
                updateFirestore(context, userRef, updatedData);
            }
        } else {
            Log.e("SettingsUtils", "Cannot save user info: current user or userRef is null.");
        }
    }

    /**
     * Updates the user's Firestore document with the provided data.
     * Shows a success or error toast based on the outcome of the update operation.
     *
     * @param context The {@link Context} for showing toasts.
     * @param userRef The {@link DocumentReference} to the user's document.
     * @param data A {@link Map} containing the fields and values to update.
     */
    private static void updateFirestore(Context context, DocumentReference userRef, Map<String, Object> data)
    {
        if (userRef == null) {
            Log.e("SettingsUtils", "Cannot update Firestore: userRef is null.");
            Toast.makeText(context, "Error: User reference not found.", Toast.LENGTH_SHORT).show();
            return;
        }
        userRef.update(data)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                {
                    Toast.makeText(context, "Error updating profile: " + e.getMessage(), Toast.LENGTH_SHORT).show(); // More informative error
                    Log.e("SettingsUtils", "Error updating Firestore profile", e);
                });
    }

    /**
     * Saves a new email address as a "pendingEmail" in the user's Firestore document.
     * This is typically called after the user requests an email change and Firebase sends
     * a verification email to the new address. The email change is finalized by
     * {@link #syncPendingEmailIfNeeded(DocumentReference, FirebaseAuth, TextView)} once verified.
     * Shows a toast informing the user that a verification email has been sent.
     *
     * @param context The {@link Context} for showing toasts.
     * @param userRef The {@link DocumentReference} to the user's document.
     * @param auth The {@link FirebaseAuth} instance.
     * @param email The new email address to be set as pending.
     */
    public static void savePendingEmail(Context context, DocumentReference userRef, FirebaseAuth auth, String email)
    {
        FirebaseUser currentUser = auth.getCurrentUser();
        if(currentUser != null && userRef != null) // Check userRef
        {
            Map<String, Object> updates = new HashMap<>();
            updates.put("pendingEmail", email);

            userRef.update(updates)
                    .addOnSuccessListener(unused ->
                    {
                        Toast.makeText(context,
                                "Verification email sent to " + email + ". Please verify to complete email update.", // More specific message
                                Toast.LENGTH_LONG).show();
                    })
                    .addOnFailureListener(e ->
                    {
                        Log.e("SettingsUtils", "Failed to save pending email to Firestore.", e);
                        Toast.makeText(context, "Failed to initiate email change: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            Log.e("SettingsUtils", "Cannot save pending email: current user or userRef is null.");
        }
    }

    /**
     * Synchronizes the email address in Firestore with the authenticated user's email,
     * particularly after an email change verification process.
     * It fetches the user's document from Firestore and compares the stored "email" and
     * "pendingEmail" fields with the email from {@link FirebaseUser#getEmail()}.
     * <ul>
     *     <li>If "pendingEmail" exists and matches the authenticated user's current email (meaning verification was successful),
     *         it updates the "email" field in Firestore to this new email and clears "pendingEmail".</li>
     *     <li>If "pendingEmail" exists but does not match the auth email, and the auth email also differs from the
     *         Firestore "email" (unexpected state), it syncs Firestore "email" to the auth email and clears "pendingEmail".</li>
     *     <li>If no "pendingEmail" exists, but the Firestore "email" differs from the auth email,
     *         it syncs Firestore "email" to the auth email.</li>
     * </ul>
     * Updates the {@code profileEmail} TextView with the synchronized email.
     *
     * @param userRef The {@link DocumentReference} to the user's document.
     * @param auth The {@link FirebaseAuth} instance.
     * @param profileEmail The {@link TextView} displaying the user's email, to be updated.
     */
    public static void syncPendingEmailIfNeeded(DocumentReference userRef, FirebaseAuth auth, TextView profileEmail)
    {
        FirebaseUser currentUser = auth.getCurrentUser();
        if(currentUser == null || userRef == null || profileEmail == null) { // Check all refs
            Log.w("SettingsUtils", "Cannot sync pending email: critical objects are null.");
            return;
        }

        userRef.get().addOnSuccessListener(documentSnapshot ->
        {
            if (documentSnapshot.exists())
            {
                String firestoreEmail = documentSnapshot.getString("email");
                String authEmail = currentUser.getEmail(); // Current email from Firebase Auth
                String pendingEmail = documentSnapshot.getString("pendingEmail");

                if (pendingEmail != null) // A pending email change was initiated
                {
                    if (pendingEmail.equals(authEmail)) // New email has been verified and is now the auth email
                    {
                        // Finalize: update Firestore 'email' to new authEmail, clear 'pendingEmail'
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("email", authEmail);
                        updates.put("pendingEmail", null); // Remove pendingEmail field

                        userRef.update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    profileEmail.setText(authEmail);
                                    Log.d("SettingsUtils", "Successfully finalized pending email change to: " + authEmail);
                                })
                                .addOnFailureListener(e -> Log.e("SettingsUtils", "Failed to finalize pending email change in Firestore.", e));
                    } else if (authEmail != null && !authEmail.equals(firestoreEmail) && !authEmail.equals(pendingEmail))
                    {
                        // Auth email changed to something else, not matching pending or current Firestore email.
                        // This is an unexpected state. Prioritize auth email.
                        Log.w("SettingsUtils", "Auth email (" + authEmail + ") differs from Firestore email (" + firestoreEmail + ") and pending email (" + pendingEmail + "). Syncing to auth email.");
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("email", authEmail);
                        updates.put("pendingEmail", null); // Clear pending as it's now irrelevant or outdated

                        userRef.update(updates)
                                .addOnSuccessListener(aVoid -> profileEmail.setText(authEmail))
                                .addOnFailureListener(e -> Log.e("SettingsUtils", "Failed to sync unexpected email change to Firestore.", e));
                    }
                    // If pendingEmail exists but authEmail is still the old firestoreEmail, means verification not yet complete. Do nothing.
                } else // No pending email change was initiated
                {
                    // If Firestore email is out of sync with auth email (e.g., changed via Firebase console or other means)
                    if (firestoreEmail != null && authEmail != null && !firestoreEmail.equals(authEmail))
                    {
                        Log.w("SettingsUtils", "Firestore email (" + firestoreEmail + ") out of sync with auth email (" + authEmail + "). Syncing.");
                        userRef.update("email", authEmail)
                                .addOnSuccessListener(aVoid -> profileEmail.setText(authEmail))
                                .addOnFailureListener(e -> Log.e("SettingsUtils", "Failed to sync Firestore email with auth email.", e));
                    } else if (firestoreEmail == null && authEmail != null) {
                        // Firestore email is null but auth email exists (e.g. new user, email set in auth but not yet in firestore)
                        Log.d("SettingsUtils", "Firestore email is null, auth email is " + authEmail + ". Syncing.");
                        userRef.update("email", authEmail)
                                .addOnSuccessListener(aVoid -> profileEmail.setText(authEmail))
                                .addOnFailureListener(e -> Log.e("SettingsUtils", "Failed to set initial Firestore email from auth email.", e));
                    }
                }
            } else {
                Log.w("SettingsUtils", "User document does not exist for syncPendingEmailIfNeeded.");
            }
        }).addOnFailureListener(e -> Log.e("SettingsUtils", "Failed to get user document for syncPendingEmailIfNeeded.", e));
    }

    /**
     * Sets up a listener for a dark mode {@link Switch}.
     * When the switch state is changed by user interaction ({@code buttonView.isPressed()} is true),
     * it calls {@link #setDarkModePreference(Fragment, boolean, DocumentReference)} to save the preference
     * to Firestore and SharedPreferences, applies the dark mode change system-wide using
     * {@link AppCompatDelegate#setDefaultNightMode(int)}, and shows a confirmation toast.
     *
     * @param fragment The {@link Fragment} context, used for accessing resources and context.
     * @param darkModeSwitch The {@link Switch} UI element for toggling dark mode.
     * @param userSettingsRef The {@link DocumentReference} to the user's settings document in Firestore.
     */
    public static void setupDarkModeSwitch(Fragment fragment, Switch darkModeSwitch, DocumentReference userSettingsRef)
    {
        if (fragment == null || darkModeSwitch == null || userSettingsRef == null) {
            Log.e("SettingsUtils", "Cannot setup dark mode switch: one or more parameters are null.");
            return;
        }
        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
        {
            if(buttonView.isPressed()) // Only react to user-initiated changes
            {
                setDarkModePreference(fragment, isChecked, userSettingsRef); // Save preference

                // Apply system dark mode change
                AppCompatDelegate.setDefaultNightMode(isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

                if(fragment.isAdded() && fragment.getContext() != null) // Check fragment state
                {
                    Toast.makeText(
                            fragment.getContext(),
                            isChecked ? "Dark mode enabled" : "Dark mode disabled",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }
        });
    }

    /**
     * Saves the dark mode preference to both Firestore and SharedPreferences.
     * Updates the "dark_mode" field in the user's Firestore settings document.
     * Saves the "darkMode" boolean preference to SharedPreferences ("FeedMe").
     *
     * @param fragment The {@link Fragment} context, used for accessing SharedPreferences and showing toasts on failure.
     * @param isEnabled True if dark mode is enabled, false otherwise.
     * @param userSettingsRef The {@link DocumentReference} to the user's settings document in Firestore.
     */
    public static void setDarkModePreference(Fragment fragment, boolean isEnabled, DocumentReference userSettingsRef)
    {
        if (userSettingsRef == null || fragment == null || fragment.getContext() == null) {
            Log.e("SettingsUtils", "Cannot set dark mode preference: critical objects are null.");
            return;
        }
        // Save to Firestore
        userSettingsRef.update("dark_mode", isEnabled)
                .addOnSuccessListener(aVoid -> Log.d("SettingsUtils", "Dark mode preference updated in Firestore to: " + isEnabled))
                .addOnFailureListener(e ->
                {
                    if(fragment.isAdded() && fragment.getContext() != null) // Check fragment state
                    {
                        Toast.makeText(fragment.getContext(),
                                "Failed to update dark mode setting in Firestore: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        Log.e("SettingsUtils", "Failed to update dark_mode in Firestore.", e);
                    }
                });

        // Save locally using SharedPreferences
        SharedPreferences sharedPreferences = fragment.requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE); // PREF_NAME used
        sharedPreferences.edit().putBoolean(DARK_MODE_KEY, isEnabled).apply(); // DARK_MODE_KEY used
    }
}