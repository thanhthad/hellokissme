package com.example.assignment2.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.assignment2.R;
import com.example.assignment2.database.DatabaseHelper;
import com.example.assignment2.models.Account;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Button btnLogin;
    private TextView tvRegisterLink;
    private DatabaseHelper dbHelper; // Đối tượng DatabaseHelper để tương tác với SQLite

    public static final String PREF_NAME = "LoginPrefs";
    public static final String KEY_ACCOUNT_ID = "accountId";
    public static final String KEY_ACCOUNT_ROLE = "accountRole";
    public static final String KEY_IS_LOGGED_IN = "isLoggedIn";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.etLoginUsername);
        etPassword = findViewById(R.id.etLoginPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegisterLink = findViewById(R.id.tvRegisterLink);

        dbHelper = new DatabaseHelper(this); // Khởi tạo DatabaseHelper

        SharedPreferences sharedPref = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        int savedAccountId = sharedPref.getInt(KEY_ACCOUNT_ID, -1);
        String savedAccountRole = sharedPref.getString(KEY_ACCOUNT_ROLE, null);

        // Kiểm tra đăng nhập tự động dựa trên role
        if (savedAccountId != -1 && savedAccountRole != null) {
            redirectToDashboard(savedAccountRole);
            finish();
            return;
        }

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = etUsername.getText().toString().trim();
                String password = etPassword.getText().toString().trim();

                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Vui lòng nhập tên đăng nhập và mật khẩu!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // >>> Đây là nơi LoginActivity gọi getAccountByCredentials từ DatabaseHelper <<<
                // >>> DatabaseHelper sẽ truy vấn SQLite <<<
                Account account = dbHelper.getAccountByCredentials(username, password);

                if (account != null) { // Nếu DatabaseHelper trả về một tài khoản (tìm thấy trong SQLite)
                    Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();

                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putInt(KEY_ACCOUNT_ID, account.getId());
                    editor.putString(KEY_ACCOUNT_ROLE, account.getRole());
                    editor.apply();

                    redirectToDashboard(account.getRole());
                    finish();
                } else { // Nếu DatabaseHelper trả về null (không tìm thấy tài khoản trong SQLite)
                    Toast.makeText(LoginActivity.this, "Tên đăng nhập hoặc mật khẩu không đúng!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        tvRegisterLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
    }

    private void redirectToDashboard(String role) {
        Intent intent;
        if ("admin".equals(role)) {
            intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
        } else if ("user".equals(role)) {
            intent = new Intent(LoginActivity.this, MainActivity.class);
        } else {
            Toast.makeText(LoginActivity.this, "Vai trò người dùng không xác định!", Toast.LENGTH_LONG).show();
            return;
        }
        startActivity(intent);
        finish();
    }
}
