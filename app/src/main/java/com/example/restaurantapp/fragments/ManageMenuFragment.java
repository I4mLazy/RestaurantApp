package com.example.restaurantapp.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.restaurantapp.R;
import com.example.restaurantapp.adapters.MenuAdapter;
import com.example.restaurantapp.models.Menu;
import com.example.restaurantapp.models.MenuItem;
import com.example.restaurantapp.utils.DiscountUtils;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class ManageMenuFragment extends Fragment
{

    private SearchView searchBar;
    private RecyclerView recyclerViewMenus;
    private MenuAdapter menuAdapter;
    private List<Menu> menuList = new ArrayList<>();
    private List<MenuItem> menuItemList = new ArrayList<>();
    private List<Menu> filteredMenus = new ArrayList<>();
    private List<MenuItem> filteredItems = new ArrayList<>();

    private ImageButton btnCreateDiscount, btnSetOrder, btnAdd, itemEditImage, menuEditImage;
    private ImageView menuViewImage, itemViewImage;
    private RelativeLayout itemViewOverlay, menuViewOverlay, addChoiceOverlay, itemEditOverlay, menuEditOverlay, discountOverlay, loadingOverlay;
    private EditText editItemName, editItemPrice, editItemDescription, editItemAllergens, editItemCategory, editMenuName, editMenuDescription, editDiscountAmount;
    private TextInputLayout editItemNameLayout, editItemPriceLayout, editItemDescriptionLayout, editItemAllergensLayout, editItemCategoryLayout;
    private Button btnChooseAddItem, btnChooseAddMenu, btnCancelAddChoice, btnSaveItem, btnCancelEdit, btnSaveMenu, btnCancelMenuEdit, btnChooseForDiscount, btnApplyDiscount, btnCancelDiscount, btnSaveOrder;
    private ProgressBar progressBar;
    private Switch switchEnableSchedule;
    private RadioGroup radioDiscountType, radioApplyScope;
    private Spinner spinnerMenuSelection, spinnerDiscountMenu, spinnerDiscountItem;
    private DatePicker startDatePicker, endDatePicker;
    private TimePicker startTimePicker, endTimePicker;
    private TextView noResults, itemEditImageTextView, menuEditImageTextView, menuViewName, menuViewDescription, menuItemCount,
            itemViewName, itemViewPrice, itemViewDescription, itemViewAvailability, itemViewCategory, itemViewAllergens,
            discountBadge, oldPrice, itemEditOverlayName, menuEditOverlayName;
    private CheckBox editItemAvailability;
    private BottomSheetDialog imageBottomSheetDialog;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private StorageReference storageRef;
    private FirebaseStorage storage;

    private String restaurantID;
    private MenuItem currentMenuItem; // For tracking item being edited
    private Menu currentMenu; // For tracking menu being edited
    private String currentType;
    private String discountType;
    private boolean isUploading = false;
    private boolean isEditMode = false;
    private boolean itemsReordered = false;
    private boolean scheduleOn = false;
    private boolean imageEdited = false;
    private boolean newItem = false;
    private boolean newMenu = false;
    boolean deleteMode = false;
    private Uri photoUri;
    private Bitmap bitmap;
    private UploadTask currentUploadTask;
    private static final String TAG = "ManageMenuFragment";

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
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

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
                                menuAdapter = new MenuAdapter(menuList,
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

                                recyclerViewMenus.setLayoutManager(new LinearLayoutManager(getContext()));
                                recyclerViewMenus.setAdapter(menuAdapter);
                                loadMenuData();
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
        editItemAllergens = view.findViewById(R.id.editItemAllergens);
        editItemCategory = view.findViewById(R.id.editItemCategory);
        editMenuName = view.findViewById(R.id.editMenuName);
        editMenuDescription = view.findViewById(R.id.editMenuDescription);
        editDiscountAmount = view.findViewById(R.id.editDiscountAmount);

        editItemNameLayout = view.findViewById(R.id.editItemNameLayout);
        editItemPriceLayout = view.findViewById(R.id.editItemPriceLayout);
        editItemDescriptionLayout = view.findViewById(R.id.editItemDescriptionLayout);
        editItemAllergensLayout = view.findViewById(R.id.editItemAllergensLayout);
        editItemCategoryLayout = view.findViewById(R.id.editItemCategoryLayout);

        btnChooseAddItem = view.findViewById(R.id.btnChooseAddItem);
        btnChooseAddMenu = view.findViewById(R.id.btnChooseAddMenu);
        btnCancelAddChoice = view.findViewById(R.id.btnCancelAddChoice);
        btnSaveItem = view.findViewById(R.id.btnSaveItem);
        btnCancelEdit = view.findViewById(R.id.btnCancelEdit);
        btnSaveMenu = view.findViewById(R.id.btnSaveMenu);
        btnCancelMenuEdit = view.findViewById(R.id.btnCancelMenuEdit);
        btnChooseForDiscount = view.findViewById(R.id.btnChooseForDiscount);
        btnApplyDiscount = view.findViewById(R.id.btnApplyDiscount);
        btnCancelDiscount = view.findViewById(R.id.btnCancelDiscount);

        switchEnableSchedule = view.findViewById(R.id.switchEnableSchedule);

        radioDiscountType = view.findViewById(R.id.radioDiscountType);
        radioApplyScope = view.findViewById(R.id.radioApplyScope);

        editItemAvailability = view.findViewById(R.id.editItemAvailability);

        progressBar = view.findViewById(R.id.progressBar);

        spinnerMenuSelection = view.findViewById(R.id.spinnerMenuSelection);
        spinnerDiscountMenu = view.findViewById(R.id.spinnerDiscountMenu);
        spinnerDiscountItem = view.findViewById(R.id.spinnerDiscountItem);

        startDatePicker = view.findViewById(R.id.datePickerStart);
        endDatePicker = view.findViewById(R.id.datePickerEnd);

        startTimePicker = view.findViewById(R.id.timePickerStart);
        startTimePicker.setIs24HourView(true);
        endTimePicker = view.findViewById(R.id.timePickerEnd);
        endTimePicker.setIs24HourView(true);

        itemEditImage = view.findViewById(R.id.editItemImage);
        menuEditImage = view.findViewById(R.id.editMenuImage);

        itemViewImage = itemViewOverlay.findViewById(R.id.itemViewImage);
        menuViewImage = menuViewOverlay.findViewById(R.id.menuViewImage);

        itemEditImageTextView = view.findViewById(R.id.itemEditImageTextView);
        menuEditImageTextView = view.findViewById(R.id.menuEditImageTextView);
        itemViewAllergens = itemViewOverlay.findViewById(R.id.itemViewAllergens);
        itemViewAvailability = itemViewOverlay.findViewById(R.id.itemViewAvailability);
        itemViewCategory = itemViewOverlay.findViewById(R.id.itemViewCategory);
        itemViewDescription = itemViewOverlay.findViewById(R.id.itemViewDescription);
        itemViewName = itemViewOverlay.findViewById(R.id.itemViewName);
        itemViewPrice = itemViewOverlay.findViewById(R.id.itemViewPrice);
        menuItemCount = menuViewOverlay.findViewById(R.id.menuItemCount);
        menuViewDescription = menuViewOverlay.findViewById(R.id.menuViewDescription);
        menuViewName = menuViewOverlay.findViewById(R.id.menuViewName);
        discountBadge = itemViewOverlay.findViewById(R.id.discountBadge);
        oldPrice = itemViewOverlay.findViewById(R.id.oldPrice);
        itemEditOverlayName = itemEditOverlay.findViewById(R.id.itemEditOverlayName);
        menuEditOverlayName = menuEditOverlay.findViewById(R.id.menuEditOverlayName);

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

        btnAdd.setOnClickListener(v ->
        {
            if(restaurantID != null)
            {
                toggleOverlay(addChoiceOverlay, true);
            } else
            {
                Toast.makeText(requireContext(), "Invalid restaurant ID", Toast.LENGTH_SHORT).show();
            }
        });
        btnCancelAddChoice.setOnClickListener(v -> toggleOverlay(addChoiceOverlay, false));

        btnChooseAddItem.setOnClickListener(v ->
        {
            if(restaurantID == null || restaurantID.isEmpty())
            {
                Toast.makeText(getContext(), "Invalid restaurant ID", Toast.LENGTH_SHORT).show();
                return;
            }
            if(menuList == null || menuList.isEmpty())
            {
                Log.e(TAG, "menuList is null or empty — not loading menus/items");
                Toast.makeText(requireContext(), "You need at least one menu to create items", Toast.LENGTH_LONG).show();
                return;
            }

            imageEdited = false;
            isEditMode = false;
            currentType = "MenuItem";
            currentMenuItem = new MenuItem();

            itemEditOverlayName.setText("Create Menu Item");
            editItemName.setText("");
            editItemPrice.setText("");
            editItemDescription.setText("");

            editItemCategory.setText("");
            editItemAvailability.setChecked(true); // Default to available
            editItemAllergens.setText("");

            itemEditImage.setImageResource(R.drawable.image_placeholder);
            loadMenusForSpinner(spinnerMenuSelection);
            toggleOverlay(addChoiceOverlay, false);
            toggleOverlay(itemEditOverlay, true);
            itemEditOverlay.findViewById(R.id.btnDeleteItem).setVisibility(View.GONE);

            itemEditImage.setOnClickListener(view ->
            {
                Log.d(TAG, "Editing item image...");
                editImage();
            });

            itemEditImageTextView.setOnClickListener(view ->
            {
                Log.d(TAG, "Editing item image (TextView)...");
                editImage();
            });
        });


        btnChooseAddMenu.setOnClickListener(v ->
        {
            if(restaurantID == null || restaurantID.isEmpty())
            {
                Toast.makeText(getContext(), "Invalid restaurant ID", Toast.LENGTH_SHORT).show();
                return;
            }
            imageEdited = false;
            isEditMode = false;
            currentType = "Menu";
            currentMenu = new Menu();
            menuEditOverlayName.setText("Create Menu");
            editMenuName.setText("");
            menuEditImage.setImageResource(R.drawable.image_placeholder);
            toggleOverlay(addChoiceOverlay, false);
            toggleOverlay(menuEditOverlay, true);
            menuEditOverlay.findViewById(R.id.btnDeleteMenu).setVisibility(View.GONE);

            menuEditImage.setOnClickListener(view ->
            {
                Log.d(TAG, "Editing menu image...");
                editImage();
            });

            menuEditImageTextView.setOnClickListener(view ->
            {
                Log.d(TAG, "Editing menu image (TextView)...");
                editImage();
            });
        });

        btnSaveItem.setOnClickListener(v -> saveItem());
        btnCancelEdit.setOnClickListener(v -> toggleOverlay(itemEditOverlay, false));

        btnSaveMenu.setOnClickListener(v -> saveMenu());
        btnCancelMenuEdit.setOnClickListener(v -> toggleOverlay(menuEditOverlay, false));

        btnSetOrder.setOnClickListener(v ->
        {
            if(restaurantID == null || restaurantID.isEmpty())
            {
                Toast.makeText(getContext(), "Invalid restaurant ID", Toast.LENGTH_SHORT).show();
                return;
            }
            searchBar.setQuery("", true);
            Toast.makeText(getContext(), "Rearrange items by dragging", Toast.LENGTH_SHORT).show();
            //setupItemTouchHelper();
        });

        //btnSaveOrder.setOnClickListener(view -> saveOrder());
        btnCreateDiscount.setOnClickListener(v ->
        {
            // Check if menuList is not empty before proceeding with any discount creation
            if(menuList == null || menuList.isEmpty())
            {
                Log.e(TAG, "menuList is null or empty — not loading menus/items");
                Toast.makeText(requireContext(), "You need at least one menu to create discounts", Toast.LENGTH_LONG).show();
                return;
            }

            toggleOverlay(discountOverlay, true);
            switchEnableSchedule.setChecked(false);
            radioApplyScope.check(R.id.radioApplyToMenuOrItem);
            radioDiscountType.check(R.id.radioPercentage);
            discountOverlay.findViewById(R.id.layoutSchedule).setVisibility(View.GONE);
            scheduleOn = false;
            editDiscountAmount.setText("");

            // Load available menus into the spinner
            loadMenusForSpinner(spinnerDiscountMenu);

            Menu firstMenu = menuList.get(0);
            if(firstMenu != null && firstMenu.getMenuID() != null)
            {
                loadItemsForSpinner(spinnerDiscountItem, firstMenu.getMenuID());
            } else
            {
                Log.e(TAG, "First menu or menu ID is null");
                // Clear the items spinner or show a message
                Toast.makeText(requireContext(), "Invalid menu ID.", Toast.LENGTH_SHORT).show();
            }
        });


        btnChooseForDiscount.setOnClickListener(v -> toggleOverlay(discountOverlay, false));
        btnApplyDiscount.setOnClickListener(v -> applyDiscount());
        btnCancelDiscount.setOnClickListener(v -> toggleOverlay(discountOverlay, false));

        radioDiscountType.setOnCheckedChangeListener((group, checkedId) ->
        {
            if(checkedId == R.id.radioPercentage)
            {
                discountType = "Percentage";
            } else if(checkedId == R.id.radioFlat)
            {
                discountType = "Flat";
            }
        });

        radioApplyScope.setOnCheckedChangeListener((group, checkedId) ->
        {
            if(checkedId == R.id.radioApplyToMenuOrItem)
            {
                btnApplyDiscount.setVisibility(View.VISIBLE);
                btnChooseForDiscount.setVisibility(View.GONE);
                spinnerDiscountMenu.setVisibility(View.VISIBLE);
                spinnerDiscountItem.setVisibility(View.VISIBLE);
                loadMenusForSpinner(spinnerDiscountMenu);
                loadItemsForSpinner(spinnerDiscountItem, menuList.get(0).getMenuID());
            } else if(checkedId == R.id.radioManualSelect)
            {
                btnApplyDiscount.setVisibility(View.GONE);
                btnChooseForDiscount.setVisibility(View.VISIBLE);
                spinnerDiscountMenu.setVisibility(View.GONE);
                spinnerDiscountItem.setVisibility(View.GONE);
            }
        });


        spinnerDiscountMenu.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                List<String> menuIDs = (List<String>) spinnerDiscountMenu.getTag();
                if(menuIDs != null && position < menuIDs.size())
                {
                    String selectedMenuID = menuIDs.get(position);
                    loadItemsForSpinner(spinnerDiscountItem, selectedMenuID);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {
                // Optional: handle case when nothing is selected, if needed
            }
        });


        switchEnableSchedule.setOnCheckedChangeListener((buttonView, isChecked) ->
        {
            if(isChecked)
            {
                discountOverlay.findViewById(R.id.layoutSchedule).setVisibility(View.VISIBLE);
                scheduleOn = true;
            } else
            {
                discountOverlay.findViewById(R.id.layoutSchedule).setVisibility(View.GONE);
                scheduleOn = false;
            }
        });


        // Item View Overlay Buttons
        itemViewOverlay.findViewById(R.id.btnCloseItemView).setOnClickListener(v ->
        {
            toggleOverlay(itemViewOverlay, false);
            oldPrice.setVisibility(View.GONE);
            discountBadge.setVisibility(View.GONE);
        });

        itemViewOverlay.findViewById(R.id.btnEditItem).setOnClickListener(v ->
        {
            loadMenusForSpinner(spinnerMenuSelection);
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

    private void loadMenusForSpinner(Spinner spinner)
    {
        if(restaurantID == null || restaurantID.isEmpty())
        {
            Log.e("ManageMenuFragment", "No restaurant assigned to user");
            return;
        }
        db.collection("Restaurants").document(restaurantID)
                .collection("Menus")
                .orderBy("menuIndex")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots ->
                {
                    if(!isAdded())
                    {
                        Log.e("ManageMenuFragment", "Fragment isn't added");
                        return;
                    }

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
                    spinner.setTag(menuIDs);

                    //Set menu to first menu
                    setSpinnerSelection(menuIDs.get(0));

                    // Set up adapter
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                            android.R.layout.simple_spinner_dropdown_item, menuNames);
                    spinner.setAdapter(adapter);
                })
                .addOnFailureListener(e ->
                {
                    Toast.makeText(getContext(), "Failed to load menus: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadItemsForSpinner(Spinner spinner, String menuID)
    {
        if(restaurantID == null || restaurantID.isEmpty() || menuID == null || menuID.isEmpty())
        {
            Toast.makeText(getContext(), "Invalid restaurant ID or menu ID", Toast.LENGTH_SHORT).show();
            return;
        }
        db.collection("Restaurants").document(restaurantID)
                .collection("Menus").document(menuID)
                .collection("Items").orderBy("orderIndex")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots ->
                {
                    List<String> itemNames = new ArrayList<>();
                    List<String> itemIDs = new ArrayList<>();

                    for(DocumentSnapshot document : queryDocumentSnapshots)
                    {
                        String itemName = document.getString("name");
                        String itemID = document.getId();

                        itemNames.add(itemName);
                        itemIDs.add(itemID);
                    }

                    if(!itemIDs.isEmpty())
                    {
                        itemNames.add(0, "All items in the menu"); // Add at the top
                        itemIDs.add(0, "ALL_ITEMS"); // Custom ID to handle later if needed
                    }

                    // Attach item IDs to the spinner via a tag
                    spinner.setTag(itemIDs);

                    // Set up adapter
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                            android.R.layout.simple_spinner_dropdown_item, itemNames);
                    spinner.setAdapter(adapter);

                    if(!itemIDs.isEmpty())
                    {
                        spinner.setSelection(0); // Select "All items" by default
                    }
                })
                .addOnFailureListener(e ->
                {
                    Toast.makeText(getContext(), "Failed to load menu items: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

    }


    private void setSpinnerSelection(String selectedMenuID)
    {
        List<String> menuIDs = (List<String>) spinnerMenuSelection.getTag(); // Retrieve stored menu IDs

        if(menuIDs != null)
        {
            int index = menuIDs.indexOf(selectedMenuID);
            if(index != -1)
            {
                spinnerMenuSelection.setSelection(index);
            }
        }
    }

    private void loadMenuData()
    {
        if(restaurantID == null || restaurantID.isEmpty())
        {
            Toast.makeText(getContext(), "Invalid restaurant ID", Toast.LENGTH_SHORT).show();
            return;
        }
        //showLoading(true);

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
                            existingMenu.setImageURL(menu.getImageURL()); // Load image URL
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

                    // Update RecyclerView adapter
                    if(menuAdapter != null)
                    {
                        menuAdapter.clearFiltering(); // Clear any existing filters
                        menuAdapter.notifyDataSetChanged();
                    }

                    // Load all menu items for search functionality
                    loadAllMenuItems();

                    //showLoading(false);

                    // If there was an active search, reapply it
                    if(searchBar != null && !TextUtils.isEmpty(searchBar.getQuery()))
                    {
                        filterResults(searchBar.getQuery().toString());
                    }
                })
                .addOnFailureListener(e ->
                {
                    //showLoading(false);
                    Toast.makeText(getContext(), "Failed to load menus: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadAllMenuItems()
    {
        if(menuAdapter == null)
            return;

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
            if(menuAdapter != null)
            {
                menuAdapter.clearFiltering();
            } else
            {
                Log.e("RestaurantMenuFragment", "MenuAdapter is null, cannot clear filtering");
            }
            noResults.setVisibility(View.GONE);
            recyclerViewMenus.setVisibility(View.VISIBLE);
            return;
        }

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
        if(menuAdapter != null)
        {
            menuAdapter.setCurrentSearchQuery(lowerQuery);
            menuAdapter.setFilterData(menuMatchIds, itemMatchMenuIds);
        }

        // Show a "No results" message if no matches found
        boolean noMenuMatch = menuMatchIds.isEmpty();
        boolean noItemMatch = itemMatchMenuIds.isEmpty();

        if(noMenuMatch && noItemMatch)
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


    private void showItemView(MenuItem item)
    {
        currentMenuItem = item;
        toggleOverlay(itemViewOverlay, true);

        itemViewName.setText(item.getName());

        // Handle null price and format currency
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();
        double price = item.getPrice();
        itemViewPrice.setText(currencyFormat.format(price));

        // Show discount if available
        DiscountUtils.applyActiveDiscounts(item, itemViewOverlay.getContext(), (original, current, hasDiscount, isFree, badgeText) ->
        {
            if(hasDiscount)
            {
                itemViewPrice.setText(currencyFormat.format(current));
                oldPrice.setVisibility(View.VISIBLE);
                oldPrice.setText(currencyFormat.format(original)); // Set the original price
                oldPrice.setPaintFlags(oldPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG); // Strikethrough effect
                discountBadge.setVisibility(View.VISIBLE);
                discountBadge.setText(badgeText);
            } else
            {
                oldPrice.setVisibility(View.GONE);
                discountBadge.setVisibility(View.GONE);
            }

            if(isFree)
            {
                discountBadge.setVisibility(View.VISIBLE);
                discountBadge.setText(itemViewOverlay.getContext().getString(R.string.free)); // Show "free" badge
            }
        });

        // Description
        if(item.getDescription() != null && !item.getDescription().isEmpty())
        {
            itemViewDescription.setText(item.getDescription());
            itemViewDescription.setVisibility(View.VISIBLE);
        } else
        {
            itemViewDescription.setVisibility(View.GONE);
        }

        // Category
        if(item.getCategory() != null && !item.getCategory().isEmpty())
        {
            itemViewCategory.setText(item.getCategory());
            itemViewCategory.setVisibility(View.VISIBLE);
        } else
        {
            itemViewCategory.setVisibility(View.GONE);
        }

        // Allergens
        List<String> allergens = item.getAllergens();
        if(allergens != null && !allergens.isEmpty())
        {
            itemViewAllergens.setText(TextUtils.join(", ", allergens));
            itemViewAllergens.setVisibility(View.VISIBLE);
        } else
        {
            itemViewAllergens.setVisibility(View.GONE);
        }

        // Availability status
        if(item.getAvailability() != null && item.getAvailability())
        {
            itemViewAvailability.setText("Available");
            itemViewAvailability.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark));
            itemViewAvailability.setVisibility(View.VISIBLE);
        } else
        {
            itemViewAvailability.setText("Unavailable");
            itemViewAvailability.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark));
            itemViewAvailability.setVisibility(View.VISIBLE);
        }
        String imageUrl = item.getImageURL();
        if(imageUrl != null && !imageUrl.isEmpty())
        {
            Glide.with(requireContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.image_placeholder)
                    .into(itemViewImage);
        } else
        {
            itemViewImage.setImageResource(R.drawable.image_placeholder);
        }
    }


    private void showMenuView(Menu menu)
    {
        currentMenu = menu;
        toggleOverlay(menuViewOverlay, true);

        menuViewName.setText(menu.getName());
        menuViewDescription.setText(menu.getDescription() != null ? menu.getDescription() : "No description available");

        //Load image
        String imageUrl = menu.getImageURL();
        if(imageUrl != null && !imageUrl.isEmpty())
        {
            Glide.with(requireContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.image_placeholder)
                    .into(menuViewImage);
        } else
        {
            menuViewImage.setImageResource(R.drawable.image_placeholder);
        }

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
        imageEdited = false;
        isEditMode = true;
        currentMenuItem = item;
        currentType = "MenuItem";

        itemEditOverlayName.setText("Edit Item");
        editItemName.setText(item.getName());
        editItemPrice.setText(String.valueOf(item.getPrice()));
        editItemDescription.setText(item.getDescription());
        setSpinnerSelection(item.getMenuID());

        // New field: Category
        editItemCategory.setText(item.getCategory() != null ? item.getCategory() : "");

        // New field: IsAvailable (assuming it's a CheckBox or Switch)
        editItemAvailability.setChecked(item.getAvailability());

        // New field: Allergens (convert list to comma-separated string)
        if(item.getAllergens() != null && !item.getAllergens().isEmpty())
        {
            String allergens = TextUtils.join(", ", item.getAllergens());
            editItemAllergens.setText(allergens);
        } else
        {
            editItemAllergens.setText("");
        }

        // Load image
        String imageUrl = item.getImageURL();
        if(imageUrl != null && !imageUrl.isEmpty())
        {
            Glide.with(requireContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.image_placeholder)
                    .into(itemEditImage);
        } else
        {
            itemEditImage.setImageResource(R.drawable.image_placeholder);
        }

        itemEditOverlay.findViewById(R.id.btnDeleteItem).setVisibility(View.VISIBLE);

        itemEditImage.setOnClickListener(v ->
        {
            Log.d(TAG, "Editing item image...");
            editImage();
        });

        itemEditImageTextView.setOnClickListener(v ->
        {
            Log.d(TAG, "Editing item image (TextView)...");
            editImage();
        });

        toggleOverlay(itemEditOverlay, true);
    }


    private void editExistingMenu(Menu menu)
    {
        imageEdited = false;
        isEditMode = true;
        currentMenu = menu;
        currentType = "Menu";

        menuEditOverlayName.setText("Edit Menu");
        editMenuName.setText(menu.getName());
        editMenuDescription.setText(menu.getDescription());

        //Load image
        String imageUrl = menu.getImageURL();
        if(imageUrl != null && !imageUrl.isEmpty())
        {
            Glide.with(requireContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.image_placeholder)
                    .into(menuEditImage);
        } else
        {
            menuEditImage.setImageResource(R.drawable.image_placeholder);
        }

        menuEditOverlay.findViewById(R.id.btnDeleteMenu).setVisibility(View.VISIBLE);

        menuEditImage.setOnClickListener(v ->
        {
            Log.d(TAG, "Editing menu image...");
            editImage();
        });

        menuEditImageTextView.setOnClickListener(v ->
        {
            Log.d(TAG, "Editing menu image (TextView)...");
            editImage();
        });

        toggleOverlay(menuEditOverlay, true);
    }

    private void saveItem()
    {
        // Reset errors
        editItemNameLayout.setError(null);
        editItemPriceLayout.setError(null);
        editItemDescriptionLayout.setError(null);
        editItemCategoryLayout.setError(null);

        boolean hasError = false;

        // Get input values
        String name = editItemName.getText().toString().trim();
        String priceStr = editItemPrice.getText().toString().trim();
        String description = editItemDescription.getText().toString().trim();
        String allergens = editItemAllergens.getText().toString().trim();
        String category = editItemCategory.getText().toString().trim();
        boolean isAvailable = editItemAvailability.isChecked();

        // Name validation
        if(name.isEmpty())
        {
            editItemNameLayout.setError("Item name is required");
            hasError = true;
        }

        // Price validation
        double price = 0;
        if(priceStr.isEmpty())
        {
            editItemPriceLayout.setError("Price is required");
            hasError = true;
        } else
        {
            try
            {
                price = Double.parseDouble(priceStr);
            } catch(NumberFormatException e)
            {
                editItemPriceLayout.setError("Please enter a valid number");
                hasError = true;
            }
        }

        // Description validation
        if(description.isEmpty())
        {
            editItemDescriptionLayout.setError("Description is required");
            hasError = true;
        }

        // Category validation
        if(category.isEmpty())
        {
            editItemCategoryLayout.setError("Category is required");
            hasError = true;
        }

        // Spinner validation
        if(spinnerMenuSelection.getSelectedItemPosition() == -1 || spinnerMenuSelection.getSelectedItem() == null)
        {
            Toast.makeText(getContext(), "Please select a menu", Toast.LENGTH_SHORT).show();
            hasError = true;
        }

        if(hasError)
        {
            return;
        }

        // Inputs are valid, continue
        toggleOverlay(itemEditOverlay, false);

        if(isEditMode && currentMenuItem != null)
        {
            newItem = false;
            if(imageEdited)
            {
                uploadImageToFirebase(bitmap);
            } else
            {
                proceedWithItemUpdate(currentMenuItem.getImageURL());
            }
        } else
        {
            newItem = true;
            showLoading(true);
            if(imageEdited)
            {
                uploadImageToFirebase(bitmap);
            } else
            {
                proceedWithItemSave(null);
            }
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
                    Log.d("ItemAmount", "Item count fetched: " + itemCount + " for Menu ID: " + menuID);
                    listener.onItemCountFetched(itemCount);
                })
                .addOnFailureListener(e ->
                {
                    Log.e("Firestore", "Failed to get item count: " + e.getMessage());
                    listener.onItemCountFetched(0); // Default to 0 if failed
                });
    }

    private void proceedWithItemUpdate(String imageURL)
    {
        // Get input values
        String name = editItemName.getText().toString().trim();
        String priceStr = editItemPrice.getText().toString().trim();
        double price = Double.parseDouble(priceStr);
        String description = editItemDescription.getText().toString().trim();
        String category = editItemCategory.getText().toString().trim();
        boolean isAvailable = editItemAvailability.isChecked();
        String allergensStr = editItemAllergens.getText().toString().trim();
        List<String> allergensList;

        if(!allergensStr.isEmpty())
        {
            allergensList = Arrays.asList(allergensStr.split(","));
            // Optionally, trim each allergen in the list
            for(int i = 0; i < allergensList.size(); i++)
            {
                allergensList.set(i, allergensList.get(i).trim());
            }
        } else
        {
            allergensList = new ArrayList<>();
        }

        // Get selected menu ID
        List<String> menuIDs = (List<String>) spinnerMenuSelection.getTag();
        String selectedMenuID = menuIDs.get(spinnerMenuSelection.getSelectedItemPosition());

        String currentMenuID = currentMenuItem.getMenuID();
        boolean isMovingMenus = !selectedMenuID.equals(currentMenuID);


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
                currentMenuItem.setCategory(category);
                currentMenuItem.setAvailability(isAvailable);
                currentMenuItem.setAllergens(allergensList);
                currentMenuItem.setOrderIndex(itemCount);
                currentMenuItem.setImageURL(imageURL);


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
                                Toast.makeText(getContext(), "Item moved successfully", Toast.LENGTH_SHORT).show();
                                loadMenuData();
                                filterResults(searchBar.getQuery().toString());
                                toggleOverlay(itemEditOverlay, false);
                            });
                        })
                        .addOnFailureListener(e ->
                        {
                            Toast.makeText(getContext(), "Failed to move item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            });
        } else
        {
            // If not moving menus, just update the existing item
            DocumentReference itemRef = db.collection("Restaurants")
                    .document(restaurantID)
                    .collection("Menus").document(currentMenuItem.getMenuID())
                    .collection("Items").document(currentMenuItem.getItemID());

            currentMenuItem.setName(name);
            currentMenuItem.setPrice(price);
            currentMenuItem.setDescription(description);
            currentMenuItem.setCategory(category);
            currentMenuItem.setAvailability(isAvailable);
            currentMenuItem.setAllergens(allergensList);
            currentMenuItem.setImageURL(imageURL);

            itemRef.set(currentMenuItem)
                    .addOnSuccessListener(aVoid ->
                    {
                        Toast.makeText(getContext(), "Item updated successfully", Toast.LENGTH_SHORT).show();
                        loadMenuData();
                        filterResults(searchBar.getQuery().toString());
                        showLoading(false);
                    })
                    .addOnFailureListener(e ->
                    {
                        showLoading(false);
                        Toast.makeText(getContext(), "Failed to update item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void proceedWithItemSave(String imageURL)
    {
        // Get input values
        String name = editItemName.getText().toString().trim();
        String priceStr = editItemPrice.getText().toString().trim();
        double price = Double.parseDouble(priceStr);
        String description = editItemDescription.getText().toString().trim();
        String category = editItemCategory.getText().toString().trim();
        boolean isAvailable = editItemAvailability.isChecked();
        String allergensStr = editItemAllergens.getText().toString().trim();
        List<String> allergensList;

        if(!allergensStr.isEmpty())
        {
            allergensList = Arrays.asList(allergensStr.split(","));
            // Optionally, trim each allergen in the list
            for(int i = 0; i < allergensList.size(); i++)
            {
                allergensList.set(i, allergensList.get(i).trim());
            }
        } else
        {
            allergensList = new ArrayList<>();
        }


        // Get selected menu ID
        List<String> menuIDs = (List<String>) spinnerMenuSelection.getTag();
        String selectedMenuID = menuIDs.get(spinnerMenuSelection.getSelectedItemPosition());

        getItemAmount(selectedMenuID, itemCount ->
        {
            // Create new item
            DocumentReference itemRef = null;
            if(imageURL == null)
            {
                itemRef = db.collection("Restaurants").document(restaurantID)
                        .collection("Menus").document(selectedMenuID)
                        .collection("Items").document();
            } else
            {
                itemRef = db.collection("Restaurants").document(restaurantID)
                        .collection("Menus").document(selectedMenuID)
                        .collection("Items").document(currentMenuItem.getItemID());
            }

            String newItemID = itemRef.getId(); // Get the generated ID first

            MenuItem newItem = new MenuItem();
            newItem.setItemID(newItemID);
            newItem.setName(name);
            newItem.setPrice(price);
            newItem.setDescription(description);
            newItem.setAvailability(isAvailable);
            newItem.setAllergens(allergensList);
            newItem.setCategory(category);
            newItem.setOrderIndex(itemCount);
            newItem.setRestaurantID(restaurantID);
            newItem.setMenuID(selectedMenuID);
            newItem.setImageURL(imageURL);


            itemRef.set(newItem)
                    .addOnSuccessListener(aVoid ->
                    {
                        Toast.makeText(getContext(), "Item added successfully", Toast.LENGTH_SHORT).show();
                        menuItemList.add(newItem);
                        loadMenuData();
                        filterResults(searchBar.getQuery().toString());
                        showLoading(false);
                    })
                    .addOnFailureListener(e ->
                    {
                        showLoading(false);
                        Toast.makeText(getContext(), "Failed to add item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
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
                .whereGreaterThan("orderIndex", removedIndex)
                .get()
                .addOnSuccessListener(snapshot ->
                {
                    Log.d("ShiftIndexesDown", "Menu ID:" + menuID);
                    Log.d("ShiftIndexesDown", "Found " + snapshot.size() + " items to shift.");
                    WriteBatch batch = db.batch();
                    for(QueryDocumentSnapshot doc : snapshot)
                    {
                        MenuItem item = doc.toObject(MenuItem.class);
                        Log.d("ShiftIndexesDown", "Shifting item: " + item.getItemID());
                        item.setOrderIndex(item.getOrderIndex() - 1);
                        batch.update(doc.getReference(), "orderIndex", item.getOrderIndex());
                    }
                    batch.commit().addOnSuccessListener(aVoid -> onComplete.run());
                })
                .addOnFailureListener(e ->
                {
                    Log.e("ShiftIndexesDown", "Failed to shift indexes: " + e.getMessage());
                    onComplete.run(); // Continue even if shifting fails
                });
    }


    private void saveMenu()
    {
        String name = editMenuName.getText().toString().trim();

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

        toggleOverlay(menuEditOverlay, false);

        if(isEditMode && currentMenu != null)
        {
            newMenu = false;
            currentMenu.setName(name);
            if(imageEdited)
            {
                uploadImageToFirebase(bitmap);
            } else
            {
                proceedWithMenuUpdate(currentMenu.getImageURL());
            }
        } else
        {
            showLoading(true);
            newMenu = true;
            if(imageEdited)
            {
                uploadImageToFirebase(bitmap);
            } else
            {
                proceedWithMenuSave(null);
            }
        }
    }

    private void proceedWithMenuUpdate(String imageURL)
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

        // Update existing menu
        currentMenu.setName(name);
        currentMenu.setDescription(description);
        currentMenu.setImageURL(imageURL);

        db.collection("Restaurants").document(restaurantID)
                .collection("Menus").document(currentMenu.getMenuID())
                .set(currentMenu)
                .addOnSuccessListener(aVoid ->
                {
                    Toast.makeText(getContext(), "Menu updated successfully", Toast.LENGTH_SHORT).show();
                    loadMenuData();
                    filterResults(searchBar.getQuery().toString());
                    loadMenusForSpinner(spinnerMenuSelection);
                    showLoading(false);
                })
                .addOnFailureListener(e ->
                {
                    showLoading(false);
                    Toast.makeText(getContext(), "Failed to update menu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });


    }

    private void proceedWithMenuSave(String imageURL)
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
        // Create new menu
        DocumentReference menuRef = null;
        if(imageURL == null)
        {
            menuRef = db.collection("Restaurants").document(restaurantID)
                    .collection("Menus").document();
        } else
        {
            menuRef = db.collection("Restaurants").document(restaurantID)
                    .collection("Menus").document(currentMenu.getMenuID());
        }

        Menu newMenu = new Menu();
        newMenu.setName(name);
        newMenu.setDescription(description);
        newMenu.setMenuID(menuRef.getId());
        newMenu.setRestaurantID(restaurantID);
        newMenu.setTimeCreated(Timestamp.now());
        newMenu.setMenuIndex(menuList.size());
        newMenu.setImageURL(imageURL);

        menuRef.set(newMenu)
                .addOnSuccessListener(aVoid ->
                {
                    Toast.makeText(getContext(), "Menu added successfully", Toast.LENGTH_SHORT).show();
                    menuList.add(newMenu);
                    filteredMenus.add(newMenu);
                    menuAdapter.notifyDataSetChanged();
                    loadMenuData();
                    filterResults(searchBar.getQuery().toString());
                    loadMenusForSpinner(spinnerMenuSelection);
                    showLoading(false);
                })
                .addOnFailureListener(e ->
                {
                    showLoading(false);
                    Toast.makeText(getContext(), "Failed to add menu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteItem(MenuItem item)
    {
        if(item == null || item.getItemID() == null || item.getMenuID() == null || restaurantID == null)
        {
            Toast.makeText(getContext(), "Invalid item or menu", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show confirmation dialog
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Item")
                .setMessage("Are you sure you want to delete this item?")
                .setPositiveButton("Delete", (dialog, which) ->
                {
                    deleteMode = true;
                    deleteOldImage(null);
                    int index = item.getOrderIndex();

                    db.collection("Restaurants")
                            .document(restaurantID)
                            .collection("Menus")
                            .document(item.getMenuID())
                            .collection("Items")
                            .document(item.getItemID())
                            .delete()
                            .addOnSuccessListener(aVoid ->
                            {
                                shiftIndexesDown(item.getMenuID(), index, () ->
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
                                    deleteMode = true;
                                    deleteOldImage(null);
                                    // Now delete the menu itself
                                    db.collection("Restaurants")
                                            .document(restaurantID)
                                            .collection("Menus")
                                            .document(menu.getMenuID())
                                            .delete()
                                            .addOnSuccessListener(aVoid2 ->
                                            {
                                                Toast.makeText(getContext(), "Menu and Items deleted successfully", Toast.LENGTH_SHORT).show();
                                                menuList.remove(menu);
                                                filteredMenus.remove(menu);
                                                menuAdapter.notifyDataSetChanged();
                                                loadMenuData();
                                                loadMenusForSpinner(spinnerMenuSelection);
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

    private void editImage()
    {
        // Check permissions
        boolean cameraPermissionGranted = ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        boolean storagePermissionGranted = checkStoragePermission();

        Log.d(TAG, "Camera Permission: " + cameraPermissionGranted + ", Storage Permission: " + storagePermissionGranted);

        if(!cameraPermissionGranted || !storagePermissionGranted)
        {
            requestPermissions();
        } else
        {
            showBottomSheetDialog();
        }
    }

    private boolean checkStoragePermission()
    {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        {
            return ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        } else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        {
            // Scoped storage doesn't require explicit permission
            return true;
        } else
        {
            // For Android 9 and below
            return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestPermissions()
    {
        List<String> permissionsToRequest = new ArrayList<>();

        // Always request camera permission
        if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED)
        {
            permissionsToRequest.add(Manifest.permission.CAMERA);
        }

        // Add storage permissions based on Android version
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        {
            if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED)
            {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
        {
            if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED)
            {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED)
            {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }

        // Launch permission request if needed
        if(!permissionsToRequest.isEmpty())
        {
            permissionLauncher.launch(permissionsToRequest.toArray(new String[0]));
        } else
        {
            // If no permissions need to be requested, show bottom sheet
            showBottomSheetDialog();
        }
    }

    // Permission request launcher
    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result ->
            {
                // Update permission checks based on Android version
                boolean cameraGranted = Boolean.TRUE.equals(result.get(Manifest.permission.CAMERA));
                boolean storageGranted = false;

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                {
                    storageGranted = Boolean.TRUE.equals(result.get(Manifest.permission.READ_MEDIA_IMAGES));
                } else if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
                {
                    storageGranted = Boolean.TRUE.equals(result.get(Manifest.permission.WRITE_EXTERNAL_STORAGE))
                            && Boolean.TRUE.equals(result.get(Manifest.permission.READ_EXTERNAL_STORAGE));
                } else
                {
                    // For Android 10-12, storage permissions are not strictly required
                    storageGranted = true;
                }

                Log.d(TAG, "Camera Permission Granted: " + cameraGranted);
                Log.d(TAG, "Storage Permission Granted: " + storageGranted);

                if(cameraGranted && storageGranted)
                {
                    showBottomSheetDialog();
                } else
                {
                    if(isAdded())
                    {
                        Toast.makeText(requireContext(), "Permissions are required", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    private void showBottomSheetDialog()
    {
        // Dismiss any existing dialog first
        if(imageBottomSheetDialog != null && imageBottomSheetDialog.isShowing())
        {
            imageBottomSheetDialog.dismiss();
        }

        // Create and configure new dialog
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_image, null);

        TextView selectImageTextview = bottomSheetView.findViewById(R.id.SelectImageTextView);
        if("Menu".equals(currentType))
        {
            selectImageTextview.setText("Select Menu Image");
        } else if("MenuItem".equals(currentType))
        {
            selectImageTextview.setText("Select Item Image");
        }

        imageBottomSheetDialog = new BottomSheetDialog(requireContext());
        imageBottomSheetDialog.setContentView(bottomSheetView);

        Button btnTakePhoto = bottomSheetView.findViewById(R.id.btn_take_photo);
        Button btnChooseGallery = bottomSheetView.findViewById(R.id.btn_choose_gallery);
        Button btnCancel = bottomSheetView.findViewById(R.id.btn_cancel);

        btnTakePhoto.setOnClickListener(v ->
        {
            Log.d(TAG, "Taking photo...");
            takePhoto();
            imageBottomSheetDialog.dismiss();
        });

        btnChooseGallery.setOnClickListener(v ->
        {
            Log.d(TAG, "Choosing from gallery...");
            pickFromGallery();
            imageBottomSheetDialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> imageBottomSheetDialog.dismiss());

        imageBottomSheetDialog.setOnDismissListener(dialog -> imageBottomSheetDialog = null);
        imageBottomSheetDialog.show();
    }

    private void takePhoto()
    {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(intent.resolveActivity(requireActivity().getPackageManager()) != null)
        {
            File photoFile = createImageFile();
            if(photoFile != null)
            {
                try
                {
                    photoUri = FileProvider.getUriForFile(
                            requireContext(),
                            "com.example.restaurantapp.fileprovider",
                            photoFile
                    );
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                    Log.d(TAG, "Launching camera with URI: " + photoUri);
                    cameraLauncher.launch(intent);
                } catch(IllegalArgumentException e)
                {
                    Log.e(TAG, "Error creating file URI", e);
                    if(isAdded())
                    {
                        Toast.makeText(requireContext(), "Unable to create file for photo", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    private void pickFromGallery()
    {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        Log.d(TAG, "Launching gallery picker...");
        imagePickerLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result ->
            {
                if(result.getResultCode() == Activity.RESULT_OK)
                {
                    Log.d(TAG, "Camera result received, URI: " + photoUri);

                    if(!isAdded() || getActivity() == null || getActivity().isFinishing())
                    {
                        return;  // Fragment is no longer attached
                    }

                    // Check permissions again
                    boolean hasCameraPermission = ContextCompat.checkSelfPermission(
                            requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
                    boolean hasStoragePermission = checkStoragePermission();

                    if(hasCameraPermission && hasStoragePermission)
                    {
                        if(photoUri != null)
                        {
                            try
                            {
                                // Load and display the image
                                bitmap = MediaStore.Images.Media.getBitmap(
                                        requireActivity().getContentResolver(), photoUri);
                                bitmap = fixImageOrientation(photoUri, bitmap);

                                // Update edit image
                                updateEditImageWithCurrentImage(bitmap);

                            } catch(IOException e)
                            {
                                Log.e(TAG, "Error processing camera image", e);
                                Toast.makeText(requireContext(),
                                        "Error processing image", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } else
                    {
                        Log.d(TAG, "Camera or storage permissions denied.");
                        Toast.makeText(requireContext(),
                                "Camera and storage permissions are required to take a photo",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    // Handle gallery result
    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result ->
            {
                if(result.getResultCode() == Activity.RESULT_OK && result.getData() != null)
                {
                    Log.d(TAG, "Gallery result received.");

                    if(!isAdded() || getActivity() == null || getActivity().isFinishing())
                    {
                        return;  // Fragment is no longer attached
                    }

                    boolean hasStoragePermission = checkStoragePermission();

                    if(hasStoragePermission)
                    {
                        Uri imageUri = result.getData().getData();
                        if(imageUri != null)
                        {
                            try
                            {
                                // Display the image
                                bitmap = MediaStore.Images.Media.getBitmap(
                                        requireActivity().getContentResolver(), imageUri);
                                bitmap = fixImageOrientation(imageUri, bitmap);

                                // Update edit image
                                updateEditImageWithCurrentImage(bitmap);
                            } catch(IOException e)
                            {
                                Log.e(TAG, "Error loading image from gallery", e);
                                Toast.makeText(requireContext(),
                                        "Failed to load image", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } else
                    {
                        Log.d(TAG, "Storage permissions denied.");
                        Toast.makeText(requireContext(),
                                "Storage permissions are required", Toast.LENGTH_SHORT).show();
                        requestPermissions();
                    }
                }
            }
    );

    private File createImageFile()
    {
        File storageDir = requireContext().getExternalFilesDir(null);
        try
        {
            File imageFile = File.createTempFile(
                    "profile_pic_", /* Prefix */
                    ".jpg",         /* Suffix */
                    storageDir      /* Directory */
            );
            Log.d(TAG, "Image file created at: " + imageFile.getAbsolutePath());
            return imageFile;
        } catch(IOException e)
        {
            Log.e(TAG, "Error creating image file", e);
            return null;
        }
    }

    private void updateEditImageWithCurrentImage(Bitmap bitmap)
    {
        if("Menu".equals(currentType))
        {
            menuEditImage.setImageBitmap(bitmap);
            imageEdited = true;
        } else if("MenuItem".equals(currentType))
        {
            itemEditImage.setImageBitmap(bitmap);
            imageEdited = true;
        }
    }

    private void uploadImageToFirebase(Bitmap bitmap)
    {
        if(bitmap == null)
        {
            Log.e(TAG, "Cannot upload null bit map");
            return;
        }

        if(!isAdded() || getActivity() == null || getActivity().isFinishing())
        {
            return;  // Fragment is no longer attached
        }

        Toast.makeText(requireContext(), "Uploading image...", Toast.LENGTH_SHORT).show();

        // Get the current user ID
        FirebaseUser currentUser = auth.getCurrentUser();
        if(currentUser == null)
        {
            Log.e(TAG, "No user is currently logged in");
            Toast.makeText(requireContext(),
                    "You must be logged in to upload an image", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a unique filename for the image
        String filename = generateFileName();

        StorageReference imageRef = storageRef.child(filename);

        // Compress the image before uploading
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        byte[] imageData = baos.toByteArray();

        // Start the upload
        isUploading = true;
        currentUploadTask = imageRef.putBytes(imageData);
        currentUploadTask.addOnSuccessListener(taskSnapshot ->
        {
            isUploading = false;
            currentUploadTask = null;

            // Only proceed if fragment is still attached
            if(isAdded() && getActivity() != null && !getActivity().isFinishing())
            {
                // Image uploaded successfully, now get the download URL
                imageRef.getDownloadUrl().addOnSuccessListener(downloadUri ->
                {
                    if(isAdded() && getActivity() != null && !getActivity().isFinishing())
                    {
                        // Got the download URL, now update the user's Firestore document
                        if(newItem || newMenu)
                        {
                            if("Menu".equals(currentType))
                            {
                                if(newMenu)
                                {
                                    proceedWithMenuSave(downloadUri.toString());
                                } else
                                {
                                    proceedWithMenuUpdate(downloadUri.toString());
                                }

                            } else if("MenuItem".equals(currentType))
                            {
                                if(newItem)
                                {
                                    proceedWithItemSave(downloadUri.toString());
                                } else
                                {
                                    proceedWithItemUpdate(downloadUri.toString());
                                }
                            }
                        } else
                        {
                            deleteOldImage(downloadUri.toString());
                            Log.d(TAG, "Called deleteOldImage with URL: " + downloadUri);
                        }
                        Log.d(TAG, "Upload successful, URL: " + downloadUri);
                    }
                }).addOnFailureListener(e ->
                {
                    if(isAdded() && getActivity() != null && !getActivity().isFinishing())
                    {
                        Log.e(TAG, "Failed to get download URL", e);
                        Toast.makeText(requireContext(),
                                "Failed to get image URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).addOnFailureListener(e ->
        {
            isUploading = false;
            currentUploadTask = null;

            if(isAdded() && getActivity() != null && !getActivity().isFinishing())
            {
                Log.e(TAG, "Image upload failed", e);
                Toast.makeText(requireContext(),
                        "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }).addOnProgressListener(taskSnapshot ->
        {
            // Calculate and show upload progress if needed
            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
            Log.d(TAG, "Upload progress: " + progress + "%");
        });
    }


    private String generateFileName()
    {
        String filename = "";

        if("Menu".equals(currentType))
        {
            if(newMenu)
            {
                // Create new menu
                DocumentReference newMenuRef = db.collection("Restaurants").document(restaurantID)
                        .collection("Menus").document();

                currentMenu = new Menu();
                currentMenu.setMenuID(newMenuRef.getId());

                // Generate a unique filename for the menu image
                filename = "menu_images/" + currentMenu.getMenuID() + "/" + UUID.randomUUID().toString() + ".jpg";
            } else
            {
                String menuID = currentMenu.getMenuID();
                filename = "menu_images/" + menuID + "/" + UUID.randomUUID().toString() + ".jpg";
            }
        } else if("MenuItem".equals(currentType))
        {
            if(newItem)
            {
                List<String> menuIDs = (List<String>) spinnerMenuSelection.getTag();
                String selectedMenuID = menuIDs.get(spinnerMenuSelection.getSelectedItemPosition());
                // Create new item
                DocumentReference newItemRef = db.collection("Restaurants").document(restaurantID)
                        .collection("Menus").document(selectedMenuID)
                        .collection("Items").document();

                currentMenuItem = new MenuItem();
                currentMenuItem.setItemID(newItemRef.getId());
                currentMenuItem.setMenuID(selectedMenuID);

                filename = "menuItem_images/" + currentMenuItem.getItemID() + "/" + UUID.randomUUID().toString() + ".jpg";
            } else
            {
                String itemID = currentMenuItem.getItemID();
                filename = "menuItem_images/" + itemID + "/" + UUID.randomUUID().toString() + ".jpg";
            }
        }

        return filename;
    }


    private Bitmap fixImageOrientation(Uri imageUri, Bitmap bitmap)
    {
        try
        {
            // Read EXIF data to determine orientation
            android.media.ExifInterface exif = null;
            try(InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri))
            {
                if(inputStream != null)
                {
                    exif = new ExifInterface(inputStream);
                }
            }

            int orientation = android.media.ExifInterface.ORIENTATION_NORMAL;
            if(exif != null)
            {
                orientation = exif.getAttributeInt(
                        android.media.ExifInterface.TAG_ORIENTATION, android.media.ExifInterface.ORIENTATION_NORMAL);
            }

            // Rotate bitmap based on orientation
            Matrix matrix = new Matrix();
            switch(orientation)
            {
                case android.media.ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.postRotate(90);
                    break;
                case android.media.ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.postRotate(180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.postRotate(270);
                    break;
            }

            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        } catch(Exception e)
        {
            Log.e(TAG, "Error fixing image orientation", e);
            return bitmap; // Return original if rotation fails
        }
    }

    private void deleteOldImage(String newImageURL)
    {
        FirebaseUser currentUser = auth.getCurrentUser();
        if(currentUser != null)
        {
            DocumentReference typeRef = null;
            if("Menu".equals(currentType))
            {
                typeRef = db.collection("Restaurants").document(restaurantID)
                        .collection("Menus").document(currentMenu.getMenuID());
            } else if("MenuItem".equals(currentType))
            {
                typeRef = db.collection("Restaurants").document(restaurantID)
                        .collection("Menus").document(currentMenuItem.getMenuID())
                        .collection("Items").document(currentMenuItem.getItemID());
            }

            Log.d(TAG, "current Type: " + currentType);

            // Get the current profile image URL
            typeRef.get().addOnSuccessListener(documentSnapshot ->
            {
                if(documentSnapshot.exists())
                {
                    String oldImageURL = documentSnapshot.getString("imageURL");

                    // Only proceed if there's an old image URL and it's different from the new one
                    if(oldImageURL != null && !oldImageURL.isEmpty())
                    {
                        if(!newImageURL.equals(oldImageURL))
                        {

                            try
                            {
                                // Get the path after "/o/" and before "?"
                                String urlPath = oldImageURL.split("/o/")[1];
                                if(urlPath.contains("?"))
                                {
                                    urlPath = urlPath.split("\\?")[0];
                                }

                                // Decode the URL-encoded path
                                String decodedPath = java.net.URLDecoder.decode(urlPath, "UTF-8");

                                // Create a reference to the old file and delete it
                                StorageReference oldImageRef = storage.getReference().child(decodedPath);
                                oldImageRef.delete().addOnSuccessListener(aVoid ->
                                {
                                    Log.d(TAG, "Old image deleted successfully");
                                    if(deleteMode)
                                    {
                                        deleteMode = false;
                                    } else
                                    {
                                        if("Menu".equals(currentType))
                                        {
                                            if(newMenu)
                                            {
                                                proceedWithMenuSave(newImageURL);
                                            } else
                                            {
                                                proceedWithMenuUpdate(newImageURL);
                                            }

                                        } else if("MenuItem".equals(currentType))
                                        {
                                            if(newItem)
                                            {
                                                proceedWithItemSave(newImageURL);
                                            } else
                                            {
                                                proceedWithItemUpdate(newImageURL);
                                            }
                                        }
                                    }
                                }).addOnFailureListener(e ->
                                {
                                    Log.e(TAG, "Error deleting old image", e);
                                    if(deleteMode)
                                    {
                                        deleteMode = false;
                                    } else
                                    {
                                        if("Menu".equals(currentType))
                                        {
                                            if(newMenu)
                                            {
                                                proceedWithMenuSave(newImageURL);
                                            } else
                                            {
                                                proceedWithMenuUpdate(newImageURL);
                                            }

                                        } else if("MenuItem".equals(currentType))
                                        {
                                            if(newItem)
                                            {
                                                proceedWithItemSave(newImageURL);
                                            } else
                                            {
                                                proceedWithItemUpdate(newImageURL);
                                            }
                                        }
                                    }
                                });
                            } catch(Exception e)
                            {
                                Log.e(TAG, "Error parsing old image URL: " + oldImageURL, e);
                            }
                        }
                    } else
                    {
                        if(deleteMode)
                        {
                            deleteMode = false;
                        } else
                        {
                            if("Menu".equals(currentType))
                            {
                                if(newMenu)
                                {
                                    proceedWithMenuSave(newImageURL);
                                } else
                                {
                                    proceedWithMenuUpdate(newImageURL);
                                }

                            } else if("MenuItem".equals(currentType))
                            {
                                if(newItem)
                                {
                                    proceedWithItemSave(newImageURL);
                                } else
                                {
                                    proceedWithItemUpdate(newImageURL);
                                }
                            }
                        }
                    }
                }
            }).addOnFailureListener(e ->
            {
                Log.e(TAG, "Error fetching user document to delete old image", e);
            });
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

            // Ensure discount is a valid amount
            if(discountAmount <= 0)
            {
                Toast.makeText(getContext(), "Discount must be a positive number", Toast.LENGTH_SHORT).show();
                return;
            }

            showLoading(true);

            // Get the tags containing the IDs
            List<String> menuIDs = (List<String>) spinnerDiscountMenu.getTag();
            List<String> itemIDs = (List<String>) spinnerDiscountItem.getTag();

            // Get the selected item positions
            int selectedMenuPosition = spinnerDiscountMenu.getSelectedItemPosition();
            int selectedItemPosition = spinnerDiscountItem.getSelectedItemPosition();

            // Ensure valid selections
            if(menuIDs == null || selectedMenuPosition >= menuIDs.size())
            {
                Toast.makeText(getContext(), "Invalid menu selection", Toast.LENGTH_SHORT).show();
                showLoading(false);
                return;
            }

            // Get the selected menu ID
            String selectedMenuID = menuIDs.get(selectedMenuPosition);

            // Get the selected item ID
            String selectedItemID = itemIDs.get(selectedItemPosition);


            //create schedule
            Timestamp startTime;
            Timestamp endTime;
            if(scheduleOn)
            {
                // Get the selected start and end times from the UI
                int startYear = startDatePicker.getYear();
                int startMonth = startDatePicker.getMonth();
                int startDay = startDatePicker.getDayOfMonth();
                int startHour = startTimePicker.getHour();
                int startMinute = startTimePicker.getMinute();

                int endYear = endDatePicker.getYear();
                int endMonth = endDatePicker.getMonth();
                int endDay = endDatePicker.getDayOfMonth();
                int endHour = endTimePicker.getHour();
                int endMinute = endTimePicker.getMinute();

                // Create Gregorian calendar instances for start and end
                GregorianCalendar startCalendar = new GregorianCalendar(startYear, startMonth, startDay, startHour, startMinute);
                GregorianCalendar endCalendar = new GregorianCalendar(endYear, endMonth, endDay, endHour, endMinute);

                // Get Seconds from the calendar instances
                long startSeconds = startCalendar.getTimeInMillis() / 1000;
                long endSeconds = endCalendar.getTimeInMillis() / 1000;


                // Create Timestamp objects with seconds only, nanoseconds set to 0
                startTime = new Timestamp(startSeconds, 0);
                endTime = new Timestamp(endSeconds, 0);
            } else
            {
                // If schedule is off, use the current time
                long currentSeconds = System.currentTimeMillis() / 1000;

                // Set current time as start time
                startTime = new Timestamp(currentSeconds, 0);
                endTime = null; // No end time if not scheduling
            }

            // If the selected item is "ALL_ITEMS", apply the discount to all items
            if("ALL_ITEMS".equals(selectedItemID))
            {
                db.collection("Restaurants").document(restaurantID)
                        .collection("Menus").document(selectedMenuID)
                        .collection("Items")
                        .get()
                        .addOnSuccessListener(querySnapshot ->
                        {
                            List<Task<Void>> discountTasks = new ArrayList<>();

                            for(DocumentSnapshot doc : querySnapshot)
                            {
                                String itemID = doc.getId();

                                // Generate a new discount document reference with ID
                                DocumentReference discountRef = db.collection("Restaurants").document(restaurantID)
                                        .collection("Menus").document(selectedMenuID)
                                        .collection("Items").document(itemID)
                                        .collection("Discounts").document();

                                String discountID = discountRef.getId();

                                Map<String, Object> discountData = new HashMap<>();
                                discountData.put("discountID", discountID);
                                discountData.put("discountType", discountType);      // "Percentage" or "Flat"
                                discountData.put("amount", discountAmount);
                                discountData.put("startTime", startTime);    // Timestamp
                                discountData.put("endTime", endTime);        // Timestamp or null

                                Task<Void> addDiscountTask = discountRef.set(discountData);
                                discountTasks.add(addDiscountTask);
                            }

                            Tasks.whenAllComplete(discountTasks).addOnCompleteListener(t ->
                            {
                                showLoading(false);
                                Toast.makeText(getContext(), "Discount scheduled for all items", Toast.LENGTH_SHORT).show();
                                loadMenuData();
                                toggleOverlay(discountOverlay, false);
                            });
                        })
                        .addOnFailureListener(e ->
                        {
                            showLoading(false);
                            Toast.makeText(getContext(), "Failed to schedule discount: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            } else
            {
                // Apply discount to a single item
                if(itemIDs == null || selectedItemPosition - 1 >= itemIDs.size())
                {
                    Toast.makeText(getContext(), "Invalid item selection", Toast.LENGTH_SHORT).show();
                    showLoading(false);
                    return;
                }
                db.collection("Restaurants").document(restaurantID)
                        .collection("Menus").document(selectedMenuID)
                        .collection("Items").document(selectedItemID)
                        .get()
                        .addOnSuccessListener(doc ->
                        {
                            if(doc.exists())
                            {
                                String itemID = doc.getId();

                                // Generate a new discount document reference with ID
                                DocumentReference discountRef = db.collection("Restaurants").document(restaurantID)
                                        .collection("Menus").document(selectedMenuID)
                                        .collection("Items").document(itemID)
                                        .collection("Discounts").document();
                                // Create discount data
                                Map<String, Object> discountData = new HashMap<>();
                                discountData.put("discountID", discountRef.getId());
                                discountData.put("discountType", discountType); // "Percentage" or "Flat"
                                discountData.put("amount", discountAmount);
                                discountData.put("startTime", startTime); // Timestamp
                                discountData.put("endTime", endTime);     // Timestamp or null

                                // Add discount to the Discounts subcollection
                                discountRef.set(discountData)
                                        .addOnSuccessListener(ref ->
                                        {
                                            showLoading(false);
                                            Toast.makeText(getContext(), "Discount scheduled for item", Toast.LENGTH_SHORT).show();
                                            loadMenuData();
                                            toggleOverlay(discountOverlay, false);
                                        })
                                        .addOnFailureListener(e ->
                                        {
                                            showLoading(false);
                                            Toast.makeText(getContext(), "Failed to schedule discount: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                            } else
                            {
                                showLoading(false);
                                Toast.makeText(getContext(), "Item not found in the database", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(e ->
                        {
                            showLoading(false);
                            Toast.makeText(getContext(), "Failed to fetch item data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });

            }

        } catch(NumberFormatException e)
        {
            Toast.makeText(getContext(), "Please enter a valid discount value", Toast.LENGTH_SHORT).show();
        }
    }
}