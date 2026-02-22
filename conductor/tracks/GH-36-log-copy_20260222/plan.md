# Implementation Plan - Track: GH-36-log-copy

## Phase 1: Selection Logic Implementation
- [ ] Task: Create `TextFlowSelectionHandler` logic.
    - [ ] Create a handler to track mouse press (anchor) and drag (extent) within the `TextFlow`.
    - [ ] Implement logic to identify which `Text` nodes and what character offsets are within the selection range.
    - [ ] Implement visual highlighting using a transparent background or a managed list of background shapes.
- [ ] Task: Conductor - User Manual Verification 'Selection Logic Implementation' (Protocol in workflow.md)

## Phase 2: Controller Integration
- [ ] Task: Integrate selection handler into `MainController`.
    - [ ] Update `createDetailsTab` to attach the selection handler to every new `logTextFlow`.
    - [ ] Ensure the handler is updated or reset when logs are cleared.
- [ ] Task: Implement Clipboard handling.
    - [ ] Add a `KeyEvent` listener to the `Details` tab or `logScrollPane` to catch `Ctrl+C`.
    - [ ] Use `javafx.scene.input.Clipboard` to put the selected text into the system clipboard.
- [ ] Task: Conductor - User Manual Verification 'Controller Integration' (Protocol in workflow.md)

## Phase 3: Final Polishing and Bug Fixes
- [ ] Task: Refine selection behavior.
    - [ ] Ensure selection works correctly when logs are scrolling.
    - [ ] Fix any edge cases where selection might "jump" or not clear properly.
- [ ] Task: Conductor - User Manual Verification 'Final Polishing and Bug Fixes' (Protocol in workflow.md)
