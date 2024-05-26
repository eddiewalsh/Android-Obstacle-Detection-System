package com.example.odsk00238061;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
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
import androidx.annotation.OptIn;
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
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class TextToSpeechActivity extends AppCompatActivity  {

    /**
     * Camera facing direction, initialized to use the back camera
     */
    int camFacing = CameraSelector.LENS_FACING_BACK;

    /**
     * Intent for initiating speech recognition
     */
    private Intent intentRecognizer;

    /**
     * Future representing the asynchronous operation of obtaining the camera provider
     */
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    /**
     * Camera provider responsible for managing the camera lifecycle
     */
    private ProcessCameraProvider cameraProvider;

    /**
     * Speech recognizer for converting speech to text
     */
    private SpeechRecognizer speechRecognizer;

    /**
     *  Text recognizer for extracting text from images
     */
    private TextRecognizer textRecognizer;

    /**
     *  Threshold for touch duration to consider it as a long touch (in milliseconds)
     */
    private static final int TOUCH_DURATION_THRESHOLD = 3000;

    /**
     *  Extracted text from image analysis or text recognition
     */
    private String extractedText = "";

    /**
     *  Handler for managing delayed execution of Runnables
     */
    private final Handler handler = new Handler();

    /**
     *  Runnable to be executed when a long touch is detected
     */
    private Runnable longTouchRunnable;

    /**
     *  View for displaying camera preview
     */
    private PreviewView previewView;

    /**
     *  Speaker instance for text-to-speech functionality
     */
    private Speaker speaker;

    /**
     *  Context used for various Android system operations
     */
    private Context context;

    /**
     *  Flag indicating whether text should be read aloud
     */
    private boolean readText = false;

    /**
     * Registers an activity result launcher to handle permission requests for accessing the camera.
     * Upon receiving the result of the permission request, it sets up the camera if permission is granted,
     * or displays a toast message informing the user that camera permission is required if permission is denied.
     */
    private final ActivityResultLauncher<String> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), // RequestPermission contract for handling permission requests
            new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean isGranted) {
                    if (isGranted) {
                        // Camera permission is granted, proceed with setting up camera
                        cameraProviderFuture.addListener(() -> { // Callback invoked when the permission request is completed
                            try {
                                // Obtain the camera provider and setup camera preview
                                cameraProvider = cameraProviderFuture.get();
                                setupCameraAndBindPreview(cameraProvider);
                            } catch (ExecutionException | InterruptedException e) {
                                // Log any errors that occur during camera setup
                                Log.e("CameraX Camera Provider", Objects.requireNonNull(e.getMessage()));
                            }
                        }, ContextCompat.getMainExecutor(TextToSpeechActivity.this));
                    } else {
                        // Inform the user that camera permission is required
                        Toast.makeText(TextToSpeechActivity.this, "Camera permission is required to use the camera", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the content view to the activity_textspeech layout
        setContentView(R.layout.activity_textspeech);

        // Initialize TextRecognizer for detecting text from images
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        // Keep the screen on while the activity is running
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // Initialize context and previewView
        context = this;

        // Initialize speaker for text-to-speech functionality
        previewView = findViewById(R.id.textCameraView);
        speaker = new Speaker(this);

        // Set up touch listener for the previewView to handle long touch events
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

        // Request RECORD_AUDIO permission for speech recognition
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},PackageManager.PERMISSION_GRANTED);

        // Initialize intent for speech recognition
        intentRecognizer = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intentRecognizer.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        // Obtain an instance of ProcessCameraProvider to set up camera
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                // Check for CAMERA permission before setting up camera
                if(ContextCompat.checkSelfPermission(TextToSpeechActivity.this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
                    // Launch permission request if CAMERA permission is not granted
                    activityResultLauncher.launch(Manifest.permission.CAMERA);
                } else{
                    // Set up camera and bind preview if CAMERA permission is granted
                    setupCameraAndBindPreview(cameraProvider);
                }
            } catch (ExecutionException | InterruptedException e) {
                Log.e("CameraX Camera Provider", Objects.requireNonNull(e.getMessage()));
            }
        }, ContextCompat.getMainExecutor(this));

        // Initialize a TextToSpeech instance with the current context and an OnInitListener
        TextToSpeech tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                // Check if TextToSpeech initialization is successful
                if (status == TextToSpeech.SUCCESS) {
                    // If initialization is successful, use the speaker object to speak a welcome message
                    speaker.speakText("Text to speech is open, hold your device over the piece of text.");
                }
            }
        });
    }

    /**
     * Sets up the camera and binds the preview to the provided CameraProvider.
     *
     * @param cameraProvider The CameraProvider instance to be used for camera setup.
     */
    public void setupCameraAndBindPreview(ProcessCameraProvider cameraProvider) {
        // Create a preview instance
        Preview preview = new Preview.Builder().build();

        // Define the camera selector to specify the camera lens facing direction
        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(camFacing).build();

        // Set the surface provider for the preview
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // Create an image analysis instance
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

        // Set up an analyzer to process images from the camera
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this),
                new ImageAnalysis.Analyzer() {
                    @OptIn(markerClass = ExperimentalGetImage.class) @Override
                    public void analyze(@NonNull ImageProxy imageProxy) {
                        // Get the rotation degrees of the captured image
                        int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
                        // Get the captured image
                        Bitmap image = imageProxy.toBitmap();

                        if (image != null) {
                            Bitmap convertedImage = ProjectHelper.binarizeImage(image);
                            // Create an InputImage from the captured image with rotation information
                            InputImage inputImage = InputImage.fromBitmap(image, rotationDegrees);
                            // Perform text detection from the image
                            detectTextFromImage(inputImage, imageProxy);
                        }
                    }

                });
        // Bind the camera lifecycle to the provided lifecycle owner, along with preview and image analysis
        cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector,imageAnalysis, preview);
    }

    /**
     * Detects text from the provided input image using the text recognizer.
     * If readText flag is true, it processes the text in the image and reads it aloud.
     * Otherwise, it closes the image proxy without processing.
     *
     * @param inputImage The InputImage containing the image data.
     * @param imageProxy The ImageProxy for accessing image properties and closing the image after processing.
     */
    public void detectTextFromImage(InputImage inputImage, ImageProxy imageProxy) {
        if (readText) {
            // Process text recognition on the input image
            textRecognizer.process(inputImage)
                    .addOnSuccessListener(visionText -> {
                        // Extract text from the VisionText object
                        extractedText = visionText.getText();
                    })
                    .addOnFailureListener(e -> {
                        // Log any errors that occur during text recognition
                        Log.d("TextToSpeechActivity", "onFailure: " + e.getMessage());
                    })
                    .addOnCompleteListener(task -> {
                        // Close the image proxy after processing
                        imageProxy.close();

                        // Speak the extracted text or notify if no text was detected
                        if(extractedText.isEmpty()){
                            speaker.speakText("No text detected, please try again.");
                        } else {
                            speaker.speakText(extractedText);
                        }
                        // Reset the readText flag
                        readText = false;
                    });

        } else {
            // Close the image proxy without processing if readText flag is false
            imageProxy.close();
        }
    }

    /**
     * Starts a timer for long touch detection.
     * If the longTouchRunnable is null, it creates a new Runnable to start speech recognition
     * after the TOUCH_DURATION_THRESHOLD milliseconds.
     */
    private void startLongTouchTimer() {
        if (longTouchRunnable == null) {
            // Create a new Runnable to start speech recognition
            longTouchRunnable = new Runnable() {
                @Override
                public void run() {
                    startSpeechRecognition();
                }
            };
            // Post the longTouchRunnable after the TOUCH_DURATION_THRESHOLD milliseconds
            handler.postDelayed(longTouchRunnable, TOUCH_DURATION_THRESHOLD);
        }
    }

    /**
     * Cancels the timer for long touch detection.
     * If the longTouchRunnable is not null, it removes any pending execution of the long touch action
     * and sets the longTouchRunnable reference to null.
     */
    private void cancelLongTouchTimer() {
        if (longTouchRunnable != null) {
            // Remove any pending execution of the long touch action
            handler.removeCallbacks(longTouchRunnable);
            // Reset the longTouchRunnable reference to null
            longTouchRunnable = null;
        }
    }

    /**
     * Initiates the speech recognition process.
     * Sets up a recognition listener to handle speech recognition events.
     * When speech recognition results are available, it handles the recognized commands.
     */
    private void startSpeechRecognition() {
        // Set up a recognition listener to handle speech recognition events
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                // Method called when the system is ready to accept speech input; not utilized in this implementation
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
                // Method called when the end of speech is detected; not utilized in this implementation
            }

            @Override
            public void onError(int error) {
                // Method called when an error occurs during recognition; not utilized in this implementation
            }
            @Override
            public void onResults(Bundle results) {
                // Handle the recognized speech results
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if(matches != null){
                    //if user says "read", set readText flag to true
                    if(matches.contains("read")){
                        readText = true;
                        //if user says "repeat" and extracted text is not empty, read the extracted text
                    } else if(matches.contains("repeat")){
                        if(extractedText.isEmpty()){
                            speaker.speakText("No text detected, please try again.");
                        } else {
                            speaker.speakText(extractedText);
                        }
                    } else {
                        // Handle the recognized commands using the ProjectHelper class
                        ProjectHelper.handleCommands(matches, TextToSpeechActivity.this, speaker, context);
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
}
