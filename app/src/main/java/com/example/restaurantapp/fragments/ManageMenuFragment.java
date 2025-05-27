package com.example.restaurantapp.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
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

/**
 * Fragment for managing restaurant menus and menu items.
 * Allows restaurant owners to create, view, edit, and delete menus and items,
 * as well as create and manage discounts for items or entire menus.
 */
public class ManageMenuFragment extends Fragment
{

    /**
     * SearchView for filtering menus and items.
     */
    private SearchView searchBar;
    /**
     * RecyclerView to display the list of menus.
     */
    private RecyclerView recyclerViewMenus;
    /**
     * Adapter for the {@link #recyclerViewMenus}.
     */
    private MenuAdapter menuAdapter;
    /**
     * List of all {@link Menu} objects for the current restaurant.
     */
    private List<Menu> menuList = new ArrayList<>();
    /**
     * List of all {@link MenuItem} objects across all menus for the current restaurant.
     */
    private List<MenuItem> menuItemList = new ArrayList<>();
    /**
     * List of {@link Menu} objects after filtering by search query. (Note: Not directly used for display, adapter handles filtering)
     */
    private List<Menu> filteredMenus = new ArrayList<>();
    /**
     * List of {@link MenuItem} objects after filtering. (Note: Not directly used for display, adapter handles filtering)
     */
    private List<MenuItem> filteredItems = new ArrayList<>();

    /**
     * ImageButton to initiate discount creation.
     */
    private ImageButton btnCreateDiscount;
    /**
     * ImageButton to initiate adding a new menu or item.
     */
    private ImageButton btnAdd;
    /**
     * ImageButton within the item edit overlay to change the item's image.
     */
    private ImageButton itemEditImage;
    /**
     * ImageButton within the menu edit overlay to change the menu's image.
     */
    private ImageButton menuEditImage;

    /**
     * ImageView within the menu view overlay to display the menu's image.
     */
    private ImageView menuViewImage;
    /**
     * ImageView within the item view overlay to display the item's image.
     */
    private ImageView itemViewImage;

    /**
     * Overlay for viewing details of a single menu item.
     */
    private RelativeLayout itemViewOverlay;
    /**
     * Overlay for viewing details of a single menu.
     */
    private RelativeLayout menuViewOverlay;
    /**
     * Overlay for choosing whether to add a new menu or a new item.
     */
    private RelativeLayout addChoiceOverlay;
    /**
     * Overlay for editing or creating a menu item.
     */
    private RelativeLayout itemEditOverlay;
    /**
     * Overlay for editing or creating a menu.
     */
    private RelativeLayout menuEditOverlay;
    /**
     * Overlay for creating and applying discounts.
     */
    private RelativeLayout discountOverlay;
    /**
     * Overlay displayed during loading operations.
     */
    private RelativeLayout loadingOverlay;

    /**
     * EditText for the name of the item being edited/created.
     */
    private EditText editItemName;
    /**
     * EditText for the price of the item being edited/created.
     */
    private EditText editItemPrice;
    /**
     * EditText for the description of the item being edited/created.
     */
    private EditText editItemDescription;
    /**
     * EditText for the allergens of the item being edited/created (comma-separated).
     */
    private EditText editItemAllergens;
    /**
     * EditText for the category of the item being edited/created.
     */
    private EditText editItemCategory;
    /**
     * EditText for the name of the menu being edited/created.
     */
    private EditText editMenuName;
    /**
     * EditText for the discount amount (percentage or flat).
     */
    private EditText editDiscountAmount;

    /**
     * TextInputLayout for {@link #editItemName} to display errors.
     */
    private TextInputLayout editItemNameLayout;
    /**
     * TextInputLayout for {@link #editItemPrice} to display errors.
     */
    private TextInputLayout editItemPriceLayout;
    /**
     * TextInputLayout for {@link #editItemDescription} to display errors.
     */
    private TextInputLayout editItemDescriptionLayout;
    /**
     * TextInputLayout for {@link #editItemAllergens} to display errors.
     */
    private TextInputLayout editItemAllergensLayout;
    /**
     * TextInputLayout for {@link #editItemCategory} to display errors.
     */
    private TextInputLayout editItemCategoryLayout;

