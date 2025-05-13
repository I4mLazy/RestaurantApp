package com.example.restaurantapp.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.restaurantapp.R;
import com.example.restaurantapp.models.MenuItem;
import com.example.restaurantapp.utils.DiscountUtils;
import com.example.restaurantapp.viewmodels.MenuItemSelectionViewModel;

public class MenuItemFragment extends Fragment
{
    private ImageView menuItemImage;
    private TextView discountBadge;
    private TextView menuItemName;
    private TextView menuItemDescription;
    private TextView menuItemPrice;
    private TextView menuItemOldPrice;
    private TextView menuItemCategory;
    private TextView menuItemAllergens;
    private TextView menuItemAvailability;

    private MenuItemSelectionViewModel viewModel;

    public MenuItemFragment()
    {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_menu_item, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState)
    {
        // Find views
        menuItemImage = view.findViewById(R.id.menuItemImage);
        discountBadge = view.findViewById(R.id.discountBadge);
        menuItemName = view.findViewById(R.id.menuItemName);
        menuItemDescription = view.findViewById(R.id.menuItemDescription);
        menuItemPrice = view.findViewById(R.id.menuItemPrice);
        menuItemOldPrice = view.findViewById(R.id.menuItemOldPrice);
        menuItemCategory = view.findViewById(R.id.menuItemCategory);
        menuItemAllergens = view.findViewById(R.id.menuItemAllergens);
        menuItemAvailability = view.findViewById(R.id.menuItemAvailability);

        viewModel = new ViewModelProvider(requireActivity()).get(MenuItemSelectionViewModel.class);

        viewModel.getSelectedMenuItem().observe(getViewLifecycleOwner(), menuItem ->
        {
            if(menuItem != null)
            {
                populateViews(menuItem, requireContext());
            } else
            {
                menuItemName.setText("No Menu Item Selected");
            }
        });

        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("FeedMe", Context.MODE_PRIVATE);

        String userType = sharedPreferences.getString("userType", "user");

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true)
        {
            @Override
            public void handleOnBackPressed()
            {
                RestaurantInfoFragment restaurantInfoFragment = new RestaurantInfoFragment();
                if("restaurant".equals(userType))
                {
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, restaurantInfoFragment)
                            .commit();
                } else if("user".equals(userType))
                {
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragmentContainer, restaurantInfoFragment)
                            .commit();
                }
            }
        });
    }

    private void populateViews(MenuItem menuItem, Context context)
    {
        // Name
        menuItemName.setText(!TextUtils.isEmpty(menuItem.getName()) ? menuItem.getName() : "N/A");

        // Description
        menuItemDescription.setText(!TextUtils.isEmpty(menuItem.getDescription()) ? menuItem.getDescription() : "No description available.");

        // Category
        menuItemCategory.setText("Category: " + (!TextUtils.isEmpty(menuItem.getCategory()) ? menuItem.getCategory() : "N/A"));

        // Availability
        if(menuItem.getAvailability())
        {
            menuItemAvailability.setText("Available");
            menuItemAvailability.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else
        {
            menuItemAvailability.setText("Unavailable");
            menuItemAvailability.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }

        // Allergens
        if(menuItem.getAllergens() != null && !menuItem.getAllergens().isEmpty())
        {
            menuItemAllergens.setText(TextUtils.join(", ", menuItem.getAllergens()));
        } else
        {
            menuItemAllergens.setText("None");
        }

        // Load image
        if(!TextUtils.isEmpty(menuItem.getImageURL()))
        {
            Glide.with(context)
                    .load(menuItem.getImageURL())
                    .placeholder(R.drawable.image_placeholder)
                    .error(R.drawable.image_placeholder)
                    .into(menuItemImage);
        } else
        {
            Glide.with(context)
                    .load(R.drawable.image_placeholder)
                    .into(menuItemImage);
        }

        // Discounts
        DiscountUtils.applyActiveDiscounts(menuItem, context, (originalPrice, finalPrice, hasDiscount, isFree, badgeText) ->
        {
            if(hasDiscount)
            {
                menuItemPrice.setText(String.format("Price: $%.2f", finalPrice));
                menuItemOldPrice.setVisibility(View.VISIBLE);
                menuItemOldPrice.setText(String.format("$%.2f", originalPrice));
                menuItemOldPrice.setPaintFlags(menuItemOldPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG); // Strikethrough effect
                discountBadge.setVisibility(View.VISIBLE);
                discountBadge.setText(badgeText);
            } else
            {
                menuItemPrice.setText(String.format("Price: $%.2f", originalPrice));
                menuItemOldPrice.setVisibility(View.GONE);
                discountBadge.setVisibility(View.GONE);
            }
        });
    }
}
