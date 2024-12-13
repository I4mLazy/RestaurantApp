package com.example.restaurantapp;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

import static androidx.core.location.LocationManagerCompat.getCurrentLocation;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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

import java.util.Collections;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link GmapsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class GmapsFragment extends Fragment
{
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public GmapsFragment()
    {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment GmapsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static GmapsFragment newInstance(String param1, String param2)
    {
        GmapsFragment fragment = new GmapsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (getArguments() != null)
        {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        String apiKey = BuildConfig.MAPS_API_KEY;
        Places.initializeWithNewPlacesApiEnabled(requireActivity().getApplicationContext(), apiKey);
        PlacesClient placesClient = Places.createClient(requireContext());

        View view = inflater.inflate(R.layout.fragment_gmaps, container, false);
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
                }
                else
                {
                    Exception exception = task.getException();
                    if (exception instanceof ApiException)
                    {
                        ApiException apiException = (ApiException) exception;
                        Log.e("NotFound", "Place not found: " + apiException.getStatusCode());
                    }
                }
            });
        }
        else
        {
            // A local method to request required permissions;
            // See https://developer.android.com/training/permissions/requesting
            //regetLocationPermission();
        }
    }

    private void setMapSettings(GoogleMap mMap, PlacesClient placesClient)
    {
        //setSearchNearby(placesClient);
        setCurrentLocation(placesClient);
        setCurrentCameraPosition();
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED)
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
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY).build();

            Task<Location> locationTask = fusedLocationClient.getCurrentLocation(currentLocationRequest, null);
            locationTask.addOnCompleteListener(task ->
            {
                if (task.isSuccessful()) {
                    Location location = task.getResult();
                    if (location != null)
                    {
                        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        focusOnLocation(mMap, currentLatLng);
                    }
                    else
                    {
                        // Handle location not available
                    }
                }
                else
                {
                    // Handle location request error
                }
            });
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
    }

    private void focusOnLocation(GoogleMap mMap, LatLng latLng)
    {
        if (mMap != null && latLng != null)
        {
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(latLng)
                    .zoom(14) // Set the desired zoom level
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