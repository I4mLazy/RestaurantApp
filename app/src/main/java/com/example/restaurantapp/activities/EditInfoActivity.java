package com.example.restaurantapp.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import com.example.restaurantapp.R;
import com.example.restaurantapp.utils.SettingsUtils;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;

public class EditInfoActivity extends AppCompatActivity
{
    private Toolbar toolbar;
    private TextInputLayout textInputLayout;
    private EditText editText;
    private Button saveButton;
    private EditText passwordEditText; // Password field
    private TextInputLayout passwordInputLayout; // Password TextInputLayout
    private String fieldType;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        SettingsUtils.loadUserSettings(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_profile);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        textInputLayout = findViewById(R.id.textInputLayout);
        editText = findViewById(R.id.editText);
        saveButton = findViewById(R.id.saveButton);
        passwordInputLayout = findViewById(R.id.passwordInputLayout); // Password input layout
        passwordEditText = findViewById(R.id.passwordEditText); // Password edit text
        saveButton.setEnabled(false); // Initially disable save button

        fieldType = getIntent().getStringExtra("fieldType");
        toolbar.setTitle(fieldType);
        String currentValue = getIntent().getStringExtra("currentValue");

        if(currentValue != null)
        {
            editText.setText(currentValue);
        }

        // Set input type based on field type
        if(fieldType != null)
        {
            switch(fieldType)
            {
                case "Email":
                    editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                    editText.setHint("Enter your email");
                    // Show password field when changing email
                    passwordInputLayout.setVisibility(View.VISIBLE);
                    break;
                case "Phone":
                    editText.setInputType(InputType.TYPE_CLASS_PHONE);
                    editText.setHint("Enter your phone number");
                    textInputLayout.setPrefixText("+");
                    editText.setKeyListener(DigitsKeyListener.getInstance("0123456789"));
                    break;
                case "Name":
                    editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
                    editText.setHint("Enter your name");
                    break;
                default:
                    editText.setInputType(InputType.TYPE_CLASS_TEXT);
                    editText.setHint("Enter value");
                    break;
            }
        }

        editText.addTextChangedListener(new TextWatcher()
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
        });

        saveButton.setOnClickListener(v ->
        {
            if(fieldType.equals("Email"))
            {
                // Validate current password and email before updating
                validateEmailAndPassword();
            } else
            {
                saveChanges();
            }
        });
    }

    private void validateInput()
    {
        String updatedValue = editText.getText().toString().trim();
        boolean isValid = false;

        if(fieldType != null)
        {
            switch(fieldType)
            {
                case "Email":
                    isValid = Patterns.EMAIL_ADDRESS.matcher(updatedValue).matches();
                    break;
                case "Phone":
                    isValid = updatedValue.matches("\\d{9,15}$"); // Assuming 9 to 15 digits after '+'
                    break;
                case "Name":
                    isValid = updatedValue.matches("^[a-zA-Z\\s]+$") && updatedValue.length() >= 2;
                    break;
                default:
                    isValid = !updatedValue.isEmpty();
                    break;
            }
        }

        saveButton.setEnabled(isValid);
        saveButton.setAlpha(isValid ? 1.0f : 0.5f); // Visually gray out button when disabled
    }

    private void validateEmailAndPassword()
    {
        String newEmail = editText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if(newEmail.isEmpty() || password.isEmpty())
        {
            Toast.makeText(EditInfoActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Re-authenticate the user with their current password
        FirebaseUser user = mAuth.getCurrentUser();
        if(user != null)
        {
            mAuth.signInWithEmailAndPassword(Objects.requireNonNull(user.getEmail()), password).addOnCompleteListener(this, task ->
            {
                if(task.isSuccessful())
                {
                    // Password is correct, now update email
                    user.verifyBeforeUpdateEmail(newEmail).addOnCompleteListener(this, updateTask ->
                    {
                        if(updateTask.isSuccessful())
                        {
                            Toast.makeText(EditInfoActivity.this, "Email updated successfully", Toast.LENGTH_SHORT).show();
                            saveChanges();
                        } else
                        {
                            Toast.makeText(EditInfoActivity.this, "Failed to update email", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else
                {
                    Toast.makeText(EditInfoActivity.this, "Incorrect password", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void saveChanges()
    {
        String updatedValue = editText.getText().toString().trim();

        Intent resultIntent = new Intent();
        resultIntent.putExtra("fieldType", fieldType);
        resultIntent.putExtra("updatedValue", updatedValue);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }

    @Override
    public boolean onSupportNavigateUp()
    {
        finish();
        return true;
    }
}
