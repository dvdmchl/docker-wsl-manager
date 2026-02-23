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
    private static final String SHORTCUTS_FILE = "/shortcuts.properties";
    private static final String ACTIVE_KEY_COMB = "activeKeyComb";
    private final Properties shortcuts = new Properties();
    private static final String CONFIG_FILE_PATH = System.getProperty("user.home") + "/.docker-wsl-manager/shortcuts.properties";

    public ShortcutManager() {
        // Load default first
        try (InputStream input = getClass().getResourceAsStream(SHORTCUTS_FILE)) {
            if (input != null) {
                shortcuts.load(input);
            } else {
                logger.warn("shortcuts.properties not found");
            }
        } catch (IOException e) {
            logger.error("Failed to load shortcuts", e);
        }

        // Load user overrides
        java.io.File userConfig = new java.io.File(CONFIG_FILE_PATH);
        if (userConfig.exists()) {
            try (java.io.InputStream input = new java.io.FileInputStream(userConfig)) {
                shortcuts.load(input);
            } catch (IOException e) {
                logger.error("Failed to load user shortcuts", e);
            }
        }
    }

    public String getShortcutsContent() {
        java.io.File userConfig = new java.io.File(CONFIG_FILE_PATH);
        if (userConfig.exists()) {
            try {
                return java.nio.file.Files.readString(userConfig.toPath());
            } catch (IOException e) {
                logger.error("Failed to read user config", e);
            }
        }
        
        // Fallback to default resource content
        try (InputStream input = getClass().getResourceAsStream(SHORTCUTS_FILE)) {
            if (input != null) {
                return new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            logger.error("Failed to read default shortcuts", e);
        }
        return "";
    }

    public void saveShortcuts(String content) throws IOException {
        java.io.File userConfig = new java.io.File(CONFIG_FILE_PATH);
        java.io.File parent = userConfig.getParentFile();
        if (parent != null && !parent.exists()) {
            java.nio.file.Files.createDirectories(parent.toPath());
        }
        java.nio.file.Files.writeString(userConfig.toPath(), content, java.nio.charset.StandardCharsets.UTF_8);
        
        // Reload properties to reflect changes immediately in memory (though UI update requires restart)
        try (java.io.InputStream input = new java.io.FileInputStream(userConfig)) {
            shortcuts.clear(); // Clear to ensure removed keys are gone? 
            // Wait, if I clear, I lose defaults that were NOT in user config?
            // Correct behavior: Load defaults, THEN load user config.
            // So re-run initialization logic.
            try (InputStream defaultInput = getClass().getResourceAsStream(SHORTCUTS_FILE)) {
                if (defaultInput != null) {
                    shortcuts.load(defaultInput);
                }
            }
            shortcuts.load(input);
        }
    }

    public void configureButton(Button button, String actionKey) {
        String keyStr = shortcuts.getProperty(actionKey);
        if (keyStr == null || keyStr.isEmpty()) {
            return;
        }

        // Parse key combination
        KeyCombination newKc;
        try {
            newKc = KeyCombination.valueOf(keyStr);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid key combination for {}: {}", actionKey, keyStr, e);
            return;
        }

        // 1. Text Handling
        // Retrieve or store original text to avoid appending shortcuts multiple times
        String originalText = (String) button.getProperties().get("originalText");
        if (originalText == null) {
            originalText = button.getText();
            // Initial strip of mnemonics if they exist in the FIRST text
            if (originalText.contains("_")) {
                originalText = originalText.replace("_", "");
            }
            button.getProperties().put("originalText", originalText);
        }
        button.setText(originalText + " (" + keyStr + ")");
        button.setMnemonicParsing(false);

        // 2. Accelerator Handling
        // Remove old accelerator if exists from current scene
        KeyCombination oldKc = (KeyCombination) button.getProperties().get(ACTIVE_KEY_COMB);
        if (oldKc != null && button.getScene() != null) {
            button.getScene().getAccelerators().remove(oldKc);
        }
        
        // Save new key
        button.getProperties().put(ACTIVE_KEY_COMB, newKc);

        // Register new accelerator
        // Attach listener only once to handle scene changes
        if (button.getProperties().get("listenerAttached") == null) {
             button.sceneProperty().addListener((obs, oldScene, newScene) -> {
                 if (oldScene != null) {
                     KeyCombination kc = (KeyCombination) button.getProperties().get(ACTIVE_KEY_COMB);
                     if (kc != null) {
                         oldScene.getAccelerators().remove(kc);
                     }
                 }
                 if (newScene != null) {
                     KeyCombination kc = (KeyCombination) button.getProperties().get(ACTIVE_KEY_COMB);
                     if (kc != null) {
                         registerAccelerator(newScene, kc, button);
                     }
                 }
             });
             button.getProperties().put("listenerAttached", true);
        }
        
        // If already attached to a scene, register immediately
        if (button.getScene() != null) {
            registerAccelerator(button.getScene(), newKc, button);
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
