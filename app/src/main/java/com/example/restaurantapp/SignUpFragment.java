package com.example.restaurantapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SignUpFragment extends Fragment
{

    private EditText emailEditText, passwordEditText, confirmPasswordEditText;
    private Button signUpButton;
    private TextView signInRedirectTextView;

    private FirebaseAuth firebaseAuth;

    public SignUpFragment()
    {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_sign_up, container, false);

        firebaseAuth = FirebaseAuth.getInstance();

        emailEditText = view.findViewById(R.id.editTextEmail);
        passwordEditText = view.findViewById(R.id.editTextPassword);
        confirmPasswordEditText = view.findViewById(R.id.editTextConfirmPassword);
        signUpButton = view.findViewById(R.id.buttonSignUp);
        signInRedirectTextView = view.findViewById(R.id.textViewSignIn);

        signUpButton.setOnClickListener(v -> createAccount());

        signInRedirectTextView.setOnClickListener(v ->
        {
            LoginFragment LoginFragment = new LoginFragment();
            FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.authentication_container, LoginFragment);
            transaction.commit();
        });

        return view;
    }

    private void createAccount()
    {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(email))
        {
            emailEditText.setError("Email is required");
            return;
        }
        if (TextUtils.isEmpty(password))
        {
            passwordEditText.setError("Password is required");
            return;
        }
        if (TextUtils.isEmpty(confirmPassword))
        {
            confirmPasswordEditText.setError("Confirm Password is required");
            return;
        }
        if (!password.equals(confirmPassword))
        {
            confirmPasswordEditText.setError("Passwords do not match");
            return;
        }

        // Create the account with Firebase Authentication
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(getActivity(), task ->
                {
                    if (task.isSuccessful())
                    {
                        // If registration is successful, log the user in
                        firebaseAuth.signInWithEmailAndPassword(email, password)
                                .addOnCompleteListener(getActivity(), signInTask ->
                                {
                                    if (signInTask.isSuccessful())
                                    {
                                        FirebaseUser user = firebaseAuth.getCurrentUser();
                                        navigateToMainActivity(user);
                                    } else
                                    {
                                        Toast.makeText(getActivity(), "Sign-in failed: " + signInTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                        Toast.makeText(getActivity(), "Account Created Successfully", Toast.LENGTH_SHORT).show();

                    } else
                    {
                        Toast.makeText(getActivity(), "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateToMainActivity(FirebaseUser user) {
        if (user != null)
        {
            Intent intent = new Intent(getActivity(), Main.class);
            startActivity(intent);
            getActivity().finish();
        }
    }

}