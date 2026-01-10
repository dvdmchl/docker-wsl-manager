package org.dreamabout.sw.dockerwslmanager;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCombination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class ShortcutManager {
    private static final Logger logger = LoggerFactory.getLogger(ShortcutManager.class);
    private final Properties shortcuts = new Properties();

    public ShortcutManager() {
        try (InputStream input = getClass().getResourceAsStream("/shortcuts.properties")) {
            if (input != null) {
                shortcuts.load(input);
            } else {
                logger.warn("shortcuts.properties not found");
            }
        } catch (IOException e) {
            logger.error("Failed to load shortcuts", e);
        }
    }

    public void configureButton(Button button, String actionKey) {
        String keyStr = shortcuts.getProperty(actionKey);
        if (keyStr == null || keyStr.isEmpty()) {
            return;
        }

        // Parse key combination
        KeyCombination kc;
        try {
            kc = KeyCombination.valueOf(keyStr);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid key combination for {}: {}", actionKey, keyStr, e);
            return;
        }

        // Update Text
        String currentText = button.getText();
        // Strip existing mnemonics if present (simple check)
        if (currentText.contains("_")) {
            currentText = currentText.replace("_", "");
        }
        button.setText(currentText + " (" + keyStr + ")");
        button.setMnemonicParsing(false); // Disable mnemonics as we use accelerators

        // Register Accelerator
        button.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                registerAccelerator(newScene, kc, button);
            }
        });

        // If already attached (dynamic buttons)
        if (button.getScene() != null) {
            registerAccelerator(button.getScene(), kc, button);
        }
    }

    private void registerAccelerator(Scene scene, KeyCombination kc, Button button) {
        scene.getAccelerators().put(kc, () -> {
            if (!button.isDisabled() && button.isVisible()) {
                button.fire();
            }
        });
    }
}
