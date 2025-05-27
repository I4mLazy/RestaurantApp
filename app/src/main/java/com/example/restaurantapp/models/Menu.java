package com.example.restaurantapp.models;

import com.google.firebase.Timestamp;

// import java.util.List; // Unused import in this class

/**
 * Represents a menu within a restaurant.
 * Contains details such as the menu's name, an image URL, the time it was created,
 * the ID of the restaurant it belongs to, its own unique ID, and an index for ordering.
 */
public class Menu
{
    /**
     * The name of the menu (e.g., "Lunch Menu", "Desserts").
     */
    private String name;
    /**
     * The URL of an image representing the menu.
     */
    private String imageURL;
    /**
     * The {@link Timestamp} indicating when the menu was created.
     */
    private Timestamp timeCreated;
    /**
     * The unique identifier of the restaurant to which this menu belongs.
     */
    private String restaurantID;
    /**
     * The unique identifier for this menu.
     */
    private String menuID;
    /**
     * An integer index used for ordering or sequencing menus.
     */
    private int menuIndex;

    /**
     * Default constructor required for calls to DataSnapshot.getValue(Menu.class)
     * when using Firebase Realtime Database or for Firestore object mapping.
     */
    public Menu()
    {
        // Default constructor
    }

    /**
     * Constructs a new {@code Menu} object with specified details.
     *
     * @param name         The name of the menu.
     * @param imageURL     The URL of the menu's image.
     * @param timeCreated  The timestamp of when the menu was created.
     * @param restaurantID The ID of the restaurant this menu belongs to.
     * @param menuID       The unique ID of this menu.
     * @param menuIndex    The index for ordering this menu.
     */
    public Menu(String name, String imageURL, Timestamp timeCreated, String restaurantID, String menuID, int menuIndex)
    {
        this.name = name;
        this.imageURL = imageURL;
        this.timeCreated = timeCreated;
        this.restaurantID = restaurantID;
        this.menuID = menuID;
        this.menuIndex = menuIndex;
    }

    /**
     * Gets the name of the menu.
     *
     * @return The name of the menu.
     */
    public String getName()
    {
        return name;
    }

    /**
     * Sets the name of the menu.
     *
     * @param name The new name for the menu.
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * Gets the image URL for the menu.
     *
     * @return The URL string of the menu's image.
     */
    public String getImageURL()
    {
        return imageURL;
    }

    /**
     * Sets the image URL for the menu.
     *
     * @param imageURL The new image URL string.
     */
    public void setImageURL(String imageURL)
    {
        this.imageURL = imageURL;
    }

    /**
     * Gets the timestamp indicating when the menu was created.
     *
     * @return The {@link Timestamp} of creation.
     */
    public Timestamp getTimeCreated()
    {
        return timeCreated;
    }

    /**
     * Sets the timestamp indicating when the menu was created.
     *
     * @param timeCreated The new creation timestamp.
     */
    public void setTimeCreated(Timestamp timeCreated)
    {
        this.timeCreated = timeCreated;
    }

    /**
     * Gets the ID of the restaurant to which this menu belongs.
     *
     * @return The restaurant ID string.
     */
    public String getRestaurantID()
    {
        return restaurantID;
    }

    /**
     * Sets the ID of the restaurant to which this menu belongs.
     *
     * @param restaurantID The new restaurant ID string.
     */
    public void setRestaurantID(String restaurantID)
    {
        this.restaurantID = restaurantID;
    }

    /**
     * Gets the unique ID of this menu.
     *
     * @return The menu ID string.
     */
    public String getMenuID()
    {
        return menuID;
    }

    /**
     * Sets the unique ID of this menu.
     *
     * @param menuID The new menu ID string.
     */
    public void setMenuID(String menuID)
    {
        this.menuID = menuID;
    }

    /**
     * Gets the index used for ordering or sequencing this menu.
     *
     * @return The menu index as an integer.
     */
    public int getMenuIndex()
    {
        return menuIndex;
    }

    /**
     * Sets the index used for ordering or sequencing this menu.
     *
     * @param menuIndex The new menu index.
     */
    public void setMenuIndex(int menuIndex)
    {
        this.menuIndex = menuIndex;
    }
}