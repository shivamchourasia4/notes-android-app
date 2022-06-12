package com.cse.notes.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.cse.notes.entities.CheckList;
import com.cse.notes.entities.Note;

import java.util.List;

@Dao
public interface NoteDao {

    @Query("SELECT * FROM notes ORDER BY id DESC")
    List<Note> getAllNotes();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertNote(Note note);

    @Delete
    void deleteNote(Note note);

    @Query("SELECT * FROM checklist WHERE ownerNoteId IS :ownerNoteId")
    List<CheckList> getChecklistOfNote(String ownerNoteId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertChecklistInNote(CheckList checkLists);

    @Delete
    void deleteChecklistFromNote(CheckList checkList);
}
