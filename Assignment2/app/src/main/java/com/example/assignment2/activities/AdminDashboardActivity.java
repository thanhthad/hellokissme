package com.example.assignment2.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.assignment2.R;
import com.example.assignment2.adapters.AccountAdapter; // You NEED to create this Adapter
import com.example.assignment2.database.DatabaseHelper;
import com.example.assignment2.models.Account;
// Import a hashing utility if you have one, e.g., for passwords
// import com.example.assignment2.utils.SecurityUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Implement the AccountAdapter's listener interface
public class AdminDashboardActivity extends AppCompatActivity implements AccountAdapter.OnAccountActionListener {

    private static final String TAG = "AdminDashboardActivity";

    private TextView tvWelcomeAdmin;
    private Button btnViewAllExpenses;
    private Button btnAdminLogout;

    private RecyclerView rvAccounts;
    private AccountAdapter accountAdapter;
    private List<Account> accountList;
    private DatabaseHelper dbHelper;
    private FloatingActionButton fabAddAccount;

    private int loggedInAdminId = -1; // To prevent admin from deleting themselves

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Ensure your layout activity_admin_dashboard.xml has rvAccounts and fabAddAccount
        setContentView(R.layout.activity_admin_dashboard);

        tvWelcomeAdmin = findViewById(R.id.tvWelcomeAdmin);
        btnViewAllExpenses = findViewById(R.id.btnViewAllExpenses);
        btnAdminLogout = findViewById(R.id.btnAdminLogout);

        // Initialize UI for account management
        rvAccounts = findViewById(R.id.rvAccounts);
        fabAddAccount = findViewById(R.id.fabAddAccount);

        dbHelper = new DatabaseHelper(this);
        accountList = new ArrayList<>();

        SharedPreferences sharedPref = getSharedPreferences(LoginActivity.PREF_NAME, Context.MODE_PRIVATE);
        loggedInAdminId = sharedPref.getInt(LoginActivity.KEY_ACCOUNT_ID, -1);
        String userRole = sharedPref.getString(LoginActivity.KEY_ACCOUNT_ROLE, null);

        if (loggedInAdminId == -1 || !"admin".equalsIgnoreCase(userRole)) {
            Toast.makeText(this, "Phiên đăng nhập không hợp lệ hoặc bạn không phải admin.", Toast.LENGTH_LONG).show();
            logoutAndGoToLogin();
            return;
        }

        tvWelcomeAdmin.setText("Chào mừng Admin (ID: " + loggedInAdminId + ")");

        setupRecyclerView();
        loadAccounts();

        btnViewAllExpenses.setOnClickListener(v -> {
            // TODO: Implement logic for viewing all expenses, possibly start a new activity
            // Example: Intent intent = new Intent(AdminDashboardActivity.this, ViewAllExpensesActivity.class);
            // startActivity(intent);
            Toast.makeText(AdminDashboardActivity.this, "Chức năng xem tất cả chi tiêu (chưa triển khai).", Toast.LENGTH_SHORT).show();
        });

        btnAdminLogout.setOnClickListener(v -> logoutAndGoToLogin());

