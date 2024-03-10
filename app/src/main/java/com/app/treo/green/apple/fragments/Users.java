package com.app.treo.green.apple.fragments;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Toast;

import com.app.treo.green.apple.R;
import com.app.treo.green.apple.adapter.AddUserAdapter;
import com.app.treo.green.apple.interfaces.UserCallback;
import com.app.treo.green.apple.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Users extends Fragment {

    Button addUserBtn;
    private RecyclerView recyclerView;
    private AddUserAdapter addUserAdapter;
    private FirebaseDatabase database;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    SwipeRefreshLayout swipLayout;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_users, container, false);

        database = FirebaseDatabase.getInstance();
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();


        swipLayout = view.findViewById(R.id.swipe_refresh_layout);
        addUserBtn = view.findViewById(R.id.add_user);
        recyclerView = view.findViewById(R.id.users_list);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 2);

        recyclerView.setLayoutManager(gridLayoutManager);
        List<User> users = new ArrayList<>();
        addUserAdapter = new AddUserAdapter(users);
        recyclerView.setAdapter(addUserAdapter);

        getAllFavs(users);

        addUserBtn.setOnClickListener(v -> addUserToFirebase(v,users));

        swipLayout.setOnRefreshListener(() -> {
            users.clear();
            getAllFavs(users);
            addUserAdapter.notifyDataSetChanged();
            swipLayout.setRefreshing(false);
        });

        return view;
    }

    private void addUserToFirebase(View v,List<User> users) {
        getContext();
        LayoutInflater layoutInflater = (LayoutInflater) requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View popupView = layoutInflater.inflate(R.layout.add_user, null);

        int width = LinearLayout.LayoutParams.MATCH_PARENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        boolean focusable = true; // lets taps outside the popup also dismiss it
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);

        EditText email = popupView.findViewById(R.id.et_email);
        Button addBtn = popupView.findViewById(R.id.add_user_btn);

        addBtn.setOnClickListener(action -> {
            if (email.getText().toString().isEmpty()) {
                email.setError("Email is required");
                email.requestFocus();
                return;
            }
            addUser(email.getText().toString(),users);
        });

        // show the popup window
        popupWindow.showAtLocation(v, Gravity.CENTER, 0, 0);

        // dismiss the popup window when touched5u
        popupView.setOnTouchListener((action, event) -> {
            popupWindow.dismiss();
            return true;
        });
    }

    public void getAllFavs(List<User> users) {
        CollectionReference collectionReference = firestore.collection("favs")
                .document(mAuth.getUid())
                .collection("users");
        collectionReference.get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (int i = 0; i < queryDocumentSnapshots.size(); i++) {
                String uid = queryDocumentSnapshots.getDocuments().get(i).getString("uid");
                getUserByID(uid, users);
            }
        }).addOnFailureListener(e -> {
            Log.e("RetrieveUserData", "Error retrieving user data: " + e.getMessage());
        });
    }

    private void getUserByID(String uid, List<User> users) {
        DatabaseReference reference = database.getReference("Users");
        reference.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String username = snapshot.child("username").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);
                    String imageUrl = snapshot.child("imageUrl").getValue(String.class);
                    User user = new User(uid, username, email);
                    user.setImageUrl(imageUrl);
                    users.add(user);
                    // Log the size of the users list to verify if data is being added
                    Log.d("Users", "Users list size: " + users.size());
                    // Notify the adapter that the dataset has changed
                    addUserAdapter.notifyDataSetChanged();
                } else {
                    Log.d("Users", "No data found for UID: " + uid);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("DatabaseError", "Error fetching user data: " + error.getMessage());
            }
        });
    }


    private void addUser(String email, List<User> users) {
        getUser(email, user1 -> {
            if (user1 != null) {
                for (User user : users) {
                    if (user.getUid().equals(user1.getUid())) {
                        Toast.makeText(getContext(), "User already added", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                Map<String, Object> userData = new HashMap<>();
                userData.put("uid", user1.getUid());
                firestore.collection("favs").document(Objects.requireNonNull(mAuth.getCurrentUser()).getUid())
                        .collection("users").add(userData).addOnSuccessListener(documentReference -> {
                            Toast.makeText(getContext(), "User added", Toast.LENGTH_SHORT).show();
                        }).addOnFailureListener(e -> {
                            Log.e("AddUser", "addUser: " + e.getMessage());
                        });
            } else {
                Log.e("AddUser", "addUser: User not found");
            }
        });

    }

    private void getUser(String email, UserCallback callback) {
        //get user from firebase
        DatabaseReference reference = database.getReference("Users");

        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String mail = userSnapshot.child("email").getValue(String.class);
                    if (mail.equalsIgnoreCase(email)) {
                        String username = userSnapshot.child("username").getValue(String.class);
                        String id = userSnapshot.child("uid").getValue(String.class);
                        User user = new User(id, username, email);
                        callback.onCallback(user);
                        return;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("AddUser", "onCancelled: " + error.getMessage());
            }
        });

    }
}