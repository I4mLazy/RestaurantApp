package com.example.restaurantapp.models;

/**
 * Represents a user's rating for a restaurant.
 * Contains the rating value itself and a timestamp indicating when the rating was given.
 * Note: The timestamp is stored as a {@code long}. If this represents milliseconds since epoch,
 * it can be converted to/from {@link com.google.firebase.Timestamp} or {@link java.util.Date}
 * as needed when interacting with databases or for display.
 */
public class Rating
{
    /**
     * The numerical rating value (e.g., stars from 1 to 5).
     */
    private double rating;
    /**
     * The timestamp when the rating was submitted, typically as milliseconds since epoch.
     */
    private long timestamp;

    /**
     * Default constructor required for calls to DataSnapshot.getValue(Rating.class)
     * when using Firebase Realtime Database or for Firestore object mapping.
     */
    public Rating()
    {
        // Default constructor
    }

    /**
     * Constructs a new {@code Rating} object with a specified rating value and timestamp.
     *
     * @param rating    The numerical rating value.
     * @param timestamp The timestamp of when the rating was given (e.g., milliseconds since epoch).
     */
    public Rating(double rating, long timestamp)
    {
        this.rating = rating;
        this.timestamp = timestamp;
    }

    /**
     * Gets the numerical rating value.
     *
     * @return The rating value.
     */
    public double getRating()
    {
        return rating;
    }

    /**
     * Sets the numerical rating value.
     *
     * @param rating The new rating value.
     */
    public void setRating(double rating)
    {
        this.rating = rating;
    }

    /**
     * Gets the timestamp of when the rating was submitted.
     * This is typically represented as milliseconds since the epoch.
     *
     * @return The timestamp as a long value.
     */
    public long getTimestamp()
    {
        return timestamp;
    }

    /**
     * Sets the timestamp of when the rating was submitted.
     *
     * @param timestamp The new timestamp (e.g., milliseconds since epoch).
     */
    public void setTimestamp(long timestamp)
    {
        this.timestamp = timestamp;
    }
}