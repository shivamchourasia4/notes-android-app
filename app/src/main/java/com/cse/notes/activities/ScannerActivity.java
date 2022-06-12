package com.cse.notes.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.cse.notes.R;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.TextRecognizerOptions;


public class ScannerActivity extends AppCompatActivity {

    public static final int REQUEST_CODE_SAVE_IMAGE_FROM_TEXT = 33;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    private ImageView captureIV;
    private Button snapBtn, detectBtn;
    private View backBtn;
    private Bitmap imageBitmap;
    private String blockText = "";
    private String capturedString = "";
    private boolean isDetectable = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);

        captureIV = findViewById(R.id.imageView);
        snapBtn = findViewById(R.id.snapBtn);
        detectBtn = findViewById(R.id.detectBtn);
        backBtn = findViewById(R.id.scanBack);

        detectBtn.setOnClickListener(v -> detectText());

        snapBtn.setOnClickListener(v -> {
            if (checkPermission()) {
                captureImage();
            } else {
                requestPermission();
            }
        });

        backBtn.setOnClickListener(view -> {
            onBackPressed();
        });
        if (!isDetectable) {
            detectBtn.setVisibility(View.GONE);
        }
    }

    //check and req permission
    private boolean checkPermission() {
        int cameraPermission = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA);
        return cameraPermission == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        int REQUEST_CODE = 200;
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CODE);
    }

    private void captureImage() {
        Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePicture.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePicture, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0) {
            boolean cameraPermission = grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (cameraPermission) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
                captureImage();
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            imageBitmap = (Bitmap) extras.get("data");
            captureIV.setImageBitmap(imageBitmap);
            isDetectable = true;
            detectBtn.setVisibility(View.VISIBLE);
        }

        if (requestCode == REQUEST_CODE_SAVE_IMAGE_FROM_TEXT && resultCode == RESULT_OK) {
            Intent intent = new Intent();
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    }

    private void detectText() {
        InputImage img = InputImage.fromBitmap(imageBitmap, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        Task<Text> result = recognizer.process(img).addOnSuccessListener(new OnSuccessListener<Text>() {
            @Override
            public void onSuccess(@NonNull Text text) {
                StringBuilder result = new StringBuilder();
                for (Text.TextBlock block : text.getTextBlocks()) {
                    blockText = blockText + " " + block.getText();
                    Point[] blockCornerPoint = block.getCornerPoints();
                    Rect blockFrame = block.getBoundingBox();
                    for (Text.Line line : block.getLines()) {
                        String lineText = line.getText();
                        Point[] lineCornerPoint = line.getCornerPoints();
                        Rect lineRect = line.getBoundingBox();
                        for (Text.Element element : line.getElements()) {
                            String elementText = element.getText();
                            result.append(elementText);
                        }
                        capturedString = blockText;
                    }
                }
                if (capturedString == "") {
                    Toast.makeText(ScannerActivity.this, "Text Not Found In Image!", Toast.LENGTH_LONG).show();
                    snapBtn.setText("Retake Image");
                } else {
                    Intent intent = new Intent(ScannerActivity.this, CreateNoteActivity.class);
                    intent.putExtra("capturedForNote", capturedString);
                    startActivityForResult(intent, REQUEST_CODE_SAVE_IMAGE_FROM_TEXT);
                }
            }
        }).addOnFailureListener(e -> Toast.makeText(ScannerActivity.this, "Failed to detect text from image!", Toast.LENGTH_SHORT).show());

    }
}