package com.example.restaurantapp.fragments;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log; // Unused import
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
 * A {@link Fragment} subclass that displays a history of past reservations.
 * It fetches reservations from Firestore, differentiating between restaurant users
 * (who see reservations for their restaurant) and regular users (who see their own reservations).
 * The fragment allows filtering reservations by specific dates using a {@link DatePickerDialog}
 * and displays active date filters as {@link Chip}s in a {@link ChipGroup}.
 * An empty state is shown if no past reservations match the current filters.
 */
public class ReservationHistoryFragment extends Fragment
{
    // UI components
    /**
     * RecyclerView to display the list of past reservations.
     */
    private RecyclerView reservationsRecyclerView;
    /**
     * LinearLayout container displayed when there are no reservations to show.
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

    // Data
    /**
     * List of past reservations currently displayed to the user (after filtering).
     */
    private final List<Reservation> pastReservations = new ArrayList<>();
    /**
     * List of all reservations fetched from Firestore before any filtering.
     */
    private final List<Reservation> allReservations = new ArrayList<>();
    /**
     * List of reservations after applying date filters (used as an intermediate step).
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
     * Adapter for the {@link #reservationsRecyclerView}.
     */
    private ReservationAdapter reservationAdapter;

    // Firebase
    /**
     * Instance of FirebaseFirestore for database operations.
     */
    private FirebaseFirestore db;
    /**
     * Instance of FirebaseAuth for user authentication.
     */
    private FirebaseAuth auth;
    /**
     * The currently authenticated FirebaseUser.
     */
    private FirebaseUser currentUser;

    // State
    /**
     * Flag indicating if the current user is a restaurant user (true) or a regular user (false).
     */
    private boolean isRestaurant;

    /**
     * Required empty public constructor for Fragment instantiation.
     */
    public ReservationHistoryFragment()
    {
        // Required empty public constructor
    }

