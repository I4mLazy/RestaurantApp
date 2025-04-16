package com.example.restaurantapp.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.restaurantapp.R;
import com.example.restaurantapp.activities.RestaurantMainActivity;
import com.example.restaurantapp.activities.UserMainActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RestaurantSignUpFragment extends Fragment
{

    private TextInputEditText emailEditText, passwordEditText, confirmPasswordEditText, restaurantNameEditText, addressEditText, phoneEditText;
    private TextInputLayout emailLayout, passwordLayout, confirmPasswordLayout, nameLayout, addressLayout, phoneLayout;
    private Button signUpButton;
    private FirebaseAuth firebaseAuth;
    private TextView signInRedirectTextView, userSignUpTextView;
    private ProgressBar progressBar;

    public RestaurantSignUpFragment()
    {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_restaurant_sign_up, container, false);

        firebaseAuth = FirebaseAuth.getInstance();

        emailLayout = view.findViewById(R.id.textInputLayoutEmail);
        passwordLayout = view.findViewById(R.id.textInputLayoutPassword);
        confirmPasswordLayout = view.findViewById(R.id.textInputLayoutConfirmPassword);
        nameLayout = view.findViewById(R.id.textInputLayoutRestaurantName);
        addressLayout = view.findViewById(R.id.textInputLayoutAddress);
        phoneLayout = view.findViewById(R.id.textInputLayoutPhone);

        emailEditText = view.findViewById(R.id.editTextRestaurantEmail);
        passwordEditText = view.findViewById(R.id.editTextRestaurantPassword);
        confirmPasswordEditText = view.findViewById(R.id.editTextRestaurantConfirmPassword);
        restaurantNameEditText = view.findViewById(R.id.editTextRestaurantName);
        addressEditText = view.findViewById(R.id.editTextRestaurantAddress);
        phoneEditText = view.findViewById(R.id.editTextRestaurantPhone);
        signUpButton = view.findViewById(R.id.buttonRestaurantSignUp);
        progressBar = view.findViewById(R.id.progressBarSignUp);
        signInRedirectTextView = view.findViewById(R.id.textViewSignIn);
        userSignUpTextView = view.findViewById(R.id.textViewUserSignUp);

        signUpButton.setOnClickListener(v -> createRestaurantAccount());

        signInRedirectTextView.setOnClickListener(v ->
        {
            LoginFragment loginFragment = new LoginFragment();
            FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.authentication_container, loginFragment);
            transaction.commit();
        });

        userSignUpTextView.setOnClickListener(v ->
        {
            UserSignUpFragment userSignUpFragment = new UserSignUpFragment();
            FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.authentication_container, userSignUpFragment);
            transaction.addToBackStack(null);
            transaction.commit();
        });

        setupErrorClearListeners();
        return view;
    }

    private void createRestaurantAccount()
    {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();
        String name = restaurantNameEditText.getText().toString().trim();
        String address = addressEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();

        boolean valid = true;

        if (TextUtils.isEmpty(email))
        {
            emailLayout.setError("Email is required");
            valid = false;
        }
        if (TextUtils.isEmpty(password))
        {
            passwordLayout.setError("Password is required");
            valid = false;
        }
        if (TextUtils.isEmpty(confirmPassword))
        {
            confirmPasswordLayout.setError("Confirm password is required");
            valid = false;
        }
        if (!password.equals(confirmPassword))
        {
            confirmPasswordLayout.setError("Passwords do not match");
            valid = false;
        }
        if (TextUtils.isEmpty(name))
        {
            nameLayout.setError("Restaurant name is required");
            valid = false;
        }
        if (TextUtils.isEmpty(address))
        {
            addressLayout.setError("Address is required");
            valid = false;
        }
        if (TextUtils.isEmpty(phone))
        {
            phoneLayout.setError("Phone number is required");
            valid = false;
        }

        if (!valid) return;

        // Disable the sign-up button and show the progress bar
        signUpButton.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        // Validate address before proceeding
        isValidAddress(address, isValid -> getActivity().runOnUiThread(() ->
        {
            if (!isValid)
            {
                addressLayout.setError("Invalid address. Please enter a valid location.");
                signUpButton.setEnabled(true);
                progressBar.setVisibility(View.GONE);
                return;
            }

            firebaseAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(getActivity(), task ->
                    {
                        if (task.isSuccessful())
                        {
                            FirebaseUser user = firebaseAuth.getCurrentUser();
                            if (user != null)
                            {
                                saveRestaurantData(user, name, email, address, phone);
                            }
                        }
                        else
                        {
                            Toast.makeText(getActivity(), "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            signUpButton.setEnabled(true);
                            progressBar.setVisibility(View.GONE);
                        }
                    });
        }));
    }

    private void isValidAddress(String address, AddressValidationCallback callback)
    {
        new Thread(() ->
        {
            Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
            try
            {
                List<Address> addresses = geocoder.getFromLocationName(address, 1);
                if(addresses != null && !addresses.isEmpty())
                {
                    callback.onResult(true); //Valid address
                    return;
                }
            } catch(IOException e)
            {
                e.printStackTrace();
            }

            //If Geocoder fails, try OpenStreetMap API
            boolean isValid = validateWithOpenStreetMap(address);
            callback.onResult(isValid);
        }).start();
    }

    private boolean validateWithOpenStreetMap(String address)
    {
        try
        {
            String urlString = "https://nominatim.openstreetmap.org/search?q=" + URLEncoder.encode(address, "UTF-8") + "&format=json&limit=1";
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String response = reader.readLine();
            reader.close();

            return response != null && response.contains("\"lat\":");
        } catch(Exception e)
        {
            e.printStackTrace();
            return false;
        }
    }

    private void saveRestaurantData(FirebaseUser user, String name, String email, String address, String phone)
    {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Generate a unique restaurant ID
        String restaurantId = db.collection("Restaurants").document().getId();

        // Restaurant data
        Map<String, Object> restaurantData = new HashMap<>();
        restaurantData.put("restaurantID", restaurantId);
        restaurantData.put("name", name);
        restaurantData.put("address", address);
        restaurantData.put("phoneNumber", phone);
        restaurantData.put("ownerID", user.getUid()); // Link owner to restaurant

        // User data update (link user to restaurant)
        Map<String, Object> userData = new HashMap<>();
        userData.put("userType", "restaurant");
        userData.put("email", email);
        userData.put("restaurantId", restaurantId); // Store restaurant reference

        // Save restaurant data
        db.collection("Restaurants").document(restaurantId).set(restaurantData).addOnSuccessListener(aVoid ->
        {
            // Initialize empty menu subcollection
            db.collection("Restaurants").document(restaurantId).collection("Menus").document("placeholder").set(new HashMap<>()).addOnSuccessListener(aVoid2 ->
            {
                // Remove placeholder if not needed
                db.collection("Restaurants").document(restaurantId).collection("Menus").document("placeholder").delete();

                // Save user data
                db.collection("Users").document(user.getUid()).set(userData, SetOptions.merge()).addOnSuccessListener(aVoid3 ->
                {
                    // Store only userType in SharedPreferences
                    saveUserTypeToPreferences();

                    navigateToMainActivity(user);
                });
            });
        }).addOnFailureListener(e -> Toast.makeText(getActivity(), "Failed to save restaurant: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void saveUserTypeToPreferences()
    {
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("FeedMe", getContext().MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString("userType", "restaurant");
        editor.apply();
    }


    private void navigateToMainActivity(FirebaseUser user)
    {
        if(user != null)
        {
            startActivity(new Intent(getActivity(), RestaurantMainActivity.class));
            getActivity().finish();
        }
    }

    private interface AddressValidationCallback
    {
        void onResult(boolean isValid);
    }

    private void setupErrorClearListeners()
    {
        emailEditText.setOnFocusChangeListener((v, hasFocus) ->
        {
            if(hasFocus) emailLayout.setError(null);
        });

        passwordEditText.setOnFocusChangeListener((v, hasFocus) ->
        {
            if(hasFocus) passwordLayout.setError(null);
        });

        confirmPasswordEditText.setOnFocusChangeListener((v, hasFocus) ->
        {
            if(hasFocus) confirmPasswordLayout.setError(null);
        });

        restaurantNameEditText.setOnFocusChangeListener((v, hasFocus) ->
        {
            if(hasFocus) nameLayout.setError(null);
        });

        addressEditText.setOnFocusChangeListener((v, hasFocus) ->
        {
            if(hasFocus) addressLayout.setError(null);
        });

        phoneEditText.setOnFocusChangeListener((v, hasFocus) ->
        {
            if(hasFocus) phoneLayout.setError(null);
        });
    }
}
