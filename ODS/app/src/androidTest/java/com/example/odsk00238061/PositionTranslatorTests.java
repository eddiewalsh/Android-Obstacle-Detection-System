package com.example.odsk00238061;

import static org.junit.Assert.assertEquals;

import android.graphics.Rect;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.odsk00238061.utils.PositionTranslator;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class PositionTranslatorTests {
    @Test
    public void testTranslateBoundingBox_NoRotation() {
        // Create a PositionTranslator instance with known parameters
        PositionTranslator translator = new PositionTranslator(100, 100,
                200, 200, 0);

        // Create a bounding box to translate
        Rect boundingBox = new Rect(10, 10, 30, 30);

        // Translate the bounding box
        Rect translatedBox = translator.translateBoundingBox(boundingBox);

        // Check if the translation is correct
        assertEquals(20, translatedBox.left);
        assertEquals(20, translatedBox.top);
        assertEquals(60, translatedBox.right);
        assertEquals(60, translatedBox.bottom);
    }

    @Test
    public void testTranslateBoundingBox_Rotation90() {
        // Create a PositionTranslator instance with known parameters
        PositionTranslator translator = new PositionTranslator(100, 100, 200, 200, 90);

        // Create a bounding box to translate
        Rect boundingBox = new Rect(10, 10, 30, 30);

        // Translate the bounding box
        Rect translatedBox = translator.translateBoundingBox(boundingBox);

        // Check if the translation is correct
        assertEquals(20, translatedBox.left);
        assertEquals(140, translatedBox.top);
        assertEquals(60, translatedBox.right);
        assertEquals(180, translatedBox.bottom);
    }
}
