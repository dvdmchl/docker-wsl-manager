package org.dreamabout.sw.dockerwslmanager;

import com.github.dockerjava.api.command.InspectVolumeResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerPort;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.Network;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.dreamabout.sw.dockerwslmanager.model.ContainerViewItem;
import org.dreamabout.sw.dockerwslmanager.model.ImageViewItem;
import org.dreamabout.sw.dockerwslmanager.model.VolumeViewItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class MainController {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);
    private static final String UNGROUPED_LABEL = "Ungrouped";

    private final ShortcutManager shortcutManager = new ShortcutManager();
    private final SettingsManager settingsManager = new SettingsManager();
    private DockerConnectionManager connectionManager;

    @FXML
    private Label connectionStatusLabel;
    @FXML
    private Button connectAutoButton;
    @FXML
    private Button disconnectButton;

    @FXML
    private TabPane mainTabPane;
    
    @FXML
    private javafx.scene.control.CheckMenuItem autoRefreshMenuItem;
    private javafx.animation.Timeline autoRefreshTimeline;

    // Containers tab
    @FXML
    private TreeTableView<ContainerViewItem> containersTable;
    @FXML
    private TreeTableColumn<ContainerViewItem, String> containerIdColumn;
    @FXML
    private TreeTableColumn<ContainerViewItem, String> containerNameColumn;
    @FXML
    private TreeTableColumn<ContainerViewItem, String> containerImageColumn;
    @FXML
    private TreeTableColumn<ContainerViewItem, String> containerPortsColumn;
    @FXML
    private TreeTableColumn<ContainerViewItem, String> containerStatusColumn;
    @FXML
    private Button refreshContainersButton;
    @FXML
    private Button startContainerButton;
    @FXML
    private Button stopContainerButton;
    @FXML
    private Button restartContainerButton;
    @FXML
    private Button removeContainerButton;
    @FXML
    private Button openDetailsButton;
    @FXML
    private Button attachConsoleButton;

    // Images tab
    @FXML
    private TreeTableView<ImageViewItem> imagesTable;
    @FXML
    private TreeTableColumn<ImageViewItem, String> imageIdColumn;
    @FXML
    private TreeTableColumn<ImageViewItem, String> imageRepoColumn;
    @FXML
    private TreeTableColumn<ImageViewItem, String> imageTagColumn;
    @FXML
    private TreeTableColumn<ImageViewItem, String> imageSizeColumn;
    @FXML
    private Button refreshImagesButton;
    @FXML
    private Button removeImageButton;
    @FXML
    private Button pullImageButton;

    // Volumes tab
    @FXML
    private TreeTableView<VolumeViewItem> volumesTable;
    @FXML
    private TreeTableColumn<VolumeViewItem, String> volumeNameColumn;
    @FXML
    private TreeTableColumn<VolumeViewItem, String> volumeDriverColumn;
    @FXML
    private TreeTableColumn<VolumeViewItem, String> volumeMountpointColumn;
    @FXML
    private Button refreshVolumesButton;
    @FXML
    private Button removeVolumeButton;

    // Networks tab
    @FXML
    private TableView<Network> networksTable;
    @FXML
    private TableColumn<Network, String> networkIdColumn;
    @FXML
    private TableColumn<Network, String> networkNameColumn;
    @FXML
    private TableColumn<Network, String> networkDriverColumn;
    @FXML
    private TableColumn<Network, String> networkScopeColumn;
    @FXML
    private Button refreshNetworksButton;
    @FXML
    private Button removeNetworkButton;

    @FXML
    public void initialize() {
        connectionManager = new DockerConnectionManager();

        updateConnectionStatus();

        // Auto-connect on startup
        autoConnectOnStartup();

        // Initialize containers table
        if (containerNameColumn != null) {
            containersTable.setRowFactory(tv -> {
                javafx.scene.control.TreeTableRow<ContainerViewItem> row = new javafx.scene.control.TreeTableRow<>();
                row.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2 && (!row.isEmpty())) {
                        ContainerViewItem item = row.getItem();
                        if (item != null && !item.isGroup()) {
                            openContainerDetails(item.getContainer());
                        }
                    }
                });
                return row;
            });

            containersTable.setOnKeyPressed(event -> {
                if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                    TreeItem<ContainerViewItem> selectedItem = containersTable.getSelectionModel().getSelectedItem();
                    if (selectedItem != null && !selectedItem.getValue().isGroup()) {
                        openContainerDetails(selectedItem.getValue().getContainer());
                        event.consume();
                    }
                }
            });

            containerNameColumn.setCellValueFactory(data ->
                    new SimpleStringProperty(data.getValue().getValue().getName()));
            
            containerIdColumn.setCellValueFactory(data -> {
                ContainerViewItem item = data.getValue().getValue();
                if (item.isGroup()) {
                    return new SimpleStringProperty("");
                }
                return new SimpleStringProperty(item.getContainer().getId()
                        .substring(0, Math.min(12, item.getContainer().getId().length())));
            });
            
            containerImageColumn.setCellValueFactory(data -> {
                ContainerViewItem item = data.getValue().getValue();
                if (item.isGroup()) {
                    return new SimpleStringProperty("");
                }
                return new SimpleStringProperty(item.getContainer().getImage());
            });
            
            // Ports column - will be rendered as hyperlinks in cell factory
            containerPortsColumn.setCellValueFactory(data -> {
                ContainerViewItem item = data.getValue().getValue();
                if (item.isGroup()) {
                    return new SimpleStringProperty("");
                }
                return new SimpleStringProperty(formatPorts(item.getContainer()));
            });
            
            // Custom cell factory to render ports as clickable hyperlinks
            containerPortsColumn.setCellFactory(column -> new TreeTableCell<ContainerViewItem, String>() {
                @Override
                protected void updateItem(String portsString, boolean empty) {
                    super.updateItem(portsString, empty);
                    
                    if (empty || portsString == null || portsString.isEmpty()) {
                        setGraphic(null);
                        setText(null);
                    } else {
                        // Get the container item to check if it's running
                        TreeItem<ContainerViewItem> treeItem = getTreeTableRow().getTreeItem();
                        if (treeItem != null && !treeItem.getValue().isGroup()) {
                            Container container = treeItem.getValue().getContainer();
                            boolean isRunning = container.getState() != null 
                                    && container.getState().equalsIgnoreCase("running");
                            
                            if (isRunning && container.getPorts() != null && container.getPorts().length > 0) {
                                // Create a FlowPane to hold hyperlinks
                                FlowPane flowPane = new FlowPane();
                                flowPane.setHgap(10);
                                flowPane.setVgap(5);
                                
                                for (ContainerPort port : container.getPorts()) {
                                    if (port.getPublicPort() != null) {
                                        String url = buildPortUrl(port);
                                        Hyperlink hyperlink = new Hyperlink(formatPortDisplay(port));
                                        hyperlink.setOnAction(e -> openInBrowser(url));
                                        hyperlink.setStyle("-fx-text-fill: blue; -fx-underline: true;");
                                        flowPane.getChildren().add(hyperlink);
                                    }
                                }
                                
                                setGraphic(flowPane);
                                setText(null);
                            } else {
                                // Container not running or no ports - show as plain text
                                setGraphic(null);
                                setText(portsString);
                            }
                        } else {
                            // Group item - show as plain text
                            setGraphic(null);
                            setText(portsString);
                        }
                    }
                }
            });
            
            containerStatusColumn.setCellValueFactory(data -> {
                ContainerViewItem item = data.getValue().getValue();
                if (item.isGroup()) {
                    return new SimpleStringProperty("");
                }
                return new SimpleStringProperty(item.getContainer().getStatus());
            });
            
            // Add custom cell factory to color status based on container state
            containerStatusColumn.setCellFactory(column -> new TreeTableCell<ContainerViewItem, String>() {
                @Override
                protected void updateItem(String status, boolean empty) {
                    super.updateItem(status, empty);
                    
                    if (empty || status == null || status.isEmpty()) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(status);
                        // Check if status indicates running (typically starts with "Up")
                        // or stopped/exited (typically starts with "Exited" or "Created")
                        String lowerStatus = status.toLowerCase(java.util.Locale.ROOT);
                        if (lowerStatus.startsWith("up")) {
                            setStyle("-fx-text-fill: green;");
                        } else if (lowerStatus.startsWith("exited")
                                   || lowerStatus.startsWith("created")) {
                            setStyle("-fx-text-fill: red;");
                        } else {
                            setStyle("");
                        }
                    }
                }
            });

            // Add selection listener to update buttons
            containersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> 
                updateContainerActionButtons(newVal));
        }

        // Initialize images table
        if (imageRepoColumn != null) {
            imageRepoColumn.setCellValueFactory(data -> 
                    new SimpleStringProperty(data.getValue().getValue().getName()));
            
            imageIdColumn.setCellValueFactory(data -> {
                ImageViewItem item = data.getValue().getValue();
                if (item.isGroup()) {
                    return new SimpleStringProperty("");
                }
                return new SimpleStringProperty(item.getImage().getId()
                        .replace("sha256:", "").substring(0, 12));
            });
            
            imageTagColumn.setCellValueFactory(data -> {
                ImageViewItem item = data.getValue().getValue();
                if (item.isGroup()) {
                    return new SimpleStringProperty("");
                }
                String[] repoTags = item.getImage().getRepoTags();
                if (repoTags != null && repoTags.length > 0) {
                    String[] parts = repoTags[0].split(":");
                    return new SimpleStringProperty(parts.length > 1 ? parts[1] : "latest");
                }
                return new SimpleStringProperty("<none>");
            });
            
            imageSizeColumn.setCellValueFactory(data -> {
                ImageViewItem item = data.getValue().getValue();
                if (item.isGroup()) {
                    return new SimpleStringProperty("");
                }
                return new SimpleStringProperty(formatSize(item.getImage().getSize()));
            });
        }

        // Initialize volumes table
        if (volumeNameColumn != null) {
            volumeNameColumn.setCellValueFactory(data ->
                    new SimpleStringProperty(data.getValue().getValue().getName()));
            
            volumeDriverColumn.setCellValueFactory(data -> {
                VolumeViewItem item = data.getValue().getValue();
                if (item.isGroup()) {
                    return new SimpleStringProperty("");
                }
                return new SimpleStringProperty(item.getVolume().getDriver());
            });
            
            volumeMountpointColumn.setCellValueFactory(data -> {
                VolumeViewItem item = data.getValue().getValue();
                if (item.isGroup()) {
                    return new SimpleStringProperty("");
                }
                return new SimpleStringProperty(item.getVolume().getMountpoint());
            });
        }

        // Initialize networks table
        if (networkIdColumn != null) {
            networkIdColumn.setCellValueFactory(data ->
                    new SimpleStringProperty(data.getValue().getId()
                            .substring(0, Math.min(12, data.getValue().getId().length()))));
            networkNameColumn.setCellValueFactory(data ->
                    new SimpleStringProperty(data.getValue().getName()));
            networkDriverColumn.setCellValueFactory(data ->
                    new SimpleStringProperty(data.getValue().getDriver()));
            networkScopeColumn.setCellValueFactory(data ->
                    new SimpleStringProperty(data.getValue().getScope()));
        }

        // Configure shortcuts
        configureAllShortcuts();
        
        // Auto-refresh default enabled
        if (autoRefreshMenuItem != null) {
            autoRefreshMenuItem.setSelected(true);
        }
        setupAutoRefreshTimeline();

        // Check for updates on startup
        performUpdateCheck(true);
    }

    private void configureAllShortcuts() {
        // Static buttons
        shortcutManager.configureButton(connectAutoButton, "action.connect");
        shortcutManager.configureButton(disconnectButton, "action.disconnect");
        
        shortcutManager.configureButton(refreshContainersButton, "action.container.refresh");
        shortcutManager.configureButton(startContainerButton, "action.container.start");
        shortcutManager.configureButton(stopContainerButton, "action.container.stop");
        shortcutManager.configureButton(restartContainerButton, "action.container.restart");
        shortcutManager.configureButton(removeContainerButton, "action.container.remove");
        shortcutManager.configureButton(openDetailsButton, "action.container.details");
        shortcutManager.configureButton(attachConsoleButton, "action.container.attach");
        
        shortcutManager.configureButton(refreshImagesButton, "action.image.refresh");
        shortcutManager.configureButton(pullImageButton, "action.image.pull");
        shortcutManager.configureButton(removeImageButton, "action.image.remove");
        
        shortcutManager.configureButton(refreshVolumesButton, "action.volume.refresh");
        shortcutManager.configureButton(removeVolumeButton, "action.volume.remove");
        
        shortcutManager.configureButton(refreshNetworksButton, "action.network.refresh");
        shortcutManager.configureButton(removeNetworkButton, "action.network.remove");

        // Dynamic buttons in tabs
        for (javafx.scene.control.Tab tab : mainTabPane.getTabs()) {
            if (tab.getContent() instanceof BorderPane) {
                BorderPane layout = (BorderPane) tab.getContent();
                javafx.scene.Node bottom = layout.getBottom();
                if (bottom instanceof HBox) {
                    HBox footer = (HBox) bottom;
                    for (javafx.scene.Node node : footer.getChildren()) {
                        if (node instanceof Button) {
                            Button btn = (Button) node;
                            Object userData = btn.getUserData();
                            if (userData instanceof String) {
                                String actionKey = (String) userData;
                                if (actionKey.startsWith("action.")) {
                                    shortcutManager.configureButton(btn, actionKey);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    private void setupAutoRefreshTimeline() {
        if (autoRefreshTimeline != null) {
            autoRefreshTimeline.stop();
        }
        
        int interval = settingsManager.getAutoRefreshInterval();
        if (autoRefreshMenuItem != null) {
            autoRefreshMenuItem.setText("Auto-refresh Containers (" + interval + "s)");
        }

        autoRefreshTimeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.seconds(interval), e -> {
                // Only refresh if connected and containers tab is selected
                if (connectionManager.isConnected() && 
                    mainTabPane.getSelectionModel().getSelectedItem() != null &&
                    "Containers".equals(mainTabPane.getSelectionModel().getSelectedItem().getText())) {
                    refreshContainers();
                }
            })
        );
        autoRefreshTimeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        
        if (autoRefreshMenuItem != null && autoRefreshMenuItem.isSelected()) {
            autoRefreshTimeline.play();
        }
    }

    @FXML
    private void handleCheckForUpdatesAction() {
        performUpdateCheck(false);
    }

    private void performUpdateCheck(boolean silentIfLatest) {
        new Thread(() -> {
            UpdateManager updateManager = new UpdateManager();
            Optional<UpdateManager.ReleaseInfo> update = updateManager.checkForUpdates();
            Platform.runLater(() -> {
                if (update.isPresent()) {
                    UpdateManager.ReleaseInfo info = update.get();
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Update Available");
                    alert.setHeaderText("A new version is available: " + info.getTagName());
                    alert.setContentText("Release Notes:\n" + info.getBody() + "\n\nDo you want to view/download it?");
                    
                    ButtonType openBtn = new ButtonType("Open Download Page");
                    ButtonType cancelBtn = new ButtonType("Cancel", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
                    alert.getButtonTypes().setAll(openBtn, cancelBtn);
                    
                    Optional<ButtonType> result = alert.showAndWait();
                    if (result.isPresent() && result.get() == openBtn) {
                        openInBrowser(info.getHtmlUrl());
                    }
                } else if (!silentIfLatest) {
                    showAlert(Alert.AlertType.INFORMATION, "Update Check", 
                            "You are using the latest version (" + updateManager.getCurrentVersion() + ").");
                }
            });
        }).start();
    }

    @FXML
    private void handleAboutAction() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About Docker WSL Manager");
        alert.setHeaderText("Docker WSL Manager v1.1.0");
        alert.setContentText("A JavaFX application to manage Docker instances running in WSL 2.\n\n" +
                "Source code: https://github.com/dvdmchl/Docker-WSL-Manager");
        alert.showAndWait();
    }

    @FXML
    private void handleGeneralSettingsAction() {
        TextInputDialog dialog = new TextInputDialog(String.valueOf(settingsManager.getAutoRefreshInterval()));
        dialog.setTitle("General Settings");
        dialog.setHeaderText("Configure Auto-refresh");
        dialog.setContentText("Interval (seconds):");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(val -> {
            try {
                int seconds = Integer.parseInt(val);
                if (seconds < 1) seconds = 1;
                settingsManager.setAutoRefreshInterval(seconds);
                settingsManager.saveSettings();
                setupAutoRefreshTimeline();
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please enter a valid number.");
            } catch (Exception e) {
                logger.error("Failed to save settings", e);
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to save settings: " + e.getMessage());
            }
        });
    }

    @FXML
    private void handleKeybindsAction() {
        javafx.scene.control.Dialog<String> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Edit Keybinds");
        dialog.setHeaderText("Edit shortcuts.properties\n(Changes apply immediately)");

        javafx.scene.control.ButtonType saveButtonType = new javafx.scene.control.ButtonType("Save", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, javafx.scene.control.ButtonType.CANCEL);

        TextArea textArea = new TextArea(shortcutManager.getShortcutsContent());
        textArea.setEditable(true);
        textArea.setWrapText(false);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, javafx.scene.layout.Priority.ALWAYS);
        GridPane.setHgrow(textArea, javafx.scene.layout.Priority.ALWAYS);

        GridPane grid = new GridPane();
        grid.setMaxWidth(Double.MAX_VALUE);
        grid.add(textArea, 0, 0);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return textArea.getText();
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(content -> {
            try {
                shortcutManager.saveShortcuts(content);
                configureAllShortcuts(); // Re-apply shortcuts immediately
                showAlert(Alert.AlertType.INFORMATION, "Success", "Keybinds saved and applied.");
            } catch (Exception e) {
                logger.error("Failed to save keybinds", e);
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to save keybinds: " + e.getMessage());
            }
        });
    }

    @FXML
    private void handleAutoRefreshAction() {
        if (autoRefreshTimeline != null) {
            if (autoRefreshMenuItem.isSelected()) {
                autoRefreshTimeline.play();
            } else {
                autoRefreshTimeline.stop();
            }
        }
    }

    @FXML
    private void handleConnectAuto() {
        if (connectionManager.connectAutoDiscover()) {
            updateConnectionStatus();
            showAlert(Alert.AlertType.INFORMATION, "Connection Successful",
                    "Auto-discovered and connected to Docker in WSL.");
            refreshAll();
        } else {
            showAlert(Alert.AlertType.ERROR, "Connection Failed",
                    "Failed to auto-discover Docker in WSL. Make sure WSL is running and Docker is accessible.");
        }
    }

    @FXML
    private void handleDisconnect() {
        connectionManager.disconnect();
        updateConnectionStatus();
        clearAllTables();
    }

    @FXML
    private void handleRefreshContainers() {
        refreshContainers();
    }

    @FXML
    private void handleStartContainer() {
        // Check if a group is selected
        if (isGroupSelected()) {
            List<Container> containers = getSelectedContainerGroup();
            if (containers.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "No Containers", "The selected group has no containers.");
                return;
            }

            String groupName = containersTable.getSelectionModel().getSelectedItem().getValue().getName();
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirm Start");
            confirm.setHeaderText("Start Container Group");
            confirm.setContentText("Are you sure you want to start all " + containers.size() 
                    + " containers in group '" + groupName + "'?");

            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                startContainers(containers);
            }
            return;
        }

        // Handle single container
        Container selected = getSelectedContainer();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a container to start.");
            return;
        }

        try {
            connectionManager.getDockerClient().startContainerCmd(selected.getId()).exec();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Container started successfully.");
            refreshContainers();
        } catch (Exception e) {
            logger.error("Failed to start container", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to start container: " + e.getMessage());
        }
    }

    private void startContainers(List<Container> containers) {
        int successCount = 0;
        int failureCount = 0;
        StringBuilder errors = new StringBuilder();

        for (Container container : containers) {
            try {
                connectionManager.getDockerClient().startContainerCmd(container.getId()).exec();
                successCount++;
                logger.info("Started container: {}", getContainerName(container));
            } catch (Exception e) {
                failureCount++;
                logger.error("Failed to start container: {}", getContainerName(container), e);
                errors.append("\n- ").append(getContainerName(container)).append(": ").append(e.getMessage());
            }
        }

        refreshContainers();

        if (failureCount == 0) {
            showAlert(Alert.AlertType.INFORMATION, "Success", 
                    "All " + successCount + " containers started successfully.");
        } else if (successCount == 0) {
            showAlert(Alert.AlertType.ERROR, "Error", 
                    "Failed to start all containers:" + errors);
        } else {
            showAlert(Alert.AlertType.WARNING, "Partial Success", 
                    successCount + " containers started successfully.\n" 
                    + failureCount + " containers failed:" + errors);
        }
    }

    @FXML
    private void handleStopContainer() {
        // Check if a group is selected
        if (isGroupSelected()) {
            List<Container> containers = getSelectedContainerGroup();
            if (containers.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "No Containers", "The selected group has no containers.");
                return;
            }

            String groupName = containersTable.getSelectionModel().getSelectedItem().getValue().getName();
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirm Stop");
            confirm.setHeaderText("Stop Container Group");
            confirm.setContentText("Are you sure you want to stop all " + containers.size() 
                    + " containers in group '" + groupName + "'?");

            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                stopContainers(containers);
            }
            return;
        }

        // Handle single container
        Container selected = getSelectedContainer();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a container to stop.");
            return;
        }

        try {
            connectionManager.getDockerClient().stopContainerCmd(selected.getId()).exec();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Container stopped successfully.");
            refreshContainers();
        } catch (Exception e) {
            logger.error("Failed to stop container", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to stop container: " + e.getMessage());
        }
    }

    private void stopContainers(List<Container> containers) {
        int successCount = 0;
        int failureCount = 0;
        StringBuilder errors = new StringBuilder();

        for (Container container : containers) {
            try {
                connectionManager.getDockerClient().stopContainerCmd(container.getId()).exec();
                successCount++;
                logger.info("Stopped container: {}", getContainerName(container));
            } catch (Exception e) {
                failureCount++;
                logger.error("Failed to stop container: {}", getContainerName(container), e);
                errors.append("\n- ").append(getContainerName(container)).append(": ").append(e.getMessage());
            }
        }

        refreshContainers();

        if (failureCount == 0) {
            showAlert(Alert.AlertType.INFORMATION, "Success", 
                    "All " + successCount + " containers stopped successfully.");
        } else if (successCount == 0) {
            showAlert(Alert.AlertType.ERROR, "Error", 
                    "Failed to stop all containers:" + errors);
        } else {
            showAlert(Alert.AlertType.WARNING, "Partial Success", 
                    successCount + " containers stopped successfully.\n" 
                    + failureCount + " containers failed:" + errors);
        }
    }

    @FXML
    private void handleRestartContainer() {
        // Check if a group is selected
        if (isGroupSelected()) {
            List<Container> containers = getSelectedContainerGroup();
            if (containers.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "No Containers", "The selected group has no containers.");
                return;
            }

            String groupName = containersTable.getSelectionModel().getSelectedItem().getValue().getName();
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirm Restart");
            confirm.setHeaderText("Restart Container Group");
            confirm.setContentText("Are you sure you want to restart all " + containers.size() 
                    + " containers in group '" + groupName + "'?");

            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                restartContainers(containers);
            }
            return;
        }

        // Handle single container
        Container selected = getSelectedContainer();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a container to restart.");
            return;
        }

        try {
            connectionManager.getDockerClient().restartContainerCmd(selected.getId()).exec();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Container restarted successfully.");
            refreshContainers();
        } catch (Exception e) {
            logger.error("Failed to restart container", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to restart container: " + e.getMessage());
        }
    }

    private void restartContainers(List<Container> containers) {
        int successCount = 0;
        int failureCount = 0;
        StringBuilder errors = new StringBuilder();

        for (Container container : containers) {
            try {
                connectionManager.getDockerClient().restartContainerCmd(container.getId()).exec();
                successCount++;
                logger.info("Restarted container: {}", getContainerName(container));
            } catch (Exception e) {
                failureCount++;
                logger.error("Failed to restart container: {}", getContainerName(container), e);
                errors.append("\n- ").append(getContainerName(container)).append(": ").append(e.getMessage());
            }
        }

        refreshContainers();

        if (failureCount == 0) {
            showAlert(Alert.AlertType.INFORMATION, "Success", 
                    "All " + successCount + " containers restarted successfully.");
        } else if (successCount == 0) {
            showAlert(Alert.AlertType.ERROR, "Error", 
                    "Failed to restart all containers:" + errors);
        } else {
            showAlert(Alert.AlertType.WARNING, "Partial Success", 
                    successCount + " containers restarted successfully.\n" 
                    + failureCount + " containers failed:" + errors);
        }
    }

    @FXML
    private void handleRemoveContainer() {
        // Check if a group is selected
        if (isGroupSelected()) {
            List<Container> containers = getSelectedContainerGroup();
            if (containers.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "No Containers", "The selected group has no containers.");
                return;
            }

            String groupName = containersTable.getSelectionModel().getSelectedItem().getValue().getName();
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirm Removal");
            confirm.setHeaderText("Remove Container Group");
            confirm.setContentText("Are you sure you want to remove all " + containers.size() 
                    + " containers in group '" + groupName + "'?");

            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                removeContainers(containers);
            }
            return;
        }

        // Handle single container
        Container selected = getSelectedContainer();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a container to remove.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Removal");
        confirm.setHeaderText("Remove Container");
        confirm.setContentText("Are you sure you want to remove container " + getContainerName(selected) + "?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                connectionManager.getDockerClient().removeContainerCmd(selected.getId()).withForce(true).exec();
                showAlert(Alert.AlertType.INFORMATION, "Success", "Container removed successfully.");
                refreshContainers();
            } catch (Exception e) {
                logger.error("Failed to remove container", e);
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to remove container: " + e.getMessage());
            }
        }
    }

    private void removeContainers(List<Container> containers) {
        int successCount = 0;
        int failureCount = 0;
        StringBuilder errors = new StringBuilder();

        for (Container container : containers) {
            try {
                connectionManager.getDockerClient().removeContainerCmd(container.getId()).withForce(true).exec();
                successCount++;
                logger.info("Removed container: {}", getContainerName(container));
            } catch (Exception e) {
                failureCount++;
                logger.error("Failed to remove container: {}", getContainerName(container), e);
                errors.append("\n- ").append(getContainerName(container)).append(": ").append(e.getMessage());
            }
        }

        refreshContainers();

        if (failureCount == 0) {
            showAlert(Alert.AlertType.INFORMATION, "Success", 
                    "All " + successCount + " containers removed successfully.");
        } else if (successCount == 0) {
            showAlert(Alert.AlertType.ERROR, "Error", 
                    "Failed to remove all containers:" + errors);
        } else {
            showAlert(Alert.AlertType.WARNING, "Partial Success", 
                    successCount + " containers removed successfully.\n" 
                    + failureCount + " containers failed:" + errors);
        }
    }

    @FXML
    private void handleOpenDetails() {
        Container selected = getSelectedContainer();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a container to view details.");
            return;
        }
        openContainerDetails(selected);
    }

    private void openContainerDetails(Container container) {
        String containerId = container.getId();
        
        // Check if a tab for this container already exists
        for (javafx.scene.control.Tab tab : mainTabPane.getTabs()) {
            if (tab.getUserData() != null && tab.getUserData().equals(containerId)) {
                // Tab already exists, just select it
                mainTabPane.getSelectionModel().select(tab);
                return;
            }
        }

        // Create a new details tab
        createDetailsTab(container);
    }

    // Map to track active log stream callbacks by container ID
    private final Map<String, java.io.Closeable> activeLogStreams = new java.util.concurrent.ConcurrentHashMap<>();

    private void stopLogStream(String containerId) {
        java.io.Closeable callback = activeLogStreams.remove(containerId);
        if (callback != null) {
            try {
                callback.close();
            } catch (Exception e) {
                logger.error("Error closing log stream for container {}", containerId, e);
            }
        }
    }

    private Button createConfiguredButton(String text, String actionKey) {
        Button button = new Button(text);
        button.setUserData(actionKey);
        shortcutManager.configureButton(button, actionKey);
        return button;
    }

    private void createDetailsTab(Container container) {
        String containerId = container.getId();
        String containerName = getContainerName(container);
        
        // Create tab
        javafx.scene.control.Tab detailsTab = new javafx.scene.control.Tab("Details: " + containerName);
        detailsTab.setClosable(true);
        detailsTab.setUserData(containerId); // Store container ID for reference
        
        // Create main layout
        BorderPane layout = new BorderPane();
        
        // Create header with container info
        VBox header = new VBox(5);
        header.setStyle("-fx-background-color: #f4f4f4; -fx-padding: 10;");
        
        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(10);
        infoGrid.setVgap(5);
        
        Label nameLabel = new Label("Name:");
        nameLabel.setStyle("-fx-font-weight: bold;");
        Label nameValue = new Label(containerName);
        
        Label idLabel = new Label("ID:");
        idLabel.setStyle("-fx-font-weight: bold;");
        Label idValue = new Label(containerId.substring(0, Math.min(12, containerId.length())));
        
        Label imageLabel = new Label("Image:");
        imageLabel.setStyle("-fx-font-weight: bold;");
        Label imageValue = new Label(container.getImage());
        
        Label portsLabel = new Label("Ports:");
        portsLabel.setStyle("-fx-font-weight: bold;");
        
        javafx.scene.Node portsNode;
        boolean isRunning = container.getState() != null && container.getState().equalsIgnoreCase("running");
        
        if (isRunning && container.getPorts() != null && container.getPorts().length > 0) {
            FlowPane flowPane = new FlowPane();
            flowPane.setHgap(10);
            flowPane.setVgap(5);
            
            boolean hasPublicPorts = false;
            for (ContainerPort port : container.getPorts()) {
                if (port.getPublicPort() != null) {
                    hasPublicPorts = true;
                    String url = buildPortUrl(port);
                    Hyperlink hyperlink = new Hyperlink(formatPortDisplay(port));
                    hyperlink.setOnAction(e -> openInBrowser(url));
                    hyperlink.setStyle("-fx-text-fill: blue; -fx-underline: true;");
                    flowPane.getChildren().add(hyperlink);
                }
            }
            
            if (hasPublicPorts) {
                portsNode = flowPane;
            } else {
                portsNode = new Label(formatPorts(container));
            }
        } else {
            portsNode = new Label(formatPorts(container));
        }
        
        Label statusLabel = new Label("Status:");
        statusLabel.setStyle("-fx-font-weight: bold;");
        Label statusValue = new Label(container.getStatus());
        if (container.getState() != null && container.getState().equalsIgnoreCase("running")) {
            statusValue.setStyle("-fx-text-fill: green;");
        } else {
            statusValue.setStyle("-fx-text-fill: red;");
        }
        
        infoGrid.add(nameLabel, 0, 0);
        infoGrid.add(nameValue, 1, 0);
        infoGrid.add(idLabel, 2, 0);
        infoGrid.add(idValue, 3, 0);
        infoGrid.add(imageLabel, 0, 1);
        infoGrid.add(imageValue, 1, 1);
        infoGrid.add(portsLabel, 2, 1);
        infoGrid.add(portsNode, 3, 1);
        infoGrid.add(statusLabel, 0, 2);
        infoGrid.add(statusValue, 1, 2);
        
        header.getChildren().add(infoGrid);
        layout.setTop(header);
        
        // Create center area with logs
        javafx.scene.text.TextFlow logTextFlow = new javafx.scene.text.TextFlow();
        logTextFlow.setStyle("-fx-background-color: black; -fx-font-family: 'Courier New', monospace;");
        logTextFlow.setPadding(new javafx.geometry.Insets(5));

        javafx.scene.control.ScrollPane logScrollPane = new javafx.scene.control.ScrollPane(logTextFlow);
        logScrollPane.setFitToWidth(true);
        logScrollPane.setFitToHeight(true);
        logScrollPane.setStyle("-fx-background: black; -fx-background-color: black;");
        
        layout.setCenter(logScrollPane);
        
        // Create footer with control buttons
        HBox footer = new HBox(10);
        footer.setStyle("-fx-padding: 10;");
        
        Button startButton = createConfiguredButton("▶ _Start", "action.details.start");
        Button stopButton = createConfiguredButton("⏹ S_top", "action.details.stop");
        Button restartButton = createConfiguredButton("↻ Res_tart", "action.details.restart");
        Button attachButton = createConfiguredButton(">_ _Attach Console", "action.details.attach");
        
        // Initial button state
        setButtonState(isRunning, startButton, stopButton, restartButton);
        
        startButton.setOnAction(e -> {
            try {
                connectionManager.getDockerClient().startContainerCmd(containerId).exec();
                showAlert(Alert.AlertType.INFORMATION, "Success", "Container started successfully.");
                refreshContainers();
                
                // Refresh local state
                Container updatedContainer = connectionManager.getDockerClient()
                        .listContainersCmd().withShowAll(true).withIdFilter(Collections.singleton(containerId)).exec().get(0);
                boolean running = updatedContainer.getState().equalsIgnoreCase("running");
                
                setButtonState(running, startButton, stopButton, restartButton);
                statusValue.setText(updatedContainer.getStatus());
                if (running) {
                    statusValue.setStyle("-fx-text-fill: green;");
                } else {
                    statusValue.setStyle("-fx-text-fill: red;");
                }

                // Restart logs
                logTextFlow.getChildren().clear();
                        startLogStreaming(logTextFlow, logScrollPane, containerId, detailsTab);
            } catch (Exception ex) {
                logger.error("Failed to start container", ex);
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to start container: " + ex.getMessage());
            }
        });
        
        stopButton.setOnAction(e -> {
            try {
                connectionManager.getDockerClient().stopContainerCmd(containerId).exec();
                showAlert(Alert.AlertType.INFORMATION, "Success", "Container stopped successfully.");
                refreshContainers();
                stopLogStream(containerId); // Stop logs when container stops
                
                // Refresh local state
                Container updatedContainer = connectionManager.getDockerClient()
                        .listContainersCmd().withShowAll(true).withIdFilter(Collections.singleton(containerId)).exec().get(0);
                boolean running = updatedContainer.getState().equalsIgnoreCase("running");
                
                setButtonState(running, startButton, stopButton, restartButton);
                statusValue.setText(updatedContainer.getStatus());
                if (running) {
                    statusValue.setStyle("-fx-text-fill: green;");
                } else {
                    statusValue.setStyle("-fx-text-fill: red;");
                }
            } catch (Exception ex) {
                logger.error("Failed to stop container", ex);
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to stop container: " + ex.getMessage());
            }
        });
        
        restartButton.setOnAction(e -> {
            try {
                connectionManager.getDockerClient().restartContainerCmd(containerId).exec();
                showAlert(Alert.AlertType.INFORMATION, "Success", "Container restarted successfully.");
                refreshContainers();
                
                // Refresh local state
                Container updatedContainer = connectionManager.getDockerClient()
                        .listContainersCmd().withShowAll(true).withIdFilter(Collections.singleton(containerId)).exec().get(0);
                boolean running = updatedContainer.getState().equalsIgnoreCase("running");
                
                setButtonState(running, startButton, stopButton, restartButton);
                statusValue.setText(updatedContainer.getStatus());
                if (running) {
                    statusValue.setStyle("-fx-text-fill: green;");
                } else {
                    statusValue.setStyle("-fx-text-fill: red;");
                }
                
                // Restart logs
                logTextFlow.getChildren().clear();
                        startLogStreaming(logTextFlow, logScrollPane, containerId, detailsTab);
            } catch (Exception ex) {
                logger.error("Failed to restart container", ex);
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to restart container: " + ex.getMessage());
            }
        });
        
        attachButton.setOnAction(e -> attachToContainer(container));
        
        footer.getChildren().addAll(startButton, stopButton, restartButton, attachButton);
        layout.setBottom(footer);
        
                detailsTab.setContent(layout);
                
                // Add tab and select it
                mainTabPane.getTabs().add(detailsTab);
                mainTabPane.getSelectionModel().select(detailsTab);
                
                // Start streaming logs in follow mode
                startLogStreaming(logTextFlow, logScrollPane, containerId, detailsTab);
            }
    private void startLogStreaming(javafx.scene.text.TextFlow logTextFlow, 
                                   javafx.scene.control.ScrollPane logScrollPane, 
                                   String containerId, 
                                   javafx.scene.control.Tab logTab) {
        // Stop any existing stream for this container first
        stopLogStream(containerId);

        Thread logThread = new Thread(() -> {
            try {
                // Buffer for incoming log chunks
                StringBuilder pendingLogs = new StringBuilder();
                // Object to synchronize access to pendingLogs and updatePending flag
                Object lock = new Object();

                com.github.dockerjava.api.async.ResultCallback.Adapter<Frame> callback =
                    new com.github.dockerjava.api.async.ResultCallback.Adapter<Frame>() {
                        private boolean updatePending = false;

                        @Override
                        public void onNext(Frame frame) {
                            String logLine = new String(frame.getPayload(), StandardCharsets.UTF_8);
                            
                            synchronized (lock) {
                                pendingLogs.append(logLine);
                                if (!updatePending) {
                                    updatePending = true;
                                    Platform.runLater(this::updateUI);
                                }
                            }
                        }

                        private void updateUI() {
                            String textToAppend;
                            synchronized (lock) {
                                textToAppend = pendingLogs.toString();
                                pendingLogs.setLength(0);
                                updatePending = false;
                            }

                            if (textToAppend.isEmpty()) {
                                return;
                            }

                            // Check if we are at the bottom BEFORE adding content
                            // We are at the bottom if:
                            // 1. Content fits within the viewport (no scrollbar needed yet)
                            // 2. OR the scrollbar is near the bottom
                            double contentHeight = logTextFlow.getBoundsInLocal().getHeight();
                            double viewportHeight = logScrollPane.getViewportBounds().getHeight();
                            boolean contentFits = contentHeight <= viewportHeight;
                            boolean scrollAtBottom = logScrollPane.getVvalue() >= 0.99;
                            
                            boolean wasAtBottom = contentFits || scrollAtBottom;

                            appendAnsiText(logTextFlow, textToAppend);

                            // Auto-scroll ONLY if we were already at the bottom
                            if (wasAtBottom) {
                                // Defer scroll to ensure layout is updated
                                Platform.runLater(() -> {
                                    logScrollPane.layout(); // Force layout update
                                    logScrollPane.setVvalue(1.0);
                                });
                            }
                        }

                        @Override
                        public void onComplete() {
                            super.onComplete();
                            activeLogStreams.remove(containerId); // Clean up map
                            Platform.runLater(() -> {
                                if (logTextFlow.getChildren().isEmpty()) {
                                    javafx.scene.text.Text text = new javafx.scene.text.Text("No logs available for this container.");
                                    text.setFill(javafx.scene.paint.Color.WHITE);
                                    logTextFlow.getChildren().add(text);
                                }
                            });
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            super.onError(throwable);
                            activeLogStreams.remove(containerId); // Clean up map
                            // Only log actual errors, not expected timeouts or closed streams
                            if (!(throwable instanceof java.net.SocketTimeoutException) && 
                                !throwable.getMessage().contains("Closed")) {
                                logger.error("Error in log stream", throwable);
                                Platform.runLater(() -> {
                                    javafx.scene.text.Text text = new javafx.scene.text.Text("\n\nError in log stream: " + throwable.getMessage());
                                    text.setFill(javafx.scene.paint.Color.RED);
                                    logTextFlow.getChildren().add(text);
                                });
                            } else {
                                logger.debug("Log stream timed out or closed");
                            }
                        }
                    };

                // Register the callback so we can stop it later
                activeLogStreams.put(containerId, callback);

                connectionManager.getDockerClient().logContainerCmd(containerId)
                        .withStdOut(true)
                        .withStdErr(true)
                        .withFollowStream(true)  // Follow stream for continuous updates
                        .withTail(1000)
                        .exec(callback);

                // Store callback so we can close it when tab is closed
                logTab.setOnClosed(e -> stopLogStream(containerId));

            } catch (Exception e) {
                logger.error("Failed to stream logs", e);
                Platform.runLater(() -> {
                    javafx.scene.text.Text text = new javafx.scene.text.Text("Error streaming logs: " + e.getMessage());
                    text.setFill(javafx.scene.paint.Color.RED);
                    logTextFlow.getChildren().add(text);
                });
            }
        });

        logThread.setDaemon(true);
        logThread.start();
    }

    private void appendAnsiText(javafx.scene.text.TextFlow textFlow, String text) {
        // Basic ANSI split regex
        String[] parts = text.split("\u001B\\[");
        
        if (parts.length == 0) {
            return;
        }

        // Default color
        javafx.scene.paint.Color currentColor = javafx.scene.paint.Color.LIGHTGRAY;
        
        // Handle first part (no escape code prefix usually, or empty if string starts with one)
        if (!parts[0].isEmpty()) {
             javafx.scene.text.Text node = new javafx.scene.text.Text(parts[0]);
             node.setFill(currentColor);
             node.setFont(javafx.scene.text.Font.font("Courier New", 12));
             textFlow.getChildren().add(node);
        }

        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            int mIndex = part.indexOf('m');
            
            if (mIndex > -1) {
                String codeStr = part.substring(0, mIndex);
                String content = part.substring(mIndex + 1);

                // Parse codes
                String[] codes = codeStr.split(";");
                for (String code : codes) {
                    try {
                        if (code.isEmpty()) continue;
                        int c = Integer.parseInt(code);
                        switch (c) {
                            case 0: currentColor = javafx.scene.paint.Color.LIGHTGRAY; break; // Reset
                            case 30: currentColor = javafx.scene.paint.Color.BLACK; break;
                            case 31: currentColor = javafx.scene.paint.Color.RED; break;
                            case 32: currentColor = javafx.scene.paint.Color.GREEN; break;
                            case 33: currentColor = javafx.scene.paint.Color.YELLOW; break;
                            case 34: currentColor = javafx.scene.paint.Color.BLUE; break;
                            case 35: currentColor = javafx.scene.paint.Color.MAGENTA; break;
                            case 36: currentColor = javafx.scene.paint.Color.CYAN; break;
                            case 37: 
                            case 97: currentColor = javafx.scene.paint.Color.WHITE; break;
                            case 90: currentColor = javafx.scene.paint.Color.GRAY; break;
                            case 91: currentColor = javafx.scene.paint.Color.INDIANRED; break;
                            case 92: currentColor = javafx.scene.paint.Color.LIGHTGREEN; break;
                            case 93: currentColor = javafx.scene.paint.Color.LIGHTYELLOW; break;
                            case 94: currentColor = javafx.scene.paint.Color.LIGHTBLUE; break;
                            case 95: currentColor = javafx.scene.paint.Color.VIOLET; break;
                            case 96: currentColor = javafx.scene.paint.Color.LIGHTCYAN; break;
                            default: break; // Ignore unknown codes
                        }
                    } catch (NumberFormatException ignored) {
                        // Ignore malformed codes
                    }
                }

                if (!content.isEmpty()) {
                    javafx.scene.text.Text node = new javafx.scene.text.Text(content);
                    node.setFill(currentColor);
                    node.setFont(javafx.scene.text.Font.font("Courier New", 12));
                    textFlow.getChildren().add(node);
                }
            } else {
                // No 'm' terminator, treat whole part as text (fallback)
                javafx.scene.text.Text node = new javafx.scene.text.Text("\u001B[" + part);
                node.setFill(currentColor);
                node.setFont(javafx.scene.text.Font.font("Courier New", 12));
                textFlow.getChildren().add(node);
            }
        }
    }

    private void attachToContainer(Container container) {
        try {
            String containerId = container.getId();
            String containerName = getContainerName(container);

            // Check if container is running
            if (!container.getState().equalsIgnoreCase("running")) {
                showAlert(Alert.AlertType.WARNING, "Container Not Running",
                        "Container " + containerName + " is not running. Please start it first.");
                return;
            }

            // Get Docker host information to construct the docker command
            String dockerHost = connectionManager.getCurrentConnectionString();

            // Build docker attach command
            StringBuilder dockerCommand = new StringBuilder();

            // If using custom Docker host, set DOCKER_HOST environment variable
            if (dockerHost != null && !dockerHost.isEmpty() && !dockerHost.equals("default")) {
                dockerCommand.append("set DOCKER_HOST=").append(dockerHost).append(" && ");
            }

            dockerCommand.append("docker attach ").append(containerId);
            dockerCommand.append(" || pause");

            // Start new cmd window with docker attach
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "cmd.exe", "/c", "start",
                    "Docker Attach - " + containerName,
                    "cmd.exe", "/k",
                    dockerCommand.toString()
            );
            processBuilder.start();

            logger.info("Opened console for container: {}", containerName);

        } catch (Exception e) {
            logger.error("Failed to attach console", e);
            showAlert(Alert.AlertType.ERROR, "Error",
                    "Failed to open console: " + e.getMessage());
        }
    }

    @FXML
    private void handleAttachConsole() {
        Container selected = getSelectedContainer();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a container to attach.");
            return;
        }
        attachToContainer(selected);
    }

    @FXML
    private void handleRefreshImages() {
        refreshImages();
    }

    @FXML
    private void handlePullImage() {
        TextInputDialog dialog = new TextInputDialog("nginx:latest");
        dialog.setTitle("Pull Image");
        dialog.setHeaderText("Pull Docker Image");
        dialog.setContentText("Enter image name (e.g., nginx:latest):");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(imageName -> {
            new Thread(() -> {
                try {
                    Platform.runLater(() ->
                            showAlert(Alert.AlertType.INFORMATION, "Pulling Image",
                                    "Pulling image " + imageName
                                            + "... This may take a while."));

                    connectionManager.getDockerClient().pullImageCmd(imageName)
                            .exec(new com.github.dockerjava.api.async.ResultCallback.Adapter<>())
                            .awaitCompletion();

                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.INFORMATION, "Success", "Image pulled successfully.");
                        refreshImages();
                    });
                } catch (Exception e) {
                    logger.error("Failed to pull image", e);
                    Platform.runLater(() ->
                            showAlert(Alert.AlertType.ERROR, "Error", "Failed to pull image: " + e.getMessage()));
                }
            }).start();
        });
    }

    @FXML
    private void handleRemoveImage() {
        Image selected = getSelectedImage();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select an image to remove.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Removal");
        confirm.setHeaderText("Remove Image");
        confirm.setContentText("Are you sure you want to remove this image?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                connectionManager.getDockerClient().removeImageCmd(selected.getId()).withForce(true).exec();
                showAlert(Alert.AlertType.INFORMATION, "Success", "Image removed successfully.");
                refreshImages();
            } catch (Exception e) {
                logger.error("Failed to remove image", e);
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to remove image: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleRefreshVolumes() {
        refreshVolumes();
    }

    @FXML
    private void handleRemoveVolume() {
        InspectVolumeResponse selected = getSelectedVolume();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a volume to remove.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Removal");
        confirm.setHeaderText("Remove Volume");
        confirm.setContentText("Are you sure you want to remove volume " + selected.getName() + "?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                connectionManager.getDockerClient().removeVolumeCmd(selected.getName()).exec();
                showAlert(Alert.AlertType.INFORMATION, "Success", "Volume removed successfully.");
                refreshVolumes();
            } catch (Exception e) {
                logger.error("Failed to remove volume", e);
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to remove volume: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleRefreshNetworks() {
        refreshNetworks();
    }

    @FXML
    private void handleRemoveNetwork() {
        Network selected = networksTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a network to remove.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Removal");
        confirm.setHeaderText("Remove Network");
        confirm.setContentText("Are you sure you want to remove network "
                + selected.getName() + "?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                connectionManager.getDockerClient().removeNetworkCmd(selected.getId()).exec();
                showAlert(Alert.AlertType.INFORMATION, "Success", "Network removed successfully.");
                refreshNetworks();
            } catch (Exception e) {
                logger.error("Failed to remove network", e);
                showAlert(Alert.AlertType.ERROR, "Error",
                        "Failed to remove network: " + e.getMessage());
            }
        }
    }

    private void setButtonState(boolean isRunning, Button start, Button stop, Button restart) {
        if (isRunning) {
            start.setDisable(true);
            stop.setDisable(false);
            restart.setDisable(false);
        } else {
            start.setDisable(false);
            stop.setDisable(true);
            restart.setDisable(true);
        }
    }

    private void updateContainerActionButtons(TreeItem<ContainerViewItem> item) {
        if (item == null) {
            startContainerButton.setDisable(true);
            stopContainerButton.setDisable(true);
            restartContainerButton.setDisable(true);
            return;
        }

        if (item.getValue().isGroup()) {
            // For groups, enable all to allow batch actions
            startContainerButton.setDisable(false);
            stopContainerButton.setDisable(false);
            restartContainerButton.setDisable(false);
            return;
        }

        Container container = item.getValue().getContainer();
        if (container == null) {
            startContainerButton.setDisable(true);
            stopContainerButton.setDisable(true);
            restartContainerButton.setDisable(true);
            return;
        }

        String state = container.getState();
        boolean isRunning = state != null && state.equalsIgnoreCase("running");

        setButtonState(isRunning, startContainerButton, stopContainerButton, restartContainerButton);
    }

    private void refreshContainers() {
        if (!checkConnection()) {
            return;
        }

        // Capture current selection
        TreeItem<ContainerViewItem> currentSelection = containersTable.getSelectionModel().getSelectedItem();
        String selectedId = null;
        if (currentSelection != null && !currentSelection.getValue().isGroup()) {
            selectedId = currentSelection.getValue().getContainer().getId();
        }

        try {
            List<Container> containers = connectionManager.getDockerClient()
                    .listContainersCmd()
                    .withShowAll(true)
                    .exec();

            // Group containers
            Map<String, List<Container>> grouped = new TreeMap<>();
            List<Container> ungrouped = new ArrayList<>();

            for (Container c : containers) {
                String project = null;
                if (c.getLabels() != null) {
                    project = c.getLabels().get("com.docker.compose.project");
                }
                
                if (project != null && !project.isEmpty()) {
                    grouped.computeIfAbsent(project, k -> new ArrayList<>()).add(c);
                } else {
                    ungrouped.add(c);
                }
            }
            if (!ungrouped.isEmpty()) {
                grouped.put(UNGROUPED_LABEL, ungrouped);
            }

            TreeItem<ContainerViewItem> root = new TreeItem<>(new ContainerViewItem("Root"));
            root.setExpanded(true);

            for (Map.Entry<String, List<Container>> entry : grouped.entrySet()) {
                TreeItem<ContainerViewItem> groupItem = new TreeItem<>(new ContainerViewItem(entry.getKey()));
                groupItem.setExpanded(true);
                for (Container c : entry.getValue()) {
                    groupItem.getChildren().add(new TreeItem<>(new ContainerViewItem(c, getContainerName(c))));
                }
                root.getChildren().add(groupItem);
            }

            containersTable.setRoot(root);
            
            // Restore selection or default to first
            boolean restored = false;
            if (selectedId != null) {
                for (TreeItem<ContainerViewItem> group : root.getChildren()) {
                    for (TreeItem<ContainerViewItem> item : group.getChildren()) {
                        if (!item.getValue().isGroup() && item.getValue().getContainer().getId().equals(selectedId)) {
                            containersTable.getSelectionModel().select(item);
                            restored = true;
                            break;
                        }
                    }
                    if (restored) {
                        break;
                    }
                }
            }

            if (!restored && !root.getChildren().isEmpty()) {
                containersTable.getSelectionModel().select(0);
                containersTable.requestFocus();
            }
        } catch (RuntimeException e) {
            logger.error("Failed to refresh containers", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to refresh containers: " + e.getMessage());
        }
    }

    private void refreshImages() {
        if (!checkConnection()) {
            return;
        }

        try {
            List<Image> images = connectionManager.getDockerClient()
                    .listImagesCmd()
                    .exec();

            // Group images
            Map<String, List<Image>> grouped = new TreeMap<>();
            List<Image> ungrouped = new ArrayList<>();

            for (Image img : images) {
                String groupName = null;
                // Try compose project label first
                if (img.getLabels() != null) {
                    groupName = img.getLabels().get("com.docker.compose.project");
                }
                // Fallback to repository name
                if (groupName == null && img.getRepoTags() != null && img.getRepoTags().length > 0) {
                    String[] parts = img.getRepoTags()[0].split(":");
                    if (parts.length > 0 && !parts[0].isEmpty()) {
                        groupName = parts[0];
                    }
                }

                if (groupName != null) {
                    grouped.computeIfAbsent(groupName, k -> new ArrayList<>()).add(img);
                } else {
                    ungrouped.add(img);
                }
            }
            if (!ungrouped.isEmpty()) {
                grouped.put(UNGROUPED_LABEL, ungrouped);
            }

            TreeItem<ImageViewItem> root = new TreeItem<>(new ImageViewItem("Root"));
            root.setExpanded(true);

            for (Map.Entry<String, List<Image>> entry : grouped.entrySet()) {
                TreeItem<ImageViewItem> groupItem = new TreeItem<>(new ImageViewItem(entry.getKey()));
                groupItem.setExpanded(true);
                for (Image img : entry.getValue()) {
                    String tagName = "<none>";
                    if (img.getRepoTags() != null && img.getRepoTags().length > 0) {
                        // Use full repo:tag or just tag? Table shows repo and tag separately.
                        // Name in tree can be repo:tag or just tag if grouped by repo.
                        // Let's use repo:tag for clarity or just tag if parent is repo.
                        // Since we grouped by repo, let's just show tag or full name if it was grouped by project.
                        tagName = img.getRepoTags()[0];
                    }
                    groupItem.getChildren().add(new TreeItem<>(new ImageViewItem(img, tagName)));
                }
                root.getChildren().add(groupItem);
            }

            imagesTable.setRoot(root);
        } catch (RuntimeException e) {
            logger.error("Failed to refresh images", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to refresh images: " + e.getMessage());
        }
    }

    private void refreshVolumes() {
        if (!checkConnection()) {
            return;
        }

        try {
            List<InspectVolumeResponse> volumes = connectionManager.getDockerClient()
                    .listVolumesCmd()
                    .exec()
                    .getVolumes();

            // Group volumes
            Map<String, List<InspectVolumeResponse>> grouped = new TreeMap<>();
            List<InspectVolumeResponse> ungrouped = new ArrayList<>();

            for (InspectVolumeResponse vol : volumes) {
                String project = null;
                if (vol.getLabels() != null) {
                    project = vol.getLabels().get("com.docker.compose.project");
                }

                if (project != null && !project.isEmpty()) {
                    grouped.computeIfAbsent(project, k -> new ArrayList<>()).add(vol);
                } else {
                    ungrouped.add(vol);
                }
            }
            if (!ungrouped.isEmpty()) {
                grouped.put(UNGROUPED_LABEL, ungrouped);
            }

            TreeItem<VolumeViewItem> root = new TreeItem<>(new VolumeViewItem("Root"));
            root.setExpanded(true);

            for (Map.Entry<String, List<InspectVolumeResponse>> entry : grouped.entrySet()) {
                TreeItem<VolumeViewItem> groupItem = new TreeItem<>(new VolumeViewItem(entry.getKey()));
                groupItem.setExpanded(true);
                for (InspectVolumeResponse vol : entry.getValue()) {
                    groupItem.getChildren().add(new TreeItem<>(new VolumeViewItem(vol, vol.getName())));
                }
                root.getChildren().add(groupItem);
            }

            volumesTable.setRoot(root);
        } catch (RuntimeException e) {
            logger.error("Failed to refresh volumes", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to refresh volumes: " + e.getMessage());
        }
    }

    private void refreshNetworks() {
        if (!checkConnection()) {
            return;
        }

        try {
            List<Network> networks = connectionManager.getDockerClient()
                    .listNetworksCmd()
                    .exec();

            ObservableList<Network> networkList = FXCollections.observableArrayList(networks);
            networksTable.setItems(networkList);
        } catch (Exception e) {
            logger.error("Failed to refresh networks", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to refresh networks: " + e.getMessage());
        }
    }

    private void refreshAll() {
        refreshContainers();
        refreshImages();
        refreshVolumes();
        refreshNetworks();
    }

    private void clearAllTables() {
        if (containersTable != null) {
            containersTable.setRoot(null);
        }
        if (imagesTable != null) {
            imagesTable.setRoot(null);
        }
        if (volumesTable != null) {
            volumesTable.setRoot(null);
        }
        if (networksTable != null) {
            networksTable.getItems().clear();
        }
    }

    private void updateConnectionStatus() {
        if (connectionManager.isConnected()) {
            connectionStatusLabel.setText("Connected: " + connectionManager.getCurrentConnectionString());
            connectionStatusLabel.setStyle("-fx-text-fill: green;");
            connectAutoButton.setDisable(true);
            disconnectButton.setDisable(false);
            mainTabPane.setDisable(false);
        } else {
            connectionStatusLabel.setText("Not Connected");
            connectionStatusLabel.setStyle("-fx-text-fill: red;");
            connectAutoButton.setDisable(false);
            disconnectButton.setDisable(true);
            mainTabPane.setDisable(true);
        }
    }

    private boolean checkConnection() {
        if (!connectionManager.isConnected()) {
            showAlert(Alert.AlertType.WARNING, "Not Connected", "Please connect to Docker first.");
            return false;
        }
        return true;
    }

    private String getContainerName(Container container) {
        if (container.getNames() != null && container.getNames().length > 0) {
            String name = container.getNames()[0];
            return name.startsWith("/") ? name.substring(1) : name;
        }
        return container.getId().substring(0, Math.min(12, container.getId().length()));
    }

    private Container getSelectedContainer() {
        TreeItem<ContainerViewItem> selected = containersTable.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getValue().isGroup()) {
            return null;
        }
        return selected.getValue().getContainer();
    }

    private List<Container> getSelectedContainerGroup() {
        TreeItem<ContainerViewItem> selected = containersTable.getSelectionModel().getSelectedItem();
        if (selected == null || !selected.getValue().isGroup()) {
            return Collections.emptyList();
        }
        List<Container> containers = new ArrayList<>();
        for (TreeItem<ContainerViewItem> child : selected.getChildren()) {
            Container container = child.getValue().getContainer();
            if (container != null) {
                containers.add(container);
            }
        }
        return containers;
    }

    private boolean isGroupSelected() {
        TreeItem<ContainerViewItem> selected = containersTable.getSelectionModel().getSelectedItem();
        return selected != null && selected.getValue().isGroup();
    }

    private Image getSelectedImage() {
        TreeItem<ImageViewItem> selected = imagesTable.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getValue().isGroup()) {
            return null;
        }
        return selected.getValue().getImage();
    }

    private InspectVolumeResponse getSelectedVolume() {
        TreeItem<VolumeViewItem> selected = volumesTable.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getValue().isGroup()) {
            return null;
        }
        return selected.getValue().getVolume();
    }

    private String formatSize(Long size) {
        if (size == null) {
            return "0 B";
        }

        if (size < 1024) {
            return size + " B";
        }
        if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        }
        if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024));
        }
        return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void autoConnectOnStartup() {
        // Try auto-discover connection in background
        new Thread(() -> {
            logger.info("Attempting auto-connect on startup...");
            
            if (connectionManager.connectAutoDiscover()) {
                Platform.runLater(() -> {
                    updateConnectionStatus();
                    refreshAll();
                    logger.info("Auto-connected to Docker successfully");
                });
            } else {
                Platform.runLater(() -> {
                    updateConnectionStatus();
                    showAlert(Alert.AlertType.ERROR, "Docker Connection Failed",
                            "Could not automatically connect to Docker in WSL.\n\n" 
                            + "Please ensure:\n" 
                            + "- WSL is running\n" 
                            + "- Docker is installed and running in WSL\n" 
                            + "- Docker daemon is listening on port 2375\n\n" 
                            + "You can use the 'Connect / Reconnect' button to retry.");
                    logger.warn("Failed to auto-connect to Docker on startup");
                });
            }
        }).start();
    }

    /**
     * Format container ports for display.
     * Returns a string representation of all exposed ports.
     */
    private String formatPorts(Container container) {
        if (container.getPorts() == null || container.getPorts().length == 0) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        for (ContainerPort port : container.getPorts()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            
            if (port.getPublicPort() != null) {
                String ip = port.getIp();
                if (ip == null || ip.isEmpty()) {
                    ip = "0.0.0.0";
                }
                sb.append(ip).append(":").append(port.getPublicPort());
                sb.append("->").append(port.getPrivatePort());
            } else {
                sb.append(port.getPrivatePort());
            }
            
            if (port.getType() != null) {
                sb.append("/").append(port.getType());
            }
        }
        
        return sb.toString();
    }

    /**
     * Format a single port for display in a hyperlink.
     */
    private String formatPortDisplay(ContainerPort port) {
        StringBuilder sb = new StringBuilder();
        
        if (port.getPublicPort() != null) {
            String ip = port.getIp();
            if (ip == null || ip.isEmpty()) {
                ip = "0.0.0.0";
            }
            sb.append(ip).append(":").append(port.getPublicPort());
            sb.append("->").append(port.getPrivatePort());
        } else {
            sb.append(port.getPrivatePort());
        }
        
        if (port.getType() != null) {
            sb.append("/").append(port.getType());
        }
        
        return sb.toString();
    }

    /**
     * Build a URL from a container port.
     */
    private String buildPortUrl(ContainerPort port) {
        Integer publicPort = port.getPublicPort();
        if (publicPort == null) {
            return null;
        }
        
        String ip = port.getIp();
        if (ip == null || ip.isEmpty() || ip.equals("0.0.0.0")) {
            ip = "localhost";
        }
        
        // Use http for common ports, or just construct with http by default
        String protocol = "http";
        if (publicPort == 443 || publicPort == 8443) {
            protocol = "https";
        }
        
        return protocol + "://" + ip + ":" + publicPort;
    }

    /**
     * Open a URL in the default system browser.
     */
    private void openInBrowser(String url) {
        if (url == null || url.isEmpty()) {
            return;
        }
        
        try {
            // Use Java Desktop API to open URL in default browser
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                if (desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
                    desktop.browse(new java.net.URI(url));
                    logger.info("Opened URL in browser: {}", url);
                }
            }
        } catch (java.io.IOException | java.net.URISyntaxException e) {
            logger.error("Failed to open URL in browser: {}", url, e);
            showAlert(Alert.AlertType.ERROR, "Error", 
                    "Failed to open URL in browser: " + e.getMessage());
        }
    }
}
