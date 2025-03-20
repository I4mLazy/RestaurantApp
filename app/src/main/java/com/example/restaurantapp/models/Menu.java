package com.example.restaurantapp.models;

import com.google.firebase.Timestamp;

import java.util.List;

public class Menu
{
    private String name;
    private String description;
    private String imageURL;
    private Timestamp timeCreated;
    private List<String> tags;
    private String restaurantID;
    private String menuID;
    private int menuIndex;

    public Menu() {
    }

    public Menu(String name, String description, String imageURL, Timestamp timeCreated,
                List<String> tags, String restaurantID, String menuID, int menuIndex)
    {
        this.name = name;
        this.description = description;
        this.imageURL = imageURL;
        this.timeCreated = timeCreated;
        this.tags = tags;
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

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
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

    public List<String> getTags()
    {
        return tags;
    }

    public void setTags(List<String> tags)
    {
        this.tags = tags;
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