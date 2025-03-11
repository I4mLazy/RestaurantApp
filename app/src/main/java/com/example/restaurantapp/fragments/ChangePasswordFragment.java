package com.example.restaurantapp.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.example.restaurantapp.R;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordFragment extends Fragment
{

    private EditText currentPassword, newPassword, confirmPassword;
    private Button changePasswordButton;
    private Toolbar toolbar;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;

    public ChangePasswordFragment()
    {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_change_password, container, false);

        mAuth = FirebaseAuth.getInstance();

        currentPassword = view.findViewById(R.id.currentPassword);
        newPassword = view.findViewById(R.id.newPassword);
        confirmPassword = view.findViewById(R.id.confirmPassword);
        changePasswordButton = view.findViewById(R.id.changePasswordButton);
        progressBar = view.findViewById(R.id.progressBar);
        toolbar = view.findViewById(R.id.toolbar);

        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        activity.setSupportActionBar(toolbar);
        if (activity.getSupportActionBar() != null)
        {
            activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            activity.getSupportActionBar().setTitle("Change Password");
        }

        toolbar.setNavigationOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        changePasswordButton.setOnClickListener(v -> changePassword());

        return view;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (getActivity() != null)
        {
            androidx.appcompat.widget.Toolbar toolbar = getActivity().findViewById(R.id.toolbar);
            if (toolbar != null)
            {
                toolbar.setTitle("Change Password");
            }
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (getActivity() != null)
        {
            androidx.appcompat.widget.Toolbar toolbar = getActivity().findViewById(R.id.toolbar);
            if (toolbar != null)
            {
                toolbar.setTitle("Settings");
            }
        }
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
        requireActivity().findViewById(R.id.settingsScrollView).setVisibility(View.VISIBLE);
        requireActivity().findViewById(R.id.fragmentContainer).setVisibility(View.GONE);
        requireActivity().findViewById(R.id.toolbar).setVisibility(View.VISIBLE);
    }


    private void changePassword()
    {
        String current = currentPassword.getText().toString().trim();
        String newPass = newPassword.getText().toString().trim();
        String confirmPass = confirmPassword.getText().toString().trim();

        // Check if any field is empty
        if (current.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty())
        {
            Toast.makeText(getContext(), "All fields must be filled", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if new password and confirm password match
        if (!newPass.equals(confirmPass))
        {
            Toast.makeText(getContext(), "New password and confirmed password do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if new password and current password match
        if (newPass.equals(current))
        {
            Toast.makeText(getContext(), "New password and current password are the same", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getEmail() != null)
        {
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), current);

            user.reauthenticate(credential).addOnCompleteListener(reAuthTask ->
            {
                if (reAuthTask.isSuccessful())
                {
                    user.updatePassword(newPass).addOnCompleteListener(task ->
                    {
                        progressBar.setVisibility(View.GONE);
                        if (task.isSuccessful())
                        {
                            Toast.makeText(getContext(), "Password updated successfully", Toast.LENGTH_SHORT).show();
                            // Navigate back to settings
                            requireActivity().getSupportFragmentManager().popBackStack();
                        } else
                        {
                            Toast.makeText(getContext(), "Failed to update password: " +
                                    task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                } else
                {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Current password is incorrect", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

}
