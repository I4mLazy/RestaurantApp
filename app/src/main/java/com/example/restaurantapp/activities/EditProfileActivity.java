package com.example.restaurantapp.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.restaurantapp.R;
import com.google.android.material.textfield.TextInputLayout;

public class EditProfileActivity extends AppCompatActivity
{
    private Toolbar toolbar;
    private TextInputLayout textInputLayout;
    private EditText editText;
    private Button saveButton;
    private String fieldType;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        textInputLayout = findViewById(R.id.textInputLayout);
        editText = findViewById(R.id.editText);
        saveButton = findViewById(R.id.saveButton);
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
                    break;
                case "Phone":
                    editText.setInputType(InputType.TYPE_CLASS_PHONE);
                    editText.setHint("Enter your phone number");
                    enforcePlusSignInPhoneNumber();
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

        saveButton.setOnClickListener(v -> saveChanges());
    }

    private void enforcePlusSignInPhoneNumber()
    {
        textInputLayout.setPrefixText("+");
        editText.setSelection(editText.getText().length()); // Move cursor to the end
        editText.setKeyListener(DigitsKeyListener.getInstance("0123456789")); // Restrict to digits
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
