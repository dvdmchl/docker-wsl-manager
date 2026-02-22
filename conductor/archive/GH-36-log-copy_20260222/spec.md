# Specification: Allow to copy text from container Details log window

## Overview
Enable users to select and copy text from the container log area in the **Details** tab. Since the log currently uses a `TextFlow` to support ANSI colors, built-in text selection is not available. This track implements custom selection logic and clipboard integration.

## Functional Requirements
1.  **Mouse Selection:**
    *   Implement click-and-drag selection across the `TextFlow`.
    *   Support partial selection (selecting specific words or lines).
    *   Clicking outside the selection or starting a new selection should clear the previous one.
2.  **Visual Feedback:**
    *   Highlight selected text with a standard selection color (e.g., semi-transparent blue).
3.  **Clipboard Integration:**
    *   Capture the `Ctrl+C` keyboard shortcut when the **Details** tab is focused.
    *   Copy the selected text (maintaining line breaks) to the system clipboard.
4.  **Content Integrity:**
    *   Ensure the copied text reflects the actual log content, even if it spans multiple `Text` nodes or is updated during selection.

## Non-Functional Requirements
*   **Performance:** Selection calculations should be efficient to avoid UI stuttering during active log streaming.
*   **Platform Consistency:** The selection behavior and keyboard shortcuts should feel native to Windows.

## Out of Scope
*   Context menu (Right-click > Copy).
*   Rich text copying (only plain text will be copied).
*   Automatic "Copy on Select" functionality.

## Acceptance Criteria
1.  User can drag the mouse over logs to highlight text.
2.  Pressing `Ctrl+C` copies the highlighted text to the clipboard.
3.  The copied text contains the correct characters and line breaks.
4.  Selection is cleared when clicking elsewhere in the log area.
