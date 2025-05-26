package com.example.restaurantapp.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.bumptech.glide.Glide;
import com.example.restaurantapp.R;
import com.example.restaurantapp.activities.AuthenticationActivity;
import com.example.restaurantapp.activities.EditInfoActivity;
import com.example.restaurantapp.utils.SettingsUtils;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class ProfileFragment extends Fragment
{
    private ImageButton editProfilePictureImageButton;
    private TextView profileName, profileEmail, profilePhone, editProfilePictureTextView;
    private Switch darkModeSwitch, notificationsSwitch;
    private ActivityResultLauncher<Intent> editInfoLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private BottomSheetDialog bottomSheetDialog;
    private UploadTask currentUploadTask;
    private boolean isUploading = false;
    private Uri photoUri;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private DocumentReference userSettingsRef;
    private DocumentReference userRef;
    private static final String TAG = "ProfileFragment";

    public ProfileFragment()
    {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        profileName = view.findViewById(R.id.profileName);
        profileEmail = view.findViewById(R.id.profileEmail);
        profilePhone = view.findViewById(R.id.profilePhone);

        editProfilePictureImageButton = view.findViewById(R.id.editProfilePictureImageButton);
        editProfilePictureTextView = view.findViewById(R.id.editProfilePictureTextView);

        darkModeSwitch = view.findViewById(R.id.darkModeSwitch);
        notificationsSwitch = view.findViewById(R.id.notificationsSwitch);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        userRef = db.collection("Users").document(Objects.requireNonNull(auth.getCurrentUser()).getUid());

        // Get reference to user's settings document
        userSettingsRef = userRef.collection("Settings").document("preferences");

        // Load user profile data
        loadUserProfile();

        // Set click listeners for profile fields
        view.findViewById(R.id.editProfileNameContainer).setOnClickListener(v -> SettingsUtils.launchEditActivity(requireContext(), editInfoLauncher, "Name", profileName.getText().toString()));
        view.findViewById(R.id.editProfileEmailContainer).setOnClickListener(v -> SettingsUtils.launchEditActivity(requireContext(), editInfoLauncher, "Email", profileEmail.getText().toString()));
        view.findViewById(R.id.editProfilePhoneContainer).setOnClickListener(v -> SettingsUtils.launchEditActivity(requireContext(), editInfoLauncher, "Phone", profilePhone.getText().toString()));
        view.findViewById(R.id.changePasswordButton).setOnClickListener(v -> SettingsUtils.launchEditActivity(requireContext(), editInfoLauncher, "Password", null));

        // Set click listeners for profile picture
        editProfilePictureImageButton.setOnClickListener(v -> selectProfilePicture());
        editProfilePictureTextView.setOnClickListener(v -> selectProfilePicture());

        // Logout button
        view.findViewById(R.id.logoutButton).setOnClickListener(v -> SettingsUtils.handleLogout(requireActivity(), auth));

        // Register the ActivityResultLauncher for profile edits
        editInfoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> SettingsUtils.handleEditInfoResult(requireContext(), result, userRef, auth, profileName, profilePhone));

        // Initialize permission launcher for Android 13+
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted ->
                {
                    if(isGranted)
                    {
                        // Permission granted, update UI and Firestore
                        notificationsSwitch.setChecked(true);
                        setNotificationPreference(true);
                        Toast.makeText(requireContext(), "Notifications enabled", Toast.LENGTH_SHORT).show();
                    } else
                    {
                        // Permission denied, update UI to reflect this
                        notificationsSwitch.setChecked(false);
                        setNotificationPreference(false);
                        Toast.makeText(requireContext(), "Notification permission denied", Toast.LENGTH_SHORT).show();
                    }
                });

        loadUserSettings();

        SettingsUtils.setupDarkModeSwitch(this, darkModeSwitch, userSettingsRef);

        notificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
        {
            // Only process if this change was user-initiated (not from loadUserSettings)
            if(buttonView.isPressed())
            {
                if(isChecked)
                {
                    // Check if we need to request permission for Android 13+
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    {
                        if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) !=
                                android.content.pm.PackageManager.PERMISSION_GRANTED)
                        {

                            // Request permission instead of enabling notifications
                            requestNotificationPermission();

                            // Don't update UI yet - wait for permission result
                            buttonView.setChecked(false);
                            return;
                        }
                    }
                }

                // Update notification preference in Firestore
                setNotificationPreference(isChecked);

                // Show confirmation toast
                Toast.makeText(
                        requireContext(),
                        isChecked ? "Notifications enabled" : "Notifications disabled",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        return view;
    }

    @Override
    public void onResume()
    {
        super.onResume();

        FirebaseUser user = auth.getCurrentUser();
        if(user != null)
        {
            user.reload().addOnCompleteListener(task ->
            {
                if(task.isSuccessful())
                {
                    Log.d("ProfileFragment", "User reloaded in onResume");
                    SettingsUtils.syncPendingEmailIfNeeded(userRef, auth, profileEmail);
                } else
                {
                    Log.e("ProfileFragment", "User reload failed", task.getException());
                }
            });
        }
    }


    @Override
    public void onDestroyView()
    {
        super.onDestroyView();

        // Dismiss any showing dialog to prevent leaks
        if(bottomSheetDialog != null && bottomSheetDialog.isShowing())
        {
            bottomSheetDialog.dismiss();
        }
        bottomSheetDialog = null;

        // Cancel any ongoing uploads
        if(currentUploadTask != null && isUploading)
        {
            currentUploadTask.cancel();
            currentUploadTask = null;
        }
        isUploading = false;
    }

    // Load user settings from preferences
    private void loadUserSettings()
    {
        SharedPreferences prefs = requireContext().getSharedPreferences("FeedMe", Context.MODE_PRIVATE);
        boolean hasLocalSettings = prefs.contains("dark_mode") && prefs.contains("notifications");

        if(hasLocalSettings)
        {
            // Load from SharedPreferences
            boolean darkMode = prefs.getBoolean("dark_mode", false);
            boolean notifications = prefs.getBoolean("notifications", true); // default true

            darkModeSwitch.setChecked(darkMode);
            AppCompatDelegate.setDefaultNightMode(darkMode ?
                    AppCompatDelegate.MODE_NIGHT_YES :
                    AppCompatDelegate.MODE_NIGHT_NO);

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            {
                boolean hasPermission = ContextCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.POST_NOTIFICATIONS) ==
                        PackageManager.PERMISSION_GRANTED;
                notificationsSwitch.setChecked(notifications && hasPermission);
            } else
            {
                notificationsSwitch.setChecked(notifications);
            }

        } else
        {
            // Fallback to Firestore
            fetchFromFirestoreAndStoreLocally(prefs);
        }
    }

    //In case load fails, load from firestore and store locally
    private void fetchFromFirestoreAndStoreLocally(SharedPreferences prefs)
    {
        userSettingsRef.get()
                .addOnSuccessListener(documentSnapshot ->
                {
                    if(documentSnapshot.exists())
                    {
                        // Retrieve settings from document
                        boolean darkMode = documentSnapshot.getBoolean("dark_mode") != null && Boolean.TRUE.equals(documentSnapshot.getBoolean("dark_mode"));
                        boolean notifications = documentSnapshot.getBoolean("notifications") != null && Boolean.TRUE.equals(documentSnapshot.getBoolean("notifications"));

                        prefs.edit()
                                .putBoolean("dark_mode", darkMode)
                                .putBoolean("notifications", notifications)
                                .apply();

                        // Set switches without triggering listeners
                            darkModeSwitch.setChecked(darkMode);
                            // Apply dark mode setting
                            AppCompatDelegate.setDefaultNightMode(darkMode ?
                                    AppCompatDelegate.MODE_NIGHT_YES :
                                    AppCompatDelegate.MODE_NIGHT_NO);

                            // For Android 13+, check if we have permission before setting checked state
                            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                            {
                                boolean hasPermission = ContextCompat.checkSelfPermission(requireContext(),
                                        Manifest.permission.POST_NOTIFICATIONS) ==
                                        android.content.pm.PackageManager.PERMISSION_GRANTED;

                                // Only allow notifications to be on if we have permission
                                notificationsSwitch.setChecked(notifications && hasPermission);
                            } else
                            {
                                notificationsSwitch.setChecked(notifications);
                            }
                    } else
                    {
                        // Document doesn't exist, create it with default values
                        boolean defaultNotifications;

                        // For Android 13+, check permission status for default value
                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                        {
                            defaultNotifications = ContextCompat.checkSelfPermission(requireContext(),
                                    Manifest.permission.POST_NOTIFICATIONS) ==
                                    android.content.pm.PackageManager.PERMISSION_GRANTED;
                        } else
                        {
                            defaultNotifications = true;
                        }

                        Map<String, Object> defaultSettings = new HashMap<>();
                        defaultSettings.put("dark_mode", false);
                        defaultSettings.put("notifications", defaultNotifications);

                        userSettingsRef.set(defaultSettings)
                                .addOnSuccessListener(aVoid ->
                                {
                                    prefs.edit()
                                            .putBoolean("dark_mode", false)
                                            .putBoolean("notifications", defaultNotifications)
                                            .apply();
                                    // Set default values on switches
                                    darkModeSwitch.setChecked(false);
                                    notificationsSwitch.setChecked(defaultNotifications);
                                })
                                .addOnFailureListener(e ->
                                {
                                    Toast.makeText(requireContext(),
                                            "Failed to create settings: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e ->
                {
                    Toast.makeText(requireContext(),
                            "Failed to load settings: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    // Function to request notification permission
    private void requestNotificationPermission()
    {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        } else
        {
            // Permissions not required before Android 13
            setNotificationPreference(true);
            notificationsSwitch.setChecked(true);
        }
    }

    private void setNotificationPreference(boolean isEnabled)
    {
        userSettingsRef.update("notifications", isEnabled)
                .addOnFailureListener(e ->
                {
                    Toast.makeText(requireContext(),
                            "Failed to update notification setting: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });

        // Update Firebase Cloud Messaging subscription
        updateNotificationSubscription(isEnabled);
    }

    private void updateNotificationSubscription(boolean isSubscribed)
    {
        if(isSubscribed)
        {
            FirebaseMessaging.getInstance().subscribeToTopic("app_notifications")
                    .addOnCompleteListener(task ->
                    {
                        if(!task.isSuccessful())
                        {
                            Toast.makeText(requireContext(),
                                    "Failed to enable notifications",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        } else
        {
            FirebaseMessaging.getInstance().unsubscribeFromTopic("app_notifications")
                    .addOnCompleteListener(task ->
                    {
                        if(!task.isSuccessful())
                        {
                            Toast.makeText(requireContext(),
                                    "Failed to disable notifications",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void loadUserProfile()
    {
        FirebaseUser currentUser = auth.getCurrentUser();
        if(currentUser != null)
        {
            userRef.get().addOnSuccessListener(documentSnapshot ->
            {
                if(!isAdded() || getActivity() == null || getActivity().isFinishing())
                {
                    return;  // Fragment is no longer attached
                }

                if(documentSnapshot.exists())
                {
                    // Get user data
                    String name = documentSnapshot.getString("name");
                    String phone = documentSnapshot.getString("phoneNumber");
                    String email = documentSnapshot.getString("email");
                    String imageUrl = documentSnapshot.getString("profileImageURL");

                    // Update UI
                    if(name != null)
                    {
                        profileName.setText(name);
                    }
                    if(phone != null)
                    {
                        profilePhone.setText(phone);
                    }
                    if(email != null)
                    {
                        profileEmail.setText(email);
                    }

                    // Load profile image if exists
                    if(imageUrl != null && !imageUrl.isEmpty())
                    {
                        Glide.with(requireContext())
                                .load(imageUrl)
                                .placeholder(R.drawable.image_placeholder)
                                .into(editProfilePictureImageButton);
                    } else
                    {
                        editProfilePictureImageButton.setImageResource(R.drawable.image_placeholder);
                    }
                } else
                {
                    // Handle case when document doesn't exist
                    editProfilePictureImageButton.setImageResource(R.drawable.image_placeholder);
                }
            }).addOnFailureListener(e ->
            {
                if(isAdded())
                {
                    Log.e(TAG, "Error loading user profile", e);
                    editProfilePictureImageButton.setImageResource(R.drawable.image_placeholder);
                }
            });
        }
    }

    private void selectProfilePicture()
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
        if(bottomSheetDialog != null && bottomSheetDialog.isShowing())
        {
            bottomSheetDialog.dismiss();
        }

        // Create and configure new dialog
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_image, null);

        TextView selectImageTextview = bottomSheetView.findViewById(R.id.SelectImageTextView);
        selectImageTextview.setText("Select Profile Picture");

        bottomSheetDialog = new BottomSheetDialog(requireContext());
        bottomSheetDialog.setContentView(bottomSheetView);

        Button btnTakePhoto = bottomSheetView.findViewById(R.id.btn_take_photo);
        Button btnChooseGallery = bottomSheetView.findViewById(R.id.btn_choose_gallery);
        Button btnCancel = bottomSheetView.findViewById(R.id.btn_cancel);

        btnTakePhoto.setOnClickListener(v ->
        {
            Log.d(TAG, "Taking photo...");
            takePhoto();
            bottomSheetDialog.dismiss();
        });

        btnChooseGallery.setOnClickListener(v ->
        {
            Log.d(TAG, "Choosing from gallery...");
            pickFromGallery();
            bottomSheetDialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> bottomSheetDialog.dismiss());

        bottomSheetDialog.setOnDismissListener(dialog -> bottomSheetDialog = null);
        bottomSheetDialog.show();
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

    // Handle camera result
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
                                Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                                        requireActivity().getContentResolver(), photoUri);
                                bitmap = fixImageOrientation(photoUri, bitmap);
                                editProfilePictureImageButton.setImageBitmap(bitmap);

                                // Upload to Firebase
                                uploadImageToFirebase(bitmap);
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
                                Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                                        requireActivity().getContentResolver(), imageUri);
                                bitmap = fixImageOrientation(imageUri, bitmap);
                                editProfilePictureImageButton.setImageBitmap(bitmap);

                                // Upload to Firebase
                                uploadImageToFirebase(bitmap);
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
        FirebaseUser currentUser = auth.getCurrentUser();
        if(currentUser == null)
        {
            Log.e(TAG, "No user is currently logged in");
            Toast.makeText(requireContext(),
                    "You must be logged in to upload a profile picture", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        // Create a unique filename for the image
        String filename = "profile_images/" + userId + "/" + UUID.randomUUID().toString() + ".jpg";
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
                        updateUserProfileWithImageUrl(downloadUri.toString());
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

    private Bitmap fixImageOrientation(Uri imageUri, Bitmap bitmap)
    {
        try
        {
            // Read EXIF data to determine orientation
            ExifInterface exif = null;
            try(InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri))
            {
                if(inputStream != null)
                {
                    exif = new ExifInterface(inputStream);
                }
            }

            int orientation = ExifInterface.ORIENTATION_NORMAL;
            if(exif != null)
            {
                orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            }

            // Rotate bitmap based on orientation
            Matrix matrix = new Matrix();
            switch(orientation)
            {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.postRotate(90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.postRotate(180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
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

    private void updateUserProfileWithImageUrl(String imageUrl)
    {
        FirebaseUser currentUser = auth.getCurrentUser();
        if(currentUser != null)
        {
            // Delete the old profile image before updating to the new one
            deleteOldProfileImage(imageUrl);

            // Update the profileImageUrl field in the user's document
            Map<String, Object> updates = new HashMap<>();
            updates.put("profileImageURL", imageUrl);

            userRef.update(updates)
                    .addOnSuccessListener(aVoid ->
                    {
                        if(isAdded() && getActivity() != null && !getActivity().isFinishing())
                        {
                            Log.d(TAG, "User profile successfully updated with new image URL");
                            Toast.makeText(requireContext(),
                                    "Profile picture updated successfully", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e ->
                    {
                        if(isAdded() && getActivity() != null && !getActivity().isFinishing())
                        {
                            Log.e(TAG, "Error updating user profile with image URL", e);
                            Toast.makeText(requireContext(),
                                    "Failed to update profile with new image: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        } else
        {
            Log.e(TAG, "Cannot update profile - no user is currently logged in");
        }
    }

    private void deleteOldProfileImage(String newImageUrl)
    {
        FirebaseUser currentUser = auth.getCurrentUser();
        if(currentUser != null)
        {
            // Get the current profile image URL
            userRef.get().addOnSuccessListener(documentSnapshot ->
            {
                if(documentSnapshot.exists())
                {
                    String oldImageUrl = documentSnapshot.getString("profileImageURL");

                    // Only proceed if there's an old image URL and it's different from the new one
                    if(oldImageUrl != null && !oldImageUrl.isEmpty() && !oldImageUrl.equals(newImageUrl))
                    {
                        try
                        {
                            // Get the path after "/o/" and before "?"
                            String urlPath = oldImageUrl.split("/o/")[1];
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
                                Log.d(TAG, "Old profile image deleted successfully");
                            }).addOnFailureListener(e ->
                            {
                                Log.e(TAG, "Error deleting old profile image", e);
                            });
                        } catch(Exception e)
                        {
                            Log.e(TAG, "Error parsing old image URL: " + oldImageUrl, e);
                        }
                    }
                }
            }).addOnFailureListener(e ->
            {
                Log.e(TAG, "Error fetching user document to delete old image", e);
            });
        }
    }
}