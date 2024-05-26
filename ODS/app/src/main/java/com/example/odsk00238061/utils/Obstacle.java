package com.example.odsk00238061.utils;

import android.graphics.Rect;
import com.google.mlkit.vision.objects.DetectedObject;

import java.util.List;

/**
 * Represents an obstacle detected in the environment, including its name, location, occurrence, and bounding box.
 */
public class Obstacle {
    // Attributes
    private String obstacleName; // Name of the obstacle
    private String obstacleLocation; // Location of the obstacle (optional)
    private Integer obstacleOccurrence; // Number of times the obstacle has been detected
    private Rect obstacleRect; // Bounding box of the obstacle

    /**
     * Default constructor for the Obstacle class.
     */
    public Obstacle(){ }

    /**
     * Constructor for the Obstacle class with parameters.
     *
     * @param object        Detected object containing labels.
     * @param newBoundingBox New bounding box for the obstacle.
     */
    public Obstacle(DetectedObject object, Rect newBoundingBox){
        // Set obstacle name based on the label with the highest confidence
        if(object.getLabels().isEmpty()) {
            this.obstacleName = "Unknown Obstacle";
        } else {
            this.obstacleName = getLabelWithHighestConfidence(object.getLabels());
        }
        this.obstacleRect = newBoundingBox;
        this.obstacleOccurrence = 0;
    }

    /**
     *  Constructor for the Obstacle class (testing purposes)
     * @param obstacleName  Detected object name
     * @param location Bounding Box for the obstacle
     */
    public Obstacle(String obstacleName, Rect location){
        if(!obstacleName.isEmpty()){
            this.obstacleName = obstacleName;
        } else {
            this.obstacleName = "Unknown Obstacle";
        }

        this.obstacleRect = location;
        this.obstacleOccurrence = 0;
    }

    // Getters and Setters
    public String getObstacleName(){
        return obstacleName;
    }

    public String getObstacleLocation(){
        return obstacleLocation;
    }

    public void setObstacleLocation(String obstacleLocation){
        this.obstacleLocation = obstacleLocation;
    }

    public Rect getObstacleRect() {
        return obstacleRect;
    }

    public Integer getObstacleOccurrence(){
        return obstacleOccurrence;
    }

    public void incrementObstacleOccurrence(){
        this.obstacleOccurrence++;
    }


    // Other methods

    /**
     * Finds the label with the highest confidence from a list of labels.
     *
     * @param labels List of detected object labels.
     * @return The label with the highest confidence.
     */
    public String getLabelWithHighestConfidence(List<DetectedObject.Label> labels) {
        String maxLabel = "";
        float maxConfidence = Float.MIN_VALUE; // Initialize to the smallest possible value

        for (DetectedObject.Label label : labels) {

            float confidence = label.getConfidence();

            if (confidence > maxConfidence) {
                maxConfidence = confidence;
                maxLabel = label.getText();
            }
        }
        return maxLabel;
    }
}
