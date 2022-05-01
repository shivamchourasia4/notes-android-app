package com.cse.smartnotes.activities;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.cse.smartnotes.R;

import java.util.ArrayList;
import java.util.Locale;

public class SpeechActivity extends AppCompatActivity {

    protected static final int RESULT_SPEECH = 1;
    private static final int REQUEST_CODE_SAVE_TEXT_FROM_SPEECH = 41;
    private TextView opText;
    private Button sayBtn,saveBtn;
    View backBtn;
    private String capturedSpeechText = "";
    private boolean isDetectable = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speech);

        opText = findViewById(R.id.speechText);

        sayBtn = findViewById(R.id.sayBtn);
        saveBtn = findViewById(R.id.saveTextFromSpeech);
        backBtn = findViewById(R.id.speechBack);
        sayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Toast.makeText(SpeechActivity.this, "Working!", Toast.LENGTH_SHORT).show();
                getSpeechResult();
            }
        });

        backBtn.setOnClickListener(view -> onBackPressed());

        saveBtn.setOnClickListener(view -> {
            saveSpeechResult();
        });

        if(!isDetectable){
            saveBtn.setVisibility(View.GONE);
        }

    }

    public void getSpeechResult(){

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Hi! Say Something...");
        try {
            startActivityForResult(intent, RESULT_SPEECH);
        }catch (ActivityNotFoundException e){
            Toast.makeText(SpeechActivity.this, "Something went wrong!", Toast.LENGTH_SHORT).show();
        }
    }

    public void saveSpeechResult(){
        Intent intent = new Intent(SpeechActivity.this, CreateNoteActivity.class);
        intent.putExtra("capturedForNote", capturedSpeechText);
        startActivityForResult(intent, REQUEST_CODE_SAVE_TEXT_FROM_SPEECH);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
                if(requestCode == RESULT_SPEECH && resultCode == RESULT_OK && data!=null){
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    opText.setText(result.get(0));
                    capturedSpeechText = result.get(0);
                    isDetectable=true;
                    saveBtn.setVisibility(View.VISIBLE);
                }
                if (requestCode == REQUEST_CODE_SAVE_TEXT_FROM_SPEECH && resultCode == RESULT_OK){
                    Intent intent = new Intent();
                    setResult(Activity.RESULT_OK,intent);
                    finish();
                }
    }
}