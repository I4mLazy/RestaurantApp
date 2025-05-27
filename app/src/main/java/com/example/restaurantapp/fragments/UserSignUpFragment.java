package com.example.restaurantapp.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment; // Standard Fragment import

import android.text.TextUtils;
import android.util.Log;
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


/**
 * A {@link Fragment} subclass that provides a user interface for new regular user account registration.
 * Users can enter their email, password, name, and phone number.
 * The fragment validates the input fields. Upon successful validation and Firebase Authentication
 * account creation, it saves additional user data (name, phone, default profile image URL,
 * empty address, empty order history, creation timestamp, and userType "user") to a "Users"
 * collection in Firestore. It also stores the user type in SharedPreferences.
 * Finally, it navig инноваates to {@link UserMainActivity}.
 * Provides links to navigate to the login screen ({@link LoginFragment}) or restaurant sign-up screen
 * ({@link RestaurantSignUpFragment}).
 */
public class UserSignUpFragment extends Fragment
{

    /**
     * EditText for user's email input.
     */
    private EditText emailEditText;
    /**
     * EditText for user's password input.
     */
    private EditText passwordEditText;
    /**
     * EditText for confirming the user's password.
     */
    private EditText confirmPasswordEditText;
    /**
     * EditText for user's name input.
     */
    private EditText nameEditText;
    /**
     * EditText for user's phone number input.
     */
    private EditText phoneNumberEditText;
    /**
     * Button to initiate the user account creation process.
     */
    private Button signUpButton;
    /**
     * TextView that acts as a link to navigate to the login screen.
     */
    private TextView signInRedirectTextView;
    /**
     * TextView that acts as a link to navigate to the restaurant sign-up screen.
     */
    private TextView restaurantSignUpTextView;
    /**
     * Instance of FirebaseAuth for handling user authentication (account creation).
     */
    private FirebaseAuth firebaseAuth;

    /**
     * Required empty public constructor for Fragment instantiation.
     */
    public UserSignUpFragment()
    {
        // Required empty public constructor
    }

    /**
     * Called to have the fragment instantiate its user interface view.
     * Inflates the layout, initializes UI components (EditTexts, Button, TextViews),
     * and obtains an instance of {@link FirebaseAuth}.
     * Sets up click listeners for:
     * <ul>
     *     <li>Sign Up button: calls {@link #createAccount()}.</li>
     *     <li>Sign In redirect text: navigates to {@link LoginFragment}.</li>
     *     <li>Restaurant Sign Up text: navigates to {@link RestaurantSignUpFragment}.</li>
     * </ul>
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
            transaction.replace(R.id.authentication_container, new LoginFragment()); // Assumes R.id.authentication_container
            transaction.commit();
        });

        restaurantSignUpTextView.setOnClickListener(v ->
        {
            FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.authentication_container, new RestaurantSignUpFragment()); // Assumes R.id.authentication_container
            transaction.addToBackStack(null); // Allow back navigation to this fragment
            transaction.commit();
        });

        return view;
    }

    /**
     * Attempts to create a new user account.
     * Retrieves and validates input for email, password, confirm password, name, and phone number.
     * Validations include checking for empty fields, email format, password match, and phone number format (7-15 digits).
     * If any validation fails, sets an error on the corresponding EditText and requests focus.
     * If all validations pass, it calls {@link FirebaseAuth#createUserWithEmailAndPassword(String, String)}
     * to create a Firebase user.
     * On successful Firebase user creation, it calls {@link #saveUserData(String, String, String, String)}
     * and shows a success toast.
     * If Firebase user creation fails, a toast message indicating the failure is shown.
     */
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
        if(!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) // Standard Android email pattern validation
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

        // Validate phone number (digits only, length 4-15)
        if(TextUtils.isEmpty(phoneNumber))
        {
            phoneNumberEditText.setError("Phone number is required");
            phoneNumberEditText.requestFocus();
            return;
        }
        if(!phoneNumber.matches("\\d{4,15}")) // Regex for 4 to 15 digits
        {
            phoneNumberEditText.setError("Enter a valid phone number (4–15 digits)"); // Adjusted message for 4-15 digits
            phoneNumberEditText.requestFocus();
            return;
        }

