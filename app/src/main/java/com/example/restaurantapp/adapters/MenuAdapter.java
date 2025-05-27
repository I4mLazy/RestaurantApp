package com.example.restaurantapp.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.restaurantapp.R;
import com.example.restaurantapp.models.Menu;
import com.example.restaurantapp.models.MenuItem;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * Adapter for displaying a list of {@link Menu} objects in a RecyclerView.
 * Each menu item in the list can display its name, a banner image, and a nested RecyclerView
 * showing its {@link MenuItem}s. This adapter supports filtering of menus based on a search query,
 * which can match either menu names or the names of items within those menus.
 */
public class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.MenuViewHolder>
{
    /**
     * The complete list of all menus available.
     */
    private List<Menu> allMenuList;
    /**
     * The list of menus currently displayed to the user, which may be a filtered subset of {@link #allMenuList}.
     */
    private List<Menu> displayMenuList;
    /**
     * Listener for click events on an entire menu.
     */
    private OnMenuClickListener onMenuClickListener;
    /**
     * Listener for click events on individual items within a menu, passed to the nested {@link MenuItemAdapter}.
     */
    private MenuItemAdapter.OnItemClickListener onItemClickListener;
    /**
     * The ID of the restaurant whose menus are being displayed, used for fetching menu items.
     */
    private String restaurantID;

    /**
     * A set of menu IDs that contain items matching the current search query. Used during filtering.
     */
    private Set<String> itemMatchMenuIds = new HashSet<>();
    /**
     * A set of menu IDs whose names directly match the current search query. Used during filtering.
     */
    private Set<String> menuMatchIds = new HashSet<>();
    /**
     * Flag indicating whether filtering is currently active.
     */
    private boolean isFiltering = false;
    /**
     * Cache for storing lists of {@link MenuItem}s, keyed by their parent menu ID.
     */
    private HashMap<String, List<MenuItem>> allMenuItems = new HashMap<>();
    /**
     * The current search query string used for filtering.
     */
    private String currentSearchQuery = "";

    /**
     * Interface definition for a callback to be invoked when a menu is clicked.
     */
    public interface OnMenuClickListener
    {
        /**
         * Called when a menu has been clicked.
         *
         * @param menu The {@link Menu} object that was clicked.
         */
        void onMenuClick(Menu menu);
    }

    /**
     * Constructs a new {@code MenuAdapter}.
     *
     * @param menuList          The initial list of {@link Menu} objects to display.
     * @param menuClickListener Listener for menu click events.
     * @param itemClickListener Listener for individual menu item click events.
     * @param restaurantID      The ID of the restaurant.
     */
    public MenuAdapter(List<Menu> menuList, OnMenuClickListener menuClickListener,
                       MenuItemAdapter.OnItemClickListener itemClickListener, String restaurantID)
    {
        this.allMenuList = menuList;
        this.displayMenuList = new ArrayList<>(menuList); // Initialize with all menus
        this.onMenuClickListener = menuClickListener;
        this.onItemClickListener = itemClickListener;
        this.restaurantID = restaurantID;
    }

    /**
     * Sets the data used for filtering the list of menus.
     * This method updates the internal sets of matching menu IDs and triggers an update
     * of the displayed list.
     *
     * @param menuMatchIds     A set of menu IDs whose names directly match the search query.
     *                         If null, an empty set is used.
     * @param itemMatchMenuIds A set of menu IDs that contain items matching the search query.
     *                         If null, an empty set is used.
     */
    public void setFilterData(Set<String> menuMatchIds, Set<String> itemMatchMenuIds)
    {
        this.menuMatchIds = menuMatchIds != null ? menuMatchIds : new HashSet<>();
        this.itemMatchMenuIds = itemMatchMenuIds != null ? itemMatchMenuIds : new HashSet<>();

        // Set filtering flag based on whether we have any matches
        this.isFiltering = !this.menuMatchIds.isEmpty() || !this.itemMatchMenuIds.isEmpty();

        // Update display list
        updateDisplayList();
    }

