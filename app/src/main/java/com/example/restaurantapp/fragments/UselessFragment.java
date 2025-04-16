package com.example.restaurantapp.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.restaurantapp.R;
import com.example.restaurantapp.adapters.MenuAdapter;
import com.example.restaurantapp.models.Menu;
import com.example.restaurantapp.models.MenuItem;
import com.example.restaurantapp.models.Restaurant;
import com.example.restaurantapp.viewmodels.MenuItemSelectionViewModel;
import com.example.restaurantapp.viewmodels.RestaurantViewModel;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class UselessFragment extends Fragment
{

    private ImageView restaurantDetailImage;
    private TextView restaurantDetailName, restaurantDetailAddress, restaurantDetailRating,
            restaurantDetailBusinessHours, restaurantDetailContactInfo, restaurantDetailReservable,
            restaurantDetailType, restaurantDetailTags, restaurantDetailPriceLevel, noResults;
    private SearchView searchBar;
    private ProgressBar progressBar;
    // RecyclerView for menus
    private RecyclerView recyclerViewMenus;
    private MenuAdapter menuAdapter;
    private List<Menu> menuList = new ArrayList<>();
    private List<MenuItem> menuItemList = new ArrayList<>();
    private String restaurantID;

    private RestaurantViewModel viewModel;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    public UselessFragment()
    {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_useless, container, false);
        restaurantDetailImage = view.findViewById(R.id.restaurantDetailImage);
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
        searchBar = view.findViewById(R.id.searchBar);
        progressBar = view.findViewById(R.id.progressBar);

        // Initialize the RecyclerView for menus
        recyclerViewMenus = view.findViewById(R.id.menusRecyclerView);
        recyclerViewMenus.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));

        viewModel = new ViewModelProvider(requireActivity()).get(RestaurantViewModel.class);
        viewModel.getCurrentRestaurant().observe(getViewLifecycleOwner(), restaurant ->
        {
            if(restaurant != null)
            {
                bindRestaurantData(restaurant);
            }
        });

        restaurantID = viewModel.getCurrentRestaurant().getValue().getRestaurantID();

        if(restaurantID != null && !restaurantID.isEmpty())
        {
            MenuItemSelectionViewModel menuItemSelectionViewModel = new ViewModelProvider(requireActivity()).get(MenuItemSelectionViewModel.class);

            menuAdapter = new MenuAdapter(menuList, menu ->
            {
            },
                    item ->
                    {
                        Log.d("RestaurantInfoFragment", "Clicked on item: " + item.getName());

                        menuItemSelectionViewModel.selectMenuItem(item);

                        // Create new fragment instance and pass data if needed
                        MenuItemFragment menuItemFragment = new MenuItemFragment();
                        getActivity().getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragmentContainer, menuItemFragment)
                                .addToBackStack(null)
                                .commit();
                    },
                    restaurantID
            );

            recyclerViewMenus.setAdapter(menuAdapter);
            loadMenuData();
        } else
        {
            Toast.makeText(getContext(), "No restaurant assigned to user", Toast.LENGTH_SHORT).show();
        }

        setUpSearchBar();

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),
                new OnBackPressedCallback(true)
                {
                    @Override
                    public void handleOnBackPressed()
                    {
                        requireActivity().getSupportFragmentManager().popBackStack();
                    }
                });
        return view;
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

        restaurantDetailName.setText(restaurant.getName() != null ? restaurant.getName() : "N/A");
        restaurantDetailAddress.setText(restaurant.getAddress() != null ? restaurant.getAddress() : "N/A");
        double rating = restaurant.getRating();
        restaurantDetailRating.setText("Rating: " + (rating > 0 ? rating : "N/A"));
        restaurantDetailBusinessHours.setText("Hours: " +
                (restaurant.getBusinessHours() != null ? restaurant.getBusinessHours().toString() : "N/A"));
        restaurantDetailContactInfo.setText("Contact: " +
                (restaurant.getContactInfo() != null ? restaurant.getContactInfo().toString() : "N/A"));
        restaurantDetailReservable.setText("Reservable: " + (restaurant.isReservable() ? "Yes" : "No"));
        restaurantDetailType.setText("Type: " +
                (restaurant.getType() != null && !restaurant.getType().isEmpty() ? restaurant.getType().toString() : "N/A"));
        restaurantDetailTags.setText("Tags: " +
                (restaurant.getTags() != null && !restaurant.getTags().isEmpty() ? restaurant.getTags().toString() : "N/A"));
        int priceLevel = restaurant.getPriceLevel();
        restaurantDetailPriceLevel.setText("Price Level: " + (priceLevel > 0 ? priceLevel : "N/A"));

        String imageUrl = restaurant.getImageURL();
        if(imageUrl == null || imageUrl.isEmpty())
        {
            Glide.with(requireContext())
                    .load(R.drawable.image_placeholder)
                    .into(restaurantDetailImage);
        } else
        {
            Glide.with(requireContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.image_placeholder)
                    .error(R.drawable.image_placeholder)
                    .into(restaurantDetailImage);
        }
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

            //Show a "No results" message if no matches found
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

    private void showLoading(boolean isLoading)
    {
        if(progressBar != null)
        {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
    }
}
