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

public class ManageMenuAdapter extends RecyclerView.Adapter<ManageMenuAdapter.MenuViewHolder>
{
    private List<Menu> allMenuList;
    private List<Menu> displayMenuList;  // This will be the filtered list we actually show
    private OnMenuClickListener onMenuClickListener;
    private ManageMenuItemAdapter.OnItemClickListener onItemClickListener;
    private String restaurantID;
    private Set<String> itemMatchMenuIds = new HashSet<>();  // Menus containing matching items
    private Set<String> menuMatchIds = new HashSet<>();      // Menus that directly match the search
    private boolean isFiltering = false;
    private HashMap<String, List<MenuItem>> allMenuItems = new HashMap<>(); // All items by menu ID
    private String currentSearchQuery = "";

    public interface OnMenuClickListener
    {
        void onMenuClick(Menu menu);
    }

    public ManageMenuAdapter(List<Menu> menuList, OnMenuClickListener menuClickListener,
                             ManageMenuItemAdapter.OnItemClickListener itemClickListener, String restaurantID)
    {
        this.allMenuList = menuList;
        this.displayMenuList = new ArrayList<>(menuList);
        this.onMenuClickListener = menuClickListener;
        this.onItemClickListener = itemClickListener;
        this.restaurantID = restaurantID;
    }

    // Method to set search filtering information
    public void setFilterData(Set<String> menuMatchIds, Set<String> itemMatchMenuIds)
    {
        this.menuMatchIds = menuMatchIds != null ? menuMatchIds : new HashSet<>();
        this.itemMatchMenuIds = itemMatchMenuIds != null ? itemMatchMenuIds : new HashSet<>();

        // Set filtering flag based on whether we have any matches
        this.isFiltering = !menuMatchIds.isEmpty() || !itemMatchMenuIds.isEmpty();

        // Update display list
        updateDisplayList();
    }

    // Update the display list based on current filters
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

    // Clear filtering
    public void clearFiltering()
    {
        this.menuMatchIds.clear();
        this.itemMatchMenuIds.clear();
        this.isFiltering = false;
        this.currentSearchQuery = "";
        updateDisplayList();
    }

    // Store items for a menu
    public void setMenuItems(String menuID, List<MenuItem> items)
    {
        allMenuItems.put(menuID, new ArrayList<>(items));
    }

    // Set current search query
    public void setCurrentSearchQuery(String query)
    {
        this.currentSearchQuery = query;
    }

    @NonNull
    @Override
    public MenuViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_manage_menu, parent, false);
        return new MenuViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MenuViewHolder holder, int position)
    {
        if(position < displayMenuList.size())
        {
            holder.bind(displayMenuList.get(position));
        }
    }

    @Override
    public int getItemCount()
    {
        return displayMenuList.size();
    }

    class MenuViewHolder extends RecyclerView.ViewHolder
    {
        ImageView imgMenuBanner;
        TextView txtMenuName;
        RecyclerView recyclerViewItems;

        public MenuViewHolder(@NonNull View itemView)
        {
            super(itemView);
            imgMenuBanner = itemView.findViewById(R.id.imgMenuBanner);
            txtMenuName = itemView.findViewById(R.id.txtMenuName);
            recyclerViewItems = itemView.findViewById(R.id.recyclerViewItems);
            recyclerViewItems.setLayoutManager(new GridLayoutManager(itemView.getContext(), 3));
        }

        public void bind(Menu menu)
        {
            String menuID = menu.getMenuID();
            txtMenuName.setText(menu.getName());

            if(menu.getImageURL() != null && !menu.getImageURL().isEmpty())
            {
                Glide.with(itemView.getContext()).load(menu.getImageURL()).into(imgMenuBanner);
            } else
            {
                imgMenuBanner.setImageResource(R.drawable.image_placeholder);
            }

            itemView.setOnClickListener(v -> onMenuClickListener.onMenuClick(menu));

            // Load menu items
            loadMenuItems(menuID);
        }

        private void loadMenuItems(String menuID)
        {
            // Use cached items if available
            if(allMenuItems.containsKey(menuID))
            {
                List<MenuItem> itemList = allMenuItems.get(menuID);

                // If filtering is active, only show matching items
                if(isFiltering && itemMatchMenuIds.contains(menuID))
                {
                    List<MenuItem> filteredItems = new ArrayList<>();
                    for(MenuItem item : itemList)
                    {
                        if(item.getName().toLowerCase().contains(currentSearchQuery.toLowerCase()))
                        {
                            filteredItems.add(item);
                        }
                    }
                    ManageMenuItemAdapter itemAdapter = new ManageMenuItemAdapter(filteredItems, onItemClickListener);
                    recyclerViewItems.setAdapter(itemAdapter);
                } else
                {
                    // Show all items if not filtering or if this menu matches by name
                    ManageMenuItemAdapter itemAdapter = new ManageMenuItemAdapter(itemList, onItemClickListener);
                    recyclerViewItems.setAdapter(itemAdapter);
                }
                return;
            }

            // Load from Firebase if not cached
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            List<MenuItem> itemList = new ArrayList<>();

            db.collection("Restaurants").document(restaurantID)
                    .collection("Menus").document(menuID)
                    .collection("Items")
                    .orderBy("orderIndex")
                    .get()
                    .addOnSuccessListener(snapshot ->
                    {
                        for(QueryDocumentSnapshot doc : snapshot)
                        {
                            MenuItem item = doc.toObject(MenuItem.class);
                            itemList.add(item);
                        }

                        // Store all items for this menu
                        allMenuItems.put(menuID, new ArrayList<>(itemList));

                        // If filtering is active, only show matching items
                        if(isFiltering && itemMatchMenuIds.contains(menuID))
                        {
                            List<MenuItem> filteredItems = new ArrayList<>();
                            for(MenuItem item : itemList)
                            {
                                if(item.getName().toLowerCase().contains(currentSearchQuery.toLowerCase()))
                                {
                                    filteredItems.add(item);
                                }
                            }
                            ManageMenuItemAdapter itemAdapter = new ManageMenuItemAdapter(filteredItems, onItemClickListener);
                            recyclerViewItems.setAdapter(itemAdapter);
                        } else
                        {
                            // Show all items if not filtering or if this menu matches by name
                            ManageMenuItemAdapter itemAdapter = new ManageMenuItemAdapter(itemList, onItemClickListener);
                            recyclerViewItems.setAdapter(itemAdapter);
                        }
                    })
                    .addOnFailureListener(e -> Log.e("MenuAdapter", "Error loading items: " + e.getMessage()));
        }
    }
}