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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.restaurantapp.R;
import com.example.restaurantapp.activities.RestaurantMainActivity;
import com.example.restaurantapp.activities.UserMainActivity;
import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.auth.api.identity.SignInCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginFragment extends Fragment
{

    private static final String TAG = "LoginFragment";

    private EditText editTextEmail, editTextPassword;
    private Button buttonLogin, buttonGoogleSignIn;
    private TextView textViewSignUp, textViewForgotPassword;

    private FirebaseAuth firebaseAuth;
    private SignInClient oneTapClient;
    private BeginSignInRequest signInRequest;

    private final ActivityResultLauncher<IntentSenderRequest> signInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), result ->
            {
                if(result.getResultCode() == requireActivity().RESULT_OK && result.getData() != null)
                {
                    try
                    {
                        SignInCredential credential = oneTapClient.getSignInCredentialFromIntent(result.getData());
                        String idToken = credential.getGoogleIdToken();
                        if(idToken != null)
                        {
                            firebaseAuthWithGoogle(idToken);
                        }
                    } catch(Exception e)
                    {
                        Log.e(TAG, "Google Sign-In failed", e);
                    }
                }
            });

    public LoginFragment()
    {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        editTextEmail = view.findViewById(R.id.editTextEmail);
        editTextPassword = view.findViewById(R.id.editTextPassword);
        buttonLogin = view.findViewById(R.id.buttonLogin);
        buttonGoogleSignIn = view.findViewById(R.id.buttonGoogleSignIn);
        textViewSignUp = view.findViewById(R.id.textViewSignUp);
        textViewForgotPassword = view.findViewById(R.id.textViewForgotPassword);

        firebaseAuth = FirebaseAuth.getInstance();
        oneTapClient = Identity.getSignInClient(requireContext());

        signInRequest = BeginSignInRequest.builder()
                .setGoogleIdTokenRequestOptions(
                        BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                                .setSupported(true)
                                .setServerClientId(getString(R.string.default_web_client_id)) // Your Web Client ID
                                .setFilterByAuthorizedAccounts(false)
                                .build())
                .setAutoSelectEnabled(false)
                .build();

        buttonLogin.setOnClickListener(v -> loginWithEmailPassword());
        buttonGoogleSignIn.setOnClickListener(v -> loginWithGoogle());

        // Handle forgot password
        textViewForgotPassword.setOnClickListener(v ->
        {
            ForgotPasswordFragment forgotPasswordFragment = new ForgotPasswordFragment();
            FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.authentication_container, forgotPasswordFragment);
            transaction.commit();
        });

        textViewSignUp.setOnClickListener(v ->
        {
            UserSignUpFragment signUpFragment = new UserSignUpFragment();
            FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.authentication_container, signUpFragment);
            transaction.commit();
        });

        return view;
    }

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
                        navigateToMainActivity(user);
                    } else
                    {
                        Toast.makeText(getContext(), "Authentication failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loginWithGoogle()
    {
        oneTapClient.beginSignIn(signInRequest)
                .addOnSuccessListener(requireActivity(), result ->
                {
                    try
                    {
                        // Start the intent sender for Google sign-in
                        signInLauncher.launch(new IntentSenderRequest.Builder(result.getPendingIntent().getIntentSender()).build());
                    } catch(Exception e)
                    {
                        Log.e(TAG, "Google Sign-In failed", e);
                    }
                })
                .addOnFailureListener(requireActivity(), e ->
                {
                    Toast.makeText(getContext(), "Google Sign-In failed", Toast.LENGTH_SHORT).show();
                });
    }

    private void firebaseAuthWithGoogle(String idToken)
    {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(requireActivity(), task ->
                {
                    if(task.isSuccessful())
                    {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        navigateToMainActivity(user);
                    } else
                    {
                        Toast.makeText(getContext(), "Google Sign-In failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateToMainActivity(FirebaseUser user)
    {
        if(user == null) return;

        SharedPreferences prefs = requireActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
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
            if(task.isSuccessful() && task.getResult().exists())
            {
                String fetchedUserType = task.getResult().getString("userType");

                if(fetchedUserType != null)
                {
                    // Store the fetched userType in SharedPreferences
                    prefs.edit().putString("userType", fetchedUserType).apply();
                    launchMainActivity(fetchedUserType);
                } else
                {
                    Toast.makeText(getContext(), "Failed to determine user type", Toast.LENGTH_SHORT).show();
                }
            } else
            {
                Toast.makeText(getContext(), "Failed to fetch user type", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Launches the correct main activity instantly
    private void launchMainActivity(String userType)
    {
        Intent intent;
        if("restaurant".equals(userType))
        {
            intent = new Intent(getContext(), RestaurantMainActivity.class);
        } else
        {
            intent = new Intent(getContext(), UserMainActivity.class);
        }
        startActivity(intent);
        requireActivity().finish();
    }

    @Override
    public void onStart()
    {
        super.onStart();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        Log.e(TAG, "Current User: " + currentUser);
        if(currentUser != null)
        {
            navigateToMainActivity(currentUser); // Navigate directly if logged in
        }
    }
}