    /**
     * Updates the {@link #displayMenuList} based on the current filtering state
     * ({@link #isFiltering}, {@link #menuMatchIds}, {@link #itemMatchMenuIds}).
     * If filtering is active, only menus matching the criteria are included.
     * Otherwise, all menus from {@link #allMenuList} are displayed.
     * Notifies the adapter that the data set has changed.
     */
    private void updateDisplayList()
    {
        displayMenuList.clear();

        if(isFiltering)
        {
            // If there are any matches (either menu names or item names)
            if(!menuMatchIds.isEmpty() || !itemMatchMenuIds.isEmpty())
            {
                // Only add menus that match the search criteria
                for(Menu menu : allMenuList)
                {
                    String menuID = menu.getMenuID();
                    if(menuMatchIds.contains(menuID) || itemMatchMenuIds.contains(menuID))
                    {
                        displayMenuList.add(menu);
                    }
                }
            }
            // If no matches, displayMenuList remains empty - showing no results
        } else
        {
            // Show all menus when not filtering
            displayMenuList.addAll(allMenuList);
        }

        notifyDataSetChanged();
    }

    /**
     * Clears all active filtering criteria.
     * Resets {@link #menuMatchIds}, {@link #itemMatchMenuIds}, sets {@link #isFiltering} to false,
     * clears {@link #currentSearchQuery}, and updates the display list to show all menus.
     */
    public void clearFiltering()
    {
        this.menuMatchIds.clear();
        this.itemMatchMenuIds.clear();
        this.isFiltering = false;
        this.currentSearchQuery = "";
        updateDisplayList();
    }

    /**
     * Stores a list of {@link MenuItem}s in the cache ({@link #allMenuItems}) for a specific menu ID.
     *
     * @param menuID The ID of the menu to which the items belong.
     * @param items  The list of {@link MenuItem}s to cache.
     */
    public void setMenuItems(String menuID, List<MenuItem> items)
    {
        allMenuItems.put(menuID, new ArrayList<>(items));
    }

    /**
     * Sets the current search query string. This query is used by the ViewHolder
     * when filtering items within a menu if the menu itself didn't match the query directly.
     *
     * @param query The search query string.
     */
    public void setCurrentSearchQuery(String query)
    {
        this.currentSearchQuery = query;
    }

