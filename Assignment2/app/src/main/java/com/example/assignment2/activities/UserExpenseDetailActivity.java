package com.example.assignment2.activities; // Hoặc package tương ứng của bạn

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.assignment2.R;
import com.example.assignment2.database.DatabaseHelper;
// Import thêm các Adapter và Model cần thiết (ví dụ: ExpenseAdapter, ExpenseModel)

public class UserExpenseDetailActivity extends AppCompatActivity {

    private TextView tvUserNameDetail;
    private RecyclerView rvUserExpenses;
    // private ExpenseAdapter expenseAdapter; // Adapter để hiển thị danh sách chi tiêu
    // private List<ExpenseModel> expenseList; // Danh sách chi tiêu
    private DatabaseHelper dbHelper;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_expense_detail);

        tvUserNameDetail = findViewById(R.id.tvUserNameDetail);
        rvUserExpenses = findViewById(R.id.rvUserExpenses);

        dbHelper = new DatabaseHelper(this);

        // Lấy USER_ID từ Intent
        userId = getIntent().getIntExtra("USER_ID", -1);

        if (userId == -1) {
            // Xử lý trường hợp không có USER_ID
            tvUserNameDetail.setText("Lỗi: Không tìm thấy người dùng.");
            // Có thể finish() Activity này
            return;
        }

        // TODO: Lấy thông tin người dùng (ví dụ: tên) từ DatabaseHelper dựa vào userId
        // String userName = dbHelper.getUserNameById(userId); // Giả sử có phương thức này
        // tvUserNameDetail.setText("Chi tiết chi tiêu của: " + userName);

        // Thiết lập RecyclerView
        rvUserExpenses.setLayoutManager(new LinearLayoutManager(this));
        // expenseList = dbHelper.getExpensesByUserId(userId); // Giả sử có phương thức này
        // expenseAdapter = new ExpenseAdapter(this, expenseList);
        // rvUserExpenses.setAdapter(expenseAdapter);

        // TODO: Load danh sách chi tiêu của người dùng (dựa vào userId) từ DatabaseHelper
        // và hiển thị trong RecyclerView
    }
}
