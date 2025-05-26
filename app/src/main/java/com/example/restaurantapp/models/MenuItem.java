package com.example.restaurantapp.models;

import java.util.List;

public class MenuItem
{

    private String name;
    private String description;
    private double price;
    private String imageURL;
    private String menuID;
    private String restaurantID;
    private String category;
    private Boolean availability;
    private List<String> allergens;
    private int orderIndex;
    private String itemID;

    public MenuItem()
    {
    }

    public MenuItem(String name, String description, double price, String imageURL, String menuID, String restaurantID,
                    String category, Boolean availability, List<String> allergens, int orderIndex, String itemID)
    {
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageURL = imageURL;
        this.menuID = menuID;
        this.restaurantID = restaurantID;
        this.category = category;
        this.availability = availability;
        this.allergens = allergens;
        this.orderIndex = orderIndex;
        this.itemID = itemID;
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

    public double getPrice()
    {
        return price;
    }

    public void setPrice(double price)
    {
        this.price = price;
    }

    public String getImageURL()
    {
        return imageURL;
    }

    public void setImageURL(String imageURL)
    {
        this.imageURL = imageURL;
    }

    public String getMenuID()
    {
        return menuID;
    }

    public void setMenuID(String menuID)
    {
        this.menuID = menuID;
    }

    public String getRestaurantID()
    {
        return restaurantID;
    }

    public void setRestaurantID(String restaurantID)
    {
        this.restaurantID = restaurantID;
    }

    public String getCategory()
    {
        return category;
    }

    public void setCategory(String category)
    {
        this.category = category;
    }

    public Boolean getAvailability()
    {
        return availability;
    }

    public void setAvailability(Boolean availability)
    {
        this.availability = availability;
    }

    public List<String> getAllergens()
    {
        return allergens;
    }

    public void setAllergens(List<String> allergens)
    {
        this.allergens = allergens;
    }

    public int getOrderIndex()
    {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex)
    {
        this.orderIndex = orderIndex;
    }

    public String getItemID()
    {
        return itemID;
    }

    public void setItemID(String itemID)
    {
        this.itemID = itemID;
    }
}