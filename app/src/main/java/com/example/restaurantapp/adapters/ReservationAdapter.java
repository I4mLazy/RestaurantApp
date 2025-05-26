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

public class ReservationAdapter extends RecyclerView.Adapter<ReservationAdapter.ReservationViewHolder>
{

    private final List<Reservation> reservationList;
    private final Context context;
    private final boolean isUpcoming;
    private final boolean isRestaurant;
    private UpcomingReservationsFragment fragment;

    private final SimpleDateFormat displayDateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());

    public ReservationAdapter(Context context, List<Reservation> reservationList, boolean isUpcoming)
    {
        this.context = context;
        this.reservationList = reservationList;
        this.isUpcoming = isUpcoming;

        // Check if parent is an UpcomingReservationsFragment
        if(context instanceof androidx.fragment.app.FragmentActivity)
        {
            androidx.fragment.app.FragmentManager fm = ((androidx.fragment.app.FragmentActivity) context).getSupportFragmentManager();
            androidx.fragment.app.Fragment currentFragment = fm.getPrimaryNavigationFragment();
            if(currentFragment instanceof UpcomingReservationsFragment)
            {
                this.fragment = (UpcomingReservationsFragment) currentFragment;
            }
        }

        SharedPreferences prefs = context.getSharedPreferences("FeedMe", Context.MODE_PRIVATE);
        this.isRestaurant = "restaurant".equals(prefs.getString("userType", ""));
    }

    // Optional constructor to directly provide the fragment reference
    public ReservationAdapter(Context context, List<Reservation> reservationList, boolean isUpcoming, UpcomingReservationsFragment fragment)
    {
        this(context, reservationList, isUpcoming);
        this.fragment = fragment;
    }

    @NonNull
    @Override
    public ReservationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reservation, parent, false);
        return new ReservationViewHolder(view);
    }

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
        } else if(reservation.getStatus() == null && date.after(dateNow))
        {
            holder.statusText.setText("Upcoming");
            holder.statusText.setTextColor(Color.parseColor("#388E3C")); // Green
        } else
        {
            holder.statusText.setText("Completed");
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

        // User name & phone (for restaurant side)
        if(isRestaurant)
        {
            holder.userNameText.setText(reservation.getName());
            holder.userPhoneText.setText(reservation.getPhoneNumber());

            holder.userNameText.setVisibility(View.VISIBLE);
            holder.userPhoneText.setVisibility(View.VISIBLE);
        } else
        {
            holder.restaurantNameText.setText(reservation.getRestaurantName());
            holder.userNameText.setVisibility(View.GONE);
            holder.userPhoneText.setVisibility(View.GONE);
            holder.restaurantNameText.setVisibility(View.VISIBLE);
        }

        // Cancel button
        if(isUpcoming && !"Cancelled".equalsIgnoreCase(reservation.getStatus()))
        {
            holder.bottomSpacer.setVisibility(View.VISIBLE);
            holder.cancelButton.setVisibility(View.VISIBLE);
            // Set up cancel button click listener
            final int pos = position;
            holder.cancelButton.setOnClickListener(v ->
            {
                if(fragment != null)
                {
                    fragment.cancelReservation(reservation, pos);
                } else
                {
                    Log.e("ReservationAdapter", "Fragment is null, cannot cancel reservation");
                }
            });
        } else
        {
            holder.cancelButton.setVisibility(View.GONE);
            holder.bottomSpacer.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount()
    {
        return reservationList.size();
    }

    static class ReservationViewHolder extends RecyclerView.ViewHolder
    {

        TextView dateTimeText, statusText, guestCountText;
        TextView specialRequestsText, userNameText, userPhoneText, restaurantNameText;
        Button cancelButton;
        View bottomSpacer;

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