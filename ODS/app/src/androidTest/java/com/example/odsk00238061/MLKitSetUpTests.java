package com.example.odsk00238061;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.mlkit.common.model.LocalModel;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MLKitSetUpTests {

    @Test
    public void SetUpObjectDetectorTest(){
        try {
            LocalModel localModel =
                    new LocalModel.Builder()
                            .setAssetFilePath("1.tflite")
                            .build();

            CustomObjectDetectorOptions customObjectDetectorOptions =
                    new CustomObjectDetectorOptions.Builder(localModel)
                            .setDetectorMode(CustomObjectDetectorOptions.STREAM_MODE)
                            .enableClassification()
                            .setClassificationConfidenceThreshold(0.6f)
                            .setMaxPerObjectLabelCount(3)
                            .build();

            ObjectDetector objectDetector =
                    ObjectDetection.getClient(customObjectDetectorOptions);

            assertNotNull("Object detector not null", objectDetector);
        } catch (Exception e) {
            fail("Exception thrown: " + e.getMessage());
        }
    }

    @Test
    public void SetUpBarcodeScannerTest(){

        try {

            BarcodeScannerOptions options =
                    new BarcodeScannerOptions.Builder()
                            .setBarcodeFormats(
                                    Barcode.FORMAT_UPC_A,
                                    Barcode.FORMAT_UPC_E)
                            .build();

            BarcodeScanner barcodeScanner = BarcodeScanning.getClient(options);

            assertNotNull("Barcode Scanner not null", barcodeScanner);

        }  catch (Exception e) {
            fail("Exception thrown: " + e.getMessage());
        }
    }

    @Test
    public void SetUpTextRecognizerTest(){

        try {
            TextRecognizer textRecognizer = TextRecognition
                    .getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

            assertNotNull("Text Recognizer not null", textRecognizer);

        }  catch (Exception e) {
            fail("Exception thrown: " + e.getMessage());
        }
    }
}
