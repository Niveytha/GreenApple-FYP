package com.app.treo.green.apple.model;

import androidx.annotation.NonNull;

public class Message {
    private String currentUser;
    private String otherUser;
    private String audioUrl;
    private String timestamp;

    public Message() {
        // Default constructor required for Firebase
    }

    public Message(String senderId, String receiverId, String duration, String timestamp) {
        this.currentUser = senderId;
        this.otherUser = receiverId;
        this.audioUrl = duration;
        this.timestamp = timestamp;
    }

    public String getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(String currentUser) {
        this.currentUser = currentUser;
    }

    public String getOtherUser() {
        return otherUser;
    }

    public void setOtherUser(String otherUser) {
        this.otherUser = otherUser;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
