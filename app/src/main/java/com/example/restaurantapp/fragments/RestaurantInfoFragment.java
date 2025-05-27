package com.example.restaurantapp.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SearchView; // Standard SearchView

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.restaurantapp.R;
import com.example.restaurantapp.adapters.MenuAdapter;
import com.example.restaurantapp.models.Menu;
import com.example.restaurantapp.models.MenuItem;
import com.example.restaurantapp.models.Restaurant;
import com.example.restaurantapp.viewmodels.MenuItemSelectionViewModel;
import com.example.restaurantapp.viewmodels.RestaurantViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A {@link Fragment} subclass that displays detailed information about a specific restaurant.
 * It shows the restaurant's name, address, rating, business hours, contact info, type, tags,
 * price level, description, and its menu (using a {@link MenuAdapter}).
 * For regular users, it allows making reservations (if the restaurant is reservable),
 * rating the restaurant, and navigating to it.
 * For restaurant users, it provides an option to edit the restaurant's information
 * (navigating to {@link EditRestaurantInfoFragment}).
 * The fragment uses a {@link RestaurantViewModel} to get the current restaurant data
 * and a {@link MenuItemSelectionViewModel} for handling menu item selections.
 */
public class RestaurantInfoFragment extends Fragment
{

    /**
     * ImageView to display the restaurant's main image/logo.
     */
    private ImageView restaurantDetailImage;
    /**
     * TextView to display the restaurant's name.
     */
    private TextView restaurantDetailName;
    /**
     * TextView to display the restaurant's address.
     */
    private TextView restaurantDetailAddress;
    /**
     * TextView to display the restaurant's average rating.
     */
    private TextView restaurantDetailRating;
    /**
     * TextView to display the restaurant's business hours.
     */
    private TextView restaurantDetailBusinessHours;
    /**
     * TextView to display the restaurant's contact information.
     */
    private TextView restaurantDetailContactInfo;
    /**
     * TextView to indicate if the restaurant is reservable.
     */
    private TextView restaurantDetailReservable;
    /**
     * TextView to display the restaurant's type/cuisine.
     */
    private TextView restaurantDetailType;
    /**
     * TextView to display the restaurant's tags.
     */
    private TextView restaurantDetailTags;
    /**
     * TextView to display the restaurant's price level.
     */
    private TextView restaurantDetailPriceLevel;
    /**
     * TextView to display the restaurant's description.
     */
    private TextView restaurantDetailDescription;
    /**
     * TextView within the reservation overlay to display the restaurant's business hours.
     */
    private TextView reservationOverlayBusinessHours;
    /**
     * TextView displayed when menu search yields no results.
     */
    private TextView noResults;

    /**
     * ImageButton for users to rate the restaurant.
     */
    private ImageButton rateButton;
    /**
     * Button for restaurant users to navigate to the edit restaurant info screen.
     */
    private Button restaurantDetailEditButton;
    /**
     * Button for users to open the reservation overlay (if reservable).
     */
    private Button openReservationOverlayButton;
    /**
     * Button for users to navigate to the restaurant's location.
     */
    private Button navigateButton;

    /**
     * EditText for users to input the number of guests for a reservation.
     */
    private EditText guestAmountEditText;
    /**
     * EditText for users to input special requests for a reservation.
     */
    private EditText specialRequestsEditText;
    /**
     * MaterialButton within the reservation overlay to cancel the reservation process.
     */
    private MaterialButton cancelReservationButton;
    /**
     * MaterialButton within the reservation overlay to confirm and save the reservation.
     */
    private MaterialButton confirmReservationButton;

    /**
     * DatePicker within the reservation overlay for selecting the reservation date.
     */
    private DatePicker datePicker;
    /**
     * NumberPicker within the reservation overlay for selecting the reservation hour.
     */
    private NumberPicker hourPicker;
    /**
     * NumberPicker within the reservation overlay for selecting the reservation minute.
     */
    private NumberPicker minutePicker;

    /**
     * SearchView for filtering the restaurant's menu.
     */
    private SearchView searchBar;
    /**
     * ProgressBar for indicating loading states.
     */
    private ProgressBar progressBar;
    /**
     * RecyclerView to display the restaurant's menus.
     */
    private RecyclerView recyclerViewMenus;
    /**
     * FrameLayout acting as an overlay for making reservations.
     */
    private FrameLayout reservationOverlay;

    /**
     * Adapter for the {@link #recyclerViewMenus}.
     */
    private MenuAdapter menuAdapter;
    /**
     * List of {@link Menu} objects for the current restaurant.
     */
    private List<Menu> menuList = new ArrayList<>();
    /**
     * List of all {@link MenuItem} objects across all menus for the current restaurant (used for search).
     */
    private List<MenuItem> menuItemList = new ArrayList<>();
    /**
     * The ID of the currently displayed restaurant.
     */
    private String restaurantID;
    /**
     * The ID of the fragment container, determined by user type.
     */
    int containerID;

    /**
     * ViewModel for accessing the current restaurant's data.
     */
    private RestaurantViewModel viewModel;

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
     * Required empty public constructor for Fragment instantiation.
     */
    public RestaurantInfoFragment()
    {
        // Required empty public constructor
    }