    /**
     * Button within {@link #addChoiceOverlay} to proceed with adding a new item.
     */
    private Button btnChooseAddItem;
    /**
     * Button within {@link #addChoiceOverlay} to proceed with adding a new menu.
     */
    private Button btnChooseAddMenu;
    /**
     * Button within {@link #addChoiceOverlay} to cancel the add choice.
     */
    private Button btnCancelAddChoice;
    /**
     * Button within {@link #itemEditOverlay} to save the item being edited/created.
     */
    private Button btnSaveItem;
    /**
     * Button within {@link #itemEditOverlay} to cancel editing/creating an item.
     */
    private Button btnCancelEdit;
    /**
     * Button within {@link #menuEditOverlay} to save the menu being edited/created.
     */
    private Button btnSaveMenu;
    /**
     * Button within {@link #menuEditOverlay} to cancel editing/creating a menu.
     */
    private Button btnCancelMenuEdit;
    /**
     * Button within {@link #discountOverlay} (when manual select is chosen) to confirm selection and close overlay.
     */
    private Button btnChooseForDiscount;
    /**
     * Button within {@link #discountOverlay} to apply the configured discount.
     */
    private Button btnApplyDiscount;
    /**
     * Button within {@link #discountOverlay} to cancel discount creation.
     */
    private Button btnCancelDiscount;

    /**
     * ProgressBar for indicating loading states, e.g., during save operations.
     */
    private ProgressBar progressBar;
    /**
     * SwitchMaterial to enable or disable discount scheduling.
     */
    private Switch switchEnableSchedule;
    /**
     * RadioGroup for selecting the discount type (percentage or flat).
     */
    private RadioGroup radioDiscountType;
    /**
     * RadioGroup for selecting the scope of discount application (specific menu/item or manual select).
     */
    private RadioGroup radioApplyScope;
    /**
     * Spinner for selecting a menu when adding/editing an item.
     */
    private Spinner spinnerMenuSelection;
    /**
     * Spinner for selecting a menu when applying a discount.
     */
    private Spinner spinnerDiscountMenu;
    /**
     * Spinner for selecting an item (or "all items") within a menu when applying a discount.
     */
    private Spinner spinnerDiscountItem;
    /**
     * DatePicker for selecting the start date of a scheduled discount.
     */
    private DatePicker startDatePicker;
    /**
     * DatePicker for selecting the end date of a scheduled discount.
     */
    private DatePicker endDatePicker;
    /**
     * TimePicker for selecting the start time of a scheduled discount.
     */
    private TimePicker startTimePicker;
    /**
     * TimePicker for selecting the end time of a scheduled discount.
     */
    private TimePicker endTimePicker;

    /**
     * TextView to display "No results found" message for search.
     */
    private TextView noResults;
    /**
     * TextView acting as a click target to edit an item's image.
     */
    private TextView itemEditImageTextView;
    /**
     * TextView acting as a click target to edit a menu's image.
     */
    private TextView menuEditImageTextView;
    /**
     * TextView in item view overlay to display the menu name.
     */
    private TextView menuViewName;
    /**
     * TextView in item view overlay to display the item name.
     */
    private TextView itemViewName;
    /**
     * TextView in item view overlay to display the item price.
     */
    private TextView itemViewPrice;
    /**
     * TextView in item view overlay to display the item description.
     */
    private TextView itemViewDescription;
    /**
     * TextView in item view overlay to display the item availability.
     */
    private TextView itemViewAvailability;
    /**
     * TextView in item view overlay to display the item category.
     */
    private TextView itemViewCategory;
    /**
     * TextView in item view overlay to display the item allergens.
     */
    private TextView itemViewAllergens;
    /**
     * TextView in item view overlay to display a discount badge.
     */
    private TextView discountBadge;
    /**
     * TextView in item view overlay to display the original price if discounted.
     */
    private TextView oldPrice;
    /**
     * TextView in item edit overlay to display the overlay title (e.g., "Create Menu Item").
     */
    private TextView itemEditOverlayName;
    /**
     * TextView in menu edit overlay to display the overlay title (e.g., "Create Menu").
     */
    private TextView menuEditOverlayName;

