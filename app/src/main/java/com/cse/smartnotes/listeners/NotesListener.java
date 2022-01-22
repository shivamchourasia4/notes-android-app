package com.cse.smartnotes.listeners;

import com.cse.smartnotes.entities.Note;

public interface NotesListener {
    void onNoteClicked(Note note,int position);
}
