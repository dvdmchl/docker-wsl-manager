package org.dreamabout.sw.dockerwslmanager;

import com.github.dockerjava.api.command.InspectVolumeResponse;
import com.github.dockerjava.api.model.Container;
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
    private static final int LOGS_TAB_INDEX = 4;
    private static final String UNGROUPED_LABEL = "Ungrouped";

    private DockerConnectionManager connectionManager;

    @FXML
    private Label connectionStatusLabel;
    @FXML
    private Button connectAutoButton;
    @FXML
    private Button disconnectButton;

    @FXML
    private TabPane mainTabPane;

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
    private TreeTableColumn<ContainerViewItem, String> containerStatusColumn;
    @FXML
    private Button startContainerButton;
    @FXML
    private Button stopContainerButton;
    @FXML
    private Button restartContainerButton;
    @FXML
    private Button removeContainerButton;
    @FXML
    private Button viewLogsButton;
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
    private Button removeNetworkButton;

    // Logs tab
    @FXML
    private TextArea logsTextArea;

    @FXML
    public void initialize() {
        connectionManager = new DockerConnectionManager();

        updateConnectionStatus();

        // Auto-connect on startup
        autoConnectOnStartup();

        // Initialize containers table
        if (containerNameColumn != null) {
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
                        if (status.toLowerCase().startsWith("up")) {
                            setStyle("-fx-text-fill: green;");
                        } else if (status.toLowerCase().startsWith("exited")
                                   || status.toLowerCase().startsWith("created")) {
                            setStyle("-fx-text-fill: red;");
                        } else {
                            setStyle("");
                        }
                    }
                }
            });
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

    @FXML
    private void handleStopContainer() {
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

    @FXML
    private void handleRestartContainer() {
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

    @FXML
    private void handleRemoveContainer() {
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

    @FXML
    private void handleViewLogs() {
        Container selected = getSelectedContainer();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a container to view logs.");
            return;
        }

        try {
            // Switch to logs tab
            mainTabPane.getSelectionModel().select(LOGS_TAB_INDEX);

            logsTextArea.clear();
            logsTextArea.appendText("Fetching logs for " + getContainerName(selected) + "...\n\n");

            // Fetch logs in background
            new Thread(() -> {
                try {
                    connectionManager.getDockerClient().logContainerCmd(selected.getId())
                            .withStdOut(true)
                            .withStdErr(true)
                            .withTail(1000)
                            .withFollowStream(false)  // Don't follow stream to get logs immediately
                            .exec(new com.github.dockerjava.api.async.ResultCallback.Adapter<Frame>() {
                                private final StringBuilder logs = new StringBuilder();
                                private long lastUpdate = System.currentTimeMillis();

                                @Override
                                public void onNext(Frame frame) {
                                    logs.append(new String(frame.getPayload(), StandardCharsets.UTF_8));

                                    // Update UI periodically (every 100ms) to avoid too many updates
                                    long now = System.currentTimeMillis();
                                    if (now - lastUpdate > 100) {
                                        String currentLogs = logs.toString();
                                        Platform.runLater(() -> logsTextArea.setText(currentLogs));
                                        lastUpdate = now;
                                    }
                                }

                                @Override
                                public void onComplete() {
                                    super.onComplete();
                                    String finalLogs = logs.toString();
                                    Platform.runLater(() -> {
                                        logsTextArea.setText(finalLogs);
                                        if (finalLogs.isEmpty()) {
                                            logsTextArea.setText("No logs available for this container.");
                                        }
                                    });
                                }

                                @Override
                                public void onError(Throwable throwable) {
                                    super.onError(throwable);
                                    logger.error("Error in log stream", throwable);
                                    Platform.runLater(() -> {
                                        logsTextArea.setText("Error fetching logs: " + throwable.getMessage());
                                    });
                                }
                            }).awaitCompletion(5, java.util.concurrent.TimeUnit.SECONDS);

                } catch (InterruptedException e) {
                    logger.warn("Log fetching interrupted", e);
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    logger.error("Failed to fetch logs", e);
                    Platform.runLater(() -> {
                        logsTextArea.clear();
                        logsTextArea.appendText("Error fetching logs: " + e.getMessage());
                    });
                }
            }).start();
        } catch (Exception e) {
            logger.error("Failed to view logs", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to view logs: " + e.getMessage());
        }
    }

    @FXML
    private void handleAttachConsole() {
        Container selected = getSelectedContainer();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a container to attach.");
            return;
        }

        try {
            String containerId = selected.getId();
            String containerName = getContainerName(selected);

            // Check if container is running
            if (!selected.getState().equalsIgnoreCase("running")) {
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
            // Using cmd /c start to open a new window, then cmd /k to keep it open after command
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

    private void refreshContainers() {
        if (!checkConnection()) {
            return;
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
        } catch (Exception e) {
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
        } catch (Exception e) {
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
        } catch (Exception e) {
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
        if (logsTextArea != null) {
            logsTextArea.clear();
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
}
