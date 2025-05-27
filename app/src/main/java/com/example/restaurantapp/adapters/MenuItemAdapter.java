package com.example.restaurantapp.adapters;

import android.graphics.Paint;
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

import java.util.List;

import com.example.restaurantapp.utils.DiscountUtils;

/**
 * Adapter for displaying a list of {@link MenuItem} objects in a RecyclerView.
 * Each item in the list shows its image, name, and price.
 * It utilizes {@link DiscountUtils} to potentially modify the display of prices and show discount information.
 */
public class MenuItemAdapter extends RecyclerView.Adapter<MenuItemAdapter.ItemViewHolder>
{
    /**
     * The list of menu items to be displayed by this adapter.
     */
    private List<MenuItem> menuItemList;
    /**
     * Listener for click events on individual menu items.
     */
    private OnItemClickListener onItemClickListener;

    /**
     * Interface definition for a callback to be invoked when a menu item is clicked.
     */
    public interface OnItemClickListener
    {
        /**
         * Called when a menu item has been clicked.
         *
         * @param item The {@link MenuItem} object that was clicked.
         */
        void onItemClick(MenuItem item);
    }

    /**
     * Constructs a new {@code MenuItemAdapter}.
     *
     * @param menuItemList The list of {@link MenuItem} objects to display.
     * @param listener     The listener that will handle item clicks.
     */
    public MenuItemAdapter(List<MenuItem> menuItemList, OnItemClickListener listener)
    {
        this.menuItemList = menuItemList;
        this.onItemClickListener = listener;
    }

    /**
     * Called when RecyclerView needs a new {@link ItemViewHolder} of the given type to represent
     * an item. This new ViewHolder should be constructed with a new View that can represent the items
     * of the given type.
     *
     * @param parent   The ViewGroup into which the new View will be added after it is bound to
     *                 an adapter position.
     * @param viewType The view type of the new View.
     * @return A new ItemViewHolder that holds a View of the given view type.
     */
    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_menu_item, parent, false);
        return new ItemViewHolder(view);
    }

    /**
     * Called by RecyclerView to display the data at the specified position. This method should
     * update the contents of the {@link ItemViewHolder#itemView} to reflect the item at the given
     * position.
     *
     * @param holder   The ItemViewHolder which should be updated to represent the contents of the
     *                 item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position)
    {
        holder.bind(menuItemList.get(position));
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of items in this adapter.
     */
    @Override
    public int getItemCount()
    {
        return menuItemList.size();
    }

    /**
     * ViewHolder class for displaying a single {@link MenuItem}.
     * It holds references to the UI components within the item's layout,
     * such as ImageView for the item image and TextViews for name, price, etc.
     */
    class ItemViewHolder extends RecyclerView.ViewHolder
    {
        /**
         * ImageView to display the image of the menu item.
         */
        ImageView itemImage;
        /**
         * TextView to display the name of the menu item.
         */
        TextView itemName;
        /**
         * TextView to display the price of the menu item. This may be updated by discount logic.
         */
        TextView itemPrice;
        /**
         * TextView to display a discount badge (e.g., percentage off, "FREE").
         */
        TextView discountBadge;
        /**
         * TextView to display the original price of the menu item if a discount is applied.
         */
        TextView oldPrice;

        /**
         * Constructs a new {@code ItemViewHolder}.
         * Initializes the UI components by finding them in the itemView.
         *
         * @param itemView The view that this ViewHolder will manage, representing a single menu item.
         */
        public ItemViewHolder(@NonNull View itemView)
        {
            super(itemView);
            itemImage = itemView.findViewById(R.id.itemImage);
            itemName = itemView.findViewById(R.id.itemName);
            itemPrice = itemView.findViewById(R.id.itemPrice);
            discountBadge = itemView.findViewById(R.id.discountBadge);
            oldPrice = itemView.findViewById(R.id.oldPrice);
        }

        /**
         * Binds a {@link MenuItem} object's data to the views in this ViewHolder.
         * Sets the item's name and initially sets its price text to the original price.
         * Loads the item's image using Glide if a URL is available; otherwise, sets a placeholder image.
         * It then calls {@link DiscountUtils#applyActiveDiscounts} to process and display any applicable discounts.
         * The callback provided to {@code applyActiveDiscounts} receives parameters indicating the discount status:
         * <ul>
         *     <li>If the {@code hasDiscount} parameter (from the callback) is true:
         *         <ul>
         *             <li>The {@code itemPrice} TextView is updated to display the {@code current} (discounted) price.</li>
         *             <li>The {@code oldPrice} TextView is made visible and set to display the {@code original} price with a strikethrough effect.</li>
         *             <li>The {@code discountBadge} TextView is made visible and its text is set to the provided {@code badgeText}.</li>
         *         </ul>
         *     </li>
         *     <li>Subsequently, if the {@code isFree} parameter (from the callback) is true:
         *         <ul>
         *             <li>The {@code discountBadge} TextView is made visible (if not already).</li>
         *             <li>The text of {@code discountBadge} is set to "FREE" (obtained from string resources using {@code itemView.getContext().getString(R.string.free)}).
         *                 If {@code hasDiscount} was also true, this action will override the text previously set by {@code badgeText}.</li>
         *         </ul>
         *     </li>
         * </ul>
         * An OnClickListener is set on the itemView to invoke {@link OnItemClickListener#onItemClick(MenuItem)}
         * if the {@link #onItemClickListener} is not null.
         *
         * @param item The {@link MenuItem} object containing the data to bind.
         */
        public void bind(MenuItem item)
        {
            itemName.setText(item.getName());
            itemPrice.setText(String.format("$%.2f", item.getPrice())); // Show original price by default

            if(item.getImageURL() != null) // Checks if imageURL is not null
            {
                Glide.with(itemView.getContext())
                        .load(item.getImageURL())
                        .into(itemImage);
            } else
            {
                itemImage.setImageResource(R.drawable.image_placeholder);
            }

            // Apply discount using utility
            DiscountUtils.applyActiveDiscounts(item, itemView.getContext(), (original, current, hasDiscount, isFree, badgeText) ->
            {
                if(hasDiscount)
                {
                    itemPrice.setText(String.format("$%.2f", current));
                    oldPrice.setVisibility(View.VISIBLE);
                    oldPrice.setText(String.format("$%.2f", original));
                    oldPrice.setPaintFlags(oldPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG); // Strikethrough effect
                    discountBadge.setVisibility(View.VISIBLE);
                    discountBadge.setText(badgeText);
                }

                if(isFree)
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