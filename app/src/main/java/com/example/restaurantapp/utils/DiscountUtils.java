package com.example.restaurantapp.utils;

import android.content.Context;
import android.util.Log;

import com.example.restaurantapp.R;
import com.example.restaurantapp.models.MenuItem;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Utility class for handling discount calculations and application for {@link MenuItem}s.
 * It provides a static method to fetch active discounts for a given menu item from Firestore,
 * calculate the final price after applying these discounts, and determine appropriate display
 * information such as whether a discount is active, if the item is free, and a badge text
 * for the discount.
 */
public class DiscountUtils
{

    /**
     * Callback interface for receiving the result of discount calculations.
     */
    public interface DiscountResultCallback
    {
        /**
         * Called when discount calculation is complete.
         *
         * @param originalPrice The original price of the menu item.
         * @param finalPrice    The price of the menu item after applying all active discounts.
         * @param hasDiscount   True if any discount was applied, false otherwise.
         * @param isFree        True if the final price is zero, false otherwise.
         * @param badgeText     A string to display as a discount badge (e.g., "% OFF", "$X OFF", "FREE"),
         *                      or null if no discount is applied.
         */
        void onResult(double originalPrice, double finalPrice, boolean hasDiscount, boolean isFree, String badgeText);
    }

    /**
     * Fetches and applies active discounts for a given {@link MenuItem} from Firestore.
     * It queries the "Discounts" subcollection of the specified item, ordered by "startTime".
     * For each discount, it checks if the current time falls within its "startTime" and "endTime"
     * (if an endTime exists).
     * Active discounts are applied sequentially:
     * <ul>
     *     <li>"Flat" discounts are subtracted from the current price.</li>
     *     <li>"Percentage" discounts are applied to the current price after any flat discounts.
     *         If multiple percentage discounts are active, they are applied cumulatively.</li>
     * </ul>
     * The final price is ensured to be non-negative (Math.max(currentPrice, 0)).
     * After calculations, it determines if any discount was applied ({@code hasDiscount}),
     * if the item is free ({@code isFree}), and generates a {@code badgeText}
     * (e.g., "X% OFF", "$Y OFF", or "FREE" from string resources).
     * The results are then passed to the provided {@link DiscountResultCallback}.
     *
     * @param item     The {@link MenuItem} for which to apply discounts. Must have valid
     *                 {@code restaurantID}, {@code menuID}, and {@code itemID}.
     * @param context  The {@link Context} used to access string resources for badge text.
     * @param callback The {@link DiscountResultCallback} to be invoked with the results.
     */
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
                .orderBy("startTime") // Order by start time, though application order might matter more
                .get()
                .addOnSuccessListener(querySnapshot ->
                {
                    double originalPrice = item.getPrice();
                    double currentPrice = originalPrice;
                    double totalFlatDiscountApplied = 0; // Tracks sum of flat discounts for badge text
                    boolean hasActivePercentageDiscount = false; // Tracks if any percentage discount was applied
                    Date now = new Date(); // Current time for checking discount validity

                    List<QueryDocumentSnapshot> activeDiscounts = new ArrayList<>();
                    // First, filter for currently active discounts
                    for(QueryDocumentSnapshot doc : querySnapshot)
                    {
                        Timestamp start = doc.getTimestamp("startTime");
                        Timestamp end = doc.getTimestamp("endTime");

                        // Check if discount is currently active
                        if(start != null && now.after(start.toDate()) &&
                                (end == null || now.before(end.toDate()))) // Active if start is past and (no end OR end is future)
                        {
                            activeDiscounts.add(doc);
                        }
                    }

                    // Apply active discounts
                    // Note: The order of application (flat vs percentage) might matter.
                    // This implementation applies them in the order they are fetched (ordered by startTime).
                    // If specific order is needed (e.g., all flat then all percentage), list should be sorted or processed in passes.
                    for(QueryDocumentSnapshot doc : activeDiscounts)
                    {
                        String type = doc.getString("discountType");
                        Double amountDouble = doc.getDouble("amount"); // Use getDouble for Firestore numbers
                        double amount = (amountDouble != null) ? amountDouble : 0.0;

                        if("Flat".equals(type))
                        {
                            currentPrice -= amount;
                            totalFlatDiscountApplied += amount; // Sum flat discounts for badge
                        } else if("Percentage".equals(type))
                        {
                            currentPrice *= (1 - (amount / 100.0)); // Apply percentage to current price
                            hasActivePercentageDiscount = true;
                        }
                    }

                    currentPrice = Math.max(currentPrice, 0); // Ensure price doesn't go below zero

                    boolean hasEffectiveDiscount = originalPrice > currentPrice; // A discount is effective if price changed
                    boolean isEffectivelyFree = currentPrice == 0 && hasEffectiveDiscount; // Free only if price became 0 due to discount

                    String badgeText = null;
                    if(hasEffectiveDiscount)
                    {
                        if(isEffectivelyFree)
                        {
                            badgeText = context.getString(R.string.free);
                        } else if(hasActivePercentageDiscount) // Prioritize showing percentage if one was applied
                        {
                            // Calculate effective percentage based on original and final price
                            double effectivePercentage = ((originalPrice - currentPrice) / originalPrice) * 100.0;
                            badgeText = context.getString(R.string.percent_off_format, (int) Math.round(effectivePercentage)); // Round percentage
                        } else if(totalFlatDiscountApplied > 0) // If only flat discounts applied
                        {
                            badgeText = context.getString(R.string.amount_off_format, (int) Math.round(totalFlatDiscountApplied)); // Round flat amount
                        }
                        // If neither percentage nor flat discount led to a price change but hasEffectiveDiscount is true (e.g. complex scenario), badgeText remains null.
                    }

                    callback.onResult(originalPrice, currentPrice, hasEffectiveDiscount, isEffectivelyFree, badgeText);
                })
                .addOnFailureListener(e ->
                {
                    Log.e("DiscountUtils", "Error fetching discounts for item: " + item.getItemID(), e);
                });
    }
}