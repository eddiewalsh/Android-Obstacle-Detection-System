package com.example.odsk00238061;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.content.Context;
import android.graphics.Rect;
import android.speech.tts.TextToSpeech;

import androidx.camera.view.PreviewView;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.odsk00238061.utils.Obstacle;
import com.example.odsk00238061.utils.Speaker;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SpeakerTests {
    private Speaker speaker;
    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        speaker = new Speaker(context);
        speaker.potentialObstacle = null; // Reset potentialObstacle to null
    }
    @Test
    public void testProcessDetectedObject() {
        Obstacle obstacle1 = new Obstacle("Obstacle1", new Rect(0, 0, 100, 100));
        Obstacle obstacle2 = new Obstacle("Obstacle2", new Rect(50, 50, 150, 150));

        // Initially, no potential obstacle
        assertNull(speaker.potentialObstacle);

        // Process the first obstacle
        speaker.processDetectedObject(obstacle1);
        assertEquals(obstacle1, speaker.potentialObstacle);

        // Process the same obstacle again
        speaker.processDetectedObject(obstacle1);
        assertEquals(obstacle1, speaker.potentialObstacle);
        assertEquals((Object) 1, speaker.potentialObstacle.getObstacleOccurrence());

        // Process a different obstacle
        speaker.processDetectedObject(obstacle2);
        assertEquals(obstacle2, speaker.potentialObstacle);
        assertEquals((Object) 0, speaker.potentialObstacle.getObstacleOccurrence());
    }

    @Test
    public void testSpeakText() {
        // Define a sample text to be spoken
        String textToSpeak = "This is a test message.";

        // Call the speakText method with the sample text
        speaker.speakText(textToSpeak);

        // Assert that the textToSpeech engine is not null after speaking
        assertNotNull("TextToSpeech engine should not be null after speaking", speaker.textToSpeech);
    }

    @Test
    public void testDestroy() {
        // Call the destroy method
        speaker.Destroy();

        // Assert that the textToSpeech engine is null after destruction
        assertNull("TextToSpeech engine should be null after destruction", speaker.textToSpeech);
    }

}
