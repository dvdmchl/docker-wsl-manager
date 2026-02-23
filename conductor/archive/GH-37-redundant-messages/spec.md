# Specification: Do not popup redundant messages (GH-37)

## 1. Overview
Remove redundant popup notifications for successful operations. The user interface already reflects these state changes through visual indicators (icons, status text), making the popups unnecessary and intrusive.

## 2. Functional Requirements
- **Suppress Success Popups:** The application must NOT display a popup dialog for the following successful events:
  - Connection established to Docker daemon.
  - Container started.
  - Container stopped.
  - Container restarted.
  - Container removed.
  - Image operations (pull/remove).
- **Retain Error Popups:** The application MUST continue to display popup dialogs for any *failed* operations (e.g., connection timeout, start failure).
- **UI Feedback:** The existing UI elements (status icons, text labels) must be the sole indicators of successful state transitions.

## 3. Acceptance Criteria
- [ ] Launching the app and connecting to Docker shows the main window *without* a "Connection Established" popup.
- [ ] Starting a container updates its icon to "Running" *without* a "Container Started" popup.
- [ ] Stopping a container updates its icon to "Stopped" *without* a "Container Stopped" popup.
- [ ] Failure scenarios still trigger an error popup.

## 4. Out of Scope
- Implementing new notification systems (toasts, snackbars).
- Changing the visual design of the container list/grid.