        fabAddAccount.setOnClickListener(view -> showAccountDialog(null)); // null for adding new account
    }

    private void setupRecyclerView() {
        accountAdapter = new AccountAdapter(this, accountList, this);
        rvAccounts.setLayoutManager(new LinearLayoutManager(this));
        rvAccounts.setAdapter(accountAdapter);
    }

    private void loadAccounts() {
        accountList.clear();
        List<Account> fetchedAccounts = dbHelper.getAllAccounts(); // Using the new method
        if (fetchedAccounts != null) {
            accountList.addAll(fetchedAccounts);
        }
        if (accountAdapter != null) {
            accountAdapter.notifyDataSetChanged();
        } else {
            Log.e(TAG, "AccountAdapter is null when trying to load accounts. Initializing again.");
            setupRecyclerView(); // Re-initialize if it was somehow null
        }
    }

    /**
     * Shows a dialog to add a new account or edit an existing one.
     * @param accountToEdit The account to edit, or null if adding a new account.
     */
    private void showAccountDialog(final Account accountToEdit) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_edit_account, null); // Create this layout file
        builder.setView(dialogView);

        final EditText etUsername = dialogView.findViewById(R.id.etDialogUsername);
        final EditText etPassword = dialogView.findViewById(R.id.etDialogPassword);
        final EditText etConfirmPassword = dialogView.findViewById(R.id.etDialogConfirmPassword);
        final Spinner spinnerRole = dialogView.findViewById(R.id.spinnerDialogRole);

        // Setup Spinner for roles
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new String[]{"user", "admin"});
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(roleAdapter);

        final String dialogTitle;
        final String positiveButtonText;

        if (accountToEdit != null) {
            dialogTitle = "Sửa tài khoản";
            positiveButtonText = "Lưu thay đổi";
            etUsername.setText(accountToEdit.getUsername());
            // Password field is usually left blank for editing for security,
            // or placeholder text like "Để trống nếu không đổi mật khẩu"
            etPassword.setHint("Để trống nếu không đổi mật khẩu");
            etConfirmPassword.setHint("Để trống nếu không đổi mật khẩu");

            if ("admin".equalsIgnoreCase(accountToEdit.getRole())) {
                spinnerRole.setSelection(1);
            } else {
                spinnerRole.setSelection(0);
            }
        } else {
            dialogTitle = "Thêm tài khoản mới";
            positiveButtonText = "Thêm";
        }
        builder.setTitle(dialogTitle);

        builder.setPositiveButton(positiveButtonText, null); // Set to null to override and control dismissal
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());

        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                String username = etUsername.getText().toString().trim();
                String password = etPassword.getText().toString().trim();
                String confirmPassword = etConfirmPassword.getText().toString().trim();
                String role = spinnerRole.getSelectedItem().toString();

                if (TextUtils.isEmpty(username)) {
                    etUsername.setError("Tên người dùng không được để trống");
                    return;
                }

                // --- Password Validation ---
                if (accountToEdit == null) { // Adding new user
                    if (TextUtils.isEmpty(password)) {
                        etPassword.setError("Mật khẩu không được để trống");
                        return;
                    }
                    if (password.length() < 3) { // Example: min length
                        etPassword.setError("Mật khẩu phải có ít nhất 3 ký tự");
                        return;
                    }
                    if (!password.equals(confirmPassword)) {
                        etConfirmPassword.setError("Mật khẩu xác nhận không khớp");
                        return;
                    }
                } else { // Editing existing user
                    if (!TextUtils.isEmpty(password)) { // User wants to change password
                        if (password.length() < 3) {
                            etPassword.setError("Mật khẩu phải có ít nhất 3 ký tự");
                            return;
                        }
                        if (!password.equals(confirmPassword)) {
                            etConfirmPassword.setError("Mật khẩu xác nhận không khớp");
                            return;
                        }
                    }
                }
                // --- End Password Validation ---


                // !!! IMPORTANT: HASH THE PASSWORD before saving to database !!!
                // String hashedPassword = password; // Placeholder
                // if (!TextUtils.isEmpty(password)) {
                //    hashedPassword = SecurityUtils.hashPassword(password); // Replace with your hashing logic
                // }
                // For now, we'll use plain text for simplicity, but THIS IS INSECURE.

                if (accountToEdit == null) { // Add new account
                    if (dbHelper.checkUsernameExists(username)) {
                        etUsername.setError("Tên người dùng đã tồn tại");
                        return;
                    }
                    Account newAccount = new Account(username, password, role); // Use plain password for now
                    long id = dbHelper.addAccount(newAccount);
                    if (id > 0) {
                        Toast.makeText(AdminDashboardActivity.this, "Đã thêm tài khoản mới.", Toast.LENGTH_SHORT).show();
                        loadAccounts(); // Refresh the list
                        dialog.dismiss();
                    } else if (id == -3) {
                        etUsername.setError("Tên người dùng đã tồn tại (DB check).");
                    } else if (id == -2) {
                        Toast.makeText(AdminDashboardActivity.this, "Vai trò không hợp lệ.", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Toast.makeText(AdminDashboardActivity.this, "Thêm tài khoản thất bại.", Toast.LENGTH_SHORT).show();
                    }
                } else { // Edit existing account
                    // Check if username is changed and if the new username already exists (excluding itself)
                    if (!accountToEdit.getUsername().equals(username) && dbHelper.checkUsernameExists(username)) {
                        etUsername.setError("Tên người dùng này đã được sử dụng bởi tài khoản khác.");
                        return;
                    }

                    accountToEdit.setUsername(username);
                    if (!TextUtils.isEmpty(password)) {
                        accountToEdit.setPassword(password); // Set new plain password for now
                    }
                    // If password is empty, DatabaseHelper's updateAccount should not update it
                    // if it's designed to only update password if a new one is provided.

                    // Prevent admin from changing their own role from 'admin' if they are the only admin
                    // (More complex logic, for now, we allow role change but ensure they can't delete themselves)
                    if (accountToEdit.getId() == loggedInAdminId && !role.equalsIgnoreCase("admin")) {
                        Toast.makeText(AdminDashboardActivity.this, "Bạn không thể thay đổi vai trò của chính mình từ admin.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    accountToEdit.setRole(role);


                    int rowsAffected = dbHelper.updateAccount(accountToEdit);
                    if (rowsAffected > 0) {
                        Toast.makeText(AdminDashboardActivity.this, "Đã cập nhật tài khoản.", Toast.LENGTH_SHORT).show();
                        loadAccounts(); // Refresh the list
                        dialog.dismiss();
                    } else if (rowsAffected == -2) {
                        etUsername.setError("Tên người dùng này đã được sử dụng bởi tài khoản khác (DB check).");
                    }
                    else {
                        Toast.makeText(AdminDashboardActivity.this, "Cập nhật tài khoản thất bại hoặc không có thay đổi.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });
        dialog.show();
    }


    // Method from OnAccountActionListener (to be implemented in AccountAdapter)
    @Override
    public void onEditAccount(Account account) {
        showAccountDialog(account);
    }

    // Method from OnAccountActionListener (to be implemented in AccountAdapter)
    @Override
    public void onDeleteAccount(Account account) {
        if (account.getId() == loggedInAdminId) {
            Toast.makeText(this, "Bạn không thể xóa tài khoản admin đang đăng nhập.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Optional: Check if this is the last admin account
        // if (account.getRole().equalsIgnoreCase("admin")) {
        // List<Account> allAdmins = dbHelper.getAllAdmins(); // You'd need a method in DB helper
        // if (allAdmins.size() <= 1) {
        // Toast.makeText(this, "Không thể xóa tài khoản admin cuối cùng.", Toast.LENGTH_LONG).show();
        // return;
        // }
        // }

        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa người dùng '" + account.getUsername() + "'?\nCác chi tiêu liên quan cũng sẽ bị xóa.")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    int rowsAffected = dbHelper.deleteAccount(account.getId());
                    if (rowsAffected > 0) {
                        Toast.makeText(AdminDashboardActivity.this, "Đã xóa người dùng.", Toast.LENGTH_SHORT).show();
                        loadAccounts(); // Refresh the list
                    } else {
                        Toast.makeText(AdminDashboardActivity.this, "Xóa người dùng thất bại.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .setIcon(android.R.drawable.ic_dialog_alert) // Changed icon
                .show();
    }

    private void logoutAndGoToLogin() {
        SharedPreferences sharedPref = getSharedPreferences(LoginActivity.PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.remove(LoginActivity.KEY_ACCOUNT_ID);
        editor.remove(LoginActivity.KEY_ACCOUNT_ROLE);
        editor.apply();

        Intent intent = new Intent(AdminDashboardActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
        Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
    }
}
