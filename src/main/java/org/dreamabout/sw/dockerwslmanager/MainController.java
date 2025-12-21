package org.dreamabout.sw.dockerwslmanager;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class MainController {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int LOGS_TAB_INDEX = 4;

    private DockerConnectionManager connectionManager;
    private Stage primaryStage;

    @FXML private TextField hostField;
    @FXML private TextField portField;
    @FXML private Label connectionStatusLabel;
    @FXML private Button connectEnvButton;
    @FXML private Button connectManualButton;
    @FXML private Button connectAutoButton;
    @FXML private Button disconnectButton;

    @FXML private TabPane mainTabPane;
    
    // Containers tab
    @FXML private TableView<Container> containersTable;
    @FXML private TableColumn<Container, String> containerIdColumn;
    @FXML private TableColumn<Container, String> containerNameColumn;
    @FXML private TableColumn<Container, String> containerImageColumn;
    @FXML private TableColumn<Container, String> containerStatusColumn;
    @FXML private Button startContainerButton;
    @FXML private Button stopContainerButton;
    @FXML private Button restartContainerButton;
    @FXML private Button removeContainerButton;
    @FXML private Button viewLogsButton;
    @FXML private Button attachConsoleButton;

    // Images tab
    @FXML private TableView<Image> imagesTable;
    @FXML private TableColumn<Image, String> imageIdColumn;
    @FXML private TableColumn<Image, String> imageRepoColumn;
    @FXML private TableColumn<Image, String> imageTagColumn;
    @FXML private TableColumn<Image, String> imageSizeColumn;
    @FXML private Button removeImageButton;
    @FXML private Button pullImageButton;

    // Volumes tab
    @FXML private TableView<com.github.dockerjava.api.command.InspectVolumeResponse> volumesTable;
    @FXML private TableColumn<com.github.dockerjava.api.command.InspectVolumeResponse, String> volumeNameColumn;
    @FXML private TableColumn<com.github.dockerjava.api.command.InspectVolumeResponse, String> volumeDriverColumn;
    @FXML private TableColumn<com.github.dockerjava.api.command.InspectVolumeResponse, String> volumeMountpointColumn;
    @FXML private Button removeVolumeButton;

    // Networks tab
    @FXML private TableView<Network> networksTable;
    @FXML private TableColumn<Network, String> networkIdColumn;
    @FXML private TableColumn<Network, String> networkNameColumn;
    @FXML private TableColumn<Network, String> networkDriverColumn;
    @FXML private TableColumn<Network, String> networkScopeColumn;
    @FXML private Button removeNetworkButton;

    // Logs tab
    @FXML private TextArea logsTextArea;

    @FXML
    public void initialize() {
        connectionManager = new DockerConnectionManager();
        
        // Initialize connection fields
        hostField.setText("localhost");
        portField.setText("2375");
        updateConnectionStatus();

        // Initialize containers table
        if (containerIdColumn != null) {
            containerIdColumn.setCellValueFactory(data -> 
                new SimpleStringProperty(data.getValue().getId().substring(0, Math.min(12, data.getValue().getId().length()))));
            containerNameColumn.setCellValueFactory(data -> 
                new SimpleStringProperty(getContainerName(data.getValue())));
            containerImageColumn.setCellValueFactory(data -> 
                new SimpleStringProperty(data.getValue().getImage()));
            containerStatusColumn.setCellValueFactory(data -> 
                new SimpleStringProperty(data.getValue().getStatus()));
        }

        // Initialize images table
        if (imageIdColumn != null) {
            imageIdColumn.setCellValueFactory(data -> 
                new SimpleStringProperty(data.getValue().getId().replace("sha256:", "").substring(0, 12)));
            imageRepoColumn.setCellValueFactory(data -> {
                String[] repoTags = data.getValue().getRepoTags();
                if (repoTags != null && repoTags.length > 0) {
                    String[] parts = repoTags[0].split(":");
                    return new SimpleStringProperty(parts.length > 0 ? parts[0] : "");
                }
                return new SimpleStringProperty("<none>");
            });
            imageTagColumn.setCellValueFactory(data -> {
                String[] repoTags = data.getValue().getRepoTags();
                if (repoTags != null && repoTags.length > 0) {
                    String[] parts = repoTags[0].split(":");
                    return new SimpleStringProperty(parts.length > 1 ? parts[1] : "latest");
                }
                return new SimpleStringProperty("<none>");
            });
            imageSizeColumn.setCellValueFactory(data -> 
                new SimpleStringProperty(formatSize(data.getValue().getSize())));
        }

        // Initialize volumes table
        if (volumeNameColumn != null) {
            volumeNameColumn.setCellValueFactory(data -> 
                new SimpleStringProperty(data.getValue().getName()));
            volumeDriverColumn.setCellValueFactory(data -> 
                new SimpleStringProperty(data.getValue().getDriver()));
            volumeMountpointColumn.setCellValueFactory(data -> 
                new SimpleStringProperty(data.getValue().getMountpoint()));
        }

        // Initialize networks table
        if (networkIdColumn != null) {
            networkIdColumn.setCellValueFactory(data -> 
                new SimpleStringProperty(data.getValue().getId().substring(0, Math.min(12, data.getValue().getId().length()))));
            networkNameColumn.setCellValueFactory(data -> 
                new SimpleStringProperty(data.getValue().getName()));
            networkDriverColumn.setCellValueFactory(data -> 
                new SimpleStringProperty(data.getValue().getDriver()));
            networkScopeColumn.setCellValueFactory(data -> 
                new SimpleStringProperty(data.getValue().getScope()));
        }
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    @FXML
    private void handleConnectEnvironment() {
        if (connectionManager.connectFromEnvironment()) {
            updateConnectionStatus();
            showAlert(Alert.AlertType.INFORMATION, "Connection Successful", 
                "Connected to Docker using DOCKER_HOST environment variable.");
            refreshAll();
        } else {
            showAlert(Alert.AlertType.ERROR, "Connection Failed", 
                "Failed to connect using DOCKER_HOST. Check if the environment variable is set correctly.");
        }
    }

    @FXML
    private void handleConnectManual() {
        try {
            String host = hostField.getText().trim();
            int port = Integer.parseInt(portField.getText().trim());
            
            if (connectionManager.connectManual(host, port)) {
                updateConnectionStatus();
                showAlert(Alert.AlertType.INFORMATION, "Connection Successful", 
                    String.format("Connected to Docker at %s:%d", host, port));
                refreshAll();
            } else {
                showAlert(Alert.AlertType.ERROR, "Connection Failed", 
                    String.format("Failed to connect to Docker at %s:%d", host, port));
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Invalid Port", "Please enter a valid port number.");
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
        Container selected = containersTable.getSelectionModel().getSelectedItem();
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
        Container selected = containersTable.getSelectionModel().getSelectedItem();
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
        Container selected = containersTable.getSelectionModel().getSelectedItem();
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
        Container selected = containersTable.getSelectionModel().getSelectedItem();
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
        Container selected = containersTable.getSelectionModel().getSelectedItem();
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
                            .exec(new com.github.dockerjava.api.async.ResultCallback.Adapter<com.github.dockerjava.api.model.Frame>() {
                                private final StringBuilder logs = new StringBuilder();
                                private long lastUpdate = System.currentTimeMillis();

                                @Override
                                public void onNext(com.github.dockerjava.api.model.Frame frame) {
                                    logs.append(new String(frame.getPayload()));

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
        Container selected = containersTable.getSelectionModel().getSelectedItem();
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
                            "Pulling image " + imageName + "... This may take a while."));
                    
                    connectionManager.getDockerClient().pullImageCmd(imageName)
                            .exec(new com.github.dockerjava.api.async.ResultCallback.Adapter<>()).awaitCompletion();
                    
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
        Image selected = imagesTable.getSelectionModel().getSelectedItem();
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
        com.github.dockerjava.api.command.InspectVolumeResponse selected = volumesTable.getSelectionModel().getSelectedItem();
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
        confirm.setContentText("Are you sure you want to remove network " + selected.getName() + "?");
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                connectionManager.getDockerClient().removeNetworkCmd(selected.getId()).exec();
                showAlert(Alert.AlertType.INFORMATION, "Success", "Network removed successfully.");
                refreshNetworks();
            } catch (Exception e) {
                logger.error("Failed to remove network", e);
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to remove network: " + e.getMessage());
            }
        }
    }

    private void refreshContainers() {
        if (!checkConnection()) return;

        try {
            List<Container> containers = connectionManager.getDockerClient()
                    .listContainersCmd()
                    .withShowAll(true)
                    .exec();
            
            ObservableList<Container> containerList = FXCollections.observableArrayList(containers);
            containersTable.setItems(containerList);
        } catch (Exception e) {
            logger.error("Failed to refresh containers", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to refresh containers: " + e.getMessage());
        }
    }

    private void refreshImages() {
        if (!checkConnection()) return;

        try {
            List<Image> images = connectionManager.getDockerClient()
                    .listImagesCmd()
                    .exec();
            
            ObservableList<Image> imageList = FXCollections.observableArrayList(images);
            imagesTable.setItems(imageList);
        } catch (Exception e) {
            logger.error("Failed to refresh images", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to refresh images: " + e.getMessage());
        }
    }

    private void refreshVolumes() {
        if (!checkConnection()) return;

        try {
            List<com.github.dockerjava.api.command.InspectVolumeResponse> volumes = connectionManager.getDockerClient()
                    .listVolumesCmd()
                    .exec()
                    .getVolumes();
            
            ObservableList<com.github.dockerjava.api.command.InspectVolumeResponse> volumeList = FXCollections.observableArrayList(volumes);
            volumesTable.setItems(volumeList);
        } catch (Exception e) {
            logger.error("Failed to refresh volumes", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to refresh volumes: " + e.getMessage());
        }
    }

    private void refreshNetworks() {
        if (!checkConnection()) return;

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
        if (containersTable != null) containersTable.getItems().clear();
        if (imagesTable != null) imagesTable.getItems().clear();
        if (volumesTable != null) volumesTable.getItems().clear();
        if (networksTable != null) networksTable.getItems().clear();
        if (logsTextArea != null) logsTextArea.clear();
    }

    private void updateConnectionStatus() {
        if (connectionManager.isConnected()) {
            connectionStatusLabel.setText("Connected: " + connectionManager.getCurrentConnectionString());
            connectionStatusLabel.setStyle("-fx-text-fill: green;");
            connectEnvButton.setDisable(true);
            connectManualButton.setDisable(true);
            connectAutoButton.setDisable(true);
            disconnectButton.setDisable(false);
            mainTabPane.setDisable(false);
        } else {
            connectionStatusLabel.setText("Not Connected");
            connectionStatusLabel.setStyle("-fx-text-fill: red;");
            connectEnvButton.setDisable(false);
            connectManualButton.setDisable(false);
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
            return container.getNames()[0];
        }
        return container.getId().substring(0, Math.min(12, container.getId().length()));
    }

    private String formatSize(Long size) {
        if (size == null) return "0 B";
        
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.2f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.2f MB", size / (1024.0 * 1024));
        return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
