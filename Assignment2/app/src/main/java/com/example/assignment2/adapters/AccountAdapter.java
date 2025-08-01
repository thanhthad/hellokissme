package com.example.assignment2.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.assignment2.R;
import com.example.assignment2.models.Account; // Đảm bảo đúng model
import java.util.List;

public class AccountAdapter extends RecyclerView.Adapter<AccountAdapter.AccountViewHolder> {

    private Context context;
    private List<Account> accountList;
    private OnAccountActionListener listener;

    // Interface để xử lý sự kiện click trên item
    public interface OnAccountActionListener {
        void onEditAccount(Account account);
        void onDeleteAccount(Account account);
    }

    public AccountAdapter(Context context, List<Account> accountList, OnAccountActionListener listener) {
        this.context = context;
        this.accountList = accountList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AccountViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Sử dụng layout item bạn đã tạo (ví dụ: list_item_account_admin.xml)
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_account_admin, parent, false);
        return new AccountViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AccountViewHolder holder, int position) {
        Account account = accountList.get(position);
        holder.tvUsername.setText(account.getUsername());
        holder.tvRole.setText("Vai trò: " + account.getRole());
        // Bạn có thể hiển thị thêm ID nếu muốn: holder.tvAccountId.setText("ID: " + account.getId());

        holder.btnEditAccount.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditAccount(account);
            }
        });

        holder.btnDeleteAccount.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteAccount(account);
            }
        });
    }

    @Override
    public int getItemCount() {
        return accountList.size();
    }

    // ViewHolder để giữ các View của mỗi item
    static class AccountViewHolder extends RecyclerView.ViewHolder {
        TextView tvUsername, tvRole; // tvAccountId (nếu có)
        ImageButton btnEditAccount, btnDeleteAccount;

        public AccountViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tvListItemUsername); // Đảm bảo ID khớp với layout item
            tvRole = itemView.findViewById(R.id.tvListItemRole);       // Đảm bảo ID khớp với layout item
            // tvAccountId = itemView.findViewById(R.id.tvListItemAccountId); // Nếu bạn thêm ID vào layout item
            btnEditAccount = itemView.findViewById(R.id.btnListItemEditAccount); // Đảm bảo ID khớp
            btnDeleteAccount = itemView.findViewById(R.id.btnListItemDeleteAccount); // Đảm bảo ID khớp
        }
    }
}
