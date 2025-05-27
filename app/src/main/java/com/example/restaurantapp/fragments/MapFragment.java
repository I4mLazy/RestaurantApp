package com.example.restaurantapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment; // Standard Fragment import

import com.example.restaurantapp.R;

/**
 * A simple {@link Fragment} subclass that likely serves as a container or placeholder
 * for map-related functionality.
 * Currently, it only inflates a layout named {@code fragment_map}.
 * Further map initialization and interaction logic would typically be added here
 * or in a child fragment if this acts as a container.
 */
public class MapFragment extends Fragment
{

    /**
     * Required empty public constructor for Fragment instantiation.
     * Android framework sometimes needs to re-instantiate fragments (e.g., on configuration changes),
     * and a public no-argument constructor is necessary for this process.
     */
    public MapFragment()
    {
        // Required empty public constructor
    }

    /**
     * Called to have the fragment instantiate its user interface view.
     * This method inflates the layout defined in {@code R.layout.fragment_map}.
     * This layout presumably contains the UI elements for displaying a map,
     * such as a {@code <fragment>} tag for a {@code SupportMapFragment} or a custom map view.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate
     *                           any views in the fragment.
     * @param container          If non-null, this is the parent view that the fragment's
     *                           UI should be attached to. The fragment should not add the view itself,
     *                           but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     *                           from a previous saved state as given here.
     * @return Return the View for the fragment's UI, which is the inflated
     * layout from {@code R.layout.fragment_map}.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_map, container, false);
    }
}