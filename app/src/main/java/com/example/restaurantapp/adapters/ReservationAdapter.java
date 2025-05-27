package com.example.restaurantapp.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.restaurantapp.R;
import com.example.restaurantapp.fragments.UpcomingReservationsFragment;
import com.example.restaurantapp.models.Reservation;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying a list of {@link Reservation} objects in a RecyclerView.
 * This adapter handles different display logic based on whether the reservations are upcoming,
 * and whether the view is for a restaurant user or a regular user. It supports displaying
 * reservation details such as date, time, status, guest count, special requests,
 * and provides a cancel button for upcoming reservations.
 */
public class ReservationAdapter extends RecyclerView.Adapter<ReservationAdapter.ReservationViewHolder>
{

    /**
     * The list of {@link Reservation} objects to be displayed.
     */
    private final List<Reservation> reservationList;
    /**
     * The context in which the adapter is operating.
     */
    private final Context context;
    /**
     * Flag indicating if the reservations in the list are upcoming (true) or past (false).
     */
    private final boolean isUpcoming;
    /**
     * Flag indicating if the adapter is being used in the context of a restaurant user (true) or a regular user (false).
     */
    private final boolean isRestaurant;
    /**
     * A reference to an {@link UpcomingReservationsFragment}. This is used to delegate
     * reservation cancellation actions. It might be null if not provided or if the context
     * is not an instance of {@code UpcomingReservationsFragment}.
     */
    private UpcomingReservationsFragment fragment;

