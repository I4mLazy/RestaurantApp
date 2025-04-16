package com.example.restaurantapp.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.restaurantapp.fragments.OrderingFragment;
import com.example.restaurantapp.fragments.UselessFragment;

public class RestaurantDetailsPagerAdapter extends FragmentStateAdapter
{

    public RestaurantDetailsPagerAdapter(@NonNull Fragment fragment)
    {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position)
    {
        if (position == 0)
        {
            return new UselessFragment();
        } else
        {
            return new OrderingFragment();
        }
    }

    @Override
    public int getItemCount()
    {
        return 2;
    }


}
