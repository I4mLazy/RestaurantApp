package com.example.restaurantapp.activities;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.FragmentTransaction;

import com.example.restaurantapp.R;
import com.example.restaurantapp.fragments.LoginFragment;

/**
 * Activity responsible for handling user authentication.
 * This activity serves as a container for authentication-related fragments,
 * such as the {@link LoginFragment}. It initializes the UI for authentication
 * and manages the display of the initial login screen.
 */
public class AuthenticationActivity extends AppCompatActivity
{

    /**
     * Called when the activity is first created.
     * This method initializes the activity, sets up the UI, and loads the
     * initial {@link LoginFragment} if the activity is not being restored
     * from a previous state. It also disables night mode for the application
     * and enables edge-to-edge display.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *                           previously being shut down then this Bundle contains the data it most
     *                           recently supplied in {@link #onSaveInstanceState}.
     *                           <b><i>Note: Otherwise it is null.</i></b>
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_authentication);

        if (savedInstanceState == null)
        {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.authentication_container, new LoginFragment());
            transaction.commit();
        }
    }
}