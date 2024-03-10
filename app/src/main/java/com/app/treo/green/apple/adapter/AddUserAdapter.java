package com.app.treo.green.apple.adapter;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.treo.green.apple.R;
import com.app.treo.green.apple.model.User;
import com.app.treo.green.apple.screens.ChatActivity;
import com.app.treo.green.apple.utils.CircleTransform;
import com.squareup.picasso.Picasso;

import java.util.List;


public class AddUserAdapter extends RecyclerView.Adapter<UserViewHolder> {
    private List<User> userList;

    public AddUserAdapter(List<User> users) {
        this.userList = users;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.users_list_tile, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.userName.setText(user.getUsername());

        //set image here make the image circular
        Picasso.get().load(user.getImageUrl()).transform(new CircleTransform()).into(holder.userImage);

        holder.userLayout.setOnClickListener(action -> {
            //show ChatActivity
            Intent intent = new Intent(holder.itemView.getContext(), ChatActivity.class);
            intent.putExtra("uid", user.getUid());
            intent.putExtra("username", user.getUsername());
            intent.putExtra("image", user.getImageUrl());
            holder.itemView.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }
}
