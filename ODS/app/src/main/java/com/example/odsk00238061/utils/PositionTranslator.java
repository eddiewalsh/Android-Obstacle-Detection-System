package com.example.odsk00238061.utils;

import android.graphics.Rect;

/**
 * Translates bounding box coordinates from a source image to a target view,
 * considering the scaling factors and phone rotation.
 */
public class PositionTranslator {
    // Attributes
    private final int sourceImageWidth; // Width of the source image
    private final int sourceImageHeight; // Height of the source image
    private final int targetViewWidth; // Width of the target view
    private final int targetViewHeight; // Height of the target view
    private final int phoneRotation; // Rotation angle of the phone

     /**
     * Constructor for the PositionTranslator class.
     *
     * @param sourceImageWidth  Width of the source image.
     * @param sourceImageHeight Height of the source image.
     * @param targetViewWidth   Width of the target view.
     * @param targetViewHeight  Height of the target view.
     * @param phoneRotation     Rotation angle of the phone.
     */
    public PositionTranslator(int sourceImageWidth, int sourceImageHeight, int targetViewWidth, int targetViewHeight, int phoneRotation) {
        this.sourceImageWidth = sourceImageWidth;
        this.sourceImageHeight = sourceImageHeight;
        this.targetViewWidth = targetViewWidth;
        this.targetViewHeight = targetViewHeight;
        this.phoneRotation = phoneRotation;
    }

    /**
     * Translates bounding box coordinates from the source image to the target view.
     *
     * @param boundingBox The bounding box to be translated.
     * @return The translated bounding box coordinates.
     */
    public Rect translateBoundingBox(Rect boundingBox) {
        // Calculate scaling factors
        float scaleX = (float) targetViewWidth / sourceImageWidth;
        float scaleY = (float) targetViewHeight / sourceImageHeight;

        // Translate bounding box coordinates
        int left = (int) (boundingBox.left * scaleX);
        int top = (int) (boundingBox.top * scaleY);
        int right = (int) (boundingBox.right * scaleX);
        int bottom = (int) (boundingBox.bottom * scaleY);

        // Adjust for phone rotation
        switch (phoneRotation) {
            case 90:
                return new Rect(top, targetViewWidth - right, bottom, targetViewWidth - left);
            case 180:
                return new Rect(targetViewWidth - right, targetViewHeight - bottom, targetViewWidth - left, targetViewHeight - top);
            case 270:
                return new Rect(targetViewHeight - bottom, left, targetViewHeight - top, right);
            default:
                return new Rect(left, top, right, bottom);
        }
    }
}
