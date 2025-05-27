package com.example.restaurantapp.models;

import java.util.List;

/**
 * Represents an individual item on a restaurant's menu.
 * Contains details such as the item's name, description, price, image URL,
 * the ID of the menu and restaurant it belongs to, its category, availability status,
 * a list of allergens, an order index for sequencing, and its own unique ID.
 */
public class MenuItem
{

    /**
     * The name of the menu item (e.g., "Cheeseburger", "Caesar Salad").
     */
    private String name;
    /**
     * A textual description of the menu item.
     */
    private String description;
    /**
     * The price of the menu item.
     */
    private double price;
    /**
     * The URL of an image representing the menu item.
     */
    private String imageURL;
    /**
     * The unique identifier of the menu to which this item belongs.
     */
    private String menuID;
    /**
     * The unique identifier of the restaurant to which this item belongs.
     */
    private String restaurantID;
    /**
     * The category of the menu item (e.g., "Appetizer", "Main Course", "Dessert").
     */
    private String category;
    /**
     * A boolean indicating whether the menu item is currently available. {@code true} if available, {@code false} otherwise. Can be null.
     */
    private Boolean availability;
    /**
     * A list of strings representing allergens present in the menu item (e.g., "nuts", "dairy", "gluten").
     */
    private List<String> allergens;
    /**
     * An integer index used for ordering or sequencing items within a menu.
     */
    private int orderIndex;
    /**
     * The unique identifier for this menu item.
     */
    private String itemID;

    /**
     * Default constructor required for calls to DataSnapshot.getValue(MenuItem.class)
     * when using Firebase Realtime Database or for Firestore object mapping.
     */
    public MenuItem()
    {
        // Default constructor
    }

    /**
     * Constructs a new {@code MenuItem} object with specified details.
     *
     * @param name         The name of the menu item.
     * @param description  The description of the menu item.
     * @param price        The price of the menu item.
     * @param imageURL     The URL of the item's image.
     * @param menuID       The ID of the menu this item belongs to.
     * @param restaurantID The ID of the restaurant this item belongs to.
     * @param category     The category of the menu item.
     * @param availability The availability status of the item (true if available, false otherwise).
     * @param allergens    A list of allergens for the item.
     * @param orderIndex   The index for ordering this item within its menu.
     * @param itemID       The unique ID of this menu item.
     */
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

    /**
     * Gets the name of the menu item.
     *
     * @return The name of the menu item.
     */
    public String getName()
    {
        return name;
    }

    /**
     * Sets the name of the menu item.
     *
     * @param name The new name for the menu item.
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * Gets the description of the menu item.
     *
     * @return The description of the menu item.
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * Sets the description of the menu item.
     *
     * @param description The new description for the menu item.
     */
    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * Gets the price of the menu item.
     *
     * @return The price of the menu item.
     */
    public double getPrice()
    {
        return price;
    }

    /**
     * Sets the price of the menu item.
     *
     * @param price The new price for the menu item.
     */
    public void setPrice(double price)
    {
        this.price = price;
    }

    /**
     * Gets the image URL for the menu item.
     *
     * @return The URL string of the item's image.
     */
    public String getImageURL()
    {
        return imageURL;
    }

    /**
     * Sets the image URL for the menu item.
     *
     * @param imageURL The new image URL string.
     */
    public void setImageURL(String imageURL)
    {
        this.imageURL = imageURL;
    }

    /**
     * Gets the ID of the menu to which this item belongs.
     *
     * @return The menu ID string.
     */
    public String getMenuID()
    {
        return menuID;
    }

    /**
     * Sets the ID of the menu to which this item belongs.
     *
     * @param menuID The new menu ID string.
     */
    public void setMenuID(String menuID)
    {
        this.menuID = menuID;
    }

    /**
     * Gets the ID of the restaurant to which this item belongs.
     *
     * @return The restaurant ID string.
     */
    public String getRestaurantID()
    {
        return restaurantID;
    }

    /**
     * Sets the ID of the restaurant to which this item belongs.
     *
     * @param restaurantID The new restaurant ID string.
     */
    public void setRestaurantID(String restaurantID)
    {
        this.restaurantID = restaurantID;
    }

    /**
     * Gets the category of the menu item.
     *
     * @return The category string (e.g., "Appetizer", "Main Course").
     */
    public String getCategory()
    {
        return category;
    }

    /**
     * Sets the category of the menu item.
     *
     * @param category The new category string.
     */
    public void setCategory(String category)
    {
        this.category = category;
    }

    /**
     * Gets the availability status of the menu item.
     *
     * @return A {@link Boolean} indicating availability: {@code true} if available,
     * {@code false} if not, or {@code null} if not set.
     */
    public Boolean getAvailability()
    {
        return availability;
    }

    /**
     * Sets the availability status of the menu item.
     *
     * @param availability The new availability status (true, false, or null).
     */
    public void setAvailability(Boolean availability)
    {
        this.availability = availability;
    }

    /**
     * Gets the list of allergens for the menu item.
     *
     * @return A list of strings, where each string is an allergen. Can be null or empty.
     */
    public List<String> getAllergens()
    {
        return allergens;
    }

    /**
     * Sets the list of allergens for the menu item.
     *
     * @param allergens The new list of allergen strings.
     */
    public void setAllergens(List<String> allergens)
    {
        this.allergens = allergens;
    }

    /**
     * Gets the order index of this item within its menu.
     *
     * @return The order index as an integer.
     */
    public int getOrderIndex()
    {
        return orderIndex;
    }

    /**
     * Sets the order index of this item within its menu.
     *
     * @param orderIndex The new order index.
     */
    public void setOrderIndex(int orderIndex)
    {
        this.orderIndex = orderIndex;
    }

    /**
     * Gets the unique ID of this menu item.
     *
     * @return The item ID string.
     */
    public String getItemID()
    {
        return itemID;
    }

    /**
     * Sets the unique ID of this menu item.
     *
     * @param itemID The new item ID string.
     */
    public void setItemID(String itemID)
    {
        this.itemID = itemID;
    }
}