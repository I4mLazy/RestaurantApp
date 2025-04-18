package com.example.restaurantapp.fragments;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.restaurantapp.R;
import com.example.restaurantapp.adapters.ReservationAdapter;
import com.example.restaurantapp.models.Reservation;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class UpcomingReservationsFragment extends Fragment
{

    private RecyclerView reservationsRecyclerView;
    private LinearLayout emptyStateContainer;
    private MaterialButton filterByDateButton, clearFilterButton;
    private ChipGroup filterChipGroup;

    private ReservationAdapter reservationAdapter;
    private final List<Reservation> allReservations = new ArrayList<>();
    private final List<Reservation> filteredReservations = new ArrayList<>();
    private final List<String> activeDateFilters = new ArrayList<>();

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private boolean isRestaurant = false;

    public UpcomingReservationsFragment()
    {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_reservations, container, false);

        reservationsRecyclerView = view.findViewById(R.id.reservationsRecyclerView);
        emptyStateContainer = view.findViewById(R.id.emptyStateContainer);
        filterByDateButton = view.findViewById(R.id.filterByDateButton);
        clearFilterButton = view.findViewById(R.id.clearFilterButton);
        filterChipGroup = view.findViewById(R.id.filterChipGroup);

        clearFilterButton.setVisibility(View.GONE);

        // Get user type to know if this is a restaurant or user
        SharedPreferences prefs = requireContext().getSharedPreferences("FeedMe", Context.MODE_PRIVATE);
        isRestaurant = "restaurant".equals(prefs.getString("userType", ""));

        // Create the adapter with the correct constructor
        reservationAdapter = new ReservationAdapter(requireContext(), filteredReservations, true, this);

        reservationsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        reservationsRecyclerView.setAdapter(reservationAdapter);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        loadReservationsFromFirestore();
        setupFilters();

        return view;
    }

    private void loadReservationsFromFirestore()
    {
        String userType = requireContext()
                .getSharedPreferences("FeedMe", Context.MODE_PRIVATE)
                .getString("userType", "");

        allReservations.clear();

        String currentUserID = currentUser.getUid();
        if("restaurant".equals(userType))
        {
            db.collection("Users")
                    .document(currentUserID)
                    .get()
                    .addOnSuccessListener(userSnapshot ->
                    {
                        String restaurantID = userSnapshot.getString("restaurantID");

                        if(restaurantID == null || restaurantID.isEmpty())
                        {
                            // Handle missing restaurantID
                            updateEmptyState();
                            return;
                        }

                        db.collection("Restaurants")
                                .document(restaurantID)
                                .collection("Reservations")
                                .orderBy("date", Query.Direction.ASCENDING)
                                .get()
                                .addOnSuccessListener(resSnapshots ->
                                {
                                    for(QueryDocumentSnapshot doc : resSnapshots)
                                    {
                                        Reservation r = doc.toObject(Reservation.class);
                                        if(r.getDate().after(new Date()) && !"Cancelled".equals(r.getStatus()))
                                        {
                                            allReservations.add(r);
                                        }
                                    }
                                    applyFilters();
                                })
                                .addOnFailureListener(Throwable::printStackTrace);
                    })
                    .addOnFailureListener(Throwable::printStackTrace);
        } else
        {
            // Load user reservations directly
            db.collection("Users")
                    .document(currentUserID)
                    .collection("Reservations")
                    .orderBy("date", Query.Direction.ASCENDING)
                    .get()
                    .addOnSuccessListener(resSnapshots ->
                    {
                        for(QueryDocumentSnapshot doc : resSnapshots)
                        {
                            Reservation r = doc.toObject(Reservation.class);
                            if(r.getDate().after(new Date()) && !"Cancelled".equals(r.getStatus()))
                            {
                                allReservations.add(r);
                            }
                        }
                        applyFilters();
                    })
                    .addOnFailureListener(Throwable::printStackTrace);
        }
    }

    public void cancelReservation(Reservation reservation, int position)
    {
        String reservationID = reservation.getReservationID();
        String userID = reservation.getUserID();
        String restaurantID = reservation.getRestaurantID();

        // Check for missing identifiers
        if(reservationID == null || userID == null || restaurantID == null)
        {
            Log.e("Cancel", "Missing identifiers for cancellation");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Update both paths in Firestore
        db.collection("Restaurants")
                .document(restaurantID)
                .collection("Reservations")
                .document(reservationID)
                .update("status", "Cancelled")
                .addOnSuccessListener(aVoid ->
                {
                    // Successfully updated the restaurant side
                    Log.d("Cancel", "Restaurant reservation status updated");

                    // Update the user's reservation status
                    db.collection("Users")
                            .document(userID)
                            .collection("Reservations")
                            .document(reservationID)
                            .update("status", "Cancelled")
                            .addOnSuccessListener(aVoid1 ->
                            {
                                Log.d("Cancel", "User reservation status updated");
                                reservation.setStatus("Cancelled"); // Update the local reservation status
                                reservationAdapter.notifyItemChanged(position);
                            })
                            .addOnFailureListener(e ->
                            {
                                Log.e("Cancel", "Failed to update user's reservation status", e);
                            });
                })
                .addOnFailureListener(e ->
                {
                    Log.e("Cancel", "Failed to update restaurant reservation status", e);
                });

        // If the current user is a restaurant, send notification to the user
        if(isRestaurant)
        {
            //sendCancellationNotificationToUser(userID, reservation);
        }
    }

    private void sendCancellationNotificationToUser(String userID, Reservation reservation)
    {
        FirebaseFirestore.getInstance()
                .collection("Users")
                .document(userID)
                .get()
                .addOnSuccessListener(doc ->
                {
                    if(doc.contains("fcmToken"))
                    {
                        String token = doc.getString("fcmToken");
                        String title = "Reservation Cancelled";
                        String message = "Your reservation on " + reservation.getTime() + " at " +
                                new SimpleDateFormat("MMM d, yyyy").format(reservation.getDate()) + " was cancelled.";

                        //sendFCM(token, title, message);
                    }
                });
    }

    private void setupFilters()
    {
        filterByDateButton.setOnClickListener(v ->
        {
            showDatePickerDialog();
        });

        clearFilterButton.setOnClickListener(v ->
        {
            activeDateFilters.clear();
            filterChipGroup.removeAllViews();
            filterChipGroup.setVisibility(View.GONE);
            clearFilterButton.setVisibility(View.GONE);
            applyFilters();
        });
    }

    private void showDatePickerDialog()
    {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                getContext(),
                (view, year1, month1, dayOfMonth) ->
                {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year1, month1, dayOfMonth);
                    onDateSelected(selectedDate.getTime());
                },
                year, month, day
        );

        datePickerDialog.show();
    }

    private void onDateSelected(Date date)
    {
        // Format it for display or Firestore querying
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String formattedDate = sdf.format(date);

        // Now apply filter logic
        addDateFilterChip(formattedDate);
    }


    private void addDateFilterChip(String date)
    {
        if(!activeDateFilters.contains(date))
        {
            activeDateFilters.add(date);
            Chip chip = new Chip(requireContext());
            chip.setText(date);
            chip.setCloseIconResource(R.drawable.baseline_clear_24);
            chip.setCloseIconVisible(true);
            chip.setOnCloseIconClickListener(v ->
            {
                activeDateFilters.remove(date);
                filterChipGroup.removeView(chip);
                if(activeDateFilters.isEmpty())
                {
                    filterChipGroup.setVisibility(View.GONE);
                    clearFilterButton.setVisibility(View.GONE);
                }
                applyFilters();
            });
            filterChipGroup.addView(chip);
            filterChipGroup.setVisibility(View.VISIBLE);
            clearFilterButton.setVisibility(View.VISIBLE);
            applyFilters();
        }
    }

    private void applyFilters()
    {
        filteredReservations.clear();
        Date now = new Date();

        for(Reservation r : allReservations)
        {
            boolean isUpcoming = r.getDate().after(now);
            if(!isUpcoming) continue;

            if(activeDateFilters.isEmpty())
            {
                filteredReservations.add(r);
            } else
            {
                String reservationDate = dateFormat.format(r.getDate());
                if(activeDateFilters.contains(reservationDate))
                {
                    filteredReservations.add(r);
                }
            }
        }

        reservationAdapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState()
    {
        emptyStateContainer.setVisibility(filteredReservations.isEmpty() ? View.VISIBLE : View.GONE);
    }
}