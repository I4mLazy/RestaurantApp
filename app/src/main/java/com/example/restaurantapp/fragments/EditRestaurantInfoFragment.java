package com.example.restaurantapp.fragments;

import static android.content.ContentValues.TAG; // Note: TAG is imported but not explicitly defined as a static final String in this class. Assuming it's used by Log calls.

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.exifinterface.media.ExifInterface; // For reading image orientation

import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.restaurantapp.R;
import com.example.restaurantapp.models.Restaurant;
import com.example.restaurantapp.viewmodels.RestaurantViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * A {@link Fragment} subclass for editing the information of an existing restaurant.
 * This fragment allows users to modify various details of a restaurant, including its name,
 * description, tags, type, address, hours, capacity, contact information, price level,
 * and operational flags (reservable, offers pickup). It also supports changing the
 * restaurant's logo image by taking a new photo or choosing from the gallery.
 * Changes are validated and then saved to Firestore. Image uploads are handled via Firebase Storage.
 * It uses a {@link RestaurantViewModel} to observe and manage the current restaurant data.
 */
public class EditRestaurantInfoFragment extends Fragment
{

    /**
     * ImageView for displaying and initiating edits to the restaurant's logo.
     */
    private ImageView editRestaurantLogo;
    /**
     * TextInputEditText for editing the restaurant's name.
     */
    private TextInputEditText editRestaurantName;
    /**
     * TextInputEditText for editing the restaurant's description.
     */
    private TextInputEditText editRestaurantDescription;
    /**
     * TextInputEditText for editing the restaurant's tags (comma-separated).
     */
    private TextInputEditText editRestaurantTags;
    /**
     * TextInputEditText for editing the restaurant's address.
     */
    private TextInputEditText editRestaurantAddress;
    /**
     * TextInputEditText for editing the restaurant's business hours.
     */
    private TextInputEditText editRestaurantHours;
    /**
     * TextInputEditText for editing the restaurant's maximum capacity.
     */
    private TextInputEditText editRestaurantMaxCapacity;
    /**
     * TextInputEditText for editing the restaurant's phone number.
     */
    private TextInputEditText editRestaurantPhone;
    /**
     * TextInputEditText for editing the restaurant's email address.
     */
    private TextInputEditText editRestaurantEmail;
    /**
     * AutoCompleteTextView for selecting the restaurant's type from a predefined list.
     */
    private AutoCompleteTextView editRestaurantType;
    /**
     * Slider for adjusting the restaurant's price level (1-4).
     */
    private Slider editRestaurantPriceLevel;
    /**
     * SwitchMaterial for toggling whether the restaurant is reservable.
     */
    private SwitchMaterial editRestaurantReservable;
    /**
     * SwitchMaterial for toggling whether the restaurant offers pickup.
     */
    private SwitchMaterial editRestaurantOffersPickup;
    /**
     * Button to trigger saving the changes made to the restaurant's information.
     */
    private Button saveButton;
    /**
     * Button to cancel the editing process and return to the previous screen.
     */
    private Button cancelButton;

    /**
     * ProgressBar to indicate loading or saving operations.
     */
    private ProgressBar progressBar;

    /**
     * BottomSheetDialog for presenting image source options (camera/gallery).
     */
    private BottomSheetDialog imageBottomSheetDialog;
    /**
     * ViewModel for accessing and managing the current restaurant's data.
     */
    private RestaurantViewModel viewModel;
    /**
     * Uri of the photo taken by the camera or selected from the gallery.
     */
    private Uri photoUri;
    /**
     * Bitmap representation of the image selected or taken, used for display and upload.
     */
    private Bitmap bitmap;
    /**
     * Reference to the current Firebase Storage upload task, if any.
     */
    private UploadTask currentUploadTask;
    /**
     * Flag indicating whether the restaurant's image has been edited by the user.
     */
    private boolean imageEdited = false;
    /**
     * Flag indicating whether an image upload is currently in progress.
     */
    private boolean isUploading = false; // Note: This field is declared but not directly used to control UI flow in the provided code.
    /**
     * Flag indicating if a validation error has occurred during the save process.
     */
    private boolean hasError = false;
    /**
     * The {@link Restaurant} object currently being edited.
     */
    private Restaurant currentRestaurant;
    /**
     * The ID of the restaurant currently being edited.
     */
    private String restaurantID;

    /**
     * Instance of FirebaseFirestore for database operations.
     */
    private FirebaseFirestore db;
    /**
     * Instance of FirebaseAuth for user authentication.
     */
    private FirebaseAuth auth;
    /**
     * The currently authenticated FirebaseUser.
     */
    private FirebaseUser currentUser;
    /**
     * Reference to the root of Firebase Storage.
     */
    private StorageReference storageRef;
    /**
     * Instance of FirebaseStorage.
     */
    private FirebaseStorage storage;


