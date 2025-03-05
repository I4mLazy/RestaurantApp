package com.example.restaurantapp.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.example.restaurantapp.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ProfileEditFragment extends Fragment
{

    private ImageButton editProfilePictureImageButton;
    private EditText editProfileUsername, editProfileEmail, editProfilePhone;
    private TextView editProfilePictureTextView;

    public ProfileEditFragment()
    {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_profile_edit, container, false);

        editProfilePictureImageButton = view.findViewById(R.id.editProfilePictureImageButton);
        editProfilePictureTextView = view.findViewById(R.id.editProfilePictureTextView);
        editProfileUsername = view.findViewById(R.id.editProfileUsername);
        editProfileEmail = view.findViewById(R.id.editProfileEmail);
        editProfilePhone = view.findViewById(R.id.editProfilePhone);
        Button saveButton = view.findViewById(R.id.saveProfileButton);
        Button cancelButton = view.findViewById(R.id.cancelProfileButton);

        editProfilePictureImageButton.setOnClickListener(v -> selectProfilePicture());
        editProfilePictureTextView.setOnClickListener(v -> selectProfilePicture());

        saveButton.setOnClickListener(v ->
        {
            // TODO: Implement save functionality (e.g., update Firestore or local storage)
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        cancelButton.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        return view;
    }

    private void selectProfilePicture()
    {
        // Check permissions for different Android versions
        boolean cameraPermissionGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        boolean storagePermissionGranted = checkStoragePermission();

        Log.d("Permissions", "Camera Permission Granted: " + cameraPermissionGranted);
        Log.d("Permissions", "Storage Permission Granted: " + storagePermissionGranted);

        if (!cameraPermissionGranted || !storagePermissionGranted)
        {
            // Request necessary permissions
            requestPermissions();
        } else
        {
            // Permissions granted, show bottom sheet
            Log.d("Permissions", "All permissions granted, showing bottom sheet.");
            showBottomSheetDialog();
        }
    }

    private boolean checkStoragePermission()
    {
        // Comprehensive storage permission check across Android versions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        {
            return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        {
            // Scoped storage doesn't require explicit permission
            return true;
        } else
        {
            // For Android 9 and below, check both read and write permissions
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
        permissionsToRequest.add(Manifest.permission.CAMERA);

        // Add storage permissions based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        {
            // For Android 13+, use READ_MEDIA_IMAGES
            permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES);
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
        {
            // For Android 9 and below, request both read and write
            permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        // Launch permission request if we have permissions to request
        if (!permissionsToRequest.isEmpty())
        {
            permissionLauncher.launch(permissionsToRequest.toArray(new String[0]));
        } else
        {
            // If no permissions need to be requested, show bottom sheet
            showBottomSheetDialog();
        }
    }

    // Permission request launcher
    // Modify the permission launcher to handle media permissions correctly
    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result ->
            {
                // Update permission checks based on Android version
                boolean cameraGranted = Boolean.TRUE.equals(result.get(Manifest.permission.CAMERA));
                boolean storageGranted = false;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                {
                    storageGranted = Boolean.TRUE.equals(result.get(Manifest.permission.READ_MEDIA_IMAGES));
                } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
                {
                    storageGranted = Boolean.TRUE.equals(result.get(Manifest.permission.WRITE_EXTERNAL_STORAGE))
                            && Boolean.TRUE.equals(result.get(Manifest.permission.READ_EXTERNAL_STORAGE));
                } else
                {
                    // For Android 10-12, storage permissions are not strictly required
                    storageGranted = true;
                }

                Log.d("Permissions", "Camera Permission Granted: " + cameraGranted);
                Log.d("Permissions", "Storage Permission Granted: " + storageGranted);

                if (cameraGranted && storageGranted)
                {
                    showBottomSheetDialog();
                } else
                {
                    Toast.makeText(requireContext(), "Permissions are required", Toast.LENGTH_SHORT).show();
                }
            });

    private void showBottomSheetDialog()
    {
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_profile_picture, null);

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        bottomSheetDialog.setContentView(bottomSheetView);

        Button btnTakePhoto = bottomSheetView.findViewById(R.id.btn_take_photo);
        Button btnChooseGallery = bottomSheetView.findViewById(R.id.btn_choose_gallery);
        Button btnCancel = bottomSheetView.findViewById(R.id.btn_cancel);

        btnTakePhoto.setOnClickListener(v ->
        {
            Log.d("ProfileEditFragment", "Taking photo...");
            takePhoto();
            bottomSheetDialog.dismiss();
        });

        btnChooseGallery.setOnClickListener(v ->
        {
            Log.d("ProfileEditFragment", "Choosing from gallery...");
            pickFromGallery();
            bottomSheetDialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> bottomSheetDialog.dismiss());

        bottomSheetDialog.show();
    }

    private Uri photoUri;

    private void takePhoto()
    {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(requireActivity().getPackageManager()) != null)
        {
            File photoFile = createImageFile();
            if (photoFile != null)
            {
                try
                {
                    photoUri = FileProvider.getUriForFile(requireContext(), "com.example.restaurantapp.fileprovider", photoFile);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                    Log.d("ProfileEditFragment", "Launching camera with URI: " + photoUri);
                    cameraLauncher.launch(intent);
                } catch (IllegalArgumentException e)
                {
                    Log.e("ProfileEditFragment", "Error creating file URI", e);
                    Toast.makeText(requireContext(), "Unable to create file for photo", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void pickFromGallery()
    {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        Log.d("ProfileEditFragment", "Launching gallery picker...");
        imagePickerLauncher.launch(intent);
    }

    // Handle camera result
    // Handle camera result
    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result ->
            {
                if (result.getResultCode() == Activity.RESULT_OK)
                {
                    Log.d("ProfileEditFragment", "Camera result received.");

                    // Check camera permission
                    boolean hasCameraPermission = ContextCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED;

                    // Check storage permission based on Android version
                    boolean hasStoragePermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                            || (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                            && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            == PackageManager.PERMISSION_GRANTED);

                    if (hasCameraPermission && hasStoragePermission)
                    {
                        Log.d("ProfileEditFragment", "Permissions granted, setting image URI.");
                        editProfilePictureImageButton.setImageURI(photoUri);
                    } else
                    {
                        Log.d("ProfileEditFragment", "Camera or storage permissions denied.");
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
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Log.d("ProfileEditFragment", "Gallery result received.");

                    // Use the same permission check logic as elsewhere in the code
                    boolean hasStoragePermission = checkStoragePermission();

                    if (hasStoragePermission) {
                        Uri imageUri = result.getData().getData();
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), imageUri);
                            editProfilePictureImageButton.setImageBitmap(bitmap);
                            Log.d("ProfileEditFragment", "Image set from gallery.");
                        } catch (IOException e) {
                            e.printStackTrace();
                            Log.e("ProfileEditFragment", "Error loading image from gallery", e);
                            Toast.makeText(requireContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // Show a message if permissions are denied
                        Log.d("ProfileEditFragment", "Storage permissions denied.");
                        Toast.makeText(requireContext(), "Storage permissions are required to access the gallery", Toast.LENGTH_SHORT).show();

                        // Request permissions again
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
            Log.d("ProfileEditFragment", "Image file created at: " + imageFile.getAbsolutePath());
            return imageFile;
        } catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }
}
