package com.example.restaurantapp.models;

import java.util.List;

public class BusinessHours
{
    private List<TimeRange> monday;
    private List<TimeRange> tuesday;
    private List<TimeRange> wednesday;
    private List<TimeRange> thursday;
    private List<TimeRange> friday;
    private List<TimeRange> saturday;
    private List<TimeRange> sunday;
    private SpecialHours special_hours;

    public BusinessHours(List<TimeRange> monday, List<TimeRange> tuesday, List<TimeRange> wednesday,
                         List<TimeRange> thursday, List<TimeRange> friday, List<TimeRange> saturday,
                         List<TimeRange> sunday, SpecialHours special_hours)
    {
        this.monday = monday;
        this.tuesday = tuesday;
        this.wednesday = wednesday;
        this.thursday = thursday;
        this.friday = friday;
        this.saturday = saturday;
        this.sunday = sunday;
        this.special_hours = special_hours;
    }

    public List<TimeRange> getMonday()
    {
        return monday;
    }

    public void setMonday(List<TimeRange> monday)
    {
        this.monday = monday;
    }

    public List<TimeRange> getTuesday()
    {
        return tuesday;
    }

    public void setTuesday(List<TimeRange> tuesday)
    {
        this.tuesday = tuesday;
    }

    public List<TimeRange> getWednesday()
    {
        return wednesday;
    }

    public void setWednesday(List<TimeRange> wednesday)
    {
        this.wednesday = wednesday;
    }

    public List<TimeRange> getThursday()
    {
        return thursday;
    }

    public void setThursday(List<TimeRange> thursday)
    {
        this.thursday = thursday;
    }

    public List<TimeRange> getFriday()
    {
        return friday;
    }

    public void setFriday(List<TimeRange> friday)
    {
        this.friday = friday;
    }

    public List<TimeRange> getSaturday()
    {
        return saturday;
    }

    public void setSaturday(List<TimeRange> saturday)
    {
        this.saturday = saturday;
    }

    public List<TimeRange> getSunday()
    {
        return sunday;
    }

    public void setSunday(List<TimeRange> sunday)
    {
        this.sunday = sunday;
    }

    public SpecialHours getSpecial_hours()
    {
        return special_hours;
    }

    public void setSpecial_hours(SpecialHours special_hours)
    {
        this.special_hours = special_hours;
    }

    public static class TimeRange
    {
        private String open;
        private String close;

        public TimeRange(String open, String close)
        {
            this.open = open;
            this.close = close;
        }

        public String getOpen()
        {
            return open;
        }

        public void setOpen(String open)
        {
            this.open = open;
        }

        public String getClose()
        {
            return close;
        }

        public void setClose(String close)
        {
            this.close = close;
        }
    }

    public static class SpecialHours
    {
        private boolean closed;
        private List<TimeRange> hours;

        public boolean isClosed()
        {
            return closed;
        }

        public void setClosed(boolean closed)
        {
            this.closed = closed;
        }

        public List<TimeRange> getHours()
        {
            return hours;
        }

        public void setHours(List<TimeRange> hours)
        {
            this.hours = hours;
        }
    }
}
