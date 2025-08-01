package com.example.assignment2.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.assignment2.R;
import com.example.assignment2.models.Category;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private List<Category> categoryList;
    private Context context;
    private OnCategoryActionListener listener;
    private int currentAccountId; // To disable editing/deleting global categories

    public interface OnCategoryActionListener {
        void onEditCategory(Category category, int position);
        void onDeleteCategory(Category category, int position);
    }

    public CategoryAdapter(Context context, List<Category> categoryList, int currentAccountId, OnCategoryActionListener listener) {
        this.context = context;
        this.categoryList = categoryList;
        this.currentAccountId = currentAccountId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = categoryList.get(position);
        holder.tvCategoryName.setText(category.getName());

        // Global categories (accountId is null) cannot be edited or deleted by users.
        boolean isUserCategory = category.getAccountId() != null && category.getAccountId() == currentAccountId;

        holder.ivEdit.setVisibility(isUserCategory ? View.VISIBLE : View.INVISIBLE);
        holder.ivDelete.setVisibility(isUserCategory ? View.VISIBLE : View.INVISIBLE);

        if (isUserCategory) {
            holder.ivEdit.setOnClickListener(v -> listener.onEditCategory(category, position));
            holder.ivDelete.setOnClickListener(v -> listener.onDeleteCategory(category, position));
        } else {
            holder.ivEdit.setOnClickListener(null); // Remove listener if not user category
            holder.ivDelete.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return categoryList.size();
    }

    public void updateCategories(List<Category> newCategories) {
        this.categoryList = newCategories;
        notifyDataSetChanged();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategoryName;
        ImageView ivEdit, ivDelete;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryName = itemView.findViewById(R.id.tv_category_name_item);
            ivEdit = itemView.findViewById(R.id.iv_edit_category);
            ivDelete = itemView.findViewById(R.id.iv_delete_category);
        }
    }
}
