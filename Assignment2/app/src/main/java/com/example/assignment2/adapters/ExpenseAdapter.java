package com.example.assignment2.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.assignment2.R;
import com.example.assignment2.models.Expense;

import java.util.List;
import java.util.Locale; // Đã thêm dòng import này

public class ExpenseAdapter extends ArrayAdapter<Expense> {

    private Context context;
    private int resource;

    public ExpenseAdapter(@NonNull Context context, int resource, @NonNull List<Expense> objects) {
        super(context, resource, objects);
        this.context = context;
        this.resource = resource;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        Expense expense = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(resource, parent, false);
        }

        TextView tvExpenseId = convertView.findViewById(R.id.tvExpenseId);
        TextView tvExpenseDescription = convertView.findViewById(R.id.tvExpenseDescription);
        TextView tvExpenseAmount = convertView.findViewById(R.id.tvExpenseAmount);
        TextView tvExpenseCategory = convertView.findViewById(R.id.tvExpenseCategory);
        TextView tvExpenseDate = convertView.findViewById(R.id.tvExpenseDate);


        if (expense != null) {
            tvExpenseId.setText("ID: " + expense.getId());
            tvExpenseDescription.setText("Mô tả: " + expense.getDescription());
            tvExpenseAmount.setText("Số tiền: " + String.format(Locale.getDefault(), "%.0f", expense.getAmount())); // Dòng này giờ sẽ hoạt động
            tvExpenseCategory.setText("Danh mục: " + expense.getCategory());
            tvExpenseDate.setText("Ngày: " + expense.getDate());
        }

        return convertView;
    }
}