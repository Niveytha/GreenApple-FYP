package com.app.treo.green.apple.screens;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.Image;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.app.treo.green.apple.R;
import com.app.treo.green.apple.adapter.MessageAdapter;
import com.app.treo.green.apple.model.Message;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttp;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class ChatActivity extends AppCompatActivity {
    private ImageView imageView;
    private TextView username;

    private FirebaseAuth mAuth;
    private ImageButton recordButton;
    private static String fileName = null;
    private MediaRecorder recorder = null;
    private MediaPlayer player = null;
    private boolean isRecording = false;
    private String otherUserId, otherUserRoom;
    private String currentUserId, currentUserRoom;

    private RecyclerView recyclerView;
    private MessageAdapter adapter;

    private DatabaseReference referenceCurrentUser, referenceOtherUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);


        imageView = findViewById(R.id.chat_image);
        username = findViewById(R.id.chat_name);
        String name = getIntent().getStringExtra("username");
        String image = getIntent().getStringExtra("image");
        otherUserId = getIntent().getStringExtra("uid");

        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser().getUid();

        otherUserRoom = currentUserId + "_" + otherUserId;
        currentUserRoom = otherUserId + "_" + currentUserId;


        username.setText(name);
        Picasso.get().load(image).into(imageView);

        fileName = getExternalCacheDir().getAbsolutePath();
        fileName += "/voice_message.3gp";

        recordButton = findViewById(R.id.btn_record);

        recordButton.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    //show snackbar at top
                    topSnackBar(v);
                    recordButton.setBackgroundResource(R.drawable.record_button_square_pressed);
                    animateButtonColor(recordButton);
                    startRecording();
                    return true;
                case MotionEvent.ACTION_UP:
                    recordButton.setBackgroundResource(R.drawable.record_button_rounded_normal);
                    return true;
            }
            return false;
        });

        ImageView sendButton = findViewById(R.id.send_button);
        ImageView cancelButton = findViewById(R.id.cancel_button);

        sendButton.setOnClickListener(v -> {
            Toast.makeText(ChatActivity.this, "Sending Message!", Toast.LENGTH_SHORT).show();
            uploadAudio();
        });

        cancelButton.setOnClickListener(v -> {
            Toast.makeText(ChatActivity.this, "Recording Cancelled!", Toast.LENGTH_SHORT).show();
            stopRecording();
            // Delete the recorded file
            File file = getRecordedFile();
            if (file.exists()) {
                file.delete();
            }
        });


        recyclerView = findViewById(R.id.chat_recycler);
        adapter = new MessageAdapter(this);
        recyclerView.setAdapter(adapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setReverseLayout(true);
        recyclerView.setLayoutManager(layoutManager);


        referenceCurrentUser = FirebaseDatabase.getInstance().getReference("conversations")
                .child(currentUserRoom);

        referenceOtherUser = FirebaseDatabase.getInstance().getReference("conversations")
                .child(otherUserRoom);


        referenceCurrentUser.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                adapter.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Message message = dataSnapshot.getValue(Message.class);
                    adapter.addMessage(message);
                }
                recyclerView.smoothScrollToPosition(0);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


    }

    private static void topSnackBar(View v) {
        Snackbar snackbar = Snackbar.make(v, "Voice is being Recorded...", Snackbar.LENGTH_SHORT);
        View snackbarView = snackbar.getView();
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) snackbarView.getLayoutParams();
        params.gravity = Gravity.TOP;
        snackbarView.setLayoutParams(params);
        snackbar.show();
    }

    private File getRecordedFile() {
        return new File(fileName);
    }

    private void uploadAudio() {
        File file = getRecordedFile();
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        String fileName = "audio_" + System.currentTimeMillis() + "_" + file.getName();
        StorageReference audioRef = storageRef.child("audio/" + fileName);
        // Upload the audio file to Firebase Storage
        UploadTask uploadTask = audioRef.putFile(Uri.fromFile(file));
        Toast.makeText(ChatActivity.this, "Sending Message!", Toast.LENGTH_SHORT).show();
        uploadTask.addOnFailureListener(e -> {
            // Handle unsuccessful uploads
        }).addOnSuccessListener(taskSnapshot -> {
            audioRef.getDownloadUrl().addOnSuccessListener(uri -> {
                // Get the download URL for the uploaded audio file
                String audioDownloadUrl = uri.toString();
                // Store the audio message in the Realtime Database
                sendMessage(audioDownloadUrl);
                sendNotification();
            });
        });
    }

    private void sendNotification() {
        //get current user details
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users")
                .child(currentUserId);
        reference.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String name = snapshot.child("username").getValue().toString();
                String imageUrl = snapshot.child("imageUrl").getValue().toString();
                String token = snapshot.child("token").getValue().toString();

                try {
                    JSONObject jsonObject = new JSONObject();

                    JSONObject notificationObject = new JSONObject();
                    notificationObject.put("title", name);
                    notificationObject.put("body", "Sent you an audio message");

                    jsonObject.put("notification", notificationObject);
                    jsonObject.put("to", token);

                    callAPI(jsonObject);

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void callAPI(JSONObject jsonObject) {
        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        OkHttpClient client = new OkHttpClient();
        String url = "https://fcm.googleapis.com/fcm/send";
        RequestBody body = RequestBody.create(jsonObject.toString(), mediaType);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .header("Authorization",
                        "Bearer AAAAwH6Uw_E:APA91bF4cY53UbKV2R9Ew3ANP0RxuejZLGy60e__qiJjBKvMkzIV4-qr86M7b6AsTS9qgrmru5O6nAl4oTiAhO5CADFvDTlKyIVM9NGs3rv_BTaf8ttfWx87xXGIXpx5SyqTuqbHYgJv")
                .build();
        client.newCall(request).enqueue(
                new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        Log.e("TAG", "onFailure: " + e.getMessage());
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        Log.e("TAG", "onResponse: " + response.body().string());
                    }
                }
        );
    }

    private void sendMessage(String audioDownloadUrl) {
        String messageId = UUID.randomUUID().toString();
        //get time stamp
        Instant timestamp = Instant.now();
        Message message = new Message(currentUserId, otherUserId, audioDownloadUrl,
                timestamp.toString());

        adapter.addMessage(message);

        referenceCurrentUser
                .child(messageId)
                .setValue(message);

        referenceOtherUser
                .child(messageId)
                .setValue(message);

    }

    public void playAudio(View view) {
        startPlaying();
    }

    public void stopPlaying(View view) {
        stopPlaying();
    }

    private void startRecording() {
        if (!isRecording) {
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setOutputFile(fileName);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

            try {
                recorder.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }

            recorder.start();
            isRecording = true;
        }
    }

    private void stopRecording() {
        if (isRecording) {
            recorder.stop();
            recorder.release();
            recorder = null;
            isRecording = false;
        }
    }

    private void startPlaying() {
        player = new MediaPlayer();
        try {
            player.setDataSource(fileName);
            player.prepare();
            player.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopPlaying() {
        if (player != null) {
            player.release();
            player = null;
        }
    }

    private void animateButtonColor(View view) {
        GradientDrawable backgroundDrawable = (GradientDrawable) view.getBackground();
        int currentColor = backgroundDrawable.getColor().getDefaultColor();

        ObjectAnimator colorAnimator = ObjectAnimator.ofArgb(backgroundDrawable,
                "color", currentColor, Color.RED);
        colorAnimator.setDuration(300);
        colorAnimator.start();
    }

}