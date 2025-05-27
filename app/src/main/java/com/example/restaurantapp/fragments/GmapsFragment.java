package com.example.restaurantapp.fragments;

import static android.Manifest.permission.ACCESS_FINE_LOCATION; // Static import for permission string

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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

import com.example.restaurantapp.BuildConfig; // Note: BuildConfig is imported but not used in the provided code.
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A {@link Fragment} subclass that displays a Google Map for discovering restaurants.
 * It allows users to search for restaurants, view results in a bottom sheet RecyclerView,
 * and see restaurant locations on the map. Users can click on search results to view
 * restaurant details or navigate to a restaurant using an external navigation app.
 * The fragment handles location permissions, fetches the user's current location,
 * and initializes the Google Places API.
 */
public class GmapsFragment extends Fragment
{
    /**
     * SearchView for users to input search queries for restaurants.
     */
    private SearchView searchView;
    /**
     * The GoogleMap object used to display the map and markers.
     */
    private GoogleMap mMap;
    /**
     * Client for accessing Google Play services location APIs.
     */
    private FusedLocationProviderClient fusedLocationClient;
    /**
     * TextView displayed when no search results are found.
     */
    private TextView noResultsTextView;
    /**
     * Adapter for the RecyclerView displaying restaurant search results.
     */
    private RestaurantSearchResultsAdapter restaurantSearchResultsAdapter;
    /**
     * RecyclerView to display search results in the bottom sheet.
     */
    private RecyclerView recyclerView;
    /**
     * Behavior controller for the bottom sheet.
     */
    private BottomSheetBehavior<View> bottomSheetBehavior;
    /**
     * The root View of the bottom sheet.
     */
    private View bottomSheet;
    /**
     * ViewModel for managing and sharing restaurant data.
     */
    private RestaurantViewModel viewModel;
    /**
     * Flag to prevent multiple rapid clicks on the navigation button.
     */
    private boolean isNavigating = false;
    /**
     * Handler for posting delayed runnables on the main looper.
     */
    private Handler handler = new Handler(Looper.getMainLooper());
    /**
     * Runnable for delaying the map loading process.
     */
    private Runnable mapLoadRunnable;
    /**
     * Client for interacting with the Google Places API.
     */
    private PlacesClient placesClient;
    /**
     * Marker for the currently searched location or first search result.
     */
    private Marker currentSearchMarker;

    /**
     * Required empty public constructor for Fragment instantiation.
     */
    public GmapsFragment()
    {
        // Required empty public constructor
    }

