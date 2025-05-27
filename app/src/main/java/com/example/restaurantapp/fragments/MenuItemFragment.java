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

/**
 * A {@link Fragment} subclass responsible for displaying the details of a single menu item.
 * It observes a {@link MenuItemSelectionViewModel} to get the selected menu item data
 * and populates the UI with its name, description, price (including discounts), category,
 * allergens, availability, and image.
 * It also handles back press navigation to return to the {@link RestaurantInfoFragment},
 * considering the current user type (user or restaurant).
 */
public class MenuItemFragment extends Fragment
{
    /**
     * ImageView to display the menu item's image.
     */
    private ImageView menuItemImage;
    /**
     * TextView to display a discount badge if applicable.
     */
    private TextView discountBadge;
    /**
     * TextView to display the menu item's name.
     */
    private TextView menuItemName;
    /**
     * TextView to display the menu item's description.
     */
    private TextView menuItemDescription;
    /**
     * TextView to display the menu item's current price (possibly discounted).
     */
    private TextView menuItemPrice;
    /**
     * TextView to display the menu item's original price if a discount is applied.
     */
    private TextView menuItemOldPrice;
    /**
     * TextView to display the menu item's category.
     */
    private TextView menuItemCategory;
    /**
     * TextView to display the menu item's allergens.
     */
    private TextView menuItemAllergens;
    /**
     * TextView to display the menu item's availability status.
     */
    private TextView menuItemAvailability;

    /**
     * ViewModel for observing the currently selected menu item.
     */
    private MenuItemSelectionViewModel viewModel;

    /**
     * Required empty public constructor for Fragment instantiation.
     */
    public MenuItemFragment()
    {
        // Required empty public constructor
    }

