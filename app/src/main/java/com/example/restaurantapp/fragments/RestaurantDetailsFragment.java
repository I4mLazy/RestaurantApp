package com.example.restaurantapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.restaurantapp.R;
import com.example.restaurantapp.adapters.RestaurantDetailsPagerAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class RestaurantDetailsFragment extends Fragment
{

    private TabLayout tabLayout;
    private ViewPager2 viewPager;

    public RestaurantDetailsFragment()
    {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_restaurant_details, container, false);
        tabLayout = view.findViewById(R.id.tabLayout);
        viewPager = view.findViewById(R.id.viewPager);

        RestaurantDetailsPagerAdapter adapter = new RestaurantDetailsPagerAdapter(this);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) ->
        {
            if (position == 0)
            {
                tab.setText("Restaurant Info");
            } else
            {
                tab.setText("Ordering");
            }
        }).attach();

        return view;
    }
}
