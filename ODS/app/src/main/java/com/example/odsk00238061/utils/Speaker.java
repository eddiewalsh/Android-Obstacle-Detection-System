package com.example.odsk00238061.utils;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import androidx.camera.view.PreviewView;
import java.util.Locale;
import java.util.Objects;


public class Speaker implements Runnable,TextToSpeech.OnInitListener {

    // Flag indicating whether the process is running or not
    private boolean isRunning = true;

    // Timestamp of the last speech output
    private long lastSpeechTime = 0;

    // Minimum delay between consecutive speech outputs (in milliseconds)
    private static final long MIN_SPEECH_DELAY = 4000;

    // TextToSpeech instance for converting text to speech
    public TextToSpeech textToSpeech;

    // PreviewView used for displaying camera preview
    private PreviewView view;

    // Context used for various Android system operations
    private final Context context;

    // Represents a potential obstacle detected by the system
    public Obstacle potentialObstacle;

    /**
     * Constructor for the Speaker class.
     *
     * @param context The context used to initialize TextToSpeech and other components.
     */
    public Speaker(Context context) {
        // Initialize the context variable with the provided context
        this.context = context;

        // Initialize the textToSpeech variable with a new TextToSpeech instance
        // Also, register this class (which implements TextToSpeech.OnInitListener) as the listener
        this.textToSpeech = new TextToSpeech(context, this);

        // Initialize the potentialObstacle variable with a new instance of the Obstacle class
        this.potentialObstacle = new Obstacle();
    }

    /**
     * Constructor for the Speaker class.
     *
     * @param context      The context used to initialize TextToSpeech and other components.
     * @param previewView  The PreviewView used for displaying camera preview.
     */
    public Speaker(Context context, PreviewView previewView) {
        // Initialize the context variable with the provided context
        this.context = context;

        // Initialize the view variable with the provided previewView
        this.view = previewView;

        // Initialize the textToSpeech variable with a new TextToSpeech instance
        // Also, register this class (which implements TextToSpeech.OnInitListener) as the listener
        this.textToSpeech = new TextToSpeech(context, this);

        // Initialize the potentialObstacle variable with a new instance of the Obstacle class
        this.potentialObstacle = new Obstacle();
    }

    /**
     * Implementation of the run method from the Runnable interface.
     * Continuously checks for the occurrence of obstacles as long as the process is running.
     */
    @Override
    public void run() {
        // Continuously loop while the process is running
            while (isRunning) {
                // Check for the occurrence of obstacles
                checkOccurrenceOfObstacle();
            }
    }

    /**
     * Checks for the occurrence of obstacles and speaks about them if necessary.
     * Uses the potentialObstacle object to determine if there is an obstacle occurrence.
     */
    public void checkOccurrenceOfObstacle() {
        // Check if there is a potential obstacle occurrence
        if(potentialObstacle.getObstacleOccurrence() != null) {
            // Get the current time in milliseconds
            long currentTime = System.currentTimeMillis();

            // Check if the obstacle occurrence is above the threshold and enough time has passed since the last speech
            if (potentialObstacle.getObstacleOccurrence() > 25 && currentTime - lastSpeechTime > MIN_SPEECH_DELAY) {
                // Calculate the location of the obstacle relative to the camera frame
                String location = ProjectHelper.calculateObstacleLocation(potentialObstacle.getObstacleRect(), view.getWidth());

                // Get the name of the obstacle
                String obstacle = potentialObstacle.getObstacleName();
                potentialObstacle.setObstacleLocation(location);

                // Speak about the obstacle's location and name
                speakText(obstacle + " located at " + potentialObstacle.getObstacleLocation() + ".");

                // Reset the potentialObstacle object for the next occurrence
                potentialObstacle = new Obstacle();

                // Update the last speech time to the current time
                lastSpeechTime = currentTime;
            }
        }
    }

    /**
     * Processes the newly detected obstacle.
     * Updates the potential obstacle information based on the newly detected obstacle.
     *
     * @param newObstacle The newly detected obstacle to process.
     */
    public void processDetectedObject(Obstacle newObstacle) {
        // Check if there is already a potential obstacle being tracked
        if(potentialObstacle != null) {

            // If the name of the new obstacle matches the potential obstacle's name
            if(newObstacle.getObstacleName().equals(potentialObstacle.getObstacleName())) {

                // Increment the occurrence count of the potential obstacle
                potentialObstacle.incrementObstacleOccurrence();

                // Update the bounding rectangle of the potential obstacle with the new obstacle's rectangle
                potentialObstacle.getObstacleRect().set(newObstacle.getObstacleRect());
            } else {
                // If the names don't match, replace the potential obstacle with the new one
                potentialObstacle = newObstacle;
            }
        } else {
            // If there is no potential obstacle being tracked, set the new obstacle as the potential obstacle
            potentialObstacle = newObstacle;
        }
    }

    /**
     * Callback method called when TextToSpeech engine initialization is completed.
     * Handles initialization success and failure cases.
     *
     * @param status The status of TextToSpeech initialization.
     */
    @Override
    public void onInit(int status) {
        // Check if TextToSpeech initialization is successful
        if (status == TextToSpeech.SUCCESS) {
            // Attempt to set the language to US English
            int langResult = textToSpeech.setLanguage(Locale.US);

            // Check if setting the language was successful
            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Log an error message if language data is missing or not supported
                Log.e("Speaker", "Language is not supported");
            }
        } else {
            // Log an error message if TextToSpeech initialization failed
            Log.e("Speaker", "Initialization failed");
        }
    }

    /**
     * Speaks the given text using the TextToSpeech engine.
     * Handles TextToSpeech initialization and speech queuing.
     *
     * @param text The text to be spoken.
     */
    public void speakText(String text) {
        try{
            // Check if the TextToSpeech engine is initialized
            if (textToSpeech == null) {
                // Initialize the TextToSpeech engine if not already initialized
                textToSpeech = new TextToSpeech(context, this);
            }

            // Stop any ongoing speech synthesis
            if (textToSpeech.isSpeaking()) {
                textToSpeech.stop();
            }

            // Speak the provided text immediately (flushing the speech queue)
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);

            // Log a debug message indicating successful speech synthesis
            Log.d("SpeakText", "Text Successfully Spoken");
        } catch(Exception e){
            // Log any exceptions that occur during speech synthesis
            Log.d("SpeakText", Objects.requireNonNull(e.getLocalizedMessage()));
        }
    }

    /**
     * Sets the flag indicating whether the process is running or not.
     *
     * @param running A boolean value indicating whether the process is running.
     */
    public void setRunning(boolean running) {
        // Update the isRunning flag with the provided value
        isRunning = running;
    }

    /**
     * Destroys the TextToSpeech engine if it is initialized.
     * Stops any ongoing speech synthesis and releases associated resources.
     */
    public void Destroy() {
        // Check if the TextToSpeech engine is initialized
        if (textToSpeech != null) {
            // Stop any ongoing speech synthesis
            textToSpeech.stop();
            // Shutdown the TextToSpeech engine to release resources
            textToSpeech.shutdown();
            // Setting the TextToSpeech engine to null
            textToSpeech = null;
        }
    }
}
