package com.example.restaurantapp.models;

import java.util.List;

public class MenuItem
{

    private String name;
    private String description;
    private double price;
    private String imageURL;
    private String itemID;
    private String menuID;
    private String category;
    private List<Option> options;
    private List<RequiredCustomization> requiredCustomizations;
    private Boolean availability;
    private String status;
    private List<String> allergens;
    private Boolean isSpecialOffer;
    private int orderIndex;

    public MenuItem(String name, String description, double price, String imageURL, String itemID, String menuID,
                    String category, List<Option> options, List<RequiredCustomization> requiredCustomizations,
                    Boolean availability, String status, List<String> allergens, Boolean isSpecialOffer, int orderIndex)
    {
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageURL = imageURL;
        this.itemID = itemID;
        this.menuID = menuID;
        this.category = category;
        this.options = options;
        this.requiredCustomizations = requiredCustomizations;
        this.availability = availability;
        this.status = status;
        this.allergens = allergens;
        this.isSpecialOffer = isSpecialOffer;
        this.orderIndex = orderIndex;
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

    public String getItemID()
    {
        return itemID;
    }

    public void setItemID(String itemID)
    {
        this.itemID = itemID;
    }

    public String getMenuID()
    {
        return menuID;
    }

    public void setMenuID(String menuID)
    {
        this.menuID = menuID;
    }

    public String getCategory()
    {
        return category;
    }

    public void setCategory(String category)
    {
        this.category = category;
    }

    public List<Option> getOptions()
    {
        return options;
    }

    public void setOptions(List<Option> options)
    {
        this.options = options;
    }

    public List<RequiredCustomization> getRequiredCustomizations()
    {
        return requiredCustomizations;
    }

    public void setRequiredCustomizations(List<RequiredCustomization> requiredCustomizations)
    {
        this.requiredCustomizations = requiredCustomizations;
    }

    public Boolean getAvailability()
    {
        return availability;
    }

    public void setAvailability(Boolean availability)
    {
        this.availability = availability;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public List<String> getAllergens()
    {
        return allergens;
    }

    public void setAllergens(List<String> allergens)
    {
        this.allergens = allergens;
    }

    public Boolean getIsSpecialOffer()
    {
        return isSpecialOffer;
    }

    public void setIsSpecialOffer(Boolean isSpecialOffer)
    {
        this.isSpecialOffer = isSpecialOffer;
    }

    public int getOrderIndex()
    {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex)
    {
        this.orderIndex = orderIndex;
    }

    public static class Option
    {
        private String name; // The name of the option (e.g., "Extra Toppings")
        private String description; // A brief description of the option
        private List<com.example.restaurantapp.models.Option.OptionValue> values; // The possible values for this option
        private int maxSelection; // Maximum number of selections allowed (if applicable)

        public Option(String name, String description, List<com.example.restaurantapp.models.Option.OptionValue> values, int maxSelection)
        {
            this.name = name;
            this.description = description;
            this.values = values;
            this.maxSelection = maxSelection;
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

        public List<com.example.restaurantapp.models.Option.OptionValue> getValues()
        {
            return values;
        }

        public void setValues(List<com.example.restaurantapp.models.Option.OptionValue> values)
        {
            this.values = values;
        }

        public int getMaxSelection()
        {
            return maxSelection;
        }

        public void setMaxSelection(int maxSelection)
        {
            this.maxSelection = maxSelection;
        }

        public static class OptionValue
        {
            private String name; // Name of the option value (e.g., "Extra Cheese")
            private double additionalPrice; // Additional price for this option value

            public OptionValue(String name, double additionalPrice)
            {
                this.name = name;
                this.additionalPrice = additionalPrice;
            }

            public String getName()
            {
                return name;
            }

            public void setName(String name)
            {
                this.name = name;
            }

            public double getAdditionalPrice()
            {
                return additionalPrice;
            }

            public void setAdditionalPrice(double additionalPrice)
            {
                this.additionalPrice = additionalPrice;
            }

        }
    }

    public static class RequiredCustomization
    {
        private String name;
        private String description;
        private List<Option> options;
        private boolean isRequired;

        public RequiredCustomization(String name, String description, List<Option> options, boolean isRequired)
        {
            this.name = name;
            this.description = description;
            this.options = options;
            this.isRequired = isRequired;
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

        public List<Option> getOptions()
        {
            return options;
        }

        public void setOptions(List<Option> options)
        {
            this.options = options;
        }

        public boolean isRequired()
        {
            return isRequired;
        }

        public void setRequired(boolean required)
        {
            isRequired = required;
        }
    }
}