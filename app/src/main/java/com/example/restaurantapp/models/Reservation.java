package com.example.restaurantapp.models;

import java.util.Date;

/**
 * Represents a reservation made by a user at a restaurant.
 * Contains details such as the date and time of the reservation, number of guests,
 * special requests, status (e.g., "Upcoming", "Cancelled", "Completed"),
 * the name of the person who made the reservation (or restaurant name depending on context),
 * phone number, and unique identifiers for the user, restaurant, and the reservation itself.
 */
public class Reservation
{
    /**
     * The date of the reservation.
     */
    private Date date;
    /**
     * The time of the reservation (e.g., "19:00").
     */
    private String time;
    /**
     * The number of guests for the reservation, stored as a string.
     */
    private String guests;
    /**
     * Any special requests made for the reservation.
     */
    private String specialRequests;
    /**
     * The current status of the reservation (e.g., "Upcoming", "Cancelled", "Completed").
     */
    private String status;
    /**
     * The name associated with the reservation (could be user's name or restaurant's name depending on context).
     */
    private String name;
    /**
     * The name of the restaurant for which the reservation is made.
     */
    private String restaurantName;
    /**
     * The phone number associated with the reservation (could be user's or restaurant's).
     */
    private String phoneNumber;
    /**
     * The unique identifier of the user who made the reservation.
     */
    private String userID;
    /**
     * The unique identifier of the restaurant for which the reservation is made.
     */
    private String restaurantID;
    /**
     * The unique identifier for this reservation.
     */
    private String reservationID;

    /**
     * Default constructor required for calls to DataSnapshot.getValue(Reservation.class)
     * when using Firebase Realtime Database or for Firestore object mapping.
     */
    public Reservation()
    {
        // Default constructor
    }

    /**
     * Constructs a new {@code Reservation} object with specified details.
     *
     * @param date            The date of the reservation.
     * @param time            The time of the reservation.
     * @param guests          The number of guests (as a string).
     * @param specialRequests Any special requests.
     * @param status          The status of the reservation.
     * @param name            The name associated with the reservation.
     * @param restaurantName  The name of the restaurant.
     * @param phoneNumber     The phone number for the reservation.
     * @param userID          The ID of the user making the reservation.
     * @param restaurantID    The ID of the restaurant.
     * @param reservationID   The unique ID for this reservation.
     */
    public Reservation(Date date, String time, String guests, String specialRequests, String status, String name, String restaurantName, String phoneNumber, String userID, String restaurantID, String reservationID)
    {
        this.date = date;
        this.time = time;
        this.guests = guests;
        this.specialRequests = specialRequests;
        this.status = status;
        this.name = name;
        this.restaurantName = restaurantName;
        this.phoneNumber = phoneNumber;
        this.userID = userID;
        this.restaurantID = restaurantID;
        this.reservationID = reservationID;
    }

    // Getters

    /**
     * Gets the date of the reservation.
     *
     * @return The {@link Date} of the reservation.
     */
    public Date getDate()
    {
        return date;
    }

    /**
     * Gets the time of the reservation.
     *
     * @return The time string (e.g., "19:00").
     */
    public String getTime()
    {
        return time;
    }

    /**
     * Gets the number of guests for the reservation.
     *
     * @return The number of guests as a string.
     */
    public String getGuests()
    {
        return guests;
    }

    /**
     * Gets any special requests made for the reservation.
     *
     * @return The special requests string, or null if none.
     */
    public String getSpecialRequests()
    {
        return specialRequests;
    }

    /**
     * Gets the current status of the reservation.
     *
     * @return The status string (e.g., "Upcoming", "Cancelled").
     */
    public String getStatus()
    {
        return status;
    }

    /**
     * Gets the name associated with the reservation.
     * This could be the user's name or the restaurant's name depending on the context
     * in which the reservation object is used.
     *
     * @return The name string.
     */
    public String getName()
    {
        return name;
    }

    /**
     * Gets the phone number associated with the reservation.
     *
     * @return The phone number string.
     */
    public String getPhoneNumber()
    {
        return phoneNumber;
    }

    // Setters

    /**
     * Sets the date of the reservation.
     *
     * @param date The new {@link Date} for the reservation.
     */
    public void setDate(Date date)
    {
        this.date = date;
    }

    /**
     * Sets the time of the reservation.
     *
     * @param time The new time string (e.g., "19:00").
     */
    public void setTime(String time)
    {
        this.time = time;
    }

    /**
     * Sets the number of guests for the reservation.
     *
     * @param guests The new number of guests as a string.
     */
    public void setGuests(String guests)
    {
        this.guests = guests;
    }

    /**
     * Sets any special requests for the reservation.
     *
     * @param specialRequests The new special requests string.
     */
    public void setSpecialRequests(String specialRequests)
    {
        this.specialRequests = specialRequests;
    }

    /**
     * Sets the status of the reservation.
     *
     * @param status The new status string (e.g., "Upcoming", "Cancelled").
     */
    public void setStatus(String status)
    {
        this.status = status;
    }

    /**
     * Sets the name associated with the reservation.
     *
     * @param name The new name string.
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * Sets the phone number associated with the reservation.
     *
     * @param phoneNumber The new phone number string.
     */
    public void setPhoneNumber(String phoneNumber)
    {
        this.phoneNumber = phoneNumber;
    }

    /**
     * Gets the unique identifier of the user who made the reservation.
     *
     * @return The user ID string.
     */
    public String getUserID()
    {
        return userID;
    }

    /**
     * Sets the unique identifier of the user who made the reservation.
     *
     * @param userID The new user ID string.
     */
    public void setUserID(String userID)
    {
        this.userID = userID;
    }

    /**
     * Gets the unique identifier of the restaurant for which the reservation is made.
     *
     * @return The restaurant ID string.
     */
    public String getRestaurantID()
    {
        return restaurantID;
    }

    /**
     * Sets the unique identifier of the restaurant for which the reservation is made.
     *
     * @param restaurantID The new restaurant ID string.
     */
    public void setRestaurantID(String restaurantID)
    {
        this.restaurantID = restaurantID;
    }

    /**
     * Gets the unique identifier for this reservation.
     *
     * @return The reservation ID string.
     */
    public String getReservationID()
    {
        return reservationID;
    }

    /**
     * Sets the unique identifier for this reservation.
     *
     * @param reservationID The new reservation ID string.
     */
    public void setReservationID(String reservationID)
    {
        this.reservationID = reservationID;
    }

    /**
     * Gets the name of the restaurant for which the reservation is made.
     *
     * @return The restaurant name string.
     */
    public String getRestaurantName()
    {
        return restaurantName;
    }

    /**
     * Sets the name of the restaurant for which the reservation is made.
     *
     * @param restaurantName The new restaurant name string.
     */
    public void setRestaurantName(String restaurantName)
    {
        this.restaurantName = restaurantName;
    }
}