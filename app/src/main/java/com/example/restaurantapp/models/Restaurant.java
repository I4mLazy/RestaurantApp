package com.example.restaurantapp.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;

import java.util.List;
import java.util.Map;

/**
 * Represents a restaurant with its various details.
 * This includes information such as name, address, geographical location, rating,
 * image URL, business hours, creation timestamp, contact information, operational flags
 * (reservable, offers pickup), type/cuisine, tags, price level, description,
 * last update timestamp, and maximum capacity.
 */
public class Restaurant
{
    /**
     * The name of the restaurant.
     */
    private String name;
    /**
     * The physical address of the restaurant.
     */
    private String address;
    /**
     * The geographical location (latitude and longitude) of the restaurant.
     */
    private GeoPoint location;
    /**
     * The average user rating for the restaurant.
     */
    private double averageRating;
    /**
     * The total number of ratings received by the restaurant.
     */
    private int ratingsCount;
    /**
     * The URL of an image representing the restaurant (e.g., logo or storefront).
     */
    private String imageURL;
    /**
     * A string describing the business hours of the restaurant.
     */
    private String businessHours;
    /**
     * The {@link Timestamp} indicating when the restaurant record was created.
     */
    private Timestamp createdAt;
    /**
     * A boolean indicating whether the restaurant accepts reservations. {@code true} if reservable, {@code false} otherwise.
     */
    private boolean reservable;
    /**
     * The type or cuisine of the restaurant (e.g., "Italian", "Cafe", "Fast Food").
     */
    private String type;
    /**
     * A list of tags or keywords associated with the restaurant (e.g., "vegetarian-friendly", "outdoor seating").
     */
    private List<String> tags;
    /**
     * An integer representing the price level of the restaurant (e.g., 1 for $, 2 for $$, etc.).
     */
    private int priceLevel;
    /**
     * The unique identifier for this restaurant.
     */
    private String restaurantID;
    /**
     * A textual description of the restaurant.
     */
    private String description;
    /**
     * The {@link Timestamp} indicating when the restaurant's information was last updated.
     */
    private Timestamp lastUpdated;
    /**
     * A boolean indicating whether the restaurant offers pickup services. {@code true} if it offers pickup, {@code false} otherwise.
     */
    private boolean offersPickup;
    /**
     * A map containing contact information for the restaurant (e.g., "phone": "123-456-7890", "email": "info@example.com").
     */
    private Map<String, String> contactInfo;
    /**
     * The maximum seating capacity of the restaurant.
     */
    private int maxCapacity;


    /**
     * Default constructor required for calls to DataSnapshot.getValue(Restaurant.class)
     * when using Firebase Realtime Database or for Firestore object mapping.
     */
    public Restaurant()
    {
        // Default constructor
    }

    /**
     * Constructs a new {@code Restaurant} object with specified details.
     *
     * @param name          The name of the restaurant.
     * @param address       The address of the restaurant.
     * @param location      The geographical location (GeoPoint).
     * @param averageRating The average user rating.
     * @param ratingsCount  The total number of ratings.
     * @param imageURL      The URL of the restaurant's image.
     * @param businessHours The business hours of the restaurant.
     * @param createdAt     The timestamp of creation.
     * @param reservable    True if the restaurant is reservable, false otherwise.
     * @param type          The type or cuisine of the restaurant.
     * @param tags          A list of tags for the restaurant.
     * @param priceLevel    The price level of the restaurant.
     * @param restaurantID  The unique ID of the restaurant.
     * @param description   A description of the restaurant.
     * @param lastUpdated   The timestamp of the last update.
     * @param offersPickup  True if the restaurant offers pickup, false otherwise.
     * @param contactInfo   A map of contact information.
     * @param maxCapacity   The maximum seating capacity.
     */
    public Restaurant(String name, String address, GeoPoint location, double averageRating, int ratingsCount, String imageURL,
                      String businessHours, Timestamp createdAt, boolean reservable, String type,
                      List<String> tags, int priceLevel, String restaurantID, String description,
                      Timestamp lastUpdated, boolean offersPickup, Map<String, String> contactInfo, int maxCapacity)
    {
        this.name = name;
        this.address = address;
        this.location = location;
        this.averageRating = averageRating;
        this.ratingsCount = ratingsCount;
        this.imageURL = imageURL;
        this.businessHours = businessHours;
        this.createdAt = createdAt;
        this.contactInfo = contactInfo;
        this.reservable = reservable;
        this.type = type;
        this.tags = tags;
        this.priceLevel = priceLevel;
        this.restaurantID = restaurantID;
        this.description = description;
        this.lastUpdated = lastUpdated;
        this.offersPickup = offersPickup;
        this.maxCapacity = maxCapacity;
    }

