package com.example.restaurantapp.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SearchView;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TimePicker;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class RestaurantInfoFragment extends Fragment
{

    private ImageView restaurantDetailImage;
    private TextView restaurantDetailName, restaurantDetailAddress, restaurantDetailRating,
            restaurantDetailBusinessHours, restaurantDetailContactInfo, restaurantDetailReservable,
            restaurantDetailType, restaurantDetailTags, restaurantDetailPriceLevel, noResults;
    private Button restaurantDetailEditButton, openReservationOverlayButton;
    private EditText guestAmountEditText, specialRequestsEditText;
    private MaterialButton cancelReservationButton, confirmReservationButton;
    private DatePicker datePicker;
    private NumberPicker hourPicker, minutePicker;
    private SearchView searchBar;
    private ProgressBar progressBar;
    private RecyclerView recyclerViewMenus;
    private FrameLayout reservationOverlay;
    private MenuAdapter menuAdapter;
    private List<Menu> menuList = new ArrayList<>();
    private List<MenuItem> menuItemList = new ArrayList<>();
    private String restaurantID;
    int containerID;

    private RestaurantViewModel viewModel;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;

    public RestaurantInfoFragment()
    {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_restaurant_info, container, false);

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
        noResults = view.findViewById(R.id.noResults);

        restaurantDetailEditButton = view.findViewById(R.id.restaurantDetailEditButton);
        openReservationOverlayButton = view.findViewById(R.id.openReserveOverlayButton);
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

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        if(currentUser != null)
        {
            db = FirebaseFirestore.getInstance();
            db.collection("Users").document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot ->
                    {
                        if(documentSnapshot.exists())
                        {
                            SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("FeedMe", Context.MODE_PRIVATE);

                            String userType = sharedPreferences.getString("userType", "user");
                            if("restaurant".equals(userType))
                            {
                                restaurantDetailEditButton.setVisibility(View.VISIBLE);
                                openReservationOverlayButton.setVisibility(View.GONE);
                                restaurantID = documentSnapshot.getString("restaurantID");

                                if(restaurantID != null && !restaurantID.isEmpty())
                                {
                                    // Fetch restaurant data directly
                                    db.collection("Restaurants").document(restaurantID).get()
                                            .addOnSuccessListener(restaurantSnapshot ->
                                            {
                                                if(restaurantSnapshot.exists())
                                                {
                                                    Restaurant restaurant = restaurantSnapshot.toObject(Restaurant.class);

                                                    // Bind restaurant data
                                                    bindRestaurantData(restaurant);
                                                    // Setup menu
                                                    setupMenuItems(restaurantID);

                                                    restaurantDetailEditButton.setOnClickListener(v ->
                                                    {

                                                        viewModel = new ViewModelProvider(requireActivity()).get(RestaurantViewModel.class);
                                                        viewModel.setCurrentRestaurant(restaurant);

                                                        EditRestaurantInfoFragment editRestaurantInfoFragment = new EditRestaurantInfoFragment();
                                                        getActivity().getSupportFragmentManager().beginTransaction()
                                                                .replace(containerID, editRestaurantInfoFragment)
                                                                .commit();

                                                    });
                                                } else
                                                {
                                                    Toast.makeText(getContext(), "Restaurant not found", Toast.LENGTH_SHORT).show();
                                                }
                                            })
                                            .addOnFailureListener(e ->
                                            {
                                                Toast.makeText(getContext(), "Failed to load restaurant: " + e.getMessage(),
                                                        Toast.LENGTH_SHORT).show();
                                            });
                                } else
                                {
                                    Toast.makeText(getContext(), "No restaurant assigned to user", Toast.LENGTH_SHORT).show();
                                }
                            } else if("user".equals(userType))
                            {
                                restaurantDetailEditButton.setVisibility(View.GONE);
                                openReservationOverlayButton.setVisibility(View.VISIBLE);
                                viewModel = new ViewModelProvider(requireActivity()).get(RestaurantViewModel.class);
                                viewModel.getCurrentRestaurant().observe(getViewLifecycleOwner(), restaurant ->
                                {
                                    if(restaurant != null)
                                    {
                                        restaurantID = Objects.requireNonNull(viewModel.getCurrentRestaurant().getValue()).getRestaurantID();
                                        // Bind restaurant data
                                        bindRestaurantData(restaurant);
                                        // Setup menu
                                        setupMenuItems(restaurantID);
                                        // Inside your overlay click listener
                                        openReservationOverlayButton.setOnClickListener(v ->
                                                setupReservationOverlay());


                                        cancelReservationButton.setOnClickListener(v ->
                                                reservationOverlay.setVisibility(View.GONE));

                                        confirmReservationButton.setOnClickListener(v ->
                                        {
                                            validateReservationAndSave(restaurant);
                                        });
                                    }
                                });


                                requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),
                                        new OnBackPressedCallback(true)
                                        {
                                            @Override
                                            public void handleOnBackPressed()
                                            {
                                                requireActivity().getSupportFragmentManager().popBackStack();
                                            }
                                        });
                            } else
                            {
                                Toast.makeText(getContext(), "User type doesn't exist", Toast.LENGTH_SHORT).show();
                            }
                        } else
                        {
                            Toast.makeText(getContext(), "User document doesn't exist", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e ->
                    {
                        Toast.makeText(getContext(), "Failed to retrieve user data: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        } else

        {
            Toast.makeText(getContext(), "No user is currently signed in", Toast.LENGTH_SHORT).show();
        }

        return view;
    }

    private void setupMenuItems(String restaurantID)
    {
        MenuItemSelectionViewModel menuItemSelectionViewModel = new ViewModelProvider(requireActivity())
                .get(MenuItemSelectionViewModel.class);
        db = FirebaseFirestore.getInstance();
        db.collection("Users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot ->
                {
                    if(documentSnapshot.exists())
                    {
                        String userType = documentSnapshot.getString("userType");
                        if("restaurant".equals(userType))
                        {
                            containerID = R.id.fragment_container;
                        } else if("user".equals(userType))
                        {
                            containerID = R.id.fragmentContainer;
                        }
                    }
                });

        menuAdapter = new MenuAdapter(
                menuList,
                menu ->
                {
                    // Menu click handler (empty)
                },
                item ->
                {
                    Log.d("RestaurantOverviewFragment", "Clicked on item: " + item.getName());

                    menuItemSelectionViewModel.selectMenuItem(item);

                    // Create new fragment instance and pass data if needed
                    MenuItemFragment menuItemFragment = new MenuItemFragment();
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(containerID, menuItemFragment)
                            .commit();
                },
                restaurantID
        );

        // Set adapter after initialization
        recyclerViewMenus.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewMenus.setAdapter(menuAdapter);
        setUpSearchBar();
    }


    private void setUpSearchBar()
    {
        searchBar.setOnClickListener(v -> searchBar.setIconified(false));
        searchBar.setOnQueryTextListener(new SearchView.OnQueryTextListener()
        {
            @Override
            public boolean onQueryTextSubmit(String query)
            {
                filterResults(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText)
            {
                filterResults(newText);
                return false;
            }
        });
    }

    private void bindRestaurantData(Restaurant restaurant)
    {
        if(restaurant == null) return;

        // Name & Address
        restaurantDetailName.setText(restaurant.getName() != null ? restaurant.getName() : "N/A");
        restaurantDetailAddress.setText(restaurant.getAddress() != null ? restaurant.getAddress() : "N/A");

        // Rating
        double rating = restaurant.getRating();
        restaurantDetailRating.setText(rating > 0 ? "Rating: " + rating : "Rating: N/A");

        // Business Hours
        restaurantDetailBusinessHours.setText("Hours: " +
                (restaurant.getBusinessHours() != null ? restaurant.getBusinessHours().toString() : "N/A"));

        // Contact Info
        restaurantDetailContactInfo.setText("Contact: " +
                (restaurant.getContactInfo() != null ? restaurant.getContactInfo().toString() : "N/A"));

        // Reservable
        restaurantDetailReservable.setText("Reservable: " + (restaurant.isReservable() ? "Yes" : "No"));

        // Type
        restaurantDetailType.setText("Type: " +
                (restaurant.getType() != null && !restaurant.getType().isEmpty() ? restaurant.getType() : "N/A"));

        // Tags
        restaurantDetailTags.setText("Tags: " +
                (restaurant.getTags() != null && !restaurant.getTags().isEmpty() ? restaurant.getTags().toString() : "N/A"));

        // Price Level
        int priceLevel = restaurant.getPriceLevel();
        restaurantDetailPriceLevel.setText("Price Level: " + (priceLevel > 0 ? priceLevel : "N/A"));

        // Image
        String imageUrl = restaurant.getImageURL();
        Glide.with(requireContext())
                .load(imageUrl != null && !imageUrl.isEmpty() ? imageUrl : R.drawable.image_placeholder)
                .placeholder(R.drawable.image_placeholder)
                .error(R.drawable.image_placeholder)
                .into(restaurantDetailImage);

        // Menu data
        loadMenuData();
    }


    private void loadMenuData()
    {
        if(restaurantID == null || restaurantID.isEmpty())
        {
            Toast.makeText(getContext(), "Invalid restaurant ID", Toast.LENGTH_SHORT).show();
            return;
        }

        // Hide search bar immediately to prevent flickering
        searchBar.setVisibility(View.GONE);

        showLoading(true);

        db.collection("Restaurants").document(restaurantID)
                .collection("Menus")
                .orderBy("menuIndex")
                .get()
                .addOnSuccessListener(snapshot ->
                {
                    List<Menu> updatedMenus = new ArrayList<>();

                    for(QueryDocumentSnapshot doc : snapshot)
                    {
                        Menu menu = doc.toObject(Menu.class);
                        menu.setMenuID(doc.getId());
                        updatedMenus.add(menu);
                    }

                    menuList.clear();
                    menuList.addAll(updatedMenus);

                    // Show search bar only if menus exist
                    if(!menuList.isEmpty())
                    {
                        searchBar.setVisibility(View.VISIBLE);
                    }

                    // Refresh adapter
                    if(menuAdapter != null)
                    {
                        menuAdapter.clearFiltering();
                    }

                    // Load all menu items for searching
                    loadAllMenuItems();
                    showLoading(false);
                })
                .addOnFailureListener(e ->
                {
                    showLoading(false);
                    Toast.makeText(getContext(), "Failed to load menus: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadAllMenuItems()
    {
        menuItemList.clear();

        // Track completed menu loads
        AtomicInteger pendingMenuLoads = new AtomicInteger(menuList.size());
        boolean hasActiveSearch = searchBar != null && !TextUtils.isEmpty(searchBar.getQuery());
        final String searchQuery = hasActiveSearch ? searchBar.getQuery().toString() : "";

        // If there are no menus, handle this case explicitly
        if(menuList.isEmpty())
        {
            // Notify adapter data changed or handle empty state
            menuAdapter.notifyDataSetChanged();
            return;
        }

        for(Menu menu : menuList)
        {
            db.collection("Restaurants").document(restaurantID)
                    .collection("Menus").document(menu.getMenuID())
                    .collection("Items")
                    .orderBy("orderIndex")
                    .get()
                    .addOnSuccessListener(snapshot ->
                    {
                        List<MenuItem> menuItems = new ArrayList<>();

                        for(QueryDocumentSnapshot doc : snapshot)
                        {
                            MenuItem item = doc.toObject(MenuItem.class);
                            item.setMenuID(menu.getMenuID());
                            menuItems.add(item);
                            menuItemList.add(item);
                        }

                        // Store the menu items in the adapter
                        menuAdapter.setMenuItems(menu.getMenuID(), menuItems);

                        // If this was the last menu to load
                        if(pendingMenuLoads.decrementAndGet() == 0)
                        {
                            if(hasActiveSearch)
                            {
                                filterResults(searchQuery);
                            } else
                            {
                                // Notify adapter to refresh display even without search
                                menuAdapter.notifyDataSetChanged();
                            }
                        }
                    })
                    .addOnFailureListener(e ->
                    {
                        Log.e("RestaurantInfoFragment", "Error loading items for menu " +
                                menu.getName() + ": " + e.getMessage());

                        // Still decrement and check if all loads (successful or not) are done
                        if(pendingMenuLoads.decrementAndGet() == 0)
                        {
                            if(hasActiveSearch)
                            {
                                filterResults(searchQuery);
                            } else
                            {
                                menuAdapter.notifyDataSetChanged();
                            }
                        }
                    });
        }
    }

    private void filterResults(String query)
    {
        if(TextUtils.isEmpty(query))
        {
            // Reset to full data if search is empty
            menuAdapter.clearFiltering();
            noResults.setVisibility(View.GONE);
            recyclerViewMenus.setVisibility(View.VISIBLE);
        } else
        {
            String lowerQuery = query.toLowerCase();
            Set<String> menuMatchIds = new HashSet<>();
            Set<String> itemMatchMenuIds = new HashSet<>();

            // First pass: Find menus whose names match the query
            for(Menu menu : menuList)
            {
                if(menu.getName().toLowerCase().contains(lowerQuery))
                {
                    menuMatchIds.add(menu.getMenuID());
                }
            }

            // Second pass: Find items whose names match the query
            for(MenuItem item : menuItemList)
            {
                if(item.getName().toLowerCase().contains(lowerQuery))
                {
                    itemMatchMenuIds.add(item.getMenuID());
                }
            }

            // Store search query and set filter data
            menuAdapter.setCurrentSearchQuery(lowerQuery);
            menuAdapter.setFilterData(menuMatchIds, itemMatchMenuIds);

            // Show a "No results" message if no matches found
            if(menuMatchIds.isEmpty() && itemMatchMenuIds.isEmpty())
            {
                // Display a "No results found" message
                noResults.setVisibility(View.VISIBLE);
                recyclerViewMenus.setVisibility(View.GONE);
            } else
            {
                // Hide the "no results" message
                noResults.setVisibility(View.GONE);
                recyclerViewMenus.setVisibility(View.VISIBLE);
            }
        }
    }

    private void setupReservationOverlay()
    {
        // Show the overlay
        reservationOverlay.setVisibility(View.VISIBLE);

        // Clear any previous input
        guestAmountEditText.setText("");  // Clear guest amount
        specialRequestsEditText.setText("");  // Clear special requests

        // Get current date and time
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        // Round up the minute to the next 15-minute interval
        if(minute > 45)
        {
            hour++;
            minute = 0;  // Round up to the next hour
        } else if(minute > 30)
        {
            minute = 45;
        } else if(minute > 15)
        {
            minute = 30;
        } else if(minute > 0)
        {
            minute = 15;
        }

        // Set up DatePicker to restrict dates to the next year
        Calendar minDate = Calendar.getInstance();
        minDate.add(Calendar.DAY_OF_YEAR, 1); // Set the minimum date to tomorrow (not today)

        Calendar maxDate = Calendar.getInstance();
        maxDate.add(Calendar.YEAR, 1); // Set the maximum date to one year from today

        datePicker.setMinDate(minDate.getTimeInMillis());
        datePicker.setMaxDate(maxDate.getTimeInMillis());

        // Set available hours (24-hour format)
        hourPicker.setMinValue(0);
        hourPicker.setMaxValue(23);
        hourPicker.setValue(hour);  // Set the current hour

        // Set available minutes (only 00, 15, 30, 45)
        int[] minuteValues = {0, 15, 30, 45};

        int minuteIndex = (minute / 15);  // Move to the next 15-minute block
        if(minuteIndex == minuteValues.length - 1)
        {
            minuteIndex = 0;  // If it's at 45, wrap around to 00
        }

        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(minuteValues.length - 1);  // Only 4 options
        minutePicker.setDisplayedValues(new String[]{"00", "15", "30", "45"});
        minutePicker.setValue(minuteIndex); // Set the current minute (rounded)

        // Ensure wrapping of minute values
        minutePicker.setOnValueChangedListener((picker, oldVal, newVal) ->
        {
            int newMinute = minuteValues[newVal];
            // Adjust the value if needed, but no other values should be shown
            if(newMinute == 0 && oldVal == 3)
            {
                hourPicker.setValue(hourPicker.getValue() + 1); // Wrap hour on 00
            }
        });
    }

    private void validateReservationAndSave(Restaurant restaurant)
    {
        showLoading(true);

        String guestAmountStr = guestAmountEditText.getText().toString().trim();
        if(guestAmountStr.isEmpty())
        {
            guestAmountEditText.setError("Please enter number of guests");
            showLoading(false);
            return;
        }

        int guestAmount = Integer.parseInt(guestAmountStr);
        if(guestAmount < 1)
        {
            guestAmountEditText.setError("Number of guests must be at least 1");
            showLoading(false);
            return;
        }

        // Get selected date and time
        int selectedYear = datePicker.getYear();
        int selectedMonth = datePicker.getMonth();
        int selectedDay = datePicker.getDayOfMonth();
        int selectedHour = hourPicker.getValue();
        int selectedMinute = minutePicker.getValue() * 15;

        Calendar selectedTime = Calendar.getInstance();
        selectedTime.set(selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute, 0);
        Date reservationDate = selectedTime.getTime();

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Step 1: Get restaurant's max capacity
        int maxCapacity = restaurant.getMaxCapacity();
        if(maxCapacity == 0)
        {
            saveReservation(reservationDate, selectedHour, selectedMinute, guestAmountStr);
        } else
        {
            if(guestAmount > maxCapacity)
            {
                guestAmountEditText.setError("Exceeds restaurant's max capacity (" + maxCapacity + ")");
                showLoading(false);
                return;
            }

            // Step 2: Calculate buffer window
            Calendar beforeWindow = (Calendar) selectedTime.clone();
            beforeWindow.add(Calendar.MINUTE, -90);

            Calendar afterWindow = (Calendar) selectedTime.clone();
            afterWindow.add(Calendar.MINUTE, 90);

            // Step 3: Fetch existing reservations in the window
            db.collection("Restaurants")
                    .document(restaurantID)
                    .collection("Reservations")
                    .whereGreaterThanOrEqualTo("date", beforeWindow.getTime())
                    .whereLessThanOrEqualTo("date", afterWindow.getTime())
                    .get()
                    .addOnSuccessListener(querySnapshot ->
                    {
                        int totalGuestsInWindow = 0;
                        for(DocumentSnapshot doc : querySnapshot.getDocuments())
                        {
                            if(!"Cancelled".equals(doc.getString("statue")))
                            {
                                String guestStr = doc.getString("guests");
                                try
                                {
                                    totalGuestsInWindow += Integer.parseInt(guestStr);
                                } catch(NumberFormatException e)
                                {
                                    Log.w("Validation", "Invalid guest number in reservation: " + guestStr);
                                }
                            }
                        }

                        if(totalGuestsInWindow + guestAmount > maxCapacity)
                        {
                            guestAmountEditText.setError("Not enough space at this time");
                            showLoading(false);
                            return;
                        }

                        //All checks passed — save reservation
                        saveReservation(reservationDate, selectedHour, selectedMinute, guestAmountStr);
                    });
        }
    }

    private void saveReservation(Date reservationDate, int hour, int minute, String guestAmountStr)
    {
        String formattedTime = String.format("%02d:%02d", hour, minute);
        String reservationID = UUID.randomUUID().toString();
        String userID = currentUser.getUid();

        Map<String, Object> reservationData = new HashMap<>();
        reservationData.put("date", reservationDate);
        reservationData.put("time", formattedTime);
        reservationData.put("guests", guestAmountStr);
        reservationData.put("specialRequests", specialRequestsEditText.getText().toString());
        reservationData.put("restaurantID", restaurantID);
        reservationData.put("reservationID", reservationID);
        reservationData.put("userID", userID);


        // Save to user's reservations
        db.collection("Users").document(currentUser.getUid())
                .collection("Reservations")
                .document(reservationID).set(reservationData)
                .addOnSuccessListener(docRef1 ->
                {
                    // Fetch name and phone from user's document
                    db.collection("Users").document(currentUser.getUid())
                            .get()
                            .addOnSuccessListener(documentSnapshot ->
                            {
                                if(documentSnapshot.exists())
                                {
                                    String name = documentSnapshot.getString("username");
                                    String phone = documentSnapshot.getString("phoneNumber");



                                    if(name != null) reservationData.put("userName", name);
                                    if(phone != null) reservationData.put("phoneNumber", phone);
                                }

                                // Save to restaurant's reservations
                                db.collection("Restaurants").document(restaurantID)
                                        .collection("Reservations")
                                        .document(reservationID).set(reservationData)
                                        .addOnSuccessListener(docRef2 ->
                                        {
                                            Log.d("Reservation", "Saved to both user and restaurant.");
                                            showLoading(false);
                                            reservationOverlay.setVisibility(View.GONE);
                                            Toast.makeText(getContext(), "Reservation successfully created!", Toast.LENGTH_SHORT).show();
                                        })
                                        .addOnFailureListener(e -> Log.e("Reservation", "Error saving to restaurant", e));
                            })
                            .addOnFailureListener(e -> Log.e("Reservation", "Failed to fetch user info", e));
                })
                .addOnFailureListener(e -> Log.e("Reservation", "Error saving to user", e));
    }


    private void showLoading(boolean isLoading)
    {
        if(progressBar != null)
        {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
    }
}