    /**
     * Date formatter for displaying reservation dates in a user-friendly format (e.g., "MMMM dd, yyyy").
     */
    private final SimpleDateFormat displayDateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());

    /**
     * Constructs a new {@code ReservationAdapter}.
     * Initializes the adapter with the provided context, list of reservations, and a flag
     * indicating if the reservations are upcoming. It attempts to obtain a reference to
     * an {@link UpcomingReservationsFragment} if the provided context is a {@code FragmentActivity}
     * and its primary navigation fragment is an instance of {@code UpcomingReservationsFragment}.
     * It also determines if the current user is a restaurant user by checking "userType"
     * in SharedPreferences.
     *
     * @param context         The current context.
     * @param reservationList The list of {@link Reservation} objects to display.
     * @param isUpcoming      True if the reservations are upcoming, false otherwise.
     */
    public ReservationAdapter(Context context, List<Reservation> reservationList, boolean isUpcoming)
    {
        this.context = context;
        this.reservationList = reservationList;
        this.isUpcoming = isUpcoming;

        // Check if parent is an UpcomingReservationsFragment
        if(context instanceof androidx.fragment.app.FragmentActivity)
        {
            androidx.fragment.app.FragmentManager fm = ((androidx.fragment.app.FragmentActivity) context).getSupportFragmentManager();
            // Attempt to get the primary navigation fragment, which might be the host fragment.
            androidx.fragment.app.Fragment currentFragment = fm.getPrimaryNavigationFragment();
            if(currentFragment instanceof UpcomingReservationsFragment)
            {
                this.fragment = (UpcomingReservationsFragment) currentFragment;
            }
        }

        SharedPreferences prefs = context.getSharedPreferences("FeedMe", Context.MODE_PRIVATE);
        this.isRestaurant = "restaurant".equals(prefs.getString("userType", ""));
    }

    /**
     * Optional constructor that allows directly providing a reference to the {@link UpcomingReservationsFragment}.
     * This is useful if the fragment reference cannot be reliably obtained from the context.
     *
     * @param context         The current context.
     * @param reservationList The list of {@link Reservation} objects to display.
     * @param isUpcoming      True if the reservations are upcoming, false otherwise.
     * @param fragment        A direct reference to the {@link UpcomingReservationsFragment} for handling cancellations.
     */
    public ReservationAdapter(Context context, List<Reservation> reservationList, boolean isUpcoming, UpcomingReservationsFragment fragment)
    {
        this(context, reservationList, isUpcoming); // Calls the primary constructor
        this.fragment = fragment; // Overrides or sets the fragment reference
    }

    /**
     * Called when RecyclerView needs a new {@link ReservationViewHolder} of the given type to represent
     * an item.
     *
     * @param parent   The ViewGroup into which the new View will be added after it is bound to
     *                 an adapter position.
     * @param viewType The view type of the new View.
     * @return A new ReservationViewHolder that holds a View of the given view type.
     */
    @NonNull
    @Override
    public ReservationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reservation, parent, false);
        return new ReservationViewHolder(view);
    }

    /**
     * Called by RecyclerView to display the data at the specified position. This method should
     * update the contents of the {@link ReservationViewHolder#itemView} to reflect the item at the given
     * position.
     * It sets the reservation's date, time, status (with color coding), guest count, and special requests.
     * It conditionally displays user name/phone (for restaurant view) or restaurant name (for user view).
     * A cancel button is shown for upcoming, non-cancelled reservations, and its click listener
     * delegates to {@link UpcomingReservationsFragment#cancelReservation(Reservation, int)} if the fragment reference is available.
     *
     * @param holder   The ReservationViewHolder which should be updated to represent the contents of the
     *                 item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull ReservationViewHolder holder, int position)
    {
        Reservation reservation = reservationList.get(position);

        // Date and Time
        String dateTimeText = displayDateFormat.format(reservation.getDate()) + " at " + reservation.getTime();
        holder.dateTimeText.setText(dateTimeText);

        // Status
        Date date = reservation.getDate();
        Date dateNow = new Date();
        if("Cancelled".equalsIgnoreCase(reservation.getStatus()))
        {
            holder.statusText.setText(reservation.getStatus());
            holder.statusText.setTextColor(Color.RED);
        } else if(reservation.getStatus() == null && date.after(dateNow)) // Assuming null status for active upcoming reservations
        {
            holder.statusText.setText("Upcoming");
            holder.statusText.setTextColor(Color.parseColor("#388E3C")); // Green
        } else // Covers completed reservations or other non-cancelled, non-upcoming states
        {
            holder.statusText.setText("Completed"); // Default to "Completed" if not cancelled and not upcoming
            holder.statusText.setTextColor(Color.parseColor("#388E3C")); // Green
        }

        // Guests
        holder.guestCountText.setText(reservation.getGuests() + " guests");

        // Special requests
        String special = reservation.getSpecialRequests();
        if(special != null && !special.isEmpty())
        {
            holder.specialRequestsText.setText(special);
            holder.specialRequestsText.setVisibility(View.VISIBLE);
        } else
        {
            holder.specialRequestsText.setVisibility(View.GONE);
        }

        // User name & phone (for restaurant side) or Restaurant name (for user side)
        if(isRestaurant)
        {
            holder.userNameText.setText(reservation.getName());
            holder.userPhoneText.setText(reservation.getPhoneNumber());

            holder.userNameText.setVisibility(View.VISIBLE);
            holder.userPhoneText.setVisibility(View.VISIBLE);
            holder.restaurantNameText.setVisibility(View.GONE); // Hide restaurant name for restaurant user
        } else
        {
            holder.restaurantNameText.setText(reservation.getRestaurantName());
            holder.userNameText.setVisibility(View.GONE); // Hide user details for regular user
            holder.userPhoneText.setVisibility(View.GONE);
            holder.restaurantNameText.setVisibility(View.VISIBLE);
        }

        // Cancel button and bottom spacer visibility
        if(isUpcoming && !"Cancelled".equalsIgnoreCase(reservation.getStatus()))
        {
            holder.bottomSpacer.setVisibility(View.VISIBLE);
            holder.cancelButton.setVisibility(View.VISIBLE);
            // Set up cancel button click listener
            final int pos = position; // effectively final for use in lambda
            holder.cancelButton.setOnClickListener(v ->
            {
                if(fragment != null)
                {
                    fragment.cancelReservation(reservation, pos);
                } else
                {
                    Log.e("ReservationAdapter", "Fragment is null, cannot cancel reservation for item at position " + pos);
                }
            });
        } else
        {
            holder.cancelButton.setVisibility(View.GONE);
            holder.bottomSpacer.setVisibility(View.GONE);
        }
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of items in this adapter.
     */
    @Override
    public int getItemCount()
    {
        return reservationList.size();
    }

    /**
     * ViewHolder class for displaying a single {@link Reservation} item.
     * It holds references to the UI components within the item's layout,
     * such as TextViews for date/time, status, guest count, etc., and a Button for cancellation.
     */
    static class ReservationViewHolder extends RecyclerView.ViewHolder
    {
        /**
         * TextView to display the reservation date and time.
         */
        TextView dateTimeText;
        /**
         * TextView to display the status of the reservation (e.g., Upcoming, Cancelled, Completed).
         */
        TextView statusText;
        /**
         * TextView to display the number of guests for the reservation.
         */
        TextView guestCountText;
        /**
         * TextView to display any special requests for the reservation.
         */
        TextView specialRequestsText;
        /**
         * TextView to display the user's name (visible for restaurant users).
         */
        TextView userNameText;
        /**
         * TextView to display the user's phone number (visible for restaurant users).
         */
        TextView userPhoneText;
        /**
         * TextView to display the restaurant's name (visible for regular users).
         */
        TextView restaurantNameText;
        /**
         * Button to cancel an upcoming reservation.
         */
        Button cancelButton;
        /**
         * A spacer view at the bottom, typically shown with the cancel button.
         */
        View bottomSpacer;

        /**
         * Constructs a new {@code ReservationViewHolder}.
         * Initializes the UI components by finding them in the itemView.
         *
         * @param itemView The view that this ViewHolder will manage, representing a single reservation item.
         */
        public ReservationViewHolder(@NonNull View itemView)
        {
            super(itemView);

            dateTimeText = itemView.findViewById(R.id.reservationDateTimeText);
            statusText = itemView.findViewById(R.id.reservationStatus);
            guestCountText = itemView.findViewById(R.id.reservationGuestCount);
            specialRequestsText = itemView.findViewById(R.id.reservationSpecialRequests);
            userNameText = itemView.findViewById(R.id.reservationUserName);
            restaurantNameText = itemView.findViewById(R.id.reservationRestaurantName);
            userPhoneText = itemView.findViewById(R.id.reservationUserPhone);
            cancelButton = itemView.findViewById(R.id.cancelReservationButton);
            bottomSpacer = itemView.findViewById(R.id.bottomSpacer);
        }
    }
}