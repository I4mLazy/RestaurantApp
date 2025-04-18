package com.example.restaurantapp.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;

import java.util.List;
import java.util.Map;

public class Restaurant
{
    private String name;
    private String address;
    private GeoPoint location;
    private double rating;
    private String imageURL;
    private BusinessHours businessHours;
    private Timestamp createdAt;
    private boolean reservable;
    private String type;
    private List<String> tags;
    private int priceLevel;
    private String restaurantID;
    private String description;
    private Timestamp lastUpdated;
    private boolean offersPickup;
    private Map<String, String> contactInfo;
    private int maxCapacity;


    public Restaurant()
    {
    }

    public Restaurant(String name, String address, GeoPoint location, double rating, String imageURL,
                      BusinessHours businessHours, Timestamp createdAt, boolean reservable, String type,
                      List<String> tags, int priceLevel, String restaurantID, String description,
                      Timestamp lastUpdated, boolean offersPickup, Map<String, String> contactInfo, int maxCapacity)
    {
        this.name = name;
        this.address = address;
        this.location = location;
        this.rating = rating;
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

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getAddress()
    {
        return address;
    }

    public void setAddress(String address)
    {
        this.address = address;
    }

    public GeoPoint getLocation()
    {
        return location;
    }

    public void setLocation(GeoPoint location)
    {
        this.location = location;
    }

    public double getRating()
    {
        return rating;
    }

    public void setRating(double rating)
    {
        this.rating = rating;
    }

    public String getImageURL()
    {
        return imageURL;
    }

    public void setImageURL(String imageURL)
    {
        this.imageURL = imageURL;
    }

    public BusinessHours getBusinessHours()
    {
        return businessHours;
    }

    public void setBusinessHours(BusinessHours businessHours)
    {
        this.businessHours = businessHours;
    }

    public Timestamp getCreatedAt()
    {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt)
    {
        this.createdAt = createdAt;
    }

    public boolean isReservable()
    {
        return reservable;
    }

    public void setReservable(boolean reservable)
    {
        this.reservable = reservable;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public List<String> getTags()
    {
        return tags;
    }

    public void setTags(List<String> tags)
    {
        this.tags = tags;
    }

    public int getPriceLevel()
    {
        return priceLevel;
    }

    public void setPriceLevel(int priceLevel)
    {
        this.priceLevel = priceLevel;
    }

    public String getRestaurantID()
    {
        return restaurantID;
    }

    public void setRestaurantID()
    {
        this.restaurantID = restaurantID;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public Timestamp getLastUpdated()
    {
        return lastUpdated;
    }

    public void setLastUpdated(Timestamp lastUpdated)
    {
        this.lastUpdated = lastUpdated;
    }

    public boolean isOffersPickup()
    {
        return offersPickup;
    }

    public void setOffersPickup(boolean offersPickup)
    {
        this.offersPickup = offersPickup;
    }

    public Map<String, String> getContactInfo()
    {
        return contactInfo;
    }

    public void setContactInfo(Map<String, String> contactInfo)
    {
        this.contactInfo = contactInfo;
    }

    public int getMaxCapacity()
    {
        return maxCapacity;
    }

    public void setMaxCapacity(int maxCapacity)
    {
        this.maxCapacity = maxCapacity;
    }
}