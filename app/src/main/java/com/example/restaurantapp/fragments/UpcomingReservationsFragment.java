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
import android.widget.Toast;

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

/**
 * A {@link Fragment} subclass that displays a list of upcoming (future) reservations.
 * It fetches reservations from Firestore, differentiating between restaurant users
 * (who see upcoming reservations for their restaurant) and regular users (who see their own
 * upcoming reservations). The fragment allows filtering reservations by specific dates
 * using a {@link DatePickerDialog} and displays active date filters as {@link Chip}s
 * in a {@link ChipGroup}. It also provides functionality to cancel an upcoming reservation,
 * which updates the reservation status in Firestore for both the restaurant and the user.
 * An empty state is shown if no upcoming reservations match the current filters.
 */
public class UpcomingReservationsFragment extends Fragment
{

    /**
     * RecyclerView to display the list of upcoming reservations.
     */
    private RecyclerView reservationsRecyclerView;
    /**
     * LinearLayout container displayed when there are no upcoming reservations to show.
     */
    private LinearLayout emptyStateContainer;
    /**
     * MaterialButton to open a DatePickerDialog for selecting a date filter.
     */
    private MaterialButton filterByDateButton;
    /**
     * MaterialButton to clear all active date filters.
     */
    private MaterialButton clearFilterButton;
    /**
     * ChipGroup to display active date filter chips.
     */
    private ChipGroup filterChipGroup;

    /**
     * Adapter for the {@link #reservationsRecyclerView}.
     */
    private ReservationAdapter reservationAdapter;
    /**
     * List of all upcoming reservations fetched from Firestore before any filtering.
     */
    private final List<Reservation> allReservations = new ArrayList<>();
    /**
     * List of upcoming reservations currently displayed to the user (after filtering). This list is passed to the adapter.
     */
    private final List<Reservation> filteredReservations = new ArrayList<>();
    /**
     * List of currently active date filter strings (formatted as "yyyy-MM-dd").
     */
    private final List<String> activeDateFilters = new ArrayList<>();

