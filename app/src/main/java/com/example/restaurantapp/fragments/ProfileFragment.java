package com.example.restaurantapp.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.bumptech.glide.Glide;
import com.example.restaurantapp.R;
import com.example.restaurantapp.activities.AuthenticationActivity;
import com.example.restaurantapp.activities.EditProfileActivity;
import com.example.restaurantapp.activities.SettingsActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
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
import java.util.UUID;

public class ProfileFragment extends Fragment
{
    private ImageButton editProfilePictureImageButton;
    private TextView profileName, profileEmail, profilePhone, editProfilePictureTextView;
    private ActivityResultLauncher<Intent> editProfileLauncher;
    private BottomSheetDialog bottomSheetDialog;
    private UploadTask currentUploadTask;
    private boolean isUploading = false;
    private Uri photoUri;

    FirebaseFirestore db;
    FirebaseAuth auth;
    FirebaseStorage storage;
    StorageReference storageRef;
    private static final String TAG = "ProfileFragment";

    public ProfileFragment()
    {
        // Required empty public constructor
    }

    //TODO: CHANGE THE WAY EMAIL IS CHANGED!!! CURRENTLY ONLY CHANGES IT IN FIREBASE!!!
    //TODO: CHANGE PFP UPLOAD TO ASYNC TASK INSTEAD OF CANCELLING

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        profileName = view.findViewById(R.id.profileName);
        profileEmail = view.findViewById(R.id.profileEmail);
        profilePhone = view.findViewById(R.id.profilePhone);
        editProfilePictureImageButton = view.findViewById(R.id.editProfilePictureImageButton);
        editProfilePictureTextView = view.findViewById(R.id.editProfilePictureTextView);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        // Load user profile data
        loadUserProfile();

        // Set click listeners for profile fields
        view.findViewById(R.id.editProfileNameContainer).setOnClickListener(v -> launchEditActivity("Name", profileName.getText().toString()));
        view.findViewById(R.id.editProfileEmailContainer).setOnClickListener(v -> launchEditActivity("Email", profileEmail.getText().toString()));
        view.findViewById(R.id.editProfilePhoneContainer).setOnClickListener(v -> launchEditActivity("Phone", profilePhone.getText().toString()));

        // Set click listeners for profile picture
        editProfilePictureImageButton.setOnClickListener(v -> selectProfilePicture());
        editProfilePictureTextView.setOnClickListener(v -> selectProfilePicture());

        // Settings button
        Button settingsButton = view.findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(v ->
        {
            if(getActivity() != null)
            {
                Intent intent = new Intent(getActivity(), SettingsActivity.class);
                startActivity(intent);
            }
        });

        // Logout button
        Button logoutButton = view.findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(v -> handleLogout());

