package com.example.restaurantapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.restaurantapp.R;
import com.example.restaurantapp.models.MenuItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.Timestamp;

public class MenuItemAdapter extends RecyclerView.Adapter<MenuItemAdapter.ItemViewHolder>
{
    private List<MenuItem> menuItemList;
    private OnItemClickListener onItemClickListener;

    public interface OnItemClickListener
    {
        void onItemClick(MenuItem item);
    }

    public MenuItemAdapter(List<MenuItem> menuItemList, OnItemClickListener listener)
    {
        this.menuItemList = menuItemList;
        this.onItemClickListener = listener;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_menu_item, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position)
    {
        holder.bind(menuItemList.get(position));
    }

    @Override
    public int getItemCount()
    {
        return menuItemList.size();
    }

    class ItemViewHolder extends RecyclerView.ViewHolder
    {
        ImageView itemImage;
        TextView itemName, itemPrice, discountBadge, oldPrice;

        public ItemViewHolder(@NonNull View itemView)
        {
            super(itemView);
            itemImage = itemView.findViewById(R.id.itemImage);
            itemName = itemView.findViewById(R.id.itemName);
            itemPrice = itemView.findViewById(R.id.itemPrice);
            discountBadge = itemView.findViewById(R.id.discountBadge);
            oldPrice = itemView.findViewById(R.id.oldPrice);

        }

        public void bind(MenuItem item)
        {
            itemName.setText(item.getName());
            itemPrice.setText("$" + item.getPrice());

            if(item.getImageURL() != null)
            {
                Glide.with(itemView.getContext())
                        .load(item.getImageURL())
                        .into(itemImage);
            } else
            {
                itemImage.setImageResource(R.drawable.image_placeholder);
            }

            // Fetch discounts
            FirebaseFirestore.getInstance()
                    .collection("Restaurants")
                    .document(item.getRestaurantID())
                    .collection("Menus")
                    .document(item.getMenuID())
                    .collection("Items")
                    .document(item.getItemID())
                    .collection("Discounts")
                    .orderBy("startTime")  // Sort by start time
                    .get()
                    .addOnSuccessListener(querySnapshot ->
                    {
                        double originalPrice = item.getPrice();
                        double currentPrice = originalPrice; // Track current price after each discount
                        double totalDiscount = 0; // This will hold the total discount applied to the item
                        Date now = new Date();
                        boolean hasPercentageDiscount = false;

                        // Collect active discounts
                        List<QueryDocumentSnapshot> activeDiscounts = new ArrayList<>();
                        for(QueryDocumentSnapshot doc : querySnapshot)
                        {
                            String type = doc.getString("discountType");
                            double amount = doc.getDouble("amount") != null ? doc.getDouble("amount") : 0;
                            Timestamp start = doc.getTimestamp("startTime");
                            Timestamp end = doc.getTimestamp("endTime");

                            // Check if the discount is active (current time is within the discount's period)
                            if(start != null && now.after(start.toDate()))
                            {
                                if(end == null || now.before(end.toDate()))
                                {
                                    activeDiscounts.add(doc);  // Add active discount
                                }
                            }
                        }

                        // Apply discounts in order
                        for(QueryDocumentSnapshot doc : activeDiscounts)
                        {
                            String type = doc.getString("discountType");
                            double amount = doc.getDouble("amount") != null ? doc.getDouble("amount") : 0;

                            if("Flat".equals(type))
                            {
                                currentPrice -= amount; // Apply flat discount on the current price
                                totalDiscount += amount; // Track the total flat discount applied
                            } else if("Percentage".equals(type))
                            {
                                currentPrice *= (1 - (amount / 100.0)); // Apply percentage discount on the current price
                                hasPercentageDiscount = true; // Mark that we had a percentage discount
                            }
                        }

                        // Round the final price for display
                        currentPrice = Math.max(currentPrice, 0); // Prevent negative price
                        if(originalPrice > currentPrice)
                        {
                            // Update UI with the final price
                            itemPrice.setText(String.format("$%.2f", currentPrice)); // Display final price
                            oldPrice.setVisibility(View.VISIBLE);
                            oldPrice.setText(String.format("$%.2f", originalPrice)); // Display original price
                            discountBadge.setVisibility(View.VISIBLE);

                            // Display the total discount in the badge
                            if(hasPercentageDiscount)
                            {
                                double percentageDiscount = (originalPrice - currentPrice) / originalPrice * 100;
                                discountBadge.setText(itemView.getContext().getString(R.string.percent_off_format, (int) percentageDiscount));
                            } else
                            {
                                discountBadge.setText(itemView.getContext().getString(R.string.amount_off_format, (int) totalDiscount));
                            }
                        }

                        if(currentPrice == 0)
                        {
                            discountBadge.setVisibility(View.VISIBLE);
                            discountBadge.setText(itemView.getContext().getString(R.string.free));
                        }
                    });


            itemView.setOnClickListener(v ->
            {
                if(onItemClickListener != null)
                {
                    onItemClickListener.onItemClick(item);
                }
            });
        }

    }
}
