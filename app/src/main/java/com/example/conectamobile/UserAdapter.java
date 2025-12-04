package com.example.conectamobile;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide; // Librería de imágenes
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {
    private List<User> users;
    private Context context;

    public UserAdapter(Context context, List<User> users) {
        this.context = context;
        this.users = users;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = users.get(position);
        holder.tvName.setText(user.name);
        holder.tvEmail.setText(user.email);

        // Lógica para cargar la foto con Glide
        if (user.photoUrl != null && !user.photoUrl.isEmpty()) {
            Glide.with(context)
                    .load(user.photoUrl)
                    .placeholder(R.mipmap.ic_launcher_round) // Imagen mientras carga
                    .error(R.mipmap.ic_launcher_round)       // Imagen si falla
                    .circleCrop()                            // Recorte circular
                    .into(holder.ivProfile);
        } else {
            // Si no tiene foto, poner la del sistema por defecto
            holder.ivProfile.setImageResource(R.mipmap.ic_launcher_round);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra("targetUid", user.uid);
            intent.putExtra("targetName", user.name);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() { return users.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail;
        ImageView ivProfile; // Referencia a la imagen

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvUserName);
            tvEmail = itemView.findViewById(R.id.tvUserEmail);
            ivProfile = itemView.findViewById(R.id.ivItemProfile); // Enlazar con el XML
        }
    }
}