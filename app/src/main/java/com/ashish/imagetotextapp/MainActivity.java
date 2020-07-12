package com.ashish.imagetotextapp;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextDetector;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private ImageView mImageView;
    private Button mTextButton;
    private Button mFaceButton;
    private Button mCaptureImage;
    private TextView mdisplayText;
    private Bitmap imageBitmap;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Set the color of the action bar
        ActionBar actionBar;
        actionBar = getSupportActionBar();
        ColorDrawable colorDrawable
                = new ColorDrawable(Color.parseColor("#8BC34A"));
        actionBar.setBackgroundDrawable(colorDrawable);

        mdisplayText=findViewById(R.id.display_txt);
        mImageView = findViewById(R.id.image_view);
        mCaptureImage=findViewById(R.id.capimg);
        mTextButton = findViewById(R.id.detectxt);
        mFaceButton = findViewById(R.id.detectFace);
         checkPermission();
        mCaptureImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    TakePictureIntent();
                    mdisplayText.setText("");
                }
                catch (Exception e)
                {
                    showSettingsDialog();
                }
            }
        });
        mTextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                        detectTextImage();
                }
                catch (Exception e){
                    Toast.makeText(MainActivity.this, "Error : Capture picture first" , Toast.LENGTH_SHORT).show();
                }
            }
        });
        mFaceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    detectFaceImage();
                }
                catch (Exception e)
                {
                    Toast.makeText(MainActivity.this, "Error : Capture picture first" , Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void TakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }
    //The following code retrieves this image and displays it in an ImageView
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            imageBitmap = (Bitmap) extras.get("data");
            mImageView.setImageBitmap(imageBitmap);
        }
    }
    private  void detectTextImage(){
        FirebaseVisionImage firebaseVisionImage=FirebaseVisionImage.fromBitmap(imageBitmap);
        FirebaseVisionTextDetector textDetector=FirebaseVision.getInstance().getVisionTextDetector();

        textDetector.detectInImage(firebaseVisionImage)
                .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                    @Override
                    public void onSuccess(FirebaseVisionText firebaseVisionText) {
                        displayTextFromImage(firebaseVisionText);                            }
                }
                )
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                            Toast.makeText(MainActivity.this, "Error : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            Log.d("Error :",e.getMessage());
        }});
    }

    private void displayTextFromImage(FirebaseVisionText firebaseVisionText) {
        String text = "";
        try {
            List<FirebaseVisionText.Block> blockList = firebaseVisionText.getBlocks();
            if (blockList.size() == 0) {
                mdisplayText.setText("No Text detected");

            } else {
                for (FirebaseVisionText.Block block : blockList) {
                    text += block.getText();
                    text += "\n";
                }
                mdisplayText.setText(text);
            }
        }
        catch (Exception e)
        {
            Toast.makeText(MainActivity.this,e.getMessage(),Toast.LENGTH_LONG).show();
        }
    }

    private void detectFaceImage() {

//Create a FirebaseVisionFaceDetectorOptions object//

        FirebaseVisionFaceDetectorOptions options = new FirebaseVisionFaceDetectorOptions.Builder()

//Set the mode type; I’m using FAST_MODE//
                .setModeType(FirebaseVisionFaceDetectorOptions.FAST_MODE)
//Run additional classifiers for characterizing facial features//
                .setClassificationType(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
//Detect all facial landmarks//
                .setLandmarkType(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
//Set the smallest desired face size//
                .setMinFaceSize(0.1f)
//Disable face tracking//
                .setTrackingEnabled(false)
                .build();

        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(imageBitmap);
        FirebaseVisionFaceDetector detector = FirebaseVision.getInstance().getVisionFaceDetector(options);
        detector.detectInImage(image).addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionFace>>() {
            @Override
            public void onSuccess(List<FirebaseVisionFace> faces) {
                mdisplayText.setText(runFaceRecog(faces));
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure
                    (@NonNull Exception exception) {
                // Task failed with an exception
                Toast.makeText(MainActivity.this, "Error : " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                Log.d("Error :",exception.getMessage());
            }
        });
    }

    private String runFaceRecog(List<FirebaseVisionFace> faces) {
        if(!faces.isEmpty())
        {
            StringBuilder result = new StringBuilder();
            float smilingProbability = 0;
            float rightEyeOpenProbability = 0;
            float leftEyeOpenProbability = 0;

            for (FirebaseVisionFace face : faces) {

//Retrieve the probability that the face is smiling//

                if (face.getSmilingProbability() !=

//Check that the property was not un-computed//

                        FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                    smilingProbability = face.getSmilingProbability();
                }

//Retrieve the probability that the right eye is open//

                if (face.getRightEyeOpenProbability() != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                    rightEyeOpenProbability = face.getRightEyeOpenProbability();
                }

//Retrieve the probability that the left eye is open//

                if (face.getLeftEyeOpenProbability() != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                    leftEyeOpenProbability = face.getLeftEyeOpenProbability();
                }

//Print “Smile:” to the TextView//

                result.append("Smile: ");

//If the probability is 0.5 or higher...//

                if (smilingProbability > 0.5) {

//...print the following//

                    result.append("Yes \nProbability: " + smilingProbability);

//If the probability is 0.4 or lower...//

                } else {

//...print the following//

                    result.append("No");
                }

                result.append("\n\nRight eye: ");

//Check whether the right eye is open and print the results//

                if (rightEyeOpenProbability > 0.5) {
                    result.append("Open \nProbability: " + rightEyeOpenProbability);
                } else {
                    result.append("Close");
                }

                result.append("\n\nLeft eye: ");

//Check whether the left eye is open and print the results//

                if (leftEyeOpenProbability > 0.5) {
                    result.append("Open \nProbability: " + leftEyeOpenProbability);
                } else {
                    result.append("Close");
                }
                result.append("\n\n");
            }
            return result.toString();
        }
        else
        return "No Face detected";
    }

    public  void checkPermission() {
        Dexter.withActivity(this)
                .withPermission(Manifest.permission.CAMERA)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                        //Normal Functional if user Allow permission
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {

                        // check for permanent denial of permission
                        if (permissionDeniedResponse.isPermanentlyDenied()) {
                            showSettingsDialog();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                        //to ask for permission until user give it
                           permissionToken.continuePermissionRequest();
                    }
                }).check();

    }

        /**
         * Showing Alert Dialog with Settings option
         * Navigates user to app settings
         * NOTE: Keep proper title and message depending on your app
         */
        private void showSettingsDialog() {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Need Permissions");
            builder.setMessage("This app needs permission to open camera. You can grant the permission in app settings.");
            builder.setPositiveButton("GOTO SETTINGS", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                    openSettings();
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            builder.show();

        }

        // navigating user to app settings
        private void openSettings() {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivityForResult(intent, 101);
        }
}