    /**
     * SimpleDateFormat for formatting dates to "yyyy-MM-dd" for filtering and display.
     */
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()); // Note: Locale.getDefault() used

    /**
     * Instance of FirebaseFirestore for database operations.
     */
    FirebaseFirestore db; // Package-private for potential access by adapter if needed, though typically private
    /**
     * Instance of FirebaseAuth for user authentication.
     */
    private FirebaseAuth auth;
    /**
     * The currently authenticated FirebaseUser.
     */
    private FirebaseUser currentUser;
    /**
     * Flag indicating if the current user is a restaurant user (true) or a regular user (false).
     */
    private boolean isRestaurant = false;

    /**
     * Required empty public constructor for Fragment instantiation.
     */
    public UpcomingReservationsFragment()
    {
        // Required empty public constructor
    }

    /**
     * Called to have the fragment instantiate its user interface view.
     * Inflates the layout, initializes UI components, Firebase services, and the
     * {@link ReservationAdapter} (passing {@code true} for {@code isUpcoming} and a reference
     * to this fragment for cancellation callbacks). It determines the user type
     * from SharedPreferences. Then, it calls {@link #loadReservationsFromFirestore()}
     * to fetch reservation data and {@link #setupFilters()} to configure filter button listeners.
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
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_reservations, container, false);

        reservationsRecyclerView = view.findViewById(R.id.reservationsRecyclerView);
        emptyStateContainer = view.findViewById(R.id.emptyStateContainer);
        filterByDateButton = view.findViewById(R.id.filterByDateButton);
        clearFilterButton = view.findViewById(R.id.clearFilterButton);
        filterChipGroup = view.findViewById(R.id.filterChipGroup);

        clearFilterButton.setVisibility(View.GONE); // Initially hidden

        // Get user type to know if this is a restaurant or user
        SharedPreferences prefs = requireContext().getSharedPreferences("FeedMe", Context.MODE_PRIVATE);
        isRestaurant = "restaurant".equals(prefs.getString("userType", "")); // Default to not restaurant

        // Create the adapter with the correct constructor, passing 'this' for cancellation
        reservationAdapter = new ReservationAdapter(requireContext(), filteredReservations, true, this);

        reservationsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        reservationsRecyclerView.setAdapter(reservationAdapter);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        if(currentUser != null)
        { // Ensure currentUser is not null before loading
            loadReservationsFromFirestore();
        } else
        {
            updateEmptyState(); // Show empty state if no user
        }
        setupFilters();

        return view;
    }

    /**
     * Loads upcoming reservations from Firestore based on the user type.
     * If the user is a restaurant, it fetches reservations for their associated restaurant ID.
     * If the user is a regular user, it fetches their own reservations.
     * Only reservations that are in the future (date after current date) and not "Cancelled"
     * are added to {@link #allReservations}.
     * After fetching, it calls {@link #applyFilters()} to update the displayed list.
     * Handles potential errors during Firestore operations by printing stack traces.
     */
    private void loadReservationsFromFirestore()
    {
        // User type is already determined by isRestaurant field.
        // String userType = requireContext().getSharedPreferences("FeedMe", Context.MODE_PRIVATE).getString("userType", "");

        allReservations.clear(); // Clear previous data

        if(currentUser == null)
        {
            updateEmptyState();
            return;
        }
        String currentUserID = currentUser.getUid();

        if(isRestaurant) // Use the class field isRestaurant
        {
            db.collection("Users")
                    .document(currentUserID)
                    .get()
                    .addOnSuccessListener(userSnapshot ->
                    {
                        if(!isAdded()) return;
                        String restaurantID = userSnapshot.getString("restaurantID");

                        if(restaurantID == null || restaurantID.isEmpty())
                        {
                            Log.e("UpcomingReservations", "Restaurant user " + currentUserID + " has no restaurantID.");
                            updateEmptyState();
                            return;
                        }

                        db.collection("Restaurants")
                                .document(restaurantID)
                                .collection("Reservations")
                                .orderBy("date", Query.Direction.ASCENDING) // Order by date ascending for upcoming
                                .get()
                                .addOnSuccessListener(resSnapshots ->
                                {
                                    if(!isAdded()) return;
                                    Date now = new Date();
                                    for(QueryDocumentSnapshot doc : resSnapshots)
                                    {
                                        Reservation r = doc.toObject(Reservation.class);
                                        // Add if reservation date is after now AND status is not "Cancelled"
                                        if(r.getDate() != null && r.getDate().after(now) && !"Cancelled".equalsIgnoreCase(r.getStatus()))
                                        {
                                            allReservations.add(r);
                                        }
                                    }
                                    applyFilters(); // Apply filters and update UI
                                })
                                .addOnFailureListener(e ->
                                { // Replaced Throwable::printStackTrace
                                    Log.e("UpcomingReservations", "Error fetching restaurant reservations.", e);
                                    updateEmptyState();
                                });
                    })
                    .addOnFailureListener(e ->
                    { // Replaced Throwable::printStackTrace
                        Log.e("UpcomingReservations", "Error fetching user document for restaurantID.", e);
                        updateEmptyState();
                    });
        } else // Regular user
        {
            db.collection("Users")
                    .document(currentUserID)
                    .collection("Reservations")
                    .orderBy("date", Query.Direction.ASCENDING)
                    .get()
                    .addOnSuccessListener(resSnapshots ->
                    {
                        if(!isAdded()) return;
                        Date now = new Date();
                        for(QueryDocumentSnapshot doc : resSnapshots)
                        {
                            Reservation r = doc.toObject(Reservation.class);
                            if(r.getDate() != null && r.getDate().after(now) && !"Cancelled".equalsIgnoreCase(r.getStatus()))
                            {
                                allReservations.add(r);
                            }
                        }
                        applyFilters();
                    })
                    .addOnFailureListener(e ->
                    { // Replaced Throwable::printStackTrace
                        Log.e("UpcomingReservations", "Error fetching user reservations.", e);
                        updateEmptyState();
                    });
        }
    }

    /**
     * Cancels a given reservation by updating its status to "Cancelled" in Firestore.
     * This update is performed in both the restaurant's reservation subcollection and the
     * user's reservation subcollection.
     * If identifiers (reservationID, userID, restaurantID) are missing, the operation is aborted.
     * After successful updates in Firestore, the local {@link Reservation} object's status
     * is updated, and the adapter is notified to refresh the item at the given position.
     *
     * @param reservation The {@link Reservation} object to be cancelled.
     * @param position    The position of the reservation in the adapter's list.
     */
    public void cancelReservation(Reservation reservation, int position)
    {
        String reservationID = reservation.getReservationID();
        String userID = reservation.getUserID();
        String restaurantID = reservation.getRestaurantID();

        if(reservationID == null || userID == null || restaurantID == null)
        {
            Log.e("CancelReservation", "Cannot cancel: Missing identifiers (reservationID, userID, or restaurantID).");
            if(getContext() != null)
                Toast.makeText(getContext(), "Error: Cannot cancel reservation due to missing information.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore dbInstance = FirebaseFirestore.getInstance(); // Use local instance or class member 'db'

        // Update restaurant's copy of the reservation
        dbInstance.collection("Restaurants")
                .document(restaurantID)
                .collection("Reservations")
                .document(reservationID)
                .update("status", "Cancelled")
                .addOnSuccessListener(aVoidRestaurant ->
                {
                    Log.d("CancelReservation", "Restaurant's reservation status updated to Cancelled for ID: " + reservationID);
                    // Now update user's copy
                    dbInstance.collection("Users")
                            .document(userID)
                            .collection("Reservations")
                            .document(reservationID)
                            .update("status", "Cancelled")
                            .addOnSuccessListener(aVoidUser ->
                            {
                                Log.d("CancelReservation", "User's reservation status updated to Cancelled for ID: " + reservationID);
                                if(isAdded() && reservationAdapter != null)
                                { // Check fragment state and adapter
                                    reservation.setStatus("Cancelled"); // Update local object
                                    // Consider removing from list or just updating view
                                    // If removing:
                                    // allReservations.remove(reservation); // Remove from master list
                                    // filteredReservations.remove(reservation); // Remove from displayed list
                                    // reservationAdapter.notifyItemRemoved(position);
                                    // reservationAdapter.notifyItemRangeChanged(position, filteredReservations.size());
                                    // For now, just update item as per original code:
                                    reservationAdapter.notifyItemChanged(position);
                                    updateEmptyState(); // Re-check empty state
                                    Toast.makeText(getContext(), "Reservation cancelled.", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(eUser ->
                            {
                                Log.e("CancelReservation", "Failed to update user's reservation status for ID: " + reservationID, eUser);
                                // Potentially try to revert restaurant's status or notify user of partial failure
                                if(getContext() != null)
                                    Toast.makeText(getContext(), "Cancellation partially failed (user side).", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(eRestaurant ->
                {
                    Log.e("CancelReservation", "Failed to update restaurant's reservation status for ID: " + reservationID, eRestaurant);
                    if(getContext() != null)
                        Toast.makeText(getContext(), "Cancellation failed (restaurant side).", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Sets up click listeners for the filter buttons.
     * The "Filter by Date" button calls {@link #showDatePickerDialog()}.
     * The "Clear Filter" button clears all active date filters, removes chips from the
     * {@link #filterChipGroup}, hides the chip group and clear button, and then calls
     * {@link #applyFilters()} to refresh the reservation list.
     */
    private void setupFilters()
    {
        filterByDateButton.setOnClickListener(v -> showDatePickerDialog());

        clearFilterButton.setOnClickListener(v ->
        {
            activeDateFilters.clear();
            filterChipGroup.removeAllViews();
            filterChipGroup.setVisibility(View.GONE);
            clearFilterButton.setVisibility(View.GONE);
            applyFilters(); // Re-apply filters (which will show all upcoming reservations)
        });
    }

    /**
     * Displays a {@link DatePickerDialog} to allow the user to select a date for filtering.
     * When a date is selected, {@link #onDateSelected(Date)} is called.
     */
    private void showDatePickerDialog()
    {
        if(getContext() == null) return;

        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(), // Use requireContext
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

    /**
     * Called when a date is selected from the {@link DatePickerDialog}.
     * Formats the selected date into "yyyy-MM-dd" string format and calls
     * {@link #addDateFilterChip(String)} to add it as a filter.
     *
     * @param date The {@link Date} object selected by the user.
     */
    private void onDateSelected(Date date)
    {
        // SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()); // dateFormat field already exists
        String formattedDate = dateFormat.format(date);
        addDateFilterChip(formattedDate);
    }


    /**
     * Adds a new date filter {@link Chip} to the {@link #filterChipGroup} if the date is not already an active filter.
     * The chip displays the formatted date and has a close icon to remove the filter.
     * When a chip is added or its close icon is clicked, {@link #applyFilters()} is called to update the list.
     * Manages the visibility of the {@link #filterChipGroup} and {@link #clearFilterButton}.
     *
     * @param date The date string (formatted as "yyyy-MM-dd") to add as a filter.
     */
    private void addDateFilterChip(String date)
    {
        if(getContext() == null) return;

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
                applyFilters(); // Update list when a filter is removed
            });
            filterChipGroup.addView(chip);
            filterChipGroup.setVisibility(View.VISIBLE);
            clearFilterButton.setVisibility(View.VISIBLE);
            applyFilters(); // Update list when a new filter is added
        } else
        {
            Toast.makeText(getContext(), "Date filter '" + date + "' already applied.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Applies the currently active date filters to the {@link #allReservations} list.
     * It clears {@link #filteredReservations} and then iterates through {@link #allReservations}.
     * A reservation is added to {@link #filteredReservations} if it's an upcoming reservation
     * (date after now and not "Cancelled") AND (if activeDateFilters is not empty, its date
     * matches one of the active filters).
     * If no date filters are active, all upcoming, non-cancelled reservations are added.
     * Finally, it notifies the {@link #reservationAdapter} and calls {@link #updateEmptyState()}.
     */
    private void applyFilters()
    {
        filteredReservations.clear();
        Date now = new Date(); // Current date for comparison

        for(Reservation r : allReservations)
        {
            if(r.getDate() == null) continue; // Skip reservations with null date

            // Condition for being an "upcoming" item (not cancelled and date is after now)
            boolean isUpcomingAndNotCancelled = r.getDate().after(now) && !"Cancelled".equalsIgnoreCase(r.getStatus());

            if(!isUpcomingAndNotCancelled) continue; // Skip if not upcoming or already cancelled

            if(activeDateFilters.isEmpty()) // No active date filters, add all valid upcoming reservations
            {
                filteredReservations.add(r);
            } else // Active date filters exist
            {
                String reservationDateStr = dateFormat.format(r.getDate());
                if(activeDateFilters.contains(reservationDateStr)) // Check if reservation date matches any active filter
                {
                    filteredReservations.add(r);
                }
            }
        }

        // The adapter is already observing filteredReservations, so just notify it.
        if(reservationAdapter != null)
        {
            reservationAdapter.notifyDataSetChanged();
        }
        updateEmptyState(); // Update visibility of empty state message
    }

    /**
     * Updates the visibility of the {@link #emptyStateContainer}.
     * If {@link #filteredReservations} (the list displayed by the adapter) is empty,
     * the empty state container is made visible; otherwise, it is hidden.
     */
    private void updateEmptyState()
    {
        if(emptyStateContainer != null)
        { // Ensure view is not null
            emptyStateContainer.setVisibility(filteredReservations.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }
}