    /**
     * Called to have the fragment instantiate its user interface view.
     * Inflates the layout, initializes UI components and Firebase services.
     * It determines the current user type (restaurant or regular user) from SharedPreferences.
     * Based on the user type, it either fetches the restaurant data directly (for restaurant users)
     * or observes the {@link RestaurantViewModel} for the current restaurant data (for regular users).
     * It then calls {@link #bindRestaurantData(Restaurant)} to populate the UI and
     * {@link #setupMenuItems(String)} to display the menu.
     * Click listeners are set up for edit, rate, navigate, and reservation buttons.
     * A custom {@link OnBackPressedCallback} is registered for user navigation.
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_restaurant_info, container, false);

        // Initialize UI components
        restaurantDetailImage = view.findViewById(R.id.restaurantDetailLogo);
        restaurantDetailName = view.findViewById(R.id.restaurantDetailName);
        restaurantDetailAddress = view.findViewById(R.id.restaurantDetailAddress);
        restaurantDetailRating = view.findViewById(R.id.restaurantDetailRating);
        restaurantDetailBusinessHours = view.findViewById(R.id.restaurantDetailBusinessHours);
        restaurantDetailContactInfo = view.findViewById(R.id.restaurantDetailContactInfo);
        restaurantDetailReservable = view.findViewById(R.id.restaurantDetailReservable);
        restaurantDetailType = view.findViewById(R.id.restaurantDetailType);
        restaurantDetailTags = view.findViewById(R.id.restaurantDetailTags);
        restaurantDetailPriceLevel = view.findViewById(R.id.restaurantDetailPriceLevel);
        restaurantDetailDescription = view.findViewById(R.id.restaurantDetailDescription);
        reservationOverlayBusinessHours = view.findViewById(R.id.reservationOverlayBusinessHours);
        noResults = view.findViewById(R.id.noResults);

        rateButton = view.findViewById(R.id.rateButton);
        restaurantDetailEditButton = view.findViewById(R.id.restaurantDetailEditButton);
        openReservationOverlayButton = view.findViewById(R.id.openReserveOverlayButton);
        navigateButton = view.findViewById(R.id.navigateButton);
        cancelReservationButton = view.findViewById(R.id.cancelReservationButton);
        confirmReservationButton = view.findViewById(R.id.confirmReservationButton);

        datePicker = view.findViewById(R.id.datePickerReservation);
        hourPicker = view.findViewById(R.id.hour_picker);
        minutePicker = view.findViewById(R.id.minute_picker);

        guestAmountEditText = view.findViewById(R.id.guestAmountEditText);
        specialRequestsEditText = view.findViewById(R.id.specialRequestsEditText);

        searchBar = view.findViewById(R.id.searchBar);
        progressBar = view.findViewById(R.id.progressBar);
        recyclerViewMenus = view.findViewById(R.id.menusRecyclerView);
        reservationOverlay = view.findViewById(R.id.reservationOverlay);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        if(currentUser != null)
        {
            // Determine user type and load data accordingly
            db.collection("Users").document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot ->
                    {
                        if(!isAdded() || getContext() == null) return; // Check fragment state

                        if(documentSnapshot.exists())
                        {
                            SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("FeedMe", Context.MODE_PRIVATE);
                            String userType = sharedPreferences.getString("userType", "user"); // Default to "user"

                            if("restaurant".equals(userType))
                            {
                                restaurantDetailEditButton.setVisibility(View.VISIBLE);
                                rateButton.setVisibility(View.GONE);
                                navigateButton.setVisibility(View.GONE);
                                openReservationOverlayButton.setVisibility(View.GONE); // Restaurants don't reserve at their own place via this UI

                                restaurantID = documentSnapshot.getString("restaurantID");
                                if(restaurantID != null && !restaurantID.isEmpty())
                                {
                                    db.collection("Restaurants").document(restaurantID).get()
                                            .addOnSuccessListener(restaurantSnapshot ->
                                            {
                                                if(!isAdded()) return;
                                                if(restaurantSnapshot.exists())
                                                {
                                                    Restaurant restaurant = restaurantSnapshot.toObject(Restaurant.class);
                                                    if(restaurant != null)
                                                    { // Null check for restaurant object
                                                        restaurant.setRestaurantID(restaurantID); // Ensure ID is set
                                                        bindRestaurantData(restaurant);
                                                        setupMenuItems(restaurantID); // Pass ID for menu loading

                                                        restaurantDetailEditButton.setOnClickListener(v ->
                                                        {
                                                            if(viewModel == null)
                                                            { // Initialize ViewModel if null
                                                                viewModel = new ViewModelProvider(requireActivity()).get(RestaurantViewModel.class);
                                                            }
                                                            viewModel.setCurrentRestaurant(restaurant);
                                                            EditRestaurantInfoFragment editFrag = new EditRestaurantInfoFragment();
                                                            if(getActivity() != null)
                                                            {
                                                                // Determine correct container ID for restaurant user
                                                                // Assuming R.id.fragment_container is for restaurant main activity
                                                                getActivity().getSupportFragmentManager().beginTransaction()
                                                                        .replace(R.id.fragment_container, editFrag)
                                                                        .addToBackStack(null) // Allow back navigation
                                                                        .commit();
                                                            }
                                                        });
                                                    } else
                                                    {
                                                        Toast.makeText(getContext(), "Error loading restaurant data.", Toast.LENGTH_SHORT).show();
                                                    }
                                                } else
                                                {
                                                    Toast.makeText(getContext(), "Restaurant data not found.", Toast.LENGTH_SHORT).show();
                                                }
                                            })
                                            .addOnFailureListener(e ->
                                            {
                                                if(isAdded())
                                                    Toast.makeText(getContext(), "Failed to load restaurant: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            });
                                } else
                                {
                                    Toast.makeText(getContext(), "No restaurant assigned to this user.", Toast.LENGTH_SHORT).show();
                                }
                            } else if("user".equals(userType))
                            {
                                restaurantDetailEditButton.setVisibility(View.GONE);
                                rateButton.setVisibility(View.VISIBLE);
                                navigateButton.setVisibility(View.VISIBLE);
                                // openReservationOverlayButton visibility handled in bindRestaurantData

                                viewModel = new ViewModelProvider(requireActivity()).get(RestaurantViewModel.class);
                                viewModel.getCurrentRestaurant().observe(getViewLifecycleOwner(), restaurant ->
                                {
                                    if(restaurant != null)
                                    {
                                        restaurantID = restaurant.getRestaurantID(); // Get ID from ViewModel's restaurant
                                        if(restaurantID == null || restaurantID.isEmpty())
                                        {
                                            Log.e("RestaurantInfo", "Restaurant ID from ViewModel is null or empty.");
                                            Toast.makeText(getContext(), "Error: Could not load restaurant details.", Toast.LENGTH_SHORT).show();
                                            return;
                                        }
                                        bindRestaurantData(restaurant);
                                        setupMenuItems(restaurantID);

                                        navigateButton.setOnClickListener(v ->
                                        {
                                            if(restaurant.getAddress() != null)
                                                openNavigationApp(restaurant.getAddress());
                                            else
                                                Toast.makeText(getContext(), "Address not available for navigation.", Toast.LENGTH_SHORT).show();
                                        });
                                        cancelReservationButton.setOnClickListener(v -> reservationOverlay.setVisibility(View.GONE));
                                        confirmReservationButton.setOnClickListener(v -> validateReservationAndSave(restaurant));
                                    } else
                                    {
                                        Log.w("RestaurantInfo", "Restaurant data from ViewModel is null.");
                                        // Handle UI for no restaurant selected, e.g., show message or navigate back
                                    }
                                });

                                // Custom back press for regular users to pop back stack
                                requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),
                                        new OnBackPressedCallback(true)
                                        {
                                            @Override
                                            public void handleOnBackPressed()
                                            {
                                                if(getParentFragmentManager().getBackStackEntryCount() > 0)
                                                {
                                                    getParentFragmentManager().popBackStack();
                                                } else
                                                {
                                                    // If no back stack, default behavior (e.g., exit or handled by activity)
                                                    setEnabled(false); // Allow default back press
                                                    requireActivity().onBackPressed(); // Trigger activity's back press
                                                }
                                            }
                                        });
                            } else
                            {
                                Toast.makeText(getContext(), "Unknown user type.", Toast.LENGTH_SHORT).show();
                            }
                        } else
                        {
                            Toast.makeText(getContext(), "User document does not exist.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e ->
                    {
                        if(isAdded())
                            Toast.makeText(getContext(), "Failed to retrieve user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else
        {
            Toast.makeText(getContext(), "No user is currently signed in.", Toast.LENGTH_SHORT).show();
            // Handle UI for non-logged-in state (e.g., navigate to login)
        }
        return view;
    }

    /**
     * Sets up the search bar functionality.
     * Initializes click listener to expand the search bar and query text listeners
     * to call {@link #filterResults(String)} when text is submitted or changed.
     */
    private void setUpSearchBar()
    {
        searchBar.setOnClickListener(v -> searchBar.setIconified(false)); // Expand on click
        searchBar.setOnQueryTextListener(new SearchView.OnQueryTextListener()
        {
            @Override
            public boolean onQueryTextSubmit(String query)
            {
                filterResults(query);
                return false; // Let SearchView handle default actions like clearing focus
            }

            @Override
            public boolean onQueryTextChange(String newText)
            {
                filterResults(newText);
                return false;
            }
        });
    }

