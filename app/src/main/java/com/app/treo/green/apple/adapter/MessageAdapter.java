package com.app.treo.green.apple.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.treo.green.apple.R;
import com.app.treo.green.apple.model.Message;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageViewHolder> {

    private Context context;
    private List<Message> messageList;

    public MessageAdapter(Context context) {
        this.context = context;
        this.messageList = new ArrayList<>();
    }

    public void addMessage(Message message) {
        messageList.add(0, message);
        notifyItemInserted(0);
    }

    public void clear() {
        messageList.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_item_sender_audio, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messageList.get(position);
        if (message.getCurrentUser().equals(FirebaseAuth.getInstance().getUid())) {
            holder.bindSenderData(message);
        } else {
            holder.bindReceiverData(message);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }
}
