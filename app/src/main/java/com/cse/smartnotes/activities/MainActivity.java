package com.cse.smartnotes.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.cse.smartnotes.R;
import com.cse.smartnotes.adapters.NotesAdapter;
import com.cse.smartnotes.database.NotesDatabase;
import com.cse.smartnotes.databinding.ActivityMainBinding;
import com.cse.smartnotes.entities.Note;
import com.cse.smartnotes.listeners.NotesListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements NotesListener {
    public static final int REQUEST_CODE_ADD_NOTE = 1; // to add note
    public static final int REQUEST_CODE_UPDATE_NOTE = 2; // to update note
    public static final int REQUEST_CODE_SHOW_NOTE = 3; // to display all note
    ActivityMainBinding binding;
    private List<Note> noteList;
    private NotesAdapter notesAdapter;

    private int noteClickedPosition = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.imageAddNoteMain.setOnClickListener(view -> {
            startActivityForResult(new Intent(getApplicationContext(), CreateNoteActivity.class), REQUEST_CODE_ADD_NOTE);
        });

        binding.notesRecyclerView.setLayoutManager(
                new StaggeredGridLayoutManager(2,
                        StaggeredGridLayoutManager.VERTICAL)
        );

        // Quick Actions checklist
        View quickActions = findViewById(R.id.layoutQuickActions);
        View nav = quickActions.findViewById(R.id.checkLists);
        nav.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, CreateNoteActivity.class);
            intent.putExtra("addChecklist", true);
            startActivityForResult(intent, REQUEST_CODE_ADD_NOTE);
        });
        // Quick Action Draw
        View drawAction = quickActions.findViewById(R.id.draw);
        drawAction.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, DrawActivity.class);
            startActivity(intent);
        });
        // Quick Actions ML Scanner
        View textScan = quickActions.findViewById(R.id.imageToText);
        textScan.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, ScannerActivity.class);
            startActivityForResult(intent, REQUEST_CODE_ADD_NOTE);
        });

        // Quick Actions ML Speech
        View speechScan = quickActions.findViewById(R.id.speech);
        speechScan.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, SpeechActivity.class);
            startActivityForResult(intent, REQUEST_CODE_ADD_NOTE);
        });

        noteList = new ArrayList<>();
        notesAdapter = new NotesAdapter(noteList, this);
        binding.notesRecyclerView.setAdapter(notesAdapter);

        // this is called on OnCreate. So, we have to fetch and display all notes.
        getNotes(REQUEST_CODE_SHOW_NOTE, false);

        binding.inputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                notesAdapter.cancelTimer();
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (noteList.size() != 0) {
                    notesAdapter.searchNotes(editable.toString());
                }
            }
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }


    @Override
    public void onNoteClicked(Note note, int position) {
        noteClickedPosition = position;
        Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
        intent.putExtra("isViewOrUpdate", true);
        intent.putExtra("note", note);
        startActivityForResult(intent, REQUEST_CODE_UPDATE_NOTE);
    }

    private void getNotes(final int requestCode, final boolean isNoteDeleted) {
        class GetNotesTask extends AsyncTask<Void, Void, List<Note>> {
            @Override
            protected List<Note> doInBackground(Void... voids) {
                return NotesDatabase.getDatabase(getApplicationContext()).noteDao().getAllNotes();
            }

            @Override
            protected void onPostExecute(List<Note> notes) {
                super.onPostExecute(notes);
                // we are adding all notes from database to noteList
                // and notify adapter about the new data set.
                if (requestCode == REQUEST_CODE_SHOW_NOTE) {
                    noteList.addAll(notes);
                    notesAdapter.notifyDataSetChanged();
                } else if (requestCode == REQUEST_CODE_ADD_NOTE) {
                    // request code is REQUEST_CODE_ADD_NOTE so we are adding an only first note(i.e, newly added)
                    // from the database to noteList and notify the adapter for the newly inserted item
                    // and scrolling recycler view to the top.
                    noteList.add(0, notes.get(0));
                    notesAdapter.notifyItemInserted(0);
                    binding.notesRecyclerView.smoothScrollToPosition(0);
                } else if (requestCode == REQUEST_CODE_UPDATE_NOTE) {
                    noteList.remove(noteClickedPosition);

                    // first we remove note from the list
                    // then we check whether the note is deleted or not.
                    // if deleted -> notify the adapter about it
                    // if not deleted -> then it must be updated that's why we are adding
                    // a newly updated note to that same position where we removed and notifying
                    // adapter about item change.
                    if (isNoteDeleted) {
                        notesAdapter.notifyItemRemoved(noteClickedPosition);
                    } else {
                        noteList.add(noteClickedPosition, notes.get(noteClickedPosition));
                        notesAdapter.notifyItemChanged(noteClickedPosition);
                    }
                }

                 /* if (noteList.size() == 0) {
                    noteList.addAll(notes);
                    notesAdapter.notifyDataSetChanged();
                } else {
                    noteList.add(0, notes.get(0));
                    notesAdapter.notifyItemInserted(0);
                }
                binding.notesRecyclerView.smoothScrollToPosition(0); */
            }
        }
        new GetNotesTask().execute();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ADD_NOTE && resultCode == RESULT_OK) {
            getNotes(REQUEST_CODE_ADD_NOTE, false);
            // also passed false as note is note deleted!
            // this is called from onActivityResult() and we check the current
            // request code is for add note and the result is RESULT_OK.
            // Basically, a new note is added from CreateNote activity and its result is
            // sent back to this activity that's why we are passing REQUEST_CODE_ADD_NOTE to it.
        } else if (requestCode == REQUEST_CODE_UPDATE_NOTE && resultCode == RESULT_OK) {
            if (data != null) {
                getNotes(REQUEST_CODE_UPDATE_NOTE, data.getBooleanExtra("isNoteDeleted", false));
                // Here, we are updating already available note from the database, and it may be possible
                // that note gets deleted therefore as a parameter isNoteDeleted, we are passing value from CreateNoteActivity,
                // whether the note is deleted or not using intent data with key "isNoteDeleted"
            }
        }
    }
}