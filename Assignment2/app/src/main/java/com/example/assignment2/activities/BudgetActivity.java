package com.example.assignment2.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
// import android.util.Log; // Bạn có thể bỏ nếu không dùng TAG
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.assignment2.R;
import com.example.assignment2.adapters.BudgetAdapter;
import com.example.assignment2.database.DatabaseHelper;
import com.example.assignment2.models.Budget;
import com.example.assignment2.models.Category;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale; // Để định dạng số tiền

public class BudgetActivity extends AppCompatActivity implements BudgetAdapter.OnBudgetActionListener { // BƯỚC 1

    // private static final String TAG = "BudgetActivity"; // Bỏ nếu không dùng

    private Spinner spBudgetCategory;
    private EditText etBudgetAmount;
    private Button btnAddOrUpdateBudget;
    private ListView lvBudgets;

    private DatabaseHelper dbHelper;
    private BudgetAdapter budgetAdapter;
    private List<Budget> budgetList;
    private List<Category> categoryList;

    private int currentAccountId = -1;
    private Category selectedCategoryForBudget;
    // private Budget currentEditingBudget = null; // Tùy chọn: để theo dõi budget đang sửa

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget);

        Toolbar toolbar = findViewById(R.id.toolbar_budget);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Quản Lý Ngân Sách");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        SharedPreferences sharedPref = getSharedPreferences(LoginActivity.PREF_NAME, Context.MODE_PRIVATE);
        currentAccountId = sharedPref.getInt(LoginActivity.KEY_ACCOUNT_ID, -1);

        if (currentAccountId == -1) {
            Toast.makeText(this, "Lỗi: Không tìm thấy thông tin tài khoản.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        dbHelper = new DatabaseHelper(this);

        spBudgetCategory = findViewById(R.id.spBudgetCategory);
        etBudgetAmount = findViewById(R.id.etBudgetAmount);
        btnAddOrUpdateBudget = findViewById(R.id.btnAddOrUpdateBudget);
        lvBudgets = findViewById(R.id.lvBudgets);

        budgetList = new ArrayList<>();
        categoryList = new ArrayList<>();

        loadCategoriesIntoSpinner();
        // Khởi tạo adapter ở đây, sau đó loadBudgetsIntoListView sẽ cập nhật nó
        // Hoặc khởi tạo trong loadBudgetsIntoListView nếu budgetAdapter là null
        budgetAdapter = new BudgetAdapter(this, budgetList, this); // BƯỚC 2: Khởi tạo với listener
        lvBudgets.setAdapter(budgetAdapter);

        loadBudgetsIntoListView(); // Tải và cập nhật dữ liệu cho adapter


        spBudgetCategory.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                selectedCategoryForBudget = (Category) parent.getItemAtPosition(position);
                if (selectedCategoryForBudget != null) {
                    Budget existingBudget = dbHelper.getBudgetForCategory(currentAccountId, selectedCategoryForBudget.getId());
                    if (existingBudget != null) {
                        // Định dạng số tiền để hiển thị (loại bỏ .0 nếu là số nguyên)
                        etBudgetAmount.setText(String.format(Locale.getDefault(), "%.0f", existingBudget.getAmount()));
                    } else {
                        etBudgetAmount.setText("");
                    }
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                selectedCategoryForBudget = null;
                etBudgetAmount.setText("");
            }
        });

        btnAddOrUpdateBudget.setOnClickListener(v -> addOrUpdateBudget());

        // BƯỚC 3: Cân nhắc loại bỏ hoặc điều chỉnh các Listener này
        // OnItemClickListener có thể vẫn hữu ích để chọn item và điền vào form
        // nhưng OnItemLongClickListener để xóa thì không cần nữa.
        /*
        lvBudgets.setOnItemClickListener((parent, view, position, id) -> {
            Budget selectedBudget = budgetList.get(position); // Hoặc budgetAdapter.getItem(position)
            if (selectedBudget != null) {
                onEditBudget(selectedBudget, position); // Gọi trực tiếp onEditBudget để điền form
                Toast.makeText(this, "Đã chọn: " + selectedBudget.getCategoryName(), Toast.LENGTH_SHORT).show();
            }
        });

        lvBudgets.setOnItemLongClickListener((parent, view, position, id) -> {
            // Đã có nút xóa riêng, không cần thiết ở đây nữa
            return true;
        });
        */
    }

    private void loadCategoriesIntoSpinner() {
        categoryList = dbHelper.getCategoriesForAccount(currentAccountId);
        if (categoryList == null) { // Thêm kiểm tra null
            categoryList = new ArrayList<>();
        }
        if (categoryList.isEmpty()) {
            Toast.makeText(this, "Không có danh mục nào để đặt ngân sách.", Toast.LENGTH_SHORT).show();
        }

        ArrayAdapter<Category> categoryAdapterSpinner = new ArrayAdapter<>(this, // Đổi tên biến để tránh trùng
                android.R.layout.simple_spinner_item, categoryList);
        categoryAdapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spBudgetCategory.setAdapter(categoryAdapterSpinner);

        if (!categoryList.isEmpty()) {
            spBudgetCategory.setSelection(0); // Đảm bảo item đầu tiên được chọn
            selectedCategoryForBudget = categoryList.get(0);
            Budget existingBudget = dbHelper.getBudgetForCategory(currentAccountId, selectedCategoryForBudget.getId());
            if (existingBudget != null) {
                etBudgetAmount.setText(String.format(Locale.getDefault(), "%.0f", existingBudget.getAmount()));
            } else {
                etBudgetAmount.setText("");
            }
        } else {
            selectedCategoryForBudget = null;
            etBudgetAmount.setText("");
            // Có thể vô hiệu hóa EditText và Button nếu không có category
            etBudgetAmount.setEnabled(false);
            btnAddOrUpdateBudget.setEnabled(false);
        }
    }

    private void loadBudgetsIntoListView() {
        budgetList = dbHelper.getAllBudgetsForAccount(currentAccountId);
        if (budgetList == null) { // Thêm kiểm tra null
            budgetList = new ArrayList<>();
        }

        if (budgetAdapter == null) {
            budgetAdapter = new BudgetAdapter(this, budgetList, this); // BƯỚC 2 (Lặp lại để chắc chắn)
            lvBudgets.setAdapter(budgetAdapter);
        } else {
            // BƯỚC 5: Cập nhật adapter
            budgetAdapter.updateData(budgetList); // Giả sử bạn có phương thức này trong adapter
            // Hoặc cách cũ:
            // budgetAdapter.clear();
            // budgetAdapter.addAll(budgetList);
            // budgetAdapter.notifyDataSetChanged();
        }
    }

    private void addOrUpdateBudget() {
        if (selectedCategoryForBudget == null) {
            Toast.makeText(this, "Vui lòng chọn một danh mục.", Toast.LENGTH_SHORT).show();
            return;
        }

        String amountStr = etBudgetAmount.getText().toString().trim();
        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập số tiền ngân sách.", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                Toast.makeText(this, "Số tiền ngân sách phải lớn hơn 0.", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Số tiền không hợp lệ. Vui lòng kiểm tra lại.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tạo đối tượng Budget mới để lưu hoặc cập nhật
        // ID của budget sẽ được xử lý bởi addOrUpdateBudget trong DatabaseHelper
        Budget budgetToSave = new Budget(currentAccountId, selectedCategoryForBudget.getId(), selectedCategoryForBudget.getName(), amount);        // Nếu bạn muốn giữ lại ID của budget đang sửa (ví dụ, từ currentEditingBudget), bạn có thể gán nó ở đây.
        // Tuy nhiên, logic addOrUpdateBudget thường dựa vào accountId và categoryId để tìm và cập nhật.

        long result = dbHelper.addOrUpdateBudget(budgetToSave);

        if (result != -1) { // -1 thường là lỗi
            Toast.makeText(this, "Đã lưu ngân sách cho: " + selectedCategoryForBudget.getName(), Toast.LENGTH_SHORT).show();
            loadBudgetsIntoListView(); // Tải lại danh sách
            // Xóa trường nhập sau khi lưu thành công
            // etBudgetAmount.setText("");
            // Tùy chọn: Đặt lại spinner về mục đầu tiên hoặc giữ nguyên
            // if (!categoryList.isEmpty()) {
            //     spBudgetCategory.setSelection(0);
            // }
            // currentEditingBudget = null; // Reset nếu bạn dùng biến này
        } else {
            Toast.makeText(this, "Lỗi khi lưu ngân sách.", Toast.LENGTH_SHORT).show();
        }
    }

    // Đổi tên phương thức này để tránh xung đột nếu tên phương thức trong interface là "deleteBudget"
    private void performDeleteBudget(Budget budget) {
        int rowsAffected = dbHelper.deleteBudget(budget.getId(), currentAccountId); // Giả sử deleteBudget cần ID của budget
        if (rowsAffected > 0) {
            Toast.makeText(this, "Đã xóa ngân sách cho: " + budget.getCategoryName(), Toast.LENGTH_SHORT).show();
            loadBudgetsIntoListView();
            // Nếu category của budget vừa xóa đang được chọn trên Spinner, hãy clear etBudgetAmount
            if (selectedCategoryForBudget != null && selectedCategoryForBudget.getId() == budget.getCategoryId()) {
                etBudgetAmount.setText("");
                // Có thể bạn muốn đặt lại selectedCategoryForBudget nếu category đó không còn ngân sách
                // Hoặc chọn lại item đầu tiên của spinner
                // if (!categoryList.isEmpty()) {
                //    spBudgetCategory.setSelection(0);
                // }
            }
        } else {
            Toast.makeText(this, "Lỗi khi xóa ngân sách.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setSpinnerToCategoryById(int categoryId) {
        if (categoryList != null) {
            for (int i = 0; i < categoryList.size(); i++) {
                if (categoryList.get(i).getId() == categoryId) {
                    spBudgetCategory.setSelection(i);
                    // selectedCategoryForBudget đã được cập nhật trong onItemSelected của Spinner
                    break;
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // BƯỚC 4: Triển khai các phương thức của OnBudgetActionListener
    @Override
    public void onEditBudget(Budget budget, int position) {
        // currentEditingBudget = budget; // Tùy chọn: lưu lại budget đang sửa
        Toast.makeText(this, "Chỉnh sửa: " + budget.getCategoryName(), Toast.LENGTH_SHORT).show();
        setSpinnerToCategoryById(budget.getCategoryId());
        // etBudgetAmount sẽ tự động được cập nhật bởi onItemSelected của Spinner
        // nhưng để chắc chắn, bạn có thể set lại ở đây:
        etBudgetAmount.setText(String.format(Locale.getDefault(), "%.0f", budget.getAmount()));
        etBudgetAmount.requestFocus(); // Đặt focus vào trường số tiền
        // btnAddOrUpdateBudget.setText("Cập Nhật Ngân Sách"); // Tùy chọn thay đổi text của button
    }

    @Override
    public void onDeleteBudget(Budget budget, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa Ngân Sách")
                .setMessage("Bạn có chắc muốn xóa ngân sách cho danh mục '" + budget.getCategoryName() + "' không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    performDeleteBudget(budget); // Gọi phương thức xóa đã được đổi tên
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}
