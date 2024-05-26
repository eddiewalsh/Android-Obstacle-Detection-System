package com.example.odsk00238061.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.List;

public class RectangleOverlayView extends View {

    // Paint object for drawing shapes or text on a canvas
    private Paint paint;

    // Paint object for drawing text on a canvas
    private Paint textPaint;

    // Coordinates of the bounding rectangle for drawing shapes or text
    private int left, top, right, bottom;
    private String text = "";

    // List of colors to choose from for drawing
    private final List<Integer> Colours = Arrays.asList(Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.CYAN, Color.MAGENTA, Color.WHITE);

    /**
     * Constructor for creating a RectangleOverlayView without any attribute set.
     *
     * @param context The context used for creating the view.
     */
    public RectangleOverlayView(Context context) {
        super(context);
        // Initialize the view
        init();
    }

    /**
     * Constructor for creating a RectangleOverlayView with attributes specified in XML.
     *
     * @param context The context used for creating the view.
     * @param attrs   The set of attributes specified in XML.
     */
    public RectangleOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // Initialize the view
        init();
    }

    /**
     * Initializes the Paint objects for drawing shapes and text on the overlay.
     * Sets up the paint attributes such as color, style, stroke width, and text size.
     */
    private void init() {
        // Initialize the paint object for drawing shapes
        paint = new Paint();
        paint.setColor(Color.RED); // Set the color to red
        paint.setStyle(Paint.Style.STROKE); // Set the style to stroke
        paint.setStrokeWidth(5); // Set the stroke width to 5 pixels

        // Initialize the paint object for drawing text
        textPaint = new Paint();
        textPaint.setColor(Color.WHITE); // Set the color to white
        textPaint.setTextSize(30); // Set the text size to 30 pixels
    }

    /**
     * Updates the bounding rectangle and text to be displayed on the overlay.
     * If the text changes, the rectangle color is changed accordingly.
     *
     * @param boundingBox The new bounding rectangle for the overlay.
     * @param text        The new text to be displayed on the overlay.
     */
    public void updateRect(Rect boundingBox, String text) {
        // Check if the new text is different from the current text
        if(!text.equals(this.text)){
            // If the text changes, change the rectangle color
            changeRectangleColor();
        }

        // Update the coordinates of the bounding rectangle and the text
        this.left = boundingBox.left;
        this.top = boundingBox.top;
        this.right = boundingBox.right;
        this.bottom = boundingBox.bottom;
        this.text = text;

        // Trigger a redraw of the overlay
        invalidate();
    }

    /**
     * Overrides the onDraw method to draw the bounding rectangle and text on the canvas.
     *
     * @param canvas The canvas on which to draw the overlay.
     */
    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas); // Call the superclass implementation of onDraw()

        // Draw the bounding rectangle on the canvas using the specified paint attributes
        canvas.drawRect(left, top, right, bottom, paint);

        // Check if the text is not empty
        if (!text.isEmpty()) {
            // Measure the width of the text
            float textWidth = textPaint.measureText(text);
            // Calculate the x-coordinate to center the text within the bounding rectangle
            float x = left + (right - left - textWidth) / 2;
            // Calculate the y-coordinate to vertically center the text within the bounding rectangle
            float y = top + (float) (bottom - top) / 2;
            // Draw the text on the canvas at the calculated coordinates
            canvas.drawText(text, x, y, textPaint);
        }
    }

    /**
     * Changes the color of the bounding rectangle to a random color from the Colours list.
     * Ensures that the new color is different from the current color.
     */
    private void changeRectangleColor() {
        // Generate a random index to select a color from the Colours list
        int randomIndex = (int) (Math.random() * Colours.size());
        int newColor = Colours.get(randomIndex);

        // Ensure that the new color is different from the current color
        while (newColor == paint.getColor()){
            randomIndex = (int) (Math.random() * Colours.size());
            newColor = Colours.get(randomIndex);
        }

        // Set the new color for the bounding rectangle
        paint.setColor(newColor);
    }

    /**
     * Clears the bounding rectangle and text from the overlay.
     */
    public void clearRect() {
        // Update the coordinates of the bounding rectangle and the text to clear them
        this.left = 0;
        this.top = 0;
        this.right = 0;
        this.bottom = 0;
        this.text = "";

        // Trigger a redraw of the overlay
        invalidate();
    }
}

