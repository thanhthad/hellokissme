package com.example.assignment2.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.assignment2.R;
import com.example.assignment2.adapters.CategoryAdapter;
import com.example.assignment2.database.DatabaseHelper;
import com.example.assignment2.models.Category;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class CategoryManagementActivity extends AppCompatActivity implements CategoryAdapter.OnCategoryActionListener {

    private RecyclerView rvCategories;
    private FloatingActionButton fabAddCategory;
    private DatabaseHelper dbHelper;
    private CategoryAdapter categoryAdapter;
    private List<Category> categoryList;
    private int currentAccountId = -1;
    private boolean categoriesChanged = false; // Flag to indicate if categories were modified

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_management);

        Toolbar toolbar = findViewById(R.id.toolbar_category_management);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Quản lý danh mục");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        SharedPreferences sharedPref = getSharedPreferences(LoginActivity.PREF_NAME, Context.MODE_PRIVATE);
        currentAccountId = sharedPref.getInt(LoginActivity.KEY_ACCOUNT_ID, -1);

        if (currentAccountId == -1) {
            Toast.makeText(this, "User session error. Please log in again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        dbHelper = new DatabaseHelper(this);
        rvCategories = findViewById(R.id.rv_categories);
        fabAddCategory = findViewById(R.id.fab_add_category);

        categoryList = new ArrayList<>();
        categoryAdapter = new CategoryAdapter(this, categoryList, currentAccountId, this);
        rvCategories.setLayoutManager(new LinearLayoutManager(this));
        rvCategories.setAdapter(categoryAdapter);

        loadCategories();

        fabAddCategory.setOnClickListener(v -> showAddEditCategoryDialog(null, -1));
    }

    private void loadCategories() {
        if (currentAccountId != -1) {
            List<Category> updatedList = dbHelper.getCategoriesForAccount(currentAccountId);
            categoryList.clear();
            categoryList.addAll(updatedList);
            categoryAdapter.notifyDataSetChanged(); // Use more specific notify methods if possible
        }
    }

    private void showAddEditCategoryDialog(final Category categoryToEdit, final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_edit_category, null);
        builder.setView(dialogView);

        final EditText etCategoryName = dialogView.findViewById(R.id.et_category_name_dialog);
        final String dialogTitle = (categoryToEdit == null) ? "Add New Category" : "Edit Category";
        builder.setTitle(dialogTitle);

        if (categoryToEdit != null) {
            etCategoryName.setText(categoryToEdit.getName());
        }

        builder.setPositiveButton((categoryToEdit == null) ? "Add" : "Save", (dialog, which) -> {
            String categoryName = etCategoryName.getText().toString().trim();
            if (TextUtils.isEmpty(categoryName)) {
                Toast.makeText(CategoryManagementActivity.this, "Category name cannot be empty.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (categoryToEdit == null) { // Add new category
                long result = dbHelper.addCategory(categoryName, currentAccountId);
                if (result > 0) {
                    Toast.makeText(CategoryManagementActivity.this, "Category added!", Toast.LENGTH_SHORT).show();
                    categoriesChanged = true;
                    loadCategories();
                } else if (result == -2) {
                    Toast.makeText(CategoryManagementActivity.this, "Category '" + categoryName + "' already exists.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(CategoryManagementActivity.this, "Failed to add category.", Toast.LENGTH_SHORT).show();
                }
            } else { // Edit existing category
                // Ensure it's a user-owned category before trying to update
                if (categoryToEdit.getAccountId() == null || categoryToEdit.getAccountId() != currentAccountId) {
                    Toast.makeText(CategoryManagementActivity.this, "Cannot edit global categories.", Toast.LENGTH_SHORT).show();
                    return;
                }
                Category updatedCategory = new Category(categoryToEdit.getId(), categoryName, currentAccountId);
                int result = dbHelper.updateCategory(updatedCategory);
                if (result > 0) {
                    Toast.makeText(CategoryManagementActivity.this, "Category updated!", Toast.LENGTH_SHORT).show();
                    categoriesChanged = true;
                    loadCategories(); // Reload to reflect changes
                } else if (result == -2) {
                    Toast.makeText(CategoryManagementActivity.this, "Category name '" + categoryName + "' already exists.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(CategoryManagementActivity.this, "Failed to update category or no changes made.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    @Override
    public void onEditCategory(Category category, int position) {
        // Global categories should already be filtered out by the adapter's logic
        // for showing the edit button, but double-check here.
        if (category.getAccountId() != null && category.getAccountId() == currentAccountId) {
            showAddEditCategoryDialog(category, position);
        } else {
            Toast.makeText(this, "Global categories cannot be edited.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDeleteCategory(Category category, int position) {
        if (category.getAccountId() == null || category.getAccountId() != currentAccountId) {
            Toast.makeText(this, "Global categories cannot be deleted.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Confirm Delete")
                .setMessage("Are you sure you want to delete the category '" + category.getName() + "'? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    int result = dbHelper.deleteCategory(category.getId(), currentAccountId);
                    if (result > 0) {
                        Toast.makeText(CategoryManagementActivity.this, "Category deleted!", Toast.LENGTH_SHORT).show();
                        categoriesChanged = true;
                        loadCategories(); // Reload to reflect removal
                    } else if (result == -2) {
                        Toast.makeText(CategoryManagementActivity.this, "Cannot delete category. It is currently in use by one or more expenses.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(CategoryManagementActivity.this, "Failed to delete category.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed(); // Or finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void finish() {
        if (categoriesChanged) {
            setResult(RESULT_OK); // Signal to MainActivity that categories might have changed
        }
        super.finish();
    }
}
