package com.example.assignment2.models;

public class Budget {
    private int id;
    private int accountId;
    private int categoryId; // ID của category
    private String categoryName; // Tên của category (để hiển thị)
    private double amount;
    // private int month; // Nếu bạn làm ngân sách theo tháng/năm
    // private int year;

    public Budget() {
    }

    public Budget(int accountId, int categoryId, String categoryName, double amount) {
        this.accountId = accountId;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.amount = amount;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getAccountId() {
        return accountId;
    }

    public void setAccountId(int accountId) {
        this.accountId = accountId;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    // toString() để dễ debug hoặc hiển thị trong ArrayAdapter nếu cần
    @Override
    public String toString() {
        return categoryName + ": " + String.format("%,.0f", amount) + " VND";
    }
}
