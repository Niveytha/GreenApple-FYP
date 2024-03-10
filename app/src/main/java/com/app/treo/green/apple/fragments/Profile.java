package com.app.treo.green.apple.fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.app.treo.green.apple.R;
import com.app.treo.green.apple.screens.Login;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;


public class Profile extends Fragment {
    private TextView username;
    private TextView email;
    private Button logoutBtn;
    private ImageView profileImage;

    private FirebaseAuth mAuth;
    private DatabaseReference database;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        username = view.findViewById(R.id.profile_name);
        email = view.findViewById(R.id.profile_email);
        logoutBtn = view.findViewById(R.id.logout);
        profileImage = view.findViewById(R.id.profile_image);

        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference("Users");
        //get user details
        database.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String userId = mAuth.getCurrentUser().getUid();
                String user_name = dataSnapshot.child(userId).child("username").getValue(String.class);
                String user_email = dataSnapshot.child(userId).child("email").getValue(String.class);
                String user_image = dataSnapshot.child(userId).child("imageUrl").getValue(String.class);

                username.setText(user_name);
                email.setText(user_email);
                Picasso.get().load(user_image).into(profileImage);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d("Profile", "onCancelled: " + databaseError.getMessage());
            }
        });

        logoutBtn.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(getActivity(), Login.class);
            startActivity(intent);
            getActivity().finish();
        });

        return view;
    }
}