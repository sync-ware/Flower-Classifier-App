package com.sync.flowerclassifier;

import static android.hardware.SensorManager.getOrientation;
import static java.lang.Math.min;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Bundle;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.util.Size;
import android.view.Surface;
import android.view.View;
import android.widget.TextView;

import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.camera.view.PreviewView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.odml.image.MlImage;
import com.google.common.util.concurrent.ListenableFuture;
import com.sync.flowerclassifier.ml.Model;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.task.core.BaseOptions;
import org.tensorflow.lite.task.core.vision.ImageProcessingOptions;
import org.tensorflow.lite.task.vision.classifier.Classifications;
import org.tensorflow.lite.task.vision.classifier.ImageClassifier;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private PreviewView previewView;
    private ImageCapture imageCapture;
    private TextView topResult;

    private ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permission is granted. Continue the action or workflow in your
                    // app.

                } else {
                    // Explain to the user that the feature is unavailable because the
                    // features requires a permission that the user has denied. At the
                    // same time, respect the user's decision. Don't link to system
                    // settings in an effort to convince the user to change their
                    // decision.
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED) {
            // You can use the API that requires the permission.
            activateCamera();
        }


//        else if (shouldShowRequestPermissionRationale()) {
//            // In an educational UI, explain to the user why your app requires this
//            // permission for a specific feature to behave as expected. In this UI,
//            // include a "cancel" or "no thanks" button that allows the user to
//            // continue using your app without granting the permission.
//            showInContextUI(...);
//        }
        else {
            // You can directly ask for the permission.
            // The registered ActivityResultCallback gets the result of this request.
            requestPermissionLauncher.launch(
                    Manifest.permission.CAMERA);
            activateCamera();
        }

        //topResult = findViewById(R.id.top_result_text);

        FloatingActionButton fab = findViewById(R.id.floating_action_button);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                imageCapture.takePicture(ContextCompat.getMainExecutor(MainActivity.this),
                        new ImageCapture.OnImageCapturedCallback() {

                            @Override
                            public void onCaptureSuccess(ImageProxy image){
                                // Initialization
                                ImageClassifier.ImageClassifierOptions options =
                                        ImageClassifier.ImageClassifierOptions.builder()
                                                .setBaseOptions(BaseOptions.builder().useGpu().build())
                                                .setMaxResults(3)
                                                .build();
                                ImageClassifier imageClassifier =
                                        null;


                                try {

                                    imageClassifier = ImageClassifier.createFromFileAndOptions(
                                            MainActivity.this, "model.tflite", options);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                Bitmap bitmap = convertImageProxyToBitmap(image);

                                ImageProcessor imageProcessor =
                                        new ImageProcessor.Builder()
                                                .add(new ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
                                                .build();

// Create a TensorImage object. This creates the tensor of the corresponding
// tensor type (uint8 in this case) that the TensorFlow Lite interpreter needs.
                                TensorImage inputImage = new TensorImage(DataType.UINT8);

// Analysis code for every frame
// Preprocess the image
                                inputImage.load(bitmap);
                                inputImage = imageProcessor.process(inputImage);

                                List<Classifications> results = imageClassifier.classify(inputImage);
                                topResult.setText(results.get(0).getCategories().get(0).getLabel());
                            }
                            @Override
                            public void onError(ImageCaptureException error) {
                                // insert your code here.
                            }
                        }
                );

            }
        });
    }

    private Bitmap convertImageProxyToBitmap(ImageProxy image) {
        ByteBuffer byteBuffer = image.getPlanes()[0].getBuffer();
        byteBuffer.rewind();
        byte[] bytes = new byte[byteBuffer.capacity()];
        byteBuffer.get(bytes);
        byte[] clonedBytes = bytes.clone();
        return BitmapFactory.decodeByteArray(clonedBytes, 0, clonedBytes.length);
    }

    private void activateCamera(){
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                // No errors need to be handled for this Future.
                // This should never be reached.
            }
        }, ContextCompat.getMainExecutor(this));
    }

    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder()
                .build();


        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();
        previewView = findViewById(R.id.previewView);
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        imageCapture =
                new ImageCapture.Builder()
                        .setTargetRotation(Surface.ROTATION_0)
                        .build();

        Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector, imageCapture, preview);



    }
}