    // ... (Getters and setters) ...

    /**
     * Gets the name of the restaurant.
     *
     * @return The name of the restaurant.
     */
    public String getName()
    {
        return name;
    }

    /**
     * Sets the name of the restaurant.
     *
     * @param name The new name for the restaurant.
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * Gets the address of the restaurant.
     *
     * @return The address of the restaurant.
     */
    public String getAddress()
    {
        return address;
    }

    /**
     * Sets the address of the restaurant.
     *
     * @param address The new address for the restaurant.
     */
    public void setAddress(String address)
    {
        this.address = address;
    }

    /**
     * Gets the geographical location (latitude and longitude) of the restaurant.
     *
     * @return The {@link GeoPoint} representing the location.
     */
    public GeoPoint getLocation()
    {
        return location;
    }

    /**
     * Sets the geographical location of the restaurant.
     *
     * @param location The new {@link GeoPoint} for the location.
     */
    public void setLocation(GeoPoint location)
    {
        this.location = location;
    }

    /**
     * Gets the average user rating for the restaurant.
     *
     * @return The average rating.
     */
    public double getAverageRating()
    {
        return averageRating;
    }

    /**
     * Sets the average user rating for the restaurant.
     *
     * @param averageRating The new average rating.
     */
    public void setAverageRating(double averageRating)
    {
        this.averageRating = averageRating;
    }

    /**
     * Gets the total number of ratings received by the restaurant.
     *
     * @return The total number of ratings.
     */
    public int getRatingsCount()
    {
        return ratingsCount;
    }

    /**
     * Sets the total number of ratings received by the restaurant.
     *
     * @param ratingsCount The new total number of ratings.
     */
    public void setRatingsCount(int ratingsCount)
    {
        this.ratingsCount = ratingsCount;
    }

    /**
     * Gets the image URL for the restaurant.
     *
     * @return The URL string of the restaurant's image.
     */
    public String getImageURL()
    {
        return imageURL;
    }

    /**
     * Sets the image URL for the restaurant.
     *
     * @param imageURL The new image URL string.
     */
    public void setImageURL(String imageURL)
    {
        this.imageURL = imageURL;
    }

    /**
     * Gets the business hours of the restaurant.
     *
     * @return A string describing the business hours.
     */
    public String getBusinessHours()
    {
        return businessHours;
    }

    /**
     * Sets the business hours of the restaurant.
     *
     * @param businessHours The new string describing business hours.
     */
    public void setBusinessHours(String businessHours)
    {
        this.businessHours = businessHours;
    }

    /**
     * Gets the timestamp indicating when the restaurant record was created.
     *
     * @return The {@link Timestamp} of creation.
     */
    public Timestamp getCreatedAt()
    {
        return createdAt;
    }

    /**
     * Sets the timestamp indicating when the restaurant record was created.
     *
     * @param createdAt The new creation timestamp.
     */
    public void setCreatedAt(Timestamp createdAt)
    {
        this.createdAt = createdAt;
    }

    /**
     * Checks if the restaurant accepts reservations.
     *
     * @return {@code true} if the restaurant is reservable, {@code false} otherwise.
     */
    public boolean isReservable() // Getter for boolean often uses "is" prefix
    {
        return reservable;
    }

