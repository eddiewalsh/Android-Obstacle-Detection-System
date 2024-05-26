package com.example.odsk00238061;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.example.odsk00238061.utils.ProjectHelper;
import com.example.odsk00238061.utils.Speaker;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public class VoiceMemoActivity extends AppCompatActivity {
    /**
     * Handles media recording functionality.
     */
    private MediaRecorder mediaRecorder;

    /**
     * Intent used for speech recognition.
     */
    private Intent intentRecognizer;

    /**
     * Speech recognizer for processing speech input.
     */
    private SpeechRecognizer speechRecognizer;

    /**
     * Manages speech synthesis.
     */
    private Speaker speaker;

    /**
     * Handles message and task scheduling.
     */
    private final Handler handler = new Handler();

    /**
     * Threshold duration for a long touch event.
     */
    private static final int TOUCH_DURATION_THRESHOLD = 3000;

    /**
     * Runnable task for long touch event handling.
     */
    private Runnable longTouchRunnable;

    /**
     * Context of the application.
     */
    private Context context;

    /**
     * Initializes the activity layout and components.
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the activity context
        context = this;
        // Initialize the speaker for speech synthesis
        speaker = new Speaker(this);
        // Keep the screen on while the activity is active
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // Set the activity layout
        setContentView(R.layout.activity_voicememo);
        // Get the RelativeLayout containing UI elements
        RelativeLayout relativeLayout = findViewById(R.id.layout);
        // Set a touch listener to start/stop recording on long touch
        relativeLayout.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startLongTouchTimer();
                    break;
                case MotionEvent.ACTION_UP:
                    cancelLongTouchTimer();
                    break;
            }
            return true;
        });

        // Initialize the speech recognizer intent
        intentRecognizer = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intentRecognizer.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        // Create the speech recognizer instance
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        // Initialize the text-to-speech engine and start voice memo recording
        TextToSpeech tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                speaker.speakText("The voice recording will began after the beep. Hold the screen to stop recording");
                BeginVoiceMemo();
            }
        });
    }

    /**
     * Initiates the voice memo recording after a delay.
     */
    public void BeginVoiceMemo() {
        // Create a handler to post delayed actions on the main looper
        Handler handler = new Handler(Looper.getMainLooper());
        // Post a delayed action to start recording after a specified delay
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Start recording voice memo after the delay
                startRecordingMemo();
            }
        }, 6000); // Delay of 6 seconds (6000 milliseconds)
    }

    /**
     * Starts the voice memo recording process.
     * Plays a beep sound to indicate the start of recording.
     * Records audio from the device's microphone and saves it as an MP3 file.
     * If an error occurs during the recording process, informs the user via text-to-speech.
     */
    public void startRecordingMemo() {
        // Play a beep sound to indicate the start of recording
        playBeep();

        // Get the directory for storing recorded memos
        ContextWrapper contextWrapper = new ContextWrapper(getApplicationContext());
        File memoDirectory = contextWrapper.getExternalFilesDir(Environment.DIRECTORY_MUSIC);

        // Create a new file for storing the recorded memo
        File file = new File(memoDirectory, "ODSRecordingFile" + ".mp3");

        try {
            // Initialize the MediaRecorder for recording audio
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setOutputFile(file.getPath());
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.prepare();
            // Start recording
            mediaRecorder.start();
        } catch (IOException e) {
            // Log any errors that occur during the recording process
            Log.d("RecordMemo", Objects.requireNonNull(e.getMessage()));
            // Inform the user via text-to-speech if recording fails
            speaker.speakText("I'm sorry, I couldn't record the memo. Please try again");
        }
    }

    /**
     * Stops the voice memo recording process.
     * Stops the MediaRecorder from recording audio.
     * Releases the resources used by the MediaRecorder.
     */
    public void stopRecordingMemo() {
        // Stop recording audio
        mediaRecorder.stop();
        // Release resources used by the MediaRecorder
        mediaRecorder.release();
        mediaRecorder = null;
    }

    /**
     * Starts a timer for long touch events.
     * If the timer expires and a long touch event is detected, stops the recording of the voice memo,
     * speaks a success message, and starts speech recognition.
     */
    private void startLongTouchTimer() {
        if (longTouchRunnable == null) {
            longTouchRunnable = new Runnable() {
                @Override
                public void run() {
                    // If recording is in progress, stop the recording and speak a success message
                    if (mediaRecorder != null) {
                        stopRecordingMemo();
                        speaker.speakText("Memo recorded successfully");
                    }
                    // Start speech recognition
                    startSpeechRecognition();
                }
            };
            // Start the timer for long touch events
            handler.postDelayed(longTouchRunnable, TOUCH_DURATION_THRESHOLD);
        }
    }

    /**
     * Cancels the timer for long touch events if it's currently active.
     */
    private void cancelLongTouchTimer() {
        // Check if the long touch timer is active
        if (longTouchRunnable != null) {
            // Remove the callback to stop the timer
            handler.removeCallbacks(longTouchRunnable);
            // Reset the long touch runnable to null
            longTouchRunnable = null;
        }
    }


    /**
     * Initiates speech recognition and starts listening for speech input continuously.
     * Sets up a RecognitionListener to handle various speech recognition events.
     */
    private void startSpeechRecognition() {
        // Set up a RecognitionListener for speech recognition events
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                // No action needed when speech recognition is ready
            }

            @Override
            public void onBeginningOfSpeech() {
                // No action needed at the beginning of speech recognition
            }

            @Override
            public void onRmsChanged(float rmsdB) {
                // No action needed for changes in RMS (Root Mean Square) value
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
                // No action needed when a sound buffer is received
            }

            @Override
            public void onEndOfSpeech() {
                // No action needed at the end of speech recognition
            }

            @Override
            public void onError(int error) {
                // No action needed for errors during speech recognition
            }

            @Override
            public void onResults(Bundle results) {
                // Retrieve the recognized speech matches from the results
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                // Check if matches are not null
                if (matches != null) {
                    // Handle recognized speech commands using a helper method
                    ProjectHelper.handleCommands(matches, VoiceMemoActivity.this, speaker, context);
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                // No action needed for partial speech recognition results
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
                // No action needed for speech recognition events
            }
        });
        // Start listening for speech input
        speechRecognizer.startListening(intentRecognizer);
    }

    /**
     * Plays a short beep sound before recording starts.
     * Initializes a ToneGenerator with the beep tone type and volume, then plays the beep sound.
     * Delays for a short period to allow the beep sound to play before recording starts.
     */
    private void playBeep() {
        // Initialize ToneGenerator with the beep tone type and volume
        ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
        // Play the beep sound for a short duration
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100);
        // Delay for a short period to allow the beep sound to play before recording starts
        try {
            Thread.sleep(1000); // Adjust the delay duration as needed

        } catch (InterruptedException e) {
            Log.d("VoiceMemoActivity", Objects.requireNonNull(e.getMessage()));
        }
        // Release resources
        toneGenerator.release();
    }

}
