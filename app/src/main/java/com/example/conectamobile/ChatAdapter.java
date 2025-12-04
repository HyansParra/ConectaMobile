package com.example.conectamobile;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {
    private List<Message> messages;
    private Context context;
    private String currentUid;

    public ChatAdapter(Context context, List<Message> messages) {
        this.context = context;
        this.messages = messages;
        this.currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Message msg = messages.get(position);
        holder.tvBody.setText(msg.text);

        // Alinear a la derecha si es mío, izquierda si es otro
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.tvBody.getLayoutParams();
        if (msg.senderId.equals(currentUid)) {
            holder.tvUser.setText("Yo");
            holder.container.setGravity(Gravity.END);
        } else {
            holder.tvUser.setText("Otro");
            holder.container.setGravity(Gravity.START);
        }
    }

    @Override
    public int getItemCount() { return messages.size(); }

    public void addMessage(Message msg) {
        messages.add(msg);
        notifyItemInserted(messages.size() - 1);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUser, tvBody;
        LinearLayout container;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUser = itemView.findViewById(R.id.tvMessageUser);
            tvBody = itemView.findViewById(R.id.tvMessageBody);
            container = (LinearLayout) itemView;
        }
    }
}