    /**
     * Called to have the fragment instantiate its user interface view.
     * This method inflates the layout, initializes Firebase services (Firestore, Auth, Storage),
     * finds UI views, and sets up the {@link RestaurantViewModel} to observe the current restaurant data.
     * When restaurant data is available, {@link #populateRestaurantViews(Restaurant)} is called.
     * It sets up click listeners for the cancel button (navigates back to {@link RestaurantInfoFragment}),
     * the save button (initiates image upload if needed, then calls {@link #saveRestaurantChanges()}),
     * and the restaurant logo ImageView (calls {@link #editImage()}).
     * It also registers a custom {@link OnBackPressedCallback} to navigate back to
     * {@link RestaurantInfoFragment} when the system back button is pressed.
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
        View view = inflater.inflate(R.layout.fragment_edit_restaurant_info, container, false);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        editRestaurantLogo = view.findViewById(R.id.editRestaurantLogo);

        editRestaurantName = view.findViewById(R.id.editRestaurantName);
        editRestaurantDescription = view.findViewById(R.id.editRestaurantDescription);
        editRestaurantTags = view.findViewById(R.id.editRestaurantTags);
        editRestaurantType = view.findViewById(R.id.editRestaurantType);
        editRestaurantAddress = view.findViewById(R.id.editRestaurantAddress);
        editRestaurantHours = view.findViewById(R.id.editRestaurantHours);
        editRestaurantMaxCapacity = view.findViewById(R.id.editRestaurantMaxCapacity);
        editRestaurantPhone = view.findViewById(R.id.editRestaurantPhone);
        editRestaurantEmail = view.findViewById(R.id.editRestaurantEmail);

        editRestaurantPriceLevel = view.findViewById(R.id.editRestaurantPriceLevel);

        editRestaurantReservable = view.findViewById(R.id.editRestaurantReservable);
        editRestaurantOffersPickup = view.findViewById(R.id.editRestaurantOffersPickup);

        progressBar = view.findViewById(R.id.saveProgressBar);

        saveButton = view.findViewById(R.id.saveButton);
        cancelButton = view.findViewById(R.id.cancelButton);


        viewModel = new ViewModelProvider(requireActivity()).get(RestaurantViewModel.class);

        viewModel.getCurrentRestaurant().observe(getViewLifecycleOwner(), restaurant ->
        {
            if(restaurant != null)
            {
                currentRestaurant = restaurant;
                restaurantID = restaurant.getRestaurantID();
                populateRestaurantViews(restaurant);
            }
        });

        cancelButton.setOnClickListener(v ->
        {
            if(getActivity() != null)
            {
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new RestaurantInfoFragment()) // Assumes fragment_container is the ID in the activity layout
                        .commit();
            }
        });

        saveButton.setOnClickListener(v ->
        {
            if(imageEdited)
            {
                uploadImageToFirebase(bitmap);
            } else
            {
                saveRestaurantChanges();
            }
        });

        editRestaurantLogo.setOnClickListener(v -> editImage());

        // Handle back press in the fragment
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true)
        {
            @Override
            public void handleOnBackPressed()
            {
                if(getActivity() != null)
                {
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, new RestaurantInfoFragment()) // Assumes fragment_container
                            .commit();
                }
                setEnabled(false); // Disable this callback after handling
            }
        });

        return view;
    }

    /**
     * Populates the input fields in the fragment with data from the provided {@link Restaurant} object.
     * Loads the restaurant's image using Glide. Sets text for name, description, tags (joined by comma),
     * address, hours, max capacity, phone, and email, with null-safe checks.
     * Sets up an {@link ArrayAdapter} for the restaurant type AutoCompleteTextView and sets its value.
     * Clamps and sets the price level slider's value.
     * Sets the checked state for reservable and offers pickup switches.
     *
     * @param restaurant The {@link Restaurant} object whose data will be used to populate the views.
     *                   If null, the method returns early.
     */
    private void populateRestaurantViews(Restaurant restaurant)
    {
        if(restaurant == null) return;

        // Load image safely
        String imageUrl = restaurant.getImageURL();
        Log.e("IMAGE", imageUrl != null ? imageUrl : "Image URL is null"); // Log image URL or null status
        Glide.with(requireContext())
                .load(imageUrl)
                .placeholder(R.drawable.image_placeholder)
                .error(R.drawable.image_placeholder)
                .into(editRestaurantLogo);

        // Populate text fields with null-safe checks
        editRestaurantName.setText(restaurant.getName() != null ? restaurant.getName() : "");
        editRestaurantDescription.setText(restaurant.getDescription() != null ? restaurant.getDescription() : "");

        if(restaurant.getTags() != null && !restaurant.getTags().isEmpty())
        {
            editRestaurantTags.setText(TextUtils.join(", ", restaurant.getTags()));
        } else
        {
            editRestaurantTags.setText("");
        }

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireActivity(),
                R.array.restaurant_types, android.R.layout.simple_dropdown_item_1line);

        // Set the adapter to the AutoCompleteTextView
        editRestaurantType.setAdapter(adapter);

        // If you have an existing restaurant type to display
        String restaurantType = restaurant.getType();
        if(restaurantType != null && !restaurantType.isEmpty())
        {
            editRestaurantType.setText(restaurantType, false); // false to prevent filtering
        }


        float priceLevel = restaurant.getPriceLevel();
        priceLevel = Math.min(Math.max(priceLevel, 1.0f), 4.0f);  // Clamp the value between 1 and 4
        editRestaurantPriceLevel.setValue(priceLevel);

        editRestaurantAddress.setText(restaurant.getAddress() != null ? restaurant.getAddress() : "");
        editRestaurantHours.setText(restaurant.getBusinessHours() != null ? restaurant.getBusinessHours() : "");

        editRestaurantMaxCapacity.setText(String.valueOf(restaurant.getMaxCapacity()));

        Map<String, String> contactInfo = restaurant.getContactInfo();

        // Extract phone number without the leading '+' if it exists, for display purposes
        String phone = (contactInfo != null && contactInfo.get("phone") != null && !contactInfo.get("phone").isEmpty() && contactInfo.get("phone").startsWith("+"))
                ? contactInfo.get("phone").substring(1)
                : (contactInfo != null ? contactInfo.get("phone") : null); // Handle cases where '+' might be missing or phone is null
        String email = contactInfo != null ? contactInfo.get("email") : null;

        editRestaurantPhone.setText(phone != null ? phone : "");
        editRestaurantEmail.setText(email != null ? email : "");


