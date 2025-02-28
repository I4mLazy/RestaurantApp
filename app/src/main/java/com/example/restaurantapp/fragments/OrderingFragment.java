package com.example.restaurantapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.restaurantapp.R;

public class OrderingFragment extends Fragment
{

    public OrderingFragment()
    {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        // Inflate a layout for the ordering screen placeholder
        return inflater.inflate(R.layout.fragment_ordering, container, false);
    }
}
