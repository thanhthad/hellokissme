package com.example.assignment2.models;

import java.util.Objects;

public class Category {
    private int id;
    private String name;
    private Integer accountId; // Can be null for global/default categories

    public Category() {
    }

    public Category(int id, String name, Integer accountId) {
        this.id = id;
        this.name = name;
        this.accountId = accountId;
    }

    // Constructor without ID, useful for creating new categories before DB insertion
    public Category(String name, Integer accountId) {
        this.name = name;
        this.accountId = accountId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAccountId() {
        return accountId;
    }

    public void setAccountId(Integer accountId) {
        this.accountId = accountId;
    }
    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Category category = (Category) o;
        return id == category.id &&
                Objects.equals(name, category.name) &&
                Objects.equals(accountId, category.accountId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, accountId);
    }
}