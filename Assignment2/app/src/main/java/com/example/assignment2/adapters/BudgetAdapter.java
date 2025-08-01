package com.example.assignment2.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast; // Thêm Toast để kiểm tra

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.assignment2.R;
import com.example.assignment2.models.Budget;

import java.util.List;
import java.util.Locale;

public class BudgetAdapter extends ArrayAdapter<Budget> {

    private Context context;
    private List<Budget> budgetList;
    private OnBudgetActionListener listener;

    // Interface để giao tiếp lại với Activity
    public interface OnBudgetActionListener {
        void onEditBudget(Budget budget, int position);
        void onDeleteBudget(Budget budget, int position);
    }

    public BudgetAdapter(@NonNull Context context, @NonNull List<Budget> budgetList, OnBudgetActionListener listener) {
        super(context, 0, budgetList); // Sử dụng layoutId 0 vì chúng ta sẽ inflate custom layout
        this.context = context;
        this.budgetList = budgetList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItemView = convertView;
        if (listItemView == null) {
            listItemView = LayoutInflater.from(context).inflate(R.layout.list_item_budget, parent, false);
        }

        Budget currentBudget = budgetList.get(position);

        TextView tvCategoryName = listItemView.findViewById(R.id.tv_budget_category_name);
        TextView tvBudgetAmount = listItemView.findViewById(R.id.tv_budget_amount);
        ImageView ivEditBudget = listItemView.findViewById(R.id.iv_edit_budget);
        ImageView ivDeleteBudget = listItemView.findViewById(R.id.iv_delete_budget);

        if (currentBudget != null) {
            tvCategoryName.setText(currentBudget.getCategoryName());
            // Định dạng số tiền cho dễ đọc hơn
            tvBudgetAmount.setText(String.format(Locale.getDefault(), "%,.0f đ", currentBudget.getAmount()));

            ivEditBudget.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditBudget(currentBudget, position);
                }
            });

            ivDeleteBudget.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteBudget(currentBudget, position);
                }
            });
        }
        return listItemView;
    }

    // Phương thức để cập nhật dữ liệu (nếu cần)
    public void updateData(List<Budget> newBudgetList) {
        this.budgetList.clear();
        this.budgetList.addAll(newBudgetList);
        notifyDataSetChanged();
    }
}
