package com.example.odsk00238061;

import static com.example.odsk00238061.utils.ProjectHelper.vibrate;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import com.example.odsk00238061.utils.ProjectHelper;
import com.example.odsk00238061.utils.Speaker;

import java.util.ArrayList;



public class MainActivity extends AppCompatActivity  {

    /**
     * Threshold for touch duration to consider it as a long touch (in milliseconds)
     */
    private static final int TOUCH_DURATION_THRESHOLD = 3000; // 3 seconds

    /**
     * Handler to manage delayed execution of a Runnable
     */
    private final Handler handler = new Handler();

    /**
     * Runnable to be executed when a long touch is detected
     */
    private Runnable longTouchRunnable;

    /**
     * SpeechRecognizer instance to perform speech recognition
     */
    private SpeechRecognizer speechRecognizer;

    /**
     * Intent for starting speech recognition activity
     */
    private Intent intentRecognizer;

    /**
     * Speaker instance for text-to-speech functionality
     */
    private Speaker speaker;

    /**
     * Context used for various Android system operations
     */
    private Context context;

    /**
     * Initializes the main activity when it is created.
     *
     * @param savedInstanceState A Bundle containing the activity's previously saved state, if any.
     */
    @SuppressLint("ClickableViewAccessibility") // Suppresses lint warning for accessibility
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the layout for the activity
        setContentView(R.layout.activity_main);
        // Vibrate the device for 1 second
        vibrate(this, 1000);
        // Keep the screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // Initialize views and context
        RelativeLayout relativeLayout = findViewById(R.id.layout);
        context = this;

        // Initialize speech recognition
        intentRecognizer = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intentRecognizer.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        // Initialize the speaker
        speaker = new Speaker(this);

        // Set up touch listener for the layout
        relativeLayout.setOnTouchListener(new View.OnTouchListener() {
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

        // Request RECORD_AUDIO permission
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},PackageManager.PERMISSION_GRANTED);

        // Initialize TextToSpeech and speak a welcome message
        TextToSpeech tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    speaker.speakText("Welcome to the ODS application. Hold down on the screen to speak and say 'help' to get a list of commands.");
                }
            }
        });
    }

    /**
     * Called when the activity is about to be destroyed.
     * Performs cleanup tasks before the activity is destroyed.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop speech recognition to release resources
        stopSpeechRecognition();
    }

    /**
     * Initiates the speech recognition process.
     * Sets up a recognition listener to handle speech recognition events.
     * When speech recognition detects the end of speech or encounters an error, it stops the recognition process.
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
                    ProjectHelper.handleCommands(matches, MainActivity.this, speaker, context);
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
     */
    private void stopSpeechRecognition() {
        // Stop listening for speech
        speechRecognizer.stopListening();
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


    /**
     * Called when the activity is no longer visible to the user.
     * This method is called after `onPause()` when the activity is no longer in the foreground.
     * It stops the speech recognition process and releases resources associated with the speaker.
     */
    @Override
    protected void onStop() {
        // Call the superclass method to handle standard activity stopping procedures
        super.onStop();
        // Stop the ongoing speech recognition process
        stopSpeechRecognition();
        // Release resources associated with the speaker
        speaker.Destroy();
    }
}
