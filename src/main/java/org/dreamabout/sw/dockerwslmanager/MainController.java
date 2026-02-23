package org.dreamabout.sw.dockerwslmanager;

import com.github.dockerjava.api.command.InspectVolumeResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerPort;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.Network;
import com.github.dockerjava.api.model.PruneResponse;
import com.github.dockerjava.api.model.PruneType;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.dreamabout.sw.dockerwslmanager.logic.FormatUtils;
import org.dreamabout.sw.dockerwslmanager.logic.TextFlowSelectionHandler;
import org.dreamabout.sw.dockerwslmanager.logic.VolumeLogic;
import org.dreamabout.sw.dockerwslmanager.logic.VolumePathResolver;
import org.dreamabout.sw.dockerwslmanager.logic.ConfigLogic;
import org.dreamabout.sw.dockerwslmanager.model.ContainerViewItem;
import org.dreamabout.sw.dockerwslmanager.model.ImageViewItem;
import org.dreamabout.sw.dockerwslmanager.model.VolumeViewItem;
import org.dreamabout.sw.dockerwslmanager.model.ContainerStats;
import org.dreamabout.sw.dockerwslmanager.service.ContainerStatsService;
import org.dreamabout.sw.dockerwslmanager.service.VolumeUsageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class MainController {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);
    private static final String UNGROUPED_LABEL = VolumeLogic.UNGROUPED_LABEL;
    
    private final VolumeLogic volumeLogic = new VolumeLogic();
    private final VolumeUsageService volumeUsageService = new VolumeUsageService();
    private final ConfigLogic configLogic = new ConfigLogic();
    private VolumePathResolver volumePathResolver;
    private ContainerStatsService containerStatsService;

    private final ShortcutManager shortcutManager = new ShortcutManager();
    private final SettingsManager settingsManager = new SettingsManager();
    private DockerConnectionManager connectionManager;

    // Map to track active stats labels by container ID for updates
    private final Map<String, ContainerStatsLabels> activeStatsLabels = new java.util.concurrent.ConcurrentHashMap<>();

    private static class ContainerStatsLabels {
        final Label cpuValue;
        final Label ramValue;
        final Label netValue;
        final Label diskValue;

        ContainerStatsLabels(Label cpuValue, Label ramValue, Label netValue, Label diskValue) {
            this.cpuValue = cpuValue;
            this.ramValue = ramValue;
            this.netValue = netValue;
            this.diskValue = diskValue;
        }
    }

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
    private TreeTableColumn<VolumeViewItem, javafx.collections.ObservableList<String>> volumeContainersColumn;
    @FXML
    private TreeTableColumn<VolumeViewItem, String> volumeSizeColumn;
    @FXML
    private TreeTableColumn<VolumeViewItem, String> volumeDriverColumn;
    @FXML
    private TreeTableColumn<VolumeViewItem, String> volumeMountpointColumn;
    @FXML
    private Button refreshVolumesButton;
    @FXML
    private Button calculateVolumeSizesButton;
    @FXML
    private Button openVolumeButton;
    @FXML
    private Button removeVolumeButton;
    @FXML
    private Button pruneVolumesButton;

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
        volumePathResolver = new VolumePathResolver(settingsManager.getWslDistro());

        updateConnectionStatus();
