package com.example.restaurantapp.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
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

import com.example.restaurantapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SignUpFragment extends Fragment
{

    private EditText emailEditText, passwordEditText, confirmPasswordEditText;
    private Button signUpButton;
    private TextView signInRedirectTextView;

    private FirebaseAuth firebaseAuth;

    public SignUpFragment() {}

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

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(getActivity(), task ->
                {
                    if (task.isSuccessful())
                    {
                        firebaseAuth.signInWithEmailAndPassword(email, password)
                                .addOnCompleteListener(getActivity(), signInTask ->
                                {
                                    if (signInTask.isSuccessful())
                                    {
                                        FirebaseUser user = firebaseAuth.getCurrentUser();
                                        if (user != null)
                                        {
                                            FirebaseFirestore db = FirebaseFirestore.getInstance();
                                            String userId = user.getUid();

                                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                                            builder.setTitle("Choose a Username");

                                            final EditText input = new EditText(getActivity());
                                            input.setHint("Enter username");
                                            builder.setView(input);

                                            builder.setPositiveButton("OK", (dialog, which) ->
                                            {
                                                String username = input.getText().toString().trim();
                                                if (TextUtils.isEmpty(username))
                                                {
                                                    username = email.split("@")[0];
                                                }
                                                saveUserData(email, username, userId, db);
                                            });

                                            builder.setNegativeButton("Cancel", (dialog, which) ->
                                            {
                                                String username = email.split("@")[0];
                                                saveUserData(email, username, userId, db);
                                            });

                                            builder.show();
                                        }
                                    }
                                    else
                                    {
                                        Toast.makeText(getActivity(), "Sign-in failed: " + signInTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                        Toast.makeText(getActivity(), "Account Created Successfully", Toast.LENGTH_SHORT).show();
                    }
                    else
                    {
                        Toast.makeText(getActivity(), "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserData(String email, String username, String userId, FirebaseFirestore db)
    {
        Map<String, Object> userData = new HashMap<>();
        userData.put("email", email);
        userData.put("username", username);
        userData.put("profileImage", "default_image_url");
        userData.put("address", "");
        userData.put("orderHistory", new ArrayList<String>());
        userData.put("createdAt", System.currentTimeMillis());

        db.collection("Users")
                .document(userId)
                .set(userData, SetOptions.merge())
                .addOnSuccessListener(aVoid ->
                {
                    Toast.makeText(getActivity(), "User data saved successfully!", Toast.LENGTH_SHORT).show();
                    navigateToMainActivity(firebaseAuth.getCurrentUser());
                })
                .addOnFailureListener(e ->
                {
                    Toast.makeText(getActivity(), "Error saving user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateToMainActivity(FirebaseUser user)
    {
        if (user != null)
        {
            Intent intent = new Intent(getActivity(), Main.class);
            startActivity(intent);
            getActivity().finish();
        }
    }
}
