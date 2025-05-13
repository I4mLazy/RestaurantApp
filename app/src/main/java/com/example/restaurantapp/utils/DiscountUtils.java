package com.example.restaurantapp.utils;

import android.content.Context;

import com.example.restaurantapp.R;
import com.example.restaurantapp.models.MenuItem;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DiscountUtils
{

    public interface DiscountResultCallback
    {
        void onResult(double originalPrice, double finalPrice, boolean hasDiscount, boolean isFree, String badgeText);
    }

    public static void applyActiveDiscounts(MenuItem item, Context context, DiscountResultCallback callback)
    {
        FirebaseFirestore.getInstance()
                .collection("Restaurants")
                .document(item.getRestaurantID())
                .collection("Menus")
                .document(item.getMenuID())
                .collection("Items")
                .document(item.getItemID())
                .collection("Discounts")
                .orderBy("startTime")
                .get()
                .addOnSuccessListener(querySnapshot ->
                {
                    double originalPrice = item.getPrice();
                    double currentPrice = originalPrice;
                    double totalDiscount = 0;
                    boolean hasPercentageDiscount = false;
                    Date now = new Date();

                    List<QueryDocumentSnapshot> activeDiscounts = new ArrayList<>();
                    for(QueryDocumentSnapshot doc : querySnapshot)
                    {
                        String type = doc.getString("discountType");
                        double amount = doc.getDouble("amount") != null ? doc.getDouble("amount") : 0;
                        Timestamp start = doc.getTimestamp("startTime");
                        Timestamp end = doc.getTimestamp("endTime");

                        if(start != null && now.after(start.toDate()) &&
                                (end == null || now.before(end.toDate())))
                        {
                            activeDiscounts.add(doc);
                        }
                    }

                    for(QueryDocumentSnapshot doc : activeDiscounts)
                    {
                        String type = doc.getString("discountType");
                        double amount = doc.getDouble("amount") != null ? doc.getDouble("amount") : 0;

                        if("Flat".equals(type))
                        {
                            currentPrice -= amount;
                            totalDiscount += amount;
                        } else if("Percentage".equals(type))
                        {
                            currentPrice *= (1 - (amount / 100.0));
                            hasPercentageDiscount = true;
                        }
                    }

                    currentPrice = Math.max(currentPrice, 0);

                    boolean hasDiscount = originalPrice > currentPrice;
                    boolean isFree = currentPrice == 0;

                    String badgeText = null;
                    if(hasDiscount)
                    {
                        if(isFree)
                        {
                            badgeText = context.getString(R.string.free);
                        } else if(hasPercentageDiscount)
                        {
                            double percentage = (originalPrice - currentPrice) / originalPrice * 100;
                            badgeText = context.getString(R.string.percent_off_format, (int) percentage);
                        } else
                        {
                            badgeText = context.getString(R.string.amount_off_format, (int) totalDiscount);
                        }
                    }

                    callback.onResult(originalPrice, currentPrice, hasDiscount, isFree, badgeText);
                });
    }
}
