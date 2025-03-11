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
import com.example.restaurantapp.fragments.RestaurantDetailsFragment;
import com.example.restaurantapp.models.Restaurant;
import com.example.restaurantapp.viewmodels.RestaurantSelectionViewModel;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class DiscoveryFragment extends Fragment
{

    private RecyclerView restaurantRecyclerView;
    private RestaurantDiscoveryAdapter restaurantAdapter;
    private RestaurantSelectionViewModel viewModel;

    public DiscoveryFragment()
    {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_discovery, container, false);

        viewModel = new ViewModelProvider(requireActivity()).get(RestaurantSelectionViewModel.class);

        restaurantRecyclerView = view.findViewById(R.id.restaurantRecyclerView);
        restaurantRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        restaurantRecyclerView.setVisibility(View.VISIBLE);

        restaurantAdapter = new RestaurantDiscoveryAdapter(new ArrayList<>(), getContext(), restaurant ->
        {
            Log.d("DiscoveryFragment", "Item clicked: " + restaurant.getName());
            viewModel.selectRestaurant(restaurant);
            RestaurantDetailsFragment detailsFragment = new RestaurantDetailsFragment();
            if (getActivity() != null)
            {
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, detailsFragment)
                        .addToBackStack(null)
                        .commit();
                Log.d("DiscoveryFragment", "Navigated to RestaurantDetailsFragment");
            }
        });
        restaurantRecyclerView.setAdapter(restaurantAdapter);

        // Fetch all restaurants from Firestore
        fetchAllRestaurants();
        return view;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        fetchAllRestaurants();
    }

    private void fetchAllRestaurants()
    {
        try
        {
            Log.d("DiscoveryFragment", "Starting to fetch restaurants");

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("Restaurants")
                    .orderBy("name")
                    .get()
                    .addOnSuccessListener(querySnapshot ->
                    {
                        List<Restaurant> restaurantList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : querySnapshot)
                        {
                            Restaurant restaurant = document.toObject(Restaurant.class);
                            restaurantList.add(restaurant);
                        }
                        Log.d("DiscoveryFragment", "Fetched " + restaurantList.size() + " restaurants");
                        if (restaurantList.isEmpty())
                        {
                            Log.w("DiscoveryFragment", "No restaurants found in Firestore");
                            // Maybe show an empty state UI here
                        }
                        restaurantAdapter.updateData(restaurantList);
                    })
                    .addOnFailureListener(e ->
                    {
                        Log.e("Firestore", "Error fetching restaurants: ", e);
                        // Show error state UI here
                    });
        } catch (Exception e)
        {
            Log.e("DiscoveryFragment", "Exception in fetchAllRestaurants", e);
        }
    }
}
