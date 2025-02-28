package com.example.restaurantapp.models;

import com.google.firebase.Timestamp;

import java.util.List;

public class Menu
{
    private String menuID;
    private String name;
    private String description;
    private String imageURL;
    private Timestamp timeCreated;
    private List<MenuItem> items;
    private List<String> tags;
    private String restaurantID;

    public Menu() {}

    public Menu(String menuID, String name, String description, String imageURL, Timestamp timeCreated,
                List<MenuItem> items, List<String> tags, String restaurantID, String type)
    {
        this.menuID = menuID;
        this.name = name;
        this.description = description;
        this.imageURL = imageURL;
        this.timeCreated = timeCreated;
        this.items = items;
        this.tags = tags;
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

    public List<MenuItem> getItems()
    {
        return items;
    }

    public void setItems(List<MenuItem> items)
    {
        this.items = items;
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


    @Override
    public String toString()
    {
        return "Menu{" +
                "menuID='" + menuID + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", imageURL='" + imageURL + '\'' +
                ", timeCreated=" + timeCreated +
                ", items=" + items +
                ", tags=" + tags +
                ", restaurantID='" + restaurantID + '\'' +
                '}';
    }
}