    /**
     * CheckBox in item edit overlay to set item availability.
     */
    private CheckBox editItemAvailability;
    /**
     * BottomSheetDialog for choosing image source (camera/gallery).
     */
    private BottomSheetDialog imageBottomSheetDialog;

    /**
     * Instance of FirebaseFirestore for database operations.
     */
    private FirebaseFirestore db;
    /**
     * Instance of FirebaseAuth for user authentication.
     */
    private FirebaseAuth auth;
    /**
     * The currently authenticated FirebaseUser.
     */
    private FirebaseUser currentUser;
    /**
     * Reference to the root of Firebase Storage.
     */
    private StorageReference storageRef;
    /**
     * Instance of FirebaseStorage.
     */
    private FirebaseStorage storage;

    /**
     * The ID of the current restaurant whose menu is being managed.
     */
    private String restaurantID;
    /**
     * The {@link MenuItem} currently being viewed or edited.
     */
    private MenuItem currentMenuItem;
    /**
     * The {@link Menu} currently being viewed or edited.
     */
    private Menu currentMenu;
    /**
     * String indicating the type of entity being edited/created ("Menu" or "MenuItem").
     */
    private String currentType;
    /**
     * String indicating the type of discount being applied ("Percentage" or "Flat").
     */
    private String discountType;
    /**
     * Flag indicating if an image upload is in progress.
     */
    private boolean isUploading = false;
    /**
     * Flag indicating if an existing entity (menu/item) is being edited (true) or a new one is being created (false).
     */
    private boolean isEditMode = false;
    /**
     * Flag indicating if menu items have been reordered. (Note: Declared but not used in provided code)
     */
    private boolean itemsReordered = false;
    /**
     * Flag indicating if discount scheduling is enabled.
     */
    private boolean scheduleOn = false;
    /**
     * Flag indicating if an image (menu or item) has been edited by the user.
     */
    private boolean imageEdited = false;
    /**
     * Flag indicating if a new item is being created.
     */
    private boolean newItem = false;
    /**
     * Flag indicating if a new menu is being created.
     */
    private boolean newMenu = false;
    /**
     * Flag indicating if an image deletion operation is in progress.
     */
    boolean deleteMode = false;
    /**
     * Uri of the photo taken by camera or selected from gallery.
     */
    private Uri photoUri;
    /**
     * Bitmap representation of the image selected/taken.
     */
    private Bitmap bitmap;
    /**
     * Reference to the current Firebase Storage upload task.
     */
    private UploadTask currentUploadTask;
    /**
     * Tag for logging purposes.
     */
    private static final String TAG = "ManageMenuFragment";

    /**
     * Default constructor for ManageMenuFragment.
     * Required empty public constructor.
     */
    public ManageMenuFragment()
    {
        // Required empty public constructor
    }

    /**
     * Called to have the fragment instantiate its user interface view.
     * This method inflates the layout for the fragment. It initializes Firebase services (Firestore, Auth, Storage),
     * retrieves the current user's restaurant ID, and if valid, sets up the {@link MenuAdapter}
     * and loads initial menu data via {@link #loadMenuData()}.
     * It then initializes all other UI components (Buttons, EditTexts, Spinners, Overlays, etc.)
     * and calls {@link #setupListeners()} to configure their behavior and interactions.
     *
     * <p>It is recommended to <strong>only</strong> inflate the layout in this method and move
     * logic that operates on the returned View to {@link #onViewCreated(View, Bundle)}.
     *
     * <p>If you return a View from here, you will later be called in
     * {@link #onDestroyView} when the view is being released.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate
     *                           any views in the fragment.
     * @param container          If non-null, this is the parent view that the fragment's
     *                           UI should be attached to. The fragment should not add the view itself,
     *                           but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     *                           from a previous saved state as given here.
     * @return Return the View for the fragment's UI.
     */
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
        btnAdd = view.findViewById(R.id.btnAdd);

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
        menuViewName = menuViewOverlay.findViewById(R.id.menuViewName);
        discountBadge = itemViewOverlay.findViewById(R.id.discountBadge);
        oldPrice = itemViewOverlay.findViewById(R.id.oldPrice);
        itemEditOverlayName = itemEditOverlay.findViewById(R.id.itemEditOverlayName);
        menuEditOverlayName = menuEditOverlay.findViewById(R.id.menuEditOverlayName);