    /**
     * Binds data from the provided {@link Restaurant} object to the UI views.
     * If the {@code restaurant} is null or {@link #getContext()} is null, the method returns early.
     * Sets text for name and address using {@link #getSafeText(String)}.
     * Sets the rating text to "Rating: X.X" or "Rating: N/A" and sets a click listener on {@link #rateButton}
     * to call {@link #showRatingDialog()}.
     * Sets business hours text to "Hours: [hours]" or "Hours: N/A".
     * Sets contact info text to "Contact: [formatted_info]" using {@link #formatContactInfo(Map)}.
     * Sets reservable status text to "Reservable: Yes/No". If reservable and user type is "user",
     * the {@link #openReservationOverlayButton} is made visible and its click listener is set to call
     * {@link #setupReservationOverlay()}; otherwise, the button is hidden.
     * Sets type text to "Type: [type]" or "Type: N/A".
     * Sets tags text to "Tags: [formatted_tags]" using {@link #formatList(List)}.
     * Sets price level text to "Price Level: [$$$]" using {@link #formatPriceLevel(int)}.
     * Sets description text to "Description: [description]" or "Description: N/A".
     * Loads the restaurant's image into {@link #restaurantDetailImage} using Glide. If the image URL
     * is null or empty, it loads a placeholder drawable ({@code R.drawable.image_placeholder}).
     * It also uses a placeholder and error drawable during loading.
     * Finally, calls {@link #loadMenuData()} to fetch and display the restaurant's menu.
     *
     * @param restaurant The {@link Restaurant} object whose data will be displayed. If null, the method returns early.
     */
    private void bindRestaurantData(Restaurant restaurant)
    {
        if(restaurant == null || getContext() == null)
        {
            Log.e("RestaurantInfo", "Cannot bind data: restaurant or context is null.");
            return;
        }

        // Name & Address
        restaurantDetailName.setText(getSafeText(restaurant.getName()));
        restaurantDetailAddress.setText(getSafeText(restaurant.getAddress()));

        // Rating
        double rating = restaurant.getAverageRating();
        restaurantDetailRating.setText(rating > 0 ? "Rating: " + String.format(Locale.getDefault(), "%.1f", rating) : "Rating: N/A");
        rateButton.setOnClickListener(v -> showRatingDialog()); // Setup rating dialog listener

        // Business Hours
        restaurantDetailBusinessHours.setText("Hours: " +
                getSafeText(restaurant.getBusinessHours() != null ? restaurant.getBusinessHours().toString() : null));

        // Contact Info (formatted without {})
        restaurantDetailContactInfo.setText("Contact: " + formatContactInfo(restaurant.getContactInfo()));

        // Reservable
        boolean isReservable = restaurant.isReservable(); // Use local var for clarity
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("FeedMe", Context.MODE_PRIVATE);
        String userType = sharedPreferences.getString("userType", "user");

        restaurantDetailReservable.setText("Reservable: " + (isReservable ? "Yes" : "No"));
        if(isReservable && "user".equals(userType))
        {
            openReservationOverlayButton.setVisibility(View.VISIBLE);
            openReservationOverlayButton.setOnClickListener(v -> setupReservationOverlay());
        } else
        {
            openReservationOverlayButton.setVisibility(View.GONE);
        }
        // Type
        restaurantDetailType.setText("Type: " + getSafeText(restaurant.getType()));

        // Tags (comma-separated)
        restaurantDetailTags.setText("Tags: " + formatList(restaurant.getTags()));

        // Price Level ($, $$, etc.)
        restaurantDetailPriceLevel.setText("Price Level: " + formatPriceLevel(restaurant.getPriceLevel()));

        // Description (added)
        restaurantDetailDescription.setText("Description: " + getSafeText(restaurant.getDescription()));

        // Image
        String imageUrl = restaurant.getImageURL();
        Glide.with(requireContext())
                .load(imageUrl != null && !imageUrl.isEmpty() ? imageUrl : R.drawable.image_placeholder) // Load placeholder if URL is bad or empty
                .placeholder(R.drawable.image_placeholder)
                .error(R.drawable.image_placeholder)
                .into(restaurantDetailImage);

        loadMenuData(); // Load menus for this restaurant
    }

    /**
     * Helper method to return the provided text or the string "N/A" if the text is null or empty.
     *
     * @param text The text to check.
     * @return The original text if it's not null or empty; otherwise, returns "N/A".
     */
    private String getSafeText(String text)
    {
        return (text != null && !text.isEmpty()) ? text : "N/A";
    }

    /**
     * Helper method to format a list of strings into a single comma-separated string.
     * Returns "N/A" if the list is null or empty.
     *
     * @param list The list of strings to format.
     * @return A comma-separated string of the list items, or "N/A" if the list is null or empty.
     */
    private String formatList(List<String> list)
    {
        if(list == null || list.isEmpty()) return "N/A";
        return TextUtils.join(", ", list);
    }

