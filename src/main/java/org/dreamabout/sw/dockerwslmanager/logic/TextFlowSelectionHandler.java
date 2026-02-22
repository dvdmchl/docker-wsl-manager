package org.dreamabout.sw.dockerwslmanager.logic;

import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.text.HitInfo;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles text selection and copying for a JavaFX TextFlow.
 */
public class TextFlowSelectionHandler {

    private final TextFlow textFlow;
    private final Path selectionPath = new Path();
    private int selectionStart = -1;
    private int selectionEnd = -1;
    private boolean mousePressed = false;

    public TextFlowSelectionHandler(TextFlow textFlow) {
        this.textFlow = textFlow;
        
        selectionPath.setFill(Color.BLUE.deriveColor(0, 1, 1, 0.3));
        selectionPath.setStroke(Color.TRANSPARENT);
        selectionPath.setMouseTransparent(true);
        selectionPath.setManaged(false); // CRITICAL: Stop Path from affecting TextFlow layout
        
        // Add selection path as the first child to be behind text
        ensureSelectionPathAttached();

        textFlow.addEventHandler(MouseEvent.MOUSE_PRESSED, this::handleMousePressed);
        textFlow.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::handleMouseDragged);
        textFlow.addEventHandler(MouseEvent.MOUSE_RELEASED, this::handleMouseReleased);
    }

    // For testing
    void setSelectionRange(int start, int end) {
        this.selectionStart = start;
        this.selectionEnd = end;
    }

    public boolean isSelecting() {
        return mousePressed || (selectionStart != -1 && selectionEnd != -1 && selectionStart != selectionEnd);
    }

    private void handleMousePressed(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY) {
            return;
        }
        
        mousePressed = true;
        clearSelection();
        HitInfo hit = textFlow.hitTest(new javafx.geometry.Point2D(event.getX(), event.getY()));
        selectionStart = hit.getCharIndex();
        selectionEnd = selectionStart;
    }

    private void handleMouseDragged(MouseEvent event) {
        if (!mousePressed) {
            return;
        }

        HitInfo hit = textFlow.hitTest(new javafx.geometry.Point2D(event.getX(), event.getY()));
        selectionEnd = hit.getCharIndex();
        updateSelection();
    }

    private void handleMouseReleased(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY) {
            mousePressed = false;
        }
    }

    private void updateSelection() {
        ensureSelectionPathAttached();
        if (selectionStart < 0 || selectionEnd < 0 || selectionStart == selectionEnd) {
            selectionPath.getElements().clear();
            return;
        }

        int start = Math.min(selectionStart, selectionEnd);
        int end = Math.max(selectionStart, selectionEnd);

        PathElement[] elements = textFlow.rangeShape(start, end);
        selectionPath.getElements().setAll(elements);
    }

    private void ensureSelectionPathAttached() {
        if (!textFlow.getChildren().contains(selectionPath)) {
            textFlow.getChildren().add(0, selectionPath);
        }
    }

    public void clearSelection() {
        selectionStart = -1;
        selectionEnd = -1;
        selectionPath.getElements().clear();
    }

    /**
     * Selects all text in the TextFlow.
     */
    public void selectAll() {
        int totalLength = 0;
        for (javafx.scene.Node node : textFlow.getChildren()) {
            if (node instanceof Text textNode) {
                totalLength += textNode.getText().length();
            }
        }
        
        if (totalLength > 0) {
            selectionStart = 0;
            selectionEnd = totalLength;
            updateSelection();
        }
    }

    /**
     * Gets the currently selected text as a string.
     */
    public String getSelectedText() {
        if (selectionStart < 0 || selectionEnd < 0 || selectionStart == selectionEnd) {
            return "";
        }

        int start = Math.min(selectionStart, selectionEnd);
        int end = Math.max(selectionStart, selectionEnd);

        StringBuilder sb = new StringBuilder();
        int currentOffset = 0;

        for (javafx.scene.Node node : textFlow.getChildren()) {
            if (node instanceof Text textNode) {
                String text = textNode.getText();
                int nodeLength = text.length();
                
                int nodeStart = currentOffset;
                int nodeEnd = currentOffset + nodeLength;

                if (nodeEnd > start && nodeStart < end) {
                    int overlapStart = Math.max(start, nodeStart) - currentOffset;
                    int overlapEnd = Math.min(end, nodeEnd) - currentOffset;
                    sb.append(text, overlapStart, overlapEnd);
                }
                
                currentOffset += nodeLength;
            }
        }

        return sb.toString();
    }

    /**
     * Copies all text in the TextFlow to the system clipboard.
     */
    public void copyAllToClipboard() {
        String allText = getAllText();
        if (!allText.isEmpty()) {
            ClipboardContent content = new ClipboardContent();
            content.putString(allText);
            Clipboard.getSystemClipboard().setContent(content);
        }
    }

    private String getAllText() {
        StringBuilder sb = new StringBuilder();
        for (javafx.scene.Node node : textFlow.getChildren()) {
            if (node instanceof Text textNode) {
                sb.append(textNode.getText());
            }
        }
        return sb.toString();
    }

    /**
     * Copies the selected text to the system clipboard.
     */
    public void copyToClipboard() {
        String selectedText = getSelectedText();
        if (!selectedText.isEmpty()) {
            ClipboardContent content = new ClipboardContent();
            content.putString(selectedText);
            Clipboard.getSystemClipboard().setContent(content);
        }
    }
}
