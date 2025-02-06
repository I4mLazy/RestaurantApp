package com.example.restaurantapp.models;

import com.google.firebase.Timestamp;

import java.util.List;

public class Restaurant
{
    private String name;
    private String address;
    private String location;
    private double rating;
    private String imageUrl;
    private String restaurantID;
    private BusinessHours businessHours;
    private Timestamp createdAt;
    private ContactInfo contactInfo;
    private boolean reservable;
    private List<String> type;
    private List<String> tags;
    private int priceLevel;
    private Menu menu;

    public Restaurant() {}
    public Restaurant(String name, String address, String location, double rating, String imageUrl, String restaurantID,
                      BusinessHours businessHours, Timestamp createdAt, ContactInfo contactInfo, boolean reservable,
                      List<String> type, List<String> tags, int priceLevel, Menu menu)
    {
        this.name = name;
        this.address = address;
        this.location = location;
        this.rating = rating;
        this.imageUrl = imageUrl;
        this.restaurantID = restaurantID;
        this.businessHours = businessHours;
        this.createdAt = createdAt;
        this.contactInfo = contactInfo;
        this.reservable = reservable;
        this.type = type;
        this.tags = tags;
        this.priceLevel = priceLevel;
        this.menu = menu;
    }

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

    public String getLocation()
    {
        return location;
    }

    public void setLocation(String location)
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

    public String getImageUrl()
    {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl)
    {
        this.imageUrl = imageUrl;
    }

    public String getRestaurantID()
    {
        return restaurantID;
    }

    public void setRestaurantID(String restaurantID)
    {
        this.restaurantID = restaurantID;
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

    public ContactInfo getContactInfo()
    {
        return contactInfo;
    }

    public void setContactInfo(ContactInfo contactInfo)
    {
        this.contactInfo = contactInfo;
    }

    public boolean isReservable()
    {
        return reservable;
    }

    public void setReservable(boolean reservable)
    {
        this.reservable = reservable;
    }

    public List<String> getType()
    {
        return type;
    }

    public void setType(List<String> type)
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

    public Menu getMenu()
    {
        return menu;
    }

    public void setMenu(Menu menu)
    {
        this.menu = menu;
    }
}