    /**
     * Helper method to format a numeric price level into a string of dollar signs (e.g., 1 -> "$", 3 -> "$$$").
     * Returns "N/A" if the price level is zero or negative.
     *
     * @param priceLevel The numeric price level.
     * @return A string representation of the price level (e.g., "$", "$$", "N/A").
     */
    private String formatPriceLevel(int priceLevel)
    {
        if(priceLevel <= 0) return "N/A";
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < priceLevel; i++)
        {
            sb.append("$");
        }
        return sb.toString();
    }

    /**
     * Helper method to format a map of contact information into a displayable string.
     * Each key-value pair is formatted as "Key: Value" on a new line.
     * Keys are capitalized using {@link #capitalizeFirst(String)}.
     * If a value in the map is null, "N/A" is used for that value.
     * Returns "N/A" if the input map is null or empty.
     * The final string has trailing whitespace trimmed.
     *
     * @param contactInfo A map where keys are contact types (e.g., "phone", "email") and values are the contact details.
     * @return A formatted string of contact information, with each entry on a new line, or "N/A" if the map is null or empty.
     */
    private String formatContactInfo(Map<String, String> contactInfo)
    {
        if(contactInfo == null || contactInfo.isEmpty()) return "N/A";

        StringBuilder sb = new StringBuilder();
        for(Map.Entry<String, String> entry : contactInfo.entrySet())
        {
            String key = capitalizeFirst(entry.getKey());
            String value = entry.getValue() != null ? entry.getValue() : "N/A";
            sb.append(key).append(": ").append(value).append("\n");
        }
        return sb.toString().trim(); // Trim trailing newline
    }

    /**
     * Helper method to capitalize the first letter of a string.
     *
     * @param str The string to capitalize.
     * @return The capitalized string, or the original string if null or empty.
     */
    private String capitalizeFirst(String str)
    {
        if(str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase(Locale.getDefault()) + str.substring(1);
    }


    /**
     * Loads all menus for the current {@link #restaurantID} from Firestore, ordered by {@code menuIndex}.
     * Updates the local {@link #menuList} and notifies the {@link #menuAdapter}.
     * Shows the search bar if menus exist. Calls {@link #loadAllMenuItems()} to fetch items.
     * Manages a loading indicator.
     */
    private void loadMenuData()
    {
        if(restaurantID == null || restaurantID.isEmpty())
        {
            if(getContext() != null)
                Toast.makeText(getContext(), "Invalid restaurant ID for loading menu.", Toast.LENGTH_SHORT).show();
            Log.e("RestaurantInfo", "restaurantID is null or empty in loadMenuData.");
            showLoading(false); // Ensure loading is hidden if we exit early
            searchBar.setVisibility(View.GONE); // Hide search bar if no ID
            return;
        }

        searchBar.setVisibility(View.GONE); // Hide initially
        showLoading(true);

        db.collection("Restaurants").document(restaurantID)
                .collection("Menus")
                .orderBy("menuIndex")
                .get()
                .addOnSuccessListener(snapshot ->
                {
                    if(!isAdded()) return; // Check fragment state
                    List<Menu> updatedMenus = new ArrayList<>();
                    for(QueryDocumentSnapshot doc : snapshot)
                    {
                        Menu menu = doc.toObject(Menu.class);
                        menu.setMenuID(doc.getId()); // Ensure ID is set
                        updatedMenus.add(menu);
                    }

                    menuList.clear();
                    menuList.addAll(updatedMenus);

                    if(!menuList.isEmpty())
                    {
                        searchBar.setVisibility(View.VISIBLE); // Show search bar if menus exist
                    }

                    if(menuAdapter != null)
                    {
                        menuAdapter.clearFiltering(); // Will notify adapter
                    }

                    loadAllMenuItems(); // Load items after menus are loaded
                    // showLoading(false) will be called after loadAllMenuItems completes or by its own logic
                })
                .addOnFailureListener(e ->
                {
                    if(isAdded())
                    {
                        showLoading(false);
                        Toast.makeText(getContext(), "Failed to load menus: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("RestaurantInfo", "Failed to load menus.", e);
                    }
                });
    }

    /**
     * Sets up the menu items display.
     * Initializes the {@link #menuAdapter} with click listeners for menus (no action defined here)
     * and menu items (navigates to {@link MenuItemFragment} using {@link MenuItemSelectionViewModel}).
     * Sets the adapter to the {@link #recyclerViewMenus} and calls {@link #setUpSearchBar()}.
     *
     * @param restaurantID The ID of the restaurant whose menu items are to be displayed.
     */
    private void setupMenuItems(String restaurantID)
    {
        if(getContext() == null || getActivity() == null)
        {
            Log.e("RestaurantInfo", "Context or Activity is null in setupMenuItems.");
            return;
        }
        MenuItemSelectionViewModel menuItemSelectionViewModel = new ViewModelProvider(requireActivity()).get(MenuItemSelectionViewModel.class);

        // Determine containerID based on user type (already done in onCreateView, but can be re-checked or passed)
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("FeedMe", Context.MODE_PRIVATE);
        String userType = sharedPreferences.getString("userType", "user");
        containerID = "restaurant".equals(userType) ? R.id.fragment_container : R.id.fragmentContainer;


        menuAdapter = new MenuAdapter(
                menuList, // Initial empty list, will be populated by loadMenuData
                menu ->
                {
                    // Menu click (header of the menu in adapter) - no action currently defined here
                    Log.d("RestaurantInfo", "Menu header clicked: " + menu.getName());
                },
                item ->
                { // Click on an individual item within a menu
                    Log.d("RestaurantInfo", "Menu item clicked: " + item.getName());
                    menuItemSelectionViewModel.selectMenuItem(item);

                    MenuItemFragment menuItemFragment = new MenuItemFragment();
                    if(getActivity() != null)
                    {
                        getActivity().getSupportFragmentManager().beginTransaction()
                                .replace(containerID, menuItemFragment)
                                .addToBackStack(null) // Allow back navigation to this info screen
                                .commit();
                    }
                },
                restaurantID
        );

        recyclerViewMenus.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewMenus.setAdapter(menuAdapter);
        setUpSearchBar(); // Setup search after adapter is ready
    }

    /**
     * Loads all menu items for all currently loaded menus from Firestore.
     * For each menu in {@link #menuList}, it fetches its items (ordered by {@code orderIndex}),
     * adds them to the global {@link #menuItemList}, and stores them in the {@link #menuAdapter} cache.
     * Once all items for all menus are loaded (or failed to load), it reapplies any active search filter
     * or notifies the adapter to refresh if no search is active.
     * Manages a loading indicator.
     */
    private void loadAllMenuItems()
    {
        if(menuAdapter == null || restaurantID == null || restaurantID.isEmpty())
        {
            Log.w("RestaurantInfo", "Cannot load all menu items: adapter or restaurantID is null/empty.");
            showLoading(false); // Ensure loading is hidden
            return;
        }

        menuItemList.clear(); // Clear global list before repopulating
        // showLoading(true) might have been called by loadMenuData

        if(menuList.isEmpty())
        {
            Log.d("RestaurantInfo", "No menus to load items from.");
            menuAdapter.notifyDataSetChanged(); // Refresh adapter (will show empty if no menus)
            showLoading(false);
            return;
        }

        AtomicInteger pendingMenuLoads = new AtomicInteger(menuList.size());
        final String currentSearchQuery = (searchBar != null && !TextUtils.isEmpty(searchBar.getQuery())) ? searchBar.getQuery().toString() : "";

        for(Menu menu : menuList)
        {
            if(menu.getMenuID() == null || menu.getMenuID().isEmpty())
            {
                Log.w("RestaurantInfo", "Skipping items load for menu with null/empty ID: " + menu.getName());
                if(pendingMenuLoads.decrementAndGet() == 0)
                {
                    finalizeMenuLoading(currentSearchQuery);
                }
                continue;
            }

            db.collection("Restaurants").document(restaurantID)
                    .collection("Menus").document(menu.getMenuID())
                    .collection("Items")
                    .orderBy("orderIndex")
                    .get()
                    .addOnSuccessListener(snapshot ->
                    {
                        if(!isAdded()) return;
                        List<MenuItem> itemsForThisMenu = new ArrayList<>();
                        for(QueryDocumentSnapshot doc : snapshot)
                        {
                            MenuItem item = doc.toObject(MenuItem.class);
                            item.setMenuID(menu.getMenuID());
                            item.setItemID(doc.getId());
                            itemsForThisMenu.add(item);
                            menuItemList.add(item); // Add to global list
                        }
                        menuAdapter.setMenuItems(menu.getMenuID(), itemsForThisMenu);

                        if(pendingMenuLoads.decrementAndGet() == 0)
                        {
                            finalizeMenuLoading(currentSearchQuery);
                        }
                    })
                    .addOnFailureListener(e ->
                    {
                        if(isAdded())
                            Log.e("RestaurantInfo", "Error loading items for menu " + menu.getName() + ": " + e.getMessage(), e);
                        if(pendingMenuLoads.decrementAndGet() == 0)
                        {
                            finalizeMenuLoading(currentSearchQuery);
                        }
                    });
        }
    }

    /**
     * Helper method called after all menu items (or attempts to load them) are complete.
     * It applies search filters if a query is active, or refreshes the adapter.
     * Hides the loading indicator.
     *
     * @param currentSearchQuery The active search query, or empty if none.
     */
    private void finalizeMenuLoading(String currentSearchQuery)
    {
        if(!isAdded() || menuAdapter == null) return;
        if(!currentSearchQuery.isEmpty())
        {
            filterResults(currentSearchQuery);
        } else
        {
            menuAdapter.notifyDataSetChanged(); // Refresh display
        }
        showLoading(false); // Hide loading indicator
    }


    /**
     * Filters the displayed menus and items based on the provided search query.
     * If the query is empty, it clears any existing filters in the {@link #menuAdapter}.
     * Otherwise, it searches for menus whose names contain the query and items (across all menus)
     * whose names contain the query. The results (sets of matching menu IDs) are passed to
     * {@link MenuAdapter#setFilterData(Set, Set)}.
     * Shows or hides the "No results" TextView based on whether any matches are found.
     *
     * @param query The search query string.
     */
    private void filterResults(String query)
    {
        if(menuAdapter == null)
        {
            Log.e("RestaurantInfo", "MenuAdapter is null in filterResults.");
            return;
        }

        if(TextUtils.isEmpty(query))
        {
            menuAdapter.clearFiltering(); // This calls notifyDataSetChanged
            noResults.setVisibility(View.GONE);
            recyclerViewMenus.setVisibility(View.VISIBLE);
        } else
        {
            String lowerQuery = query.toLowerCase(Locale.getDefault());
            Set<String> menuMatchIds = new HashSet<>();
            Set<String> itemMatchMenuIds = new HashSet<>();

            for(Menu menu : menuList)
            {
                if(menu.getName() != null && menu.getName().toLowerCase(Locale.getDefault()).contains(lowerQuery))
                {
                    menuMatchIds.add(menu.getMenuID());
                }
            }

            for(MenuItem item : menuItemList) // Use global list of all items
            {
                if(item.getName() != null && item.getName().toLowerCase(Locale.getDefault()).contains(lowerQuery))
                {
                    itemMatchMenuIds.add(item.getMenuID()); // Add parent menu ID if item matches
                }
            }

            menuAdapter.setCurrentSearchQuery(lowerQuery);
            menuAdapter.setFilterData(menuMatchIds, itemMatchMenuIds); // This calls notifyDataSetChanged

            if(menuAdapter.getItemCount() == 0) // Check after adapter updates
            {
                noResults.setVisibility(View.VISIBLE);
                recyclerViewMenus.setVisibility(View.GONE);
            } else
            {
                noResults.setVisibility(View.GONE);
                recyclerViewMenus.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * Sets up the reservation overlay UI.
     * Makes the overlay visible. Sets the business hours text in the overlay.
     * Clears previous input from guest amount and special requests fields.
     * Configures the {@link #datePicker} to allow selection from tomorrow up to one year ahead.
     * Configures the {@link #hourPicker} (0-23) and {@link #minutePicker} (00, 15, 30, 45)
     * with default values based on the current time, rounded up to the next 15-minute interval.
     */
    private void setupReservationOverlay()
    {
        reservationOverlay.setVisibility(View.VISIBLE);

        Restaurant currentRestaurant = (viewModel != null && viewModel.getCurrentRestaurant().getValue() != null)
                ? viewModel.getCurrentRestaurant().getValue()
                : null;
        String businessHours = (currentRestaurant != null && currentRestaurant.getBusinessHours() != null)
                ? currentRestaurant.getBusinessHours() : "Hours not available";
        reservationOverlayBusinessHours.setText(businessHours);

        guestAmountEditText.setText("");
        specialRequestsEditText.setText("");

        Calendar calendar = Calendar.getInstance();
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        int currentMinute = calendar.get(Calendar.MINUTE);

        // Round up minute to next 15-min interval for default selection
        int roundedMinuteInterval;
        if(currentMinute == 0) roundedMinuteInterval = 0;
        else if(currentMinute <= 15) roundedMinuteInterval = 1; // 15
        else if(currentMinute <= 30) roundedMinuteInterval = 2; // 30
        else if(currentMinute <= 45) roundedMinuteInterval = 3; // 45
        else
        { // currentMinute > 45
            roundedMinuteInterval = 0; // 00 of next hour
            currentHour = (currentHour + 1) % 24; // Increment hour, wrap around if needed
        }

        // DatePicker: min date tomorrow, max date one year from today
        Calendar minDate = Calendar.getInstance();
        minDate.add(Calendar.DAY_OF_YEAR, 1);
        datePicker.setMinDate(minDate.getTimeInMillis());

        Calendar maxDate = Calendar.getInstance();
        maxDate.add(Calendar.YEAR, 1);
        datePicker.setMaxDate(maxDate.getTimeInMillis());
        // Set default date to minDate (tomorrow)
        datePicker.updateDate(minDate.get(Calendar.YEAR), minDate.get(Calendar.MONTH), minDate.get(Calendar.DAY_OF_MONTH));


        hourPicker.setMinValue(0);
        hourPicker.setMaxValue(23);
        hourPicker.setValue(currentHour);

        final String[] minuteDisplayValues = {"00", "15", "30", "45"};
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(minuteDisplayValues.length - 1);
        minutePicker.setDisplayedValues(minuteDisplayValues);
        minutePicker.setValue(roundedMinuteInterval); // Index for {0, 15, 30, 45}
    }

    /**
     * Validates the reservation details entered by the user and, if valid, saves the reservation.
     * Checks for guest amount (must be >= 1 and not exceed restaurant's max capacity if set).
     * If max capacity is set, it queries existing reservations within a +/- 90-minute window
     * of the selected time to ensure total guests (existing + new) do not exceed capacity.
     * If all checks pass, calls {@link #saveReservation(Date, int, int, String)}.
     * Manages a loading indicator.
     *
     * @param restaurant The {@link Restaurant} object for which the reservation is being made.
     */
    private void validateReservationAndSave(Restaurant restaurant)
    {
        if(getContext() == null || currentUser == null || restaurant == null || restaurantID == null || restaurantID.isEmpty())
        {
            Toast.makeText(getContext(), "Error: Cannot process reservation.", Toast.LENGTH_SHORT).show();
            return;
        }
        showLoading(true);

        String guestAmountStr = guestAmountEditText.getText().toString().trim();
        if(guestAmountStr.isEmpty())
        {
            guestAmountEditText.setError("Please enter number of guests.");
            showLoading(false);
            return;
        }
        int guestAmount;
        try
        {
            guestAmount = Integer.parseInt(guestAmountStr);
        } catch(NumberFormatException e)
        {
            guestAmountEditText.setError("Invalid number of guests.");
            showLoading(false);
            return;
        }

        if(guestAmount < 1)
        {
            guestAmountEditText.setError("Number of guests must be at least 1.");
            showLoading(false);
            return;
        }

        int selectedYear = datePicker.getYear();
        int selectedMonth = datePicker.getMonth();
        int selectedDay = datePicker.getDayOfMonth();
        int selectedHour = hourPicker.getValue();
        int selectedMinute = minutePicker.getValue() * 15; // Convert index back to minute value

        Calendar selectedDateTimeCal = Calendar.getInstance();
        selectedDateTimeCal.set(selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute, 0);
        selectedDateTimeCal.set(Calendar.MILLISECOND, 0);
        Date reservationDateTime = selectedDateTimeCal.getTime();

        // Validate if selected time is in the past (considering only date part for simplicity here, could be more precise)
        Calendar todayCal = Calendar.getInstance();
        todayCal.set(Calendar.HOUR_OF_DAY, 0);
        todayCal.set(Calendar.MINUTE, 0);
        todayCal.set(Calendar.SECOND, 0);
        todayCal.set(Calendar.MILLISECOND, 0);
        if(selectedDateTimeCal.before(todayCal) || (selectedDateTimeCal.equals(todayCal) && selectedDateTimeCal.getTimeInMillis() < System.currentTimeMillis()))
        {
            Toast.makeText(getContext(), "Cannot make a reservation for a past date or time.", Toast.LENGTH_LONG).show();
            showLoading(false);
            return;
        }


        int maxCapacity = restaurant.getMaxCapacity();
        if(maxCapacity == 0) // If maxCapacity is 0, assume no capacity limit check needed
        {
            saveReservation(reservationDateTime, selectedHour, selectedMinute, guestAmountStr);
        } else
        {
            if(guestAmount > maxCapacity)
            {
                guestAmountEditText.setError("Exceeds restaurant's max capacity (" + maxCapacity + ").");
                showLoading(false);
                return;
            }

            Calendar windowStart = (Calendar) selectedDateTimeCal.clone();
            windowStart.add(Calendar.MINUTE, -90);
            Calendar windowEnd = (Calendar) selectedDateTimeCal.clone();
            windowEnd.add(Calendar.MINUTE, 90);

            db.collection("Restaurants").document(restaurantID).collection("Reservations")
                    .whereGreaterThanOrEqualTo("date", windowStart.getTime())
                    .whereLessThanOrEqualTo("date", windowEnd.getTime())
                    .get()
                    .addOnSuccessListener(querySnapshot ->
                    {
                        if(!isAdded()) return;
                        int totalGuestsInWindow = 0;
                        for(DocumentSnapshot doc : querySnapshot.getDocuments())
                        {
                            if(!"Cancelled".equalsIgnoreCase(doc.getString("status"))) // Check status field name
                            {
                                String existingGuestStr = doc.getString("guests");
                                if(existingGuestStr != null)
                                {
                                    try
                                    {
                                        totalGuestsInWindow += Integer.parseInt(existingGuestStr);
                                    } catch(NumberFormatException e)
                                    {
                                        Log.w("ReservationValidation", "Invalid guest number in existing reservation: " + existingGuestStr);
                                    }
                                }
                            }
                        }

                        if(totalGuestsInWindow + guestAmount > maxCapacity)
                        {
                            guestAmountEditText.setError("Not enough capacity available at this time slot.");
                            showLoading(false);
                        } else
                        {
                            saveReservation(reservationDateTime, selectedHour, selectedMinute, guestAmountStr);
                        }
                    })
                    .addOnFailureListener(e ->
                    {
                        if(isAdded())
                        {
                            Log.e("ReservationValidation", "Error fetching existing reservations.", e);
                            Toast.makeText(getContext(), "Could not verify availability. Please try again.", Toast.LENGTH_SHORT).show();
                            showLoading(false);
                        }
                    });
        }
    }

    /**
     * Saves the reservation data to Firestore for both the user and the restaurant.
     * Creates a unique reservation ID. Constructs a map of reservation data including date, time,
     * guests, special requests, restaurant ID, user ID, and restaurant name.
     * First, it saves this data to the user's "Reservations" subcollection.
     * On success, it fetches the user's name and phone number from their main document,
     * adds/updates these in the reservation data map (removing restaurantName as it's not needed for restaurant's copy),
     * and then saves the modified map to the restaurant's "Reservations" subcollection.
     * Shows toasts for success or logs errors on failure. Hides loading indicator and reservation overlay on success.
     *
     * @param reservationDate The {@link Date} object for the reservation.
     * @param hour            The selected hour for the reservation.
     * @param minute          The selected minute for the reservation.
     * @param guestAmountStr  The number of guests as a string.
     */
    private void saveReservation(Date reservationDate, int hour, int minute, String guestAmountStr)
    {
        if(currentUser == null || restaurantID == null || getContext() == null)
        {
            Log.e("SaveReservation", "Cannot save: critical data missing (user, restaurantID, or context).");
            showLoading(false);
            return;
        }

        String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
        String newReservationID = UUID.randomUUID().toString(); // Use local var for clarity
        String currentUserID = currentUser.getUid();
        String currentRestaurantName = (viewModel != null && viewModel.getCurrentRestaurant().getValue() != null)
                ? viewModel.getCurrentRestaurant().getValue().getName() : "Unknown Restaurant";

        Map<String, Object> reservationDataForUser = new HashMap<>();
        reservationDataForUser.put("date", reservationDate);
        reservationDataForUser.put("time", formattedTime);
        reservationDataForUser.put("guests", guestAmountStr);
        reservationDataForUser.put("specialRequests", specialRequestsEditText.getText().toString().trim());
        reservationDataForUser.put("restaurantID", restaurantID);
        reservationDataForUser.put("reservationID", newReservationID);
        reservationDataForUser.put("userID", currentUserID);
        reservationDataForUser.put("restaurantName", currentRestaurantName); // For user's copy
        // reservationDataForUser.put("status", "Upcoming"); // Implicitly upcoming

        // Save to user's reservations
        db.collection("Users").document(currentUserID)
                .collection("Reservations").document(newReservationID)
                .set(reservationDataForUser)
                .addOnSuccessListener(docRefUser ->
                {
                    if(!isAdded()) return;
                    Log.d("ReservationSave", "Reservation saved to user's collection.");
                    // Now prepare data for restaurant's collection
                    db.collection("Users").document(currentUserID).get()
                            .addOnSuccessListener(userDocSnapshot ->
                            {
                                if(!isAdded()) return;
                                Map<String, Object> reservationDataForRestaurant = new HashMap<>(reservationDataForUser);
                                reservationDataForRestaurant.remove("restaurantName"); // Not needed for restaurant's copy

                                if(userDocSnapshot.exists())
                                {
                                    String userName = userDocSnapshot.getString("name");
                                    String userPhone = userDocSnapshot.getString("phoneNumber");
                                    if(userName != null)
                                        reservationDataForRestaurant.put("name", userName); // User's name for restaurant
                                    if(userPhone != null)
                                        reservationDataForRestaurant.put("phoneNumber", userPhone); // User's phone
                                }

                                db.collection("Restaurants").document(restaurantID)
                                        .collection("Reservations").document(newReservationID)
                                        .set(reservationDataForRestaurant)
                                        .addOnSuccessListener(docRefRestaurant ->
                                        {
                                            if(!isAdded()) return;
                                            Log.d("ReservationSave", "Reservation saved to restaurant's collection.");
                                            showLoading(false);
                                            reservationOverlay.setVisibility(View.GONE);
                                            Toast.makeText(getContext(), "Reservation successfully created!", Toast.LENGTH_LONG).show();
                                        })
                                        .addOnFailureListener(eRestaurant ->
                                        {
                                            if(isAdded())
                                                Log.e("ReservationSave", "Error saving reservation to restaurant's collection.", eRestaurant);
                                            // Consider rollback or notification if this part fails
                                            showLoading(false);
                                        });
                            })
                            .addOnFailureListener(eUserDoc ->
                            {
                                if(isAdded())
                                    Log.e("ReservationSave", "Failed to fetch user info for restaurant's reservation copy.", eUserDoc);
                                // Still try to save to restaurant with available info, or handle error
                                // For simplicity, current code doesn't explicitly handle this failure path for restaurant save.
                                showLoading(false);
                            });
                })
                .addOnFailureListener(eUser ->
                {
                    if(isAdded())
                    {
                        Log.e("ReservationSave", "Error saving reservation to user's collection.", eUser);
                        Toast.makeText(getContext(), "Failed to create reservation. Please try again.", Toast.LENGTH_SHORT).show();
                        showLoading(false);
                    }
                });
    }

    /**
     * Opens an external navigation application to provide directions to the given address.
     * It creates a "geo" URI intent and uses {@link Intent#createChooser(Intent, CharSequence)}
     * to allow the user to select their preferred navigation app.
     * If no app can handle the intent, a toast message is displayed.
     *
     * @param address The destination address string.
     */
    private void openNavigationApp(String address)
    {
        if(getContext() == null || address == null || address.isEmpty())
        {
            if(getContext() != null)
                Toast.makeText(getContext(), "Address not available for navigation.", Toast.LENGTH_SHORT).show();
            return;
        }
        String uri = "geo:0,0?q=" + Uri.encode(address);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        Intent chooserIntent = Intent.createChooser(intent, "Choose a Navigation App");

        if(chooserIntent.resolveActivity(requireContext().getPackageManager()) != null)
        {
            startActivity(chooserIntent);
        } else
        {
            Toast.makeText(requireContext(), "No navigation apps available on this device.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Toggles the visibility of the main progress bar.
     *
     * @param isLoading True to show the progress bar, false to hide it.
     */
    private void showLoading(boolean isLoading)
    {
        if(progressBar != null) // Check if progressBar is initialized
        {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Displays an {@link AlertDialog} for the user to rate the currently viewed restaurant.
     * The dialog, inflated from {@code R.layout.dialog_rate_restaurant}, contains a {@link RatingBar},
     * a submit button, and a cancel button.
     * The {@link RatingBar}'s progress tint, secondary progress tint, and indeterminate tint
     * are all set to a gold color defined in {@code R.color.gold}.
     * <p>
     * Pre-conditions for showing the dialog: {@link #getContext()}, {@link #currentUser},
     * {@link #restaurantID} must not be null, and {@code restaurantID} must not be empty.
     * If these conditions are not met, a toast message "Cannot rate: User or restaurant information missing."
     * is shown, and the method returns.
     * <p>
     * On submit:
     * <ul>
     *     <li>If the rating from the {@code RatingBar} is 0 stars, a toast "Please select at least 1 star" is shown.</li>
     *     <li>Otherwise, the user's rating for this restaurant is fetched from Firestore
     *         (from "Restaurants/{restaurantID}/Ratings/{userID}").</li>
     *     <li>A new rating data map (with the new rating value and a server timestamp) is created.</li>
     *     <li>This new rating data is set (or overwritten) in the user's rating document.</li>
     *     <li>On successful save of the user's rating, the main restaurant document is fetched.</li>
     *     <li>The restaurant's {@code averageRating} and {@code ratingsCount} are recalculated based on whether
     *         this is a new rating from the user or an update to their previous rating.</li>
     *     <li>The restaurant document is updated with the new average rating and ratings count.</li>
     *     <li>On successful update of the restaurant's aggregate rating, a toast "Rating submitted" is shown,
     *         the dialog is dismissed, and the {@link #restaurantDetailRating} TextView is updated with the new average.
     *         The format is "Rating: X.X" or "Rating: N/A".</li>
     *     <li>Various failure scenarios (fetching previous rating, submitting rating, fetching restaurant data,
     *         updating summary) result in appropriate toast messages.</li>
     * </ul>
     * The cancel button dismisses the dialog.
     * The {@link #showLoading(boolean)} method is called to manage a loading indicator during the submission process,
     * though its implementation is not shown in this specific method's context in the provided snippet.
     */
    private void showRatingDialog()
    {
        if(getContext() == null || currentUser == null || restaurantID == null || restaurantID.isEmpty())
        {
            Toast.makeText(getContext(), "Cannot rate: User or restaurant information missing.", Toast.LENGTH_SHORT).show();
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_rate_restaurant, null);

        RatingBar ratingBar = dialogView.findViewById(R.id.ratingBar);
        Button submitButton = dialogView.findViewById(R.id.submitRatingButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelRatingButton);

        ratingBar.setProgressTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.gold)));
        ratingBar.setSecondaryProgressTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.gold)));
        ratingBar.setIndeterminateTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.gold)));
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        submitButton.setOnClickListener(v ->
        {
            int rating = (int) ratingBar.getRating();
            if(rating == 0)
            {
                Toast.makeText(requireContext(), "Please select at least 1 star", Toast.LENGTH_SHORT).show();
                return;
            }
            showLoading(true); // Assuming showLoading is defined elsewhere

            DocumentReference restaurantRef = db.collection("Restaurants").document(restaurantID);
            DocumentReference userRatingRef = restaurantRef.collection("Ratings").document(currentUser.getUid());

            userRatingRef.get().addOnSuccessListener(docSnapshot ->
            {
                Integer oldRating;
                if(docSnapshot.exists() && docSnapshot.getLong("rating") != null)
                {
                    oldRating = Objects.requireNonNull(docSnapshot.getLong("rating")).intValue();
                } else
                {
                    oldRating = null;
                }

                Map<String, Object> ratingData = new HashMap<>();
                ratingData.put("rating", rating);
                ratingData.put("timestamp", FieldValue.serverTimestamp());

                userRatingRef.set(ratingData).addOnSuccessListener(aVoid ->
                        restaurantRef.get().addOnSuccessListener(restaurantSnapshot ->
                        {
                            if(!isAdded()) return;
                            double oldAverage = restaurantSnapshot.getDouble("averageRating") != null
                                    ? restaurantSnapshot.getDouble("averageRating")
                                    : 0.0;
                            long oldCount = restaurantSnapshot.getLong("ratingsCount") != null
                                    ? restaurantSnapshot.getLong("ratingsCount")
                                    : 0;

                            double newAverage;
                            long newCount = oldCount;

                            if(oldRating == null)
                            {
                                // New rating
                                newAverage = (oldAverage * oldCount + rating) / (oldCount + 1);
                                newCount = oldCount + 1;
                            } else
                            {
                                // Updated rating
                                if(oldCount > 0)
                                { // Ensure oldCount is not zero before division
                                    newAverage = (oldAverage * oldCount - oldRating + rating) / oldCount;
                                } else
                                { // Fallback if oldCount is zero but oldRating existed (should not happen)
                                    newAverage = rating;
                                    newCount = 1; // Correct the count
                                }
                            }

                            Map<String, Object> updateData = new HashMap<>();
                            updateData.put("averageRating", newAverage);
                            updateData.put("ratingsCount", newCount);

                            restaurantRef.update(updateData).addOnSuccessListener(updateVoid ->
                            {
                                Toast.makeText(requireContext(), "Rating submitted", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                                // Update Rating shown
                                restaurantDetailRating.setText(newAverage > 0 ? "Rating: " + String.format(Locale.getDefault(), "%.1f", newAverage) : "Rating: N/A");
                            }).addOnFailureListener(e ->
                            {
                                Toast.makeText(requireContext(), "Failed to update rating summary", Toast.LENGTH_SHORT).show();
                            }).addOnCompleteListener(task -> showLoading(false)); // Hide loading after attempt
                        }).addOnFailureListener(e ->
                        {
                            Toast.makeText(requireContext(), "Failed to fetch restaurant data", Toast.LENGTH_SHORT).show();
                            showLoading(false);
                        })).addOnFailureListener(e ->
                {
                    Toast.makeText(requireContext(), "Failed to submit rating", Toast.LENGTH_SHORT).show();
                    showLoading(false);
                });
            }).addOnFailureListener(e ->
            {
                Toast.makeText(requireContext(), "Failed to fetch your previous rating", Toast.LENGTH_SHORT).show();
                showLoading(false);
            });
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}