        // All checks passed – create the account
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(getActivity(), task -> // Use getActivity() for context
                {
                    if(task.isSuccessful())
                    {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if(user != null)
                        {
                            saveUserData(name, email, user.getUid(), phoneNumber);
                            // navigateToMainActivity will be called from saveUserData
                        }
                        // Toast.makeText(getActivity(), "Account Created Successfully", Toast.LENGTH_SHORT).show(); // Moved to saveUserData success
                    } else
                    {
                        Toast.makeText(getActivity(), "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show(); // Longer toast for error
                    }
                });
    }

    /**
     * Saves additional user data to Firestore after successful Firebase Authentication account creation.
     * Creates a data map including email, name, phone number (prefixed with "+"), a default profile image URL,
     * an empty address string, an empty order history list, a server timestamp for creation, and "userType" set to "user".
     * This data is set (merged) into the user's document in the "Users" collection using their UID.
     * On successful save, it calls {@link #saveUserTypeToPreferences()}, shows a success toast,
     * and then calls {@link #navigateToMainActivity(FirebaseUser)}.
     * Shows an error toast if saving user data fails.
     *
     * @param name        The user's name.
     * @param email       The user's email address.
     * @param userId      The UID of the newly created Firebase user.
     * @param phoneNumber The user's phone number (without country code prefix).
     */
    private void saveUserData(String name, String email, String userId, String phoneNumber)
    {
        Map<String, Object> userData = new HashMap<>();
        userData.put("email", email);
        userData.put("name", name);
        userData.put("phoneNumber", "+" + phoneNumber); // Add "+" prefix
        userData.put("profileImageURL", "default_image_url"); // Default placeholder
        userData.put("address", ""); // Empty address initially
        userData.put("orderHistory", new ArrayList<String>()); // Empty order history
        userData.put("createdAt", FieldValue.serverTimestamp());
        userData.put("userType", "user");

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Users")
                .document(userId)
                .set(userData, SetOptions.merge()) // Use merge to avoid overwriting if doc somehow exists
                .addOnSuccessListener(aVoid ->
                {
                    saveUserTypeToPreferences(); // Store userType locally
                    Toast.makeText(getActivity(), "Account Created Successfully. User data saved.", Toast.LENGTH_SHORT).show(); // Combined toast
                    navigateToMainActivity(firebaseAuth.getCurrentUser()); // Navigate after data save
                })
                .addOnFailureListener(e ->
                {
                    Toast.makeText(getActivity(), "Error saving user data: " + e.getMessage(), Toast.LENGTH_LONG).show(); // Longer toast for error
                    // Consider what to do if user auth succeeded but Firestore save failed (e.g., delete auth user or prompt retry)
                });
    }

    /**
     * Saves the user type ("user") to SharedPreferences ("FeedMe").
     * This allows for quicker determination of user type on subsequent app launches or logins.
     */
    private void saveUserTypeToPreferences()
    {
        if(getContext() == null || getActivity() == null) return; // Check context
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("FeedMe", android.content.Context.MODE_PRIVATE); // Corrected context usage
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("userType", "user");
        editor.apply();
    }


    /**
     * Navigates to the {@link UserMainActivity} after successful user account creation and data saving.
     * Finishes the current hosting activity.
     *
     * @param user The authenticated {@link FirebaseUser}. If null, no navigation occurs.
     */
    private void navigateToMainActivity(FirebaseUser user)
    {
        if(user != null && getActivity() != null) // Check getActivity()
        {
            Intent intent = new Intent(getActivity(), UserMainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear back stack
            startActivity(intent);
            getActivity().finish(); // Finish the AuthenticationActivity
        } else if(getActivity() == null)
        {
            Log.e("UserSignUpFragment", "Cannot navigate to main activity: getActivity() is null.");
        }
    }
}