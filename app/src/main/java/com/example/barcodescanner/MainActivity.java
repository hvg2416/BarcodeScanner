package com.example.barcodescanner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraX;
import androidx.camera.core.Preview;
import androidx.camera.core.impl.PreviewConfig;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import android.Manifest;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;

import org.w3c.dom.Text;

import java.util.List;

public class MainActivity extends AppCompatActivity implements LifecycleOwner{

    static final int REQUEST_IMAGE_CAPTURE = 1;
    ImageView imageView;
    TextView textView,hinttextView,captextView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView = (ImageView)findViewById(R.id.image_view);
        textView = (TextView)findViewById(R.id.result_text_view);
        hinttextView = (TextView)findViewById(R.id.hint_text_view);
        captextView = (TextView)findViewById(R.id.captured_text_view);
        captextView.setVisibility(View.GONE);
    }

    public void openCamera(View view) {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
        {
            requestPermissions(new String[]{Manifest.permission.CAMERA},100);
        }
        else
        {
            Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                Log.d("DEBUG_MESSAGE","AFTER CREATING INTENT");
                startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE);
                Log.d("DEBUG_MESSAGE","AFTER CALLING INTENT");
            }
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100)
        {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE);
            }
            else
            {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle bundle = data.getExtras();
            if(bundle != null)
            {
                Bitmap bitmap = (Bitmap) bundle.get("data");
                if(bitmap != null)
                {
                    Bitmap newbitmap = Bitmap.createScaledBitmap(bitmap,220,220,false);
                    imageView.setImageBitmap(bitmap);
                    hinttextView.setVisibility(View.GONE);
                    captextView.setVisibility(View.VISIBLE);
                    //textView.setText("DONE");
                    processImageUsingFirebaseVisionAPI(newbitmap);
                }
            }
        }
    }
    public void processImage(Bitmap bitmap) {
        BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(this).build();
        if(!barcodeDetector.isOperational())
        {
            textView.setText("Something wrong happened...");
            return;
        }
        //Creating bitmap of Barcode image to pass it to Frame Object
        //Bitmap bitmap = BitmapFactory.decodeResource(this.getResources(),R.drawable.puppy);
        //Creating a Frame object to pass it to barcodeDetector
        Frame frame = new Frame.Builder().setBitmap(bitmap).build();
        Log.d("DEBUG_MESSAGE","AFTER FRAME OBJECT CREATED");
        SparseArray<Barcode> barcodeSparseArray = barcodeDetector.detect(frame);
        if(barcodeSparseArray.size() != 0)
        {
            Log.d("DEBUG_MESSAGE","AFTER SPARSEARRAY OBJECT CREATED");
            //Iterating over barcodeSparseArray and getting required Barcode Object
            Barcode barcode = barcodeSparseArray.valueAt(0);
            Log.d("DEBUG_MESSAGE","AFTER BARCODE OBJECT CREATED");
            textView.setText(barcode.displayValue);
        }
        else
        {
            Toast.makeText(this,"Error Occurred",Toast.LENGTH_SHORT).show();
        }
    }
    public void processImageUsingFirebaseVisionAPI(Bitmap bitmap)
    {
        FirebaseVisionImage firebaseVisionImage = FirebaseVisionImage.fromBitmap(bitmap);
        FirebaseVisionBarcodeDetector firebaseVisionBarcodeDetector = FirebaseVision.getInstance().getVisionBarcodeDetector();
        firebaseVisionBarcodeDetector.detectInImage(firebaseVisionImage).addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionBarcode>>() {
            @Override
            public void onSuccess(List<FirebaseVisionBarcode> firebaseVisionBarcodes) {
                if(firebaseVisionBarcodes.size()==0)
                {
                    Toast toast = Toast.makeText(MainActivity.this,"Take Snapshot Again",Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER,0,-124);
                    toast.show();
                }
                else if(firebaseVisionBarcodes.size()==1)
                {
                    String rawValue = firebaseVisionBarcodes.get(0).getRawValue();
                    Log.d("DEBUG_MESSAGE",rawValue);
                    textView.setText("=> " + rawValue + "\n");
                    Toast toast = Toast.makeText(MainActivity.this,"Single Code Detected",Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER,0,-124);
                    toast.show();
                }
                else
                {
                    for(FirebaseVisionBarcode firebaseVisionBarcode:firebaseVisionBarcodes) {
                        String rawValue = firebaseVisionBarcode.getRawValue();
                        Log.d("DEBUG_MESSAGE",rawValue);
                        textView.append("=> " + rawValue + "\n");
                    }
                    Toast toast = Toast.makeText(MainActivity.this,"Multiple Codes Detected",Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER,0,-124);
                    toast.show();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d("DEBUG_MESSAGE","ERROR OCCURRES OnFailureListener()");
            }
        });
    }

    public void copyText(View view) {
        ClipboardManager clipboardManager = (ClipboardManager)getSystemService(this.CLIPBOARD_SERVICE);
        clipboardManager.setText(textView.getText());
        Toast.makeText(this,"Text Copied",Toast.LENGTH_SHORT).show();
    }
}