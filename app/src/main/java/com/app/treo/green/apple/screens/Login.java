package com.app.treo.green.apple.screens;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.app.treo.green.apple.R;
import com.app.treo.green.apple.model.User;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.FirebaseDatabase;

public class Login extends AppCompatActivity {
    int RC_SIGN_IN = 20;

    EditText email, password;
    Button loginBtn;
    SignInButton googleSignInBtn;
    TextView signUpTv, forgotPasswordTv;

    private FirebaseAuth mAuth;
    FirebaseDatabase database;
    GoogleSignInClient mGoogleSignInClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initViews();

        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail().build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        //mouse click on sign up text view
        signUpTv.setOnClickListener(v -> {
            Intent intent = new Intent(Login.this, Register.class);
            startActivity(intent);
        });

        googleSignInBtn.setOnClickListener(v -> {
            loginWithGoogle();
        });

        loginBtn.setOnClickListener(action -> {
            login();
        });

        forgotPasswordTv.setOnClickListener(action -> {
            LayoutInflater inflater = (LayoutInflater)
                    getSystemService(LAYOUT_INFLATER_SERVICE);
            View popupView = inflater.inflate(R.layout.forgot_password, null);


            // create the popup window
            int width = LinearLayout.LayoutParams.MATCH_PARENT;
            int height = LinearLayout.LayoutParams.WRAP_CONTENT;
            boolean focusable = true; // lets taps outside the popup also dismiss it
            final PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);

            EditText email = popupView.findViewById(R.id.et_email);
            Button resetBtn = popupView.findViewById(R.id.btn_forgot_password);

            resetBtn.setOnClickListener(v -> {
                String emailStr = email.getText().toString();
                if (emailStr.isEmpty()) {
                    email.setError("Email is required");
                    return;
                }
                FirebaseAuth.getInstance().sendPasswordResetEmail(emailStr).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Snackbar.make(v, "Password reset link sent to your email", Snackbar.LENGTH_LONG).show();
                    } else {
                        Snackbar.make(v, "Failed to send reset link", Snackbar.LENGTH_LONG).show();
                    }
                }).addOnFailureListener(e -> {
                    Snackbar.make(v, "Failed to send reset link", Snackbar.LENGTH_LONG).show();

                });
            });

            // show the popup window
            popupWindow.showAtLocation(action, Gravity.CENTER, 0, 0);

            // dismiss the popup window when touched5u
            popupView.setOnTouchListener((v, event) -> {
                popupWindow.dismiss();
                return true;
            });
        });

    }

    private void login() {
        String email = this.email.getText().toString();
        String password = this.password.getText().toString();

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


        Toast.makeText(Login.this, "Login in progress! Please wait!!!", Toast.LENGTH_SHORT).show();

        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Intent intent = new Intent(Login.this, MainActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(Login.this, "Login Failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loginWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuth(account.getIdToken());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, "Google Sign In Failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void firebaseAuth(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = mAuth.getCurrentUser();
                User loggedInUser = new User();
                loggedInUser.setUid(user.getUid());
                loggedInUser.setEmail(user.getEmail());
                loggedInUser.setUsername(user.getDisplayName());

                database.getReference().child("Users").child(user.getUid()).setValue(loggedInUser);

                Intent intent = new Intent(Login.this, MainActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Google Sign In Failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initViews() {
        email = findViewById(R.id.et_email);
        password = findViewById(R.id.et_password);
        loginBtn = findViewById(R.id.btn_login);
        forgotPasswordTv = findViewById(R.id.tv_forgot_password);
        googleSignInBtn = findViewById(R.id.btn_login_google);
        signUpTv = findViewById(R.id.tv_register);
    }
}