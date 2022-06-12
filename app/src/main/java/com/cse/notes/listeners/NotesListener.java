package com.cse.notes.listeners;

import com.cse.notes.entities.Note;

public interface NotesListener {
    void onNoteClicked(Note note, int position);
}
