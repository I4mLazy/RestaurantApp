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
            if(listener != null)
            {
                listener.onMenuClick(menu);
            }
        });

        // Clear any previous state
        holder.itemsRecyclerView.setAdapter(null);
        holder.itemsRecyclerView.setLayoutManager(null);

        // Set tag on the RecyclerView to track which menu it belongs to
        holder.itemsRecyclerView.setTag(menu.getMenuID());

        // Set new layout manager and adapter
        holder.itemsRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        MenuItemAdapter adapter = new MenuItemAdapter(new ArrayList<>(), context, menuItem ->
        {
            if(context instanceof FragmentActivity)
            {
                MenuItemSelectionViewModel viewModel = new ViewModelProvider((FragmentActivity) context)
                        .get(MenuItemSelectionViewModel.class);
                viewModel.selectMenuItem(menuItem);

                MenuItemFragment menuItemFragment = new MenuItemFragment();
                ((FragmentActivity) context).getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, menuItemFragment)
                        .addToBackStack(null)
                        .commit();
            } else
            {
                Log.e("MenusAdapter", "Context is not a FragmentActivity, cannot proceed with ViewModel.");
            }
        });
        holder.itemsRecyclerView.setAdapter(adapter);

        if(restaurantRef != null)
        {
            Log.d("MenusAdapter", "Starting fetch for menu: " + menu.getName() + " (ID: " + menu.getMenuID() + ")");
            fetchMenuItems(menu, adapter);
        }
    }

    private void fetchMenuItems(Menu menu, MenuItemAdapter adapter)
    {
        Log.d("MenusAdapter", "Fetching menu items for: " + menu.getName() + " (ID: " + menu.getMenuID() + ")");

        if(restaurantRef == null)
        {
            Log.e("MenusAdapter", "Restaurant reference is null!");
            return;
        }

        // Query the specific menu document
        restaurantRef.collection("Menus").document(menu.getMenuID()).collection("Items")
                .orderBy("orderIndex")
                .get()
                .addOnCompleteListener(task ->
                {
                    if(task.isSuccessful())
                    {
                        List<MenuItem> menuItems = new ArrayList<>();
                        for(QueryDocumentSnapshot document : task.getResult())
                        {
                            menuItems.add(document.toObject(MenuItem.class));
                        }

                        if(menuItems.isEmpty())
                        {
                            Log.w("MenusAdapter", "No menu items found for menu: " + menu.getName() + " (ID: " + menu.getMenuID() + ")");
                        } else
                        {
                            Log.d("MenusAdapter", "Found " + menuItems.size() + " items for menu: " + menu.getName() + " (ID: " + menu.getMenuID() + ")");
                        }

                        // Update the adapter's data
                        adapter.updateData(menuItems);
                    } else
                    {
                        Log.e("MenusAdapter", "Error getting menu items for " + menu.getName() + ": ", task.getException());
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