    @NonNull
    @Override
    public MenuViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_menu, parent, false);
        return new MenuViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MenuViewHolder holder, int position)
    {
        if(position < displayMenuList.size())
        {
            Menu menu = displayMenuList.get(position);
            holder.bind(menu, onMenuClickListener, onItemClickListener, restaurantID,
                    isFiltering, menuMatchIds, currentSearchQuery, allMenuItems);
        }
    }

    @Override
    public int getItemCount()
    {
        return displayMenuList.size();
    }

    /**
     * ViewHolder class for displaying a single {@link Menu} item.
     * It includes views for the menu's banner image, name, and a nested RecyclerView
     * to display the menu's items.
     */
    public class MenuViewHolder extends RecyclerView.ViewHolder
    {
        /**
         * ImageView for displaying the menu's banner image.
         */
        public ImageView imgMenuBanner;
        /**
         * TextView for displaying the menu's name.
         */
        TextView txtMenuName;
        /**
         * Nested RecyclerView for displaying the items within this menu.
         */
        RecyclerView recyclerViewItems;

        /**
         * Constructs a new {@code MenuViewHolder}.
         *
         * @param itemView The view that this ViewHolder will manage.
         */
        public MenuViewHolder(@NonNull View itemView)
        {
            super(itemView);
            imgMenuBanner = itemView.findViewById(R.id.imgMenuBanner);
            txtMenuName = itemView.findViewById(R.id.txtMenuName);
            recyclerViewItems = itemView.findViewById(R.id.recyclerViewItems);
            // Set up the layout manager for the nested RecyclerView displaying menu items.
            recyclerViewItems.setLayoutManager(new GridLayoutManager(itemView.getContext(), 3));
        }

        /**
         * Binds a {@link Menu} object to this ViewHolder.
         * Sets the menu name, loads the banner image using Glide, and sets up an
         * OnClickListener for the menu. It then calls {@link #loadMenuItems} to populate
         * the nested RecyclerView with items from this menu, applying filtering logic if active.
         *
         * @param menu               The {@link Menu} object to bind.
         * @param menuClickListener  Listener for click events on the entire menu.
         * @param itemClickListener  Listener for click events on individual items within the menu.
         * @param restaurantID       The ID of the restaurant.
         * @param filtering          True if filtering is active, false otherwise.
         * @param menuMatchIds       Set of menu IDs that directly match the search query.
         * @param currentSearchQuery The current search query string.
         * @param allMenuItems       Cache of menu items.
         */
        public void bind(Menu menu, OnMenuClickListener menuClickListener,
                         MenuItemAdapter.OnItemClickListener itemClickListener,
                         String restaurantID, boolean filtering,
                         Set<String> menuMatchIds, String currentSearchQuery,
                         HashMap<String, List<MenuItem>> allMenuItems)
        {
            String menuID = menu.getMenuID();
            txtMenuName.setText(menu.getName());

            // Load image with Glide and set placeholder if URL is empty or null
            String imageUrl = menu.getImageURL();
            if(imageUrl != null && !imageUrl.isEmpty())
            {
                Glide.with(imgMenuBanner.getContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.image_placeholder) // Placeholder image
                        .error(R.drawable.image_placeholder) // Error image if loading fails
                        .into(imgMenuBanner);
            } else
            {
                imgMenuBanner.setImageResource(R.drawable.image_placeholder); // Default image
            }

            itemView.setOnClickListener(v -> menuClickListener.onMenuClick(menu));

            // Load menu items for this menu
            loadMenuItems(menuID, restaurantID, filtering, menuMatchIds,
                    currentSearchQuery, allMenuItems, itemClickListener);
        }

        /**
         * Loads and displays {@link MenuItem}s for a given menu ID into the nested {@link #recyclerViewItems}.
         * It first checks a local cache ({@code allMenuItems}). If items are found, they are displayed,
         * applying filtering based on {@code filtering}, {@code menuMatchIds}, and {@code currentSearchQuery}.
         * If the menu itself ({@code menuID}) is in {@code menuMatchIds}, all its items are shown.
         * Otherwise, if {@code filtering} is true, only items whose names contain {@code currentSearchQuery} are shown.
         * If items are not cached, they are fetched from Firestore, then cached and displayed with the same filtering logic.
         * Images for the items are loaded using Glide.
         *
         * @param menuID             The ID of the menu whose items are to be loaded.
         * @param restaurantID       The ID of the restaurant, used for Firestore queries.
         * @param filtering          True if filtering is active, false otherwise.
         * @param menuMatchIds       Set of menu IDs that directly match the search query.
         * @param currentSearchQuery The current search query string, used to filter items if the menu itself doesn't match.
         * @param allMenuItems       Cache containing pre-loaded menu items.
         * @param itemClickListener  Listener for click events on individual menu items.
         */
        private void loadMenuItems(String menuID, String restaurantID,
                                   boolean filtering, Set<String> menuMatchIds,
                                   String currentSearchQuery,
                                   HashMap<String, List<MenuItem>> allMenuItems,
                                   MenuItemAdapter.OnItemClickListener itemClickListener)
        {
            // Use cached items if available
            if(allMenuItems.containsKey(menuID))
            {
                List<MenuItem> itemList = allMenuItems.get(menuID);
                List<MenuItem> itemsToDisplay;

                if(filtering)
                {
                    if(menuMatchIds.contains(menuID))
                    {
                        // If the menu itself matches, show all its items
                        itemsToDisplay = new ArrayList<>(itemList);
                    } else
                    {
                        // If the menu doesn't match directly, but filtering is on (meaning items within might match)
                        // Filter items based on the currentSearchQuery
                        itemsToDisplay = new ArrayList<>();
                        if(itemList != null && currentSearchQuery != null && !currentSearchQuery.isEmpty())
                        {
                            for(MenuItem item : itemList)
                            {
                                if(item.getName() != null && item.getName().toLowerCase().contains(currentSearchQuery.toLowerCase()))
                                {
                                    itemsToDisplay.add(item);
                                }
                            }
                        }
                    }
                } else
                {
                    // No filtering, show all items for this menu
                    itemsToDisplay = new ArrayList<>(itemList);
                }

                MenuItemAdapter itemAdapter = new MenuItemAdapter(itemsToDisplay, itemClickListener);
                recyclerViewItems.setAdapter(itemAdapter);

                // Post a runnable to load images for visible items after layout
                recyclerViewItems.post(() ->
                {
                    if(itemsToDisplay == null) return;
                    for(int i = 0; i < recyclerViewItems.getChildCount(); i++)
                    {
                        View childView = recyclerViewItems.getChildAt(i);
                        RecyclerView.ViewHolder rvViewHolder = recyclerViewItems.getChildViewHolder(childView);
                        if(rvViewHolder instanceof MenuItemAdapter.ItemViewHolder)
                        {
                            MenuItemAdapter.ItemViewHolder viewHolder = (MenuItemAdapter.ItemViewHolder) rvViewHolder;
                            int adapterPosition = viewHolder.getAdapterPosition(); // Use adapter position for safety
                            if(adapterPosition != RecyclerView.NO_POSITION && adapterPosition < itemsToDisplay.size())
                            {
                                MenuItem item = itemsToDisplay.get(adapterPosition);
                                if(item.getImageURL() != null && !item.getImageURL().isEmpty())
                                {
                                    Glide.with(childView.getContext()).load(item.getImageURL())
                                            .placeholder(R.drawable.image_placeholder)
                                            .error(R.drawable.image_placeholder)
                                            .into(viewHolder.itemImage);
                                } else
                                {
                                    viewHolder.itemImage.setImageResource(R.drawable.image_placeholder);
                                }
                            }
                        }
                    }
                });
                return;
            }

            // Load from Firebase if not cached
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            List<MenuItem> fetchedItemList = new ArrayList<>();

            db.collection("Restaurants").document(restaurantID)
                    .collection("Menus").document(menuID)
                    .collection("Items")
                    .orderBy("orderIndex") // Assuming items have an orderIndex field
                    .get()
                    .addOnSuccessListener(snapshot ->
                    {
                        for(QueryDocumentSnapshot doc : snapshot)
                        {
                            MenuItem item = doc.toObject(MenuItem.class);
                            fetchedItemList.add(item);
                        }
                        // Cache the fetched items
                        allMenuItems.put(menuID, new ArrayList<>(fetchedItemList));

                        List<MenuItem> itemsToDisplay;
                        if(filtering)
                        {
                            if(menuMatchIds.contains(menuID))
                            {
                                itemsToDisplay = new ArrayList<>(fetchedItemList);
                            } else
                            {
                                itemsToDisplay = new ArrayList<>();
                                if(currentSearchQuery != null && !currentSearchQuery.isEmpty())
                                {
                                    for(MenuItem item : fetchedItemList)
                                    {
                                        if(item.getName() != null && item.getName().toLowerCase().contains(currentSearchQuery.toLowerCase()))
                                        {
                                            itemsToDisplay.add(item);
                                        }
                                    }
                                }
                            }
                        } else
                        {
                            itemsToDisplay = new ArrayList<>(fetchedItemList);
                        }

                        MenuItemAdapter itemAdapter = new MenuItemAdapter(itemsToDisplay, itemClickListener);
                        recyclerViewItems.setAdapter(itemAdapter);

                        // Post a runnable to load images for visible items after layout
                        recyclerViewItems.post(() ->
                        {
                            if(itemsToDisplay == null) return;
                            for(int i = 0; i < recyclerViewItems.getChildCount(); i++)
                            {
                                View childView = recyclerViewItems.getChildAt(i);
                                RecyclerView.ViewHolder rvViewHolder = recyclerViewItems.getChildViewHolder(childView);
                                if(rvViewHolder instanceof MenuItemAdapter.ItemViewHolder)
                                {
                                    MenuItemAdapter.ItemViewHolder viewHolder = (MenuItemAdapter.ItemViewHolder) rvViewHolder;
                                    int adapterPosition = viewHolder.getAdapterPosition();
                                    if(adapterPosition != RecyclerView.NO_POSITION && adapterPosition < itemsToDisplay.size())
                                    {
                                        MenuItem item = itemsToDisplay.get(adapterPosition);
                                        if(item.getImageURL() != null && !item.getImageURL().isEmpty())
                                        {
                                            Glide.with(childView.getContext()).load(item.getImageURL())
                                                    .placeholder(R.drawable.image_placeholder)
                                                    .error(R.drawable.image_placeholder)
                                                    .into(viewHolder.itemImage);
                                        } else
                                        {
                                            viewHolder.itemImage.setImageResource(R.drawable.image_placeholder);
                                        }
                                    }
                                }
                            }
                        });
                    })
                    .addOnFailureListener(e -> Log.e("MenuAdapter", "Error loading items for menu " + menuID + ": " + e.getMessage()));
        }
    }
}