package com.example.restaurantapp.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Switch;
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

public class SettingsUtils
{

    private static final String PREF_NAME = "FeedMe";
    private static final String DARK_MODE_KEY = "darkMode";

    // Loads and applies the user's settings (e.g., dark mode)
    public static void loadUserSettings(Context context)
    {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();  // FirebaseAuth instance to get the current user
        FirebaseFirestore db = FirebaseFirestore.getInstance();  // Firestore instance

        String userID = mAuth.getCurrentUser().getUid();

        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean(DARK_MODE_KEY, false); // Default to false (light mode)

        // Apply dark mode based on shared preference
        if(isDarkMode)
        {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else
        {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

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
                                // Save this to SharedPreferences
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putBoolean(DARK_MODE_KEY, darkModeFromFirestore);
                                editor.apply();

                                // Apply the dark mode setting from Firestore
                                AppCompatDelegate.setDefaultNightMode(darkModeFromFirestore ?
                                        AppCompatDelegate.MODE_NIGHT_YES :
                                        AppCompatDelegate.MODE_NIGHT_NO);
                            }
                        }
                    })
                    .addOnFailureListener(e ->
                    {
                        Toast.makeText(context, "Failed to load settings: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

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

                        // Clear shared preferences
                        SharedPreferences sharedPreferences = activity.getSharedPreferences("FeedMe", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.clear();
                        editor.apply();

                        // Redirect to authentication
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
        }
    }

    public static void launchEditActivity(Context context, ActivityResultLauncher<Intent> launcher, String fieldType, String currentValue)
    {
        Intent intent = new Intent(context, EditInfoActivity.class);
        intent.putExtra("fieldType", fieldType);
        intent.putExtra("currentValue", currentValue);
        launcher.launch(intent);
    }

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
                    profileName.setText(updatedValue);
                    saveUserInfo(context, userRef, updatedValue, fieldType);
                } else if("Email".equals(fieldType))
                {
                    savePendingEmail(context, userRef, auth, updatedValue);
                } else if("Phone".equals(fieldType))
                {
                    profilePhone.setText("+" + updatedValue);
                    saveUserInfo(context, userRef, updatedValue, fieldType);
                }
            }
        }
    }

    public static void saveUserInfo(Context context, DocumentReference userRef, String updatedValue, String fieldType)
    {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if(currentUser != null)
        {
            Map<String, Object> updatedData = new HashMap<>();

            if("Name".equals(fieldType))
            {
                updatedData.put("name", updatedValue);
            } else if("Phone".equals(fieldType))
            {
                updatedData.put("phoneNumber", "+" + updatedValue);
            }

            if(!updatedData.isEmpty())
            {
                updateFirestore(context, userRef, updatedData);
            }
        }
    }

    private static void updateFirestore(Context context, DocumentReference userRef, Map<String, Object> data)
    {
        userRef.update(data)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                {
                    Toast.makeText(context, "Error updating profile", Toast.LENGTH_SHORT).show();
                    Log.e("SettingsUtils", "Error updating profile", e);
                });
    }

    public static void savePendingEmail(Context context, DocumentReference userRef, FirebaseAuth auth, String email)
    {
        FirebaseUser currentUser = auth.getCurrentUser();

        if(currentUser != null)
        {
            Map<String, Object> updates = new HashMap<>();
            updates.put("pendingEmail", email); // We'll finalize it later

            userRef.update(updates)
                    .addOnSuccessListener(unused ->
                    {
                        Toast.makeText(context,
                                "Verification email sent. Please verify to complete email update.",
                                Toast.LENGTH_LONG).show();
                    })
                    .addOnFailureListener(e ->
                    {
                        Log.e("SettingsUtils", "Failed to save pending email", e);
                    });
        }
    }

    public static void syncPendingEmailIfNeeded(Context context, DocumentReference userRef, FirebaseAuth auth, TextView profileEmail)
    {
        FirebaseUser currentUser = auth.getCurrentUser();
        if(currentUser == null) return;

        userRef.get().addOnSuccessListener(documentSnapshot ->
        {
            if(documentSnapshot.exists())
            {
                String firestoreEmail = documentSnapshot.getString("email");
                String authEmail = currentUser.getEmail();

                String pendingEmail = documentSnapshot.getString("pendingEmail");

                if(pendingEmail != null)
                {
                    if(pendingEmail.equals(authEmail))
                    {
                        // Verified new email, finalize change
                        userRef.update("email", authEmail)
                                .addOnSuccessListener(aVoid ->
                                {
                                    profileEmail.setText(authEmail);
                                    userRef.update("pendingEmail", null);
                                })
                                .addOnFailureListener(e -> Log.e("SettingsUtils", "Failed to finalize email change", e));
                    } else if(!authEmail.equals(firestoreEmail))
                    {
                        // Auth email changed unexpectedly — sync Firestore and clear pending
                        Log.w("SettingsUtils", "Auth email differs from pendingEmail and Firestore email. Syncing and clearing pending.");
                        userRef.update("email", authEmail)
                                .addOnSuccessListener(aVoid ->
                                {
                                    profileEmail.setText(authEmail);
                                    userRef.update("pendingEmail", null);
                                })
                                .addOnFailureListener(e -> Log.e("SettingsUtils", "Failed to sync unexpected email change", e));
                    }
                } else
                {
                    // No pending email, sync Firestore if out of date
                    if(firestoreEmail != null && !firestoreEmail.equals(authEmail))
                    {
                        userRef.update("email", authEmail)
                                .addOnSuccessListener(aVoid ->
                                {
                                    profileEmail.setText(authEmail);
                                    userRef.update("pendingEmail", null);
                                })
                                .addOnFailureListener(e -> Log.e("SettingsUtils", "Failed to sync email", e));
                    }
                }
            }
        }).addOnFailureListener(e -> Log.e("SettingsUtils", "Failed to get user doc", e));
    }

    public static void setupDarkModeSwitch(Fragment fragment, Switch darkModeSwitch, DocumentReference userSettingsRef)
    {
        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
        {
            if(buttonView.isPressed())
            {  // only user-initiated changes
                // Update Firestore + SharedPreferences
                setDarkModePreference(fragment, isChecked, userSettingsRef);

                // Apply system dark mode change
                if(isChecked)
                {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                } else
                {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                }

                // Show toast confirmation
                if(fragment.isAdded() && fragment.getContext() != null)
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

    // Function to save the dark mode preference to Firestore and SharedPreferences
    public static void setDarkModePreference(Fragment fragment, boolean isEnabled, DocumentReference userSettingsRef)
    {
        // Save to Firestore
        userSettingsRef.update("dark_mode", isEnabled)
                .addOnFailureListener(e ->
                {
                    if(fragment.isAdded() && fragment.getContext() != null)
                    {
                        Toast.makeText(fragment.getContext(),
                                "Failed to update dark mode setting: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });

        // Save locally using SharedPreferences
        if(fragment.isAdded() && fragment.getContext() != null)
        {
            SharedPreferences sharedPreferences = fragment.getContext().getSharedPreferences("FeedMe", Context.MODE_PRIVATE);
            sharedPreferences.edit().putBoolean("darkMode", isEnabled).apply();
        }
    }

}
