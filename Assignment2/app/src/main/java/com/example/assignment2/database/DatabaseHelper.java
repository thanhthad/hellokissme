package com.example.assignment2.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils; // Import TextUtils
import android.util.Log;

import com.example.assignment2.models.Account;
import com.example.assignment2.models.Budget;
import com.example.assignment2.models.Category; // Import Category model
import com.example.assignment2.models.Expense;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 8; // << INCREMENTED VERSION
    private static final String DATABASE_NAME = "campusExpenseManagerDB";
    private static final String TAG = "DatabaseHelper";

    // Table Expenses
    private static final String TABLE_EXPENSES = "expenses";
    private static final String KEY_EXPENSE_ID = "id";
    private static final String KEY_EXPENSE_DESCRIPTION = "description";
    private static final String KEY_EXPENSE_AMOUNT = "amount";
    private static final String KEY_EXPENSE_CATEGORY = "category"; // This will store the category NAME
    private static final String KEY_EXPENSE_DATE = "date";
    private static final String KEY_EXPENSE_ACCOUNT_ID = "account_id";

    // Table Accounts
    private static final String TABLE_ACCOUNTS = "accounts";
    private static final String KEY_ACCOUNT_ID = "id";
    private static final String KEY_ACCOUNT_USERNAME = "username";
    private static final String KEY_ACCOUNT_PASSWORD = "password";
    private static final String KEY_ACCOUNT_ROLE = "role";

    // Table Categories
    private static final String TABLE_CATEGORIES = "categories";
    private static final String KEY_CATEGORY_ID = "id";
    private static final String KEY_CATEGORY_NAME = "name";
    private static final String KEY_CATEGORY_ACCOUNT_ID = "account_id"; // FK to accounts, NULL for global

    //Table Budgets
    private static final String TABLE_BUDGETS = "budgets";
    private static final String KEY_BUDGET_ID = "id";
    private static final String KEY_BUDGET_ACCOUNT_ID = "account_id";   // FK to accounts
    private static final String KEY_BUDGET_CATEGORY_ID = "category_id"; // FK to categories
    private static final String KEY_BUDGET_CATEGORY_NAME = "category_name"; // Lưu tên category để dễ truy vấn
    private static final String KEY_BUDGET_AMOUNT = "amount";
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create accounts table (must be created before tables that reference it)
        String CREATE_ACCOUNTS_TABLE = "CREATE TABLE " + TABLE_ACCOUNTS + "("
                + KEY_ACCOUNT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_ACCOUNT_USERNAME + " TEXT UNIQUE NOT NULL,"
                + KEY_ACCOUNT_PASSWORD + " TEXT NOT NULL,"
                + KEY_ACCOUNT_ROLE + " TEXT CHECK(" + KEY_ACCOUNT_ROLE + " IN ('admin', 'user')) NOT NULL DEFAULT 'user'"
                + ")";
        db.execSQL(CREATE_ACCOUNTS_TABLE);
        Log.d(TAG, "Accounts table created.");

        // Create expenses table with foreign key
        String CREATE_EXPENSES_TABLE = "CREATE TABLE " + TABLE_EXPENSES + "("
                + KEY_EXPENSE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_EXPENSE_DESCRIPTION + " TEXT,"
                + KEY_EXPENSE_AMOUNT + " REAL,"
                + KEY_EXPENSE_CATEGORY + " TEXT," // Storing category name
                + KEY_EXPENSE_DATE + " TEXT,"
                + KEY_EXPENSE_ACCOUNT_ID + " INTEGER,"
                + " FOREIGN KEY (" + KEY_EXPENSE_ACCOUNT_ID + ") REFERENCES " + TABLE_ACCOUNTS + "(" + KEY_ACCOUNT_ID + ")"
                + " ON DELETE CASCADE"
                + ")";
        db.execSQL(CREATE_EXPENSES_TABLE);
        Log.d(TAG, "Expenses table created.");

        // Create categories table
        String CREATE_CATEGORIES_TABLE = "CREATE TABLE " + TABLE_CATEGORIES + "("
                + KEY_CATEGORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_CATEGORY_NAME + " TEXT NOT NULL,"
                + KEY_CATEGORY_ACCOUNT_ID + " INTEGER," // Can be NULL for default categories
                + "UNIQUE(" + KEY_CATEGORY_NAME + ", " + KEY_CATEGORY_ACCOUNT_ID + ")," // Category name should be unique per account (or globally if account_id is null)
                + " FOREIGN KEY (" + KEY_CATEGORY_ACCOUNT_ID + ") REFERENCES " + TABLE_ACCOUNTS + "(" + KEY_ACCOUNT_ID + ")"
                + " ON DELETE CASCADE" // If an account is deleted, their custom categories are also deleted
                + ")";
        db.execSQL(CREATE_CATEGORIES_TABLE);
        Log.d(TAG, "Categories table created.");

        // Create budgets table
        String CREATE_BUDGETS_TABLE_SQL = "CREATE TABLE " + TABLE_BUDGETS + "("
                + KEY_BUDGET_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_BUDGET_ACCOUNT_ID + " INTEGER NOT NULL," // This is "account_id"
                + KEY_BUDGET_CATEGORY_ID + " INTEGER NOT NULL," // This is "category_id"
                + KEY_BUDGET_CATEGORY_NAME + " TEXT,"
                + KEY_BUDGET_AMOUNT + " REAL NOT NULL,"
                // CORRECTED FOREIGN KEYS:
                + "FOREIGN KEY(" + KEY_BUDGET_ACCOUNT_ID + ") REFERENCES " + TABLE_ACCOUNTS + "(" + KEY_ACCOUNT_ID + ") ON DELETE CASCADE," // Referencing "id" in accounts
                + "FOREIGN KEY(" + KEY_BUDGET_CATEGORY_ID + ") REFERENCES " + TABLE_CATEGORIES + "(" + KEY_CATEGORY_ID + ") ON DELETE CASCADE," // Referencing "id" in categories
                + "UNIQUE (" + KEY_BUDGET_ACCOUNT_ID + ", " + KEY_BUDGET_CATEGORY_ID + ")" // Ensures one budget per account-category pair
                + ")";
        db.execSQL(CREATE_BUDGETS_TABLE_SQL);
        Log.d(TAG, "Budgets table created.");


        // Add default admin account
        ContentValues adminValues = new ContentValues();
        adminValues.put(KEY_ACCOUNT_USERNAME, "admin");
        adminValues.put(KEY_ACCOUNT_PASSWORD, "123"); // STORE HASHED PASSWORD IN PRODUCTION
        adminValues.put(KEY_ACCOUNT_ROLE, "admin");
        db.insertWithOnConflict(TABLE_ACCOUNTS, null, adminValues, SQLiteDatabase.CONFLICT_IGNORE);
        Log.d(TAG, "Default admin account potentially inserted.");

        // Add default global categories
        addDefaultCategoryIfNotExists(db, "Ăn uống");
        addDefaultCategoryIfNotExists(db, "Di chuyển");
        addDefaultCategoryIfNotExists(db, "Mua sắm");
        addDefaultCategoryIfNotExists(db, "Hóa đơn");
        addDefaultCategoryIfNotExists(db, "Giải trí");
        addDefaultCategoryIfNotExists(db, "Sức khỏe");
        addDefaultCategoryIfNotExists(db, "Giáo dục");
        addDefaultCategoryIfNotExists(db, "Khác");
        Log.d(TAG, "Default global categories potentially inserted.");
    }

    private void addDefaultCategoryIfNotExists(SQLiteDatabase db, String categoryName) {
        ContentValues values = new ContentValues();
        values.put(KEY_CATEGORY_NAME, categoryName);
        // KEY_CATEGORY_ACCOUNT_ID is omitted (defaults to NULL) for global categories
        db.insertWithOnConflict(TABLE_CATEGORIES, null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EXPENSES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CATEGORIES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ACCOUNTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BUDGETS); // << ENSURE THIS IS HERE
        onCreate(db); // This will now call your corrected onCreate
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    // --- EXPENSE CRUD Operations ---
    // Trong DatabaseHelper.java

    public long addExpense(Expense expense, int accountId) {
        // Lấy Category object từ tên category trong Expense object
        Category category = getCategoryByName(expense.getCategory(), accountId);
        if (category == null) {
            Log.e(TAG, "Category '" + expense.getCategory() + "' not found for account " + accountId);
            return -4; // Category not found (mã lỗi mới cho category không tìm thấy)
        }
        int categoryId = category.getId();

        Budget budget = getBudgetForCategory(accountId, categoryId);

        if (budget != null) {
            // Lấy tháng và năm của chi phí để so sánh đúng kỳ ngân sách (nếu ngân sách theo tháng/năm)
            // Nếu ngân sách của bạn không theo kỳ cụ thể mà là tổng thể cho category, bạn có thể bỏ qua phần ngày tháng này.
            // Tuy nhiên, ví dụ dưới đây giả sử bạn muốn kiểm tra cho tháng hiện tại của chi phí.
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            java.text.SimpleDateFormat monthYearFormat = new java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault());
            String expenseDateStr = expense.getDate(); // Giả sử định dạng YYYY-MM-DD
            String expenseMonthYear;
            try {
                java.util.Date parsedDate = sdf.parse(expenseDateStr);
                expenseMonthYear = monthYearFormat.format(parsedDate); // Format thành "YYYY-MM"
            } catch (java.text.ParseException e) {
                Log.e(TAG, "Invalid expense date format for budget check: " + expenseDateStr);
                return -1; // Hoặc một mã lỗi khác cho ngày không hợp lệ
            }

            double currentExpensesInPeriod = getTotalExpensesForCategory(accountId, expense.getCategory(), expenseMonthYear);

            if (currentExpensesInPeriod + expense.getAmount() > budget.getAmount()) {
                Log.w(TAG, "Expense exceeds budget for category: " + expense.getCategory() +
                        ". Budget: " + budget.getAmount() + ", Current Spent: " + currentExpensesInPeriod + ", New Expense: " + expense.getAmount());
                return -2; // **QUAN TRỌNG: Budget exceeded, KHÔNG thực hiện insert**
            }
        } else {
            // Không có budget nào được đặt cho category này.
            // Nếu bạn muốn BẮT BUỘC phải có budget, hãy trả về lỗi ở đây.
            // Ví dụ:
            // Log.w(TAG, "No budget set for category: " + expense.getCategory() + ". Expense not allowed.");
            // return -3; // Mã lỗi cho việc không có budget (nếu bạn muốn chặn)
            Log.d(TAG, "No budget set for category: " + expense.getCategory() + ". Allowing expense.");
        }

        // Nếu không vượt budget (hoặc không có budget và bạn cho phép), thì tiến hành thêm chi phí
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_EXPENSE_DESCRIPTION, expense.getDescription());
        values.put(KEY_EXPENSE_AMOUNT, expense.getAmount());
        values.put(KEY_EXPENSE_CATEGORY, expense.getCategory()); // Lưu tên category
        values.put(KEY_EXPENSE_DATE, expense.getDate());
        values.put(KEY_EXPENSE_ACCOUNT_ID, accountId); // Lưu account_id

        long id = -1;
        try {
            id = db.insertOrThrow(TABLE_EXPENSES, null, values); //Sử dụng insertOrThrow để nhận Exception nếu có lỗi
            if (id > 0) {
                Log.d(TAG, "Expense added successfully with ID: " + id);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error adding expense after budget check: " + e.getMessage());
            id = -1; // Đảm bảo trả về -1 nếu có lỗi insert
        } finally {
            db.close();
        }
        return id; // Trả về ID của expense mới hoặc mã lỗi (-1, -2, -3, -4)
    }

    public List<Expense> getAllExpensesForAccount(int accountId) {
        List<Expense> expenseList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_EXPENSES +
                " WHERE " + KEY_EXPENSE_ACCOUNT_ID + " = ? ORDER BY " + KEY_EXPENSE_DATE + " DESC, " + KEY_EXPENSE_ID + " DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(selectQuery, new String[]{String.valueOf(accountId)});
            if (cursor.moveToFirst()) {
                do {
                    Expense expense = new Expense();
                    expense.setId(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_EXPENSE_ID)));
                    expense.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(KEY_EXPENSE_DESCRIPTION)));
                    expense.setAmount(cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_EXPENSE_AMOUNT)));
                    expense.setCategory(cursor.getString(cursor.getColumnIndexOrThrow(KEY_EXPENSE_CATEGORY)));
                    expense.setDate(cursor.getString(cursor.getColumnIndexOrThrow(KEY_EXPENSE_DATE)));
                    expenseList.add(expense);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting all expenses for account: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return expenseList;
    }

    public Expense getExpense(int expenseId, int accountId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        Expense expense = null;
        try {
            cursor = db.query(TABLE_EXPENSES, new String[]{KEY_EXPENSE_ID,
                            KEY_EXPENSE_DESCRIPTION, KEY_EXPENSE_AMOUNT, KEY_EXPENSE_CATEGORY, KEY_EXPENSE_DATE},
                    KEY_EXPENSE_ID + "=? AND " + KEY_EXPENSE_ACCOUNT_ID + "=?",
                    new String[]{String.valueOf(expenseId), String.valueOf(accountId)}, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                expense = new Expense(
                        cursor.getInt(cursor.getColumnIndexOrThrow(KEY_EXPENSE_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_EXPENSE_DESCRIPTION)),
                        cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_EXPENSE_AMOUNT)),
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_EXPENSE_CATEGORY)),
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_EXPENSE_DATE)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(KEY_EXPENSE_ACCOUNT_ID))
                );
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting expense: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return expense;
    }

    public int updateExpense(Expense expense, int accountId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_EXPENSE_DESCRIPTION, expense.getDescription());
        values.put(KEY_EXPENSE_AMOUNT, expense.getAmount());
        values.put(KEY_EXPENSE_CATEGORY, expense.getCategory());
        values.put(KEY_EXPENSE_DATE, expense.getDate());
        int rowsAffected = 0;
        try {
            rowsAffected = db.update(TABLE_EXPENSES, values,
                    KEY_EXPENSE_ID + " = ? AND " + KEY_EXPENSE_ACCOUNT_ID + " = ?",
                    new String[]{String.valueOf(expense.getId()), String.valueOf(accountId)});
        } catch (Exception e) {
            Log.e(TAG, "Error updating expense: " + e.getMessage());
        } finally {
            db.close();
        }
        return rowsAffected;
    }

    public int deleteExpense(int expenseId, int accountId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsAffected = 0;
        try {
            rowsAffected = db.delete(TABLE_EXPENSES,
                    KEY_EXPENSE_ID + " = ? AND " + KEY_EXPENSE_ACCOUNT_ID + " = ?",
                    new String[]{String.valueOf(expenseId), String.valueOf(accountId)});
        } catch (Exception e) {
            Log.e(TAG, "Error deleting expense: " + e.getMessage());
        } finally {
            db.close();
        }
        return rowsAffected;
    }

    // --- ACCOUNT CRUD Operations ---
    public long addAccount(Account account) {
        if (checkUsernameExists(account.getUsername())) {
            Log.w(TAG, "Username '" + account.getUsername() + "' already exists. Cannot add account.");
            return -3; // Username exists
        }
        if (account.getRole() == null || (!account.getRole().equals("user") && !account.getRole().equals("admin"))) {
            Log.w(TAG, "Invalid role for account: " + account.getRole());
            return -2; // Invalid role
        }

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_ACCOUNT_USERNAME, account.getUsername());
        values.put(KEY_ACCOUNT_PASSWORD, account.getPassword()); // HASH IN ACTIVITY/SERVICE
        values.put(KEY_ACCOUNT_ROLE, account.getRole());

        long id = -1;
        try {
            id = db.insertOrThrow(TABLE_ACCOUNTS, null, values);
            Log.d(TAG, "Account added with ID: " + id + ", Username: " + account.getUsername());
        } catch (Exception e) {
            Log.e(TAG, "Error adding account: " + e.getMessage());
        } finally {
            db.close();
        }
        return id;
    }
    public Account getAccountByCredentials(String username, String rawPassword) {
        SQLiteDatabase db = this.getReadableDatabase();
        Account account = null;
        Cursor cursor = null;
        // In a real app, you would HASH rawPassword here and compare with DB hash.
        try {
            Log.d(TAG, "Attempting login for username: " + username);
            cursor = db.query(TABLE_ACCOUNTS,
                    new String[]{KEY_ACCOUNT_ID, KEY_ACCOUNT_USERNAME, KEY_ACCOUNT_PASSWORD, KEY_ACCOUNT_ROLE},
                    KEY_ACCOUNT_USERNAME + "=?",
                    new String[]{username},
                    null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                String dbPassword = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ACCOUNT_PASSWORD));
                // THIS IS WHERE YOU'D COMPARE HASHED PASSWORDS.
                // For now, direct comparison (INSECURE for production).
                // Example: if (PasswordHasher.verifyPassword(rawPassword, dbPassword)) {
                if (dbPassword.equals(rawPassword)) { // Replace with secure hash comparison
                    Log.d(TAG, "Password match for: " + username);
                    account = new Account();
                    account.setId(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ACCOUNT_ID)));
                    account.setUsername(cursor.getString(cursor.getColumnIndexOrThrow(KEY_ACCOUNT_USERNAME)));
                    account.setRole(cursor.getString(cursor.getColumnIndexOrThrow(KEY_ACCOUNT_ROLE)));
                } else {
                    Log.w(TAG, "Password mismatch for: " + username);
                }
            } else {
                Log.d(TAG, "Account NOT found for username: " + username);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getAccountByCredentials: " + e.getMessage(), e);
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return account;
    }

    public Account getAccountById(int accountId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        Account account = null;
        try {
            cursor = db.query(TABLE_ACCOUNTS,
                    new String[]{KEY_ACCOUNT_ID, KEY_ACCOUNT_USERNAME, KEY_ACCOUNT_PASSWORD, KEY_ACCOUNT_ROLE},
                    KEY_ACCOUNT_ID + "=?",
                    new String[]{String.valueOf(accountId)}, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                account = new Account();
                account.setId(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ACCOUNT_ID)));
                account.setUsername(cursor.getString(cursor.getColumnIndexOrThrow(KEY_ACCOUNT_USERNAME)));
                account.setRole(cursor.getString(cursor.getColumnIndexOrThrow(KEY_ACCOUNT_ROLE)));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting account by ID: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return account;
    }

    public List<Account> getAllAccounts() {
        List<Account> accountList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        String selectQuery = "SELECT * FROM " + TABLE_ACCOUNTS + " ORDER BY " + KEY_ACCOUNT_USERNAME + " ASC";
        try {
            cursor = db.rawQuery(selectQuery, null);
            if (cursor.moveToFirst()) {
                do {
                    Account account = new Account();
                    account.setId(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ACCOUNT_ID)));
                    account.setUsername(cursor.getString(cursor.getColumnIndexOrThrow(KEY_ACCOUNT_USERNAME)));
                    account.setRole(cursor.getString(cursor.getColumnIndexOrThrow(KEY_ACCOUNT_ROLE)));
                    accountList.add(account);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting all accounts: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return accountList;
    }

    public int updateAccount(Account account) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_ACCOUNT_USERNAME, account.getUsername());
        if (account.getPassword() != null && !account.getPassword().isEmpty()) {
            values.put(KEY_ACCOUNT_PASSWORD, account.getPassword()); // HASH IN ACTIVITY/SERVICE
        }
        values.put(KEY_ACCOUNT_ROLE, account.getRole());

        int rowsAffected = 0;
        try {
            Cursor tempCursor = db.query(TABLE_ACCOUNTS, new String[]{KEY_ACCOUNT_ID},
                    KEY_ACCOUNT_USERNAME + " = ? AND " + KEY_ACCOUNT_ID + " != ?",
                    new String[]{account.getUsername(), String.valueOf(account.getId())},
                    null, null, null);
            if (tempCursor.getCount() > 0) {
                Log.w(TAG, "Update failed: Username '" + account.getUsername() + "' already exists.");
                tempCursor.close();
                db.close();
                return -2; // Username conflict
            }
            tempCursor.close();

            rowsAffected = db.update(TABLE_ACCOUNTS, values, KEY_ACCOUNT_ID + " = ?",
                    new String[]{String.valueOf(account.getId())});
        } catch (Exception e) {
            Log.e(TAG, "Error updating account: " + e.getMessage());
        } finally {
            db.close();
        }
        return rowsAffected;
    }

    public int deleteAccount(int accountId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsAffected = 0;
        try {
            rowsAffected = db.delete(TABLE_ACCOUNTS, KEY_ACCOUNT_ID + " = ?",
                    new String[]{String.valueOf(accountId)});
        } catch (Exception e) {
            Log.e(TAG, "Error deleting account: " + e.getMessage());
        } finally {
            db.close();
        }
        return rowsAffected;
    }

    public boolean checkUsernameExists(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        boolean exists = false;
        try {
            cursor = db.query(TABLE_ACCOUNTS, new String[]{KEY_ACCOUNT_ID},
                    KEY_ACCOUNT_USERNAME + "=?", new String[]{username},
                    null, null, null);
            exists = cursor.getCount() > 0;
        } catch (Exception e) {
            Log.e(TAG, "Error checking username: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return exists;
    }

    public String getAccountRole(int accountId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String role = null;
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_ACCOUNTS, new String[]{KEY_ACCOUNT_ROLE},
                    KEY_ACCOUNT_ID + "=?", new String[]{String.valueOf(accountId)},
                    null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                role = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ACCOUNT_ROLE));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting account role: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return role;
    }

    // --- CATEGORY CRUD Operations ---

    public long addCategory(String categoryName, int accountId) {
        if (TextUtils.isEmpty(categoryName)) {
            Log.e(TAG, "Category name cannot be empty.");
            return -1; // Invalid input
        }
        SQLiteDatabase db = this.getWritableDatabase();
        long id = -1;
        try {
            // Check if category already exists for this user
            Cursor cursor = db.query(TABLE_CATEGORIES, new String[]{KEY_CATEGORY_ID},
                    KEY_CATEGORY_NAME + " = ? AND " + KEY_CATEGORY_ACCOUNT_ID + " = ?",
                    new String[]{categoryName, String.valueOf(accountId)}, null, null, null);
            if (cursor.getCount() > 0) {
                cursor.close();
                Log.w(TAG, "Category '" + categoryName + "' already exists for account " + accountId);
                return -2; // Category already exists for this user
            }
            cursor.close();

            ContentValues values = new ContentValues();
            values.put(KEY_CATEGORY_NAME, categoryName);
            values.put(KEY_CATEGORY_ACCOUNT_ID, accountId); // User-specific category
            id = db.insertOrThrow(TABLE_CATEGORIES, null, values);
            Log.d(TAG, "Category added with ID: " + id + " ('" + categoryName + "') for account ID: " + accountId);
        } catch (Exception e) {
            Log.e(TAG, "Error adding category '" + categoryName + "': " + e.getMessage());
        } finally {
            db.close();
        }
        return id;
    }

    public List<Category> getCategoriesForAccount(int accountId) {
        List<Category> categoryList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        String selectQuery = "SELECT * FROM " + TABLE_CATEGORIES +
                " WHERE " + KEY_CATEGORY_ACCOUNT_ID + " = ? OR " + KEY_CATEGORY_ACCOUNT_ID + " IS NULL" +
                " ORDER BY CASE WHEN " + KEY_CATEGORY_ACCOUNT_ID + " IS NULL THEN 0 ELSE 1 END, " + KEY_CATEGORY_NAME + " ASC";
        try {
            cursor = db.rawQuery(selectQuery, new String[]{String.valueOf(accountId)});
            if (cursor.moveToFirst()) {
                do {
                    Category category = new Category();
                    category.setId(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_CATEGORY_ID)));
                    category.setName(cursor.getString(cursor.getColumnIndexOrThrow(KEY_CATEGORY_NAME)));
                    int accIdColumnIndex = cursor.getColumnIndex(KEY_CATEGORY_ACCOUNT_ID);
                    if (!cursor.isNull(accIdColumnIndex)) {
                        category.setAccountId(cursor.getInt(accIdColumnIndex));
                    } else {
                        category.setAccountId(null); // Global category
                    }
                    categoryList.add(category);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting categories for account " + accountId + ": " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return categoryList;
    }

    public int updateCategory(Category category) {
        if (category.getAccountId() == null) {
            Log.w(TAG, "Cannot update a global category using this method. ID: " + category.getId());
            return 0; // Or throw an exception
        }
        if (TextUtils.isEmpty(category.getName())) {
            Log.e(TAG, "Category name cannot be empty for update. ID: " + category.getId());
            return -1;
        }

        SQLiteDatabase db = this.getWritableDatabase();
        int rowsAffected = 0;
        try {
            // Check if new name conflicts with an existing category for the same user (excluding itself)
            Cursor cursor = db.query(TABLE_CATEGORIES, new String[]{KEY_CATEGORY_ID},
                    KEY_CATEGORY_NAME + " = ? AND " + KEY_CATEGORY_ACCOUNT_ID + " = ? AND " + KEY_CATEGORY_ID + " != ?",
                    new String[]{category.getName(), String.valueOf(category.getAccountId()), String.valueOf(category.getId())},
                    null, null, null);
            if (cursor.getCount() > 0) {
                cursor.close();
                Log.w(TAG, "Update failed: Category name '" + category.getName() + "' already exists for account " + category.getAccountId());
                return -2; // Conflict
            }
            cursor.close();

            ContentValues values = new ContentValues();
            values.put(KEY_CATEGORY_NAME, category.getName());
            rowsAffected = db.update(TABLE_CATEGORIES, values,
                    KEY_CATEGORY_ID + " = ? AND " + KEY_CATEGORY_ACCOUNT_ID + " = ?",
                    new String[]{String.valueOf(category.getId()), String.valueOf(category.getAccountId())});
            if (rowsAffected > 0) {
                Log.d(TAG, "Category updated successfully. ID: " + category.getId() + ", New Name: " + category.getName());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating category ID " + category.getId() + ": " + e.getMessage());
        } finally {
            db.close();
        }
        return rowsAffected;
    }

    public int deleteCategory(int categoryId, int accountId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsAffected = 0;
        String categoryNameForCheck = "";

        try {
            // First, ensure the category is user-owned and get its name
            Cursor ownerCursor = db.query(TABLE_CATEGORIES, new String[]{KEY_CATEGORY_ACCOUNT_ID, KEY_CATEGORY_NAME},
                    KEY_CATEGORY_ID + " = ?", new String[]{String.valueOf(categoryId)}, null, null, null);
            if (ownerCursor.moveToFirst()) {
                int catAccountIdColumnIndex = ownerCursor.getColumnIndex(KEY_CATEGORY_ACCOUNT_ID);
                if (ownerCursor.isNull(catAccountIdColumnIndex) || ownerCursor.getInt(catAccountIdColumnIndex) != accountId) {
                    ownerCursor.close();
                    Log.w(TAG, "Attempt to delete global or non-owned category. Category ID: " + categoryId + ", Requester Account: " + accountId);
                    db.close();
                    return 0; // Not allowed
                }
                categoryNameForCheck = ownerCursor.getString(ownerCursor.getColumnIndexOrThrow(KEY_CATEGORY_NAME));
            } else {
                ownerCursor.close();
                Log.w(TAG, "Category not found for deletion. ID: " + categoryId);
                db.close();
                return 0; // Not found
            }
            ownerCursor.close();

            // Check if the category (by name) is used in any expenses for THIS user
            if (!TextUtils.isEmpty(categoryNameForCheck)) {
                Cursor usageCursor = db.query(TABLE_EXPENSES, new String[]{KEY_EXPENSE_ID},
                        KEY_EXPENSE_CATEGORY + " = ? AND " + KEY_EXPENSE_ACCOUNT_ID + " = ?",
                        new String[]{categoryNameForCheck, String.valueOf(accountId)}, null, null, null);
                if (usageCursor.getCount() > 0) {
                    usageCursor.close();
                    Log.w(TAG, "Category '" + categoryNameForCheck + "' (ID: " + categoryId + ") is in use by expenses for account " + accountId + " and cannot be deleted.");
                    db.close();
                    return -2; // Category in use
                }
                usageCursor.close();
            }

            // Proceed with deletion if not in use and owned by user
            rowsAffected = db.delete(TABLE_CATEGORIES,
                    KEY_CATEGORY_ID + " = ? AND " + KEY_CATEGORY_ACCOUNT_ID + " = ?",
                    new String[]{String.valueOf(categoryId), String.valueOf(accountId)});
            if (rowsAffected > 0) {
                Log.d(TAG, "Category deleted successfully. ID: " + categoryId + " for account " + accountId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleting category ID " + categoryId + ": " + e.getMessage());
        } finally {
            db.close();
        }
        return rowsAffected;
    }

    // Inside DatabaseHelper.java
    public long registerUserAccount(String username, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_ACCOUNT_USERNAME, username); // Replace COLUMN_USERNAME with your actual column name
        values.put(KEY_ACCOUNT_PASSWORD, password); // Replace COLUMN_PASSWORD with your actual column name
        values.put(KEY_ACCOUNT_ROLE, "user");     // Assuming you have a role column and default to "user"

        // Check if username already exists
        if (isUsernameExists(username)) {
            return -3; // Or some other specific code for existing username
        }

        long id = db.insert(TABLE_ACCOUNTS, null, values); // Replace TABLE_ACCOUNTS with your table name
        db.close();
        return id; // Returns the row ID of the newly inserted row, or -1 if an error occurred
    }

    // You'd also need the isUsernameExists method, or similar logic
    public boolean isUsernameExists(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_ACCOUNTS + " WHERE " + KEY_ACCOUNT_USERNAME + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{username});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        // db.close(); // Don't close readable database here if it's used elsewhere frequently
        return exists;
    }
    // --- CATEGORY HELPER ---
    public Category getCategoryByName(String categoryName, int accountId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        Category category = null;
        // Ưu tiên tìm category của user, sau đó là global category
        String query = "SELECT * FROM " + TABLE_CATEGORIES + " WHERE " + KEY_CATEGORY_NAME + " = ? " +
                "AND (" + KEY_CATEGORY_ACCOUNT_ID + " = ? OR " + KEY_CATEGORY_ACCOUNT_ID + " IS NULL) " +
                "ORDER BY " + KEY_CATEGORY_ACCOUNT_ID + " DESC"; // User-specific first (NOT NULL DESC is > NULL)

        try {
            cursor = db.rawQuery(query, new String[]{categoryName, String.valueOf(accountId)});
            if (cursor != null && cursor.moveToFirst()) {
                // Nếu có nhiều kết quả (ví dụ user tạo category trùng tên global), ưu tiên của user
                // Do ORDER BY, dòng đầu tiên sẽ là của user nếu tồn tại
                category = new Category();
                category.setId(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_CATEGORY_ID)));
                category.setName(cursor.getString(cursor.getColumnIndexOrThrow(KEY_CATEGORY_NAME)));
                int accIdColIdx = cursor.getColumnIndex(KEY_CATEGORY_ACCOUNT_ID);
                if (!cursor.isNull(accIdColIdx)) {
                    category.setAccountId(cursor.getInt(accIdColIdx));
                } else {
                    category.setAccountId(null);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting category by name '" + categoryName + "': " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return category;
    }
    public long addOrUpdateBudget(Budget budget) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_BUDGET_ACCOUNT_ID, budget.getAccountId());
        values.put(KEY_BUDGET_CATEGORY_ID, budget.getCategoryId());
        values.put(KEY_BUDGET_CATEGORY_NAME, budget.getCategoryName()); // Make sure this is always set in BudgetActivity
        values.put(KEY_BUDGET_AMOUNT, budget.getAmount());

        long id = -1;
        try {
            id = db.insertWithOnConflict(TABLE_BUDGETS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            if (id == -1) { // Explicitly check if insert failed
                Log.e(TAG, "Failed to insert/replace budget. Potential FK violation or other SQL error for category: " + budget.getCategoryName() + ", accountId: " + budget.getAccountId() + ", categoryId: " + budget.getCategoryId());
            } else {
                Log.d(TAG, "Budget added/updated with ID: " + id + " for category: " + budget.getCategoryName());
            }
        } catch (Exception e) {
            // This catch block will catch more severe errors, but FK violations
            // with CONFLICT_REPLACE might just return -1 without throwing an Exception here.
            Log.e(TAG, "Error adding/updating budget (Exception): " + e.getMessage() + " for category: " + budget.getCategoryName());
            id = -1; // Ensure id is -1 on exception
        } finally {
            db.close();
        }
        return id;
    }

    public Budget getBudgetForCategory(int accountId, int categoryId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        Budget budget = null;
        // String selection = KEY_BUDGET_ACCOUNT_ID + " = ? AND " + KEY_BUDGET_CATEGORY_ID + " = ? AND " +
        //                    KEY_BUDGET_MONTH + " = ? AND " + KEY_BUDGET_YEAR + " = ?"; // Nếu theo kỳ
        String selection = KEY_BUDGET_ACCOUNT_ID + " = ? AND " + KEY_BUDGET_CATEGORY_ID + " = ?";
        String[] selectionArgs = {String.valueOf(accountId), String.valueOf(categoryId)};
        // String[] selectionArgs = {String.valueOf(accountId), String.valueOf(categoryId), String.valueOf(month), String.valueOf(year)}; // Nếu theo kỳ

        try {
            cursor = db.query(TABLE_BUDGETS, null, selection, selectionArgs, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                budget = new Budget();
                budget.setId(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_BUDGET_ID)));
                budget.setAccountId(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_BUDGET_ACCOUNT_ID)));
                budget.setCategoryId(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_BUDGET_CATEGORY_ID)));
                budget.setCategoryName(cursor.getString(cursor.getColumnIndexOrThrow(KEY_BUDGET_CATEGORY_NAME)));
                budget.setAmount(cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_BUDGET_AMOUNT)));
                // budget.setMonth(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_BUDGET_MONTH))); // Nếu có
                // budget.setYear(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_BUDGET_YEAR)));   // Nếu có
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting budget for category: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return budget;
    }

    public List<Budget> getAllBudgetsForAccount(int accountId) {
        List<Budget> budgetList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        // String query = "SELECT b.*, c." + KEY_CATEGORY_NAME + " as category_actual_name FROM " + TABLE_BUDGETS + " b " +
        //                "INNER JOIN " + TABLE_CATEGORIES + " c ON b." + KEY_BUDGET_CATEGORY_ID + " = c." + KEY_CATEGORY_ID +
        //                " WHERE b." + KEY_BUDGET_ACCOUNT_ID + " = ? ORDER BY c." + KEY_CATEGORY_NAME + " ASC";
        // Nếu không join, mà dùng KEY_BUDGET_CATEGORY_NAME đã lưu:
        String query = "SELECT * FROM " + TABLE_BUDGETS +
                " WHERE " + KEY_BUDGET_ACCOUNT_ID + " = ? ORDER BY " + KEY_BUDGET_CATEGORY_NAME + " ASC";

        try {
            cursor = db.rawQuery(query, new String[]{String.valueOf(accountId)});
            if (cursor.moveToFirst()) {
                do {
                    Budget budget = new Budget();
                    budget.setId(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_BUDGET_ID)));
                    budget.setAccountId(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_BUDGET_ACCOUNT_ID)));
                    budget.setCategoryId(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_BUDGET_CATEGORY_ID)));
                    budget.setCategoryName(cursor.getString(cursor.getColumnIndexOrThrow(KEY_BUDGET_CATEGORY_NAME))); // Lấy từ cột đã lưu
                    // budget.setCategoryName(cursor.getString(cursor.getColumnIndexOrThrow("category_actual_name"))); // Nếu dùng JOIN
                    budget.setAmount(cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_BUDGET_AMOUNT)));
                    // budget.setMonth(...); // Nếu có
                    // budget.setYear(...);  // Nếu có
                    budgetList.add(budget);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting all budgets for account: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return budgetList;
    }

    public int deleteBudget(int budgetId, int accountId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsAffected = 0;
        try {
            rowsAffected = db.delete(TABLE_BUDGETS,
                    KEY_BUDGET_ID + " = ? AND " + KEY_BUDGET_ACCOUNT_ID + " = ?",
                    new String[]{String.valueOf(budgetId), String.valueOf(accountId)});
        } catch (Exception e) {
            Log.e(TAG, "Error deleting budget: " + e.getMessage());
        } finally {
            db.close();
        }
        return rowsAffected;
    }

    public double getTotalExpensesForCategory(int accountId, String categoryName, String currentMonthYear) {
        // currentMonthYear có dạng "YYYY-MM" hoặc bạn có thể truyền tháng và năm riêng
        // Ví dụ: Lấy tổng chi tiêu cho tháng hiện tại
        // String currentMonthYear = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        double total = 0;
        // Cần đảm bảo KEY_EXPENSE_DATE lưu trữ ngày tháng theo định dạng có thể so sánh được (ví dụ YYYY-MM-DD)
        // Và KEY_EXPENSE_CATEGORY lưu tên category khớp với categoryName
        String query = "SELECT SUM(" + KEY_EXPENSE_AMOUNT + ") FROM " + TABLE_EXPENSES +
                " WHERE " + KEY_EXPENSE_ACCOUNT_ID + " = ?" +
                " AND " + KEY_EXPENSE_CATEGORY + " = ?" +
                " AND strftime('%Y-%m', " + KEY_EXPENSE_DATE + ") = ?"; // Giả sử date lưu dạng 'YYYY-MM-DD'
        // Hoặc nếu bạn lưu date dạng timestamp:
        // " AND strftime('%Y-%m', " + KEY_EXPENSE_DATE + "/1000, 'unixepoch') = ?";

        try {
            cursor = db.rawQuery(query, new String[]{String.valueOf(accountId), categoryName, currentMonthYear});
            if (cursor != null && cursor.moveToFirst()) {
                total = cursor.getDouble(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting total expenses for category '" + categoryName + "': " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
            db.close(); // Quan trọng: đóng DB sau mỗi truy vấn đọc
        }
        return total;
    }
}