    /**
     * Sets whether the restaurant accepts reservations.
     *
     * @param reservable {@code true} if reservable, {@code false} otherwise.
     */
    public void setReservable(boolean reservable)
    {
        this.reservable = reservable;
    }

    /**
     * Gets the type or cuisine of the restaurant.
     *
     * @return The type string (e.g., "Italian", "Cafe").
     */
    public String getType()
    {
        return type;
    }

    /**
     * Sets the type or cuisine of the restaurant.
     *
     * @param type The new type string.
     */
    public void setType(String type)
    {
        this.type = type;
    }

    /**
     * Gets the list of tags associated with the restaurant.
     *
     * @return A list of strings, where each string is a tag. Can be null or empty.
     */
    public List<String> getTags()
    {
        return tags;
    }

    /**
     * Sets the list of tags associated with the restaurant.
     *
     * @param tags The new list of tag strings.
     */
    public void setTags(List<String> tags)
    {
        this.tags = tags;
    }

    /**
     * Gets the price level of the restaurant.
     *
     * @return An integer representing the price level (e.g., 1 for $, 2 for $$).
     */
    public int getPriceLevel()
    {
        return priceLevel;
    }

    /**
     * Sets the price level of the restaurant.
     *
     * @param priceLevel The new price level integer.
     */
    public void setPriceLevel(int priceLevel)
    {
        this.priceLevel = priceLevel;
    }

    /**
     * Gets the unique identifier for this restaurant.
     *
     * @return The restaurant ID string.
     */
    public String getRestaurantID()
    {
        return restaurantID;
    }

    /**
     * Sets the unique identifier for this restaurant.
     * Note: The original implementation {@code this.restaurantID = this.restaurantID;} appears to be a typo
     * and should likely be {@code this.restaurantID = restaurantID;}.
     * The Javadoc describes the intended behavior.
     *
     * @param restaurantID The new restaurant ID string.
     */
    public void setRestaurantID(String restaurantID)
    {
        this.restaurantID = this.restaurantID; // Original code: this.restaurantID = this.restaurantID;
    }

    /**
     * Gets the description of the restaurant.
     *
     * @return The description string.
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * Sets the description of the restaurant.
     *
     * @param description The new description string.
     */
    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * Gets the timestamp indicating when the restaurant's information was last updated.
     *
     * @return The {@link Timestamp} of the last update.
     */
    public Timestamp getLastUpdated()
    {
        return lastUpdated;
    }

    /**
     * Sets the timestamp indicating when the restaurant's information was last updated.
     *
     * @param lastUpdated The new last update timestamp.
     */
    public void setLastUpdated(Timestamp lastUpdated)
    {
        this.lastUpdated = lastUpdated;
    }

    /**
     * Checks if the restaurant offers pickup services.
     *
     * @return {@code true} if the restaurant offers pickup, {@code false} otherwise.
     */
    public boolean isOffersPickup() // Getter for boolean
    {
        return offersPickup;
    }

    /**
     * Sets whether the restaurant offers pickup services.
     *
     * @param offersPickup {@code true} if it offers pickup, {@code false} otherwise.
     */
    public void setOffersPickup(boolean offersPickup)
    {
        this.offersPickup = offersPickup;
    }

    /**
     * Gets the contact information for the restaurant.
     *
     * @return A map where keys are contact types (e.g., "phone", "email") and values are the contact details.
     * Can be null or empty.
     */
    public Map<String, String> getContactInfo()
    {
        return contactInfo;
    }

    /**
     * Sets the contact information for the restaurant.
     *
     * @param contactInfo The new map of contact information.
     */
    public void setContactInfo(Map<String, String> contactInfo)
    {
        this.contactInfo = contactInfo;
    }

    /**
     * Gets the maximum seating capacity of the restaurant.
     *
     * @return The maximum capacity as an integer.
     */
    public int getMaxCapacity()
    {
        return maxCapacity;
    }

    /**
     * Sets the maximum seating capacity of the restaurant.
     *
     * @param maxCapacity The new maximum capacity.
     */
    public void setMaxCapacity(int maxCapacity)
    {
        this.maxCapacity = maxCapacity;
    }
}