# Implementation Plan - Track: GH-28-container-config

## Phase 1: Logic & Data Fetching
- [x] Task: Ensure Jackson is available for pretty printing JSON.
- [x] Task: Implement `ConfigLogic` in `org.dreamabout.sw.dockerwslmanager.logic`.
    - [x] Add a method to fetch container inspection data: `inspectContainerCmd(id).exec()`.
    - [x] Add a method to convert the response object to a pretty-printed JSON string.
- [x] Task: Write unit tests for `ConfigLogic`.
- [x] Task: Conductor - User Manual Verification 'Logic & Data Fetching' (Protocol in workflow.md)

## Phase 2: UI Integration
- [x] Task: Create a new UI component/view for displaying the configuration.
    - [x] Use a `VBox` with a search `TextField` at the top and a `TextArea` (mono-spaced font) for the JSON content.
    - [x] Implement basic search/filter logic for the `TextArea`.
- [x] Task: Update `MainController.createDetailsTab` to include the "⚙ Config" button.
    - [x] Add the button to the footer `HBox`.
    - [x] Implement the action to fetch config and open the configuration view in a new tab within `mainTabPane`.
- [x] Task: Update `shortcuts.properties` and `ShortcutManager` to support `action.details.config` (e.g., `Ctrl+Alt+C`).
- [x] Task: Conductor - User Manual Verification 'UI Integration' (Protocol in workflow.md)

## Phase 3: Final Verification & Polishing
- [x] Task: Ensure the configuration view updates correctly if clicked multiple times (e.g., re-selection logic).
- [x] Task: Verify that the search functionality correctly highlights or jumps to matches in the JSON text.
- [x] Task: Conductor - User Manual Verification 'Final Verification & Polishing' (Protocol in workflow.md)
