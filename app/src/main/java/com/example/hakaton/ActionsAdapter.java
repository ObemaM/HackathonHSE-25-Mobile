package com.example.hakaton;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ActionsAdapter extends RecyclerView.Adapter<ActionsAdapter.ActionViewHolder> {

    private final List<ActionItem> actionList;
    private final List<ActionItem> filteredList;
    private final OnActionClickListener listener;

    public interface OnActionClickListener {
        void onActionClick(ActionItem action);
    }

    public ActionsAdapter(List<ActionItem> actionList, OnActionClickListener listener) {
        this.actionList = actionList;
        this.filteredList = new ArrayList<>(actionList);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ActionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_action_button, parent, false);
        return new ActionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ActionViewHolder holder, int position) {
        ActionItem action = filteredList.get(position);
        holder.buttonAction.setText(action.getActionText());

        // Используем ваш метод через контекст Activity
        Context context = holder.itemView.getContext();
        if (context instanceof MainActivity) {
            MainActivity activity = (MainActivity) context;
            activity.setupAntiSpamButton(holder.buttonAction, () -> {
                if (listener != null) {
                    listener.onActionClick(action);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    // Фильтрация по тексту
    public void filter(String searchText) {
        filteredList.clear();

        if (searchText.isEmpty()) {
            filteredList.addAll(actionList);
        } else {
            String query = searchText.toLowerCase().trim();
            for (ActionItem action : actionList) {
                if (action.getActionText().toLowerCase().contains(query)) {
                    filteredList.add(action);
                }
            }
        }
        notifyDataSetChanged();
    }

    static class ActionViewHolder extends RecyclerView.ViewHolder {
        Button buttonAction;

        public ActionViewHolder(@NonNull View itemView) {
            super(itemView);
            buttonAction = itemView.findViewById(R.id.buttonAction);
        }
    }
}
