package com.example.restaurantapp.models;

import java.util.List;

public class Option
{
    private String name; // The name of the option (e.g., "Extra Toppings")
    private String description; // A brief description of the option
    private List<OptionValue> values; // The possible values for this option
    private int maxSelection; // Maximum number of selections allowed (if applicable)

    public Option(String name, String description, List<OptionValue> values, int maxSelection)
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

    public List<OptionValue> getValues()
    {
        return values;
    }

    public void setValues(List<OptionValue> values)
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



