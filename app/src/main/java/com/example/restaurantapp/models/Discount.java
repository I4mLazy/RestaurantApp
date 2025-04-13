package com.example.restaurantapp.models;

import com.google.firebase.Timestamp;

public class Discount
{
    private String discountType; // "Percentage" or "Flat"
    private double discountValue;
    private Timestamp startTime;
    private Timestamp endTime;
    private String discountID;

    public Discount()
    {
    }

    public Discount(String discountType, double discountValue, Timestamp startTime, Timestamp endTime, String discountID)
    {
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.startTime = startTime;
        this.endTime = endTime;
        this.discountID = discountID;
    }

    public String getDiscountType()
    {
        return discountType;
    }

    public void setDiscountType(String discountType)
    {
        this.discountType = discountType;
    }

    public double getDiscountValue()
    {
        return discountValue;
    }

    public void setDiscountValue(double discountValue)
    {
        this.discountValue = discountValue;
    }

    public Timestamp getStartTime()
    {
        return startTime;
    }

    public void setStartTime(Timestamp startTime)
    {
        this.startTime = startTime;
    }

    public Timestamp getEndTime()
    {
        return endTime;
    }

    public void setEndTime(Timestamp endTime)
    {
        this.endTime = endTime;
    }

    public void setDiscountID(String discountID)
    {
        this.discountID = discountID;
    }

    public String getDiscountID()
    {
        return discountID;
    }
}