    /**
     * Called to have the fragment instantiate its user interface view.
     * This method inflates the layout defined in {@code R.layout.fragment_menu_item}.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate
     *                           any views in the fragment.
     * @param container          If non-null, this is the parent view that the fragment's
     *                           UI should be attached to. The fragment should not add the view itself,
     *                           but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     *                           from a previous saved state as given here.
     * @return Return the View for the fragment's UI.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_menu_item, container, false);
    }

    /**
     * Called immediately after {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}
     * has returned, but before any saved state has been restored in to the view.
     * Initializes UI components by finding their views.
     * Initializes the {@link MenuItemSelectionViewModel} and observes the selected menu item.
     * When a menu item is selected (or changes), {@link #populateViews(MenuItem, Context)} is called.
     * If no item is selected, a default message is shown.
     * It also sets up a custom {@link OnBackPressedCallback} to navigate back to
     * {@link RestaurantInfoFragment}, determining the correct fragment container ID based on the
     * "userType" stored in SharedPreferences.
     *
     * @param view               The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     *                           from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) // Added @NonNull for view
    {
        super.onViewCreated(view, savedInstanceState); // Call super method

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
                // Handle case where no menu item is selected or data is cleared
                menuItemName.setText("No Menu Item Selected");
                // Optionally clear other fields or show a placeholder state
                menuItemDescription.setText("");
                menuItemPrice.setText("");
                menuItemOldPrice.setVisibility(View.GONE);
                discountBadge.setVisibility(View.GONE);
                menuItemCategory.setText("");
                menuItemAllergens.setText("");
                menuItemAvailability.setText("");
                menuItemImage.setImageResource(R.drawable.image_placeholder); // Default image
            }
        });

        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("FeedMe", Context.MODE_PRIVATE);
        String userType = sharedPreferences.getString("userType", "user"); // Default to "user"

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true)
        {
            @Override
            public void handleOnBackPressed()
            {
                RestaurantInfoFragment restaurantInfoFragment = new RestaurantInfoFragment();
                int containerId;
                if("restaurant".equals(userType))
                {
                    containerId = R.id.fragment_container; // Container for restaurant user
                } else // Default to user or if userType is "user"
                {
                    containerId = R.id.fragmentContainer; // Container for regular user
                }

                if(getActivity() != null)
                {
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(containerId, restaurantInfoFragment)
                            .commit();
                }
                // Note: setEnabled(false) is not called here, so the callback remains active.
                // If this is intended to be a one-off back press handler for this specific state,
                // consider disabling it or removing it after execution.
            }
        });
    }

    /**
     * Populates the UI views with data from the provided {@link MenuItem} object.
     * Sets the item's name (defaults to "N/A" if empty/null), description (defaults to
     * "No description available." if empty/null), and category (prefixed with "Category: ",
     * defaults to "N/A" if category is empty/null).
     * Sets the availability status text ("Available" or "Unavailable") and color
     * (green or red respectively) based on {@link MenuItem#getAvailability()}.
     * Note: This assumes {@link MenuItem#getAvailability()} returns a non-null Boolean if called.
     * Displays allergens as a comma-separated string, or "None" if no allergens are present.
     * Loads the item's image using Glide from {@link MenuItem#getImageURL()}, showing a placeholder
     * if the URL is empty/null or if loading fails. If the URL is empty/null, it explicitly loads
     * the placeholder drawable.
     * Uses {@link com.example.restaurantapp.utils.DiscountUtils#applyActiveDiscounts(com.example.restaurantapp.models.MenuItem, android.content.Context, com.example.restaurantapp.utils.DiscountUtils.DiscountResultCallback)}
     * to determine and display pricing information.
     * Inside the callback:
     * <ul>
     *     <li>If {@code hasDiscount} is true:
     *         <ul>
     *             <li>{@code menuItemPrice} is set to the {@code finalPrice} formatted as "Price: $X.XX".</li>
     *             <li>{@code menuItemOldPrice} is made visible and set to the {@code originalPrice} formatted as "$X.XX" with a strikethrough.</li>
     *             <li>{@code discountBadge} is made visible and its text is set to {@code badgeText}.</li>
     *         </ul>
     *     </li>
     *     <li>If {@code hasDiscount} is false:
     *         <ul>
     *             <li>{@code menuItemPrice} is set to the {@code originalPrice} formatted as "Price: $X.XX".</li>
     *             <li>{@code menuItemOldPrice} is hidden.</li>
     *             <li>{@code discountBadge} is hidden.</li>
     *         </ul>
     *     </li>
     * </ul>
     * The {@code isFree} parameter from the callback is not directly used to set text in this method's implementation of the callback,
     * but its value might influence {@code badgeText} or {@code finalPrice} as determined by {@code DiscountUtils}.
     *
     * @param menuItem The {@link MenuItem} object whose data will be used to populate the views.
     * @param context  The context used for resource access (e.g., Glide, string resources).
     */
    private void populateViews(MenuItem menuItem, Context context)
    {
        // Name
        menuItemName.setText(!TextUtils.isEmpty(menuItem.getName()) ? menuItem.getName() : "N/A");

        // Description
        menuItemDescription.setText(!TextUtils.isEmpty(menuItem.getDescription()) ? menuItem.getDescription() : "No description available.");

        // Category
        menuItemCategory.setText("Category: " + (!TextUtils.isEmpty(menuItem.getCategory()) ? menuItem.getCategory() : "N/A"));

        // Availability
        if(menuItem.getAvailability()) // Potential NPE if getAvailability() returns null and is unboxed
        {
            menuItemAvailability.setText("Available");
            menuItemAvailability.setTextColor(getResources().getColor(android.R.color.holo_green_dark)); // Deprecated getColor without theme
        } else
        {
            menuItemAvailability.setText("Unavailable");
            menuItemAvailability.setTextColor(getResources().getColor(android.R.color.holo_red_dark)); // Deprecated getColor without theme
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
                    .load(R.drawable.image_placeholder) // Loads placeholder if URL is empty
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
            // Note: The isFree parameter from the callback is not explicitly used here to set text,
            // but DiscountUtils might influence badgeText or finalPrice based on it.
        });
    }
}
