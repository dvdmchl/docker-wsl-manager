# Implementation Plan - Track: GH-36-log-copy

## Phase 1: Selection Logic Implementation
- [x] Task: Create `TextFlowSelectionHandler` logic.
    - [x] Create a handler to track mouse press (anchor) and drag (extent) within the `TextFlow`.
    - [x] Implement logic to identify which `Text` nodes and what character offsets are within the selection range.
    - [x] Implement visual highlighting using a transparent background or a managed list of background shapes.
- [x] Task: Conductor - User Manual Verification 'Selection Logic Implementation' (Protocol in workflow.md)

## Phase 2: Controller Integration
- [x] Task: Integrate selection handler into `MainController`.
    - [x] Update `createDetailsTab` to attach the selection handler to every new `logTextFlow`.
    - [x] Ensure the handler is updated or reset when logs are cleared.
- [x] Task: Implement Clipboard handling.
    - [x] Add a `KeyEvent` listener to the `Details` tab or `logScrollPane` to catch `Ctrl+C`.
    - [x] Use `javafx.scene.input.Clipboard` to put the selected text into the system clipboard.
- [x] Task: Conductor - User Manual Verification 'Controller Integration' (Protocol in workflow.md)

## Phase 3: Final Polishing and Bug Fixes
- [x] Task: Refine selection behavior.
    - [x] Ensure selection works correctly when logs are scrolling.
    - [x] Fix any edge cases where selection might "jump" or not clear properly.
- [x] Task: Conductor - User Manual Verification 'Final Polishing and Bug Fixes' (Protocol in workflow.md)

## Phase: Review Fixes
- [x] Task: Apply review suggestions 24bc9b0
