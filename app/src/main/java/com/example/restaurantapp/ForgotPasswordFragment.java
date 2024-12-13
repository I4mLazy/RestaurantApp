package com.example.restaurantapp;

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

import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordFragment extends Fragment
{

    private EditText editTextEmail;
    private Button buttonResetPassword, buttonBackToLogin;
    private ProgressBar progressBar;
    private FirebaseAuth firebaseAuth;

    public ForgotPasswordFragment()
    {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_forgot_password, container, false);

        editTextEmail = view.findViewById(R.id.editTextEmail);
        buttonResetPassword = view.findViewById(R.id.buttonResetPassword);
        buttonBackToLogin = view.findViewById(R.id.buttonBackToLogin);
        progressBar = view.findViewById(R.id.progressBar);

        firebaseAuth = FirebaseAuth.getInstance();

        buttonResetPassword.setOnClickListener(v ->
        {
            String email = editTextEmail.getText().toString().trim();
            if (TextUtils.isEmpty(email))
            {
                Toast.makeText(getContext(), "Please enter your email address.", Toast.LENGTH_SHORT).show();
                return;
            }

            progressBar.setVisibility(View.VISIBLE);
            firebaseAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task ->
                    {
                        progressBar.setVisibility(View.GONE);
                        if (task.isSuccessful())
                        {
                            Toast.makeText(getContext(), "Password reset email sent successfully.", Toast.LENGTH_SHORT).show();
                        }
                        else
                        {
                            Toast.makeText(getContext(), "Failed to send reset email. Try again.", Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        buttonBackToLogin.setOnClickListener(v ->
        {
            LoginFragment loginFragment = new LoginFragment();
            FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.authentication_container, loginFragment);
            transaction.commit();
        });

        return view;
    }
}
