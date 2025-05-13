package com.example.restaurantapp.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
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
import com.example.restaurantapp.activities.UserMainActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class UserSignUpFragment extends Fragment
{

    private EditText emailEditText, passwordEditText, confirmPasswordEditText, nameEditText, phoneNumberEditText;
    private Button signUpButton;
    private TextView signInRedirectTextView, restaurantSignUpTextView;
    private FirebaseAuth firebaseAuth;

    public UserSignUpFragment()
    {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_user_sign_up, container, false);

        firebaseAuth = FirebaseAuth.getInstance();

        emailEditText = view.findViewById(R.id.editTextEmail);
        passwordEditText = view.findViewById(R.id.editTextPassword);
        confirmPasswordEditText = view.findViewById(R.id.editTextConfirmPassword);
        nameEditText = view.findViewById(R.id.editTextName);
        phoneNumberEditText = view.findViewById(R.id.editTextPhone);
        signUpButton = view.findViewById(R.id.buttonSignUp);
        signInRedirectTextView = view.findViewById(R.id.textViewSignIn);
        restaurantSignUpTextView = view.findViewById(R.id.textViewRestaurantSignUp);

        signUpButton.setOnClickListener(v -> createAccount());

        signInRedirectTextView.setOnClickListener(v ->
        {
            FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.authentication_container, new LoginFragment());
            transaction.commit();
        });

        restaurantSignUpTextView.setOnClickListener(v ->
        {
            FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.authentication_container, new RestaurantSignUpFragment());
            transaction.addToBackStack(null);
            transaction.commit();
        });

        return view;
    }

    private void createAccount()
    {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();
        String name = nameEditText.getText().toString().trim();
        String phoneNumber = phoneNumberEditText.getText().toString().trim();

        // Validate email
        if(TextUtils.isEmpty(email))
        {
            emailEditText.setError("Email is required");
            emailEditText.requestFocus();
            return;
        }
        if(!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches())
        {
            emailEditText.setError("Invalid email format");
            emailEditText.requestFocus();
            return;
        }

        // Validate password confirmation
        if(TextUtils.isEmpty(password))
        {
            passwordEditText.setError("Password is required");
            passwordEditText.requestFocus();
            return;
        }
        if(TextUtils.isEmpty(confirmPassword))
        {
            confirmPasswordEditText.setError("Confirm Password is required");
            confirmPasswordEditText.requestFocus();
            return;
        }
        if(!password.equals(confirmPassword))
        {
            confirmPasswordEditText.setError("Passwords do not match");
            confirmPasswordEditText.requestFocus();
            return;
        }

        // Validate name
        if(TextUtils.isEmpty(name))
        {
            nameEditText.setError("Name is required");
            nameEditText.requestFocus();
            return;
        }

        // Validate phone number
        if(TextUtils.isEmpty(phoneNumber))
        {
            phoneNumberEditText.setError("Phone number is required");
            phoneNumberEditText.requestFocus();
            return;
        }
        if(!phoneNumber.matches("\\d{4,15}"))
        {
            phoneNumberEditText.setError("Enter a valid phone number (7–15 digits)");
            phoneNumberEditText.requestFocus();
            return;
        }

        // All checks passed – create the account
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(getActivity(), task ->
                {
                    if(task.isSuccessful())
                    {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if(user != null)
                        {
                            saveUserData(name, email, user.getUid(), phoneNumber);
                        }
                        Toast.makeText(getActivity(), "Account Created Successfully", Toast.LENGTH_SHORT).show();
                    } else
                    {
                        Toast.makeText(getActivity(), "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserData(String name, String email, String userId, String phoneNumber)
    {
        Map<String, Object> userData = new HashMap<>();
        userData.put("email", email);
        userData.put("name", name);
        userData.put("phoneNumber", "+" + phoneNumber);
        userData.put("profileImageURL", "default_image_url");
        userData.put("address", "");
        userData.put("orderHistory", new ArrayList<String>());
        userData.put("createdAt", FieldValue.serverTimestamp());
        userData.put("userType", "user");

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Users")
                .document(userId)
                .set(userData, SetOptions.merge())
                .addOnSuccessListener(aVoid ->
                {
                    // Store userType in SharedPreferences
                    saveUserTypeToPreferences();

                    Toast.makeText(getActivity(), "User data saved successfully!", Toast.LENGTH_SHORT).show();
                    navigateToMainActivity(firebaseAuth.getCurrentUser());
                })
                .addOnFailureListener(e ->
                {
                    Toast.makeText(getActivity(), "Error saving user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveUserTypeToPreferences()
    {
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("FeedMe", getContext().MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString("userType", "user");
        editor.apply();
    }


    private void navigateToMainActivity(FirebaseUser user)
    {
        if(user != null)
        {
            Intent intent = new Intent(getActivity(), UserMainActivity.class);
            startActivity(intent);
            getActivity().finish();
        }
    }
}
