package com.example.odsk00238061;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.Size;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.example.odsk00238061.utils.Obstacle;
import com.example.odsk00238061.utils.PositionTranslator;
import com.example.odsk00238061.utils.ProjectHelper;
import com.example.odsk00238061.utils.RectangleOverlayView;
import com.example.odsk00238061.utils.Speaker;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.common.model.LocalModel;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions;


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class ObjectDetectionActivity extends AppCompatActivity {

    /**
     * Threshold duration in milliseconds to consider a touch as a long touch.
     */
    private static final int TOUCH_DURATION_THRESHOLD = 3000; // 3 seconds

    /**
     * Handler object to manage the execution of Runnable tasks.
     */
    private final Handler handler = new Handler();

    /**
     * Runnable task to be executed when a long touch is detected.
     */
    private Runnable longTouchRunnable;


    /**
     * SpeechRecognizer instance for performing speech recognition.
     */
    private SpeechRecognizer speechRecognizer;

    /**
     * Intent for starting the speech recognition activity.
     */
    private Intent intentRecognizer;

    /**
     * Speaker instance for text-to-speech functionality.
     */
    private Speaker speaker;

    /**
     * View for overlaying rectangles on the camera preview for object detection visualization.
     */
    private RectangleOverlayView rectangleOverlayView;

    /**
     * View for displaying the camera preview.
     */
    private PreviewView previewView;

    /**
     * Camera facing direction (back or front).
     */
    int camFacing = CameraSelector.LENS_FACING_BACK;

    /**
     * Future for retrieving the camera provider instance.
     */
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    /**
     * ObjectDetector instance for detecting objects in images.
     */
    private ObjectDetector objectDetector;

    /**
     * ProcessCameraProvider instance for managing camera operations.
     */
    public ProcessCameraProvider cameraProvider;

    /**
     * Context used for various Android system operations.
     */
    private Context context;

    /**
     * Registers an activity result launcher to handle permission requests for accessing the camera.
     * Upon receiving the result of the permission request, it sets up the camera if permission is granted,
     * or displays a toast message informing the user that camera permission is required if permission is denied.
     */
    private final ActivityResultLauncher<String> activityResultLauncher = registerForActivityResult(
            // Registering for activity result using the RequestPermission contract
            new ActivityResultContracts.RequestPermission(),
            // Callback to handle the result of the permission request
            new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean isGranted) {
                    // Check if the permission is granted
                    if (isGranted) {
                        // Add a listener to the camera provider future
                        cameraProviderFuture.addListener(() -> {
                            try {
                                // Get the camera provider and bind the preview
                                cameraProvider = cameraProviderFuture.get();
                                BindPreview(cameraProvider);
                            } catch (ExecutionException | InterruptedException e) {
                                // Log any errors that occur during camera provider retrieval
                                Log.e("CameraX Camera Provider", Objects.requireNonNull(e.getMessage()));
                            }
                        }, ContextCompat.getMainExecutor(ObjectDetectionActivity.this));
                    } else {
                        // Display a toast message indicating that camera permission is required
                        Toast.makeText(ObjectDetectionActivity.this, "Camera permission is required to use the camera", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );


    /**
     * Initializes the object detection activity when it is created.
     *
     * @param savedInstanceState A Bundle containing the activity's previously saved state, if any.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the layout for the activity
        setContentView(R.layout.activity_object_detection);
        // Find and initialize the preview view for camera
        previewView = findViewById(R.id.cameraPreview);
        // Keep the screen on while the activity is running
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // Initialize the context
        context = this;
        // Find and initialize the rectangle overlay view for displaying object detection results
        rectangleOverlayView = findViewById(R.id.rectangle_overlay);
        // Initialize the intent for speech recognition
        intentRecognizer = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intentRecognizer.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                  RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        // Create a SpeechRecognizer instance for speech recognition
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        // Initialize the speaker with the preview view
        speaker = new Speaker(this, previewView);
        // Create and start a new thread for the speaker
        Thread speakerThread = new Thread(speaker);
        speakerThread.start();

        // Configure the local model for object detection
        LocalModel localModel =
                new LocalModel.Builder()
                        .setAssetFilePath("1.tflite") // Specify the TFLite model asset file path
                        .build();

        // Configure custom options for the object detector
        CustomObjectDetectorOptions customObjectDetectorOptions =
                new CustomObjectDetectorOptions.Builder(localModel)
                        .setDetectorMode(CustomObjectDetectorOptions.STREAM_MODE) // Set detection mode to stream mode
                        .enableClassification() // Enable classification of detected objects
                        .setClassificationConfidenceThreshold(0.6f) // Set confidence threshold for classification
                        .setMaxPerObjectLabelCount(3) // Set maximum number of labels per detected object
                        .build();

        // Create an object detector instance using the configured options
        objectDetector = ObjectDetection.getClient(customObjectDetectorOptions);

        // Set up touch listener for the preview view
        previewView.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Handle touch events for starting and cancelling long touch timer
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startLongTouchTimer();
                        break;
                    case MotionEvent.ACTION_UP:
                        cancelLongTouchTimer();
                        break;
                }
                return true;
            }
        });

        // Retrieve the camera provider instance asynchronously
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                // Get the camera provider instance
                cameraProvider = cameraProviderFuture.get();

                // Check if camera permission is granted
                if(ContextCompat.checkSelfPermission(ObjectDetectionActivity.this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
                    // Launch the activity result launcher for camera permission request
                    activityResultLauncher.launch(Manifest.permission.CAMERA);
                } else {
                    // Initialize TextToSpeech and speak a welcome message
                    TextToSpeech tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
                        @Override
                        public void onInit(int status) {
                            if (status == TextToSpeech.SUCCESS) {
                                speaker.speakText("Obstacle Detection has begun. Hold your device in front of you.");
                            }
                        }
                    });

                    // Bind the camera preview
                    BindPreview(cameraProvider);
                }
            } catch (ExecutionException | InterruptedException e) {
                // Log any errors that occur during camera provider retrieval
                Log.e("CameraX Camera Provider", Objects.requireNonNull(e.getMessage()));
            }
            }, ContextCompat.getMainExecutor(this));
    }

    /**
     * Configures the camera preview, image analysis, and object detection components,
     * and binds them to the activity's lifecycle.
     * This method sets up the camera preview with a specified resolution,
     * analyzes frames from the camera feed using image analysis,
     * detects objects in the frames, and overlays rectangles on the preview
     * to highlight the detected objects.
     *
     * @param CameraProvider The camera provider instance.
     */
    private void BindPreview(ProcessCameraProvider CameraProvider) {
        // Configure the preview use case with target resolution
        Preview preview = new Preview.Builder()
                .setTargetResolution(new Size(360,800 )) // Set the target resolution for preview
                .build();

        // Set the surface provider for the preview
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // Configure the camera selector to choose the desired camera lens facing
        CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(camFacing)
                        .build();

        // Configure the image analysis use case with backpressure strategy
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // Set the backpressure strategy
                        .build();

        // Initialize a position translator to translate bounding box coordinates
        PositionTranslator positionTranslator = new PositionTranslator(224, 224,
                                                                        previewView.getWidth(),
                                                                        previewView.getHeight(),
                                                            0);

        // Set an analyzer for the image analysis use case
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this),
                new ImageAnalysis.Analyzer()
                {
                    @ExperimentalGetImage
                    @Override
                    public void analyze(@NonNull ImageProxy imageProxy)
                    {
                        Image image = imageProxy.getImage();
                        Log.d("ImageProxy", imageProxy.getWidth() + "x" + imageProxy.getHeight());
                        if (image != null) {

                            // Convert ImageProxy to InputImage
                            InputImage inputImage = ProjectHelper.imageProxyToInputImage(imageProxy, 224);

                            // Process the input image with the object detector
                            Task<List<DetectedObject>> task = objectDetector.process(inputImage);
                            task.addOnSuccessListener(
                                            new OnSuccessListener<List<DetectedObject>>() {
                                                @Override
                                                public void onSuccess(List<DetectedObject> detectedObjects) {
                                                    if(!detectedObjects.isEmpty()){
                                                        // Get the first detected object
                                                        DetectedObject object = detectedObjects.get(0);

                                                        // Translate the bounding box coordinates
                                                        Rect boundingBox = positionTranslator.translateBoundingBox(object.getBoundingBox());

                                                        // Create an Obstacle object from the detected object
                                                        Obstacle obstacle = new Obstacle(object, boundingBox);

                                                        // Process the detected obstacle
                                                        speaker.processDetectedObject(obstacle);

                                                        // Update the rectangle overlay view with the detected obstacle
                                                        rectangleOverlayView.updateRect(boundingBox, obstacle.getObstacleName());

                                                        // Invalidate the rectangle overlay view to trigger redraw
                                                        rectangleOverlayView.invalidate();

                                                    } else{
                                                        rectangleOverlayView.clearRect();
                                                    }
                                                }
                                            }
                                    )
                                    .addOnFailureListener(
                                            new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    // Log any failure in object detection
                                                    Log.e("Object Detection", Objects.requireNonNull(e.getMessage()));
                                                }
                                            }
                                    )
                                    .addOnCompleteListener(
                                            new OnCompleteListener<List<DetectedObject>>() {
                                                @Override
                                                public void onComplete(@NonNull Task<List<DetectedObject>> task) {
                                                    // Close the ImageProxy after completion
                                                    imageProxy.close();
                                                }
                                            }
                                    );

                            }
                        }
                });
        // Bind the camera use cases to the activity's lifecycle
        CameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, imageAnalysis, preview);
    }

    /**
     * Initiates the speech recognition process.
     * Sets up a recognition listener to handle speech recognition events.
     * When speech recognition detects the end of speech or encounters an error, it stops the recognition process.
     * When speech recognition results are available, it checks for specific commands and handles them accordingly.
     */
    private void startSpeechRecognition() {
        // Set up a recognition listener to handle speech recognition events
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                // Indicate that speech recognition is running
                speaker.setRunning(false);
            }

            @Override
            public void onBeginningOfSpeech() {
                // Method called when the beginning of speech is detected; not utilized in this implementation
            }

            @Override
            public void onRmsChanged(float rmsdB) {
                // Method called when the RMS (root mean square) value of the audio changes; not utilized in this implementation
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
                // Method called when a partial recognition result is available; not utilized in this implementation
            }

            @Override
            public void onEndOfSpeech() {
                // Stop speech recognition when the end of speech is detected
                stopSpeechRecognition();
            }

            @Override
            public void onError(int error) {
                // Stop speech recognition when an error occurs during recognition
                stopSpeechRecognition();
            }

            @Override
            public void onResults(Bundle results) {
                // Handle the recognized speech results
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null) {
                    // Check for specific commands and handle them accordingly
                    if(matches.contains("play memo")){
                        speaker.speakText("Leave obstacle detection to play recording.");
                    } else {
                        ProjectHelper.handleCommands(matches, ObjectDetectionActivity.this, speaker, context);
                    }
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                // Method called when partial recognition results are available; not utilized in this implementation
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
                // Method called when a recognition event occurs; not utilized in this implementation
            }
        });
        // Start listening for continuous speech recognition using the specified recognition intent
        speechRecognizer.startListening(intentRecognizer);
    }

    /**
     * Stops the ongoing speech recognition process.
     * This method interrupts the speech recognition process and stops listening for further speech input.
     * It also notifies the Speaker to resume its operation.
     */
    private void stopSpeechRecognition() {
        // Stop listening for speech
        speechRecognizer.stopListening();
        // Notify the Speaker to resume its operation
        speaker.setRunning(true);
    }

    /**
     * Initiates a timer for long touch detection.
     * If the long touch duration threshold is reached, it starts the speech recognition process.
     */
    private void startLongTouchTimer() {
        // Check if the long touch runnable is not already initialized
        if (longTouchRunnable == null) {
            // Initialize the long touch runnable to start speech recognition after the threshold duration
            longTouchRunnable = new Runnable() {
                @Override
                public void run() {
                    // Start the speech recognition process when the long touch duration threshold is reached
                    startSpeechRecognition();
                }
            };
            // Schedule the execution of the long touch runnable after the threshold duration
            handler.postDelayed(longTouchRunnable, TOUCH_DURATION_THRESHOLD);
        }
    }

    /**
     * Cancels the timer for long touch detection.
     * If a long touch timer is active, it cancels the scheduled execution of the long touch action.
     */
    private void cancelLongTouchTimer() {
        // Check if the long touch runnable is initialized
        if (longTouchRunnable != null) {
            // Remove any pending execution of the long touch runnable
            handler.removeCallbacks(longTouchRunnable);
            // Reset the long touch runnable reference to null
            longTouchRunnable = null;
        }
    }
}
