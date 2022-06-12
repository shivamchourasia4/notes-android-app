package com.cse.notes.listeners;

import com.cse.notes.entities.CheckList;

public interface ChecklistListener {
    void onChecklistClicked(CheckList checkList, String content);

    void onUnmarkedClicked(CheckList checkList);

    void onMarkedClicked(CheckList checkList);

    void onChecklistDeleteClicked(CheckList checkList, int position);
}
