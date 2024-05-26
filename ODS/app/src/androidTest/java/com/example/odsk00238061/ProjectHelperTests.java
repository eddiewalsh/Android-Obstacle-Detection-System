package com.example.odsk00238061;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Vibrator;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.odsk00238061.utils.ProjectHelper;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ProjectHelperTests {
    int bitmapSize = 600;
    int targetBitmapSize = 255;

    @Test
    public void resizeBitmapTest() {
        Bitmap originalBitmap = Bitmap.createBitmap(bitmapSize, bitmapSize,
                Bitmap.Config.ARGB_8888);

        Bitmap resizeBitmap = ProjectHelper.resizeBitmap(originalBitmap, targetBitmapSize);

        assertEquals(resizeBitmap.getHeight(), targetBitmapSize);
        assertEquals(resizeBitmap.getWidth(), targetBitmapSize);
    }

    @Test
    public void getBatteryLifeTest(){
        // Get the application context using InstrumentationRegistry
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // Call the getBatteryLevel function and get the result
        float batteryLevel = ProjectHelper.getBatteryLevel(context);

        // Check if the battery level is within the expected range (0 to 100)
        assertTrue("Battery level should be between 0 and 100", batteryLevel >= 0 && batteryLevel <= 100);
    }

    @Test
    public void testCalculateObstacleLocation() {
        // Define the preview width
        int previewWidth = 1000;

        // Define obstacle rectangles with varying positions
        Rect farLeftObstacle = new Rect(50, 0, 100, 50);
        Rect centerLeftObstacle = new Rect(225, 0, 275, 50);
        Rect centerAheadObstacle = new Rect(450, 0, 500, 50);
        Rect centerRightObstacle = new Rect(600, 0, 650, 50);
        Rect farRightObstacle = new Rect(900, 0, 950, 50);

        // Test the calculateObstacleLocation method with different obstacle positions
        assertEquals("Far Left", ProjectHelper.calculateObstacleLocation(farLeftObstacle, previewWidth));
        assertEquals("Center Left", ProjectHelper.calculateObstacleLocation(centerLeftObstacle, previewWidth));
        assertEquals("Center", ProjectHelper.calculateObstacleLocation(centerAheadObstacle, previewWidth));
        assertEquals("Center Right", ProjectHelper.calculateObstacleLocation(centerRightObstacle, previewWidth));
        assertEquals("Far Right", ProjectHelper.calculateObstacleLocation(farRightObstacle, previewWidth));
    }
}
