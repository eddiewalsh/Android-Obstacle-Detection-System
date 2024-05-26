package com.example.odsk00238061;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
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
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.odsk00238061.utils.Product;
import com.example.odsk00238061.utils.ProjectHelper;
import com.example.odsk00238061.utils.Speaker;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ProductDetailsActivity extends AppCompatActivity {

    /**
     * Context variable to access application-specific resources and classes
     */
    private Context context;

    /**
     * SpeechRecognizer instance to perform speech recognition
     */
    private SpeechRecognizer speechRecognizer;

    /**
     * Intent used for speech recognition
     */
    private Intent intentRecognizer;

    /**
     * Duration threshold for long touch event in milliseconds
     */
    private static final int TOUCH_DURATION_THRESHOLD = 3000;

    /**
     * Handler object to schedule and execute Runnable tasks
     */
    private final Handler handler = new Handler();

    /**
     * Boolean flag to indicate whether a specific action has been executed
     */
    private boolean hasExecuted = false;

    /**
     * Instance of the Product class representing a product
     */
    private Product product;

    /**
     * Runnable object used for handling long touch events
     */
    private Runnable longTouchRunnable;

    /**
     * TextView displaying the name of the product
     */
    private TextView productNameTextView;

    /**
     * TextView displaying the description of the product
     */
    private TextView productDescriptionTextView;

    /**
     * ImageView displaying the image of the product
     */
    private ImageView productImageView;

    /**
     * Speaker object responsible for speech synthesis
     */
    private Speaker speaker;

    /**
     * RelativeLayout used to organize UI components
     */
    private RelativeLayout relativeLayout;

    /**
     * Suppresses lint warnings related to accessibility for the entire method.
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_details);
        relativeLayout = findViewById(R.id.layout);

        // Retrieve the intent and prevent the screen from sleeping
        Intent intent = getIntent();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Extract UPC from intent and initialize product
        String upc = intent.getStringExtra("barcode");
        product = new Product();

        // Initialize context and UI components
        context = this;

        productNameTextView = findViewById(R.id.productNameTextView);
        productDescriptionTextView = findViewById(R.id.productDescriptionTextView);
        productImageView = findViewById(R.id.productImageView);

        // Initialize speech recognition components
        intentRecognizer = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intentRecognizer.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        // Fetch product details using UPC
        getProductDetails(upc);

        // Initialize speaker and set touch listener for long touch events
        speaker = new Speaker(this);
        relativeLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
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

        // Schedule a task to be executed after a dela
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                executeOnStartLogic();
            }
        }, 3000);
    }

    /**
     * Executes logic when the activity starts if it hasn't been executed before.
     * Initializes the TextToSpeech engine and the product details command handler.
     */
    private void executeOnStartLogic() {
        if (!hasExecuted) {
            // Initialize TextToSpeech engine
            TextToSpeech tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if (status == TextToSpeech.SUCCESS) {
                        // Initialize the product details command handler
                        ProjectHelper.initProductDetailsCommandHandler(speaker, product);
                    }
                }
            });
            hasExecuted = true;
        }
    }

    /**
     * Retrieves product details from the API using the provided UPC.
     * @param upc The UPC (Universal Product Code) of the product to fetch details for.
     */
    public void getProductDetails(String upc) {
        // Create a request queue using Volley
        RequestQueue queue = Volley.newRequestQueue(this);

        // Construct the URL for the API request using the UPC and API key
        String url = getResources().getString(R.string.BASE_URL) + upc
                    + "&formatted=y&key="
                    + getResources().getString(R.string.API_KEY);

        // Define a StringRequest to handle the API request
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,

                // Define onResponse to handle successful response from the API
                new Response.Listener<String>() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onResponse(String response) {
                        try {
                            // Parse the JSON response
                            JSONObject jsonResponse = new JSONObject(response);

                            // Extract the 'products' array from the response
                            JSONArray products = jsonResponse.getJSONArray("products");

                            // Check if there are any products returned
                            if (products.length() > 0) {
                                // Extract details for the first product in the array
                                JSONObject productJ = products.getJSONObject(0);

                                // Extract product details
                                String prodTitle = productJ.optString("title");
                                String prodCategory = productJ.optString("category");
                                String prodDescription = productJ.optString("description");
                                String prodIngredients = productJ.optString("ingredients");

                                if(prodTitle.trim().toLowerCase().equals(prodDescription
                                                   .trim().toLowerCase())){
                                    prodDescription = "";
                                }

                                // Set product details
                                product.setTitle(prodTitle);
                                product.setCategory(prodCategory);
                                product.setIngredients(prodIngredients);

                                // Extract image URLs and set product images
                                JSONArray imagesArray = productJ.getJSONArray("images");
                                List<String> imagesList = new ArrayList<>();
                                for (int i = 0; i < imagesArray.length(); i++) {
                                    imagesList.add(imagesArray.getString(i));
                                }

                                product.setImages(imagesList);

                                // Display the first image (if available) in the ImageView
                                if(!(product.getImages().isEmpty())){
                                    Picasso.get().load(product.getImages().get(0))
                                            .into(productImageView);
                                } else {
                                    productImageView.setImageResource(R.drawable.placeholder_image);
                                }

                                // Set product name TextView
                                productNameTextView.setText(product.getTitle());

                                // Set product description TextView or display
                                if(product.isDescriptionAvailable()) {
                                    productDescriptionTextView.setText(product.getDescription());
                                } else {
                                    productDescriptionTextView.setText("No description available");
                                }

                            }
                        } catch (JSONException e) {
                            // Log any JSON parsing errors
                            Log.d("ProductDetailsActivity", "Error: " + e.getMessage());
                            throw new RuntimeException(e);
                        }
                    }
                },
                // Define onErrorResponse to handle errors from the API request
                new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // Log any errors from the API request
                Log.d("ProductDetailsActivity", "Error: " + error.getMessage());
            }
        });
        // Add the StringRequest to the request queue
        queue.add(stringRequest);
    }

    /**
     * Starts a timer to detect a long touch event. If the timer is not already running,
     * it creates a new Runnable to start speech recognition after a specified duration.
     */
    private void startLongTouchTimer() {
        // Checks if the long touch runnable is null.
        if (longTouchRunnable == null) {
            // Creates a new runnable that triggers speech recognition.
            longTouchRunnable = new Runnable() {
                @Override
                public void run() {
                    startSpeechRecognition();
                }
            };
            // Posts the runnable to the handler with a delay based on the touch duration threshold.
            handler.postDelayed(longTouchRunnable, TOUCH_DURATION_THRESHOLD);
        }
    }

    /**
     * Cancels the long touch timer by removing the callback for the long touch runnable from the handler.
     */
    private void cancelLongTouchTimer() {
        // Checks if the long touch runnable is not null.
        if (longTouchRunnable != null) {
            // Removes the callback for the long touch runnable from the handler.
            handler.removeCallbacks(longTouchRunnable);
            // Sets the long touch runnable to null.
            longTouchRunnable = null;
        }
    }

    /**
     * Initiates speech recognition by setting a recognition listener and starting listening for speech.
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
                // Method called when sound buffer is received; not utilized in this implementation
            }

            @Override
            public void onEndOfSpeech() {
                // Method called when the end of speech is detected; not utilized in this implementation
            }

            @Override
            public void onError(int error) {
                // Method called when an error occurs during speech recognition
                Log.d("ProductDetailsActivity", "Error: " + error);
            }

            @Override
            public void onResults(Bundle results) {
                // Handle the recognized speech results
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if(matches != null) {
                    ProjectHelper.ProductDetailsCommandHandler(matches, ProductDetailsActivity.this, speaker, context, product);
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                // Method called when partial recognition results are available; not utilized in this implementation
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
                // Method called when an event occurs during speech recognition; not utilized in this implementation
            }
        });
        // Start listening for continuous speech recognition using the specified recognition intent
        speechRecognizer.startListening(intentRecognizer);
    }
}
