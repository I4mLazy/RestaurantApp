package com.example.restaurantapp.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

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
}
