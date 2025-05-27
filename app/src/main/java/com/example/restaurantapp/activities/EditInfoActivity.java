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

/**
 * Activity for editing user profile information such as email, phone number, name, or password.
 * This activity receives the type of field to edit and its current value via an {@link Intent}.
 * It provides a dynamic UI based on the field type and handles validation and saving of changes,
 * including re-authentication for sensitive operations like changing email or password.
 */
public class EditInfoActivity extends AppCompatActivity
{
    /** The toolbar for this activity. */
    private Toolbar toolbar;
    /** TextInputLayout for the primary input field, used for most field types. */
    private TextInputLayout universalInputLayout;
    /** TextInputLayout for the new password input, visible only when editing password. */
    private TextInputLayout newPasswordInputLayout;
    /** TextInputLayout for confirming the password, visible when editing email or password. */
    private TextInputLayout confirmPasswordInputLayout;
    /** EditText for the primary input field. */
    private EditText universalEditText;
    /** EditText for the new password, visible only when editing password. */
    private EditText newPasswordEditText;
    /** EditText for confirming the password, visible when editing email or password. */
    private EditText confirmPasswordEditText;
    /** Button to trigger saving the changes. */
    private Button saveButton;
    /** String indicating the type of field being edited (e.g., "Email", "Phone", "Name", "Password"). */
    private String fieldType;
    /** Instance of FirebaseAuth for handling user authentication and updates. */
    private FirebaseAuth mAuth;

    /**
     * Called when the activity is first created.
     * Initializes the UI components, Firebase Authentication, and sets up the input fields
     * based on the {@code fieldType} passed in the intent. It also configures a
     * {@link TextWatcher} to validate input dynamically and enable/disable the save button.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *                           previously being shut down then this Bundle contains the data it most
     *                           recently supplied in {@link #onSaveInstanceState}.
     *                           <b><i>Note: Otherwise it is null.</i></b>
     */
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
                // Not used in this implementation
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                validateInput();
            }

            @Override
            public void afterTextChanged(Editable s)
            {
                // Not used in this implementation
            }
        };

        // Attach the same watcher to all 3 fields
        universalEditText.addTextChangedListener(watcher);
        newPasswordEditText.addTextChangedListener(watcher);
        confirmPasswordEditText.addTextChangedListener(watcher);


        saveButton.setOnClickListener(v -> saveChanges());
    }

    /**
     * Validates the input fields based on the current {@code fieldType}.
     * Enables or disables the save button and adjusts its opacity accordingly.
     * Validation rules are specific to each field type (e.g., email format, phone number length,
     * password strength and confirmation).
     */
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

    /**
     * Handles the process of saving the changes made by the user.
     * The behavior varies based on the {@code fieldType}:
     * <ul>
     *     <li><b>Password:</b> Re-authenticates the user with their current password, then updates to the new password.
     *         Sets an error on {@code universalInputLayout} or {@code newPasswordInputLayout} on failure.
     *     </li>
     *     <li><b>Email:</b> Re-authenticates the user, updates the email in Firebase, and sends a verification email to the new address.
     *         Sets an error on {@code confirmPasswordInputLayout} or shows a Toast on failure.
     *     </li>
     *     <li><b>Other fields (Name, Phone):</b> Directly prepares the result intent with the updated value.</li>
     * </ul>
     * On successful update, it sets the activity result to {@link Activity#RESULT_OK} with the
     * updated field type and value, and then finishes the activity.
     * Displays appropriate error messages or toasts for failures.
     */
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


    /**
     * Handles the action when the up navigation button in the toolbar is pressed.
     * Finishes the current activity, returning the user to the previous screen.
     *
     * @return {@code true} to indicate that the navigation event has been handled.
     */
    @Override
    public boolean onSupportNavigateUp()
    {
        finish();
        return true;
    }
}