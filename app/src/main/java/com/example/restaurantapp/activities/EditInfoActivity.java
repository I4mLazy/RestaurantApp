package com.example.restaurantapp.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.restaurantapp.R;
import com.example.restaurantapp.utils.SettingsUtils;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;

public class EditInfoActivity extends AppCompatActivity
{
    private Toolbar toolbar;
    private TextInputLayout universalInputLayout, newPasswordInputLayout, confirmPasswordInputLayout;
    private EditText universalEditText, newPasswordEditText, confirmPasswordEditText;
    private Button saveButton;
    private String fieldType;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        SettingsUtils.loadUserSettings(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_info);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        universalInputLayout = findViewById(R.id.universalInputLayout);
        newPasswordInputLayout = findViewById(R.id.newPasswordInputLayout);
        confirmPasswordInputLayout = findViewById(R.id.confirmPasswordInputLayout);

        universalEditText = findViewById(R.id.universalEditText);
        newPasswordEditText = findViewById(R.id.newPasswordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);

        saveButton = findViewById(R.id.saveButton);
        saveButton.setEnabled(false); // Initially disable save button

        fieldType = getIntent().getStringExtra("fieldType");
        toolbar.setTitle(fieldType);
        String currentValue = getIntent().getStringExtra("currentValue");

        // Set input type based on field type
        if(fieldType != null)
        {
            switch(fieldType)
            {
                case "Email":
                    if(currentValue != null)
                    {
                        universalEditText.setText(currentValue);
                    }
                    universalEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                    universalInputLayout.setHint("Enter your email");
                    confirmPasswordInputLayout.setVisibility(View.VISIBLE);
                    confirmPasswordInputLayout.setHint("Enter Password");
                    confirmPasswordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    break;
                case "Phone":
                    if(currentValue != null)
                    {
                        universalEditText.setText(currentValue.substring(1));
                    }
                    universalEditText.setInputType(InputType.TYPE_CLASS_PHONE);
                    universalInputLayout.setHint("Enter your phone number");
                    universalInputLayout.setPrefixText("+");
                    universalEditText.setKeyListener(DigitsKeyListener.getInstance("0123456789"));
                    break;
                case "Name":
                    if(currentValue != null)
                    {
                        universalEditText.setText(currentValue);
                    }
                    universalEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
                    universalInputLayout.setHint("Enter your name");
                    break;
                case "Password":
                    universalEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    universalInputLayout.setHint("Enter your password");
                    newPasswordInputLayout.setVisibility(View.VISIBLE);
                    newPasswordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    confirmPasswordInputLayout.setVisibility(View.VISIBLE);
                    confirmPasswordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    break;
                default:
                    if(currentValue != null)
                    {
                        universalEditText.setText(currentValue);
                    }
                    universalEditText.setInputType(InputType.TYPE_CLASS_TEXT);
                    universalInputLayout.setHint("Enter value");
                    break;
            }
        }

