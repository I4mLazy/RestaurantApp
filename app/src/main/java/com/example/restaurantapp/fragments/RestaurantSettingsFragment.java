package com.example.restaurantapp.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.example.restaurantapp.R;
import com.example.restaurantapp.activities.AuthenticationActivity;
import com.google.firebase.auth.FirebaseAuth;

public class RestaurantSettingsFragment extends Fragment
{
    Button logoutButton;
    FirebaseAuth auth;

    public RestaurantSettingsFragment()
    {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_restaurant_settings, container, false);

        auth = FirebaseAuth.getInstance();
        logoutButton = view.findViewById(R.id.logoutButton);

        logoutButton.setOnClickListener(v -> handleLogout());

        return view;
    }

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

                        // Redirect to authentication
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

}
