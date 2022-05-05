package com.cse.smartnotes.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Patterns;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.cse.smartnotes.R;
import com.cse.smartnotes.broadcast.ReminderBroadcast;
import com.cse.smartnotes.database.NotesDatabase;
import com.cse.smartnotes.databinding.ActivityCreateNoteBinding;
import com.cse.smartnotes.entities.Note;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CreateNoteActivity extends AppCompatActivity implements DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {

    public static final int SELECT_PICTURE = 200;
    private static final int REQUEST_CODE_STORAGE_PERMISSION = 1;
    private static final int REQUEST_CODE_SELECT_IMAGE = 2;
    ActivityCreateNoteBinding binding;
    Calendar now = Calendar.getInstance();
    private String selectedNoteColor;
    private String selectedImagePath;
    private TimePickerDialog tpd;
    private DatePickerDialog dpd;
    private Note alreadyAvailableNote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateNoteBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.imageBack.setOnClickListener(view -> {
            hideKeyboard(binding.inputNote);
            onBackPressed();
        });

        binding.textDateTime.setText(
                new SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault())
                        .format(new Date())
        );

        binding.imageSave.setOnClickListener(view -> saveNote());

        selectedNoteColor = "#FFFFFFFF";
        selectedImagePath = "";
        setTextHintColorLight();

        if (getIntent().getBooleanExtra("isViewOrUpdate", false)) {
            alreadyAvailableNote = (Note) getIntent().getSerializableExtra("note");
            setViewOrUpdateNote();
        }

        if (getIntent().getStringExtra("capturedForNote") != null) {
            binding.inputNote.setText(getIntent().getStringExtra("capturedForNote"));
        }

        binding.removeWebURL.setOnClickListener(view -> {
            binding.textWebURL.setText(null);
            binding.layoutWebURL.setVisibility(View.GONE);
        });

        binding.imageRemoveImage.setOnClickListener(view -> {
            binding.imageNote.setImageBitmap(null);
            binding.imageNote.setVisibility(View.GONE);
            binding.imageRemoveImage.setVisibility(View.GONE);
            selectedImagePath = "";
        });

        initTools();
        setNoteBackgroundColor();
    }

    private void hideKeyboard(View p) {
        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(p.getWindowToken(), 0);
    }

    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager)getSystemService(this.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,InputMethodManager.HIDE_IMPLICIT_ONLY);
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
                    hideKeyboard(v);
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }


    private void setViewOrUpdateNote() {
        binding.inputNoteTitle.setText(alreadyAvailableNote.getTitle());
        binding.inputNoteSubtitle.setText(alreadyAvailableNote.getSubtitle());
        binding.inputNote.setText(alreadyAvailableNote.getNoteText());
        binding.textDateTime.setText(alreadyAvailableNote.getDateTime());

        if (alreadyAvailableNote.getImagePath() != null && !alreadyAvailableNote.getImagePath().trim().isEmpty()) {
            binding.imageNote.setImageBitmap(BitmapFactory.decodeFile(alreadyAvailableNote.getImagePath()));
            binding.imageNote.setVisibility(View.VISIBLE);
            binding.imageRemoveImage.setVisibility(View.VISIBLE);
            selectedImagePath = alreadyAvailableNote.getImagePath();
        }

        if (alreadyAvailableNote.getWebLink() != null && !alreadyAvailableNote.getWebLink().trim().isEmpty()) {
            binding.textWebURL.setText(alreadyAvailableNote.getWebLink());
            binding.layoutWebURL.setVisibility(View.VISIBLE);
        }
    }

    private void saveNote() {
        if (binding.inputNoteTitle.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Note title can't be empty!", Toast.LENGTH_SHORT).show();
            return;
        } else if (binding.inputNoteSubtitle.getText().toString().trim().isEmpty() && binding.inputNote.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Note can't be empty", Toast.LENGTH_SHORT).show();
            return;
        }
        hideKeyboard(binding.inputNote);

        final Note note = new Note();
        note.setTitle(binding.inputNoteTitle.getText().toString());
        note.setSubtitle(binding.inputNoteSubtitle.getText().toString());
        note.setNoteText(binding.inputNote.getText().toString());
        note.setDateTime(binding.textDateTime.getText().toString());
        note.setColor(selectedNoteColor);
        note.setImagePath(selectedImagePath);

        // check if layoutWebURL is visible or not. If it is not visible
        // then Web URL is added since we have made it visible only while adding
        // Web URL from add URL dialog.

        if (binding.layoutWebURL.getVisibility() == View.VISIBLE) {
            note.setWebLink(binding.textWebURL.getText().toString());
        }

        // here we are setting id of new note from an already available note
        // as set onConflictStrategy is set to "REPLACE" in NoteDao.
        // i.e, if id of new note is already available in the DB then,
        // it will be replaced with new note and our note get updated.
        if (alreadyAvailableNote != null) {
            note.setId(alreadyAvailableNote.getId());
        }


        class SaveNoteTask extends AsyncTask<Void, Void, Void> {
            @Override
            protected Void doInBackground(Void... voids) {
                NotesDatabase.getDatabase(getApplicationContext()).noteDao().insertNote(note);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                Intent intent = new Intent();
                setResult(RESULT_OK, intent);
                finish();
            }
        }
        new SaveNoteTask().execute();
    }

    private void initTools() {
        final LinearLayout layoutTools = findViewById(R.id.layoutTools);
        final BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(layoutTools);
        layoutTools.findViewById(R.id.textTools).setOnClickListener(view -> {
            if (bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            } else {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });

        final ImageView imageColor1 = layoutTools.findViewById(R.id.imageColor1);
        final ImageView imageColor2 = layoutTools.findViewById(R.id.imageColor2);
        final ImageView imageColor3 = layoutTools.findViewById(R.id.imageColor3);
        final ImageView imageColor4 = layoutTools.findViewById(R.id.imageColor4);
        final ImageView imageColor5 = layoutTools.findViewById(R.id.imageColor5);

        layoutTools.findViewById(R.id.viewColor1).setOnClickListener(view -> {
            selectedNoteColor = "#FFFFFFFF";
            imageColor1.setImageResource(R.drawable.ic_baseline_done);
            imageColor2.setImageResource(0);
            imageColor3.setImageResource(0);
            imageColor4.setImageResource(0);
            imageColor5.setImageResource(0);
            setNoteBackgroundColor();
            setTextHintColorLight();
        });

        layoutTools.findViewById(R.id.viewColor2).setOnClickListener(view -> {
            selectedNoteColor = "#fff59d";
            imageColor2.setImageResource(R.drawable.ic_baseline_done);
            imageColor1.setImageResource(0);
            imageColor3.setImageResource(0);
            imageColor4.setImageResource(0);
            imageColor5.setImageResource(0);
            setNoteBackgroundColor();
            setTextHintColorDark();
        });

        layoutTools.findViewById(R.id.viewColor3).setOnClickListener(view -> {
            selectedNoteColor = "#ffccbc";
            imageColor3.setImageResource(R.drawable.ic_baseline_done);
            imageColor2.setImageResource(0);
            imageColor1.setImageResource(0);
            imageColor4.setImageResource(0);
            imageColor5.setImageResource(0);
            setNoteBackgroundColor();
            setTextHintColorDark();
        });

        layoutTools.findViewById(R.id.viewColor4).setOnClickListener(view -> {
            selectedNoteColor = "#b3e5fc";
            imageColor4.setImageResource(R.drawable.ic_baseline_done);
            imageColor2.setImageResource(0);
            imageColor3.setImageResource(0);
            imageColor1.setImageResource(0);
            imageColor5.setImageResource(0);
            setNoteBackgroundColor();
            setTextHintColorDark();
        });

        layoutTools.findViewById(R.id.viewColor5).setOnClickListener(view -> {
            selectedNoteColor = "#e1bee7";
            imageColor5.setImageResource(R.drawable.ic_baseline_done);
            imageColor2.setImageResource(0);
            imageColor3.setImageResource(0);
            imageColor4.setImageResource(0);
            imageColor1.setImageResource(0);
            setNoteBackgroundColor();
            setTextHintColorDark();
        });

        if (alreadyAvailableNote != null && alreadyAvailableNote.getColor() != null && !alreadyAvailableNote.getColor().trim().isEmpty()) {
            switch (alreadyAvailableNote.getColor()) {
                case "#fff59d":
                    layoutTools.findViewById(R.id.viewColor2).performClick();
                    break;
                case "#ffccbc":
                    layoutTools.findViewById(R.id.viewColor3).performClick();
                    break;
                case "#b3e5fc":
                    layoutTools.findViewById(R.id.viewColor4).performClick();
                    break;
                case "#e1bee7":
                    layoutTools.findViewById(R.id.viewColor5).performClick();
                    break;
            }
        }

        layoutTools.findViewById(R.id.layoutAddImage).setOnClickListener(view -> {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            if (ContextCompat.checkSelfPermission(
                    getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        CreateNoteActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_CODE_STORAGE_PERMISSION
                );
            } else {
                selectImage();
            }
        });

        layoutTools.findViewById(R.id.layoutAddUrl).setOnClickListener(view -> {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            showAddURLDialog();
        });

        layoutTools.findViewById(R.id.layoutAddReminder).setOnClickListener(view -> {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            dpd = DatePickerDialog.newInstance(
                    CreateNoteActivity.this,
                    now.get(Calendar.YEAR),
                    now.get(Calendar.MONTH),
                    now.get(Calendar.DAY_OF_MONTH)
            );

            tpd = TimePickerDialog.newInstance(
                    CreateNoteActivity.this,
                    now.get(Calendar.HOUR_OF_DAY),
                    now.get(Calendar.MINUTE),
                    now.get(Calendar.SECOND),
                    false
            );
            dpd.setAccentColor("#4E0D3A");
            tpd.setAccentColor("#4E0D3A");

            dpd.setMinDate(Calendar.getInstance());
            tpd.setMinTime(Calendar.getInstance().get(Calendar.HOUR_OF_DAY), Calendar.getInstance().get(Calendar.MINUTE), 0);

            dpd.show(getSupportFragmentManager(), "DatePickerDialog");


            createNotificationChannel();

        });

        if (alreadyAvailableNote != null) {
            layoutTools.findViewById(R.id.layoutDeleteNote).setVisibility(View.VISIBLE);
            layoutTools.findViewById(R.id.layoutDeleteNote).setOnClickListener(view -> {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                showDeleteNoteDialog();
            });

        }
    }

    private void setTextHintColorDark() {
        binding.inputNoteTitle.setHintTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorHintDark));
        binding.inputNoteSubtitle.setHintTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorHintDark));
        binding.inputNote.setHintTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorHintDark));
    }

    private void setTextHintColorLight() {
        binding.inputNoteTitle.setHintTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorHint));
        binding.inputNoteSubtitle.setHintTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorHint));
        binding.inputNote.setHintTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorHint));
    }

    // call onClickListener
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "NoteReminderChannel";
            String description = "Channel for Note Reminder";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("notifyReminder", name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }


    @Override
    public void onDateSet(DatePickerDialog view, int year, int month, int day) {
        now.set(Calendar.YEAR, year);
        now.set(Calendar.MONTH, month);
        now.set(Calendar.DAY_OF_MONTH, day);

        tpd.show(getSupportFragmentManager(), "TimePickerDialog");
    }

    @Override
    public void onTimeSet(TimePickerDialog view, int hrs, int min, int sec) {
        now.set(Calendar.HOUR_OF_DAY, hrs);
        now.set(Calendar.MINUTE, min);
        now.set(Calendar.SECOND, 0);
        if (binding.inputNoteTitle.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Note title can't be empty!", Toast.LENGTH_SHORT).show();
            return;
        } else if (binding.inputNoteSubtitle.getText().toString().trim().isEmpty() && binding.inputNote.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Note can't be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        Long reminderInMillis = now.getTimeInMillis();

        //show Alert and Set Reminder based on response
        showAlertForReminderConfirm(reminderInMillis, binding.inputNoteTitle.getText().toString(), binding.inputNoteSubtitle.getText().toString());

    }


    private void showAlertForReminderConfirm(Long time, String title, String message) {
        Date date = new Date(time);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(CreateNoteActivity.this);
        alertDialogBuilder.setTitle("Schedule Notification?");
        alertDialogBuilder
                .setMessage("For " + title + "\nAt " + new SimpleDateFormat("E, dd MMM yyyy hh:mm a z", Locale.getDefault())
                        .format(date))
                .setCancelable(false)
                .setIcon(R.drawable.ic_baseline_notifications)
                .setPositiveButton("Yes", (dialog, id) -> setReminder(time))
                .setNegativeButton("No", (dialog, id) -> {
                    // if this button is clicked, just close
                    // the dialog box and do nothing
                    dialog.cancel();
                });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    // call on date and time set
    private void setReminder(Long reminderTimeInMillis) {
        createNotificationChannel();

        Toast.makeText(this, "Reminder Set Successfully!", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(getApplicationContext(), ReminderBroadcast.class);
        intent.putExtra("titleExtra", binding.inputNoteTitle.getText().toString());
        intent.putExtra("messageExtra", binding.inputNoteSubtitle.getText().toString());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        alarmManager.set(AlarmManager.RTC_WAKEUP,
                reminderTimeInMillis, pendingIntent);
    }

    private void showDeleteNoteDialog() {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(CreateNoteActivity.this);
        alertDialogBuilder.setTitle("Delete Note");
        alertDialogBuilder
                .setMessage("Are You Sure ?")
                .setCancelable(false)
                .setIcon(R.drawable.ic_baseline_delete)
                .setPositiveButton("Yes", (dialog, id) -> {
                    class DeleteNoteTask extends AsyncTask<Void, Void, Void> {

                        @Override
                        protected Void doInBackground(Void... voids) {
                            NotesDatabase.getDatabase(getApplicationContext()).noteDao().deleteNote(alreadyAvailableNote);
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Void aVoid) {
                            super.onPostExecute(aVoid);
                            Intent intent = new Intent();
                            intent.putExtra("isNoteDeleted", true);
                            setResult(RESULT_OK, intent);
                            finish();
                        }
                    }

                    new DeleteNoteTask().execute();
                })
                .setNegativeButton("No", (dialog, id) -> {
                    // if this button is clicked, just close
                    // the dialog box and do nothing
                    dialog.cancel();
                });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();

    }

    private void setNoteBackgroundColor() {
        binding.noteContainer.setBackgroundColor(Color.parseColor(selectedNoteColor));
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGE);
        } else {
            Toast.makeText(this, "Opening Image Picker...", Toast.LENGTH_SHORT).show();
            Intent i = new Intent();
            i.setType("image/*");
            i.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(i, "Select Picture"), SELECT_PICTURE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                selectImage();
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SELECT_IMAGE && resultCode == RESULT_OK) {
            if (data != null) {
                Uri selectedImageUri = data.getData();
                System.err.println(selectedImageUri.toString());
                if (selectedImageUri != null) {
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        binding.imageNote.setImageBitmap(bitmap);
                        binding.imageNote.setVisibility(View.VISIBLE);
                        binding.imageRemoveImage.setVisibility(View.VISIBLE);

                        selectedImagePath = getPathFromUri(selectedImageUri);
                        Toast.makeText(this, "Image Added", Toast.LENGTH_SHORT).show();
                    } catch (Exception exception) {
                        Toast.makeText(this, exception.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
        if (requestCode == SELECT_PICTURE && resultCode == RESULT_OK) {
            // Get the url of the image from data
            if (data != null) {
                Uri selectedImageUri = data.getData();
                System.err.println(selectedImageUri.toString());
                if (null != selectedImageUri) {
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        binding.imageNote.setImageBitmap(bitmap);
                        binding.imageNote.setVisibility(View.VISIBLE);
                        binding.imageRemoveImage.setVisibility(View.VISIBLE);

                        selectedImagePath = getPathFromUri(selectedImageUri);
                        Toast.makeText(this, "Image Added", Toast.LENGTH_SHORT).show();
                    } catch (Exception exception) {
                        Toast.makeText(this, exception.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            }
        }
    }


    //content://com.android.providers.downloads.documents/document/msf%3A31
    //content://media/external_primary/images/media/24769
    private String getPathFromUri(Uri contentUri) {
        String filePath;
        Cursor cursor = getContentResolver().query(contentUri, null, null, null, null);
        if (cursor == null) {
            filePath = contentUri.getPath();
        } else {
            cursor.moveToFirst();
            int index = cursor.getColumnIndex("_data");
            filePath = cursor.getString(index);
            cursor.close();
        }
        System.out.println(filePath);
        return filePath;
    }

    @SuppressLint("RestrictedApi")
    private void showAddURLDialog() {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(CreateNoteActivity.this);
        alertDialogBuilder.setTitle("Add URL");
        // Set up the input
        final EditText inputURL = new EditText(this);
        inputURL.requestFocus();
        showKeyboard();

        // Specify the type of input expected.
        inputURL.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        alertDialogBuilder
                .setView(inputURL, 50, 0,50, 0)
                .setCancelable(false)
                .setIcon(R.drawable.ic_baseline_web)
                .setPositiveButton("Add", (dialog, id) -> {
                    if (inputURL.getText().toString().trim().isEmpty()) {
                        Toast.makeText(CreateNoteActivity.this, "Enter URL", Toast.LENGTH_SHORT).show();
                    } else if (!Patterns.WEB_URL.matcher(inputURL.getText().toString()).matches()) {
                        Toast.makeText(CreateNoteActivity.this, "Enter valid url", Toast.LENGTH_SHORT).show();
                    } else {
                        binding.textWebURL.setText(inputURL.getText().toString());
                        binding.layoutWebURL.setVisibility(View.VISIBLE);
                    }
                    hideKeyboard(inputURL);
                })
                .setNegativeButton("Cancel", (dialog, id) -> {
                    // if this button is clicked, just close
                    // the dialog box and do nothing
                    hideKeyboard(inputURL);
                    dialog.cancel();
                });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();

//        if (dialogAddURL == null) {
//            AlertDialog.Builder builder = new AlertDialog.Builder(CreateNoteActivity.this);
//            View view = LayoutInflater.from(this).inflate(
//                    R.layout.layout_add_url,
//                    findViewById(R.id.layoutAddUrlContainer)
//            );
//
//            builder.setView(view);
//
//            dialogAddURL = builder.create();
//            if (dialogAddURL.getWindow() != null) {
//                dialogAddURL.getWindow().setBackgroundDrawable(new ColorDrawable(0));
//            }
//
//            final EditText inputURL = view.findViewById(R.id.inputUrl);
//            inputURL.requestFocus();
//
//            view.findViewById(R.id.textAdd).setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    if (inputURL.getText().toString().trim().isEmpty()) {
//                        Toast.makeText(CreateNoteActivity.this, "Enter URL", Toast.LENGTH_SHORT).show();
//                    } else if (!Patterns.WEB_URL.matcher(inputURL.getText().toString()).matches()) {
//                        Toast.makeText(CreateNoteActivity.this, "Enter valid url", Toast.LENGTH_SHORT).show();
//                    } else {
//                        binding.textWebURL.setText(inputURL.getText().toString());
//                        binding.layoutWebURL.setVisibility(View.VISIBLE);
//                        dialogAddURL.dismiss();
//                    }
//                }
//            });
//
//            view.findViewById(R.id.textCancel).setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    dialogAddURL.dismiss();
//                }
//            });
//        }
//        dialogAddURL.show();
    }
}