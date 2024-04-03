package com.app.treo.green.apple.screens;

import static android.app.PendingIntent.getActivity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import com.app.treo.green.apple.R;
import com.app.treo.green.apple.adapter.ViewPagerAdapter;
import com.app.treo.green.apple.fragments.Profile;
import com.app.treo.green.apple.fragments.Users;
import com.app.treo.green.apple.utils.ReplaceFont;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager2 viewPager2;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        myToolbar.setTitle("Green Apple™");
        setSupportActionBar(myToolbar);

        //ask permission for RECORD_AUDIO
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        }


        tabLayout = findViewById(R.id.tab_layout);
        viewPager2 = findViewById(R.id.view_pager);


        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager(), getLifecycle());
        adapter.addFragment(new Users(), "Users");
        adapter.addFragment(new Profile(), "Profile");
        viewPager2.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager2, (tab, position) ->
                tab.setText(adapter.getTitle(position))).attach();

        mAuth = FirebaseAuth.getInstance();

        getToken();

        //get permission for push notifications
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){

            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 99);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        //permission granted
        //permission denied
    }

    public void getToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        return;
                    }
                    // Get new FCM registration token
                    String token = task.getResult();
                    DatabaseReference database = FirebaseDatabase.getInstance().getReference();
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        //update the user details to add token
                        database
                                .child("Users")
                                .child(user.getUid())
                                .child("token")
                                .setValue(token);
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Intent intent = new Intent(MainActivity.this, Login.class);
            startActivity(intent);
            finish();
        }
    }
}