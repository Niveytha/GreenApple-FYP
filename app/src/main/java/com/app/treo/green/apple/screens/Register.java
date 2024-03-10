package com.app.treo.green.apple.screens;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.app.treo.green.apple.R;
import com.app.treo.green.apple.model.User;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

public class Register extends AppCompatActivity {

    EditText email, username, password, confirmPassword;
    Button registerBtn;
    TextView loginTv;
    private FirebaseAuth mAuth;
    private FirebaseDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initViews();
        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();

        //mouse click on login text view
        loginTv.setOnClickListener(v -> {
            finish();
        });

        registerBtn.setOnClickListener(action -> {
            registerUser();
        });
    }

    private void registerUser() {
        String email = this.email.getText().toString();
        String password = this.password.getText().toString();
        String confirmPassword = this.confirmPassword.getText().toString();

        if (email.isEmpty()) {
            this.email.setError("Email is required");
            this.email.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            this.password.setError("Password is required");
            this.password.requestFocus();
            return;
        }

        if (confirmPassword.isEmpty()) {
            this.confirmPassword.setError("Confirm password is required");
            this.confirmPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            this.confirmPassword.setError("Password and confirm password must be same");
            this.confirmPassword.requestFocus();
            return;
        }

        Toast.makeText(Register.this, "Registration in progress", Toast.LENGTH_SHORT).show();

        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                User user = new User(mAuth.getCurrentUser().getUid(),
                        username.getText().toString(), email);
                database.getReference().child("Users").child(user.getUid()).setValue(user);
                finish();
            } else {
                Toast.makeText(Register.this, "Registration Failed", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void initViews() {
        email = findViewById(R.id.et_email);
        username = findViewById(R.id.et_username);
        password = findViewById(R.id.et_password);
        confirmPassword = findViewById(R.id.et_confirm_password);
        registerBtn = findViewById(R.id.btn_register);
        loginTv = findViewById(R.id.tv_login);
    }
}