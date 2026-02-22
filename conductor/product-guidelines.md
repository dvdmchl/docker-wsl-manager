# Product Guidelines: Docker-WSL-Manager

## Design Principles
- **Native Integration:** The UI should feel like a natural extension of the Windows desktop environment while respecting JavaFX conventions.
- **Minimalism:** Avoid clutter. Present only the most relevant information for each context (e.g., list view vs. detail view).
- **Responsiveness:** Ensure the UI remains responsive during long-running Docker operations (e.g., pulling images) by using background threads and progress indicators.
- **Clarity:** Use clear, non-technical language for error messages where possible, but retain technical details for advanced users (e.g., in logs).

## UX Guidelines
- **Feedback:** Always provide immediate feedback for user actions. Use loading spinners or progress bars for async tasks.
- **Error Handling:** Gracefully handle connection failures (e.g., if WSL is stopped). Offer actionable advice (e.g., "Start WSL") rather than just raw stack traces.
- **Consistency:** Maintain consistent spacing, typography, and color schemes across all tabs and dialogs.

## Coding Standards
- **Java:** Follow standard Java naming conventions (CamelCase for classes/methods, constant case for static finals).
- **JavaFX:** Keep FXML files clean and separate from controller logic. Use CSS for styling where possible.
- **Testing:** Prioritize unit tests for logic and integration tests for Docker interactions.
- **Documentation:** Document public API methods and complex logic blocks.

## Branding
- **Name:** "Docker-WSL-Manager" (or "DWM" for short).
- **Icon:** Use the existing Docker-themed icon consistently.
- **Tone:** Professional, helpful, and efficient.
