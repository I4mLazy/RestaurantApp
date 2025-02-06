package com.example.restaurantapp.models;

public class ContactInfo
{
    private String phone;
    private String email;
    private String website;
    private String facebook;
    private String instagram;
    private String twitter;
    private String tiktok;

    public ContactInfo(String phone, String email, String website, String facebook, String instagram, String twitter, String tiktok)
    {
        this.phone = phone;
        this.email = email;
        this.website = website;
        this.facebook = facebook;
        this.instagram = instagram;
        this.twitter = twitter;
        this.tiktok = tiktok;
    }

    public String getPhone()
    {
        return phone;
    }

    public void setPhone(String phone)
    {
        this.phone = phone;
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    public String getWebsite()
    {
        return website;
    }

    public void setWebsite(String website)
    {
        this.website = website;
    }

    public String getFacebook()
    {
        return facebook;
    }

    public void setFacebook(String facebook)
    {
        this.facebook = facebook;
    }

    public String getInstagram()
    {
        return instagram;
    }

    public void setInstagram(String instagram)
    {
        this.instagram = instagram;
    }

    public String getTwitter()
    {
        return twitter;
    }

    public void setTwitter(String twitter)
    {
        this.twitter = twitter;
    }

    public String getTiktok()
    {
        return tiktok;
    }

    public void setTiktok(String tiktok)
    {
        this.tiktok = tiktok;
    }
}