    /**
     * Called to have the fragment instantiate its user interface view.
     * Inflates the layout, initializes UI components, Firebase services, and the
     * {@link ReservationAdapter}. It determines the user type (restaurant or regular user)
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

        // Initialize UI components
        reservationsRecyclerView = view.findViewById(R.id.reservationsRecyclerView);
        emptyStateContainer = view.findViewById(R.id.emptyStateContainer);
        filterByDateButton = view.findViewById(R.id.filterByDateButton);
        clearFilterButton = view.findViewById(R.id.clearFilterButton);
        filterChipGroup = view.findViewById(R.id.filterChipGroup);

        clearFilterButton.setVisibility(View.GONE); // Initially hidden

        // Initialize adapter for past reservations (isUpcoming = false)
        reservationAdapter = new ReservationAdapter(requireContext(), pastReservations, false, null);
        reservationsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        reservationsRecyclerView.setAdapter(reservationAdapter);

        // Initialize Firebase components
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        // Determine if user is a restaurant or customer
        SharedPreferences prefs = requireContext().getSharedPreferences("FeedMe", Context.MODE_PRIVATE);
        isRestaurant = "restaurant".equals(prefs.getString("userType", "")); // Default to not restaurant

        // Load past reservations and set up filters
        if(currentUser != null)
        { // Ensure currentUser is not null before loading
            loadReservationsFromFirestore();
        } else
        {
            // Handle case where user is not logged in, e.g., show error or navigate to login
            updateEmptyState(); // Show empty state if no user
        }
        setupFilters();

        return view;
    }

    /**
     * Loads reservations from Firestore based on the user type.
     * If the user is a restaurant, it fetches reservations for their associated restaurant ID.
     * If the user is a regular user, it fetches their own reservations.
     * Only reservations that are in the past (date before current date) or have a status of "Cancelled"
     * are added to {@link #allReservations}.
     * After fetching, it calls {@link #applyFilters()} to update the displayed list.
     * Handles potential errors during Firestore operations by printing stack traces.
     */
    private void loadReservationsFromFirestore()
    {
        // User type is already determined by isRestaurant field, can use that directly.
        // String userType = requireContext().getSharedPreferences("FeedMe", Context.MODE_PRIVATE).getString("userType", "");

        allReservations.clear(); // Clear previous data

        if(currentUser == null)
        {
            updateEmptyState(); // No user, show empty state
            return;
        }
        String currentUserID = currentUser.getUid();

        if(isRestaurant) // Use the class field isRestaurant
        {
            // Fetch restaurantID first for restaurant user
            db.collection("Users")
                    .document(currentUserID)
                    .get()
                    .addOnSuccessListener(userSnapshot ->
                    {
                        if(!isAdded()) return; // Check fragment state
                        String restaurantID = userSnapshot.getString("restaurantID");

                        if(restaurantID == null || restaurantID.isEmpty())
                        {
                            Log.e("ReservationHistory", "Restaurant user " + currentUserID + " has no restaurantID.");
                            updateEmptyState();
                            return;
                        }

                        // Fetch reservations for the restaurant
                        db.collection("Restaurants")
                                .document(restaurantID)
                                .collection("Reservations")
                                .orderBy("date", Query.Direction.DESCENDING) // Order by date descending
                                .get()
                                .addOnSuccessListener(resSnapshots ->
                                {
                                    if(!isAdded()) return;
                                    Date now = new Date();
                                    for(QueryDocumentSnapshot doc : resSnapshots)
                                    {
                                        Reservation r = doc.toObject(Reservation.class);
                                        // Add if reservation date is before now OR status is "Cancelled"
                                        if(r.getDate() != null && (r.getDate().before(now) || "Cancelled".equalsIgnoreCase(r.getStatus())))
                                        {
                                            allReservations.add(r);
                                        }
                                    }
                                    applyFilters(); // Apply filters and update UI
                                })
                                .addOnFailureListener(e ->
                                {
                                    Log.e("ReservationHistory", "Error fetching restaurant reservations.", e);
                                    // e.printStackTrace(); // Original code
                                    updateEmptyState(); // Show empty state on error
                                });
                    })
                    .addOnFailureListener(e ->
                    {
                        Log.e("ReservationHistory", "Error fetching user document for restaurantID.", e);
                        // e.printStackTrace(); // Original code
                        updateEmptyState();
                    });
        } else // Regular user
        {
            db.collection("Users")
                    .document(currentUserID)
                    .collection("Reservations")
                    .orderBy("date", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener(resSnapshots ->
                    {
                        if(!isAdded()) return;
                        Date now = new Date();
                        for(QueryDocumentSnapshot doc : resSnapshots)
                        {
                            Reservation r = doc.toObject(Reservation.class);
                            if(r.getDate() != null && (r.getDate().before(now) || "Cancelled".equalsIgnoreCase(r.getStatus())))
                            {
                                allReservations.add(r);
                            }
                        }
                        applyFilters();
                    })
                    .addOnFailureListener(e ->
                    { // Replaced Throwable::printStackTrace with lambda for logging
                        Log.e("ReservationHistory", "Error fetching user reservations.", e);
                        updateEmptyState();
                    });
        }
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
            applyFilters(); // Re-apply filters (which will show all past reservations)
        });
    }

    /**
     * Displays a {@link DatePickerDialog} to allow the user to select a date for filtering.
     * When a date is selected, {@link #onDateSelected(Date)} is called.
     */
    private void showDatePickerDialog()
    {
        if(getContext() == null) return; // Prevent crash if context is null

        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(), // Use requireContext for non-null context
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
        // Format it for display or Firestore querying
        // SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()); // dateFormat field already exists
        String formattedDate = dateFormat.format(date);

        // Now apply filter logic
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
            chip.setCloseIconResource(R.drawable.baseline_clear_24); // Ensure this drawable exists
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
            Toast.makeText(getContext(), "Date filter already applied: " + date, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Applies the currently active date filters to the {@link #allReservations} list.
     * It clears {@link #filteredReservations} and then iterates through {@link #allReservations}.
     * A reservation is added to {@link #filteredReservations} if it's a past reservation (date before now
     * or status is "Cancelled") AND (if activeDateFilters is not empty, its date matches one of the active filters).
     * If no date filters are active, all past/cancelled reservations are added.
     * Finally, it updates {@link #pastReservations} with the content of {@link #filteredReservations},
     * notifies the {@link #reservationAdapter}, and calls {@link #updateEmptyState()}.
     */
    private void applyFilters()
    {
        filteredReservations.clear();
        Date now = new Date(); // Current date for comparison

        for(Reservation r : allReservations)
        {
            if(r.getDate() == null) continue; // Skip reservations with null date

            // Condition for being a "past" or "history" item
            boolean isHistoryItem = r.getDate().before(now) || "Cancelled".equalsIgnoreCase(r.getStatus());

            if(isHistoryItem)
            {
                if(activeDateFilters.isEmpty()) // No active date filters, add all history items
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
        }

        pastReservations.clear();
        pastReservations.addAll(filteredReservations); // Update the list the adapter is observing

        if(reservationAdapter != null)
        {
            reservationAdapter.notifyDataSetChanged();
        }
        updateEmptyState(); // Update visibility of empty state message
    }

    /**
     * Updates the visibility of the {@link #emptyStateContainer}.
     * If {@link #pastReservations} is empty, the empty state container is made visible;
     * otherwise, it is hidden.
     */
    private void updateEmptyState()
    {
        if(emptyStateContainer != null)
        { // Ensure view is not null
            emptyStateContainer.setVisibility(pastReservations.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }
}