// ...

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
                return new SimpleStringProperty(FormatUtils.formatSize(item.getImage().getSize()));
            });
        }

        // Initialize volumes table
        if (volumeNameColumn != null) {
            volumeNameColumn.setCellValueFactory(data ->
                    new SimpleStringProperty(data.getValue().getValue().getName()));

            volumeNameColumn.setCellFactory(column -> new TreeTableCell<VolumeViewItem, String>() {
                @Override
                protected void updateItem(String name, boolean empty) {
                    super.updateItem(name, empty);
                    if (empty || name == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(name);
                        VolumeViewItem item = getTreeTableRow().getItem();
                        if (item != null && !item.isGroup()) {
                            if (item.isInUseByRunningContainer()) {
                                setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                            } else if (item.isUnused()) {
                                setStyle("-fx-text-fill: gray;");
                            } else {
                                setStyle("");
                            }
                        } else {
                            setStyle("");
                        }
                    }
                }
            });

            volumeContainersColumn.setCellValueFactory(data ->
                    javafx.beans.binding.Bindings.createObjectBinding(() -> 
                            data.getValue().getValue().getContainerNames()));

            volumeContainersColumn.setCellFactory(col -> new javafx.scene.control.TreeTableCell<>() {
                private final FlowPane flowPane = new FlowPane();

                {
                    flowPane.setHgap(5);
                    flowPane.setVgap(2);
                }

                @Override
                protected void updateItem(javafx.collections.ObservableList<String> containerNames, boolean empty) {
                    super.updateItem(containerNames, empty);
                    if (empty || containerNames == null || containerNames.isEmpty()) {
                        setGraphic(null);
                    } else {
                        flowPane.getChildren().clear();
                        for (int i = 0; i < containerNames.size(); i++) {
                            String name = containerNames.get(i);
                            Hyperlink link = new Hyperlink(name);
                            link.setOnAction(e -> navigateToContainer(name));
                            flowPane.getChildren().add(link);
                            if (i < containerNames.size() - 1) {
                                flowPane.getChildren().add(new Label(","));
                            }
                        }
                        setGraphic(flowPane);
                    }
                }
            });

            volumeContainersColumn.setComparator((list1, list2) -> {
                if (list1 == null && list2 == null) {
                    return 0;
                }
                if (list1 == null) {
                    return -1;
                }
                if (list2 == null) {
                    return 1;
                }
                
                int sizeComp = Integer.compare(list1.size(), list2.size());
                if (sizeComp != 0) {
                    return sizeComp;
                }
                
                if (list1.isEmpty()) {
                    return 0;
                }
                return list1.get(0).compareTo(list2.get(0));
            });

            volumeSizeColumn.setCellValueFactory(data ->
                    data.getValue().getValue().sizeStringProperty());

            volumeSizeColumn.setComparator((s1, s2) -> {
                long b1 = VolumeUsageService.parseDockerSize(s1);
                long b2 = VolumeUsageService.parseDockerSize(s2);
                return Long.compare(b1, b2);
            });
            
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

            volumesTable.setOnKeyPressed(event -> {
                if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                    TreeItem<VolumeViewItem> selectedItem = volumesTable.getSelectionModel().getSelectedItem();
                    if (selectedItem != null && !selectedItem.getValue().isGroup()) {
                        handleOpenVolumeAction();
                        event.consume();
                    }
                }
            });

            volumesTable.setRowFactory(tv -> new TreeTableRow<VolumeViewItem>() {
                {
                    setOnMouseClicked(event -> {
                        if (event.getClickCount() == 2 && !isEmpty()) {
                            VolumeViewItem item = getItem();
                            if (item != null && !item.isGroup()) {
                                handleOpenVolumeAction();
                            }
                        }
                    });
                }

                @Override
                protected void updateItem(VolumeViewItem item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setStyle("");
                    } else if (!item.isGroup() && item.isInUseByRunningContainer()) {
                        setStyle("-fx-text-fill: green;");
                    } else if (!item.isGroup() && item.isUnused()) {
                        setStyle("-fx-text-fill: gray; -fx-opacity: 0.7;");
                    } else {
                        setStyle("");
                    }
                }
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
        shortcutManager.configureButton(openVolumeButton, "action.volume.open");
        shortcutManager.configureButton(removeVolumeButton, "action.volume.remove");
        shortcutManager.configureButton(pruneVolumesButton, "action.volume.prune");
        
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
        javafx.scene.control.Dialog<javafx.util.Pair<String, String>> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("General Settings");
        dialog.setHeaderText("Configure Application Settings");

        javafx.scene.control.ButtonType saveButtonType = new javafx.scene.control.ButtonType("Save", 
                javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, javafx.scene.control.ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        TextField intervalField = new TextField(String.valueOf(settingsManager.getAutoRefreshInterval()));
        TextField statsIntervalField = new TextField(String.valueOf(settingsManager.getStatsRefreshInterval()));
        TextField distroField = new TextField(settingsManager.getWslDistro());

        grid.add(new Label("Auto-refresh Interval (seconds):"), 0, 0);
        grid.add(intervalField, 1, 0);
        grid.add(new Label("Stats Refresh Interval (seconds):"), 0, 1);
        grid.add(statsIntervalField, 1, 1);
        grid.add(new Label("WSL Distro (for volumes):"), 0, 2);
        grid.add(distroField, 1, 2);

        dialog.getDialogPane().setContent(grid);

        Platform.runLater(intervalField::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return new javafx.util.Pair<>(intervalField.getText() + "|" + statsIntervalField.getText(), 
                        distroField.getText());
            }
            return null;
        });

        Optional<javafx.util.Pair<String, String>> result = dialog.showAndWait();
        result.ifPresent(settings -> {
            try {
                String[] intervals = settings.getKey().split("\\|");
                int autoRefreshSecs = Integer.parseInt(intervals[0]);
                int statsRefreshSecs = Integer.parseInt(intervals[1]);
                
                if (autoRefreshSecs < 1) autoRefreshSecs = 1;
                if (statsRefreshSecs < 1) statsRefreshSecs = 1;

                settingsManager.setAutoRefreshInterval(autoRefreshSecs);
                settingsManager.setStatsRefreshInterval(statsRefreshSecs);
                settingsManager.setWslDistro(settings.getValue());
                settingsManager.saveSettings();
                volumePathResolver = new VolumePathResolver(settings.getValue());
                setupAutoRefreshTimeline();
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please enter valid numbers for intervals.");
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
        containerStatsService = null;
        activeStatsLabels.clear();
        activeStatsStreams.clear();
        updateConnectionStatus();
        clearAllTables();
    }

    private void stopStatsStream(String containerId) {
        java.io.Closeable callback = activeStatsStreams.remove(containerId);
        if (callback != null) {
            try {
                callback.close();
            } catch (Exception e) {
                logger.error("Error closing stats stream for container {}", containerId, e);
            }
        }
        activeStatsLabels.remove(containerId);
    }

    private void startStatsStreaming(String containerId, Label cpuValue, Label ramValue, 
                                     Label netValue, Label diskValue) {
        stopStatsStream(containerId);

        ContainerStatsService service = getContainerStatsService();
        if (service == null) {
            return;
        }

        activeStatsLabels.put(containerId, new ContainerStatsLabels(cpuValue, ramValue, netValue, diskValue));

        java.io.Closeable stream = service.fetchStats(containerId, stats -> {
            Platform.runLater(() -> {
                ContainerStatsLabels labels = activeStatsLabels.get(containerId);
                if (labels != null) {
                    labels.cpuValue.setText(String.format("%.2f%%", stats.getCpuPercentage()));
                    labels.ramValue.setText(FormatUtils.formatSize(stats.getMemoryUsage()) + " / " 
                            + FormatUtils.formatSize(stats.getMemoryLimit()));
                    labels.netValue.setText(FormatUtils.formatSize(stats.getNetworkReadBytes()) + " / " 
                            + FormatUtils.formatSize(stats.getNetworkWriteBytes()));
                    labels.diskValue.setText(FormatUtils.formatSize(stats.getDiskReadBytes()) + " / " 
                            + FormatUtils.formatSize(stats.getDiskWriteBytes()));
                }
            });
        });

        activeStatsStreams.put(containerId, stream);
    }

    private ContainerStatsService getContainerStatsService() {
        if (containerStatsService == null && connectionManager.isConnected()) {
            containerStatsService = new ContainerStatsService(connectionManager.getDockerClient());
        }
        return containerStatsService;
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
    // Map to track active stats stream callbacks by container ID
    private final Map<String, java.io.Closeable> activeStatsStreams = new java.util.concurrent.ConcurrentHashMap<>();

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

        // Resource Consumption Section
        header.getChildren().add(new Separator());
        Label statsTitle = new Label("Resource Consumption");
        statsTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        header.getChildren().add(statsTitle);

        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(10);
        statsGrid.setVgap(5);

        Label cpuLabel = new Label("CPU:");
        cpuLabel.setStyle("-fx-font-weight: bold;");
        Label cpuValue = new Label("---");
        
        Label ramLabel = new Label("RAM:");
        ramLabel.setStyle("-fx-font-weight: bold;");
        Label ramValue = new Label("---");

        Label netLabel = new Label("Net I/O:");
        netLabel.setStyle("-fx-font-weight: bold;");
        Label netValue = new Label("---");

        Label diskLabel = new Label("Disk I/O:");
        diskLabel.setStyle("-fx-font-weight: bold;");
        Label diskValue = new Label("---");

        statsGrid.add(cpuLabel, 0, 0);
        statsGrid.add(cpuValue, 1, 0);
        statsGrid.add(ramLabel, 2, 0);
        statsGrid.add(ramValue, 3, 0);
        statsGrid.add(netLabel, 0, 1);
        statsGrid.add(netValue, 1, 1);
        statsGrid.add(diskLabel, 2, 1);
        statsGrid.add(diskValue, 3, 1);

        header.getChildren().add(statsGrid);
        
        layout.setTop(header);
        
        // Create center area with logs
        javafx.scene.text.TextFlow logTextFlow = new javafx.scene.text.TextFlow();
        logTextFlow.setStyle("-fx-background-color: black; -fx-font-family: 'Courier New', monospace;");
        logTextFlow.setPadding(new javafx.geometry.Insets(5));

        javafx.scene.control.ScrollPane logScrollPane = new javafx.scene.control.ScrollPane(logTextFlow);
        logScrollPane.setFitToWidth(true);
        logScrollPane.setFitToHeight(false);
        logScrollPane.setStyle("-fx-background: black; -fx-background-color: black;");
        
        layout.setCenter(logScrollPane);
        
        // Create selection handler
        TextFlowSelectionHandler selectionHandler = new TextFlowSelectionHandler(logTextFlow);
        logTextFlow.setUserData(selectionHandler); // Store for use in log streaming logic

        // Add copy and select all shortcuts
        layout.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.isControlDown() && !event.isAltDown()) {
                if (event.getCode() == KeyCode.C) {
                    selectionHandler.copyToClipboard();
                    event.consume();
                } else if (event.getCode() == KeyCode.A) {
                    selectionHandler.selectAll();
                    event.consume();
                }
            }
        });
        
        // Create footer with control buttons
        HBox footer = new HBox(10);
        footer.setStyle("-fx-padding: 10;");
        
        Button startButton = createConfiguredButton("▶ _Start", "action.details.start");
        Button stopButton = createConfiguredButton("⏹ S_top", "action.details.stop");
        Button restartButton = createConfiguredButton("↻ Res_tart", "action.details.restart");
        Button attachButton = createConfiguredButton(">_ _Attach Console", "action.details.attach");
        Button configButton = createConfiguredButton("⚙ _Config", "action.details.config");
        Button openVolumesButton = createConfiguredButton("📂 Open _Volumes", "action.details.volumes");
        Button showProcessesButton = new Button("🔍 Show Processes");
        Button copyAllButton = new Button("📋 Copy All");
        copyAllButton.setOnAction(e -> selectionHandler.copyAllToClipboard());
        
        openVolumesButton.setOnAction(e -> handleOpenContainerVolumes(container));
        configButton.setOnAction(e -> openContainerConfig(containerId, containerName));
        showProcessesButton.setOnAction(e -> handleShowProcesses(containerId, containerName));
        
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
        
        footer.getChildren().addAll(startButton, stopButton, restartButton, attachButton, configButton, 
                openVolumesButton, showProcessesButton, copyAllButton);
        layout.setBottom(footer);
        
        detailsTab.setContent(layout);
        
        // Add tab and select it
        mainTabPane.getTabs().add(detailsTab);
        mainTabPane.getSelectionModel().select(detailsTab);
        
        // Set cleanup
        detailsTab.setOnClosed(e -> {
            stopLogStream(containerId);
            stopStatsStream(containerId);
        });

        // Start streaming logs in follow mode
        startLogStreaming(logTextFlow, logScrollPane, containerId, detailsTab);
        // Start streaming stats
        startStatsStreaming(containerId, cpuValue, ramValue, netValue, diskValue);
    }

    private void handleOpenContainerVolumes(Container container) {
        if (container.getMounts() == null || container.getMounts().isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "No Volumes", "This container has no configured volumes or mounts.");
            return;
        }

        for (com.github.dockerjava.api.model.ContainerMount mount : container.getMounts()) {
            String source = mount.getSource();
            String name = mount.getName();

            if (name != null && !name.isEmpty()) {
                // It's a named volume
                openVolumeInExplorer(name);
            } else if (source != null && !source.isEmpty()) {
                // It's a bind mount
                CompletableFuture.supplyAsync(() -> volumePathResolver.resolveBindMountPath(source))
                        .thenAccept(optPath -> optPath.ifPresent(this::openPathInExplorer));
            }
        }
    }

    private void openContainerConfig(String containerId, String containerName) {
        String tabId = "config-" + containerId;
        
        // Check if tab already exists
        for (javafx.scene.control.Tab tab : mainTabPane.getTabs()) {
            if (tabId.equals(tab.getUserData())) {
                mainTabPane.getSelectionModel().select(tab);
                return;
            }
        }

        javafx.scene.control.Tab configTab = new javafx.scene.control.Tab("Config: " + containerName);
        configTab.setClosable(true);
        configTab.setUserData(tabId);

        VBox layout = new VBox(10);
        layout.setPadding(new javafx.geometry.Insets(10));

        HBox toolbar = new HBox(10);
        toolbar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        TextField searchField = new TextField();
        searchField.setPromptText("Search...");
        searchField.setPrefWidth(300);

        Button copyButton = new Button("📋 Copy Config");
        
        toolbar.getChildren().addAll(new Label("Filter:"), searchField, copyButton);

        TextArea configTextArea = new TextArea();
        configTextArea.setEditable(false);
        configTextArea.setStyle("-fx-font-family: 'Courier New', monospace;");
        VBox.setVgrow(configTextArea, javafx.scene.layout.Priority.ALWAYS);

        layout.getChildren().addAll(toolbar, configTextArea);
        configTab.setContent(layout);

        // Fetch and display config
        CompletableFuture.supplyAsync(() -> {
            var response = configLogic.inspectContainer(connectionManager.getDockerClient(), containerId);
            return configLogic.formatAsPrettyJson(response);
        }).thenAccept(json -> Platform.runLater(() -> configTextArea.setText(json)));

        // Copy logic
        copyButton.setOnAction(e -> {
            javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(configTextArea.getText());
            clipboard.setContent(content);
        });

        // Search logic
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isEmpty()) {
                configTextArea.deselect();
                return;
            }
            String text = configTextArea.getText();
            int index = text.toLowerCase(java.util.Locale.ROOT).indexOf(newVal.toLowerCase(java.util.Locale.ROOT));
            if (index >= 0) {
                configTextArea.selectRange(index, index + newVal.length());
                // TextArea doesn't have built-in "scroll to selection" that works reliably with programmatic selection
                // but selectRange usually does it if it gains focus or already has it.
            }
        });

        mainTabPane.getTabs().add(configTab);
        mainTabPane.getSelectionModel().select(configTab);
    }

    private void handleShowProcesses(String containerId, String containerName) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/process_list.fxml"));
            javafx.scene.Parent root = loader.load();
            
            ProcessListController controller = loader.getController();
            controller.setContainerInfo(containerId, containerName, connectionManager);
            
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Container Processes: " + containerName);
            stage.setScene(new javafx.scene.Scene(root));
            stage.getIcons().add(new javafx.scene.image.Image(getClass().getResourceAsStream("/app_icon.png")));
            stage.show();
        } catch (Exception e) {
            logger.error("Failed to open process list window", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to open process list window: " + e.getMessage());
        }
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

                            // Auto-scroll ONLY if we were already at the bottom AND not currently selecting text
                            boolean isSelecting = false;
                            if (logTextFlow.getUserData() instanceof TextFlowSelectionHandler handler) {
                                isSelecting = handler.isSelecting();
                            }

                            if (wasAtBottom && !isSelecting) {
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

            // Fetch the latest state to avoid stale data (especially in Details tabs)
            List<Container> containers = connectionManager.getDockerClient()
                    .listContainersCmd()
                    .withShowAll(true)
                    .withIdFilter(Collections.singleton(containerId))
                    .exec();

            if (containers.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Container Not Found",
                        "Container " + containerName + " no longer exists.");
                return;
            }

            Container latestContainer = containers.get(0);

            // Check if container is running
            if (latestContainer.getState() == null || !latestContainer.getState().equalsIgnoreCase("running")) {
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

            dockerCommand.append("docker exec -it ").append(containerId).append(" sh");
            dockerCommand.append(" || pause");

            // Start new cmd window with docker exec
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "cmd.exe", "/c", "start",
                    "Docker Console - " + containerName,
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
    private void handleCalculateVolumeSizes() {
        if (!checkConnection()) {
            return;
        }

        calculateVolumeSizesButton.setDisable(true);
        new Thread(() -> {
            try {
                Map<String, Long> sizes = volumeUsageService.fetchVolumeSizes();
                Platform.runLater(() -> {
                    updateVolumeSizes(sizes);
                    calculateVolumeSizesButton.setDisable(false);
                });
            } catch (Exception e) {
                logger.error("Failed to calculate volume sizes", e);
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to calculate volume sizes: " + e.getMessage());
                    calculateVolumeSizesButton.setDisable(false);
                });
            }
        }).start();
    }

    private void updateVolumeSizes(Map<String, Long> sizes) {
        TreeItem<VolumeViewItem> root = volumesTable.getRoot();
        if (root == null) {
            logger.warn("Volumes table root is null, cannot update sizes");
            return;
        }

        int updatedCount = 0;
        for (TreeItem<VolumeViewItem> group : root.getChildren()) {
            long groupTotal = 0;
            for (TreeItem<VolumeViewItem> item : group.getChildren()) {
                VolumeViewItem vol = item.getValue();
                if (!vol.isGroup()) {
                    Long size = sizes.get(vol.getName());
                    if (size != null) {
                        vol.setSizeBytes(size);
                        groupTotal += size;
                        updatedCount++;
                    }
                }
            }
            // Update group size as well
            group.getValue().setSizeBytes(groupTotal);
        }
        logger.info("Updated sizes for {} volumes in UI", updatedCount);
    }

    @FXML
    private void handleOpenVolumeAction() {
        InspectVolumeResponse selected = getSelectedVolume();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a volume to open.");
            return;
        }

        openVolumeInExplorer(selected.getName());
    }

    private void openVolumeInExplorer(String volumeName) {
        CompletableFuture.supplyAsync(() -> volumePathResolver.resolveNamedVolumePath(volumeName))
                .thenAccept(optPath -> optPath.ifPresent(this::openPathInExplorer));
    }

    private void openPathInExplorer(String path) {
        if (path == null) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                File file = new File(path);
                if (file.exists()) {
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().open(file);
                    } else {
                        // Fallback for some environments
                        new ProcessBuilder("explorer.exe", path).start();
                    }
                } else {
                    Platform.runLater(() -> {
                        String msg = "The resolved path does not exist or is inaccessible: " + path + 
                                "\n\nPossible reasons:\n" +
                                "1. WSL distribution is not running.\n" +
                                "2. The 'WSL Distro' in General Settings is incorrect.\n" +
                                "3. Access is denied due to WSL permissions (Common for /var/lib/docker).";
                        
                        if (path.contains("var\\lib\\docker")) {
                            msg += "\n\nTo fix permissions, run these commands in your WSL terminal:\n" +
                                   "  sudo chmod 755 /var/lib/docker\n" +
                                   "  sudo chmod 755 /var/lib/docker/volumes";
                        }
                        
                        showAlert(Alert.AlertType.ERROR, "Access Denied / Path Not Found", msg);
                    });
                }
            } catch (Exception e) {
                logger.error("Failed to open path in explorer: {}", path, e);
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Error", 
                        "Failed to open explorer: " + e.getMessage()));
            }
        });
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
    private void handlePruneVolumes() {
        if (!checkConnection()) {
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Prune Unused Volumes");
        confirm.setHeaderText("Prune Unused Volumes");
        confirm.setContentText("Are you sure you want to remove ALL unused volumes? This cannot be undone.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                PruneResponse response = connectionManager.getDockerClient()
                        .pruneCmd(PruneType.VOLUMES)
                        .exec();
                
                long reclaimed = response.getSpaceReclaimed() != null ? response.getSpaceReclaimed() : 0;
                String msg = "Unused volumes pruned successfully.\nReclaimed space: " + FormatUtils.formatSize(reclaimed);
                showAlert(Alert.AlertType.INFORMATION, "Success", msg);
                refreshVolumes();
            } catch (RuntimeException e) {
                logger.error("Failed to prune volumes", e);
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to prune volumes: " + e.getMessage());
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

            List<Container> containers = connectionManager.getDockerClient()
                    .listContainersCmd()
                    .withShowAll(true)
                    .exec();

            Map<String, List<String>> volumeToContainers = volumeLogic.mapVolumesToContainers(containers);
            Set<String> runningVolumeNames = volumeLogic.getRunningContainerVolumeNames(containers);
            logger.info("Running container volume names: {}", runningVolumeNames);
            Set<String> danglingNames = getDanglingVolumeNames();
            Map<String, List<InspectVolumeResponse>> grouped = volumeLogic.groupVolumes(volumes);

            TreeItem<VolumeViewItem> root = new TreeItem<>(new VolumeViewItem("Root"));
            root.setExpanded(true);

            for (Map.Entry<String, List<InspectVolumeResponse>> entry : grouped.entrySet()) {
                TreeItem<VolumeViewItem> groupItem = new TreeItem<>(new VolumeViewItem(entry.getKey()));
                groupItem.setExpanded(true);
                for (InspectVolumeResponse vol : entry.getValue()) {
                    boolean unused = danglingNames.contains(vol.getName());
                    VolumeViewItem item = new VolumeViewItem(vol, vol.getName(), unused);
                    
                    List<String> containerNames = volumeToContainers.get(vol.getName());
                    if (containerNames != null) {
                        item.getContainerNames().setAll(containerNames);
                    }
                    
                    item.setInUseByRunningContainer(runningVolumeNames.contains(vol.getName()));
                    
                    groupItem.getChildren().add(new TreeItem<>(item));
                }
                root.getChildren().add(groupItem);
            }

            volumesTable.setRoot(root);
        } catch (RuntimeException e) {
            logger.error("Failed to refresh volumes", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to refresh volumes: " + e.getMessage());
        }
    }

    private Set<String> getDanglingVolumeNames() {
        try {
            List<InspectVolumeResponse> danglingVolumes = connectionManager.getDockerClient()
                    .listVolumesCmd()
                    .withFilter("dangling", Collections.singletonList("true"))
                    .exec()
                    .getVolumes();

            return volumeLogic.extractVolumeNames(danglingVolumes);
        } catch (Exception e) {
            logger.warn("Failed to fetch dangling volumes", e);
        }
        return Collections.emptySet();
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

    private void navigateToContainer(String containerName) {
        if (containerName == null || containerName.isEmpty()) {
            return;
        }

        // 1. Switch to Containers tab
        for (javafx.scene.control.Tab tab : mainTabPane.getTabs()) {
            if ("Containers".equals(tab.getText())) {
                mainTabPane.getSelectionModel().select(tab);
                break;
            }
        }

        // 2. Find and select container in the tree
        TreeItem<ContainerViewItem> root = containersTable.getRoot();
        if (root == null) {
            return;
        }

        for (TreeItem<ContainerViewItem> group : root.getChildren()) {
            for (TreeItem<ContainerViewItem> item : group.getChildren()) {
                ContainerViewItem containerItem = item.getValue();
                if (!containerItem.isGroup() && containerName.equals(containerItem.getName())) {
                    containersTable.getSelectionModel().select(item);
                    containersTable.scrollTo(containersTable.getSelectionModel().getSelectedIndex());
                    containersTable.requestFocus();
                    return;
                }
            }
        }
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
