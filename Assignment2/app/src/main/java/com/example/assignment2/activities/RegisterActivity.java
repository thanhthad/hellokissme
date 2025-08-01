package com.example.assignment2.activities; // TÊN GÓI MỚI

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.assignment2.R; // Import R
import com.example.assignment2.database.DatabaseHelper; // Import từ package database
// Không cần import Account model trực tiếp ở đây nếu chỉ đăng ký user thông thường
// import com.example.assignment2.models.Account; // Import từ package models

public class RegisterActivity extends AppCompatActivity {

    private EditText etUsername, etPassword, etConfirmPassword;
    private Button btnRegister;
    private TextView tvLoginLink;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etUsername = findViewById(R.id.etRegUsername);
        etPassword = findViewById(R.id.etRegPassword);
        etConfirmPassword = findViewById(R.id.etRegConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvLoginLink = findViewById(R.id.tvLoginLink);

        dbHelper = new DatabaseHelper(this);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = etUsername.getText().toString().trim();
                String password = etPassword.getText().toString().trim();
                String confirmPassword = etConfirmPassword.getText().toString().trim();

                if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                    Toast.makeText(RegisterActivity.this, "Vui lòng điền đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!password.equals(confirmPassword)) {
                    Toast.makeText(RegisterActivity.this, "Mật khẩu không khớp!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Không cần kiểm tra isUsernameExists ở đây nữa nếu DatabaseHelper đã xử lý
                // if (dbHelper.isUsernameExists(username)) {
                //     Toast.makeText(RegisterActivity.this, "Tên đăng nhập đã tồn tại!", Toast.LENGTH_SHORT).show();
                // } else {

                // <<< THAY ĐỔI: Sử dụng phương thức registerUserAccount hoặc registerAccount phù hợp
                // Giả sử chúng ta đăng ký tất cả tài khoản từ màn hình này là 'user'
                long id = dbHelper.registerUserAccount(username, password);
                // Hoặc nếu bạn muốn truyền role một cách tường minh qua đối tượng Account:
                // Account newUser = new Account(username, password, "user");
                // long id = dbHelper.registerAccount(newUser);


                if (id == -3) { // Mã lỗi cho username đã tồn tại từ DatabaseHelper
                    Toast.makeText(RegisterActivity.this, "Tên đăng nhập đã tồn tại!", Toast.LENGTH_SHORT).show();
                } else if (id != -1 && id != -2 ) { // -1 là lỗi chung, -2 là lỗi role (nếu dùng registerAccount)
                    Toast.makeText(RegisterActivity.this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(RegisterActivity.this, "Đăng ký thất bại. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                }
                // } // Kết thúc của else (nếu bạn giữ lại isUsernameExists ở đây)
            }
        });

        tvLoginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}
