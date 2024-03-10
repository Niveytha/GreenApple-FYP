package com.app.treo.green.apple.adapter;

import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.app.treo.green.apple.R;

public class UserViewHolder extends RecyclerView.ViewHolder {
    public ImageView userImage;
    public TextView userName;
    public LinearLayout userLayout;

    public UserViewHolder(View itemView) {
        super(itemView);
        userImage = itemView.findViewById(R.id.user_image);
        userName = itemView.findViewById(R.id.user_name);
        userLayout = itemView.findViewById(R.id.root_layout);
    }
}
