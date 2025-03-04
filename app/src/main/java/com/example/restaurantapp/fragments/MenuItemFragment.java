package com.example.restaurantapp.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.restaurantapp.R;
import com.example.restaurantapp.models.MenuItem;
import com.example.restaurantapp.viewmodels.MenuItemSelectionViewModel;

public class MenuItemFragment extends Fragment {

    private ImageView menuItemImage;
    private TextView menuItemName;
    private TextView menuItemDescription;
    private TextView menuItemPrice;
    private TextView menuItemOptions;
    private TextView menuItemCustomizations;
    private TextView menuItemAllergens;
    private TextView menuItemSpecialOffer;
    private TextView menuItemOrderIndex;
    private TextView menuItemMaxSelection;

    private MenuItemSelectionViewModel viewModel;

    public MenuItemFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_menu_item, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        // Find all views safely
        menuItemImage = view.findViewById(R.id.menuItemImage);
        menuItemName = view.findViewById(R.id.menuItemName);
        menuItemDescription = view.findViewById(R.id.menuItemDescription);
        menuItemPrice = view.findViewById(R.id.menuItemPrice);
        menuItemOptions = view.findViewById(R.id.menuItemOptions);
        menuItemCustomizations = view.findViewById(R.id.menuItemCustomizations);
        menuItemAllergens = view.findViewById(R.id.menuItemAllergens);
        menuItemSpecialOffer = view.findViewById(R.id.menuItemSpecialOffer);
        menuItemOrderIndex = view.findViewById(R.id.menuItemOrderIndex);
        menuItemMaxSelection = view.findViewById(R.id.menuItemMaxSelection);

        // Get the shared ViewModel
        viewModel = new ViewModelProvider(requireActivity()).get(MenuItemSelectionViewModel.class);

        // Observe the selected MenuItem and update the UI safely
        viewModel.getSelectedMenuItem().observe(getViewLifecycleOwner(), menuItem -> {
            if (menuItem != null) {
                populateViews(menuItem);
            } else {
                // Optionally, show a default message if no item is selected
                menuItemName.setText("No Menu Item Selected");
            }
        });
    }

    private void populateViews(MenuItem menuItem) {
        // Menu item name
        if (!TextUtils.isEmpty(menuItem.getName())) {
            menuItemName.setText(menuItem.getName());
        } else {
            menuItemName.setText("N/A");
        }

        // Menu item description
        if (!TextUtils.isEmpty(menuItem.getDescription())) {
            menuItemDescription.setText(menuItem.getDescription());
        } else {
            menuItemDescription.setText("No description available.");
        }

        // Price
        if (menuItem.getPrice() > 0) {
            menuItemPrice.setText("Price: $" + menuItem.getPrice());
        } else {
            menuItemPrice.setText("Price: N/A");
        }

        // Options (if available, join names by comma)
        if (menuItem.getOptions() != null && !menuItem.getOptions().isEmpty()) {
            StringBuilder optionsBuilder = new StringBuilder();
            for (MenuItem.Option option : menuItem.getOptions()) {
                if (!TextUtils.isEmpty(option.getName())) {
                    if (optionsBuilder.length() > 0) {
                        optionsBuilder.append(", ");
                    }
                    optionsBuilder.append(option.getName());
                }
            }
            menuItemOptions.setText(optionsBuilder.toString());
        } else {
            menuItemOptions.setText("None");
        }

        // Customizations (if available)
        if (menuItem.getRequiredCustomizations() != null && !menuItem.getRequiredCustomizations().isEmpty()) {
            StringBuilder customizationsBuilder = new StringBuilder();
            for (MenuItem.RequiredCustomization customization : menuItem.getRequiredCustomizations()) {
                if (!TextUtils.isEmpty(customization.getName())) {
                    if (customizationsBuilder.length() > 0) {
                        customizationsBuilder.append("\n");
                    }
                    customizationsBuilder.append(customization.getName());
                    if (!TextUtils.isEmpty(customization.getDescription())) {
                        customizationsBuilder.append(": ").append(customization.getDescription());
                    }
                }
            }
            menuItemCustomizations.setText(customizationsBuilder.toString());
        } else {
            menuItemCustomizations.setText("None");
        }

        // Allergens (if available)
        if (menuItem.getAllergens() != null && !menuItem.getAllergens().isEmpty()) {
            menuItemAllergens.setText(android.text.TextUtils.join(", ", menuItem.getAllergens()));
        } else {
            menuItemAllergens.setText("None");
        }

        // Special offer
        if (menuItem.getIsSpecialOffer() != null && menuItem.getIsSpecialOffer()) {
            menuItemSpecialOffer.setText("Special Offer: Yes");
        } else {
            menuItemSpecialOffer.setText("Special Offer: No");
        }

        // Order index
        menuItemOrderIndex.setText("Order Index: " + menuItem.getOrderIndex());

        // Max selection
        menuItemMaxSelection.setText("Max Selection: " + menuItem.getMaxSelection());

        // Load image using Glide with fallback
        String imageUrl = menuItem.getImageURL();
        if (!TextUtils.isEmpty(imageUrl)) {
            Glide.with(requireContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.image_placeholder)
                    .error(R.drawable.image_placeholder)
                    .into(menuItemImage);
        } else {
            Glide.with(requireContext())
                    .load(R.drawable.image_placeholder)
                    .into(menuItemImage);
        }
    }
}
