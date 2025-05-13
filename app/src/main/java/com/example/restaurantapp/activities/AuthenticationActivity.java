package com.example.restaurantapp.activities;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.FragmentTransaction;

import com.example.restaurantapp.R;
import com.example.restaurantapp.fragments.LoginFragment;

public class AuthenticationActivity extends AppCompatActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
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
