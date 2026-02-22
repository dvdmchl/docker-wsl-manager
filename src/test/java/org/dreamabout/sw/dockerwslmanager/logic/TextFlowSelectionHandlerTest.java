package org.dreamabout.sw.dockerwslmanager.logic;

import javafx.application.Platform;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TextFlowSelectionHandlerTest {

    @BeforeAll
    public static void initJFX() throws InterruptedException {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            latch.await();
        } catch (IllegalStateException e) {
            // Already initialized
        }
    }

    @Test
    public void testGetSelectedText() {
        TextFlow textFlow = new TextFlow();
        Text t1 = new Text("Hello ");
        Text t2 = new Text("World");
        textFlow.getChildren().addAll(t1, t2);

        TextFlowSelectionHandler handler = new TextFlowSelectionHandler(textFlow);
        
        // Select "Hello" (indices 0 to 5)
        handler.setSelectionRange(0, 5);
        assertEquals("Hello", handler.getSelectedText());

        // Select "World" (indices 6 to 11)
        handler.setSelectionRange(6, 11);
        assertEquals("World", handler.getSelectedText());

        // Select "o Wor" (indices 4 to 9)
        handler.setSelectionRange(4, 9);
        assertEquals("o Wor", handler.getSelectedText());
        
        // Reverse selection
        handler.setSelectionRange(9, 4);
        assertEquals("o Wor", handler.getSelectedText());
    }

    @Test
    public void testSelectAll() {
        TextFlow textFlow = new TextFlow();
        Text t1 = new Text("Part 1 ");
        Text t2 = new Text("Part 2");
        textFlow.getChildren().addAll(t1, t2);

        TextFlowSelectionHandler handler = new TextFlowSelectionHandler(textFlow);
        handler.selectAll();
        
        assertEquals("Part 1 Part 2", handler.getSelectedText());
    }
}
