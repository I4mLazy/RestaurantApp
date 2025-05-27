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

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.restaurantapp.R;
import com.google.firebase.auth.FirebaseAuth;

/**
 * A {@link Fragment} subclass that provides a user interface for users to request a password reset.
 * Users can enter their email address, and upon submission, a password reset email is sent
 * via Firebase Authentication. It also provides an option to navigate back to the login screen.
 */
public class ForgotPasswordFragment extends Fragment
{

    /**
     * EditText field for the user to input their email address.
     */
    private EditText editTextEmail;
    /**
     * Button to initiate the password reset process.
     */
    private Button buttonResetPassword;
    /**
     * Button to navigate back to the {@link LoginFragment}.
     */
    private Button buttonBackToLogin;
    /**
     * ProgressBar to indicate the progress of sending the reset email.
     */
    private ProgressBar progressBar;
    /**
     * Instance of FirebaseAuth for handling password reset functionality.
     */
    private FirebaseAuth firebaseAuth;

    /**
     * Required empty public constructor for Fragment instantiation.
     */
    public ForgotPasswordFragment()
    {
        // Required empty public constructor
    }

    /**
     * Called to have the fragment instantiate its user interface view.
     * This method inflates the layout for the fragment, initializes UI components
     * (EditText for email, Buttons for reset and back to login, ProgressBar),
     * and obtains an instance of {@link FirebaseAuth}.
     * It sets up click listeners for the "Reset Password" button, which validates the email input,
     * shows a progress bar, and calls {@link FirebaseAuth#sendPasswordResetEmail(String)}.
     * Toasts are displayed to indicate success or failure of the email sending process.
     * The "Back to Login" button listener navigates the user back to the {@link LoginFragment}.
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
        View view = inflater.inflate(R.layout.fragment_forgot_password, container, false);

        editTextEmail = view.findViewById(R.id.editTextEmail);
        buttonResetPassword = view.findViewById(R.id.buttonResetPassword);
        buttonBackToLogin = view.findViewById(R.id.buttonBackToLogin);
        progressBar = view.findViewById(R.id.progressBar);

        firebaseAuth = FirebaseAuth.getInstance();

        buttonResetPassword.setOnClickListener(v ->
        {
            String email = editTextEmail.getText().toString().trim();
            if(TextUtils.isEmpty(email))
            {
                Toast.makeText(getContext(), "Please enter your email address.", Toast.LENGTH_SHORT).show();
                return; // Stop if email is empty
            }

            progressBar.setVisibility(View.VISIBLE); // Show progress
            firebaseAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task ->
                    {
                        progressBar.setVisibility(View.GONE); // Hide progress
                        if(task.isSuccessful())
                        {
                            Toast.makeText(getContext(), "Password reset email sent successfully.", Toast.LENGTH_SHORT).show();
                        } else
                        {
                            Toast.makeText(getContext(), "Failed to send reset email. Try again.", Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        buttonBackToLogin.setOnClickListener(v ->
        {
            LoginFragment loginFragment = new LoginFragment();
            // Perform fragment transaction to replace current fragment with LoginFragment
            FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.authentication_container, loginFragment); // Assumes R.id.authentication_container is the ID of the container in the activity
            transaction.commit();
        });

        return view;
    }
}