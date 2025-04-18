package com.example.restaurantapp.models;

import java.util.Date;

public class Reservation
{
    private Date date;
    private String time;
    private String guests;
    private String specialRequests;
    private String status;
    private String userName;
    private String phoneNumber;
    private String userID;
    private String restaurantID;
    private String reservationID;

    public Reservation()
    {
    }

    public Reservation(Date date, String time, String guests, String specialRequests, String status, String userName, String phoneNumber, String userID, String restaurantID, String reservationID)
    {
        this.date = date;
        this.time = time;
        this.guests = guests;
        this.specialRequests = specialRequests;
        this.status = status;
        this.userName = userName;
        this.phoneNumber = phoneNumber;
        this.userID = userID;
        this.restaurantID = restaurantID;
        this.reservationID = reservationID;
    }

    // Getters
    public Date getDate()
    {
        return date;
    }

    public String getTime()
    {
        return time;
    }

    public String getGuests()
    {
        return guests;
    }

    public String getSpecialRequests()
    {
        return specialRequests;
    }

    public String getStatus()
    {
        return status;
    }

    public String getUserName()
    {
        return userName;
    }

    public String getPhoneNumber()
    {
        return phoneNumber;
    }

    // Setters
    public void setDate(Date date)
    {
        this.date = date;
    }

    public void setTime(String time)
    {
        this.time = time;
    }

    public void setGuests(String guests)
    {
        this.guests = guests;
    }

    public void setSpecialRequests(String specialRequests)
    {
        this.specialRequests = specialRequests;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public void setPhoneNumber(String phoneNumber)
    {
        this.phoneNumber = phoneNumber;
    }

    public String getUserID()
    {
        return userID;
    }

    public void setUserID(String userID)
    {
        this.userID = userID;
    }

    public String getRestaurantID()
    {
        return restaurantID;
    }

    public void setRestaurantID(String restaurantID)
    {
        this.restaurantID = restaurantID;
    }

    public String getReservationID()
    {
        return reservationID;
    }

    public void setReservationID(String reservationID)
    {
        this.reservationID = reservationID;
    }
}