        // Register the ActivityResultLauncher for profile edits
        editProfileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> handleEditProfileResult(result)
        );

        // Set up real-time updates for user profile
        setupProfileListener();

        return view;
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

    private void setupProfileListener()
    {
        FirebaseUser currentUser = auth.getCurrentUser();
        if(currentUser != null)
        {
            DocumentReference userRef = db.collection("Users").document(currentUser.getUid());
            userRef.addSnapshotListener((documentSnapshot, e) ->
            {
                if(e != null)
                {
                    Log.e(TAG, "Listen failed", e);
                    return;
                }

                if(isAdded() && getActivity() != null && !getActivity().isFinishing())
                {
                    loadUserProfile();
                }
            });
        }
    }

    private void handleLogout()
    {
        if(auth.getCurrentUser() != null)
        {
            new androidx.appcompat.app.AlertDialog.Builder(requireActivity())
                    .setTitle("Log Out")
                    .setMessage("Are you sure you want to log out?")
                    .setPositiveButton("Yes", (dialog, which) ->
                    {
                        FirebaseAuth.getInstance().signOut();

                        // Clear shared preferences
                        SharedPreferences sharedPreferences = requireActivity()
                                .getSharedPreferences("MyAppPrefs", FragmentActivity.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.clear();
                        editor.apply();

                        // Redirect to authentication
                        Intent intent = new Intent(getActivity(), AuthenticationActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                Intent.FLAG_ACTIVITY_NEW_TASK |
                                Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        requireActivity().finish();
                    })
                    .setNegativeButton("No", (dialog, id) -> dialog.dismiss())
                    .create()
                    .show();
        }
    }

    private void handleEditProfileResult(androidx.activity.result.ActivityResult result)
    {
        if(result.getResultCode() == Activity.RESULT_OK && result.getData() != null)
        {
            String updatedValue = result.getData().getStringExtra("updatedValue");
            String fieldType = result.getData().getStringExtra("fieldType");

            if(updatedValue != null && fieldType != null)
            {
                // Update UI based on field type
                if("Name".equals(fieldType))
                {
                    profileName.setText(updatedValue);
                } else if("Email".equals(fieldType))
                {
                    profileEmail.setText(updatedValue);
                } else if("Phone".equals(fieldType))
                {
                    profilePhone.setText("+" + updatedValue);
                }

                // Save to Firestore
                saveUserProfile(updatedValue, fieldType);
            }
        }
    }

    private void loadUserProfile()
    {
        FirebaseUser currentUser = auth.getCurrentUser();
        if(currentUser != null)
        {
            DocumentReference userRef = db.collection("Users").document(currentUser.getUid());

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
                    String imageUrl = documentSnapshot.getString("profileImageUrl");

                    // Update UI
                    profileName.setText(name);
                    profilePhone.setText(phone);
                    profileEmail.setText(email);

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

    private void saveUserProfile(String updatedValue, String fieldType)
    {
        FirebaseUser currentUser = auth.getCurrentUser();
        if(currentUser != null)
        {
            DocumentReference userRef = db.collection("Users").document(currentUser.getUid());

            Map<String, Object> updatedData = new HashMap<>();
            if("Name".equals(fieldType))
            {
                updatedData.put("name", updatedValue);
            } else if("Email".equals(fieldType))
            {
                updatedData.put("email", updatedValue);
            } else if("Phone".equals(fieldType))
            {
                updatedData.put("phoneNumber", "+" + updatedValue);
            }

            userRef.update(updatedData)
                    .addOnSuccessListener(aVoid ->
                    {
                        if(isAdded())
                        {
                            Toast.makeText(getContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e ->
                    {
                        if(isAdded())
                        {
                            Toast.makeText(getContext(), "Error updating profile", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Error updating profile", e);
                        }
                    });
        }
    }

    private void launchEditActivity(String fieldType, String currentValue)
    {
        Intent intent = new Intent(getActivity(), EditProfileActivity.class);
        intent.putExtra("fieldType", fieldType);
        intent.putExtra("currentValue", currentValue);
        editProfileLauncher.launch(intent);
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
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_profile_picture, null);
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
                                Log.d(TAG, "Image set from gallery.");

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
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            {
                try(InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri))
                {
                    if(inputStream != null)
                    {
                        exif = new ExifInterface(inputStream);
                    }
                }
            } else
            {
                // For older Android versions
                String[] filePathColumn = {MediaStore.Images.Media.DATA};
                Cursor cursor = requireContext().getContentResolver().query(
                        imageUri, filePathColumn, null, null, null);
                if(cursor != null && cursor.moveToFirst())
                {
                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    String filePath = cursor.getString(columnIndex);
                    cursor.close();
                    if(filePath != null)
                    {
                        exif = new ExifInterface(filePath);
                    }
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
            DocumentReference userRef = db.collection("Users").document(currentUser.getUid());

            // Delete the old profile image before updating to the new one
            deleteOldProfileImage(imageUrl);

            // Update the profileImageUrl field in the user's document
            Map<String, Object> updates = new HashMap<>();
            updates.put("profileImageUrl", imageUrl);

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
            DocumentReference userRef = db.collection("Users").document(currentUser.getUid());

            // Get the current profile image URL
            userRef.get().addOnSuccessListener(documentSnapshot ->
            {
                if(documentSnapshot.exists())
                {
                    String oldImageUrl = documentSnapshot.getString("profileImageUrl");

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