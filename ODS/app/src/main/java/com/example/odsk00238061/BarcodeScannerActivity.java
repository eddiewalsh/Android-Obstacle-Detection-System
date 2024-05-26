package com.example.odsk00238061;

import static com.example.odsk00238061.utils.ProjectHelper.*;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.example.odsk00238061.utils.ProjectHelper;
import com.example.odsk00238061.utils.Speaker;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class BarcodeScannerActivity extends AppCompatActivity {

    /**
     * Intent for starting speech recognition activity.
     */
    private Intent intentRecognizer;

    /**
     * SpeechRecognizer instance to perform speech recognition.
     */
    private SpeechRecognizer speechRecognizer;

    /**
     * Threshold for touch duration to consider it as a long touch (in milliseconds).
     */
    private static final int TOUCH_DURATION_THRESHOLD = 3000;

    /**
     * Handler to manage delayed execution of a Runnable.
     */
    private final Handler handler = new Handler();

    /**
     * Runnable to be executed when a long touch is detected.
     */
    private Runnable longTouchRunnable;

    /**
     * Speaker instance for text-to-speech functionality.
     */
    private Speaker speaker;

    /**
     * Camera lens facing constant for the back camera.
     */
    int camFacing = CameraSelector.LENS_FACING_BACK;

    /**
     * View for displaying camera preview.
     */
    private PreviewView previewView;

    /**
     * Future for providing the camera provider.
     */
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    /**
     * ProcessCameraProvider instance for controlling the camera lifecycle.
     */
    private ProcessCameraProvider cameraProvider;

    /**
     * Context used for various Android system operations.
     */
    private Context context;

    /**
     * BarcodeScanner instance for detecting barcodes.
     */
    private BarcodeScanner barcodeScanner;

    /**
     * Boolean flag indicating whether to get barcode data.
     */
    private boolean getBarcode = true;

    /**
     * UPC (Universal Product Code) extracted from barcode.
     */
    private String barcode_UPC = "";

    /**
     * Counter for the number of times the UPC is detected.
     */
    private Integer barcode_UPC_count = 0;

    /**
     * Registers an activity result launcher to handle permission requests for accessing the camera.
     * Upon receiving the result of the permission request, it sets up the camera if permission is granted,
     * or displays a toast message informing the user that camera permission is required if permission is denied.
     */
    private final ActivityResultLauncher<String> activityResultLauncher = registerForActivityResult(
            // Specify the contract for requesting permission
            new ActivityResultContracts.RequestPermission(),
            // Define a callback function to handle the result of the permission request
            new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean isGranted) {
                    // Check if the camera permission is granted
                    if (isGranted) {
                        // Camera permission is granted, proceed with setting up camera
                        cameraProviderFuture.addListener(() -> {
                            try {
                                // Obtain the camera provider and setup camera preview
                                cameraProvider = cameraProviderFuture.get();
                                setupCameraAndBindPreview(cameraProvider, context);
                            } catch (ExecutionException | InterruptedException e) {
                                // Log any errors that occur during camera setup
                                Log.e("CameraX Camera Provider", Objects.requireNonNull(e.getMessage()));
                            }
                        }, ContextCompat.getMainExecutor(BarcodeScannerActivity.this));
                    } else {
                        // Camera permission is not granted, display a toast message
                        Toast.makeText(BarcodeScannerActivity.this, "Camera permission is required to use the camera", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    /**
     * Initializes the BarcodeScannerActivity when it is created.
     * Sets up the layout, initializes views and context, requests necessary permissions,
     * initializes speech recognition and barcode scanning components, sets up touch listener
     * for the preview view, initializes the camera provider, and initiates TextToSpeech
     * to provide a welcome message.
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the layout for the activity
        setContentView(R.layout.activity_barcodescanner);
        // Keep the screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // Vibrate the device for 1 second
        vibrate(this, 1000);
        // Initialize views and context
        previewView = findViewById(R.id.barcodePreview);
        context = this;

        // Initialize the speaker
        speaker = new Speaker(this);

        // Obtain an instance of the camera provider
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        // Request RECORD_AUDIO permission
        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.RECORD_AUDIO}, PackageManager.PERMISSION_GRANTED);

        // Initialize speech recognition intent
        intentRecognizer = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intentRecognizer.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        // Create a speech recognizer instance
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        // Define barcode scanning options
        BarcodeScannerOptions options =
                new BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(
                                Barcode.FORMAT_UPC_A,
                                Barcode.FORMAT_UPC_E)
                        .build();

        // Create a barcode scanner instance
        barcodeScanner = BarcodeScanning.getClient(options);

        // Set up touch listener for the preview view
        previewView.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // Start a timer for long touch event
                        startLongTouchTimer();
                        break;
                    case MotionEvent.ACTION_UP:
                        // Cancel the long touch timer
                        cancelLongTouchTimer();
                        break;
                }
                return true;
            }
        });

        // Listen for camera provider initialization
        cameraProviderFuture.addListener(() -> {
            try {
                // Get the camera provider
                cameraProvider = cameraProviderFuture.get();
                // Check camera permission
                if (ContextCompat.checkSelfPermission(BarcodeScannerActivity.this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    // Launch permission request if not granted
                    activityResultLauncher.launch(Manifest.permission.CAMERA);
                } else {
                    // Proceed with setting up camera and preview
                    setupCameraAndBindPreview(cameraProvider, this);
                }
            } catch (ExecutionException | InterruptedException e) {
                // Log any errors during camera setup
                Log.e("CameraX Camera Provider", Objects.requireNonNull(e.getMessage()));
            }
        }, ContextCompat.getMainExecutor(this));

        // Initialize TextToSpeech and speak a welcome message
        TextToSpeech tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    speaker.speakText("Barcode scanner is open, hold your device over the barcode.");
                }
            }
        });
    }

    /**
     * Sets up the camera and binds the preview to display the camera feed.
     * Initializes a preview and sets its surface provider to the preview view.
     * Configures image analysis for barcode detection and sets up an analyzer
     * to process images from the camera feed.
     *
     * @param cameraProvider The camera provider instance to bind the camera to.
     * @param context        The context used for accessing resources and lifecycle.
     */
    private void setupCameraAndBindPreview(ProcessCameraProvider cameraProvider, Context context) {
        // Initialize a preview
        Preview preview = new Preview.Builder().build();

        // Configure camera selector to use the specified camera facing direction
        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(camFacing).build();

        // Set the surface provider for the preview
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // Initialize image analysis for barcode detection
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

        // Set up analyzer to process images from the camera feed
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), new ImageAnalysis.Analyzer() {
            @ExperimentalGetImage
            @Override
            public void analyze(@NonNull ImageProxy imageProxy) {
                // Get rotation information for the image
                int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
                Image image = imageProxy.getImage();
                if (image != null) {
                    // Convert the media image to InputImage for barcode detection
                    InputImage inputImage = InputImage.fromMediaImage(image, rotationDegrees);
                    // Detect barcode from the image
                    detectBarcodeFromImage(inputImage, imageProxy);
                }
            }
        });
        // Bind the camera lifecycle, camera selector, image analysis, and preview
        cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, imageAnalysis, preview);
    }

    /**
     * Detects barcodes from the provided input image using the barcode scanner.
     * If enabled, processes the input image to detect barcodes and perform actions based on the detected barcode.
     *
     * @param inputImage  The input image containing the barcode to be detected.
     * @param imageProxy  The image proxy associated with the input image.
     */
    public void detectBarcodeFromImage(InputImage inputImage, ImageProxy imageProxy) {
        // Check if barcode detection is enabled
        if (getBarcode) {
            // Process the input image to detect barcodes
            barcodeScanner.process(inputImage)
                .addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
                    @Override
                    public void onSuccess(List<Barcode> barcodes) {
                        // Check if any barcodes are detected
                        if (!barcodes.isEmpty()) {
                            // Check if the detected barcode matches the previous one
                            if(!barcode_UPC.isEmpty()){
                                // If the new barcode matches the previous one, increment the counter
                                if(barcode_UPC.equals(barcodes.get(0).getRawValue())) {
                                    barcode_UPC_count++;
                                } else {
                                    // If the new barcode is different, update the barcode and reset the count
                                    barcode_UPC = barcodes.get(0).getRawValue();
                                    barcode_UPC_count = 0;
                                }

                                // If the count threshold is reached, disable barcode detection and launch product details activity
                                if(barcode_UPC_count >= 5) {
                                    getBarcode = false;
                                    // Vibrate the device
                                    vibrate(BarcodeScannerActivity.this, 1000);

                                    // Launch the product details activity with the detected barcode
                                    Intent intent = new Intent(BarcodeScannerActivity.this, ProductDetailsActivity.class);
                                    intent.putExtra("barcode", barcodes.get(0).getRawValue());
                                    startActivity(intent);
                                }
                            } else {
                                // If no previous barcode, set the detected barcode as the current one
                                barcode_UPC = barcodes.get(0).getRawValue();
                            }
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Log any errors that occur during barcode detection
                        Log.e("BarcodeScanner", Objects.requireNonNull(e.getMessage()));
                    }
                })
                .addOnCompleteListener(new OnCompleteListener<List<Barcode>>() {
                    @Override
                    public void onComplete(@NonNull Task<List<Barcode>> task) {
                        // Close the image proxy after barcode detection is complete
                        imageProxy.close();
                        // Log completion message
                        Log.d("BarcodeScanner", "Still running");
                    }
                });
        }
    }

    /**
     * Starts a timer for long touch detection.
     * If no long touch is detected within the specified threshold, it triggers speech recognition.
     */
    private void startLongTouchTimer() {
        // Check if a long touch runnable is not already running
        if (longTouchRunnable == null) {
            // Define a new runnable to start speech recognition when the long touch threshold is reached
            longTouchRunnable = new Runnable() {
                @Override
                public void run() {
                    startSpeechRecognition();
                }
            };
            // Post the long touch runnable with a delay equal to the touch duration threshold
            handler.postDelayed(longTouchRunnable, TOUCH_DURATION_THRESHOLD);
        }
    }

    /**
     * Cancels the long touch timer if it is currently running.
     */
    private void cancelLongTouchTimer() {
        // Check if a long touch runnable is currently running
        if (longTouchRunnable != null) {
            // Remove the callbacks associated with the long touch runnable
            handler.removeCallbacks(longTouchRunnable);
            // Reset the long touch runnable to null
            longTouchRunnable = null;
        }
    }

    /**
     * Starts the speech recognition process.
     * It sets the recognition listener for speech recognition events and starts listening for speech input.
     */
    private void startSpeechRecognition() {
        // Set the recognition listener for speech recognition events
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                // Implementation for when the recognizer is ready to receive speech input
            }

            @Override
            public void onBeginningOfSpeech() {
                // Implementation for when the user starts speaking
            }

            @Override
            public void onRmsChanged(float rmsdB) {
                // Implementation for when the RMS (Root Mean Square) dB value of the input audio changes
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
                // Implementation for when the audio buffer is received during speech recognition
            }

            @Override
            public void onEndOfSpeech() {
                // Implementation for when the user stops speaking
            }

            @Override
            public void onError(int error) {
                // Implementation for when an error occurs during speech recognition
            }

            @Override
            public void onResults(Bundle results) {
                // Implementation for when speech recognition results are available
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if(matches != null) {
                    // Pass the recognized speech matches to be handled by the helper method
                    ProjectHelper.handleCommands(matches, BarcodeScannerActivity.this, speaker, context);
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                // Implementation for when partial speech recognition results are available
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
                // Implementation for when a speech recognition event occurs
            }
        });
        // Start listening for speech input
        speechRecognizer.startListening(intentRecognizer);
    }
}
