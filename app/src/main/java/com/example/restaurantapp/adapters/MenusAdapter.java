package com.example.restaurantapp.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.restaurantapp.R;
import com.example.restaurantapp.fragments.MenuItemFragment;
import com.example.restaurantapp.models.Menu;
import com.example.restaurantapp.models.MenuItem;
import com.example.restaurantapp.viewmodels.MenuItemSelectionViewModel;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MenusAdapter extends RecyclerView.Adapter<MenusAdapter.ViewHolder>
{

    public interface OnMenuClickListener
    {
        void onMenuClick(Menu menu);
    }

    private final List<Menu> menus;
    private final Context context;
    private final OnMenuClickListener listener;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private DocumentReference restaurantRef;

    public MenusAdapter(List<Menu> menus, Context context, OnMenuClickListener listener)
    {
        this.menus = menus;
        this.context = context;
        this.listener = listener;
    }

    public void updateData(List<Menu> newMenus)
    {
        menus.clear();
        menus.addAll(newMenus);
        notifyDataSetChanged();
    }

    public void setRestaurantReference(DocumentReference restaurantRef)
    {
        this.restaurantRef = restaurantRef;
    }

    @NonNull
    @Override
    public MenusAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(context).inflate(R.layout.item_menu, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MenusAdapter.ViewHolder holder, int position)
    {
        Menu menu = menus.get(position);
        holder.menuName.setText(menu.getName() != null ? menu.getName() : "N/A");
        holder.menuDescription.setText(menu.getDescription() != null ? menu.getDescription() : "N/A");

        String imageUrl = menu.getImageURL();
        Glide.with(context)
                .load(imageUrl != null && !imageUrl.isEmpty() ? imageUrl : R.drawable.image_placeholder)
                .placeholder(R.drawable.image_placeholder)
                .error(R.drawable.image_placeholder)
                .into(holder.menuImage);

        holder.itemView.setOnClickListener(v ->
        {
            if (listener != null)
            {
                listener.onMenuClick(menu);
            }
        });

        if (restaurantRef != null)
        {
            fetchMenuItems(menu, holder.itemsRecyclerView);
        }
    }

    private void fetchMenuItems(Menu menu, RecyclerView itemsRecyclerView) {
        Log.e("MenusAdapter", "Fetching menu items for: " + menu.getName());
        if (menu == null || restaurantRef == null) {
            Log.e("MenusAdapter", "Menu or restaurant reference is null!");
            return;
        }

        restaurantRef.collection("Menus").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                boolean menuFound = false;
                for (QueryDocumentSnapshot document : task.getResult()) {
                    if (document.getId().equals(menu.getMenuID())) {
                        menuFound = true;
                        document.getReference().collection("MenuItems").get().addOnCompleteListener(task2 -> {
                            if (task2.isSuccessful()) {
                                List<MenuItem> menuItems = new ArrayList<>();
                                for (QueryDocumentSnapshot document2 : task2.getResult()) {
                                    menuItems.add(document2.toObject(MenuItem.class));
                                }
                                if (menuItems.isEmpty()) {
                                    Log.w("MenusAdapter", "No menu items found for menu: " + menu.getName());
                                }
                                itemsRecyclerView.setLayoutManager(new LinearLayoutManager(context));
                                itemsRecyclerView.setAdapter(new MenuItemAdapter(menuItems, context, menuItem -> {
                                    // Handle the menu item click:
                                    MenuItemSelectionViewModel viewModel = new ViewModelProvider((FragmentActivity) context)
                                            .get(MenuItemSelectionViewModel.class);
                                    viewModel.selectMenuItem(menuItem);

                                    MenuItemFragment menuItemFragment = new MenuItemFragment();
                                    ((FragmentActivity) context).getSupportFragmentManager().beginTransaction()
                                            .replace(R.id.fragmentContainer, menuItemFragment)
                                            .addToBackStack(null)
                                            .commit();
                                }));
                            } else {
                                Log.e("MenusAdapter", "Error getting menu items: ", task2.getException());
                                // Set an empty adapter to avoid showing stale data.
                                itemsRecyclerView.setLayoutManager(new LinearLayoutManager(context));
                                itemsRecyclerView.setAdapter(new MenuItemAdapter(new ArrayList<>(), context, menuItem -> {
                                    // Optionally handle clicks, if needed.
                                }));
                            }
                        });
                        break; // Exit loop once the matching menu is found.
                    }
                }
                if (!menuFound) {
                    Log.w("MenusAdapter", "No matching menu document found for menu: " + menu.getName());
                    // Update the RecyclerView with an empty adapter.
                    itemsRecyclerView.setLayoutManager(new LinearLayoutManager(context));
                    itemsRecyclerView.setAdapter(new MenuItemAdapter(new ArrayList<>(), context, menuItem -> {
                        // Optionally handle clicks, if needed.
                    }));
                }
            }
        });
    }


    @Override
    public int getItemCount()
    {
        return menus.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder
    {
        TextView menuName, menuDescription;
        ImageView menuImage;
        RecyclerView itemsRecyclerView;

        public ViewHolder(View itemView)
        {
            super(itemView);
            menuName = itemView.findViewById(R.id.menuName);
            menuDescription = itemView.findViewById(R.id.menuDescription);
            menuImage = itemView.findViewById(R.id.menuImage);
            itemsRecyclerView = itemView.findViewById(R.id.itemsRecyclerView);
        }
    }
}