        TextWatcher watcher = new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                validateInput();
            }

            @Override
            public void afterTextChanged(Editable s)
            {
            }
        };

        // Attach the same watcher to all 3 fields
        universalEditText.addTextChangedListener(watcher);
        newPasswordEditText.addTextChangedListener(watcher);
        confirmPasswordEditText.addTextChangedListener(watcher);


        saveButton.setOnClickListener(v -> saveChanges());
    }

    private void validateInput()
    {
        String updatedValue = universalEditText.getText().toString().trim();
        boolean isValid = false;

        if(fieldType != null)
        {
            switch(fieldType)
            {
                case "Email":
                    String password = confirmPasswordEditText.getText().toString();
                    isValid = Patterns.EMAIL_ADDRESS.matcher(updatedValue).matches() && !password.isEmpty();
                    break;
                case "Phone":
                    isValid = updatedValue.matches("\\d{9,15}$"); // Assuming 9 to 15 digits after '+'
                    break;
                case "Name":
                    isValid = updatedValue.matches("^[a-zA-Z\\s]+$") && updatedValue.length() >= 2;
                    break;
                case "Password":
                    String newPassword = newPasswordEditText.getText().toString();
                    String confirmPassword = confirmPasswordEditText.getText().toString();
                    isValid = newPassword.length() >= 6 && newPassword.equals(confirmPassword) && !newPassword.equals(updatedValue) && !updatedValue.isEmpty();
                    break;
                default:
                    isValid = !updatedValue.isEmpty();
                    break;
            }
        }

        saveButton.setEnabled(isValid);
        saveButton.setAlpha(isValid ? 1.0f : 0.5f); // Visually gray out button when disabled
    }

    private void saveChanges()
    {
        saveButton.setEnabled(false);
        if("Password".equals(fieldType))
        {
            String currentPassword = universalEditText.getText().toString().trim();
            String newPassword = newPasswordEditText.getText().toString().trim();

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if(user == null || user.getEmail() == null)
            {
                universalInputLayout.setError("Unexpected error. Please try again.");
                saveButton.setEnabled(true);
                return;
            }

            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);
            user.reauthenticate(credential).addOnCompleteListener(authTask ->
            {
                if(authTask.isSuccessful())
                {
                    // Now try to update the password
                    user.updatePassword(newPassword).addOnCompleteListener(updateTask ->
                    {
                        if(updateTask.isSuccessful())
                        {
                            // Success
                            Intent resultIntent = new Intent();
                            resultIntent.putExtra("fieldType", fieldType);
                            resultIntent.putExtra("updatedValue", newPassword);
                            setResult(Activity.RESULT_OK, resultIntent);
                            finish();
                        } else
                        {
                            Exception e = updateTask.getException();
                            if(e instanceof FirebaseAuthWeakPasswordException)
                            {
                                newPasswordInputLayout.setError("Password too weak. Try a stronger one.");
                            } else
                            {
                                newPasswordInputLayout.setError("Failed to update password. Try again later.");
                            }
                            saveButton.setEnabled(true);
                        }
                    });
                } else
                {
                    universalInputLayout.setError("Current password is incorrect.");
                    saveButton.setEnabled(true);
                }
            });

        } else if("Email".equals(fieldType))
        {
            FirebaseUser user = mAuth.getCurrentUser();
            String newEmail = universalEditText.getText().toString().trim();
            String currentEmail = user != null ? user.getEmail() : null;
            String password = confirmPasswordEditText.getText().toString().trim();

            if(user != null && currentEmail != null && !password.isEmpty())
            {
                AuthCredential cred = EmailAuthProvider.getCredential(currentEmail, password);
                user.reauthenticate(cred)
                        .addOnSuccessListener(unused ->
                        {
                            user.updateEmail(newEmail)
                                    .addOnSuccessListener(u ->
                                    {
                                        user.sendEmailVerification()
                                                .addOnSuccessListener(v ->
                                                {
                                                    // Return newEmail; fragment will save pendingEmail
                                                    Intent result = new Intent();
                                                    result.putExtra("fieldType", fieldType);
                                                    result.putExtra("updatedValue", newEmail);
                                                    setResult(Activity.RESULT_OK, result);
                                                    finish();
                                                })
                                                .addOnFailureListener(e ->
                                                {
                                                    saveButton.setEnabled(true);
                                                    Toast.makeText(this,
                                                            "Failed to send verification email: " + e.getMessage(),
                                                            Toast.LENGTH_SHORT).show();
                                                });
                                    })
                                    .addOnFailureListener(e ->
                                    {
                                        saveButton.setEnabled(true);
                                        Log.e("EditInfoActivity", "Failed to update email: " + e.getMessage());
                                        Toast.makeText(this, "Failed to update email: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        })
                        .addOnFailureListener(e ->
                        {
                            saveButton.setEnabled(true);
                            confirmPasswordInputLayout.setError("Password incorrect.");
                            Toast.makeText(this,
                                    "Re-authentication failed: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        });
            } else
            {
                saveButton.setEnabled(true);
                Toast.makeText(this,
                        "Please enter your current password.",
                        Toast.LENGTH_SHORT).show();
            }
        } else
        {
            // Normal fields (email, phone, name)
            String updatedValue = universalEditText.getText().toString().trim();
            Intent resultIntent = new Intent();
            resultIntent.putExtra("fieldType", fieldType);
            resultIntent.putExtra("updatedValue", updatedValue);
            setResult(Activity.RESULT_OK, resultIntent);
            finish();
        }
    }


    @Override
    public boolean onSupportNavigateUp()
    {
        finish();
        return true;
    }
}
