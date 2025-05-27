package com.example.restaurantapp.models;

import com.google.firebase.Timestamp;

/**
 * Represents a discount that can be applied to a menu item or a menu.
 * Contains information about the type of discount (percentage or flat amount),
 * the value of the discount, and its validity period (start and end times).
 * Each discount also has a unique ID.
 */
public class Discount
{
    /**
     * The type of discount, e.g., "Percentage" or "Flat".
     */
    private String discountType;
    /**
     * The numerical value of the discount (e.g., 20 for 20% or 5 for $5 off).
     */
    private double discountValue;
    /**
     * The {@link Timestamp} indicating when the discount becomes active.
     */
    private Timestamp startTime;
    /**
     * The {@link Timestamp} indicating when the discount expires. Can be null if the discount does not expire.
     */
    private Timestamp endTime;
    /**
     * A unique identifier for this discount.
     */
    private String discountID;

    /**
     * Default constructor required for calls to DataSnapshot.getValue(Discount.class)
     * when using Firebase Realtime Database or for Firestore object mapping.
     */
    public Discount()
    {
        // Default constructor
    }

    /**
     * Constructs a new {@code Discount} object with specified details.
     *
     * @param discountType  The type of the discount (e.g., "Percentage", "Flat").
     * @param discountValue The value of the discount.
     * @param startTime     The start time of the discount.
     * @param endTime       The end time of the discount (can be null).
     * @param discountID    The unique ID for this discount.
     */
    public Discount(String discountType, double discountValue, Timestamp startTime, Timestamp endTime, String discountID)
    {
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.startTime = startTime;
        this.endTime = endTime;
        this.discountID = discountID;
    }

    /**
     * Gets the type of the discount.
     *
     * @return A string representing the discount type (e.g., "Percentage", "Flat").
     */
    public String getDiscountType()
    {
        return discountType;
    }

    /**
     * Sets the type of the discount.
     *
     * @param discountType The new discount type.
     */
    public void setDiscountType(String discountType)
    {
        this.discountType = discountType;
    }

    /**
     * Gets the value of the discount.
     * This could be a percentage (e.g., 20 for 20%) or a flat amount (e.g., 5 for $5).
     *
     * @return The numerical value of the discount.
     */
    public double getDiscountValue()
    {
        return discountValue;
    }

    /**
     * Sets the value of the discount.
     *
     * @param discountValue The new discount value.
     */
    public void setDiscountValue(double discountValue)
    {
        this.discountValue = discountValue;
    }

    /**
     * Gets the start time of the discount.
     *
     * @return The {@link Timestamp} when the discount becomes active.
     */
    public Timestamp getStartTime()
    {
        return startTime;
    }

    /**
     * Sets the start time of the discount.
     *
     * @param startTime The new start time.
     */
    public void setStartTime(Timestamp startTime)
    {
        this.startTime = startTime;
    }

    /**
     * Gets the end time of the discount.
     *
     * @return The {@link Timestamp} when the discount expires, or null if it doesn't expire.
     */
    public Timestamp getEndTime()
    {
        return endTime;
    }

    /**
     * Sets the end time of the discount.
     *
     * @param endTime The new end time. Can be null.
     */
    public void setEndTime(Timestamp endTime)
    {
        this.endTime = endTime;
    }

    /**
     * Sets the unique identifier for this discount.
     *
     * @param discountID The new unique ID for the discount.
     */
    public void setDiscountID(String discountID)
    {
        this.discountID = discountID;
    }

    /**
     * Gets the unique identifier for this discount.
     *
     * @return The unique ID of the discount.
     */
    public String getDiscountID()
    {
        return discountID;
    }
}