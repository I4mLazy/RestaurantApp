package com.example.restaurantapp.fragments;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.restaurantapp.BuildConfig;
import com.example.restaurantapp.R;
import com.example.restaurantapp.adapters.RestaurantSearchResultsAdapter;
import com.example.restaurantapp.models.Restaurant;
import com.example.restaurantapp.viewmodels.RestaurantViewModel;
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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
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
import com.google.firebase.firestore.GeoPoint;
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
    private RestaurantViewModel viewModel;
    private boolean isNavigating = false;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable mapLoadRunnable;
    private PlacesClient placesClient;
    private Marker currentSearchMarker;

    public GmapsFragment()
    {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {

        View view = inflater.inflate(R.layout.fragment_gmaps, container, false);

        bottomSheet = view.findViewById(R.id.bottomSheetContainer);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        bottomSheetBehavior.setPeekHeight(1000);
        bottomSheetBehavior.setHideable(true);

        return view;
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
        // Remove the delayed runnable if the fragment is destroyed before the delay finishes
        handler.removeCallbacks(mapLoadRunnable);
        if(placesClient != null)
        {
            placesClient = null;
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        noResultsTextView = view.findViewById(R.id.noResultsTextView);
        recyclerView = view.findViewById(R.id.searchResultsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Create adapter with listener
        viewModel = new ViewModelProvider(requireActivity()).get(RestaurantViewModel.class);
        restaurantSearchResultsAdapter = new RestaurantSearchResultsAdapter(
                new ArrayList<>(),
                requireContext(),
                new RestaurantSearchResultsAdapter.OnItemClickListener()
                {
                    @Override
                    public void onItemClick(Restaurant restaurant)
                    {
                        Log.d("GmapsFragment", "Item clicked: " + restaurant.getName());
                        viewModel.setCurrentRestaurant(restaurant);
                        RestaurantInfoFragment restaurantInfoFragment = new RestaurantInfoFragment();
                        if(getActivity() != null)
                        {
                            getActivity().getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.fragmentContainer, restaurantInfoFragment)
                                    .addToBackStack(null)
                                    .commit();
                            Log.d("GmapsFragment", "Navigated to RestaurantDetailsFragment");
                        }
                    }

                    @Override
                    public void onNavigateClick(Restaurant restaurant)
                    {
                        if(restaurant.getLocation() == null || restaurant.getAddress() == null)
                        {
                            Log.e("GmapsFragment", "Restaurant location is null, cannot navigate.");
                            return;  // If restaurant location is null, return early
                        }

                        if(!isNavigating)
                        {
                            isNavigating = true;
                            // Open the navigation app
                            openNavigationApp(restaurant.getAddress());
                        } else
                        {
                            return;
                        }

                        // log the navigation action
                        Log.d("GmapsFragment", "Navigation to: " + restaurant.getAddress());

                        // Re-enable the button after a short delay (500ms)
                        new Handler(Looper.getMainLooper()).postDelayed(() -> isNavigating = false, 500);
                    }
                }
        );

        recyclerView.setAdapter(restaurantSearchResultsAdapter);

        searchView = view.findViewById(R.id.searchBar);
        searchView.setOnClickListener(v -> searchView.setIconified(false));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener()
        {
            @Override
            public boolean onQueryTextSubmit(String query)
            {
                performSearch(query);
                InputMethodManager imm = (InputMethodManager) requireContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                if(imm != null)
                {
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText)
            {
                if(newText.isEmpty())
                {
                    recyclerView.setVisibility(View.GONE);
                    noResultsTextView.setVisibility(View.GONE);
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                }

                if(currentSearchMarker != null)
                {
                    currentSearchMarker.remove();
                    currentSearchMarker = null;
                }
                // updateSearchSuggestions(newText);
                return true;
            }
        });

        String apiKey = BuildConfig.MAPS_API_KEY;
        Places.initializeWithNewPlacesApiEnabled(requireActivity().getApplicationContext(), apiKey);
        placesClient = Places.createClient(requireContext());

        mapLoadRunnable = new Runnable()
        {
            @Override
            public void run()
            {
                loadMap(placesClient);
            }
        };
        handler.postDelayed(mapLoadRunnable, 500);
    }

    private void performSearch(String query)
    {
        if(!query.isEmpty())
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
                        for(QueryDocumentSnapshot document : querySnapshot)
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
        if(results.isEmpty())
        {
            // Show no results text and hide the RecyclerView
            noResultsTextView.setVisibility(View.VISIBLE);
            noResultsTextView.setText("No results found");
            recyclerView.setVisibility(View.GONE);
        } else
        {
            // Update the RecyclerView with new data and show it
            restaurantSearchResultsAdapter.updateData(results);
            noResultsTextView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);

            // Show marker for first result
            Restaurant first = results.get(0);
            GeoPoint geoPoint = first.getLocation();
            if(geoPoint != null && mMap != null)
            {
                LatLng latLng = new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude());

                if(currentSearchMarker != null)
                {
                    currentSearchMarker.remove();
                }

                currentSearchMarker = mMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title(first.getName()));

                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));
            }
        }

        // Show the bottom sheet (it will be visible regardless of results)
        bottomSheet.setVisibility(View.VISIBLE);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }


    private void updateSearchSuggestions(String newText)
    {
        if(newText.isEmpty())
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
                    for(DocumentSnapshot document : queryDocumentSnapshots)
                    {
                        Restaurant restaurant = document.toObject(Restaurant.class);
                        filteredList.add(restaurant);
                    }
                    if(filteredList.isEmpty())
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
        if(ContextCompat.checkSelfPermission(requireActivity().getApplicationContext(), ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            Log.d("PlacesDebug", "Before findCurrentPlace");
            Task<FindCurrentPlaceResponse> placeResponse = placesClient.findCurrentPlace(request);
            Log.d("PlacesDebug", "After findCurrentPlace");
            placeResponse.addOnCompleteListener(task ->
            {
                if(task.isSuccessful())
                {
                    FindCurrentPlaceResponse response = task.getResult();
                    for(PlaceLikelihood placeLikelihood : response.getPlaceLikelihoods())
                    {
                        Log.i("CurrentLocation", String.format("Place '%s' has likelihood: %f", placeLikelihood.getPlace().getDisplayName(),
                                placeLikelihood.getLikelihood()));
                    }
                } else
                {
                    Exception exception = task.getException();
                    if(exception instanceof ApiException)
                    {
                        ApiException apiException = (ApiException) exception;
                        Log.e("NotFound", "Place not found: " + apiException.getStatusCode());
                    }
                }
            });
        } else
        {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private final ActivityResultLauncher<String> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted ->
            {
                if(isGranted)
                {
                    Log.d("Permission", "Location permission granted.");
                    setCurrentLocation(placesClient); // make sure this is accessible
                } else
                {
                    Log.w("Permission", "Location permission denied.");
                }
            });


    private void loadMap(PlacesClient placesClient)
    {
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.mapContainer);

        // If the map fragment doesn't exist, create it
        if(mapFragment == null)
        {
            final SupportMapFragment newMapFragment = SupportMapFragment.newInstance(
                    new GoogleMapOptions().mapId(getResources().getString(R.string.map_id)));

            FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
            if(!getChildFragmentManager().isStateSaved())
            {
                transaction.replace(R.id.mapContainer, newMapFragment);
                transaction.commit();
            }

            newMapFragment.getMapAsync(new OnMapReadyCallback()
            {
                @Override
                public void onMapReady(@NonNull GoogleMap googleMap)
                {
                    mMap = googleMap;
                    setMapSettings(mMap, placesClient);
                    moveMapButtons(newMapFragment);
                }
            });
        }
    }

    private void setMapSettings(GoogleMap mMap, PlacesClient placesClient)
    {
        setCurrentLocation(placesClient);
        setCurrentCameraPosition();
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            mMap.setMyLocationEnabled(true);
        }
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setRotateGesturesEnabled(true);
    }

    private void moveMapButtons(SupportMapFragment mapFragment)
    {
        View mapView = mapFragment.getView();
        if(mapView == null)
        {
            Log.w("MapsFragment", "MapView is null in moveMapButtons");
            return;
        }

        // Move MyLocation button
        View locationButton = mapView.findViewById(Integer.parseInt("2"));
        if(locationButton != null && locationButton.getLayoutParams() instanceof RelativeLayout.LayoutParams)
        {
            RelativeLayout.LayoutParams locationParams = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();
            locationParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
            locationParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
            locationParams.setMargins(0, 0, dpToPx(16), dpToPx(16));  // Right 16dp, Bottom 16dp
            locationButton.setLayoutParams(locationParams);
            locationButton.setScaleX(1.2f);
            locationButton.setScaleY(1.2f);
        } else
        {
            Log.w("MapsFragment", "MyLocation button not found or wrong layout params (id=2)");
        }

        // Move Zoom controls
        View zoomControls = mapView.findViewById(Integer.parseInt("1"));
        if(zoomControls != null && zoomControls.getLayoutParams() instanceof RelativeLayout.LayoutParams)
        {
            RelativeLayout.LayoutParams zoomParams = (RelativeLayout.LayoutParams) zoomControls.getLayoutParams();
            zoomParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
            zoomParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
            zoomParams.setMargins(0, 0, dpToPx(16), dpToPx(96));  // Slightly higher than MyLocation
            zoomControls.setLayoutParams(zoomParams);
            zoomControls.setScaleX(1.2f);
            zoomControls.setScaleY(1.2f);
        } else
        {
            Log.w("MapsFragment", "Zoom controls button not found or wrong layout params (id=1)");
        }

        // Move Compass button lower (~80dp from top)
        View compassButton = mapView.findViewById(Integer.parseInt("5"));
        if(compassButton != null && compassButton.getLayoutParams() instanceof RelativeLayout.LayoutParams)
        {
            RelativeLayout.LayoutParams compassParams = (RelativeLayout.LayoutParams) compassButton.getLayoutParams();
            compassParams.setMargins(0, dpToPx(80), dpToPx(16), 0);  // Top margin 80dp, Right 16dp
            compassButton.setLayoutParams(compassParams);
            compassButton.setScaleX(1.2f);
            compassButton.setScaleY(1.2f);
        } else
        {
            Log.w("MapsFragment", "Compass button not found or wrong layout params (id=5)");
        }
    }

    private int dpToPx(int dp)
    {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void setCurrentCameraPosition()
    {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity().getApplicationContext());
        if(ActivityCompat.checkSelfPermission(requireActivity().getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(requireActivity().getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            CurrentLocationRequest currentLocationRequest = new CurrentLocationRequest.Builder()
                    .setDurationMillis(5000)
                    .setMaxUpdateAgeMillis(0)
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    .build();

            Task<Location> locationTask = fusedLocationClient.getCurrentLocation(currentLocationRequest, null);
            locationTask.addOnCompleteListener(task ->
            {
                if(task.isSuccessful())
                {
                    Location location = task.getResult();
                    if(location != null)
                    {
                        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        focusOnLocation(mMap, currentLatLng);
                    }
                }
            });
        }
    }

    private void focusOnLocation(GoogleMap mMap, LatLng latLng)
    {
        if(mMap != null && latLng != null)
        {
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(latLng)
                    .zoom(14)
                    .build();
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }

    // Helper to launch navigation
    private void openNavigationApp(String address)
    {
        // The general geo URI that can be handled by multiple apps (including Maps and other navigation apps)
        String uri = "geo:0,0?q=" + Uri.encode(address);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));

        // Create chooser for available navigation apps
        Intent chooserIntent = Intent.createChooser(intent, "Choose a Navigation App");

        // Check if there's any app that can handle the intent
        if(intent.resolveActivity(requireContext().getPackageManager()) != null)
        {
            startActivity(chooserIntent);  // This will show a prompt with apps like Google Maps, Waze, etc.
        } else
        {
            // Fallback for no apps available
            Toast.makeText(requireContext(), "No navigation apps available", Toast.LENGTH_SHORT).show();
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