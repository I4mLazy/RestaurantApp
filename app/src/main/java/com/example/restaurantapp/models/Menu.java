package com.example.restaurantapp.models;

import com.google.firebase.Timestamp;

import java.util.List;

public class Menu
{
    private String name;
    private String imageURL;
    private Timestamp timeCreated;
    private String restaurantID;
    private String menuID;
    private int menuIndex;

    public Menu()
    {
    }

    public Menu(String name, String imageURL, Timestamp timeCreated, String restaurantID, String menuID, int menuIndex)
    {
        this.name = name;
        this.imageURL = imageURL;
        this.timeCreated = timeCreated;
        this.restaurantID = restaurantID;
        this.menuID = menuID;
        this.menuIndex = menuIndex;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getImageURL()
    {
        return imageURL;
    }

    public void setImageURL(String imageURL)
    {
        this.imageURL = imageURL;
    }

    public Timestamp getTimeCreated()
    {
        return timeCreated;
    }

    public void setTimeCreated(Timestamp timeCreated)
    {
        this.timeCreated = timeCreated;
    }

    public String getRestaurantID()
    {
        return restaurantID;
    }

    public void setRestaurantID(String restaurantID)
    {
        this.restaurantID = restaurantID;
    }

    public String getMenuID()
    {
        return menuID;
    }

    public void setMenuID(String menuID)
    {
        this.menuID = menuID;
    }

    public int getMenuIndex()
    {
        return menuIndex;
    }

    public void setMenuIndex(int menuIndex)
    {
        this.menuIndex = menuIndex;
    }


}