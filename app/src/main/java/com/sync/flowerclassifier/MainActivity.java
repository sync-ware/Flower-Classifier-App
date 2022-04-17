package com.sync.flowerclassifier;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;;
import android.os.Bundle;
import android.util.Size;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.camera.view.PreviewView;

import com.google.android.material.chip.Chip;
import com.google.common.util.concurrent.ListenableFuture;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.task.core.BaseOptions;
import org.tensorflow.lite.task.vision.classifier.Classifications;
import org.tensorflow.lite.task.vision.classifier.ImageClassifier;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity {

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private PreviewView previewView;
    private ImageCapture imageCapture;
    private TextView topResult;
    private Chip topChip;
    private ImageView topImage;
    private CardView topImageCard;
    private TextView secondResult;
    private Chip secondChip;
    private ImageView secondImage;
    private CardView secondImageCard;
    private TextView thirdResult;
    private Chip thirdChip;
    private ImageView thirdImage;
    private CardView thirdImageCard;
    private Chip infChip;
    private ImageClassifier imageClassifier = null;
    private ImageView imagePreview;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
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
        Executor mainExecutor = ContextCompat.getMainExecutor(this);

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

        topResult = findViewById(R.id.result_1);
        secondResult = findViewById(R.id.result_2);
        thirdResult = findViewById(R.id.result_3);

        topChip = findViewById(R.id.chip_1);
        secondChip = findViewById(R.id.chip_2);
        thirdChip = findViewById(R.id.chip_3);
        infChip = findViewById(R.id.chip_inf);

        topImage = findViewById(R.id.image_1);
        topImageCard = findViewById(R.id.card_image_1);
        secondImage = findViewById(R.id.image_2);
        secondImageCard = findViewById(R.id.card_image_2);
        thirdImage = findViewById(R.id.image_3);
        thirdImageCard = findViewById(R.id.card_image_3);

        View div1 = findViewById(R.id.div_1);
        View div2 = findViewById(R.id.div_2);
        View div3 = findViewById(R.id.div_3);

        imagePreview = findViewById(R.id.image_preview);

        ImageClassifier.ImageClassifierOptions options =
                ImageClassifier.ImageClassifierOptions.builder()
                        .setBaseOptions(BaseOptions.builder().useGpu().build())
                        .setMaxResults(3)
                        .build();
        try {

            imageClassifier = ImageClassifier.createFromFileAndOptions(
                    this, "InceptionV3.tflite", options);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ImageProcessor imageProcessor =
                new ImageProcessor.Builder()
                        .add(new ResizeOp(299, 299, ResizeOp.ResizeMethod.BILINEAR))
                        .build();

        Button captureBtn = findViewById(R.id.capture_button);
        Button clearBtn = findViewById(R.id.clear_button);
        captureBtn.setOnClickListener(view -> imageCapture.takePicture(mainExecutor,
                new ImageCapture.OnImageCapturedCallback() {

                    @Override
                    public void onCaptureSuccess(ImageProxy image){
                        // Initialization

                        captureBtn.setVisibility(View.GONE);
                        clearBtn.setVisibility(View.VISIBLE);

                        Instant startTime = Instant.now();
                        Bitmap bitmap = convertImageProxyToBitmap(image);
                        imagePreview.setImageBitmap(bitmap);
                        imagePreview.setVisibility(View.VISIBLE);
                        previewView.setVisibility(View.INVISIBLE);

                        TensorImage inputImage = new TensorImage(DataType.FLOAT32);

                        inputImage.load(bitmap);
                        inputImage = imageProcessor.process(inputImage);

                        List<Classifications> results = imageClassifier.classify(inputImage);

                        Instant endTime = Instant.now();
                        Duration timeElapsed = Duration.between(startTime, endTime);
                        runOnUiThread(() -> {

                            String result1 = results.get(0).getCategories().get(0).getLabel();
                            String result2 = results.get(0).getCategories().get(1).getLabel();
                            String result3 = results.get(0).getCategories().get(2).getLabel();

                            topResult.setText(result1);
                            secondResult.setText(result2);
                            thirdResult.setText(result3);

                            topChip.setVisibility(View.VISIBLE);
                            secondChip.setVisibility(View.VISIBLE);
                            thirdChip.setVisibility(View.VISIBLE);
                            infChip.setVisibility(View.VISIBLE);

                            div1.setVisibility(View.VISIBLE);
                            div2.setVisibility(View.VISIBLE);
                            div3.setVisibility(View.VISIBLE);

                            String result1Res = result1.replaceAll("\\s|[-]", "_");
                            String result2Res = result2.replaceAll("\\s|[-]", "_");
                            String result3Res = result3.replaceAll("\\s|[-]", "_");

                            topImageCard.setVisibility(View.VISIBLE);
                            topImage.setImageDrawable(getDrawable(getResId(result1Res, R.drawable.class)));

                            secondImageCard.setVisibility(View.VISIBLE);
                            secondImage.setImageDrawable(getDrawable(getResId(result2Res, R.drawable.class)));

                            thirdImageCard.setVisibility(View.VISIBLE);
                            thirdImage.setImageDrawable(getDrawable(getResId(result3Res, R.drawable.class)));

                            topChip.setText(Math.ceil(results.get(0).getCategories().get(0).getScore()*100) + "%");
                            secondChip.setText(Math.ceil(results.get(0).getCategories().get(1).getScore()*100) + "%");
                            thirdChip.setText(Math.ceil(results.get(0).getCategories().get(2).getScore()*100) + "%");
                            infChip.setText(timeElapsed.toMillis() + "ms");
                        });

                        image.close();

                    }
                    @Override
                    public void onError(ImageCaptureException error) {
                        // insert your code here.
                    }
                }
        ));

        clearBtn.setOnClickListener(view -> {
            imagePreview.setVisibility(View.INVISIBLE);
            previewView.setVisibility(View.VISIBLE);

            clearBtn.setVisibility(View.GONE);
            captureBtn.setVisibility(View.VISIBLE);
        });
    }

    private Bitmap convertImageProxyToBitmap(ImageProxy image) {
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        ByteBuffer byteBuffer = image.getPlanes()[0].getBuffer();
        byteBuffer.rewind();
        byte[] bytes = new byte[byteBuffer.capacity()];
        byteBuffer.get(bytes);
        byte[] clonedBytes = bytes.clone();
        Bitmap convertedBitmap = BitmapFactory.decodeByteArray(clonedBytes, 0, clonedBytes.length);
        Bitmap rotatedBitmap = Bitmap.createBitmap(convertedBitmap, 0, 0, convertedBitmap.getWidth(), convertedBitmap.getHeight(), matrix, true);
        return rotatedBitmap;
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
                        .setTargetResolution(new Size(480, 480))
                        .setTargetRotation(Surface.ROTATION_0)
                        .build();

        Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, imageCapture, preview);



    }

    public static int getResId(String resName, Class<?> c) {

        try {
            Field idField = c.getDeclaredField(resName);
            return idField.getInt(idField);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }
}