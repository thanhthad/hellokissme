package com.example.assignment2.activities;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar; // QUAN TRỌNG: Import Toolbar

import com.example.assignment2.R;
import com.example.assignment2.adapters.ExpenseAdapter;
import com.example.assignment2.database.DatabaseHelper;
import com.example.assignment2.models.Budget;
import com.example.assignment2.models.Category;
import com.example.assignment2.models.Expense;

import java.text.SimpleDateFormat; // For formatting date
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private EditText etDescription, etAmount, etDate;
    private Spinner spCategory;
    private Button btnAdd, btnUpdate, btnDelete;
    // private Button btnUserLogout; // Consider removing if using menu logout
    private ListView lvExpenses;
    private DatabaseHelper dbHelper;
    private ExpenseAdapter expenseAdapter;
    private List<Expense> expenseList;
    // Store Category objects for better handling
    private List<Category> categorySpinnerList;
    private Category selectedCategoryObject; // To store the selected Category object

    private int selectedExpenseId = -1;
    private int currentAccountId = -1;

    private static final int RC_MANAGE_CATEGORIES = 101;
    // private static final int RC_BUDGET_ACTIVITY = 102; // If you need a result from BudgetActivity

    // Constants for "Add new category" placeholder in Spinner
    private static final String ADD_NEW_CATEGORY_PLACEHOLDER_NAME = "+ Thêm danh mục mới";
    private static final int ADD_NEW_CATEGORY_PLACEHOLDER_ID = -99; // A unique ID
    private int previousSpinnerSelection = 0; // To restore selection if "Add new" is cancelled

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // --- Toolbar Setup ---
        Toolbar toolbar = findViewById(R.id.toolbar_main); // Make sure R.id.toolbar_main exists in activity_main.xml
        setSupportActionBar(toolbar);
        // Optional: Set title for the Toolbar
        // if (getSupportActionBar() != null) {
        //     getSupportActionBar().setTitle("Quản Lý Chi Tiêu");
        // }
        // --- End Toolbar Setup ---

        SharedPreferences sharedPref = getSharedPreferences(LoginActivity.PREF_NAME, Context.MODE_PRIVATE);
        currentAccountId = sharedPref.getInt(LoginActivity.KEY_ACCOUNT_ID, -1);

        if (currentAccountId == -1) {
            Toast.makeText(this, "Vui lòng đăng nhập lại.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        etDescription = findViewById(R.id.etDescription);
        etAmount = findViewById(R.id.etAmount);
        spCategory = findViewById(R.id.spCategory);
        etDate = findViewById(R.id.etDate);
        btnAdd = findViewById(R.id.btnAdd);
        btnUpdate = findViewById(R.id.btnUpdate);
        btnDelete = findViewById(R.id.btnDelete);
        lvExpenses = findViewById(R.id.lvExpenses);
        Button btnUserLogout = findViewById(R.id.btnUserLogout); // Get reference
        if (btnUserLogout != null) {
            btnUserLogout.setVisibility(View.GONE); // Hide the button if using menu logout
        }


        dbHelper = new DatabaseHelper(this);
        categorySpinnerList = new ArrayList<>(); // Initialize

        loadCategoriesIntoSpinner(); // Load categories

        spCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Category selected = (Category) parent.getItemAtPosition(position);
                if (selected != null) {
                    if (selected.getId() == ADD_NEW_CATEGORY_PLACEHOLDER_ID && ADD_NEW_CATEGORY_PLACEHOLDER_NAME.equals(selected.getName())) {
                        // User selected "+ Thêm danh mục mới"
                        showAddCategoryDialogFromMain();
                        // Restore previous selection or handle as needed
                        spCategory.setSelection(previousSpinnerSelection);
                        selectedCategoryObject = (Category) parent.getItemAtPosition(previousSpinnerSelection);
                    } else {
                        selectedCategoryObject = selected;
                        previousSpinnerSelection = position; // Update previous selection
                    }
                } else {
                    // Fallback if somehow the selected item is null
                    if (parent.getAdapter() != null && parent.getAdapter().getCount() > 0) {
                        selectedCategoryObject = (Category) parent.getItemAtPosition(0); // Default to first item
                        if(selectedCategoryObject.getId() == ADD_NEW_CATEGORY_PLACEHOLDER_ID && parent.getAdapter().getCount() > 1){
                            selectedCategoryObject = (Category) parent.getItemAtPosition(1); // Try second if first is placeholder
                            previousSpinnerSelection = 1;
                        } else {
                            previousSpinnerSelection = 0;
                        }
                    } else {
                        selectedCategoryObject = new Category(0,"Khác", null); // Absolute fallback
                        previousSpinnerSelection = 0;
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Try to set to the first actual category item if available
                if (parent.getAdapter() != null && parent.getAdapter().getCount() > 0) {
                    Category firstCategory = (Category) parent.getItemAtPosition(0);
                    if (firstCategory != null && firstCategory.getId() != ADD_NEW_CATEGORY_PLACEHOLDER_ID) {
                        selectedCategoryObject = firstCategory;
                        previousSpinnerSelection = 0;
                    } else if (parent.getAdapter().getCount() > 1) {
                        selectedCategoryObject = (Category) parent.getItemAtPosition(1); // Try second if first is placeholder
                        previousSpinnerSelection = 1;
                    } else {
                        selectedCategoryObject = new Category(0,"Khác", null); // Fallback
                        previousSpinnerSelection = 0;
                    }
                } else {
                    selectedCategoryObject = new Category(0,"Khác", null); // Absolute fallback
                    previousSpinnerSelection = 0;
                }
            }
        });

        etDate.setOnClickListener(v -> showDatePickerDialog());

        loadExpensesIntoListView();

        btnAdd.setOnClickListener(v -> addExpense());
        btnUpdate.setOnClickListener(v -> updateExpense());
        btnDelete.setOnClickListener(v -> deleteExpenseWithConfirmation());

        lvExpenses.setOnItemClickListener((parent, view, position, id) -> {
            Expense selectedExpense = expenseList.get(position);
            if (selectedExpense == null) return;

            etDescription.setText(selectedExpense.getDescription());
            // Format amount to avoid scientific notation and remove trailing .0
            etAmount.setText(String.format(Locale.US, "%.0f", selectedExpense.getAmount()));
            etDate.setText(selectedExpense.getDate());

            // Set spinner selection based on the category name/object
            setSpinnerToCategory(selectedExpense.getCategory()); // Use category name from Expense

            selectedExpenseId = selectedExpense.getId();
            // Toast.makeText(MainActivity.this, "Đã chọn: " + selectedExpense.getDescription(), Toast.LENGTH_SHORT).show(); // Optional
        });

        // The btnUserLogout's click listener is now handled by the menu item.
        // If you keep the button visible, you'd need its listener here.
        // btnUserLogout.setOnClickListener(v -> logoutUser());

    } // End of onCreate

    private void loadCategoriesIntoSpinner() {
        if (currentAccountId != -1) {
            List<Category> actualCategories = dbHelper.getCategoriesForAccount(currentAccountId);
            if (actualCategories == null) {
                actualCategories = new ArrayList<>();
            }

            categorySpinnerList.clear(); // Clear previous items

            // Add the "Add new category" placeholder at the beginning
            categorySpinnerList.add(new Category(ADD_NEW_CATEGORY_PLACEHOLDER_ID, ADD_NEW_CATEGORY_PLACEHOLDER_NAME, null));

            categorySpinnerList.addAll(actualCategories); // Add existing categories

            // If no actual categories exist (besides "+ Thêm mới"), add a default "Khác"
            if (actualCategories.isEmpty()) {
                // Ensure "Khác" isn't added if it's already the placeholder or only item.
                // This logic might need refinement based on how you want "Khác" to behave.
                boolean hasKhac = false;
                for(Category cat : categorySpinnerList){
                    if("Khác".equals(cat.getName()) && cat.getId() != ADD_NEW_CATEGORY_PLACEHOLDER_ID){
                        hasKhac = true;
                        break;
                    }
                }
                if(!hasKhac) {
                    categorySpinnerList.add(new Category(0, "Khác", null)); // ID 0 as placeholder for "Khác" if needed
                }
            }

            ArrayAdapter<Category> spinnerAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, categorySpinnerList);
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spCategory.setAdapter(spinnerAdapter);

            // Set default selected category
            if (categorySpinnerList.size() > 1 && categorySpinnerList.get(1).getId() != ADD_NEW_CATEGORY_PLACEHOLDER_ID) {
                // If there's an actual category after the placeholder
                selectedCategoryObject = categorySpinnerList.get(1);
                spCategory.setSelection(1);
                previousSpinnerSelection = 1;
            } else if (!categorySpinnerList.isEmpty()){
                // If only placeholder or placeholder + "Khác"
                selectedCategoryObject = categorySpinnerList.get(0);
                spCategory.setSelection(0);
                previousSpinnerSelection = 0;
                if(selectedCategoryObject.getId() == ADD_NEW_CATEGORY_PLACEHOLDER_ID && categorySpinnerList.size() > 1){
                    selectedCategoryObject = categorySpinnerList.get(1); // Should be "Khac"
                    spCategory.setSelection(1);
                    previousSpinnerSelection = 1;
                } else if (selectedCategoryObject.getId() == ADD_NEW_CATEGORY_PLACEHOLDER_ID){
                    // Only placeholder, this state should ideally be avoided by adding "Khác"
                    selectedCategoryObject = new Category(0, "Khác", null); // Fallback
                }
            } else {
                // Absolute fallback if spinnerCategories is somehow empty
                selectedCategoryObject = new Category(0, "Khác", null);
                spCategory.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, List.of(selectedCategoryObject)));
                previousSpinnerSelection = 0;
            }

        } else {
            ArrayAdapter<String> emptyAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, new String[]{"Lỗi tải danh mục"});
            spCategory.setAdapter(emptyAdapter);
            selectedCategoryObject = new Category(0, "Lỗi tải danh mục", null);
            previousSpinnerSelection = 0;
        }
    }

    // Trong MainActivity.java

    private void addExpense() {
        String description = etDescription.getText().toString().trim();
        String amountStr = etAmount.getText().toString().trim();
        String date = etDate.getText().toString().trim();

        // Lấy Category object từ spinner
        Category selectedCategoryFromSpinner = (Category) spCategory.getSelectedItem();

        if (description.isEmpty() || amountStr.isEmpty() || date.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedCategoryFromSpinner == null ||
                (selectedCategoryFromSpinner.getId() == ADD_NEW_CATEGORY_PLACEHOLDER_ID &&
                        ADD_NEW_CATEGORY_PLACEHOLDER_NAME.equals(selectedCategoryFromSpinner.getName()))) {
            Toast.makeText(this, "Vui lòng chọn một danh mục hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        // Lấy tên category từ selectedCategoryObject đã được cập nhật bởi onItemSelected
        String categoryName = selectedCategoryObject.getName();
        // Hoặc, nếu bạn muốn chắc chắn lấy từ spinner tại thời điểm click:
        // String categoryName = ((Category) spCategory.getSelectedItem()).getName();

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                Toast.makeText(this, "Số tiền phải lớn hơn 0", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        Expense newExpense = new Expense(0, description, amount, categoryName, date, currentAccountId);

        // Truyền currentAccountId vào addExpense
        long result = dbHelper.addExpense(newExpense, currentAccountId);

        if (result > 0) {
            Toast.makeText(this, "Thêm chi tiêu thành công!", Toast.LENGTH_SHORT).show();
            loadExpensesIntoListView(); // Tải lại danh sách
            clearInputFields();
        } else if (result == -2) { // Mã lỗi cho vượt ngân sách
            // Lấy thông tin ngân sách để hiển thị chi tiết hơn (tùy chọn)
            Category categoryForBudgetCheck = dbHelper.getCategoryByName(categoryName, currentAccountId);
            if (categoryForBudgetCheck != null) {
                Budget budget = dbHelper.getBudgetForCategory(currentAccountId, categoryForBudgetCheck.getId());
                if (budget != null) {
                    // Giả sử bạn muốn kiểm tra cho tháng hiện tại của chi phí
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    java.text.SimpleDateFormat monthYearFormat = new java.text.SimpleDateFormat("yyyy-MM", Locale.getDefault());
                    String expenseMonthYear = "";
                    try {
                        java.util.Date parsedDate = sdf.parse(date);
                        expenseMonthYear = monthYearFormat.format(parsedDate);
                    } catch (java.text.ParseException e) {
                        // Xử lý lỗi nếu ngày không hợp lệ
                    }
                    double currentExpensesInPeriod = dbHelper.getTotalExpensesForCategory(currentAccountId, categoryName, expenseMonthYear);
                    double remainingBudget = budget.getAmount() - currentExpensesInPeriod;

                    new AlertDialog.Builder(this)
                            .setTitle("Vượt Ngân Sách")
                            .setMessage("Không thể thêm chi tiêu này.\n\n" +
                                    "Danh mục: " + categoryName + "\n" +
                                    "Ngân sách: " + String.format(Locale.US, "%.0f", budget.getAmount()) + "\n" +
                                    "Đã chi: " + String.format(Locale.US, "%.0f", currentExpensesInPeriod) + "\n" +
                                    "Còn lại: " + String.format(Locale.US, "%.0f", remainingBudget) + "\n" +
                                    "Chi tiêu mới: " + String.format(Locale.US, "%.0f", amount) + "\n\n" +
                                    "Chi tiêu này sẽ làm bạn vượt quá ngân sách đã đặt cho danh mục này trong kỳ hiện tại.")
                            .setPositiveButton("OK", null)
                            .show();
                } else {
                    Toast.makeText(this, "Chi tiêu vượt quá ngân sách đã đặt cho danh mục '" + categoryName + "'.", Toast.LENGTH_LONG).show();
                }
            } else {
                // Trường hợp không tìm thấy category, dù không nên xảy ra nếu spinner hoạt động đúng
                Toast.makeText(this, "Lỗi: Không tìm thấy danh mục để kiểm tra ngân sách.", Toast.LENGTH_LONG).show();
            }

        } else if (result == -3) { // Mã lỗi cho trường hợp không có budget (nếu bạn implement logic chặn)
            Toast.makeText(this, "Không có ngân sách nào được đặt cho danh mục '" + categoryName + "'. Vui lòng đặt ngân sách trước.", Toast.LENGTH_LONG).show();
        } else if (result == -4) { // Mã lỗi cho category không tìm thấy
            Toast.makeText(this, "Lỗi: Danh mục '" + categoryName + "' không tồn tại.", Toast.LENGTH_LONG).show();
        }
        else {
            Toast.makeText(this, "Thêm chi tiêu thất bại. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateExpense() {
        if (selectedExpenseId == -1) {
            Toast.makeText(MainActivity.this, "Vui lòng chọn chi tiêu để cập nhật.", Toast.LENGTH_SHORT).show();
            return;
        }

        String description = etDescription.getText().toString().trim();
        String amountStr = etAmount.getText().toString().trim();
        String date = etDate.getText().toString().trim();

        if (selectedCategoryObject == null || ADD_NEW_CATEGORY_PLACEHOLDER_NAME.equals(selectedCategoryObject.getName())) {
            Toast.makeText(MainActivity.this, "Vui lòng chọn một danh mục hợp lệ.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (description.isEmpty() || amountStr.isEmpty() || date.isEmpty()) {
            Toast.makeText(MainActivity.this, "Vui lòng nhập đầy đủ thông tin chi tiêu.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!date.matches("\\d{4}-\\d{2}-\\d{2}")) {
            Toast.makeText(MainActivity.this, "Định dạng ngày không hợp lệ. Vui lòng dùng YYYY-MM-DD.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double amount = Double.parseDouble(amountStr);
            Expense expense = new Expense(selectedExpenseId, description, amount, selectedCategoryObject.getName(), date, currentAccountId);
            // Consider if budget check is needed for update as well.
            // dbHelper.updateExpense currently doesn't re-check budget.
            int rowsAffected = dbHelper.updateExpense(expense, currentAccountId);
            if (rowsAffected > 0) {
                Toast.makeText(MainActivity.this, "Đã cập nhật chi tiêu!", Toast.LENGTH_SHORT).show();
                clearInputFields();
                loadExpensesIntoListView();
                selectedExpenseId = -1;
            } else {
                Toast.makeText(MainActivity.this, "Cập nhật chi tiêu thất bại hoặc không có thay đổi!", Toast.LENGTH_SHORT).show();
            }
        } catch (NumberFormatException e) {
            Toast.makeText(MainActivity.this, "Số tiền không hợp lệ!", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteExpenseWithConfirmation() {
        if (selectedExpenseId == -1) {
            Toast.makeText(MainActivity.this, "Vui lòng chọn chi tiêu để xóa.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(MainActivity.this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa khoản chi tiêu này không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    int rowsAffected = dbHelper.deleteExpense(selectedExpenseId, currentAccountId);
                    if (rowsAffected > 0) {
                        Toast.makeText(MainActivity.this, "Đã xóa chi tiêu!", Toast.LENGTH_SHORT).show();
                        clearInputFields();
                        loadExpensesIntoListView();
                        selectedExpenseId = -1;
                    } else {
                        Toast.makeText(MainActivity.this, "Xóa chi tiêu thất bại!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }


    private void loadExpensesIntoListView() {
        expenseList = dbHelper.getAllExpensesForAccount(currentAccountId);
        if (expenseList == null) {
            expenseList = new ArrayList<>();
        }

        if (expenseAdapter == null) {
            // Ensure you have R.layout.list_item_expense and it's suitable for Expense objects
            expenseAdapter = new ExpenseAdapter(this, R.layout.list_item_expense, expenseList);
            // If constructor is ExpenseAdapter(Context context, int resource, List<Expense> objects)
            // expenseAdapter = new ExpenseAdapter(this, R.layout.list_item_expense, expenseList);
            lvExpenses.setAdapter(expenseAdapter);
        } else {
            expenseAdapter.clear();
            expenseAdapter.addAll(expenseList);
            expenseAdapter.notifyDataSetChanged();
        }
    }

    private void clearInputFields() {
        etDescription.setText("");
        etAmount.setText("");
        etDate.setText(""); // Có thể đặt ngày hiện tại làm mặc định nếu muốn
        // spCategory.setSelection(0); // Cẩn thận với vị trí placeholder
        if (categorySpinnerList.size() > 1 && categorySpinnerList.get(1).getId() != ADD_NEW_CATEGORY_PLACEHOLDER_ID) {
            spCategory.setSelection(1); // Chọn category thực đầu tiên
            previousSpinnerSelection = 1;
            selectedCategoryObject = categorySpinnerList.get(1);
        } else if (!categorySpinnerList.isEmpty()){
            // Nếu chỉ placeholder hoặc placeholder + "Khác"
            if(categorySpinnerList.get(0).getId() == ADD_NEW_CATEGORY_PLACEHOLDER_ID && categorySpinnerList.size() > 1){
                spCategory.setSelection(1); // Chọn "Khác"
                previousSpinnerSelection = 1;
                selectedCategoryObject = categorySpinnerList.get(1);
            } else {
                spCategory.setSelection(0);
                previousSpinnerSelection = 0;
                selectedCategoryObject = categorySpinnerList.get(0);
            }
        }
        selectedExpenseId = -1;
    }

    private void showDatePickerDialog() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year1, monthOfYear, dayOfMonth) -> {
                    // Format date to YYYY-MM-DD
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                    c.set(year1, monthOfYear, dayOfMonth);
                    etDate.setText(sdf.format(c.getTime()));
                }, year, month, day);
        datePickerDialog.show();
    }

    private void logoutUser() {
        SharedPreferences sharedPref = getSharedPreferences(LoginActivity.PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.remove(LoginActivity.KEY_ACCOUNT_ID);
        editor.putBoolean(LoginActivity.KEY_IS_LOGGED_IN, false); // Explicitly set logged out
        editor.apply();

        Toast.makeText(this, "Đã đăng xuất.", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // --- Menu Handling ---
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu); // Use your main_menu.xml
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_manage_categories) {
            Intent intent = new Intent(MainActivity.this, CategoryManagementActivity.class);
            startActivityForResult(intent, RC_MANAGE_CATEGORIES);
            return true;
        } else if (id == R.id.action_manage_budgets) {
            Intent intent = new Intent(MainActivity.this, BudgetActivity.class);
            startActivity(intent); // No result needed if BudgetActivity auto-refreshes
            return true;
        } else if (id == R.id.action_user_logout) {
            logoutUser();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_MANAGE_CATEGORIES) {
            // No need to check resultCode if CategoryManagementActivity always saves changes
            // and MainActivity just needs to reload categories.
            // However, checking RESULT_OK is good practice if CategoryManagementActivity sets it.
            if (resultCode == RESULT_OK || data != null && data.getBooleanExtra("categoriesChanged", false)) { // Example
                Toast.makeText(this, "Danh sách danh mục được làm mới.", Toast.LENGTH_SHORT).show();
            }
            loadCategoriesIntoSpinner(); // Reload categories into the spinner
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload categories and expenses when returning to this activity
        // This ensures data is fresh after changes in other activities (Category, Budget)
        loadCategoriesIntoSpinner();
        loadExpensesIntoListView();
    }

    // Dialog to add a new category directly from MainActivity Spinner
    private void showAddCategoryDialogFromMain() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        // Ensure R.layout.dialog_add_edit_category exists and has an EditText with id et_category_name_dialog
        View dialogView = inflater.inflate(R.layout.dialog_add_edit_category, null);
        builder.setView(dialogView);

        final EditText etCategoryNameDialog = dialogView.findViewById(R.id.et_category_name_dialog);
        builder.setTitle("Thêm Danh Mục Mới");

        builder.setPositiveButton("Thêm", (dialog, which) -> {
            String categoryName = etCategoryNameDialog.getText().toString().trim();
            if (categoryName.isEmpty()) {
                Toast.makeText(MainActivity.this, "Tên danh mục không được để trống.", Toast.LENGTH_SHORT).show();
                // Keep dialog open or re-select previous spinner item
                spCategory.setSelection(previousSpinnerSelection);
                return;
            }

            long result = dbHelper.addCategory(categoryName, currentAccountId); // Assuming addCategory returns id or error code
            if (result > 0) {
                Toast.makeText(MainActivity.this, "Đã thêm danh mục: " + categoryName, Toast.LENGTH_SHORT).show();
                loadCategoriesIntoSpinner(); // Reload spinner
                setSpinnerToCategory(categoryName); // Auto-select the newly added category
            } else if (result == -2) { // Assuming -2 is for existing category
                Toast.makeText(MainActivity.this, "Danh mục '" + categoryName + "' đã tồn tại.", Toast.LENGTH_SHORT).show();
                setSpinnerToCategory(categoryName); // Select the existing one
            } else {
                Toast.makeText(MainActivity.this, "Thêm danh mục thất bại.", Toast.LENGTH_SHORT).show();
                loadCategoriesIntoSpinner(); // Reload to restore previous valid state
                spCategory.setSelection(previousSpinnerSelection); // Restore previous selection
            }
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> {
            dialog.dismiss();
            loadCategoriesIntoSpinner(); // Reload to restore
            spCategory.setSelection(previousSpinnerSelection); // Restore previous selection
        });
        builder.setOnCancelListener(dialog -> {
            loadCategoriesIntoSpinner(); // Reload to restore
            spCategory.setSelection(previousSpinnerSelection); // Restore previous selection
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // Utility method to select a category in the spinner by its name
    private void setSpinnerToCategory(String categoryName) {
        if (categoryName == null) return;
        ArrayAdapter<Category> adapter = (ArrayAdapter<Category>) spCategory.getAdapter();
        if (adapter != null) {
            for (int i = 0; i < adapter.getCount(); i++) {
                Category categoryItem = adapter.getItem(i);
                if (categoryItem != null && categoryName.equals(categoryItem.getName()) && categoryItem.getId() != ADD_NEW_CATEGORY_PLACEHOLDER_ID) {
                    spCategory.setSelection(i);
                    selectedCategoryObject = categoryItem;
                    previousSpinnerSelection = i; // Update last valid selection
                    break;
                }
            }
        }
    }
}
