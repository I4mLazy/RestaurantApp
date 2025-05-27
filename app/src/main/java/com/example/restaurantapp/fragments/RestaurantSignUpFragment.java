package com.example.restaurantapp.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
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

/**
 * A {@link Fragment} subclass that provides a user interface for new restaurant account registration.
 * Users can enter their email, password, restaurant name, address, and phone number.
 * The fragment validates the input, including address validation using Android's Geocoder and
 * OpenStreetMap's Nominatim API as a fallback.
 * Upon successful validation and Firebase Authentication account creation, it saves the restaurant's
 * data (including a geocoded location) to a "Restaurants" collection in Firestore and updates
 * the user's document in the "Users" collection with their type ("restaurant") and a reference
 * to the new restaurant ID. It also stores the user type in SharedPreferences.
 * Finally, it navigates to {@link RestaurantMainActivity}.
 * Provides links to navigate to the login screen ({@link LoginFragment}) or user sign-up screen
 * ({@link UserSignUpFragment}).
 */
public class RestaurantSignUpFragment extends Fragment
{

    /**
     * TextInputEditText for restaurant's email input.
     */
    private TextInputEditText emailEditText;
    /**
     * TextInputEditText for restaurant's password input.
     */
    private TextInputEditText passwordEditText;
    /**
     * TextInputEditText for confirming the restaurant's password.
     */
    private TextInputEditText confirmPasswordEditText;
    /**
     * TextInputEditText for the restaurant's name input.
     */
    private TextInputEditText restaurantNameEditText;
    /**
     * TextInputEditText for the restaurant's address input.
     */
    private TextInputEditText addressEditText;
    /**
     * TextInputEditText for the restaurant's phone number input.
     */
    private TextInputEditText phoneEditText;

    /**
     * TextInputLayout for {@link #emailEditText} to display errors.
     */
    private TextInputLayout emailLayout;
    /**
     * TextInputLayout for {@link #passwordEditText} to display errors.
     */
    private TextInputLayout passwordLayout;
    /**
     * TextInputLayout for {@link #confirmPasswordEditText} to display errors.
     */
    private TextInputLayout confirmPasswordLayout;
    /**
     * TextInputLayout for {@link #restaurantNameEditText} to display errors.
     */
    private TextInputLayout nameLayout;
    /**
     * TextInputLayout for {@link #addressEditText} to display errors.
     */
    private TextInputLayout addressLayout;
    /**
     * TextInputLayout for {@link #phoneEditText} to display errors.
     */
    private TextInputLayout phoneLayout;

    /**
     * Button to initiate the restaurant account creation process.
     */
    private Button signUpButton;
    /**
     * Instance of FirebaseAuth for handling user authentication (account creation).
     */
    private FirebaseAuth firebaseAuth;
    /**
     * TextView that acts as a link to navigate to the login screen.
     */
    private TextView signInRedirectTextView;
    /**
     * TextView that acts as a link to navigate to the regular user sign-up screen.
     */
    private TextView userSignUpTextView;
    /**
     * ProgressBar to indicate the progress of the sign-up process.
     */
    private ProgressBar progressBar;

    /**
     * Required empty public constructor for Fragment instantiation.
     */
    public RestaurantSignUpFragment()
    {
    }

    /**
     * Called to have the fragment instantiate its user interface view.
     * Inflates the layout, initializes UI components (EditTexts, TextInputLayouts, Buttons, ProgressBar, TextViews),
     * and obtains an instance of {@link FirebaseAuth}.
     * Sets up click listeners for:
     * <ul>
     *     <li>Sign Up button: calls {@link #createRestaurantAccount()}.</li>
     *     <li>Sign In redirect text: navigates to {@link LoginFragment}.</li>
     *     <li>User Sign Up text: navigates to {@link UserSignUpFragment}.</li>
     * </ul>
     * Calls {@link #setupErrorClearListeners()} to configure listeners that clear input field errors on focus.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate
     *                           any views in the fragment.
     * @param container          If non-null, this is the parent view that the fragment's
     *                           UI should be attached to. The fragment should not add the view itself,
     *                           but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     *                           from a previous saved state as given here.
     * @return Return the View for the fragment's UI.
     */
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

