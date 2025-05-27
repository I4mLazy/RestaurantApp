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

/**
 * A {@link Fragment} subclass that serves as a container for displaying reservations
 * organized into tabs. It uses a {@link ViewPager2} to manage two child fragments:
 * {@link ReservationHistoryFragment} for past reservations and
 * {@link UpcomingReservationsFragment} for upcoming reservations.
 * A {@link TabLayout} is used for navigation between these two fragments.
 * By default, the "Upcoming" tab is selected.
 */
public class ReservationsTabLayoutFragment extends Fragment
{

    /**
     * TabLayout for displaying the "History" and "Upcoming" reservation tabs.
     */
    private TabLayout tabLayout;
    /**
     * ViewPager2 for swiping between the reservation history and upcoming reservations fragments.
     */
    private ViewPager2 viewPager;

    /**
     * Required empty public constructor for Fragment instantiation.
     */
    public ReservationsTabLayoutFragment()
    {
        // Required empty public constructor
    }

    /**
     * Called to have the fragment instantiate its user interface view.
     * This method inflates the layout {@code R.layout.fragment_reservations_tab_layout},
     * which contains a {@link TabLayout} and a {@link ViewPager2}.
     * It initializes instances of {@link ReservationHistoryFragment} and {@link UpcomingReservationsFragment}.
     * A {@link FragmentStateAdapter} is created and set to the {@code ViewPager2} to manage these two fragments.
     * A {@link TabLayoutMediator} is then used to link the {@code TabLayout} with the {@code ViewPager2},
     * setting the tab titles to "History" and "Upcoming" respectively.
     * Finally, it sets the current item of the {@code ViewPager2} to the "Upcoming" tab (index 1)
     * without a smooth scroll animation.
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
        View view = inflater.inflate(R.layout.fragment_reservations_tab_layout, container, false);

        tabLayout = view.findViewById(R.id.tabLayout);
        viewPager = view.findViewById(R.id.viewPager);

        // Create and initialize the fragments
        Fragment historyFragment = new ReservationHistoryFragment();
        Fragment upcomingFragment = new UpcomingReservationsFragment();

        // Set up ViewPager2 with a list of fragments
        viewPager.setAdapter(new FragmentStateAdapter(getChildFragmentManager(), getLifecycle())
        {
            /**
             * Returns the total number of items in the data set held by the adapter.
             * @return The total number of items in this adapter (2 for "History" and "Upcoming").
             */
            @Override
            public int getItemCount()
            {
                return 2; // Two tabs: history and upcoming
            }

            /**
             * Provide a new Fragment associated with the specified position.
             * @param position The position of the item within the adapter's data set.
             * @return A new Fragment instance. Returns {@code historyFragment} for position 0,
             *         and {@code upcomingFragment} for position 1.
             */
            @NonNull // Added @NonNull as createFragment should always return a Fragment
            @Override
            public Fragment createFragment(int position)
            {
                if(position == 0)
                {
                    return historyFragment;
                } else // position == 1
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
            } else // position == 1
            {
                tab.setText("Upcoming");
            }
        }).attach(); // Attaches the mediator to sync tab selection and view pager scrolling

        // Set the default tab to "Upcoming" (index 1) without smooth scroll
        viewPager.setCurrentItem(1, false);

        return view;
    }
}