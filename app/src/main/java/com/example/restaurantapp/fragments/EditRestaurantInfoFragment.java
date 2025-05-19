package com.example.restaurantapp.fragments;

import static android.content.ContentValues.TAG;

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
import androidx.exifinterface.media.ExifInterface;

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

public class EditRestaurantInfoFragment extends Fragment
{

    private ImageView editRestaurantLogo;
    private TextInputEditText editRestaurantName, editRestaurantDescription, editRestaurantTags,
            editRestaurantAddress, editRestaurantHours, editRestaurantMaxCapacity, editRestaurantPhone, editRestaurantEmail;
    private AutoCompleteTextView editRestaurantType;
    private Slider editRestaurantPriceLevel;
    private SwitchMaterial editRestaurantReservable, editRestaurantOffersPickup;
    private Button saveButton, cancelButton;

    private ProgressBar progressBar;

    private BottomSheetDialog imageBottomSheetDialog;
    private RestaurantViewModel viewModel;
    private Uri photoUri;
    private Bitmap bitmap;
    private UploadTask currentUploadTask;
    private boolean imageEdited = false;
    private boolean isUploading = false;
    private boolean hasError = false;
    private Restaurant currentRestaurant;
    private String restaurantID;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private StorageReference storageRef;
    private FirebaseStorage storage;


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
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new RestaurantInfoFragment())
                    .commit();
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
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new RestaurantInfoFragment())
                        .commit();

                setEnabled(false);
            }
        });

        return view;
    }

    private void populateRestaurantViews(Restaurant restaurant)
    {
        if(restaurant == null) return;

        // Load image safely
        String imageUrl = restaurant.getImageURL();
        Log.e("IMAGE", imageUrl);
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
            editRestaurantType.setText(restaurantType, false);
        }


        float priceLevel = restaurant.getPriceLevel();
        priceLevel = Math.min(Math.max(priceLevel, 1.0f), 4.0f);  // Clamp the value
        editRestaurantPriceLevel.setValue(priceLevel);

        editRestaurantAddress.setText(restaurant.getAddress() != null ? restaurant.getAddress() : "");
        editRestaurantHours.setText(restaurant.getBusinessHours() != null ? restaurant.getBusinessHours() : "");

        editRestaurantMaxCapacity.setText(String.valueOf(restaurant.getMaxCapacity()));

        Map<String, String> contactInfo = restaurant.getContactInfo();

        String phone = (contactInfo != null && contactInfo.get("phone") != null && !contactInfo.get("phone").isEmpty())
                ? contactInfo.get("phone").substring(1)
                : null;
        String email = contactInfo != null ? contactInfo.get("email") : null;

        editRestaurantPhone.setText(phone != null ? phone : "");
        editRestaurantEmail.setText(email != null ? email : "");


        // Populate boolean values (safe even if default false)
        editRestaurantReservable.setChecked(Boolean.TRUE.equals(restaurant.isReservable()));
        editRestaurantOffersPickup.setChecked(Boolean.TRUE.equals(restaurant.isOffersPickup()));
    }

    private void saveRestaurantChanges()
    {
        progressBar.setVisibility(View.VISIBLE);

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

        // Contact info
        String phone = "+" + editRestaurantPhone.getText().toString().trim();
        String email = editRestaurantEmail.getText().toString().trim();

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
        } else if(!currentRestaurant.getAddress().equals(address))
        {
            isValidAddress(address, isValid -> getActivity().runOnUiThread(() ->
            {
                if(!isValid)
                {
                    editRestaurantAddress.setError("Address isinvalid");
                    hasError = true;
                }
            }));
            editRestaurantAddress.setError(null);
        }
        if(businessHours.isEmpty())
        {
            editRestaurantHours.setError("Restaurant Hours are required");
            hasError = true;
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
        }

        if(email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches())
        {
            editRestaurantEmail.setError("Invalid email address");
            hasError = true;
        } else
        {
            editRestaurantEmail.setError(null);
        }

        if(phone.length() < 5)
        {
            editRestaurantPhone.setError("Invalid phone number");
            hasError = true;
        } else
        {
            editRestaurantPhone.setError(null);
        }

        if(hasError) return;


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
            Log.e("GEOCODER", "Failed to geocode address", e);
        }

        // Add contact info to the same document
        if(!phone.isEmpty() || !email.isEmpty())
        {
            Map<String, Object> contactMap = new HashMap<>();
            if(!phone.isEmpty()) contactMap.put("phone", phone);
            if(!email.isEmpty()) contactMap.put("email", email);
            restaurantMap.put("contactInfo", contactMap);
        }

        db.collection("Restaurants").document(restaurantID)
                .update(restaurantMap)
                .addOnSuccessListener(unused ->
                {
                    Toast.makeText(requireContext(), "Restaurant updated", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, new RestaurantInfoFragment())
                            .commit();
                })
                .addOnFailureListener(e ->
                {
                    Log.e("SAVE_RESTAURANT", "Failed to update restaurant", e);
                    Toast.makeText(requireContext(), "Failed to save changes", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                });
    }

    private void isValidAddress(String address, EditRestaurantInfoFragment.AddressValidationCallback callback)
    {
        new Thread(() ->
        {
            Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
            try
            {
                List<Address> addresses = geocoder.getFromLocationName(address, 1);
                if(addresses != null && !addresses.isEmpty())
                {
                    callback.onResult(true); //Valid address
                    return;
                }
            } catch(IOException e)
            {
                e.printStackTrace();
            }

            //If Geocoder fails, try OpenStreetMap API
            boolean isValid = validateWithOpenStreetMap(address);
            callback.onResult(isValid);
        }).start();
    }

    private interface AddressValidationCallback
    {
        void onResult(boolean isValid);
    }

    private boolean validateWithOpenStreetMap(String address)
    {
        try
        {
            String urlString = "https://nominatim.openstreetmap.org/search?q=" + URLEncoder.encode(address, "UTF-8") + "&format=json&limit=1";
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String response = reader.readLine();
            reader.close();

            return response != null && response.contains("\"lat\":");
        } catch(Exception e)
        {
            e.printStackTrace();
            return false;
        }
    }

    private void editImage()
    {
        // Check permissions
        boolean cameraPermissionGranted = ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        boolean storagePermissionGranted = checkStoragePermission();

        Log.d(TAG, "Camera Permission: " + cameraPermissionGranted + ", Storage Permission: " + storagePermissionGranted);

        if(!cameraPermissionGranted || !storagePermissionGranted)
        {
            requestPermissions();
        } else
        {
            showBottomSheetDialog();
        }
    }

    private boolean checkStoragePermission()
    {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        {
            return ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        } else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        {
            // Scoped storage doesn't require explicit permission
            return true;
        } else
        {
            // For Android 9 and below
            return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestPermissions()
    {
        List<String> permissionsToRequest = new ArrayList<>();

        // Always request camera permission
        if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED)
        {
            permissionsToRequest.add(Manifest.permission.CAMERA);
        }

        // Add storage permissions based on Android version
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        {
            if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED)
            {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
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

        // Launch permission request if needed
        if(!permissionsToRequest.isEmpty())
        {
            permissionLauncher.launch(permissionsToRequest.toArray(new String[0]));
        } else
        {
            // If no permissions need to be requested, show bottom sheet
            showBottomSheetDialog();
        }
    }

    // Permission request launcher
    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result ->
            {
                // Update permission checks based on Android version
                boolean cameraGranted = Boolean.TRUE.equals(result.get(Manifest.permission.CAMERA));
                boolean storageGranted = false;

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                {
                    storageGranted = Boolean.TRUE.equals(result.get(Manifest.permission.READ_MEDIA_IMAGES));
                } else if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
                {
                    storageGranted = Boolean.TRUE.equals(result.get(Manifest.permission.WRITE_EXTERNAL_STORAGE))
                            && Boolean.TRUE.equals(result.get(Manifest.permission.READ_EXTERNAL_STORAGE));
                } else
                {
                    // For Android 10-12, storage permissions are not strictly required
                    storageGranted = true;
                }

                Log.d(TAG, "Camera Permission Granted: " + cameraGranted);
                Log.d(TAG, "Storage Permission Granted: " + storageGranted);

                if(cameraGranted && storageGranted)
                {
                    showBottomSheetDialog();
                } else
                {
                    if(isAdded())
                    {
                        Toast.makeText(requireContext(), "Permissions are required", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    private void showBottomSheetDialog()
    {
        // Dismiss any existing dialog first
        if(imageBottomSheetDialog != null && imageBottomSheetDialog.isShowing())
        {
            imageBottomSheetDialog.dismiss();
        }

        // Create and configure new dialog
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_image, null);

        TextView selectImageTextview = bottomSheetView.findViewById(R.id.SelectImageTextView);
        selectImageTextview.setText("Select Logo Image");

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

        imageBottomSheetDialog.setOnDismissListener(dialog -> imageBottomSheetDialog = null);
        imageBottomSheetDialog.show();
    }

    private void takePhoto()
    {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(intent.resolveActivity(requireActivity().getPackageManager()) != null)
        {
            File photoFile = createImageFile();
            if(photoFile != null)
            {
                try
                {
                    photoUri = FileProvider.getUriForFile(
                            requireContext(),
                            "com.example.restaurantapp.fileprovider",
                            photoFile
                    );
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                    Log.d(TAG, "Launching camera with URI: " + photoUri);
                    cameraLauncher.launch(intent);
                } catch(IllegalArgumentException e)
                {
                    Log.e(TAG, "Error creating file URI", e);
                    if(isAdded())
                    {
                        Toast.makeText(requireContext(), "Unable to create file for photo", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    private void pickFromGallery()
    {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        Log.d(TAG, "Launching gallery picker...");
        imagePickerLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result ->
            {
                if(result.getResultCode() == Activity.RESULT_OK)
                {
                    Log.d(TAG, "Camera result received, URI: " + photoUri);

                    if(!isAdded() || getActivity() == null || getActivity().isFinishing())
                    {
                        return;  // Fragment is no longer attached
                    }

                    // Check permissions again
                    boolean hasCameraPermission = ContextCompat.checkSelfPermission(
                            requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
                    boolean hasStoragePermission = checkStoragePermission();

                    if(hasCameraPermission && hasStoragePermission)
                    {
                        if(photoUri != null)
                        {
                            try
                            {
                                // Load and display the image
                                bitmap = MediaStore.Images.Media.getBitmap(
                                        requireActivity().getContentResolver(), photoUri);
                                bitmap = fixImageOrientation(photoUri, bitmap);

                                editRestaurantLogo.setImageBitmap(bitmap);
                                imageEdited = true;
                            } catch(IOException e)
                            {
                                Log.e(TAG, "Error processing camera image", e);
                                Toast.makeText(requireContext(),
                                        "Error processing image", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } else
                    {
                        Log.d(TAG, "Camera or storage permissions denied.");
                        Toast.makeText(requireContext(),
                                "Camera and storage permissions are required to take a photo",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    // Handle gallery result
    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result ->
            {
                if(result.getResultCode() == Activity.RESULT_OK && result.getData() != null)
                {
                    Log.d(TAG, "Gallery result received.");

                    if(!isAdded() || getActivity() == null || getActivity().isFinishing())
                    {
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
                                bitmap = fixImageOrientation(imageUri, bitmap);

                                editRestaurantLogo.setImageBitmap(bitmap);
                                imageEdited = true;
                            } catch(IOException e)
                            {
                                Log.e(TAG, "Error loading image from gallery", e);
                                Toast.makeText(requireContext(),
                                        "Failed to load image", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } else
                    {
                        Log.d(TAG, "Storage permissions denied.");
                        Toast.makeText(requireContext(),
                                "Storage permissions are required", Toast.LENGTH_SHORT).show();
                        requestPermissions();
                    }
                }
            }
    );

    private File createImageFile()
    {
        File storageDir = requireContext().getExternalFilesDir(null);
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
                        androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION, androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL);
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
            }

            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        } catch(Exception e)
        {
            Log.e(TAG, "Error fixing image orientation", e);
            return bitmap; // Return original if rotation fails
        }
    }

    private void uploadImageToFirebase(Bitmap bitmap)
    {
        if(bitmap == null)
        {
            Log.e(TAG, "Cannot upload null bit map");
            return;
        }

        if(!isAdded() || getActivity() == null || getActivity().isFinishing())
        {
            return;  // Fragment is no longer attached
        }

        // Show a loading indicator
        Toast.makeText(requireContext(), "Uploading image...", Toast.LENGTH_SHORT).show();

        // Get the current user ID
        if(currentUser == null)
        {
            Log.e(TAG, "No user is currently logged in");
            Toast.makeText(requireContext(),
                    "You must be logged in to upload an image", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a unique filename for the image
        String filename = "restaurant_images/" + restaurantID + "/" + UUID.randomUUID().toString() + ".jpg";


        StorageReference imageRef = storageRef.child(filename);

        // Compress the image before uploading
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        byte[] imageData = baos.toByteArray();

        // Start the upload
        isUploading = true;
        currentUploadTask = imageRef.putBytes(imageData);
        currentUploadTask.addOnSuccessListener(taskSnapshot ->
        {
            isUploading = false;
            currentUploadTask = null;

            // Only proceed if fragment is still attached
            if(isAdded() && getActivity() != null && !getActivity().isFinishing())
            {
                // Image uploaded successfully, now get the download URL
                imageRef.getDownloadUrl().addOnSuccessListener(downloadUri ->
                {
                    if(isAdded() && getActivity() != null && !getActivity().isFinishing())
                    {
                        // Got the download URL, now update the user's Firestore document
                        Log.d(TAG, "Called updateImageWithImageUrl with URL: " + downloadUri.toString());
                        deleteOldImage(downloadUri.toString());
                        Log.d(TAG, "Upload successful, URL: " + downloadUri.toString());
                    }
                }).addOnFailureListener(e ->
                {
                    if(isAdded() && getActivity() != null && !getActivity().isFinishing())
                    {
                        Log.e(TAG, "Failed to get download URL", e);
                        Toast.makeText(requireContext(),
                                "Failed to get image URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).addOnFailureListener(e ->
        {
            isUploading = false;
            currentUploadTask = null;

            if(isAdded() && getActivity() != null && !getActivity().isFinishing())
            {
                Log.e(TAG, "Image upload failed", e);
                Toast.makeText(requireContext(),
                        "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }).addOnProgressListener(taskSnapshot ->
        {
            // Calculate and show upload progress if needed
            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
            Log.d(TAG, "Upload progress: " + progress + "%");
        });
    }

    private void deleteOldImage(String newImageURL)
    {
        currentUser = auth.getCurrentUser();
        if(currentUser != null)
        {
            DocumentReference docRef = db.collection("Restaurants").document(restaurantID);

            // Get the current profile image URL
            docRef.get().addOnSuccessListener(documentSnapshot ->
            {
                if(documentSnapshot.exists())
                {
                    String oldImageURL = documentSnapshot.getString("imageURL");

                    // Only proceed if there's an old image URL and it's different from the new one
                    if(oldImageURL != null && !oldImageURL.isEmpty() && !newImageURL.equals(oldImageURL))
                    {
                        try
                        {
                            // Get the path after "/o/" and before "?"
                            String urlPath = oldImageURL.split("/o/")[1];
                            if(urlPath.contains("?"))
                            {
                                urlPath = urlPath.split("\\?")[0];
                            }

                            // Decode the URL-encoded path
                            String decodedPath = java.net.URLDecoder.decode(urlPath, "UTF-8");

                            // Create a reference to the old file and delete it
                            StorageReference oldImageRef = storage.getReference().child(decodedPath);
                            oldImageRef.delete().addOnSuccessListener(aVoid ->
                            {
                                Log.d(TAG, "Old image deleted successfully");
                                updateImageWithImageUrl(newImageURL);
                            }).addOnFailureListener(e ->
                            {
                                Log.e(TAG, "Error deleting old image", e);
                            });
                        } catch(Exception e)
                        {
                            Log.e(TAG, "Error parsing old image URL: " + oldImageURL, e);
                        }
                    }
                }
            }).addOnFailureListener(e ->
            {
                Log.e(TAG, "Error fetching user document to delete old image", e);
            });
        }
    }

    private void updateImageWithImageUrl(String imageURL)
    {
        DocumentReference docRef = db.collection("Restaurants").document(restaurantID);
        Log.d(TAG, "newImageUrl: " + imageURL);

        // Update the imageURL field
        Map<String, Object> updates = new HashMap<>();
        updates.put("imageURL", imageURL);

        Log.d(TAG, "Attempting to update imageURL with value: " + imageURL);

        docRef.update(updates)
                .addOnSuccessListener(aVoid ->
                {
                    if(isAdded() && getActivity() != null && !getActivity().isFinishing())
                    {
                        Toast.makeText(requireContext(),
                                "Image updated successfully", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Image updated successfully");
                        saveRestaurantChanges();
                    }
                })
                .addOnFailureListener(e ->
                {
                    if(isAdded() && getActivity() != null && !getActivity().isFinishing())
                    {
                        Log.e(TAG, "Error updating image with image URL", e);
                        Toast.makeText(requireContext(),
                                "Failed to update image: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
