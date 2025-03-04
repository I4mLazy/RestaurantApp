package com.example.restaurantapp.models;

import java.util.List;

public class MenuItem {

    private String name;
    private String description;
    private double price;
    private String imageURL;
    private String menuID;
    private String category;
    private List<Option> options;
    private List<RequiredCustomization> requiredCustomizations;
    private Boolean availability;
    private String status;
    private List<String> allergens;
    private Boolean isSpecialOffer;
    private int orderIndex;
    private int maxSelection;
    private String itemID;

    public MenuItem(){}
    public MenuItem(String name, String description, double price, String imageURL, String menuID,
                    String category, List<Option> options, List<RequiredCustomization> requiredCustomizations,
                    Boolean availability, String status, List<String> allergens, Boolean isSpecialOffer, int orderIndex, int maxSelection, String itemID) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageURL = imageURL;
        this.menuID = menuID;
        this.category = category;
        this.options = options;
        this.requiredCustomizations = requiredCustomizations;
        this.availability = availability;
        this.status = status;
        this.allergens = allergens;
        this.isSpecialOffer = isSpecialOffer;
        this.orderIndex = orderIndex;
        this.maxSelection = maxSelection;
        this.itemID = itemID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getImageURL() {
        return imageURL;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }
    public String getMenuID() {
        return menuID;
    }

    public void setMenuID(String menuID) {
        this.menuID = menuID;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<Option> getOptions() {
        return options;
    }

    public void setOptions(List<Option> options) {
        this.options = options;
    }

    public List<RequiredCustomization> getRequiredCustomizations() {
        return requiredCustomizations;
    }

    public void setRequiredCustomizations(List<RequiredCustomization> requiredCustomizations) {
        this.requiredCustomizations = requiredCustomizations;
    }

    public Boolean getAvailability() {
        return availability;
    }

    public void setAvailability(Boolean availability) {
        this.availability = availability;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getAllergens() {
        return allergens;
    }

    public void setAllergens(List<String> allergens) {
        this.allergens = allergens;
    }

    public Boolean getIsSpecialOffer() {
        return isSpecialOffer;
    }

    public void setIsSpecialOffer(Boolean isSpecialOffer) {
        this.isSpecialOffer = isSpecialOffer;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }

    public void setMaxSelection(int maxSelection) {
        this.maxSelection = maxSelection;
    }

    public int getMaxSelection() {
        return maxSelection;
    }

    public String getItemID()
    {
        return itemID;
    }

    public void setItemID()
    {
        this.itemID = itemID;
    }
    public static class Option {
        private String name; // The name of the option (e.g., "Extra Toppings")
        private String description; // A brief description of the option
        private double price; // The price of this option

        public Option(String name, String description, double price) {
            this.name = name;
            this.description = description;
            this.price = price;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public double getPrice() {
            return price;
        }

        public void setPrice(double price) {
            this.price = price;
        }
    }

    public static class RequiredCustomization {
        private String name;
        private String description;
        private List<Option> options; // Now a list of Option objects
        private int maxSelection; // Maximum number of selections allowed

        public RequiredCustomization(String name, String description, List<Option> options, int maxSelection) {
            this.name = name;
            this.description = description;
            this.options = options;
            this.maxSelection = maxSelection;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public List<Option> getOptions() {
            return options;
        }

        public void setOptions(List<Option> options) {
            this.options = options;
        }

        public int getMaxSelection() {
            return maxSelection;
        }

        public void setMaxSelection(int maxSelection) {
            this.maxSelection = maxSelection;
        }
    }
}