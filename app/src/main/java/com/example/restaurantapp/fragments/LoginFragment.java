package com.example.restaurantapp.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.restaurantapp.R;
import com.example.restaurantapp.activities.RestaurantMainActivity;
import com.example.restaurantapp.activities.UserMainActivity;
// Unused imports: AuthCredential, GoogleAuthProvider
// import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
// import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * A {@link Fragment} subclass that provides a user interface for user login.
 * Users can log in using their email and password. It also provides navigation
 * to sign-up ({@link UserSignUpFragment}) and password recovery ({@link ForgotPasswordFragment}) screens.
 * Upon successful login, it determines the user type (regular user or restaurant)
 * and navigates to the appropriate main activity ({@link UserMainActivity} or {@link RestaurantMainActivity}).
 * User type is cached in SharedPreferences for faster subsequent logins.
 */
public class LoginFragment extends Fragment
{

    /**
     * Tag for logging purposes.
     */
    private static final String TAG = "LoginFragment";

    /**
     * EditText field for the user to input their email address.
     */
    private EditText editTextEmail;
    /**
     * EditText field for the user to input their password.
     */
    private EditText editTextPassword;
    /**
     * Button to initiate the login process.
     */
    private Button buttonLogin;
    /**
     * TextView that acts as a link to navigate to the sign-up screen.
     */
    private TextView textViewSignUp;
    /**
     * TextView that acts as a link to navigate to the forgot password screen.
     */
    private TextView textViewForgotPassword;

    /**
     * Instance of FirebaseAuth for handling user authentication.
     */
    private FirebaseAuth firebaseAuth;

    /**
     * Required empty public constructor for Fragment instantiation.
     */
    public LoginFragment()
    {
        // Required empty public constructor
    }

