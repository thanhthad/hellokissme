// Trong file Account.java
package com.example.assignment2.models;

public class Account {
    private int id;
    private String username;
    private String password;
    private String role; // <<< THÊM TRƯỜNG NÀY

    // Constructors
    public Account() {
    }

    public Account(int id, String username, String password, String role) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role; // <<< CẬP NHẬT CONSTRUCTOR
    }

    public Account(String username, String password, String role) {
        this.username = username;
        this.password = password;
        this.role = role; // <<< CẬP NHẬT CONSTRUCTOR
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    // <<< THÊM GETTER VÀ SETTER CHO ROLE
    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        // Bạn có thể thêm kiểm tra ở đây để đảm bảo role chỉ là 'admin' hoặc 'user'
        // ví dụ: if ("admin".equals(role) || "user".equals(role))
        this.role = role;
    }
}
