package com.example.restaurantapp.models;

public class Rating
{
    private double rating;
    private long timestamp;

    public Rating()
    {
    }

    public Rating(double rating, long timestamp)
    {
        this.rating = rating;
        this.timestamp = timestamp;
    }

    public double getRating()
    {
        return rating;
    }

    public void setRating(double rating)
    {
        this.rating = rating;
    }

    public long getTimestamp()
    {
        return timestamp;
    }

    public void setTimestamp(long timestamp)
    {
        this.timestamp = timestamp;
    }
}

