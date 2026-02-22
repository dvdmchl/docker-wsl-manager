# Implementation Plan - Track: GH-28-container-config

## Phase 1: Logic & Data Fetching
- [ ] Task: Ensure Jackson is available for pretty printing JSON.
- [ ] Task: Implement `ConfigLogic` in `org.dreamabout.sw.dockerwslmanager.logic`.
    - [ ] Add a method to fetch container inspection data: `inspectContainerCmd(id).exec()`.
    - [ ] Add a method to convert the response object to a pretty-printed JSON string.
- [ ] Task: Write unit tests for `ConfigLogic`.
- [ ] Task: Conductor - User Manual Verification 'Logic & Data Fetching' (Protocol in workflow.md)

## Phase 2: UI Integration
- [ ] Task: Create a new UI component/view for displaying the configuration.
    - [ ] Use a `VBox` with a search `TextField` at the top and a `TextArea` (mono-spaced font) for the JSON content.
    - [ ] Implement basic search/filter logic for the `TextArea`.
- [ ] Task: Update `MainController.createDetailsTab` to include the "⚙ Config" button.
    - [ ] Add the button to the footer `HBox`.
    - [ ] Implement the action to fetch config and open the configuration view in a new tab within `mainTabPane`.
- [ ] Task: Update `shortcuts.properties` and `ShortcutManager` to support `action.details.config` (e.g., `Ctrl+Alt+C`).
- [ ] Task: Conductor - User Manual Verification 'UI Integration' (Protocol in workflow.md)

## Phase 3: Final Verification & Polishing
- [ ] Task: Ensure the configuration view updates correctly if clicked multiple times (e.g., re-selection logic).
- [ ] Task: Verify that the search functionality correctly highlights or jumps to matches in the JSON text.
- [ ] Task: Conductor - User Manual Verification 'Final Verification & Polishing' (Protocol in workflow.md)