    /**
     * Called to have the fragment instantiate its user interface view.
     * This method inflates the layout for the fragment, initializes UI components
     * (EditTexts for email/password, Button for login, TextViews for sign-up/forgot password),
     * and obtains an instance of {@link FirebaseAuth}.
     * It sets up click listeners for:
     * <ul>
     *     <li>Login button: calls {@link #loginWithEmailPassword()}.</li>
     *     <li>Forgot Password text: navigates to {@link ForgotPasswordFragment}.</li>
     *     <li>Sign Up text: navigates to {@link UserSignUpFragment}.</li>
     * </ul>
     *
     * @param inflater           The LayoutInflater object that can be used to inflate
     *                           any views in the fragment.
     * @param container          If non-null, this is the parent view that the fragment's
     *                           UI should be attached to. The fragment should not add the view itself,
     *                           but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     *                           from a previous saved state as given here.
     * @return Return the View for the fragment's UI, or null.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        editTextEmail = view.findViewById(R.id.editTextEmail);
        editTextPassword = view.findViewById(R.id.editTextPassword);
        buttonLogin = view.findViewById(R.id.buttonLogin);
        textViewSignUp = view.findViewById(R.id.textViewSignUp);
        textViewForgotPassword = view.findViewById(R.id.textViewForgotPassword);

        firebaseAuth = FirebaseAuth.getInstance();

        buttonLogin.setOnClickListener(v -> loginWithEmailPassword());

        // Handle forgot password
        textViewForgotPassword.setOnClickListener(v ->
        {
            ForgotPasswordFragment forgotPasswordFragment = new ForgotPasswordFragment();
            FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.authentication_container, forgotPasswordFragment); // Assumes R.id.authentication_container is the ID in the activity
            transaction.commit();
        });

        textViewSignUp.setOnClickListener(v ->
        {
            UserSignUpFragment signUpFragment = new UserSignUpFragment();
            FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.authentication_container, signUpFragment); // Assumes R.id.authentication_container
            transaction.commit();
        });

        return view;
    }

    /**
     * Attempts to log in the user with the provided email and password.
     * Retrieves email and password from the input fields. If either is empty,
     * a toast message is shown. Otherwise, it calls
     * {@link FirebaseAuth#signInWithEmailAndPassword(String, String)}.
     * On successful authentication, it calls {@link #navigateToMainActivity(FirebaseUser)}.
     * On failure, a toast message indicating authentication failure is shown.
     */
    private void loginWithEmailPassword()
    {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if(email.isEmpty() || password.isEmpty())
        {
            Toast.makeText(getContext(), "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), task ->
                {
                    if(task.isSuccessful())
                    {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        navigateToMainActivity(user); // Proceed to navigate
                    } else
                    {
                        Toast.makeText(getContext(), "Authentication failed. Check credentials.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Navigates to the appropriate main activity based on the user's type.
     * If the user is null, the method returns.
     * It first checks SharedPreferences for a stored "userType". If found, it calls
     * {@link #launchMainActivity(String)} immediately.
     * If not found in SharedPreferences, it fetches the "userType" field from the user's
     * document in the "Users" collection in Firestore. Upon successful fetch, it stores
     * the user type in SharedPreferences and then calls {@link #launchMainActivity(String)}.
     * Toasts are shown if fetching the user type fails or if the type is not determined.
     *
     * @param user The currently authenticated {@link FirebaseUser}.
     */
    private void navigateToMainActivity(FirebaseUser user)
    {
        if(user == null)
        {
            Log.w(TAG, "navigateToMainActivity called with null user.");
            return;
        }

        SharedPreferences prefs = requireActivity().getSharedPreferences("FeedMe", Context.MODE_PRIVATE);
        String userType = prefs.getString("userType", null);

        if(userType != null)
        {
            // If userType is already stored, navigate instantly
            launchMainActivity(userType);
            return;
        }

        // If userType is not stored, fetch from Firestore and save it
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userRef = db.collection("Users").document(user.getUid());

        userRef.get().addOnCompleteListener(task ->
        {
            if(task.isSuccessful() && task.getResult() != null && task.getResult().exists()) // Added null check for task.getResult()
            {
                String fetchedUserType = task.getResult().getString("userType");

                if(fetchedUserType != null)
                {
                    // Store the fetched userType in SharedPreferences
                    prefs.edit().putString("userType", fetchedUserType).apply();
                    launchMainActivity(fetchedUserType);
                } else
                {
                    Log.e(TAG, "User type field is null in Firestore for user: " + user.getUid());
                    Toast.makeText(getContext(), "Failed to determine user type from database.", Toast.LENGTH_SHORT).show();
                    // Potentially log out user or handle this state appropriately
                }
            } else
            {
                Log.e(TAG, "Failed to fetch user document or document does not exist for user: " + user.getUid(), task.getException());
                Toast.makeText(getContext(), "Failed to fetch user type information.", Toast.LENGTH_SHORT).show();
                // Potentially log out user or handle this state
            }
        });
    }

    /**
     * Launches the appropriate main activity ({@link UserMainActivity} or {@link RestaurantMainActivity})
     * based on the provided {@code userType}.
     * After starting the activity, it finishes the current hosting activity.
     *
     * @param userType A string indicating the type of user ("restaurant" or other for regular user).
     */
    private void launchMainActivity(String userType)
    {
        Intent intent;
        if("restaurant".equals(userType))
        {
            intent = new Intent(getContext(), RestaurantMainActivity.class);
        } else
        {
            intent = new Intent(getContext(), UserMainActivity.class); // Default to UserMainActivity
        }
        startActivity(intent);
        if(getActivity() != null)
        {
            requireActivity().finish(); // Finish the current (Authentication) activity
        }
    }

    /**
     * Called when the fragment is visible to the user and actively running.
     * Checks if a user is already logged in ({@link FirebaseAuth#getCurrentUser()} is not null).
     * If a user is logged in, it directly calls {@link #navigateToMainActivity(FirebaseUser)}
     * to proceed to the appropriate main activity, bypassing the login screen.
     */
    @Override
    public void onStart()
    {
        super.onStart();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        Log.d(TAG, "onStart - Current User: " + (currentUser != null ? currentUser.getUid() : "null")); // Changed to d for debug
        if(currentUser != null)
        {
            navigateToMainActivity(currentUser); // Navigate directly if already logged in
        }
    }
}