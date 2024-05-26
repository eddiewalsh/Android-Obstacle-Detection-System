package com.example.odsk00238061;

import com.google.mlkit.common.model.LocalModel;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions;

import java.io.File;
import java.net.URL;

public class TestHelper {

    public static ObjectDetector ObjectDetectionConfiguration(){
        LocalModel localModel =
                new LocalModel.Builder()
                        .setAssetFilePath("1.tflite")
                        .build();

        CustomObjectDetectorOptions customObjectDetectorOptions =
                new CustomObjectDetectorOptions.Builder(localModel)
                        .setDetectorMode(CustomObjectDetectorOptions.SINGLE_IMAGE_MODE)
                        .enableClassification()
                        .setClassificationConfidenceThreshold(0.6f)
                        .setMaxPerObjectLabelCount(3)
                        .build();

        return ObjectDetection.getClient(customObjectDetectorOptions);
    }
}
