package com.example.odsk00238061.utils;


import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.os.BatteryManager;
import android.os.Environment;
import android.os.VibrationEffect;
import android.os.Vibrator;
import androidx.camera.core.ImageProxy;


import com.example.odsk00238061.BarcodeScannerActivity;
import com.example.odsk00238061.ObjectDetectionActivity;
import com.example.odsk00238061.TextToSpeechActivity;
import com.example.odsk00238061.VoiceMemoActivity;
import com.google.mlkit.vision.common.InputImage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;


public class ProjectHelper {

    /**
     * Calculates the location of an obstacle relative to the camera frame.
     *
     * @param rect   The bounding rectangle of the detected obstacle.
     * @param pWidth The width of the preview frame.
     * @return The location of the obstacle relative to the camera frame.
     */
    public static String calculateObstacleLocation(Rect rect, int pWidth) {
        // Calculate the center X-coordinate of the bounding rectangle
        int rectCenterX = rect.centerX();

        // Define threshold values for different regions of the frame
        int leftThreshold = pWidth / 4;
        int rightThreshold = 3 * pWidth / 4;

        // Determine the location of the obstacle based on its center X-coordinate
        if (rectCenterX < leftThreshold) {
            return "Far Left";
        } else if (rectCenterX < pWidth / 2) {
            // Additional condition to ensure it's not too far right to be Center Left
            if (rectCenterX + (rect.width() / 2) < pWidth / 2) {
                return "Center Left";
            } else {
                return "Center";
            }
        } else if (rectCenterX < rightThreshold) {
            // Additional condition to ensure it's not too far left to be Center Right
            if (rectCenterX - (rect.width() / 2) > pWidth / 2) {
                return "Center Right";
            } else {
                return "Center";
            }
        } else {
            return "Far Right";
        }
    }

    /**
     * Retrieves the current battery level percentage.
     *
     * @param context The context used to retrieve the battery information.
     * @return The battery level percentage as a float value.
     */
    public static float getBatteryLevel(Context context) {
        // Register a BroadcastReceiver to monitor battery changes
        Intent batteryIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        // Retrieve the battery level and scale from the battery Intent
        if(batteryIntent != null){
            int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

            // Calculate the battery percentage and return the result
            return (level / (float) scale) * 100;
        } else {
            // If the battery Intent is null, return -1
            return -1;
        }
    }

