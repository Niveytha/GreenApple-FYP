package com.app.treo.green.apple.adapter;

import android.media.MediaPlayer;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.treo.green.apple.R;
import com.app.treo.green.apple.model.Message;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class MessageViewHolder extends RecyclerView.ViewHolder {
    private ImageView audioIconSender, audioIconReceiver;
    private TextView timestampSender, timestampReceiver;
    private ImageView tickMark;

    RelativeLayout senderLayout, receiverLayout;


    public MessageViewHolder(@NonNull View itemView) {
        super(itemView);
        audioIconSender = itemView.findViewById(R.id.audio_icon_sender);
        timestampSender = itemView.findViewById(R.id.timestamp_sender);
        tickMark = itemView.findViewById(R.id.tick_sender);

        senderLayout = itemView.findViewById(R.id.audio_sender_layout);
        receiverLayout = itemView.findViewById(R.id.audio_receiver_layout);
        audioIconReceiver = itemView.findViewById(R.id.play_audio_sender);
        timestampReceiver = itemView.findViewById(R.id.timestamp_receiver);

    }

    public void bindSenderData(Message message) {
        receiverLayout.setVisibility(View.GONE);
        ZonedDateTime timestamp = ZonedDateTime.parse(message.getTimestamp());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy hh:mm a"); // Use 'MMM d, yyyy hh:mm a' for date and time in 12-hour format
        String formattedTime = timestamp.withZoneSameInstant(ZoneId.systemDefault()).format(formatter);
        timestampSender.setText(formattedTime);
        audioIconSender.setImageResource(R.drawable.ic_audio_sender);
        tickMark.setImageResource(R.drawable.ic_tick);

        senderLayout.setOnClickListener(v -> {
            //play audio
            try {
                MediaPlayer mediaPlayer = new MediaPlayer();

                mediaPlayer.setDataSource(message.getAudioUrl());
                mediaPlayer.prepare();
                mediaPlayer.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

    }

    public void bindReceiverData(Message message) {
        senderLayout.setVisibility(View.GONE);
        //get date time in 12 hour format
        ZonedDateTime timestamp = ZonedDateTime.parse(message.getTimestamp());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy hh:mm a"); // Use 'MMM d, yyyy hh:mm a' for date and time in 12-hour format
        String formattedTime = timestamp.withZoneSameInstant(ZoneId.systemDefault()).format(formatter);
        timestampReceiver.setText(formattedTime);
        audioIconReceiver.setImageResource(R.drawable.ic_audio_sender);
        tickMark.setVisibility(View.GONE);

        receiverLayout.setOnClickListener(v -> {
            //play audio
            try {
                MediaPlayer mediaPlayer = new MediaPlayer();
                mediaPlayer.setDataSource(message.getAudioUrl());
                mediaPlayer.prepare();
                mediaPlayer.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

}

