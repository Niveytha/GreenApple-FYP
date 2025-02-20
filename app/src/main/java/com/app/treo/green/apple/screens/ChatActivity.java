package com.app.treo.green.apple.screens;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.app.treo.green.apple.R;
import com.app.treo.green.apple.adapter.MessageAdapter;
import com.app.treo.green.apple.model.Message;
import com.app.treo.green.apple.model.User;
import com.app.treo.green.apple.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.snackbar.Snackbar;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
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
    private ImageView imageView,sendButton,cancelButton;
    private TextView username;

    private FirebaseAuth mAuth;
    private ImageButton recordButton;
    private static String fileName = null;
    private MediaRecorder recorder = null;
    private MediaPlayer player = null;
    private boolean isRecording = false;
    private String otherUserId;
    private String currentUserId;

    private RecyclerView recyclerView;
    private MessageAdapter adapter;
    private Message model;

    private boolean voiceBeingRecorded = false;


    String chatRoomId, otherUserToken;

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

        chatRoomId = FirebaseUtil.getChatroomId(currentUserId, otherUserId);


        getOtherUserToken();


        username.setText(name);
        Picasso.get().load(image).into(imageView);

        fileName = getExternalCacheDir().getAbsolutePath();
        fileName += "/voice_message.3gp";

        recordButton = findViewById(R.id.btn_record);

        sendButton = findViewById(R.id.send_button);
        cancelButton = findViewById(R.id.cancel_button);
        recyclerView = findViewById(R.id.chat_recycler);

        sendButton.setVisibility(View.GONE);
        cancelButton.setVisibility(View.GONE);

        sendButton.setOnClickListener(v -> {
            Toast.makeText(ChatActivity.this, "Sending message!", Toast.LENGTH_SHORT).show();
            uploadAudio();
        });

        cancelButton.setOnClickListener(v -> {
            Toast.makeText(ChatActivity.this, "Recording cancelled!", Toast.LENGTH_SHORT).show();
            stopRecording();
            // Delete the recorded file
            File file = getRecordedFile();
            if (file.exists()) {
                file.delete();
            }
            recordButton.setVisibility(View.VISIBLE);
            sendButton.setVisibility(View.GONE);
            cancelButton.setVisibility(View.GONE);
        });

        recordButton.setOnClickListener(action -> {
            if (!voiceBeingRecorded) {
                topSnackBar();
                recordButton.setBackgroundResource(R.drawable.record_button_square_pressed);
                animateButtonColor(recordButton);
                startRecording();
                voiceBeingRecorded = true;
            } else {
                stopRecording();
                sendButton.setVisibility(View.VISIBLE);
                cancelButton.setVisibility(View.VISIBLE);
                recordButton.setBackgroundResource(R.drawable.record_button_rounded_normal);
                voiceBeingRecorded = false;
                recordButton.setVisibility(View.GONE);
            }

        });


        getOrCreateChatroomModel();
        setupChatRecyclerView();


    }

    private void getOtherUserToken() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users").child(otherUserId);
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                otherUserToken = snapshot.child("token").getValue(String.class);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    void setupChatRecyclerView() {
        Query query = FirebaseUtil.getChatroomMessageReference(chatRoomId)
                .orderBy("timestamp", Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<Message> options = new FirestoreRecyclerOptions.Builder<Message>()
                .setQuery(query, Message.class).build();

        adapter = new MessageAdapter(options, getApplicationContext());
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setReverseLayout(true);
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(adapter);
        adapter.startListening();
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                recyclerView.smoothScrollToPosition(0);
            }
        });
    }

    private void topSnackBar() {
        Snackbar snackbar = Snackbar.make(findViewById(R.id.chat_layout),
                "Audio is being recorded!", Snackbar.LENGTH_SHORT);
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
        Toast.makeText(ChatActivity.this, "Uploading audio!", Toast.LENGTH_LONG).show();
        uploadTask.addOnFailureListener(e -> {
            // Handle unsuccessful uploads
            Toast.makeText(this, "Failed to upload audio", Toast.LENGTH_SHORT).show();
            recordButton.setVisibility(View.VISIBLE);
            sendButton.setVisibility(View.GONE);
            cancelButton.setVisibility(View.GONE);
        }).addOnSuccessListener(taskSnapshot -> {
            audioRef.getDownloadUrl().addOnSuccessListener(uri -> {
                // Get the download URL for the uploaded audio file
                String audioDownloadUrl = uri.toString();
                // Store the audio message in the Realtime Database
                sendMessage(audioDownloadUrl);
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

                try {
                    JSONObject jsonObject = new JSONObject();

                    JSONObject notificationObject = new JSONObject();
                    notificationObject.put("title", name);
                    notificationObject.put("body", "Sent you an audio message");

                    jsonObject.put("notification", notificationObject);
                    jsonObject.put("to", otherUserToken);

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
                        "Bearer ")
                .build();
        client.newCall(request).enqueue(
                new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        Log.e("TAG", "onFailure: " + e.getMessage());
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        Log.i("TAG", "onResponse: " + response.body().string());
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

        FirebaseUtil.getChatroomMessageReference(chatRoomId).add(message)
                .addOnCompleteListener(
                        task -> {
                            if (task.isSuccessful()) {
                                sendNotification();
                                recordButton.setVisibility(View.VISIBLE);
                                sendButton.setVisibility(View.GONE);
                                cancelButton.setVisibility(View.GONE);
                            }
                        }
                );
    }

    void getOrCreateChatroomModel() {
        FirebaseUtil.getChatroomReference(chatRoomId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                model = task.getResult().toObject(Message.class);
                if (model == null) {
                    //first time chat
                    ZonedDateTime timestamp = ZonedDateTime.now();
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy hh:mm a"); // Use 'MMM d, yyyy hh:mm a' for date and time in 12-hour format
                    String formattedTime = timestamp.withZoneSameInstant(ZoneId.systemDefault()).format(formatter);

                    model = new Message(
                            currentUserId,
                            otherUserId,
                            "",
                            formattedTime
                    );
                    FirebaseUtil.getChatroomReference(otherUserId).set(model);
                }
            }
        });
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
        //check file size if its exceed 10Mb then delete it
        File file = new File(fileName);
        if (file.exists()) {
            if (file.length() > 10000000) {
                file.delete();
                //show snackbar to user with proper message
                Snackbar snackbar = Snackbar.make(findViewById(R.id.chat_layout),
                        "Recording size should be less than 10Mb", Snackbar.LENGTH_SHORT);
                snackbar.setDuration(5000);
                //add action to dismiss
                snackbar.setAction("Dismiss", v -> snackbar.dismiss());

                snackbar.show();
            }
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