        noResults = view.findViewById(R.id.noResults);

        setupListeners();
        return view;
    }

    /**
     * Sets up listeners for various UI components in the fragment.
     * This includes search bar, add buttons, save/cancel buttons for edit overlays,
     * discount creation components, and overlay close/edit/delete buttons.
     */
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

    /**
     * Loads the list of menus for the current restaurant from Firestore and populates the given spinner.
     * Stores menu IDs in the spinner's tag for later retrieval.
     *
     * @param spinner The Spinner to populate with menu names.
     */
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
                    if(!menuIDs.isEmpty())
                    { // Check if menuIDs is not empty before accessing
                        setSpinnerSelection(menuIDs.get(0));
                    }


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

    /**
     * Loads the list of items for a specific menu from Firestore and populates the given spinner.
     * Adds an "All items in the menu" option at the beginning.
     * Stores item IDs in the spinner's tag.
     *
     * @param spinner The Spinner to populate with item names.
     * @param menuID  The ID of the menu whose items are to be loaded.
     */
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


    /**
     * Sets the selection of the {@code spinnerMenuSelection} to the menu with the given ID.
     *
     * @param selectedMenuID The ID of the menu to select.
     */
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

    /**
     * Loads all menus for the current restaurant from Firestore, ordered by their index.
     * Updates the local {@code menuList} and notifies the {@code menuAdapter}.
     * After loading menus, it calls {@link #loadAllMenuItems()} to fetch items for these menus.
     * If a search query was active, it reapplies the filter.
     */
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

    /**
     * Loads all menu items for all menus in the {@code menuList} from Firestore.
     * Stores the fetched items in the {@code menuAdapter} and the local {@code menuItemList}.
     * Notifies the adapter after all items are loaded or if a search query was active, reapplies the filter.
     */
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

    /**
     * Filters the displayed menus and items based on the provided query string.
     * If the query is empty, clears the filter and shows all menus and items.
     * Otherwise, searches for matches in menu names and item names (case-insensitive).
     * Updates the {@code menuAdapter} with the filter results and shows/hides a "no results" message.
     *
     * @param query The search query string.
     */
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


    /**
     * Displays an overlay with detailed information about the selected menu item.
     * Populates the overlay with the item's name, price (including active discounts),
     * description, category, allergens, availability status, and image.
     *
     * @param item The {@link MenuItem} to display.
     */
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


    /**
     * Displays an overlay with information about the selected menu.
     * Populates the overlay with the menu's name and image.
     *
     * @param menu The {@link Menu} to display.
     */
    private void showMenuView(Menu menu)
    {
        currentMenu = menu;
        toggleOverlay(menuViewOverlay, true);

        menuViewName.setText(menu.getName());

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
    }

    /**
     * Prepares and displays the item edit overlay for an existing menu item.
     * Populates the form fields with the item's current data.
     *
     * @param item The {@link MenuItem} to be edited.
     */
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


    /**
     * Prepares and displays the menu edit overlay for an existing menu.
     * Populates the form fields with the menu's current data.
     *
     * @param menu The {@link Menu} to be edited.
     */
    private void editExistingMenu(Menu menu)
    {
        imageEdited = false;
        isEditMode = true;
        currentMenu = menu;
        currentType = "Menu";

        menuEditOverlayName.setText("Edit Menu");
        editMenuName.setText(menu.getName());

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

    /**
     * Saves the changes made to a menu item (either new or existing).
     * Validates the input fields. If an image was edited, uploads it to Firebase Storage first.
     * Then proceeds to save or update the item data in Firestore.
     */
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

    /**
     * Fetches the current number of items in a given menu.
     * This is used to determine the {@code orderIndex} for a new item.
     *
     * @param menuID   The ID of the menu.
     * @param listener A callback to receive the item count.
     */
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

    /**
     * Proceeds with updating an existing menu item in Firestore.
     * Handles cases where the item is moved to a different menu, which involves
     * deleting the item from the old menu, adding it to the new menu, and shifting item indexes.
     *
     * @param imageURL The URL of the item's image (can be existing or new).
     */
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

    /**
     * Proceeds with saving a new menu item to Firestore.
     * Determines the item's {@code orderIndex} based on the current number of items in the selected menu.
     *
     * @param imageURL The URL of the item's image (can be null if no image was added).
     */
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
            if(imageURL == null) // If no image was uploaded, generate a new ID
            {
                itemRef = db.collection("Restaurants").document(restaurantID)
                        .collection("Menus").document(selectedMenuID)
                        .collection("Items").document();
            } else // If an image was uploaded, currentMenuItem.getItemID() was set in generateFileName()
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

    /**
     * Interface for a callback to be invoked when the item count for a menu is fetched.
     */
    interface OnItemCountFetchedListener
    {
        /**
         * Called when the item count has been successfully fetched.
         *
         * @param itemCount The number of items in the menu.
         */
        void onItemCountFetched(int itemCount);
    }

    /**
     * Shifts the {@code orderIndex} of items in a menu downwards after an item is removed.
     * This ensures that there are no gaps in the ordering.
     *
     * @param menuID       The ID of the menu.
     * @param removedIndex The index of the item that was removed.
     * @param onComplete   A {@link Runnable} to be executed after the shifting is complete.
     */
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


    /**
     * Saves the changes made to a menu (either new or existing).
     * Validates the menu name. If an image was edited, uploads it to Firebase Storage first.
     * Then proceeds to save or update the menu data in Firestore.
     */
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

    /**
     * Proceeds with updating an existing menu in Firestore.
     *
     * @param imageURL The URL of the menu's image (can be existing or new).
     */
    private void proceedWithMenuUpdate(String imageURL)
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

        // Update existing menu
        currentMenu.setName(name);
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

    /**
     * Proceeds with saving a new menu to Firestore.
     * Determines the menu's {@code menuIndex} based on the current number of menus.
     *
     * @param imageURL The URL of the menu's image (can be null if no image was added).
     */
    private void proceedWithMenuSave(String imageURL)
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
        // Create new menu
        DocumentReference menuRef = null;
        if(imageURL == null) // If no image was uploaded, generate a new ID
        {
            menuRef = db.collection("Restaurants").document(restaurantID)
                    .collection("Menus").document();
        } else // If an image was uploaded, currentMenu.getMenuID() was set in generateFileName()
        {
            menuRef = db.collection("Restaurants").document(restaurantID)
                    .collection("Menus").document(currentMenu.getMenuID());
        }


        Menu newMenu = new Menu();
        newMenu.setName(name);
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

    /**
     * Deletes a specified menu item from Firestore after user confirmation.
     * Also deletes the item's image from Firebase Storage and shifts the indexes of subsequent items.
     *
     * @param item The {@link MenuItem} to delete.
     */
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
                    deleteOldImage(null); // Pass null to indicate deletion without new image
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


    /**
     * Deletes a specified menu and all its associated items from Firestore after user confirmation.
     * Also deletes the menu's image from Firebase Storage.
     *
     * @param menu The {@link Menu} to delete.
     */
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
                                    deleteOldImage(null); // Pass null for deletion
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

    /**
     * Shows or hides the loading overlay and progress bar.
     *
     * @param isLoading True to show loading, false to hide.
     */
    private void showLoading(boolean isLoading)
    {
        if(progressBar != null)
        {
            toggleOverlay(loadingOverlay, isLoading);
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Toggles the visibility of a given overlay. Ensures only one overlay is visible at a time.
     *
     * @param overlay The {@link RelativeLayout} overlay to show or hide.
     * @param show    True to show the overlay, false to hide it.
     */
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

    /**
     * Initiates the process of selecting or taking a new image for a menu or menu item.
     * Checks for camera and storage permissions before proceeding.
     */
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

    /**
     * Checks if the necessary storage permissions are granted based on the Android version.
     *
     * @return True if storage permissions are granted, false otherwise.
     */
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

    /**
     * Requests necessary permissions (camera and storage) from the user if not already granted.
     */
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

    /**
     * ActivityResultLauncher for handling multiple permission requests.
     * If permissions are granted, shows the image selection bottom sheet.
     */
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

    /**
     * Displays a bottom sheet dialog allowing the user to choose between taking a photo
     * or selecting an image from the gallery.
     */
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

    /**
     * Launches an intent to capture an image using the device's camera.
     * The captured image is saved to a temporary file.
     */
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

    /**
     * Launches an intent to pick an image from the device's gallery.
     */
    private void pickFromGallery()
    {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        Log.d(TAG, "Launching gallery picker...");
        imagePickerLauncher.launch(intent);
    }

    /**
     * ActivityResultLauncher for handling the result from the camera intent.
     * If an image is successfully captured, it's processed, displayed, and prepared for upload.
     */
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

    /**
     * ActivityResultLauncher for handling the result from the gallery picker intent.
     * If an image is successfully selected, it's processed, displayed, and prepared for upload.
     */
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

    /**
     * Creates a temporary image file in the app's external files directory.
     *
     * @return The created {@link File} object, or null if an error occurred.
     */
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

    /**
     * Updates the appropriate ImageView (for menu or menu item) in the edit overlay
     * with the newly selected/captured image.
     *
     * @param bitmap The {@link Bitmap} of the image to display.
     */
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

    /**
     * Uploads the given Bitmap image to Firebase Storage.
     * Generates a unique filename based on whether it's a new or existing menu/item.
     * After successful upload, proceeds to save/update the Firestore document with the new image URL.
     *
     * @param bitmap The {@link Bitmap} image to upload.
     */
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
                        if(newItem || newMenu) // If creating a new entity
                        {
                            if("Menu".equals(currentType))
                            {
                                if(newMenu)
                                {
                                    proceedWithMenuSave(downloadUri.toString());
                                } else // This case should ideally not happen if newItem/newMenu is true
                                {
                                    proceedWithMenuUpdate(downloadUri.toString());
                                }

                            } else if("MenuItem".equals(currentType))
                            {
                                if(newItem)
                                {
                                    proceedWithItemSave(downloadUri.toString());
                                } else // This case should ideally not happen if newItem/newMenu is true
                                {
                                    proceedWithItemUpdate(downloadUri.toString());
                                }
                            }
                        } else // If editing an existing entity
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


    /**
     * Generates a unique filename for an image to be uploaded to Firebase Storage.
     * The filename path depends on whether it's an image for a "Menu" or "MenuItem",
     * and whether it's a new entity or an existing one.
     * For new entities, it first generates a new Firestore document ID to use in the filename.
     *
     * @return The generated unique filename string.
     */
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

                currentMenu = new Menu(); // Ensure currentMenu is initialized
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

                currentMenuItem = new MenuItem(); // Ensure currentMenuItem is initialized
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


    /**
     * Corrects the orientation of an image based on its EXIF data.
     *
     * @param imageUri The URI of the image.
     * @param bitmap   The {@link Bitmap} of the image.
     * @return The orientation-corrected {@link Bitmap}.
     */
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
                case ExifInterface.ORIENTATION_ROTATE_270: // Corrected constant
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

    /**
     * Deletes the old image from Firebase Storage before updating to a new one or when deleting an entity.
     *
     * @param newImageURL The URL of the new image. If null, it indicates the entity is being deleted,
     *                    and the old image should be removed without proceeding to an update.
     */
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

                    // Only proceed if there's an old image URL
                    if(oldImageURL != null && !oldImageURL.isEmpty())
                    {
                        // And it's different from the new one (or if newImageURL is null for deletion)
                        if(newImageURL == null || !newImageURL.equals(oldImageURL))
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
                                    if(deleteMode) // If in delete mode for the entity
                                    {
                                        deleteMode = false; // Reset flag
                                    } else if(newImageURL != null) // If not deleting entity, proceed with update
                                    {
                                        if("Menu".equals(currentType))
                                        {
                                            if(newMenu) proceedWithMenuSave(newImageURL);
                                            else proceedWithMenuUpdate(newImageURL);
                                        } else if("MenuItem".equals(currentType))
                                        {
                                            if(newItem) proceedWithItemSave(newImageURL);
                                            else proceedWithItemUpdate(newImageURL);
                                        }
                                    }
                                }).addOnFailureListener(e ->
                                {
                                    Log.e(TAG, "Error deleting old image", e);
                                    // Even if deletion fails, proceed with saving/updating if not in deleteMode
                                    if(deleteMode)
                                    {
                                        deleteMode = false;
                                    } else if(newImageURL != null)
                                    {
                                        if("Menu".equals(currentType))
                                        {
                                            if(newMenu) proceedWithMenuSave(newImageURL);
                                            else proceedWithMenuUpdate(newImageURL);
                                        } else if("MenuItem".equals(currentType))
                                        {
                                            if(newItem) proceedWithItemSave(newImageURL);
                                            else proceedWithItemUpdate(newImageURL);
                                        }
                                    }
                                });
                            } catch(Exception e)
                            {
                                Log.e(TAG, "Error parsing old image URL: " + oldImageURL, e);
                            }
                        } else if(newImageURL != null)
                        { // Old and new URLs are the same, no need to delete/re-upload
                            if("Menu".equals(currentType)) proceedWithMenuUpdate(newImageURL);
                            else if("MenuItem".equals(currentType))
                                proceedWithItemUpdate(newImageURL);
                        }
                    } else if(newImageURL != null)
                    { // No old image, just proceed with saving the new one
                        if(deleteMode)
                        {
                            deleteMode = false;
                        } else
                        {
                            if("Menu".equals(currentType))
                            {
                                if(newMenu) proceedWithMenuSave(newImageURL);
                                else proceedWithMenuUpdate(newImageURL);
                            } else if("MenuItem".equals(currentType))
                            {
                                if(newItem) proceedWithItemSave(newImageURL);
                                else proceedWithItemUpdate(newImageURL);
                            }
                        }
                    }
                } else if(newImageURL != null)
                { // Document doesn't exist (shouldn't happen for edit, but handle for new)
                    if("Menu".equals(currentType) && newMenu) proceedWithMenuSave(newImageURL);
                    else if("MenuItem".equals(currentType) && newItem)
                        proceedWithItemSave(newImageURL);
                }
            }).addOnFailureListener(e ->
            {
                Log.e(TAG, "Error fetching document to delete old image", e);
            });
        }
    }

    /**
     * Applies a discount to selected menu items or an entire menu.
     * Gathers discount details (amount, type, scope, schedule) from the UI.
     * Saves the discount information to the "Discounts" subcollection of the relevant
     * menu items in Firestore.
     */
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
                if(itemIDs == null || selectedItemPosition >= itemIDs.size()) // Check against full size
                {
                    Toast.makeText(getContext(), "Invalid item selection", Toast.LENGTH_SHORT).show();
                    showLoading(false);
                    return;
                }
                // Use selectedItemID directly as it's the actual ID from the spinner's tag
                db.collection("Restaurants").document(restaurantID)
                        .collection("Menus").document(selectedMenuID)
                        .collection("Items").document(selectedItemID) // Use the direct item ID
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