    /**
     * Called to have the fragment instantiate its user interface view.
     * Inflates the layout for the fragment and initializes the bottom sheet behavior,
     * setting its initial state to hidden.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate
     *                           any views in the fragment.
     * @param container          If non-null, this is the parent view that the fragment's
     *                           UI should be attached to. The fragment should not add the view itself,
     *                           but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     *                           from a previous saved state as given here.
     * @return Return the View for the fragment's UI, or null.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_gmaps, container, false);

        bottomSheet = view.findViewById(R.id.bottomSheetContainer);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN); // Initially hidden
        bottomSheetBehavior.setPeekHeight(1000); // Peek height when collapsed
        bottomSheetBehavior.setHideable(true);   // Allows hiding the bottom sheet

        return view;
    }

    /**
     * Called when the view previously created by {@link #onCreateView} has
     * been detached from the fragment.
     * Removes any pending callbacks for {@link #mapLoadRunnable} to prevent
     * issues if the fragment is destroyed before the map loads.
     * Sets {@link #placesClient} to null.
     */
    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
        // Remove the delayed runnable if the fragment is destroyed
        handler.removeCallbacks(mapLoadRunnable);
        if(placesClient != null)
        {
            placesClient = null; // Release PlacesClient
        }
    }

    /**
     * Called immediately after {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}
     * has returned, but before any saved state has been restored in to the view.
     * Initializes UI components like {@link #noResultsTextView}, {@link #recyclerView},
     * and {@link #searchView}. Sets up the {@link RestaurantSearchResultsAdapter} with click
     * listeners for item clicks (navigates to {@link RestaurantInfoFragment}) and navigation
     * button clicks (calls {@link #openNavigationApp(String)}).
     * Initializes the Google Places API and schedules the map loading via {@link #mapLoadRunnable}
     * with a delay.
     *
     * @param view               The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     *                           from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) // Added @NonNull for view
    {
        super.onViewCreated(view, savedInstanceState);

        noResultsTextView = view.findViewById(R.id.noResultsTextView);
        recyclerView = view.findViewById(R.id.searchResultsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

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
                                    .replace(R.id.fragmentContainer, restaurantInfoFragment) // Assumes R.id.fragmentContainer is the main container
                                    .addToBackStack(null)
                                    .commit();
                            Log.d("GmapsFragment", "Navigated to RestaurantInfoFragment"); // Corrected log message
                        }
                    }

                    @Override
                    public void onNavigateClick(Restaurant restaurant)
                    {
                        if(restaurant.getLocation() == null || restaurant.getAddress() == null)
                        {
                            Log.e("GmapsFragment", "Restaurant location or address is null, cannot navigate.");
                            Toast.makeText(getContext(), "Restaurant location not available.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if(!isNavigating) // Prevent rapid clicks
                        {
                            isNavigating = true;
                            openNavigationApp(restaurant.getAddress());
                            Log.d("GmapsFragment", "Navigation initiated to: " + restaurant.getAddress());
                            // Re-enable the button after a short delay
                            new Handler(Looper.getMainLooper()).postDelayed(() -> isNavigating = false, 500);
                        }
                    }
                }
        );
        recyclerView.setAdapter(restaurantSearchResultsAdapter);

        searchView = view.findViewById(R.id.searchBar);
        searchView.setOnClickListener(v -> searchView.setIconified(false)); // Expand search view on click
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener()
        {
            @Override
            public boolean onQueryTextSubmit(String query)
            {
                performSearch(query);
                // Hide keyboard
                InputMethodManager imm = (InputMethodManager) requireContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                if(imm != null && getView() != null) // Added null check for getView()
                {
                    imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
                }
                return true; // Query handled
            }

            @Override
            public boolean onQueryTextChange(String newText)
            {
                if(newText.isEmpty())
                {
                    // Clear results and hide bottom sheet if query is empty
                    recyclerView.setVisibility(View.GONE);
                    noResultsTextView.setVisibility(View.GONE);
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                }
                // Remove previous search marker if text changes
                if(currentSearchMarker != null)
                {
                    currentSearchMarker.remove();
                    currentSearchMarker = null;
                }
                return true; // Text change handled
            }
        });

        // Initialize Places API
        // Ensure API key is stored securely, e.g., in local.properties and accessed via BuildConfig
        String apiKey = "AIzaSyAcuJr7LuNMrciDVv9oACjSOV9wrMRaKwI"; // Hardcoded API Key - Not Recommended for production
        if(!Places.isInitialized())
        { // Initialize only if not already initialized
            Places.initializeWithNewPlacesApiEnabled(requireActivity().getApplicationContext(), apiKey);
        }
        placesClient = Places.createClient(requireContext());

        // Delayed map loading
        mapLoadRunnable = () -> loadMap(placesClient);
        handler.postDelayed(mapLoadRunnable, 500); // Delay to allow layout inflation
    }

    /**
     * Performs a search for restaurants in Firestore based on the provided query string.
     * Searches the "Restaurants" collection, ordering by name, and matching names that
     * start with the query. On success, calls {@link #updateSearchResults(List)} with the results.
     * Logs an error on failure.
     *
     * @param query The search query string.
     */
    private void performSearch(String query)
    {
        if(!query.isEmpty())
        {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("Restaurants")
                    .orderBy("name")
                    .startAt(query)
                    .endAt(query + "\uf8ff") // \uf8ff is a high Unicode character for prefix matching
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
                        Log.e("SearchError", "Error performing search for query: " + query, e);
                        Toast.makeText(getContext(), "Search failed. Please try again.", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    /**
     * Updates the UI with the search results.
     * If results are empty, it shows {@link #noResultsTextView} and hides the {@link #recyclerView}.
     * Otherwise, it updates the {@link #restaurantSearchResultsAdapter} with the new data,
     * shows the RecyclerView, and hides the no results text.
     * If results are found, it adds a marker on the map for the first restaurant in the list
     * (removing any previous search marker) and animates the camera to its location.
     * Finally, it makes the bottom sheet visible and sets its state to collapsed.
     *
     * @param results The list of {@link Restaurant} objects found by the search.
     */
    private void updateSearchResults(List<Restaurant> results)
    {
        if(results.isEmpty())
        {
            noResultsTextView.setVisibility(View.VISIBLE);
            noResultsTextView.setText("No results found"); // Use string resource
            recyclerView.setVisibility(View.GONE);
        } else
        {
            restaurantSearchResultsAdapter.updateData(results);
            noResultsTextView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);

            // Show marker for the first result on the map
            Restaurant firstResult = results.get(0);
            GeoPoint geoPoint = firstResult.getLocation();
            if(geoPoint != null && mMap != null)
            {
                LatLng latLng = new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude());

                if(currentSearchMarker != null)
                {
                    currentSearchMarker.remove(); // Remove previous marker
                }
                currentSearchMarker = mMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title(firstResult.getName()));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16)); // Zoom to marker
            }
        }

        // Show the bottom sheet
        bottomSheet.setVisibility(View.VISIBLE);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    /**
     * Fetches the user's current place using the Google Places API.
     * This method requires {@link Manifest.permission#ACCESS_FINE_LOCATION}.
     * If permission is granted, it makes a {@link FindCurrentPlaceRequest} and logs the
     * place likelihoods. If permission is not granted, it launches the {@link #locationPermissionLauncher}.
     * Handles API exceptions by logging them.
     *
     * @param placesClient The {@link PlacesClient} instance to use for the API call.
     */
    private void setCurrentLocation(PlacesClient placesClient)
    {
        List<Place.Field> placeFields = Collections.singletonList(Place.Field.DISPLAY_NAME);
        Log.d("PlacesDebug", "Before FindCurrentPlaceRequest");
        FindCurrentPlaceRequest request = FindCurrentPlaceRequest.newInstance(placeFields);
        Log.d("PlacesDebug", "After FindCurrentPlaceRequest");

        if(ContextCompat.checkSelfPermission(requireActivity().getApplicationContext(), ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            Log.d("PlacesDebug", "Before findCurrentPlace call");
            Task<FindCurrentPlaceResponse> placeResponse = placesClient.findCurrentPlace(request);
            Log.d("PlacesDebug", "After findCurrentPlace call initiated");
            placeResponse.addOnCompleteListener(task ->
            {
                if(task.isSuccessful())
                {
                    FindCurrentPlaceResponse response = task.getResult();
                    if(response != null)
                    {
                        for(PlaceLikelihood placeLikelihood : response.getPlaceLikelihoods())
                        {
                            Log.i("CurrentLocation", String.format("Place '%s' has likelihood: %f",
                                    placeLikelihood.getPlace().getDisplayName(),
                                    placeLikelihood.getLikelihood()));
                        }
                    } else
                    {
                        Log.w("CurrentLocation", "FindCurrentPlaceResponse is null.");
                    }
                } else
                {
                    Exception exception = task.getException();
                    if(exception instanceof ApiException)
                    {
                        ApiException apiException = (ApiException) exception;
                        Log.e("PlacesAPIError", "Place not found: " + apiException.getStatusCode() + " " + apiException.getMessage());
                    } else if(exception != null)
                    {
                        Log.e("PlacesAPIError", "Error finding current place: " + exception.getMessage(), exception);
                    } else
                    {
                        Log.e("PlacesAPIError", "Unknown error finding current place.");
                    }
                }
            });
        } else
        {
            // Request location permission
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    /**
     * ActivityResultLauncher for handling location permission requests.
     * If {@link Manifest.permission#ACCESS_FINE_LOCATION} is granted, it calls
     * {@link #setCurrentLocation(PlacesClient)}. Otherwise, it logs a warning.
     */
    private final ActivityResultLauncher<String> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted ->
            {
                if(isGranted)
                {
                    Log.d("Permission", "Location permission granted by user.");
                    if(placesClient != null)
                    { // Ensure placesClient is not null
                        setCurrentLocation(placesClient);
                    } else
                    {
                        Log.e("Permission", "PlacesClient is null after permission grant.");
                    }
                } else
                {
                    Log.w("Permission", "Location permission denied by user.");
                    Toast.makeText(getContext(), "Location permission is needed to show current location.", Toast.LENGTH_LONG).show();
                }
            });


    /**
     * Loads the Google Map into the {@code R.id.mapContainer}.
     * If the {@link SupportMapFragment} doesn't exist, it creates a new instance with a specific map ID,
     * replaces the container content with it, and commits the transaction.
     * Once the map is ready (via {@link OnMapReadyCallback}), it calls {@link #setMapSettings(GoogleMap, PlacesClient)}
     * and {@link #moveMapButtons(SupportMapFragment)}.
     *
     * @param placesClient The {@link PlacesClient} to be used with map settings.
     */
    private void loadMap(PlacesClient placesClient)
    {
        if(getContext() == null || getChildFragmentManager() == null)
        {
            Log.e("GmapsFragment", "Context or ChildFragmentManager is null in loadMap. Cannot proceed.");
            return;
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.mapContainer);

        if(mapFragment == null)
        {
            final SupportMapFragment newMapFragment = SupportMapFragment.newInstance(
                    new GoogleMapOptions().mapId(getResources().getString(R.string.map_id))); // Use map ID from resources

            FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
            if(!getChildFragmentManager().isStateSaved()) // Check if state is saved to prevent IllegalStateException
            {
                transaction.replace(R.id.mapContainer, newMapFragment);
                transaction.commit(); // Use commitNow() if immediate effect is needed and allowed
            } else
            {
                Log.w("GmapsFragment", "Cannot commit fragment transaction: state already saved.");
                return;
            }

            newMapFragment.getMapAsync(googleMap ->
            { // onMapReady callback
                mMap = googleMap;
                setMapSettings(mMap, placesClient);
                moveMapButtons(newMapFragment); // Pass the fragment instance
            });
        } else
        {
            // If mapFragment already exists, re-initialize or ensure it's ready
            mapFragment.getMapAsync(googleMap ->
            {
                mMap = googleMap;
                setMapSettings(mMap, placesClient);
                moveMapButtons(mapFragment);
            });
        }
    }

    /**
     * Configures settings for the provided {@link GoogleMap} instance.
     * Calls {@link #setCurrentLocation(PlacesClient)} to attempt to fetch current place data.
     * Calls {@link #setCurrentCameraPosition()} to move the camera to the user's current location.
     * Enables UI settings like compass, zoom controls, "My Location" button (if permission granted),
     * and rotate gestures.
     *
     * @param googleMap    The {@link GoogleMap} instance to configure.
     * @param placesClient The {@link PlacesClient} for location-related operations.
     */
    private void setMapSettings(GoogleMap googleMap, PlacesClient placesClient) // Renamed param for clarity
    {
        if(getContext() == null)
        {
            Log.e("GmapsFragment", "Context is null in setMapSettings.");
            return;
        }
        this.mMap = googleMap; // Ensure mMap is set
        setCurrentLocation(placesClient); // Attempt to get current place info
        setCurrentCameraPosition();       // Move camera to current GPS location

        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true); // Default zoom controls
        if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            mMap.setMyLocationEnabled(true); // Show blue dot for current location
        }
        mMap.getUiSettings().setMyLocationButtonEnabled(true); // Show "My Location" button
        mMap.getUiSettings().setRotateGesturesEnabled(true);
    }

    /**
     * Repositions the default Google Maps UI buttons (My Location, Zoom, Compass)
     * to custom locations on the screen. This is done by finding the views by their
     * internal IDs (which is fragile and might break with SDK updates) and adjusting
     * their {@link RelativeLayout.LayoutParams} and scale.
     *
     * @param mapFragment The {@link SupportMapFragment} whose view contains the map buttons.
     */
    private void moveMapButtons(SupportMapFragment mapFragment)
    {
        View mapView = mapFragment.getView();
        if(mapView == null)
        {
            Log.w("GmapsFragment", "MapView is null in moveMapButtons. Cannot move buttons.");
            return;
        }

        // IDs for map buttons are internal and not guaranteed. This is a common workaround.
        // MyLocation button typically has ID 2
        // Zoom controls typically have ID 1
        // Compass button typically has ID 5

        // Move MyLocation button
        View locationButton = mapView.findViewById(Integer.parseInt("2")); // Fragile: internal ID
        if(locationButton != null && locationButton.getLayoutParams() instanceof RelativeLayout.LayoutParams)
        {
            RelativeLayout.LayoutParams locationParams = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();
            locationParams.removeRule(RelativeLayout.ALIGN_PARENT_TOP); // Clear existing top alignment
            locationParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
            locationParams.setMargins(0, 0, dpToPx(16), dpToPx(16));
            locationButton.setLayoutParams(locationParams);
            locationButton.setScaleX(1.2f);
            locationButton.setScaleY(1.2f);
        } else
        {
            Log.w("GmapsFragment", "MyLocation button (id=2) not found or has unexpected LayoutParams.");
        }

        // Move Zoom controls
        View zoomControls = mapView.findViewById(Integer.parseInt("1")); // Fragile: internal ID
        if(zoomControls != null && zoomControls.getLayoutParams() instanceof RelativeLayout.LayoutParams)
        {
            RelativeLayout.LayoutParams zoomParams = (RelativeLayout.LayoutParams) zoomControls.getLayoutParams();
            zoomParams.removeRule(RelativeLayout.ALIGN_PARENT_TOP); // Clear existing top alignment
            zoomParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
            zoomParams.setMargins(0, 0, dpToPx(16), dpToPx(96)); // Position above MyLocation button
            zoomControls.setLayoutParams(zoomParams);
            zoomControls.setScaleX(1.2f);
            zoomControls.setScaleY(1.2f);
        } else
        {
            Log.w("GmapsFragment", "Zoom controls (id=1) not found or has unexpected LayoutParams.");
        }

        // Move Compass button
        View compassButton = mapView.findViewById(Integer.parseInt("5")); // Fragile: internal ID
        if(compassButton != null && compassButton.getLayoutParams() instanceof RelativeLayout.LayoutParams)
        {
            RelativeLayout.LayoutParams compassParams = (RelativeLayout.LayoutParams) compassButton.getLayoutParams();
            // Assuming default is top-right, adjust top margin
            compassParams.setMargins(0, dpToPx(80), dpToPx(16), 0);
            compassButton.setLayoutParams(compassParams);
            compassButton.setScaleX(1.2f);
            compassButton.setScaleY(1.2f);
        } else
        {
            Log.w("GmapsFragment", "Compass button (id=5) not found or has unexpected LayoutParams.");
        }
    }

    /**
     * Converts density-independent pixels (dp) to physical pixels (px).
     *
     * @param dp The value in dp.
     * @return The equivalent value in px.
     */
    private int dpToPx(int dp)
    {
        if(getContext() == null) return dp; // Fallback if context is not available
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    /**
     * Fetches the user's current GPS location using {@link FusedLocationProviderClient}
     * and moves the map camera to focus on this location.
     * Requires {@link Manifest.permission#ACCESS_FINE_LOCATION} and
     * {@link Manifest.permission#ACCESS_COARSE_LOCATION}.
     * If permissions are granted and location is successfully obtained, it calls
     * {@link #focusOnLocation(GoogleMap, LatLng)}.
     */
    private void setCurrentCameraPosition()
    {
        if(getContext() == null)
        {
            Log.e("GmapsFragment", "Context is null in setCurrentCameraPosition.");
            return;
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity().getApplicationContext());
        if(ActivityCompat.checkSelfPermission(requireActivity().getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireActivity().getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            CurrentLocationRequest currentLocationRequest = new CurrentLocationRequest.Builder()
                    .setDurationMillis(5000)      // How long to actively listen for updates
                    .setMaxUpdateAgeMillis(0)     // Request a fresh location
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
                    } else
                    {
                        Log.w("GmapsFragment", "FusedLocationProvider returned null location.");
                        // Optionally, show a toast or default to a known location
                    }
                } else
                {
                    Log.e("GmapsFragment", "Failed to get current location for camera.", task.getException());
                    // Optionally, show a toast
                }
            });
        } else
        {
            Log.w("GmapsFragment", "Location permissions not granted for setCurrentCameraPosition.");
            // Permissions should have been requested earlier, but this is a fallback log.
        }
    }

    /**
     * Animates the Google Map camera to the specified {@link LatLng} with a zoom level of 14.
     *
     * @param googleMap The {@link GoogleMap} instance to animate.
     * @param latLng    The {@link LatLng} to focus on.
     */
    private void focusOnLocation(GoogleMap googleMap, LatLng latLng) // Renamed param for clarity
    {
        if(googleMap != null && latLng != null)
        {
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(latLng)
                    .zoom(14) // Default zoom level
                    .build();
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }

    /**
     * Opens an external navigation application to provide directions to the given address.
     * It creates a "geo" URI intent and uses {@link Intent#createChooser(Intent, CharSequence)}
     * to allow the user to select their preferred navigation app (e.g., Google Maps, Waze).
     * If no app can handle the intent, a toast message is displayed.
     *
     * @param address The destination address string.
     */
    private void openNavigationApp(String address)
    {
        // geo:0,0?q=address URI for searching address in map apps
        String uriString = "geo:0,0?q=" + Uri.encode(address);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriString));

        // Create a chooser to let the user pick a navigation app
        Intent chooserIntent = Intent.createChooser(mapIntent, "Choose a Navigation App");

        if(getContext() != null && chooserIntent.resolveActivity(requireContext().getPackageManager()) != null)
        {
            startActivity(chooserIntent);
        } else
        {
            Toast.makeText(getContext(), "No navigation apps available on this device.", Toast.LENGTH_SHORT).show();
        }
    }
}