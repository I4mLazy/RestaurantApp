package com.example.restaurantapp.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.restaurantapp.R;
import com.example.restaurantapp.adapters.RestaurantDiscoveryAdapter;
import com.example.restaurantapp.models.Restaurant;
import com.example.restaurantapp.viewmodels.RestaurantViewModel;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link Fragment} subclass responsible for displaying a list of restaurants for discovery.
 * It fetches restaurant data from Firestore and displays it using a {@link RecyclerView}
 * with a {@link RestaurantDiscoveryAdapter}. Users can click on a restaurant to navigate
 * to its details page ({@link RestaurantInfoFragment}). The fragment uses a
 * {@link RestaurantViewModel} to share the selected restaurant's data.
 */
public class DiscoveryFragment extends Fragment
{

    /**
     * RecyclerView to display the list of restaurants.
     */
    private RecyclerView restaurantRecyclerView;
    /**
     * Adapter for the restaurant RecyclerView.
     */
    private RestaurantDiscoveryAdapter restaurantAdapter;
    /**
     * ViewModel for managing and sharing restaurant data.
     */
    private RestaurantViewModel viewModel;

    /**
     * Required empty public constructor for Fragment instantiation.
     */
    public DiscoveryFragment()
    {
        // Required empty public constructor
    }

    /**
     * Called to have the fragment instantiate its user interface view.
     * This method inflates the layout for the fragment, initializes the {@link RestaurantViewModel},
     * sets up the {@link RecyclerView} with a {@link LinearLayoutManager} and
     * the {@link RestaurantDiscoveryAdapter}. The adapter's item click listener
     * handles navigation to the {@link RestaurantInfoFragment} upon clicking a restaurant,
     * passing the selected restaurant data via the ViewModel.
     * It also initiates the first fetch of restaurant data by calling {@link #fetchAllRestaurants()}.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate
     *                           any views in the fragment.
     * @param container          If non-null, this is the parent view that the fragment's
     *                           UI should be attached to. The fragment should not add the view itself,
     *                           but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     *                           from a previous saved state as given here.
     * @return Return the View for the fragment's UI, or null.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_discovery, container, false);

        viewModel = new ViewModelProvider(requireActivity()).get(RestaurantViewModel.class);

        restaurantRecyclerView = view.findViewById(R.id.restaurantRecyclerView);
        restaurantRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        restaurantRecyclerView.setVisibility(View.VISIBLE); // Ensure RecyclerView is visible

        restaurantAdapter = new RestaurantDiscoveryAdapter(new ArrayList<>(), getContext(), restaurant ->
        {
            Log.d("DiscoveryFragment", "Item clicked: " + restaurant.getName());
            viewModel.setCurrentRestaurant(restaurant); // Set the selected restaurant in ViewModel
            RestaurantInfoFragment restaurantInfoFragment = new RestaurantInfoFragment();
            if(getActivity() != null)
            {
                // Navigate to RestaurantInfoFragment
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, restaurantInfoFragment)
                        .addToBackStack(null) // Add transaction to back stack
                        .commit();
                Log.d("DiscoveryFragment", "Navigated to RestaurantDetailsFragment"); // Note: Log message says RestaurantDetailsFragment, actual is RestaurantInfoFragment
            }
        });
        restaurantRecyclerView.setAdapter(restaurantAdapter);

        // Fetch all restaurants from Firestore
        fetchAllRestaurants();
        return view;
    }

    /**
     * Called when the fragment is visible to the user and actively running.
     * This lifecycle method is overridden to call {@link #fetchAllRestaurants()}
     * to ensure the list of restaurants is refreshed or re-fetched when the fragment
     * becomes active, for instance, when returning from another fragment.
     */
    @Override
    public void onResume()
    {
        super.onResume();
        fetchAllRestaurants(); // Refresh data when fragment resumes
    }

    /**
     * Fetches all restaurant documents from the "Restaurants" collection in Firestore,
     * ordered by name.
     * On successful retrieval, it converts each document to a {@link Restaurant} object,
     * populates a list, and updates the {@link #restaurantAdapter} with this new list.
     * Logs the number of fetched restaurants or a warning if no restaurants are found.
     * On failure, it logs an error. It also includes a general try-catch block for
     * unexpected exceptions during the fetch operation.
     */
    private void fetchAllRestaurants()
    {
        try
        {
            Log.d("DiscoveryFragment", "Starting to fetch restaurants");

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("Restaurants")
                    .orderBy("name") // Order restaurants by name
                    .get()
                    .addOnSuccessListener(querySnapshot ->
                    {
                        List<Restaurant> restaurantList = new ArrayList<>();
                        for(QueryDocumentSnapshot document : querySnapshot)
                        {
                            Restaurant restaurant = document.toObject(Restaurant.class);
                            restaurantList.add(restaurant);
                        }
                        Log.d("DiscoveryFragment", "Fetched " + restaurantList.size() + " restaurants");
                        if(restaurantList.isEmpty())
                        {
                            Log.w("DiscoveryFragment", "No restaurants found in Firestore");
                            // Consider showing an empty state UI here
                        }
                        restaurantAdapter.updateData(restaurantList); // Update adapter with fetched data
                    })
                    .addOnFailureListener(e ->
                    {
                        Log.e("Firestore", "Error fetching restaurants: ", e);
                        // Consider showing an error state UI here
                    });
        } catch(Exception e)
        {
            Log.e("DiscoveryFragment", "Exception in fetchAllRestaurants", e);
        }
    }
}