    /**
     * Vibrates the device for the specified duration.
     *
     * @param context     The context used to access the system service.
     * @param milliseconds The duration of the vibration in milliseconds.
     */
    public static void vibrate(Context context, long milliseconds) {
        // Get the Vibrator system service
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        // Check if the Vibrator service is available
        if (vibrator != null) {

            // Check the Android version to determine how to vibrate
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                // For Android Oreo (API level 26) and above, use VibrationEffect
                vibrator.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                // For versions below Android Oreo, simply vibrate with the specified duration
                vibrator.vibrate(milliseconds);
            }
        }
    }

    /**
     * Resizes a Bitmap while maintaining its aspect ratio.
     *
     * @param getBitmap The Bitmap to resize.
     * @param maxSize   The maximum size (width or height) of the resized Bitmap.
     * @return The resized Bitmap.
     */
    public static Bitmap resizeBitmap(Bitmap getBitmap, int maxSize) {
        // Get the width and height of the original Bitmap
        int width = getBitmap.getWidth();
        int height = getBitmap.getHeight();
        double x;

        // If the width is greater than or equal to the height and exceeds the maximum size
        if (width >= height && width > maxSize) {
            // Calculate the ratio of width to height
            x = width / (double) height;
            // Set the width to the maximum size
            width = maxSize;
            // Calculate the new height based on the ratio
            height = (int) (maxSize / x);

            // If the height is greater than or equal to the width and exceeds the maximum size
        } else if (height >= width && height > maxSize) {
            // Calculate the ratio of height to width
            x = height / (double) width;
            // Set the height to the maximum size
            height = maxSize;
            // Calculate the new width based on the ratio
            width = (int) (maxSize / x);
        }

        // Return the resized Bitmap using Bitmap.createScaledBitmap()
        return Bitmap.createScaledBitmap(getBitmap, width, height, false);
    }

    /**
     * Handles voice commands and performs corresponding actions.
     *
     * @param matches  List of voice command matches.
     * @param activity The activity context.
     * @param speaker  The speaker object for text-to-speech functionality.
     * @param context  The application context.
     */
    public static void handleCommands(ArrayList<String> matches, Activity activity, Speaker speaker, Context context){
        // Check if the matches contain commands related to text-to-speech
        if(matches.contains("text-to-speech") || matches.contains("text to speech")
                || matches.contains("speech") || matches.contains("text")){

            // Start the TextToSpeechActivity
            Intent intent = new Intent(activity, TextToSpeechActivity.class);
            speaker.Destroy();  // Stop any ongoing speech
            activity.startActivity(intent);

            // Check if the matches contain commands related to detecting obstacles
        } else if(matches.contains("detect obstacles") || matches.contains("obstacles") || matches.contains("detect")) {

            // Start the ObjectDetectionActivity
            Intent intent = new Intent(activity, ObjectDetectionActivity.class);
            speaker.Destroy();  // Stop any ongoing speech
            activity.startActivity(intent);

            // Check if the matches contain commands related to scanning barcode
        } else if(matches.contains("scan barcode") || matches.contains("barcode") || matches.contains("scan")) {

            // Start the BarcodeScannerActivity
            Intent intent = new Intent(activity, BarcodeScannerActivity.class);
            speaker.Destroy();  // Stop any ongoing speech
            activity.startActivity(intent);

            // Check if the matches contain commands related to checking battery level
        } else if(matches.contains("battery")) {

            // Get the current battery level and speak it
            float batteryLevel = getBatteryLevel(context);
            speaker.speakText("The current battery life is " + batteryLevel + " percent");

            // Check if the matches contain commands related to recording memo
        } else if(matches.contains("record memo") || matches.contains("record")) {

            // Start the VoiceMemoActivity
            Intent intent = new Intent(activity, VoiceMemoActivity.class);
            speaker.Destroy();  // Stop any ongoing speech
            activity.startActivity(intent);

            // Check if the matches contain commands related to playing memo
        } else if(matches.contains("play memo") || matches.contains("play")) {

            // Play the latest recorded memo
            PlayLatestMemo(context, speaker);

            // Check if the matches contain commands related to getting help
        } else if(matches.contains("help")) {

            // Speak the help message
            speaker.speakText(getHelpMessage());

        } else {
            // If none of the specific commands are matched, speak an error message
            speaker.speakText("I'm sorry, I didn't understand that. Please try again");
        }
    }

    /**
     * Plays the latest recorded memo stored as an MP3 file.
     *
     * @param context The application context.
     * @param speaker An object for text-to-speech functionality.
     */
    private static void PlayLatestMemo(Context context, Speaker speaker){
        // Access the application-specific directory for storing music files
        ContextWrapper contextWrapper = new ContextWrapper(context);
        File memoDirectory = contextWrapper.getExternalFilesDir(Environment.DIRECTORY_MUSIC);

        // Construct the file path for the latest recorded memo
        File file = new File(memoDirectory, "ODSRecordingFile" + ".mp3");

        try {
            // Create a MediaPlayer instance and set the data source to the memo file
            MediaPlayer mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(file.getPath());

            // Prepare and start playback
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {

            // If an IOException occurs, speak an error message
            speaker.speakText("I'm sorry, I couldn't play the memo. Please try again");
        }
    }

    /**
     * Generates a help message listing available commands.
     *
     * @return The help message as a string.
     */
    private static String getHelpMessage() {
        return "You can say the following commands: \n" +
                "Text to speech, Detect obstacles, Scan barcode, " +
                "Battery, Record memo, Play memo";
    }

    /**
     * The initial introduction when product details are returned
     *
     * @param speaker  The speaker object for text-to-speech functionality.
     * @param product  The product for which details are to be handled.
     */
    public static void initProductDetailsCommandHandler(Speaker speaker, Product product){
        // Check if title and description are unavailable for the product
        if(!product.isTitleAvailable() && !product.isDescriptionAvailable()){
            // Speak a message indicating that no information about the product was found
            speaker.speakText("I'm sorry, I couldn't find any information about this product.");
            return;
        }

        // Check if the product is classified as food
        if(product.isFood()) {
            // Check if ingredients and nutrition facts are available
            if(product.isIngredientsAvailable() && product.isNutritionFactsAvailable()) {
                // Speak the product name along with options to get ingredients or nutrition facts
                speaker.speakText("The product name is " + product.getTitle() + ". " +
                                  "To get the ingredients, say 'ingredients'. " +
                                  "To get the nutrition facts, say 'nutrition'.");
            } else {
                // Speak the product name along with a message indicating some details may not be available
                speaker.speakText("The product name is " + product.getTitle() + ". " +
                                  "Some of the product details may not be available.");
            }

        } else {
            // Check if the description is available for the product
            if(product.isDescriptionAvailable()){
                // Speak the product name along with an option to get the description
                speaker.speakText("The product name is " + product.getTitle() + ". " +
                                  "To get the description, say 'description'.");
            } else {
                // Speak only the product name if no description is available
                speaker.speakText("The product name is " + product.getTitle() + ".");
            }
        }
    }

    /**
     * Handles voice commands related to product details.
     *
     * @param matches  List of voice command matches.
     * @param activity The activity context.
     * @param speaker  The speaker object for text-to-speech functionality.
     * @param context  The application context.
     * @param product  The product for which details are to be handled.
     */
    public static void ProductDetailsCommandHandler(ArrayList<String> matches, Activity activity, Speaker speaker, Context context, Product product) {
        // Check if the matches contain the command "ingredients"
        if(matches.contains("ingredients")){
            // If ingredients are available for the product, speak them
            if(product.isIngredientsAvailable()){
                speaker.speakText("The ingredients include " + product.getIngredients());
            } else {
                // If ingredients are not available, inform the user
                speaker.speakText("Unfortunately, there are no ingredients available for this product.");
            }
            // Check if the matches contain the command "nutrition"
        } else if(matches.contains("nutrition")){
            // If nutrition facts are available for the product, speak them
            if(product.isNutritionFactsAvailable()){
                speaker.speakText("The nutrition facts include " + product.getNutritionFacts());
            } else {
                // If nutrition facts are not available, inform the user
                speaker.speakText("Unfortunately, there are no nutrition facts available for this product.");
            }
            // Check if the matches contain the command "description"
        } else if(matches.contains("description")){
            // If description is available for the product, speak it
            if(product.isDescriptionAvailable()){
                speaker.speakText(product.getDescription());
            } else {
                // If description is not available, inform the user
                speaker.speakText("Unfortunately, there is no description available for this product.");
            }
            // Check if the matches contain commands related to product name/title
        } else if(matches.contains("product name") || matches.contains("title") || matches.contains("name")) {
            // If product name/title is available, speak it
            if(product.isTitleAvailable()){
                speaker.speakText("The product name is " + product.getTitle());
            } else {
                // If product name/title is not available, inform the user
                speaker.speakText("Unfortunately, there is no product name available for this product.");
            }
            // Check if the matches contain the command "repeat"
        } else if(matches.contains("repeat")){
            // Repeat the product details using the initProductDetailsCommandHandler function
            initProductDetailsCommandHandler(speaker,  product);
            // If none of the specific commands are matched, delegate to the general command handler
        } else {
            handleCommands(matches, activity, speaker, context);
        }
    }

    /**
     * Converts an ImageProxy to an InputImage for ML Kit processing.
     *
     * @param imageProxy The ImageProxy containing the captured image.
     * @param imageSize  The desired size of the processed image.
     * @return The InputImage for ML Kit processing.
     */
    public static InputImage imageProxyToInputImage(ImageProxy imageProxy, int imageSize) {
        // Convert the ImageProxy to a Bitmap
        Bitmap bitmap = imageProxy.toBitmap();

        // Resize the Bitmap to the desired size
        bitmap = ProjectHelper.resizeBitmap(bitmap, imageSize);

        // Create an InputImage from the resized Bitmap and rotation information
        return InputImage.fromBitmap(bitmap, imageProxy.getImageInfo().getRotationDegrees());
    }

    /**
     * Converts the input image to a binary image using a specified threshold.
     *
     * @param originalImage The original image to be binarized.
     * @return A binary version of the original image.
     */
    public static Bitmap binarizeImage(Bitmap originalImage) {
        // Convert the image to grayscale
        Bitmap grayscaleImage = toGrayscale(originalImage);

        // Create a new bitmap to store the binary image
        Bitmap binaryImage = Bitmap.createBitmap(grayscaleImage.getWidth(), grayscaleImage.getHeight(), Bitmap.Config.ARGB_8888);

        // Set the threshold value for binarization
        int threshold = 128; // Adjust the threshold as needed

        // Iterate over each pixel in the grayscale image
        for (int i = 0; i < grayscaleImage.getWidth(); i++) {
            for (int j = 0; j < grayscaleImage.getHeight(); j++) {
                // Get the grayscale value of the pixel
                int pixel = grayscaleImage.getPixel(i, j);
                int grayValue = Color.red(pixel); // Assuming grayscale

                // Apply binarization based on the threshold
                if (grayValue > threshold) {
                    binaryImage.setPixel(i, j, Color.WHITE);
                } else {
                    binaryImage.setPixel(i, j, Color.BLACK);
                }
            }
        }

        // Return the binary image
        return binaryImage;
    }

    /**
     * Converts the input bitmap to grayscale.
     *
     * @param originalImage The original image to be converted to grayscale.
     * @return A grayscale version of the original image.
     */
    public static Bitmap toGrayscale(Bitmap originalImage) {
        // Create a new bitmap with the same dimensions as the original image
        Bitmap grayscaleImage = Bitmap.createBitmap(originalImage.getWidth(), originalImage.getHeight(), Bitmap.Config.RGB_565);

        // Create a canvas to draw on the grayscale image
        Canvas canvas = new Canvas(grayscaleImage);

        // Create a paint object for applying color transformations
        Paint paint = new Paint();

        // Create a color matrix for transforming colors to grayscale
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0); // Set saturation to 0 to convert to grayscale

        // Create a color filter using the color matrix
        ColorMatrixColorFilter colorMatrixColorFilter = new ColorMatrixColorFilter(colorMatrix);

        // Apply the color filter to the paint object
        paint.setColorFilter(colorMatrixColorFilter);

        // Draw the original image onto the canvas with the applied color filter
        canvas.drawBitmap(originalImage, 0, 0, paint);

        // Return the grayscale image
        return grayscaleImage;
    }
}