    /**
     * Attempts to create a new restaurant account.
     * Retrieves and validates input for email, password, confirm password, restaurant name, address, and phone.
     * If any validation fails, sets an error on the corresponding {@link TextInputLayout} and returns.
     * If basic validations pass, it disables the sign-up button, shows a progress bar, and calls
     * {@link #isValidAddress(String, AddressValidationCallback)} to validate the address asynchronously.
     * If the address is valid, it proceeds to create a Firebase user with email and password using
     * {@link FirebaseAuth#createUserWithEmailAndPassword(String, String)}.
     * On successful Firebase user creation, it calls {@link #saveRestaurantData(FirebaseUser, String, String, String, String)}.
     * If Firebase user creation fails, a toast message is shown.
     * Re-enables the sign-up button and hides the progress bar after the process completes or fails.
     */
    private void createRestaurantAccount()
    {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();
        String name = restaurantNameEditText.getText().toString().trim();
        String address = addressEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();

        boolean valid = true;

        if(TextUtils.isEmpty(email))
        {
            emailLayout.setError("Email is required");
            valid = false;
        }
        if(TextUtils.isEmpty(password))
        {
            passwordLayout.setError("Password is required");
            valid = false;
        }
        if(TextUtils.isEmpty(confirmPassword))
        {
            confirmPasswordLayout.setError("Confirm password is required");
            valid = false;
        }
        if(!password.equals(confirmPassword))
        {
            confirmPasswordLayout.setError("Passwords do not match");
            valid = false;
        }
        if(TextUtils.isEmpty(name))
        {
            nameLayout.setError("Restaurant name is required");
            valid = false;
        }
        if(TextUtils.isEmpty(address))
        {
            addressLayout.setError("Address is required");
            valid = false;
        }
        if(TextUtils.isEmpty(phone))
        {
            phoneLayout.setError("Phone number is required");
            valid = false;
        }

        if(!valid) return;

        // Disable the sign-up button and show the progress bar
        signUpButton.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        // Validate address before proceeding
        isValidAddress(address, isValid -> getActivity().runOnUiThread(() ->
        {
            if(!isValid)
            {
                addressLayout.setError("Invalid address. Please enter a valid location.");
                signUpButton.setEnabled(true);
                progressBar.setVisibility(View.GONE);
                return;
            }

            firebaseAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(getActivity(), task ->
                    {
                        if(task.isSuccessful())
                        {
                            FirebaseUser user = firebaseAuth.getCurrentUser();
                            if(user != null)
                            {
                                saveRestaurantData(user, name, email, address, phone);
                            }
                        } else
                        {
                            Toast.makeText(getActivity(), "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            signUpButton.setEnabled(true);
                            progressBar.setVisibility(View.GONE);
                        }
                    });
        }));
    }

    /**
     * Validates an address asynchronously using Android's {@link Geocoder} and, as a fallback,
     * OpenStreetMap's Nominatim API via {@link #validateWithOpenStreetMap(String)}.
     * The result is passed to the provided {@link AddressValidationCallback}.
     * This operation runs on a background thread.
     *
     * @param address  The address string to validate.
     * @param callback The callback to be invoked with the validation result.
     */
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

    /**
     * Validates an address using the OpenStreetMap Nominatim API.
     * Sends a GET request to the Nominatim search endpoint. If the JSON response
     * contains a "lat" field, the address is considered valid.
     *
     * @param address The address string to validate.
     * @return True if the address is found by OpenStreetMap, false otherwise or if an error occurs.
     */
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

    /**
     * Saves the new restaurant's data to Firestore and updates the user's document.
     * Generates a unique ID for the restaurant. Attempts to geocode the provided address
     * to get a {@link GeoPoint} for the location.
     * Creates a restaurant data map including ID, name, address, phone, ownerID, creation timestamp, and location.
     * Creates a user data map to set "userType" to "restaurant", store email, and link to the {@code restaurantId}.
     * Saves the restaurant data to the "Restaurants" collection.
     * Initializes an empty "Menus" subcollection for the new restaurant (with a temporary placeholder document that is then deleted).
     * Merges the user data into the user's document in the "Users" collection.
     * On successful completion of all saves, calls {@link #saveUserTypeToPreferences()} and then
     * {@link #navigateToMainActivity(FirebaseUser)}.
     *
     * @param user    The newly created {@link FirebaseUser} for the restaurant owner.
     * @param name    The name of the restaurant.
     * @param email   The email address of the restaurant.
     * @param address The address of the restaurant.
     * @param phone   The phone number of the restaurant.
     */
    private void saveRestaurantData(FirebaseUser user, String name, String email, String address, String phone)
    {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Generate a unique restaurant ID
        String restaurantId = db.collection("Restaurants").document().getId();

        // Restaurant data
        Map<String, Object> restaurantData = new HashMap<>();

        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        try
        {
            List<Address> locations = geocoder.getFromLocationName(address, 1);
            if(locations != null && !locations.isEmpty())
            {
                Address loc = locations.get(0);
                GeoPoint location = new GeoPoint(loc.getLatitude(), loc.getLongitude());
                restaurantData.put("location", location);
            }
        } catch(IOException e)
        {
            Log.e("GEOCODER", "Failed to geocode address", e);
        }

        restaurantData.put("restaurantID", restaurantId);
        restaurantData.put("name", name);
        restaurantData.put("address", address);
        restaurantData.put("phoneNumber", phone);
        restaurantData.put("ownerID", user.getUid()); // Link owner to restaurant
        restaurantData.put("createdAt", FieldValue.serverTimestamp());

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

    /**
     * Saves the user type ("restaurant") to SharedPreferences ("FeedMe").
     * This allows for quicker determination of user type on subsequent app launches or logins.
     */
    private void saveUserTypeToPreferences()
    {
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("FeedMe", getContext().MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString("userType", "restaurant");
        editor.apply();
    }


    /**
     * Navigates to the {@link RestaurantMainActivity} after successful restaurant account creation and data saving.
     * Finishes the current hosting activity.
     *
     * @param user The authenticated {@link FirebaseUser}. If null, no navigation occurs.
     */
    private void navigateToMainActivity(FirebaseUser user)
    {
        if(user != null)
        {
            startActivity(new Intent(getActivity(), RestaurantMainActivity.class));
            getActivity().finish();
        }
    }

    /**
     * Interface for receiving the result of an asynchronous address validation.
     */
    private interface AddressValidationCallback
    {
        /**
         * Called when address validation is complete.
         *
         * @param isValid True if the address is considered valid, false otherwise.
         */
        void onResult(boolean isValid);
    }

    /**
     * Sets up focus change listeners for all {@link TextInputEditText} fields.
     * When a field gains focus, any error message previously set on its corresponding
     * {@link TextInputLayout} is cleared (set to null).
     */
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