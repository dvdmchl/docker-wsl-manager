package org.dreamabout.sw.dockerwslmanager;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public class ProcessListController {
    private static final Logger logger = LoggerFactory.getLogger(ProcessListController.class);

    @FXML
    private Label titleLabel;
    @FXML
    private TableView<String[]> processTable;
    @FXML
    private Button refreshButton;

    private String containerId;
    private String containerName;
    private DockerConnectionManager connectionManager;

    public void setContainerInfo(String containerId, String containerName, DockerConnectionManager connectionManager) {
        this.containerId = containerId;
        this.containerName = containerName;
        this.connectionManager = connectionManager;
        this.titleLabel.setText("Processes for: " + containerName);
        handleRefresh();
    }

    @FXML
    private void handleRefresh() {
        if (containerId == null || connectionManager == null || !connectionManager.isConnected()) {
            return;
        }

        refreshButton.setDisable(true);
        CompletableFuture.runAsync(() -> {
            try {
                com.github.dockerjava.api.command.TopContainerResponse response = 
                        connectionManager.getDockerClient().topContainerCmd(containerId).exec();
                
                Platform.runLater(() -> updateTable(response));
            } catch (Exception e) {
                logger.error("Failed to fetch processes for container {}", containerId, e);
            } finally {
                Platform.runLater(() -> refreshButton.setDisable(false));
            }
        });
    }

    private void updateTable(com.github.dockerjava.api.command.TopContainerResponse response) {
        processTable.getColumns().clear();
        processTable.getItems().clear();

        String[] titles = response.getTitles();
        if (titles == null || titles.length == 0) {
            return;
        }

        for (int i = 0; i < titles.length; i++) {
            final int index = i;
            TableColumn<String[], String> column = new TableColumn<>(titles[i]);
            column.setCellValueFactory(data -> {
                String[] row = data.getValue();
                if (index < row.length) {
                    return new SimpleStringProperty(row[index]);
                }
                return new SimpleStringProperty("");
            });
            column.setPrefWidth(100);
            processTable.getColumns().add(column);
        }

        String[][] processes = response.getProcesses();
        if (processes != null) {
            processTable.setItems(FXCollections.observableArrayList(Arrays.asList(processes)));
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) titleLabel.getScene().getWindow();
        stage.close();
    }
}
