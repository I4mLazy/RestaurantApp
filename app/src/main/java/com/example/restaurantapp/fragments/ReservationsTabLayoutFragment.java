package com.example.restaurantapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.restaurantapp.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class ReservationsTabLayoutFragment extends Fragment
{

    private TabLayout tabLayout;
    private ViewPager2 viewPager;

    public ReservationsTabLayoutFragment()
    {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_reservations_tab_layout, container, false);

        tabLayout = view.findViewById(R.id.tabLayout);
        viewPager = view.findViewById(R.id.viewPager);

        // Create and initialize the fragments
        Fragment historyFragment = new ReservationHistoryFragment();
        Fragment upcomingFragment = new UpcomingReservationsFragment();

        // Set up ViewPager2 with a list of fragments
        viewPager.setAdapter(new FragmentStateAdapter(getChildFragmentManager(), getLifecycle())
        {
            @Override
            public int getItemCount()
            {
                return 2; // Two tabs, history and upcoming
            }

            @Override
            public Fragment createFragment(int position)
            {
                if(position == 0)
                {
                    return historyFragment;
                } else
                {
                    return upcomingFragment;
                }
            }
        });

        // Attach the TabLayout and ViewPager2 together
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) ->
        {
            if(position == 0)
            {
                tab.setText("History");
            } else
            {
                tab.setText("Upcoming");
            }
        }).attach();

        viewPager.setCurrentItem(1, false);

        return view;
    }
}
