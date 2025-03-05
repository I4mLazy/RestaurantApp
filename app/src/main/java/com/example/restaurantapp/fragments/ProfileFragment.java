package com.example.restaurantapp.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.example.restaurantapp.R;
import com.example.restaurantapp.activities.AuthenticationActivity;
import com.google.firebase.auth.FirebaseAuth;

public class ProfileFragment extends Fragment
{

    public ProfileFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        Button editProfileButton = view.findViewById(R.id.editProfileButton);
        editProfileButton.setOnClickListener(v ->
        {
            ProfileEditFragment profileEditFragment = new ProfileEditFragment();
            if (getActivity() != null)
            {
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, profileEditFragment)
                        .addToBackStack(null)
                        .commit();
            }
        });

        Button settingsButton = view.findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(v ->
        {
            SettingsFragment settingsFragment = new SettingsFragment();
            if (getActivity() != null)
            {
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, settingsFragment)
                        .addToBackStack(null)
                        .commit();
            }
        });

        Button logoutButton = view.findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(v ->
        {
            FirebaseAuth.getInstance().signOut();

            SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("MyAppPrefs", FragmentActivity.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.apply();

            Intent intent = new Intent(getActivity(), AuthenticationActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
        });

        return view;
    }
}