        // Populate boolean values (safe even if default false)
        editRestaurantReservable.setChecked(Boolean.TRUE.equals(restaurant.isReservable()));
        editRestaurantOffersPickup.setChecked(Boolean.TRUE.equals(restaurant.isOffersPickup()));
    }

    /**
     * Saves the changes made to the restaurant's information to Firestore.
     * It first makes the progress bar visible. Then, it retrieves and trims input from all
     * editable fields. It performs validation for required fields (name, type, address, hours),
     * price level (1-4), max capacity (>0), email format, and phone number length.
     * If the address has changed, it calls {@link #isValidAddress(String, AddressValidationCallback)}
     * for asynchronous validation. If any validation fails, an error is set on the respective field,
     * {@link #hasError} is set to true, and the method returns.
     * If all validations pass, it constructs a {@code Map} of the restaurant's data,
     * attempts to geocode the address to obtain a {@link GeoPoint}, and includes contact information.
     * Finally, it updates the restaurant's document in the "Restaurants" collection in Firestore.
     * On success, a toast is shown, the progress bar is hidden, and it navigates back to
     * {@link RestaurantInfoFragment}. On failure, an error toast is shown, and the progress bar is hidden.
     */
    private void saveRestaurantChanges()
    {
        progressBar.setVisibility(View.VISIBLE);
        hasError = false; // Reset error flag

        String name = editRestaurantName.getText().toString().trim();
        String description = editRestaurantDescription.getText().toString().trim();
        String tagsRaw = editRestaurantTags.getText().toString().trim();

        List<String> tags = new ArrayList<>();
        if(!tagsRaw.isEmpty())
        {
            for(String tag : tagsRaw.split(","))
            {
                String trimmed = tag.trim();
                if(!trimmed.isEmpty()) tags.add(trimmed);
            }
        }

        String type = editRestaurantType.getText().toString().trim();
        int priceLevel = (int) editRestaurantPriceLevel.getValue();
        String maxCapacityText = Objects.requireNonNull(editRestaurantMaxCapacity.getText()).toString().trim();
        int maxCapacity = maxCapacityText.isEmpty() ? 0 : Integer.parseInt(maxCapacityText);


        String address = editRestaurantAddress.getText().toString().trim();
        String businessHours = editRestaurantHours.getText().toString().trim();

        boolean reservable = editRestaurantReservable.isChecked();
        boolean offersPickup = editRestaurantOffersPickup.isChecked();

        // Contact info - ensure phone number starts with '+' when saving
        String phoneInput = editRestaurantPhone.getText().toString().trim();
        String phone = !phoneInput.isEmpty() ? "+" + phoneInput : "";
        String email = editRestaurantEmail.getText().toString().trim();

        // --- Validations ---
        if(name.isEmpty())
        {
            editRestaurantName.setError("Name is required");
            hasError = true;
        } else
        {
            editRestaurantName.setError(null);
        }

        if(type.isEmpty())
        {
            editRestaurantType.setError("Type is required");
            hasError = true;
        } else
        {
            editRestaurantType.setError(null);
        }

        if(address.isEmpty())
        {
            editRestaurantAddress.setError("Address is required");
            hasError = true;
        } else if(currentRestaurant != null && !currentRestaurant.getAddress().equals(address))
        {
            // Asynchronous validation, might not block save immediately if not handled carefully
            isValidAddress(address, isValid ->
            {
                if(getActivity() != null)
                {
                    getActivity().runOnUiThread(() ->
                    {
                        if(!isValid)
                        {
                            editRestaurantAddress.setError("Address is invalid");
                            // Note: hasError might need to be re-evaluated or save process re-triggered
                            // if this callback is critical for blocking the save.
                            // For now, it sets an error but doesn't stop the current save flow directly.
                        } else
                        {
                            editRestaurantAddress.setError(null);
                        }
                    });
                }
            });
        } else
        {
            editRestaurantAddress.setError(null);
        }

        if(businessHours.isEmpty())
        {
            editRestaurantHours.setError("Restaurant Hours are required");
            hasError = true;
        } else
        {
            editRestaurantHours.setError(null);
        }


        if(priceLevel < 1 || priceLevel > 4)
        {
            Toast.makeText(requireContext(), "Price level must be between 1 and 4", Toast.LENGTH_SHORT).show();
            hasError = true;
        }

        if(maxCapacity <= 0)
        {
            editRestaurantMaxCapacity.setError("Max capacity must be greater than 0");
            hasError = true;
        } else
        {
            editRestaurantMaxCapacity.setError(null);
        }


        if(email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches())
        {
            editRestaurantEmail.setError("Invalid email address");
            hasError = true;
        } else
        {
            editRestaurantEmail.setError(null);
        }

        // Validate phone number (e.g., starts with '+' and has a certain length)
        if(phone.length() < 5) // Basic check, assuming '+' plus at least 4 digits
        {
            editRestaurantPhone.setError("Invalid phone number");
            hasError = true;
        } else
        {
            editRestaurantPhone.setError(null);
        }

        if(hasError)
        {
            progressBar.setVisibility(View.GONE);
            return;
        }


        // Build Firestore update map
        Map<String, Object> restaurantMap = new HashMap<>();
        restaurantMap.put("name", name);
        restaurantMap.put("description", description);
        restaurantMap.put("tags", tags);
        restaurantMap.put("type", type);
        restaurantMap.put("priceLevel", priceLevel);
        restaurantMap.put("address", address);
        restaurantMap.put("businessHours", businessHours);
        restaurantMap.put("maxCapacity", maxCapacity);
        restaurantMap.put("reservable", reservable);
        restaurantMap.put("offersPickup", offersPickup);
        restaurantMap.put("lastUpdated", FieldValue.serverTimestamp());

        // Try to get GeoPoint from address
        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        try
        {
            List<Address> locations = geocoder.getFromLocationName(address, 1);
            if(locations != null && !locations.isEmpty())
            {
                Address loc = locations.get(0);
                GeoPoint location = new GeoPoint(loc.getLatitude(), loc.getLongitude());
                restaurantMap.put("location", location);
            }
        } catch(IOException e)
        {
            Log.e("GEOCODER", "Failed to geocode address: " + address, e);
            // Continue without location if geocoding fails
        }

        // Add contact info to the same document
        Map<String, Object> contactMap = new HashMap<>();
        if(!phone.isEmpty()) contactMap.put("phone", phone); // Save phone with '+'
        if(!email.isEmpty()) contactMap.put("email", email);
        if(!contactMap.isEmpty())
        {
            restaurantMap.put("contactInfo", contactMap);
        } else
        {
            // If both are empty, consider removing the field or setting it to null
            restaurantMap.put("contactInfo", FieldValue.delete()); // Or null, depending on desired behavior
        }


        db.collection("Restaurants").document(restaurantID)
                .update(restaurantMap)
                .addOnSuccessListener(unused ->
                {
                    Toast.makeText(requireContext(), "Restaurant updated", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    if(getActivity() != null)
                    {
                        getActivity().getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, new RestaurantInfoFragment()) // Assumes fragment_container
                                .commit();
                    }
                })
                .addOnFailureListener(e ->
                {
                    Log.e("SAVE_RESTAURANT", "Failed to update restaurant", e);
                    Toast.makeText(requireContext(), "Failed to save changes: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                });
    }

    /**
     * Validates an address asynchronously.
     * First, it attempts to use Android's {@link Geocoder}. If successful and addresses are found,
     * the callback is invoked with {@code true}.
     * If Geocoder fails or returns no results, it falls back to validating with OpenStreetMap's
     * Nominatim API via {@link #validateWithOpenStreetMap(String)}.
     * The entire operation runs on a background thread.
     *
     * @param address  The address string to validate.
     * @param callback The {@link AddressValidationCallback} to be invoked with the validation result.
     */
    private void isValidAddress(String address, EditRestaurantInfoFragment.AddressValidationCallback callback)
    {
        new Thread(() ->
        {
            Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
            boolean geocoderSuccess = false;
            try
            {
                List<Address> addresses = geocoder.getFromLocationName(address, 1);
                if(addresses != null && !addresses.isEmpty())
                {
                    geocoderSuccess = true;
                }
            } catch(IOException e)
            {
                Log.e("GEOCODER_VALIDATION", "Geocoder failed for address: " + address, e);
            }

            if(geocoderSuccess)
            {
                callback.onResult(true); // Valid address according to Geocoder
                return;
            }

            // If Geocoder fails or returns no results, try OpenStreetMap API
            boolean isValidOSM = validateWithOpenStreetMap(address);
            callback.onResult(isValidOSM);
        }).start();
    }

    /**
     * Interface for receiving the result of an address validation.
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
     * Validates an address using the OpenStreetMap Nominatim API.
     * Sends a GET request to the Nominatim search endpoint with the address.
     * If the JSON response contains a "lat" field, the address is considered valid.
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
            connection.setRequestProperty("User-Agent", "Mozilla/5.0"); // Nominatim requires a User-Agent

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder responseBuilder = new StringBuilder();
            String line;
            while((line = reader.readLine()) != null)
            {
                responseBuilder.append(line);
            }
            reader.close();
            String response = responseBuilder.toString();

            // Check if the response (which is a JSON array) is not empty and contains latitude
            return response != null && !response.equals("[]") && response.contains("\"lat\":");
        } catch(Exception e)
        {
            Log.e("OSM_VALIDATION", "Error validating with OpenStreetMap for address: " + address, e);
            return false;
        }
    }

    /**
     * Initiates the process of editing the restaurant's logo image.
     * It first checks for camera and storage permissions using {@link #checkStoragePermission()}.
     * If permissions are not granted, it calls {@link #requestPermissions()}.
     * If permissions are already granted, it calls {@link #showBottomSheetDialog()} to present
     * image source options to the user.
     */
    private void editImage()
    {
        // Check permissions
        boolean cameraPermissionGranted = ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        boolean storagePermissionGranted = checkStoragePermission();

        Log.d(TAG, "editImage - Camera Permission: " + cameraPermissionGranted + ", Storage Permission: " + storagePermissionGranted);

        if(!cameraPermissionGranted || !storagePermissionGranted)
        {
            requestPermissions();
        } else
        {
            showBottomSheetDialog();
        }
    }

    /**
     * Checks if the necessary storage permissions are granted, considering the Android SDK version.
     * For Android 13 (TIRAMISU) and above, checks {@link Manifest.permission#READ_MEDIA_IMAGES}.
     * For Android 10 (Q) to 12, assumes scoped storage handles access, so returns true.
     * For Android 9 (P) and below, checks {@link Manifest.permission#WRITE_EXTERNAL_STORAGE}
     * and {@link Manifest.permission#READ_EXTERNAL_STORAGE}.
     *
     * @return True if required storage permissions are granted, false otherwise.
     */
    private boolean checkStoragePermission()
    {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        {
            return ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        } else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        {
            // For Android 10, 11, 12 (API 29, 30, 31, 32), direct storage permissions for app's own files
            // or media picked via picker are often not needed in the same way.
            // This simplified check assumes access through MediaStore or app-specific directories.
            return true;
        } else
        {
            // For Android 9 (API 28) and below
            return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }

    /**
     * Requests necessary permissions (camera and storage) from the user.
     * It builds a list of permissions to request based on current grant status and Android SDK version.
     * If the list is not empty, it launches the {@link #permissionLauncher}.
     * If all required permissions are already granted, it proceeds to {@link #showBottomSheetDialog()}.
     */
    private void requestPermissions()
    {
        List<String> permissionsToRequest = new ArrayList<>();

        // Always request camera permission if not granted
        if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED)
        {
            permissionsToRequest.add(Manifest.permission.CAMERA);
        }

        // Add storage permissions based on Android version if not granted
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        {
            if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED)
            {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) // Android 9 (P) and below
        {
            if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED)
            {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED)
            {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
        // For Android 10-12 (Q, R, S), specific storage permissions might not be needed for ACTION_PICK.

        // Launch permission request if needed
        if(!permissionsToRequest.isEmpty())
        {
            permissionLauncher.launch(permissionsToRequest.toArray(new String[0]));
        } else
        {
            // If no permissions need to be requested (i.e., all are granted or not applicable for this flow on current SDK)
            showBottomSheetDialog();
        }
    }

    /**
     * ActivityResultLauncher for handling multiple permission requests.
     * After permissions are requested, this launcher's callback is invoked.
     * It checks if camera and appropriate storage permissions (based on SDK version) were granted.
     * If all necessary permissions are granted, it calls {@link #showBottomSheetDialog()}.
     * Otherwise, if the fragment is still added, it shows a toast indicating that permissions are required.
     */
    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result ->
            {
                boolean cameraGranted = Boolean.TRUE.equals(result.get(Manifest.permission.CAMERA));
                boolean storageGranted;

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                {
                    storageGranted = Boolean.TRUE.equals(result.get(Manifest.permission.READ_MEDIA_IMAGES));
                } else if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
                {
                    // For P and below, both read and write are typically requested together or are implied.
                    storageGranted = Boolean.TRUE.equals(result.get(Manifest.permission.WRITE_EXTERNAL_STORAGE))
                            && Boolean.TRUE.equals(result.get(Manifest.permission.READ_EXTERNAL_STORAGE));
                } else
                {
                    // For Android 10-12, if we didn't request specific storage perms, assume true for this flow.
                    storageGranted = true;
                }

                Log.d(TAG, "permissionLauncher - Camera Permission Granted: " + cameraGranted);
                Log.d(TAG, "permissionLauncher - Storage Permission Granted: " + storageGranted);

                if(cameraGranted && storageGranted)
                {
                    showBottomSheetDialog();
                } else
                {
                    if(isAdded()) // Check if fragment is still attached
                    {
                        Toast.makeText(requireContext(), "Permissions are required to change the image.", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    /**
     * Displays a {@link BottomSheetDialog} allowing the user to choose an image source:
     * either take a new photo with the camera or select an existing image from the gallery.
     * It inflates the bottom sheet layout, sets up click listeners for the "Take Photo",
     * "Choose from Gallery", and "Cancel" buttons.
     * Dismisses any existing dialog before showing a new one.
     */
    private void showBottomSheetDialog()
    {
        // Dismiss any existing dialog first to prevent duplicates
        if(imageBottomSheetDialog != null && imageBottomSheetDialog.isShowing())
        {
            imageBottomSheetDialog.dismiss();
        }

        // Create and configure new dialog
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_image, null);

        TextView selectImageTextview = bottomSheetView.findViewById(R.id.SelectImageTextView);
        selectImageTextview.setText("Select Logo Image"); // Customize title

        imageBottomSheetDialog = new BottomSheetDialog(requireContext());
        imageBottomSheetDialog.setContentView(bottomSheetView);

        Button btnTakePhoto = bottomSheetView.findViewById(R.id.btn_take_photo);
        Button btnChooseGallery = bottomSheetView.findViewById(R.id.btn_choose_gallery);
        Button btnCancel = bottomSheetView.findViewById(R.id.btn_cancel);

        btnTakePhoto.setOnClickListener(v ->
        {
            Log.d(TAG, "Taking photo...");
            takePhoto();
            imageBottomSheetDialog.dismiss();
        });

        btnChooseGallery.setOnClickListener(v ->
        {
            Log.d(TAG, "Choosing from gallery...");
            pickFromGallery();
            imageBottomSheetDialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> imageBottomSheetDialog.dismiss());

        imageBottomSheetDialog.setOnDismissListener(dialog -> imageBottomSheetDialog = null); // Clear reference on dismiss
        imageBottomSheetDialog.show();
    }

    /**
     * Initiates taking a photo using the device camera.
     * Creates an {@link Intent} with {@link MediaStore#ACTION_IMAGE_CAPTURE}.
     * If a camera app is available, it creates a temporary image file using {@link #createImageFile()},
     * obtains a content URI for this file via {@link FileProvider}, and passes this URI as
     * {@link MediaStore#EXTRA_OUTPUT} to the camera intent.
     * Finally, it launches the {@link #cameraLauncher} with the prepared intent.
     * Handles potential {@link IllegalArgumentException} if URI creation fails.
     */
    private void takePhoto()
    {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(intent.resolveActivity(requireActivity().getPackageManager()) != null)
        {
            File photoFile = createImageFile(); // Create a file to store the image
            if(photoFile != null)
            {
                try
                {
                    photoUri = FileProvider.getUriForFile(
                            requireContext(),
                            "com.example.restaurantapp.fileprovider", // Authority must match AndroidManifest.xml
                            photoFile
                    );
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                    Log.d(TAG, "Launching camera with URI: " + photoUri);
                    cameraLauncher.launch(intent);
                } catch(IllegalArgumentException e)
                {
                    Log.e(TAG, "Error creating file URI for camera: " + e.getMessage(), e);
                    if(isAdded())
                    {
                        Toast.makeText(requireContext(), "Unable to create file for photo.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    /**
     * Initiates picking an image from the device's gallery.
     * Creates an {@link Intent} with {@link Intent#ACTION_PICK} and {@link MediaStore.Images.Media#EXTERNAL_CONTENT_URI}.
     * Launches the {@link #imagePickerLauncher} with this intent.
     */
    private void pickFromGallery()
    {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        Log.d(TAG, "Launching gallery picker...");
        imagePickerLauncher.launch(intent);
    }

    /**
     * ActivityResultLauncher for handling the result from the camera intent.
     * If the result code is {@link Activity#RESULT_OK} and the fragment is still attached,
     * it re-checks camera and storage permissions.
     * If permissions are granted and {@link #photoUri} is not null, it attempts to load the
     * captured image as a {@link Bitmap}, fixes its orientation using {@link #fixImageOrientation(Uri, Bitmap)},
     * displays it in {@link #editRestaurantLogo}, and sets {@link #imageEdited} to true.
     * Handles potential {@link IOException} during image processing.
     */
    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result ->
            {
                if(result.getResultCode() == Activity.RESULT_OK)
                {
                    Log.d(TAG, "Camera result received, URI: " + photoUri);

                    if(!isAdded() || getActivity() == null || getActivity().isFinishing())
                    {
                        Log.w(TAG, "Camera result received but fragment not attached or finishing.");
                        return;  // Fragment is no longer attached
                    }

                    // Re-check permissions as they might have been revoked in settings
                    boolean hasCameraPermission = ContextCompat.checkSelfPermission(
                            requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
                    boolean hasStoragePermission = checkStoragePermission(); // Checks appropriate storage perm

                    if(hasCameraPermission && hasStoragePermission)
                    {
                        if(photoUri != null)
                        {
                            try
                            {
                                // Load and display the image
                                bitmap = MediaStore.Images.Media.getBitmap(
                                        requireActivity().getContentResolver(), photoUri);
                                bitmap = fixImageOrientation(photoUri, bitmap); // Correct orientation

                                editRestaurantLogo.setImageBitmap(bitmap);
                                imageEdited = true; // Mark image as edited
                            } catch(IOException e)
                            {
                                Log.e(TAG, "Error processing camera image", e);
                                Toast.makeText(requireContext(),
                                        "Error processing image from camera.", Toast.LENGTH_SHORT).show();
                            }
                        } else
                        {
                            Log.e(TAG, "photoUri is null after camera result.");
                        }
                    } else
                    {
                        Log.d(TAG, "Camera or storage permissions denied after camera result.");
                        Toast.makeText(requireContext(),
                                "Camera and/or storage permissions are required.",
                                Toast.LENGTH_SHORT).show();
                    }
                } else
                {
                    Log.d(TAG, "Camera capture cancelled or failed. Result code: " + result.getResultCode());
                }
            }
    );

    /**
     * ActivityResultLauncher for handling the result from the gallery image picker intent.
     * If the result code is {@link Activity#RESULT_OK}, data is not null, and the fragment is still attached,
     * it checks for storage permissions.
     * If permissions are granted and an image URI is obtained from the result data, it attempts to
     * load the image as a {@link Bitmap}, fixes its orientation using {@link #fixImageOrientation(Uri, Bitmap)},
     * displays it in {@link #editRestaurantLogo}, and sets {@link #imageEdited} to true.
     * Handles potential {@link IOException} during image loading. If permissions are denied,
     * it shows a toast and calls {@link #requestPermissions()}.
     */
    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result ->
            {
                if(result.getResultCode() == Activity.RESULT_OK && result.getData() != null)
                {
                    Log.d(TAG, "Gallery result received.");

                    if(!isAdded() || getActivity() == null || getActivity().isFinishing())
                    {
                        Log.w(TAG, "Gallery result received but fragment not attached or finishing.");
                        return;  // Fragment is no longer attached
                    }

                    boolean hasStoragePermission = checkStoragePermission();

                    if(hasStoragePermission)
                    {
                        Uri imageUri = result.getData().getData();
                        if(imageUri != null)
                        {
                            try
                            {
                                // Display the image
                                bitmap = MediaStore.Images.Media.getBitmap(
                                        requireActivity().getContentResolver(), imageUri);
                                bitmap = fixImageOrientation(imageUri, bitmap); // Correct orientation

                                editRestaurantLogo.setImageBitmap(bitmap);
                                imageEdited = true; // Mark image as edited
                            } catch(IOException e)
                            {
                                Log.e(TAG, "Error loading image from gallery", e);
                                Toast.makeText(requireContext(),
                                        "Failed to load image from gallery.", Toast.LENGTH_SHORT).show();
                            }
                        } else
                        {
                            Log.e(TAG, "Image URI from gallery is null.");
                        }
                    } else
                    {
                        Log.d(TAG, "Storage permissions denied for gallery access.");
                        Toast.makeText(requireContext(),
                                "Storage permissions are required to select an image.", Toast.LENGTH_SHORT).show();
                        requestPermissions(); // Re-request permissions
                    }
                } else
                {
                    Log.d(TAG, "Gallery picking cancelled or failed. Result code: " + result.getResultCode());
                }
            }
    );

    /**
     * Creates a temporary image file in the app's external files directory.
     * The file is named with a "profile_pic_" prefix and ".jpg" suffix.
     *
     * @return The created {@link File} object, or null if file creation fails.
     */
    private File createImageFile()
    {
        File storageDir = requireContext().getExternalFilesDir(null); // App-specific directory
        try
        {
            File imageFile = File.createTempFile(
                    "profile_pic_", /* Prefix */
                    ".jpg",         /* Suffix */
                    storageDir      /* Directory */
            );
            Log.d(TAG, "Image file created at: " + imageFile.getAbsolutePath());
            return imageFile;
        } catch(IOException e)
        {
            Log.e(TAG, "Error creating image file", e);
            return null;
        }
    }

    /**
     * Corrects the orientation of a given {@link Bitmap} based on EXIF data from its {@link Uri}.
     * Reads the orientation tag from the image's EXIF metadata. If the orientation indicates
     * rotation (90, 180, or 270 degrees), it applies the corresponding rotation to the bitmap
     * using a {@link Matrix}.
     *
     * @param imageUri The URI of the image, used to read EXIF data.
     * @param bitmap   The original bitmap to be potentially rotated.
     * @return The orientation-corrected bitmap, or the original bitmap if an error occurs or no rotation is needed.
     */
    private Bitmap fixImageOrientation(Uri imageUri, Bitmap bitmap)
    {
        try
        {
            // Read EXIF data to determine orientation
            androidx.exifinterface.media.ExifInterface exif = null;
            try(InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri))
            {
                if(inputStream != null)
                {
                    exif = new ExifInterface(inputStream);
                }
            }

            int orientation = androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL;
            if(exif != null)
            {
                orientation = exif.getAttributeInt(
                        androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                        androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
                );
            }

            // Rotate bitmap based on orientation
            Matrix matrix = new Matrix();
            switch(orientation)
            {
                case androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.postRotate(90);
                    break;
                case androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.postRotate(180);
                    break;
                case androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.postRotate(270);
                    break;
                // No default needed as matrix is identity if orientation is normal or undefined
            }

            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        } catch(Exception e) // Catch broad exception for robustness
        {
            Log.e(TAG, "Error fixing image orientation for URI: " + imageUri, e);
            return bitmap; // Return original if rotation fails
        }
    }

    /**
     * Uploads the provided {@link Bitmap} image to Firebase Storage.
     * If the bitmap is null or the fragment is not attached, it returns early.
     * Shows a toast indicating "Uploading image...". Checks if a user is logged in.
     * Creates a unique filename under "restaurant_images/{restaurantID}/" path.
     * Compresses the bitmap to JPEG format (80% quality) and converts it to a byte array.
     * Initiates the upload using {@code imageRef.putBytes(imageData)}.
     * Handles success by getting the download URL, then calling {@link #deleteOldImage(String)}
     * followed by {@link #updateImageWithImageUrl(String)}.
     * Handles failure by showing an error toast. Also logs upload progress.
     *
     * @param bitmap The {@link Bitmap} image to upload.
     */
    private void uploadImageToFirebase(Bitmap bitmap)
    {
        if(bitmap == null)
        {
            Log.e(TAG, "Cannot upload null bitmap.");
            // Optionally, inform the user or proceed to save other changes without image.
            saveRestaurantChanges(); // Proceed to save other changes if bitmap is null
            return;
        }

        if(!isAdded() || getActivity() == null || getActivity().isFinishing())
        {
            Log.w(TAG, "Upload attempt while fragment not attached or finishing.");
            return;  // Fragment is no longer attached
        }

        // Show a loading indicator
        Toast.makeText(requireContext(), "Uploading image...", Toast.LENGTH_SHORT).show();
        progressBar.setVisibility(View.VISIBLE); // Show progress bar during upload

        // Get the current user ID
        if(currentUser == null)
        {
            Log.e(TAG, "No user is currently logged in for image upload.");
            Toast.makeText(requireContext(),
                    "You must be logged in to upload an image.", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            return;
        }

        // Create a unique filename for the image
        String filename = "restaurant_images/" + restaurantID + "/" + UUID.randomUUID().toString() + ".jpg";
        StorageReference imageRef = storageRef.child(filename);

        // Compress the image before uploading
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos); // Compress to 80% quality
        byte[] imageData = baos.toByteArray();

        // Start the upload
        isUploading = true; // This flag is set but not used to prevent other actions in this code.
        currentUploadTask = imageRef.putBytes(imageData);
        currentUploadTask.addOnSuccessListener(taskSnapshot ->
        {
            isUploading = false;
            currentUploadTask = null;

            if(isAdded() && getActivity() != null && !getActivity().isFinishing())
            {
                imageRef.getDownloadUrl().addOnSuccessListener(downloadUri ->
                {
                    if(isAdded() && getActivity() != null && !getActivity().isFinishing())
                    {
                        Log.d(TAG, "Image upload successful, URL: " + downloadUri.toString());
                        // After getting new URL, delete old image then update Firestore with new URL
                        deleteOldImage(downloadUri.toString());
                        // Note: updateImageWithImageUrl will then call saveRestaurantChanges
                    }
                }).addOnFailureListener(e ->
                {
                    if(isAdded() && getActivity() != null && !getActivity().isFinishing())
                    {
                        Log.e(TAG, "Failed to get download URL after upload.", e);
                        Toast.makeText(requireContext(),
                                "Failed to get image URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                        // Consider if saveRestaurantChanges should be called here with old image URL or error handling
                    }
                });
            }
        }).addOnFailureListener(e ->
        {
            isUploading = false;
            currentUploadTask = null;
            if(isAdded() && getActivity() != null && !getActivity().isFinishing())
            {
                Log.e(TAG, "Image upload failed.", e);
                Toast.makeText(requireContext(),
                        "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                // Consider if saveRestaurantChanges should be called here with old image URL or error handling
            }
        }).addOnProgressListener(taskSnapshot ->
        {
            // Calculate and show upload progress if needed
            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
            Log.d(TAG, "Upload progress: " + progress + "%");
            // Update a progress bar UI element here if desired
        });
    }

    /**
     * Deletes the old restaurant image from Firebase Storage if one exists and is different from the new image URL.
     * It first fetches the current restaurant document to get the {@code imageURL}.
     * If an old URL exists and differs from {@code newImageURL}, it parses the old URL to extract
     * the storage path, creates a {@link StorageReference} to the old image, and attempts to delete it.
     * On successful deletion of the old image, it calls {@link #updateImageWithImageUrl(String)} with the new URL.
     * Logs errors if fetching the document or deleting the old image fails.
     *
     * @param newImageURL The URL of the newly uploaded image.
     */
    private void deleteOldImage(String newImageURL)
    {
        // currentUser is already checked in uploadImageToFirebase, but good practice to ensure it's still valid
        if(currentUser != null && restaurantID != null)
        {
            DocumentReference docRef = db.collection("Restaurants").document(restaurantID);

            docRef.get().addOnSuccessListener(documentSnapshot ->
            {
                if(documentSnapshot.exists())
                {
                    String oldImageURL = documentSnapshot.getString("imageURL");

                    if(oldImageURL != null && !oldImageURL.isEmpty() && !newImageURL.equals(oldImageURL))
                    {
                        try
                        {
                            // Extract path from Firebase Storage URL. Example: gs://<bucket>/path/to/image.jpg
                            // Or from HTTPS URL: https://firebasestorage.googleapis.com/v0/b/<bucket>/o/path%2Fto%2Fimage.jpg?alt=media...
                            // This parsing logic assumes HTTPS URL structure.
                            if(oldImageURL.contains("/o/"))
                            {
                                String urlPath = oldImageURL.split("/o/")[1];
                                if(urlPath.contains("?"))
                                {
                                    urlPath = urlPath.split("\\?")[0]; // Remove query parameters
                                }
                                String decodedPath = java.net.URLDecoder.decode(urlPath, "UTF-8");

                                StorageReference oldImageRef = storage.getReference().child(decodedPath);
                                oldImageRef.delete().addOnSuccessListener(aVoid ->
                                {
                                    Log.d(TAG, "Old image deleted successfully: " + oldImageURL);
                                    updateImageWithImageUrl(newImageURL); // Proceed to update Firestore with new URL
                                }).addOnFailureListener(e ->
                                {
                                    Log.e(TAG, "Error deleting old image: " + oldImageURL, e);
                                    // Even if old image deletion fails, proceed to update with new URL
                                    updateImageWithImageUrl(newImageURL);
                                });
                            } else
                            {
                                Log.w(TAG, "Old image URL format not recognized for deletion: " + oldImageURL);
                                updateImageWithImageUrl(newImageURL); // Proceed without deleting if format is unknown
                            }
                        } catch(Exception e)
                        {
                            Log.e(TAG, "Error parsing or deleting old image URL: " + oldImageURL, e);
                            updateImageWithImageUrl(newImageURL); // Proceed on error
                        }
                    } else
                    {
                        // No old image, or old image is same as new, or old image URL is empty
                        updateImageWithImageUrl(newImageURL);
                    }
                } else
                {
                    // Document doesn't exist, which shouldn't happen if we are editing
                    Log.w(TAG, "Restaurant document not found when trying to delete old image.");
                    updateImageWithImageUrl(newImageURL);
                }
            }).addOnFailureListener(e ->
            {
                Log.e(TAG, "Error fetching restaurant document to delete old image.", e);
                updateImageWithImageUrl(newImageURL); // Proceed on error
            });
        } else
        {
            Log.w(TAG, "User or restaurantID is null in deleteOldImage.");
            updateImageWithImageUrl(newImageURL); // Proceed if user/ID is null, though this indicates an issue
        }
    }

    /**
     * Updates the {@code imageURL} field in the restaurant's Firestore document with the provided URL.
     * After successfully updating the Firestore document, it calls {@link #saveRestaurantChanges()}
     * to save any other (non-image) modifications made to the restaurant's details.
     * Shows a toast on success or failure of the Firestore update.
     *
     * @param imageURL The new image URL to save to Firestore.
     */
    private void updateImageWithImageUrl(String imageURL)
    {
        if(restaurantID == null)
        {
            Log.e(TAG, "restaurantID is null in updateImageWithImageUrl. Cannot update Firestore.");
            progressBar.setVisibility(View.GONE);
            return;
        }
        DocumentReference docRef = db.collection("Restaurants").document(restaurantID);
        Log.d(TAG, "Updating Firestore with new imageURL: " + imageURL + " for restaurantID: " + restaurantID);

        Map<String, Object> updates = new HashMap<>();
        updates.put("imageURL", imageURL);

        docRef.update(updates)
                .addOnSuccessListener(aVoid ->
                {
                    if(isAdded() && getActivity() != null && !getActivity().isFinishing())
                    {
                        Toast.makeText(requireContext(),
                                "Image URL updated in Firestore.", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Firestore imageURL updated successfully.");
                        // Now that image is handled, save other changes
                        imageEdited = false; // Reset flag as image part is done
                        saveRestaurantChanges();
                    }
                })
                .addOnFailureListener(e ->
                {
                    if(isAdded() && getActivity() != null && !getActivity().isFinishing())
                    {
                        Log.e(TAG, "Error updating Firestore with image URL.", e);
                        Toast.makeText(requireContext(),
                                "Failed to update image URL in database: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                        // Decide if we should still try to save other changes or stop.
                        // Current flow: if image URL update fails, other changes are not saved via this path.
                    }
                });
    }
}