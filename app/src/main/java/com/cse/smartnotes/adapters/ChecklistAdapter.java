package com.cse.smartnotes.adapters;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cse.smartnotes.R;
import com.cse.smartnotes.entities.CheckList;
import com.cse.smartnotes.listeners.ChecklistListener;

import java.util.List;

public class ChecklistAdapter extends RecyclerView.Adapter<ChecklistAdapter.ChecklistViewHolder> {

    private final List<CheckList> checkLists;
    private final ChecklistListener checklistListener;

    public ChecklistAdapter(List<CheckList> checkLists, ChecklistListener checklistListener) {
        this.checkLists = checkLists;
        this.checklistListener = checklistListener;
    }

    @NonNull
    @Override
    public ChecklistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ChecklistViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.item_container_checklist, parent, false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull ChecklistViewHolder holder, int position) {
        holder.setChecklist(checkLists.get(position));
        holder.checklistText.requestFocus();
        holder.checklistText.setOnFocusChangeListener((view, hasFocus) -> {
            if (!hasFocus) {
                checklistListener.onChecklistClicked(checkLists.get(position), holder.checklistText.getText().toString());
            }
        });
        holder.markedCheckbox.setOnClickListener(view -> {
            holder.unmarkedCheckbox.setVisibility(View.VISIBLE);
            holder.markedCheckbox.setVisibility(View.GONE);
            holder.checklistText.setPaintFlags(holder.checklistText.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            checklistListener.onMarkedClicked(checkLists.get(position));
        });
        holder.unmarkedCheckbox.setOnClickListener(view -> {
            holder.markedCheckbox.setVisibility(View.VISIBLE);
            holder.unmarkedCheckbox.setVisibility(View.GONE);
            holder.checklistText.setPaintFlags(holder.checklistText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            checklistListener.onUnmarkedClicked(checkLists.get(position));

        });
        holder.deleteChecklistItem.setOnClickListener(view -> {
            checklistListener.onChecklistDeleteClicked(checkLists.get(position), holder.getAbsoluteAdapterPosition());
        });

    }

    @Override
    public int getItemCount() {
        return checkLists.size();
    }

    static class ChecklistViewHolder extends RecyclerView.ViewHolder {

        EditText checklistText;
        ImageView markedCheckbox;
        ImageView unmarkedCheckbox;
        ImageView deleteChecklistItem;

        ChecklistViewHolder(@NonNull View itemView) {
            super(itemView);
            checklistText = itemView.findViewById(R.id.textCheckList);
            markedCheckbox = itemView.findViewById(R.id.markedChecklist);
            unmarkedCheckbox = itemView.findViewById(R.id.unmarkedChecklist);
            deleteChecklistItem = itemView.findViewById(R.id.removeChecklist);
        }

        public void setChecklist(CheckList checklist) {
            checklistText.setText(checklist.getChecklistContent());
            if (checklist.isChecked()) {
                unmarkedCheckbox.setVisibility(View.GONE);
                markedCheckbox.setVisibility(View.VISIBLE);
                checklistText.setPaintFlags(checklistText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            }
        }
    }
}
