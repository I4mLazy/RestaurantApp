package com.example.restaurantapp.fragments;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.restaurantapp.BuildConfig;
import com.example.restaurantapp.R;
import com.example.restaurantapp.adapters.RestaurantSearchResultsAdapter;
import com.example.restaurantapp.models.Restaurant;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.PlaceLikelihood;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.net.FindCurrentPlaceResponse;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GmapsFragment extends Fragment
{
    private SearchView searchView;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private TextView noResultsTextView;
    private RestaurantSearchResultsAdapter restaurantSearchResultsAdapter;
    private RecyclerView recyclerView;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private View bottomSheet;
    private View bottomSheetContainer;

    public GmapsFragment()
    {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        String apiKey = BuildConfig.MAPS_API_KEY;
        Places.initializeWithNewPlacesApiEnabled(requireActivity().getApplicationContext(), apiKey);
        PlacesClient placesClient = Places.createClient(requireContext());

        View view = inflater.inflate(R.layout.fragment_gmaps, container, false);

        bottomSheet = view.findViewById(R.id.bottomSheetContainer);
        bottomSheetContainer = view.findViewById(R.id.bottomSheetContainer);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetContainer);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        bottomSheetBehavior.setPeekHeight(1000);
        bottomSheetBehavior.setHideable(true);

        SupportMapFragment supportMapFragment = SupportMapFragment.newInstance(
                new GoogleMapOptions().mapId(getResources().getString(R.string.map_id)));
        getChildFragmentManager().beginTransaction()
                .replace(R.id.mapContainer, supportMapFragment).commit();
        supportMapFragment.getMapAsync(new OnMapReadyCallback()
        {
            @Override
            public void onMapReady(@NonNull GoogleMap googleMap)
            {
                mMap = googleMap;
                setMapSettings(mMap, placesClient);
            }
        });

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        noResultsTextView = view.findViewById(R.id.noResultsTextView);
        recyclerView = view.findViewById(R.id.searchResultsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        restaurantSearchResultsAdapter = new RestaurantSearchResultsAdapter(new ArrayList<>(), requireActivity().getApplicationContext());
        recyclerView.setAdapter(restaurantSearchResultsAdapter);

        searchView = view.findViewById(R.id.searchBar);
        searchView.setOnClickListener(v -> searchView.setIconified(false));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener()
        {
            @Override
            public boolean onQueryTextSubmit(String query)
            {
                performSearch(query);
                InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null)
                {
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText)
            {
                //updateSearchSuggestions(newText);
                return true;
            }
        });
    }

    private void performSearch(String query)
    {
        if (!query.isEmpty())
        {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("Restaurants")
                    .orderBy("name")
                    .startAt(query)
                    .endAt(query + "\uf8ff")
                    .get()
                    .addOnSuccessListener(querySnapshot ->
                    {
                        List<Restaurant> results = new ArrayList<>();
                        for (QueryDocumentSnapshot document : querySnapshot)
                        {
                            Restaurant restaurant = document.toObject(Restaurant.class);
                            results.add(restaurant);
                        }
                        updateSearchResults(results);
                    })
                    .addOnFailureListener(e ->
                    {
                        Log.e("SearchError", "Error performing search: ", e);
                    });
        }
    }

    private void updateSearchResults(List<Restaurant> results)
    {
        if (results.isEmpty())
        {
            noResultsTextView.setVisibility(View.VISIBLE);
            noResultsTextView.setText("No results found");
            recyclerView.setVisibility(View.GONE);
            bottomSheet.setVisibility(View.VISIBLE);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else
        {
            noResultsTextView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            restaurantSearchResultsAdapter.updateData(results);
            bottomSheet.setVisibility(View.VISIBLE);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }
    }

    private void updateSearchSuggestions(String newText)
    {
        if (newText.isEmpty())
        {
            recyclerView.setVisibility(View.GONE);
            noResultsTextView.setVisibility(View.GONE);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            return;
        }
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Restaurants")
                .orderBy("name")
                .startAt(newText)
                .endAt(newText + "\uf8ff")
                .limit(10)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots ->
                {
                    List<Restaurant> filteredList = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots)
                    {
                        Restaurant restaurant = document.toObject(Restaurant.class);
                        filteredList.add(restaurant);
                    }
                    if (filteredList.isEmpty())
                    {
                        recyclerView.setVisibility(View.GONE);
                        noResultsTextView.setVisibility(View.VISIBLE);
                        bottomSheet.setVisibility(View.VISIBLE);
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                    } else
                    {
                        recyclerView.setVisibility(View.VISIBLE);
                        noResultsTextView.setVisibility(View.GONE);
                        restaurantSearchResultsAdapter.updateData(filteredList);
                        bottomSheet.setVisibility(View.VISIBLE);
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    }
                })
                .addOnFailureListener(e ->
                        Log.e("Search", "Error fetching search results", e));
    }

    private void setCurrentLocation(PlacesClient placesClient)
    {
        List<Place.Field> placeFields = Collections.singletonList(Place.Field.DISPLAY_NAME);
        Log.d("PlacesDebug", "Before FindCurrentPlaceRequest");
        FindCurrentPlaceRequest request = FindCurrentPlaceRequest.newInstance(placeFields);
        Log.d("PlacesDebug", "After FindCurrentPlaceRequest");
        if (ContextCompat.checkSelfPermission(requireActivity().getApplicationContext(), ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            Log.d("PlacesDebug", "Before findCurrentPlace");
            Task<FindCurrentPlaceResponse> placeResponse = placesClient.findCurrentPlace(request);
            Log.d("PlacesDebug", "After findCurrentPlace");
            placeResponse.addOnCompleteListener(task ->
            {
                if (task.isSuccessful())
                {
                    FindCurrentPlaceResponse response = task.getResult();
                    for (PlaceLikelihood placeLikelihood : response.getPlaceLikelihoods())
                    {
                        Log.i("CurrentLocation", String.format("Place '%s' has likelihood: %f", placeLikelihood.getPlace().getDisplayName(),
                                placeLikelihood.getLikelihood()));
                    }
                } else
                {
                    Exception exception = task.getException();
                    if (exception instanceof ApiException)
                    {
                        ApiException apiException = (ApiException) exception;
                        Log.e("NotFound", "Place not found: " + apiException.getStatusCode());
                    }
                }
            });
        } else
        {
            // Request permissions if needed
        }
    }

    private void setMapSettings(GoogleMap mMap, PlacesClient placesClient)
    {
        setCurrentLocation(placesClient);
        setCurrentCameraPosition();
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            mMap.setMyLocationEnabled(true);
        }
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setRotateGesturesEnabled(true);
    }

    private void setCurrentCameraPosition()
    {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity().getApplicationContext());
        if (ActivityCompat.checkSelfPermission(requireActivity().getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(requireActivity().getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            CurrentLocationRequest currentLocationRequest = new CurrentLocationRequest.Builder()
                    .setDurationMillis(5000)
                    .setMaxUpdateAgeMillis(0)
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    .build();

            Task<Location> locationTask = fusedLocationClient.getCurrentLocation(currentLocationRequest, null);
            locationTask.addOnCompleteListener(task ->
            {
                if (task.isSuccessful())
                {
                    Location location = task.getResult();
                    if (location != null)
                    {
                        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        focusOnLocation(mMap, currentLatLng);
                    }
                }
            });
            return;
        }
    }

    private void focusOnLocation(GoogleMap mMap, LatLng latLng)
    {
        if (mMap != null && latLng != null)
        {
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(latLng)
                    .zoom(14)
                    .build();
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }
//private void setSearchNearby(PlacesClient placesClient)
    //{
    //    fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity().getApplicationContext());
    //    if (ActivityCompat.checkSelfPermission(requireActivity().getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(requireActivity().getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    //    {
    //        fusedLocationClient.getLastLocation()
    //                .addOnSuccessListener(requireActivity(), location ->
    //                {
    //                    if (location != null)
    //                    {
    //                        LatLng center = new LatLng(location.getLatitude(), location.getLongitude());
    //                        final List<Place.Field> placeFields = Arrays.asList(Place.Field.ID, Place.Field.DISPLAY_NAME);
    //                        CircularBounds circle = CircularBounds.newInstance(center, /* radius = */ 5000);
    //                        final List<String> includedTypes = Arrays.asList("restaurant", "cafe");
    //                        final SearchNearbyRequest searchNearbyRequest =
    //                                SearchNearbyRequest.builder(/* location restriction = */ circle, placeFields)
    //                                        .setIncludedTypes(includedTypes)
    //                                        .setMaxResultCount(10)
    //                                        .build();
    //                        placesClient.searchNearby(searchNearbyRequest)
    //                                .addOnSuccessListener(response ->
    //                                {
    //                                    List<Place> places = response.getPlaces();
    //                                    mMap.clear();
    //                                    for (Place place : places)
    //                                    {
    //                                        LatLng placeLatLng = place.getLocation();
    //                                        if (placeLatLng != null)
    //                                        {
    //                                            mMap.addMarker(new MarkerOptions()
    //                                                    .position(placeLatLng)
    //                                                  .title(place.getDisplayName()));
    //                                        }
    //                                }).addOnFailureListener(exception ->
    //                                    }
    //                                {
    //                                    // Handle error
    //                                    Log.e("SearchNearby", "Error searching nearby places", exception);
    //                                });
    //                    }
    //                }).addOnFailureListener(exception ->
    //                {
    //                    // Handle error
    //                    Log.e("GetLastLocation", "Error getting last location", exception);
    //                });
    //    }
    //}
}