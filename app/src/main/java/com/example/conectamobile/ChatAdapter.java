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

/**
 * Adaptador para los mensajes del Chat.
 * Maneja la lógica visual de "Burbujas":
 * - Mensajes propios: Alineados a la derecha, fondo verde.
 * - Mensajes externos: Alineados a la izquierda, fondo gris.
 */
public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {
    private List<Message> messages;
    private Context context;
    private String currentUid;

    public ChatAdapter(Context context, List<Message> messages) {
        this.context = context;
        this.messages = messages;
        // Obtener UID actual para distinguir mensajes propios
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            this.currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            this.currentUid = "";
        }
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

        // Determinar si el mensaje es mío
        boolean isMe = msg.senderId != null && msg.senderId.equals(currentUid);

        if (isMe) {
            // Estilo: Emisor (Derecha)
            holder.rootLayout.setGravity(Gravity.END);
            holder.container.setBackgroundResource(R.drawable.bg_message_me);
            holder.tvUser.setVisibility(View.GONE);
        } else {
            // Estilo: Receptor (Izquierda)
            holder.rootLayout.setGravity(Gravity.START);
            holder.container.setBackgroundResource(R.drawable.bg_message_other);
            holder.tvUser.setVisibility(View.VISIBLE);
            holder.tvUser.setText("Contacto");
        }
    }

    @Override
    public int getItemCount() { return messages.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUser, tvBody;
        LinearLayout container, rootLayout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUser = itemView.findViewById(R.id.tvMessageUser);
            tvBody = itemView.findViewById(R.id.tvMessageBody);
            container = itemView.findViewById(R.id.messageContainer);
            rootLayout = itemView.findViewById(R.id.rootLayout);
        }
    }
}