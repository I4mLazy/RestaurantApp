package com.example.restaurantapp.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.restaurantapp.R;
import com.example.restaurantapp.adapters.MenusAdapter;
import com.example.restaurantapp.models.Menu;
import com.example.restaurantapp.models.Restaurant;
import com.example.restaurantapp.viewmodels.RestaurantSelectionViewModel;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class RestaurantInfoFragment extends Fragment
{

    private ImageView restaurantDetailImage;
    private TextView restaurantDetailName, restaurantDetailAddress, restaurantDetailRating,
            restaurantDetailBusinessHours, restaurantDetailContactInfo,
            restaurantDetailReservable, restaurantDetailType, restaurantDetailTags,
            restaurantDetailPriceLevel;
    // RecyclerView for menus
    private RecyclerView menusRecyclerView;
    private MenusAdapter menusAdapter;

    private RestaurantSelectionViewModel viewModel;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    public RestaurantInfoFragment()
    {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_restaurant_info, container, false);
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

        // Initialize the RecyclerView for menus
        menusRecyclerView = view.findViewById(R.id.menusRecyclerView);
        menusRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
        menusAdapter = new MenusAdapter(new ArrayList<>(), getContext(), menu ->
        {
            Log.d("RestaurantInfoFragment", "Menu clicked: " + menu.getName());
        });
        menusRecyclerView.setAdapter(menusAdapter);


        viewModel = new ViewModelProvider(requireActivity()).get(RestaurantSelectionViewModel.class);
        viewModel.getSelectedRestaurant().observe(getViewLifecycleOwner(), restaurant ->
        {
            if (restaurant != null)
            {
                bindRestaurantData(restaurant);
            }
        });
        return view;
    }

    private void bindRestaurantData(Restaurant restaurant)
    {
        if (restaurant == null) return;

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

        String imageUrl = restaurant.getImageUrl();
        if (imageUrl == null || imageUrl.isEmpty())
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
        fetchMenus(restaurant);
    }


    private void fetchMenus(Restaurant restaurant) {
        String restaurantId = restaurant.getRestaurantID();
        if (restaurantId == null || restaurantId.isEmpty()) {
            Log.e("RestaurantInfoFragment", "Restaurant ID is null or empty for restaurant: " + restaurant.getName());
            // Clear the adapter to avoid showing stale data.
            menusAdapter.updateData(new ArrayList<>());
            return;
        }

        DocumentReference restaurantRef = db.collection("Restaurants").document(restaurantId);
        menusAdapter.setRestaurantReference(restaurantRef);

        restaurantRef.collection("Menus").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<Menu> menus = new ArrayList<>();
                if (task.getResult() != null && !task.getResult().isEmpty()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Menu menu = document.toObject(Menu.class);
                        menus.add(menu);
                    }
                } else {
                    Log.w("RestaurantInfoFragment", "No menus found for restaurant: " + restaurant.getName());
                }
                menusAdapter.updateData(menus);
            } else {
                Log.e("RestaurantInfoFragment", "Error fetching menus: ", task.getException());
                menusAdapter.updateData(new ArrayList<>());
            }
        });
    }

}
