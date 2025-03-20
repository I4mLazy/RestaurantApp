package com.example.restaurantapp.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.restaurantapp.R;
import com.example.restaurantapp.adapters.ManageMenuAdapter;
import com.example.restaurantapp.models.Menu;
import com.example.restaurantapp.models.MenuItem;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class ManageMenuFragment extends Fragment
{

    private SearchView searchBar;
    private RecyclerView recyclerViewMenus;
    private ManageMenuAdapter menuAdapter;
    private List<Menu> menuList = new ArrayList<>();
    private List<MenuItem> menuItemList = new ArrayList<>();
    private List<Menu> filteredMenus = new ArrayList<>();
    private List<MenuItem> filteredItems = new ArrayList<>();

    private ImageButton btnCreateDiscount, btnSetOrder, btnAdd;
    private RelativeLayout itemViewOverlay, menuViewOverlay, addChoiceOverlay, itemEditOverlay, menuEditOverlay, discountOverlay, loadingOverlay;
    private EditText editItemName, editItemPrice, editMenuName, editMenuDescription, editDiscountAmount, editItemDescription;
    private Button btnChooseAddItem, btnChooseAddMenu, btnCancelAddChoice, btnSaveItem, btnCancelEdit, btnSaveMenu, btnCancelMenuEdit, btnApplyDiscount, btnCancelDiscount, btnSaveOrder;
    private ProgressBar progressBar;
    private Spinner spinnerMenuSelection;
    private TextView noResults;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private String restaurantID;
    private MenuItem currentMenuItem; // For tracking item being edited
    private Menu currentMenu; // For tracking menu being edited
    private boolean isEditMode = false;
    private boolean itemsReordered = false;
    private Map<String, Integer> menuPositionMap; // To track menu positions in the adapter

    public ManageMenuFragment()
    {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_manage_menu, container, false);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        if(currentUser != null)
        {
            db.collection("Users").document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot ->
                    {
                        if(documentSnapshot.exists())
                        {
                            restaurantID = documentSnapshot.getString("restaurantID"); // Get the field

                            if(restaurantID != null && !restaurantID.isEmpty())
                            {
                                menuAdapter = new ManageMenuAdapter(menuList,
                                        menu ->
                                        {
                                            Log.d("RestaurantMenuFragment", "Clicked on menu: " + menu.getName());
                                            showMenuView(menu);
                                        },
                                        item ->
                                        {
                                            Log.d("RestaurantMenuFragment", "Clicked on item: " + item.getName());
                                            showItemView(item);
                                        },
                                        restaurantID
                                );

                                recyclerViewMenus.setAdapter(menuAdapter);

                                recyclerViewMenus.setLayoutManager(new LinearLayoutManager(getContext()));
                                recyclerViewMenus.setAdapter(menuAdapter);
                                loadMenuData();
                                loadMenusForSpinner();
                            } else
                            {
                                Toast.makeText(getContext(), "No restaurant assigned to user", Toast.LENGTH_SHORT).show();
                            }
                        } else
                        {
                            Toast.makeText(getContext(), "User document not found", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e ->
                    {
                        Toast.makeText(getContext(), "Failed to fetch user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }

        // Initialize UI components
        searchBar = view.findViewById(R.id.searchBar);
        recyclerViewMenus = view.findViewById(R.id.recyclerViewMenus);
        btnCreateDiscount = view.findViewById(R.id.btnCreateDiscount);
        btnSetOrder = view.findViewById(R.id.btnSetOrder);
        btnAdd = view.findViewById(R.id.btnAdd);
        btnSaveOrder = view.findViewById(R.id.btnSaveOrder);

        itemViewOverlay = view.findViewById(R.id.itemViewOverlay);
        menuViewOverlay = view.findViewById(R.id.menuViewOverlay);
        addChoiceOverlay = view.findViewById(R.id.addChoiceOverlay);
        itemEditOverlay = view.findViewById(R.id.itemEditOverlay);
        menuEditOverlay = view.findViewById(R.id.menuEditOverlay);
        discountOverlay = view.findViewById(R.id.discountOverlay);
        loadingOverlay = view.findViewById(R.id.loadingOverlay);

        editItemName = view.findViewById(R.id.editItemName);
        editItemPrice = view.findViewById(R.id.editItemPrice);
        editItemDescription = view.findViewById(R.id.editItemDescription);
        editMenuName = view.findViewById(R.id.editMenuName);
        editMenuDescription = view.findViewById(R.id.editMenuDescription);
        editDiscountAmount = view.findViewById(R.id.editDiscountAmount);

        btnChooseAddItem = view.findViewById(R.id.btnChooseAddItem);
        btnChooseAddMenu = view.findViewById(R.id.btnChooseAddMenu);
        btnCancelAddChoice = view.findViewById(R.id.btnCancelAddChoice);
        btnSaveItem = view.findViewById(R.id.btnSaveItem);
        btnCancelEdit = view.findViewById(R.id.btnCancelEdit);
        btnSaveMenu = view.findViewById(R.id.btnSaveMenu);
        btnCancelMenuEdit = view.findViewById(R.id.btnCancelMenuEdit);
        btnApplyDiscount = view.findViewById(R.id.btnApplyDiscount);
        btnCancelDiscount = view.findViewById(R.id.btnCancelDiscount);

        progressBar = view.findViewById(R.id.progressBar);
        spinnerMenuSelection = view.findViewById(R.id.spinnerMenuSelection);

        noResults = view.findViewById(R.id.noResults);

        setupListeners();
        return view;
    }

    private void setupListeners()
    {
        searchBar.setOnClickListener(v -> searchBar.setIconified(false));
        searchBar.setOnQueryTextListener(new SearchView.OnQueryTextListener()
        {
            @Override
            public boolean onQueryTextSubmit(String query)
            {
                filterResults(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText)
            {
                filterResults(newText);
                return false;
            }
        });

        btnAdd.setOnClickListener(v -> toggleOverlay(addChoiceOverlay, true));
        btnCancelAddChoice.setOnClickListener(v -> toggleOverlay(addChoiceOverlay, false));

        btnChooseAddItem.setOnClickListener(v ->
        {
            isEditMode = false;
            currentMenuItem = null;
            editItemName.setText("");
            editItemPrice.setText("");
            editItemDescription.setText("");
            toggleOverlay(addChoiceOverlay, false);
            toggleOverlay(itemEditOverlay, true);
            itemEditOverlay.findViewById(R.id.btnDeleteItem).setVisibility(View.GONE);

        });

        btnChooseAddMenu.setOnClickListener(v ->
        {
            isEditMode = false;
            currentMenu = null;
            editMenuName.setText("");
            toggleOverlay(addChoiceOverlay, false);
            toggleOverlay(menuEditOverlay, true);
            menuEditOverlay.findViewById(R.id.btnDeleteMenu).setVisibility(View.GONE);

        });

        btnSaveItem.setOnClickListener(v -> saveItem());
        btnCancelEdit.setOnClickListener(v -> toggleOverlay(itemEditOverlay, false));

        btnSaveMenu.setOnClickListener(v -> saveMenu());
        btnCancelMenuEdit.setOnClickListener(v -> toggleOverlay(menuEditOverlay, false));

        btnSetOrder.setOnClickListener(v ->
        {
            Toast.makeText(getContext(), "Rearrange items by dragging", Toast.LENGTH_SHORT).show();
            setupItemTouchHelper();
        });

        btnSaveOrder.setOnClickListener(view -> saveOrder());

        btnCreateDiscount.setOnClickListener(v -> toggleOverlay(discountOverlay, true));
        btnCancelDiscount.setOnClickListener(v -> toggleOverlay(discountOverlay, false));
        btnApplyDiscount.setOnClickListener(v -> applyDiscount());

        // Item View Overlay Buttons
        itemViewOverlay.findViewById(R.id.btnCloseItemView).setOnClickListener(v ->
                toggleOverlay(itemViewOverlay, false));

        itemViewOverlay.findViewById(R.id.btnEditItem).setOnClickListener(v ->
        {
            editExistingItem(currentMenuItem);
            toggleOverlay(itemViewOverlay, false);
            toggleOverlay(itemEditOverlay, true);
        });

        itemEditOverlay.findViewById(R.id.btnDeleteItem).setOnClickListener(v ->
        {
            deleteItem(currentMenuItem);
            toggleOverlay(itemViewOverlay, false);
        });

        // Menu View Overlay Buttons
        menuViewOverlay.findViewById(R.id.btnCloseMenuView).setOnClickListener(v ->
                toggleOverlay(menuViewOverlay, false));

        menuViewOverlay.findViewById(R.id.btnEditMenu).setOnClickListener(v ->
        {
            editExistingMenu(currentMenu);
            toggleOverlay(menuViewOverlay, false);
            toggleOverlay(menuEditOverlay, true);
        });

        menuEditOverlay.findViewById(R.id.btnDeleteMenu).setOnClickListener(v ->
        {
            deleteMenu(currentMenu);
            toggleOverlay(menuViewOverlay, false);
        });
    }

    private void loadMenusForSpinner()
    {
        db.collection("Restaurants").document(restaurantID)
                .collection("Menus")
                .orderBy("menuIndex")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots ->
                {
                    List<String> menuNames = new ArrayList<>();
                    List<String> menuIDs = new ArrayList<>();

                    for(DocumentSnapshot document : queryDocumentSnapshots)
                    {
                        String menuName = document.getString("name");
                        String menuID = document.getId();

                        menuNames.add(menuName);
                        menuIDs.add(menuID);
                    }

                    // Attach menu IDs to the spinner via a tag
                    spinnerMenuSelection.setTag(menuIDs);

                    // Set up adapter
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                            android.R.layout.simple_spinner_dropdown_item, menuNames);
                    spinnerMenuSelection.setAdapter(adapter);
                })
                .addOnFailureListener(e ->
                {
                    Toast.makeText(getContext(), "Failed to load menus: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    private void loadMenuData()
    {
        if(restaurantID == null || restaurantID.isEmpty())
        {
            Toast.makeText(getContext(), "Invalid restaurant ID", Toast.LENGTH_SHORT).show();
            return;
        }
        showLoading(true);

        db.collection("Restaurants").document(restaurantID)
                .collection("Menus")
                .orderBy("menuIndex")
                .get()
                .addOnSuccessListener(snapshot ->
                {
                    // Map current menuList by menuID for quick lookup
                    HashMap<String, Menu> currentMenusMap = new HashMap<>();
                    for(Menu menu : menuList)
                    {
                        currentMenusMap.put(menu.getMenuID(), menu);
                    }

                    List<Menu> updatedMenus = new ArrayList<>();

                    for(QueryDocumentSnapshot doc : snapshot)
                    {
                        String menuID = doc.getId();
                        Menu menu = doc.toObject(Menu.class);
                        menu.setMenuID(menuID);

                        if(currentMenusMap.containsKey(menuID))
                        {
                            // Preserve the existing instance (to maintain index)
                            Menu existingMenu = currentMenusMap.get(menuID);
                            existingMenu.setName(menu.getName());
                            existingMenu.setImageURL(menu.getImageURL());
                            existingMenu.setMenuIndex(menu.getMenuIndex());
                            updatedMenus.add(existingMenu);
                        } else
                        {
                            // New menu item, add it to the list
                            updatedMenus.add(menu);
                        }
                    }

                    // Update the main lists
                    menuList.clear();
                    menuList.addAll(updatedMenus);

                    // The adapter is already set up in onCreateView, just notify it
                    if(menuAdapter != null)
                    {
                        menuAdapter.clearFiltering();  // Clear any existing filters
                    }

                    // Load all menu items for search functionality
                    loadAllMenuItems();

                    showLoading(false);

                    // If there was an active search, reapply it
                    if(searchBar != null && !TextUtils.isEmpty(searchBar.getQuery()))
                    {
                        filterResults(searchBar.getQuery().toString());
                    }
                })
                .addOnFailureListener(e ->
                {
                    showLoading(false);
                    Toast.makeText(getContext(), "Failed to load menus: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadAllMenuItems()
    {
        menuItemList.clear();

        // Track completed menu loads
        AtomicInteger pendingMenuLoads = new AtomicInteger(menuList.size());
        boolean hasActiveSearch = searchBar != null && !TextUtils.isEmpty(searchBar.getQuery());
        final String searchQuery = hasActiveSearch ? searchBar.getQuery().toString() : "";

        // If there are no menus, handle this case explicitly
        if(menuList.isEmpty())
        {
            // Notify adapter data changed or handle empty state
            menuAdapter.notifyDataSetChanged();
            return;
        }

        for(Menu menu : menuList)
        {
            db.collection("Restaurants").document(restaurantID)
                    .collection("Menus").document(menu.getMenuID())
                    .collection("Items")
                    .orderBy("orderIndex")
                    .get()
                    .addOnSuccessListener(snapshot ->
                    {
                        List<MenuItem> menuItems = new ArrayList<>();

                        for(QueryDocumentSnapshot doc : snapshot)
                        {
                            MenuItem item = doc.toObject(MenuItem.class);
                            item.setMenuID(menu.getMenuID());
                            menuItems.add(item);
                            menuItemList.add(item);
                        }

                        // Store the menu items in the adapter
                        menuAdapter.setMenuItems(menu.getMenuID(), menuItems);

                        // If this was the last menu to load
                        if(pendingMenuLoads.decrementAndGet() == 0)
                        {
                            if(hasActiveSearch)
                            {
                                filterResults(searchQuery);
                            } else
                            {
                                // Notify adapter to refresh display even without search
                                menuAdapter.notifyDataSetChanged();
                            }
                        }
                    })
                    .addOnFailureListener(e ->
                    {
                        Log.e("RestaurantMenuFragment", "Error loading items for menu " +
                                menu.getName() + ": " + e.getMessage());

                        // Still decrement and check if all loads (successful or not) are done
                        if(pendingMenuLoads.decrementAndGet() == 0)
                        {
                            if(hasActiveSearch)
                            {
                                filterResults(searchQuery);
                            } else
                            {
                                menuAdapter.notifyDataSetChanged();
                            }
                        }
                    });
        }
    }

    private void filterResults(String query)
    {
        if(TextUtils.isEmpty(query))
        {
            // Reset to full data if search is empty
            menuAdapter.clearFiltering();
            noResults.setVisibility(View.GONE);
            recyclerViewMenus.setVisibility(View.VISIBLE);
        } else
        {
            String lowerQuery = query.toLowerCase();
            Set<String> menuMatchIds = new HashSet<>();
            Set<String> itemMatchMenuIds = new HashSet<>();

            // First pass: Find menus whose names match the query
            for(Menu menu : menuList)
            {
                if(menu.getName().toLowerCase().contains(lowerQuery))
                {
                    menuMatchIds.add(menu.getMenuID());
                }
            }

            // Second pass: Find items whose names match the query
            for(MenuItem item : menuItemList)
            {
                if(item.getName().toLowerCase().contains(lowerQuery))
                {
                    itemMatchMenuIds.add(item.getMenuID());
                }
            }

            // Store search query and set filter data
            menuAdapter.setCurrentSearchQuery(lowerQuery);
            menuAdapter.setFilterData(menuMatchIds, itemMatchMenuIds);

            //Show a "No results" message if no matches found
            if(menuMatchIds.isEmpty() && itemMatchMenuIds.isEmpty())
            {
                // Display a "No results found" message
                noResults.setVisibility(View.VISIBLE);
                recyclerViewMenus.setVisibility(View.GONE);
            } else
            {
                // Hide the "no results" message
                noResults.setVisibility(View.GONE);
                recyclerViewMenus.setVisibility(View.VISIBLE);
            }
        }
    }

    private void applyDiscount()
    {
        String discountText = editDiscountAmount.getText().toString().trim();

        if(discountText.isEmpty())
        {
            Toast.makeText(getContext(), "Please enter a discount amount", Toast.LENGTH_SHORT).show();
            return;
        }

        try
        {
            double discountAmount = Double.parseDouble(discountText);

            if(discountAmount <= 0 || discountAmount > 100)
            {
                Toast.makeText(getContext(), "Discount must be between 0 and 100%", Toast.LENGTH_SHORT).show();
                return;
            }

            showLoading(true);
            List<Task<Void>> updateTasks = new ArrayList<>();

            // Apply discount to individual items
            for(MenuItem item : filteredItems)
            {
                double discountedPrice = item.getPrice() * (1 - (discountAmount / 100));
                Task<Void> updateTask = db.collection("Restaurants").document(restaurantID)
                        .collection("Menus").document(item.getMenuID())
                        .collection("Items").document(item.getItemID())
                        .update("price", discountedPrice, "isSpecialOffer", true);
                updateTasks.add(updateTask);
            }

            // Apply discount to menus (if applicable)
            for(Menu menu : filteredMenus)
            {
                DocumentReference menuRef = db.collection("Restaurants").document(restaurantID)
                        .collection("Menus").document(menu.getMenuID());

                menuRef.get().addOnSuccessListener(documentSnapshot ->
                {
                    if(documentSnapshot.exists() && documentSnapshot.contains("price"))
                    {
                        double originalPrice = documentSnapshot.getDouble("price");
                        double discountedPrice = originalPrice * (1 - (discountAmount / 100));

                        Task<Void> updateTask = menuRef.update("price", discountedPrice, "isSpecialOffer", true);
                        updateTasks.add(updateTask);
                    }
                });
            }
            Tasks.whenAllComplete(updateTasks).addOnCompleteListener(t ->
            {
                showLoading(false);
                Toast.makeText(getContext(), "Discount applied", Toast.LENGTH_SHORT).show();
                loadMenuData();
                toggleOverlay(discountOverlay, false);
            });
        } catch(NumberFormatException e)
        {
            Toast.makeText(getContext(), "Please enter a valid discount percentage", Toast.LENGTH_SHORT).show();
        }
    }

    private void showItemView(MenuItem item)
    {
        currentMenuItem = item;
        toggleOverlay(itemViewOverlay, true);

        TextView itemViewName = itemViewOverlay.findViewById(R.id.itemViewName);
        TextView itemViewPrice = itemViewOverlay.findViewById(R.id.itemViewPrice);
        TextView itemViewDescription = itemViewOverlay.findViewById(R.id.itemViewDescription);
        TextView itemViewAvailability = itemViewOverlay.findViewById(R.id.itemViewAvailability);

        itemViewName.setText(item.getName());

        // Handle null price and format currency
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();
        double price = item.getPrice();
        itemViewPrice.setText(currencyFormat.format(price));

        itemViewDescription.setText(item.getDescription() != null ? item.getDescription() : "No description available");

        // Show availability status
        if(item.getAvailability() != null && item.getAvailability())
        {
            itemViewAvailability.setText("Available");
            itemViewAvailability.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else
        {
            itemViewAvailability.setText("Unavailable");
            itemViewAvailability.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }

    private void showMenuView(Menu menu)
    {
        currentMenu = menu;
        toggleOverlay(menuViewOverlay, true);

        TextView menuViewName = menuViewOverlay.findViewById(R.id.menuViewName);
        TextView menuViewDescription = menuViewOverlay.findViewById(R.id.menuViewDescription);
        TextView menuItemCount = menuViewOverlay.findViewById(R.id.menuItemCount);

        menuViewName.setText(menu.getName());
        menuViewDescription.setText(menu.getDescription() != null ? menu.getDescription() : "No description available");

        // Load menu item count
        db.collection("Restaurants").document(restaurantID)
                .collection("Menus").document(menu.getMenuID())
                .collection("Items")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots ->
                {
                    int count = queryDocumentSnapshots.size();
                    menuItemCount.setText(count + " items");
                })
                .addOnFailureListener(e ->
                {
                    Toast.makeText(getContext(), "Failed to load menu items: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void editExistingItem(MenuItem item)
    {
        isEditMode = true;
        currentMenuItem = item;

        editItemName.setText(item.getName());
        editItemPrice.setText(String.valueOf(item.getPrice()));
        editItemDescription.setText(item.getDescription());

        itemEditOverlay.findViewById(R.id.btnDeleteItem).setVisibility(View.VISIBLE);

        toggleOverlay(itemEditOverlay, true);
    }

    private void editExistingMenu(Menu menu)
    {
        isEditMode = true;
        currentMenu = menu;

        editMenuName.setText(menu.getName());
        editMenuDescription.setText(menu.getDescription());

        menuEditOverlay.findViewById(R.id.btnDeleteMenu).setVisibility(View.VISIBLE);

        toggleOverlay(menuEditOverlay, true);
    }

    private void saveItem()
    {
        // Get input values
        String name = editItemName.getText().toString().trim();
        String priceStr = editItemPrice.getText().toString().trim();
        String description = editItemDescription.getText().toString().trim();

        // Validate inputs
        if(name.isEmpty() || priceStr.isEmpty())
        {
            Toast.makeText(getContext(), "Please enter name and price", Toast.LENGTH_SHORT).show();
            return;
        }

        // Parse price
        double price;
        try
        {
            price = Double.parseDouble(priceStr);
        } catch(NumberFormatException e)
        {
            Toast.makeText(getContext(), "Please enter a valid price", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get selected menu from spinner
        int selectedMenuIndex = spinnerMenuSelection.getSelectedItemPosition();
        if(selectedMenuIndex == -1)
        {
            Toast.makeText(getContext(), "Please select a menu", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> menuIDs = (List<String>) spinnerMenuSelection.getTag();
        String selectedMenuID = menuIDs.get(selectedMenuIndex);

        showLoading(true);

        if(isEditMode && currentMenuItem != null)
        {
            boolean isMovingMenus = !currentMenuItem.getMenuID().equals(selectedMenuID);
            String currentMenuID = currentMenuItem.getMenuID();

            if(isMovingMenus)
            {
                // 1. Get the new index BEFORE moving the item
                getItemAmount(selectedMenuID, itemCount ->
                {
                    int oldIndex = currentMenuItem.getOrderIndex();
                    // Reference for new location
                    DocumentReference newItemRef = db.collection("Restaurants")
                            .document(restaurantID)
                            .collection("Menus").document(selectedMenuID)
                            .collection("Items").document(currentMenuItem.getItemID()); // Keep same ID

                    currentMenuItem.setMenuID(selectedMenuID); // Assign new menu ID
                    currentMenuItem.setName(name);
                    currentMenuItem.setPrice(price);
                    currentMenuItem.setDescription(description);
                    currentMenuItem.setOrderIndex(itemCount);


                    newItemRef.set(currentMenuItem)
                            .addOnSuccessListener(aVoid ->
                            {
                                // 2. Delete old item AFTER saving to new menu
                                db.collection("Restaurants").document(restaurantID)
                                        .collection("Menus").document(currentMenuID)
                                        .collection("Items").document(currentMenuItem.getItemID())
                                        .delete()
                                        .addOnSuccessListener(aVoid2 -> Log.d("MoveItem", "Old item deleted successfully"))
                                        .addOnFailureListener(e -> Log.e("MoveItem", "Failed to delete old item: " + e.getMessage()));

                                // 3. Shift indexes down in old menu
                                shiftIndexesDown(currentMenuID, oldIndex, () ->
                                {
                                    showLoading(false);
                                    Toast.makeText(getContext(), "Item moved successfully", Toast.LENGTH_SHORT).show();
                                    loadMenuData();
                                    filterResults(searchBar.getQuery().toString());
                                    toggleOverlay(itemEditOverlay, false);
                                });
                            })
                            .addOnFailureListener(e ->
                            {
                                showLoading(false);
                                Toast.makeText(getContext(), "Failed to move item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                });
            } else
            {
                // If not moving menus, just update the existing item
                DocumentReference itemRef = db.collection("Restaurants")
                        .document(restaurantID)
                        .collection("Menus").document(currentMenuID)
                        .collection("Items").document(currentMenuItem.getItemID());

                currentMenuItem.setName(name);
                currentMenuItem.setPrice(price);
                currentMenuItem.setDescription(description);

                itemRef.set(currentMenuItem)
                        .addOnSuccessListener(aVoid ->
                        {
                            showLoading(false);
                            Toast.makeText(getContext(), "Item updated successfully", Toast.LENGTH_SHORT).show();
                            loadMenuData();
                            filterResults(searchBar.getQuery().toString());
                            toggleOverlay(itemEditOverlay, false);
                        })
                        .addOnFailureListener(e ->
                        {
                            showLoading(false);
                            Toast.makeText(getContext(), "Failed to update item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }
        } else
        {
            getItemAmount(selectedMenuID, itemCount ->
            {
                // Create new item
                DocumentReference newItemRef = db.collection("Restaurants").document(restaurantID)
                        .collection("Menus").document(selectedMenuID)
                        .collection("Items").document();

                String newItemID = newItemRef.getId(); // Get the generated ID first

                MenuItem newItem = new MenuItem();
                newItem.setItemID(newItemID);
                newItem.setName(name);
                newItem.setPrice(price);
                newItem.setDescription(description);
                newItem.setAvailability(true);
                newItem.setStatus("Active");
                newItem.setOrderIndex(itemCount);
                newItem.setRestaurantID(restaurantID);
                newItem.setMenuID(selectedMenuID);


                newItemRef.set(newItem)
                        .addOnSuccessListener(aVoid ->
                        {
                            showLoading(false);
                            Toast.makeText(getContext(), "Item added successfully", Toast.LENGTH_SHORT).show();
                            menuItemList.add(newItem);
                            loadMenuData();
                            filterResults(searchBar.getQuery().toString());
                            toggleOverlay(itemEditOverlay, false);
                        })
                        .addOnFailureListener(e ->
                        {
                            showLoading(false);
                            Toast.makeText(getContext(), "Failed to add item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            });
        }
    }

    private void getItemAmount(String menuID, OnItemCountFetchedListener listener)
    {
        db.collection("Restaurants").document(restaurantID)
                .collection("Menus").document(menuID)
                .collection("Items")
                .get()
                .addOnSuccessListener(snapshot ->
                {
                    int itemCount = snapshot.size();  // Get number of items
                    listener.onItemCountFetched(itemCount);
                })
                .addOnFailureListener(e ->
                {
                    Log.e("Firestore", "Failed to get item count: " + e.getMessage());
                    listener.onItemCountFetched(0); // Default to 0 if failed
                });
    }

    interface OnItemCountFetchedListener
    {
        void onItemCountFetched(int itemCount);
    }

    private void shiftIndexesDown(String menuID, int removedIndex, Runnable onComplete)
    {
        db.collection("Restaurants").document(restaurantID)
                .collection("Menus").document(menuID)
                .collection("Items")
                .whereGreaterThan("orderIndex", removedIndex) // Get items after the deleted one
                .get()
                .addOnSuccessListener(snapshot ->
                {
                    WriteBatch batch = db.batch();
                    for(QueryDocumentSnapshot doc : snapshot)
                    {
                        MenuItem item = doc.toObject(MenuItem.class);
                        item.setOrderIndex(item.getOrderIndex() - 1);
                        batch.update(doc.getReference(), "orderIndex", item.getOrderIndex());
                    }
                    batch.commit().addOnSuccessListener(aVoid -> onComplete.run());
                })
                .addOnFailureListener(e ->
                {
                    Log.e("ShiftIndexesDown", "Failed to shift indexes: " + e.getMessage());
                    onComplete.run(); // Still continue the flow even if shifting fails
                });
    }


    private void saveMenu()
    {
        String name = editMenuName.getText().toString().trim();
        String description = editMenuDescription.getText().toString().trim();

        if(name.isEmpty())
        {
            Toast.makeText(getContext(), "Please enter a menu name", Toast.LENGTH_SHORT).show();
            return;
        }

        if(restaurantID == null || restaurantID.isEmpty())
        {
            Toast.makeText(getContext(), "Invalid restaurant ID", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        if(isEditMode && currentMenu != null)
        {
            // Update existing menu
            currentMenu.setName(name);
            currentMenu.setDescription(description);

            db.collection("Restaurants").document(restaurantID)
                    .collection("Menus").document(currentMenu.getMenuID())
                    .set(currentMenu)
                    .addOnSuccessListener(aVoid ->
                    {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Menu updated successfully", Toast.LENGTH_SHORT).show();
                        loadMenuData();
                        filterResults(searchBar.getQuery().toString());
                        loadMenusForSpinner();
                        toggleOverlay(menuEditOverlay, false);
                    })
                    .addOnFailureListener(e ->
                    {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Failed to update menu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

        } else
        {
            // Create new menu
            DocumentReference menuRef = db.collection("Restaurants").document(restaurantID)
                    .collection("Menus").document();

            Menu newMenu = new Menu();
            newMenu.setName(name);
            newMenu.setDescription(description);
            newMenu.setMenuID(menuRef.getId());
            newMenu.setRestaurantID(restaurantID);
            newMenu.setTimeCreated(Timestamp.now());
            newMenu.setMenuIndex(menuList.size());

            menuRef.set(newMenu)
                    .addOnSuccessListener(aVoid ->
                    {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Menu added successfully", Toast.LENGTH_SHORT).show();
                        menuList.add(newMenu);
                        filteredMenus.add(newMenu);
                        menuAdapter.notifyDataSetChanged();
                        loadMenuData();
                        filterResults(searchBar.getQuery().toString());
                        loadMenusForSpinner();
                        toggleOverlay(menuEditOverlay, false);
                    })
                    .addOnFailureListener(e ->
                    {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Failed to add menu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void deleteItem(MenuItem item)
    {
        if(item == null || item.getItemID() == null || item.getMenuID() == null || restaurantID == null)
        {
            Log.e("DeleteItem", "Error: Missing data for deletion!");
            Log.e("DeleteItem", "item: " + item);
            Log.e("DeleteItem", "item.getItemID(): " + (item != null ? item.getItemID() : "null"));
            Log.e("DeleteItem", "item.getMenuID(): " + (item != null ? item.getMenuID() : "null"));
            Log.e("DeleteItem", "restaurantID: " + restaurantID);

            Toast.makeText(getContext(), "Invalid item or menu", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show confirmation dialog
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Item")
                .setMessage("Are you sure you want to delete this item?")
                .setPositiveButton("Delete", (dialog, which) ->
                {
                    String currentMenuID = item.getMenuID();
                    int index = item.getOrderIndex();

                    // Reference to the item inside its menu - use item.getMenuID() instead of currentMenu.getMenuID()
                    db.collection("Restaurants")
                            .document(restaurantID)
                            .collection("Menus")
                            .document(item.getMenuID())
                            .collection("Items")
                            .document(item.getItemID())
                            .delete()
                            .addOnSuccessListener(aVoid ->
                            {
                                shiftIndexesDown(currentMenuID, index, () ->
                                {
                                    Toast.makeText(getContext(), "Item deleted successfully", Toast.LENGTH_SHORT).show();
                                    menuItemList.remove(item);
                                    filteredItems.remove(item);
                                    loadMenuData();
                                });
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(getContext(), "Failed to delete item: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }


    private void deleteMenu(Menu menu)
    {
        if(menu == null || menu.getMenuID() == null || restaurantID == null)
        {
            Toast.makeText(getContext(), "Invalid menu or restaurant ID", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show confirmation dialog
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Menu")
                .setMessage("This will also delete all menu items. Are you sure?")
                .setPositiveButton("Delete", (dialog, which) ->
                {
                    // Reference to the menu's items collection
                    CollectionReference itemsRef = db.collection("Restaurants")
                            .document(restaurantID)
                            .collection("Menus")
                            .document(menu.getMenuID())
                            .collection("Items");

                    // First, delete all items in the menu
                    itemsRef.get()
                            .addOnSuccessListener(queryDocumentSnapshots ->
                            {
                                WriteBatch batch = db.batch();

                                for(DocumentSnapshot doc : queryDocumentSnapshots.getDocuments())
                                {
                                    batch.delete(doc.getReference()); // Delete each item
                                }

                                // Execute batch deletion
                                batch.commit().addOnSuccessListener(aVoid ->
                                {
                                    // Now delete the menu itself
                                    db.collection("Restaurants")
                                            .document(restaurantID)
                                            .collection("Menus")
                                            .document(menu.getMenuID())
                                            .delete()
                                            .addOnSuccessListener(aVoid2 ->
                                            {
                                                Toast.makeText(getContext(), "Menu and items deleted successfully", Toast.LENGTH_SHORT).show();
                                                menuList.remove(menu);
                                                filteredMenus.remove(menu);
                                                menuAdapter.notifyDataSetChanged();
                                                loadMenuData();
                                                loadMenusForSpinner();
                                            })
                                            .addOnFailureListener(e ->
                                                    Toast.makeText(getContext(), "Failed to delete menu: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                                }).addOnFailureListener(e ->
                                        Toast.makeText(getContext(), "Failed to delete menu items: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(getContext(), "Failed to check menu items: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }


    private void setupItemTouchHelper()
    {
        ItemTouchHelper.Callback callback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.START | ItemTouchHelper.END, 0)
        {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target)
            {
                int fromPosition = viewHolder.getBindingAdapterPosition();
                int toPosition = target.getBindingAdapterPosition();

                if (fromPosition == RecyclerView.NO_POSITION || toPosition == RecyclerView.NO_POSITION
                        || fromPosition >= filteredItems.size() || toPosition >= filteredItems.size()) {
                    return false;
                }

                // Get menuIDs for source and destination positions
                String sourceMenuId = getMenuIdForPosition(fromPosition);
                String destinationMenuId = getMenuIdForPosition(toPosition);

                // Update menuID if item is moved to a different menu
                if (!sourceMenuId.equals(destinationMenuId)) {
                    filteredItems.get(fromPosition).setMenuID(destinationMenuId);
                }

                // Swap items in local list
                Collections.swap(filteredItems, fromPosition, toPosition);

                // Update order indexes
                updateOrderIndexesForAllMenus();

                // Mark as reordered and show save button
                itemsReordered = true;
                btnSaveOrder.setVisibility(View.VISIBLE);

                // Ensure UI updates correctly after move
                menuAdapter.notifyItemMoved(fromPosition, toPosition);

                return true;
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder)
            {
                super.clearView(recyclerView, viewHolder);
                // Ensure UI updates correctly after drag
                menuAdapter.notifyDataSetChanged();
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction)
            {
                // No swipe actions needed
            }
        };

        new ItemTouchHelper(callback).attachToRecyclerView(recyclerViewMenus);
    }

    // Retrieve the menuID for a given item position
    private String getMenuIdForPosition(int position)
    {
        if (position >= 0 && position < filteredItems.size())
        {
            return filteredItems.get(position).getMenuID();
        }
        return ""; // Default in case of an invalid position
    }

    // Update order indexes for all menus separately
    private void updateOrderIndexesForAllMenus()
    {
        Map<String, List<MenuItem>> itemsByMenu = new HashMap<>();

        // Group items by menuID
        for (MenuItem item : filteredItems)
        {
            String menuId = item.getMenuID();
            itemsByMenu.putIfAbsent(menuId, new ArrayList<>());
            itemsByMenu.get(menuId).add(item);
        }

        // Update order indexes within each menu separately
        for (Map.Entry<String, List<MenuItem>> entry : itemsByMenu.entrySet())
        {
            List<MenuItem> menuItems = entry.getValue();
            for (int i = 0; i < menuItems.size(); i++)
            {
                menuItems.get(i).setOrderIndex(i);
            }
        }

        // Apply back to filteredItems
        filteredItems.clear();
        for (List<MenuItem> menuItems : itemsByMenu.values())
        {
            filteredItems.addAll(menuItems);
        }

        // Force UI update
        menuAdapter.notifyDataSetChanged();
    }

    private void rearrangeItems()
    {
        // Initialize menu position map based on adapter structure
        setupMenuPositionMap();

        Toast.makeText(getContext(), "You can now drag and drop items to reorder", Toast.LENGTH_SHORT).show();
        setupItemTouchHelper();

        // Hide Save button at the start, show it only if changes are made
        btnSaveOrder.setVisibility(View.GONE);
        itemsReordered = false;
    }

    // Setup menu position map based on adapter
    private void setupMenuPositionMap()
    {
        menuPositionMap = new HashMap<>();

        int currentPosition = 0;
        for (Menu menu : menuList) // menusList should be your list of menus
        {
            menuPositionMap.put(menu.getMenuID(), currentPosition);
            currentPosition += getItemCountForMenu(menu.getMenuID()); // Adjust based on your adapter logic
        }
    }

    // Get item count for a menu
    private int getItemCountForMenu(String menuID)
    {
        int count = 0;
        for (MenuItem item : filteredItems)
        {
            if (item.getMenuID().equals(menuID))
            {
                count++;
            }
        }
        return count;
    }

    // Save the reordered items to Firestore
    private void saveOrder()
    {
        if (itemsReordered)
        {
            WriteBatch batch = db.batch();

            for (MenuItem item : filteredItems)
            {
                DocumentReference itemRef = db.collection("Restaurants").document(restaurantID)
                        .collection("Menus").document(item.getMenuID())
                        .collection("Items").document(item.getItemID());

                Map<String, Object> updates = new HashMap<>();
                updates.put("orderIndex", item.getOrderIndex());
                updates.put("menuID", item.getMenuID());

                batch.update(itemRef, updates);
            }

            batch.commit()
                    .addOnSuccessListener(aVoid ->
                    {
                        Toast.makeText(getContext(), "Items reordered successfully", Toast.LENGTH_SHORT).show();

                        // Hide save button only on success
                        btnSaveOrder.setVisibility(View.GONE);
                        itemsReordered = false;
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(), "Failed to update order: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    private void showLoading(boolean isLoading)
    {
        if(progressBar != null)
        {
            toggleOverlay(loadingOverlay, isLoading);
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
    }

    private void toggleOverlay(RelativeLayout overlay, boolean show)
    {
        itemViewOverlay.setVisibility(View.GONE);
        menuViewOverlay.setVisibility(View.GONE);
        addChoiceOverlay.setVisibility(View.GONE);
        itemEditOverlay.setVisibility(View.GONE);
        menuEditOverlay.setVisibility(View.GONE);
        discountOverlay.setVisibility(View.GONE);
        loadingOverlay.setVisibility(View.GONE);

        overlay.setVisibility(show ? View.VISIBLE : View.GONE);
        if(show)
        {
            overlay.setVisibility(View.VISIBLE);
            overlay.setClickable(true);
            overlay.setFocusable(true);
        } else
        {
            overlay.setVisibility(View.GONE);
            overlay.setClickable(false);
            overlay.setFocusable(false);
        }
    }
}