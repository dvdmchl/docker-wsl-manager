package org.dreamabout.sw.dockerwslmanager;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class IconGenerator extends Application {

    @Override
    public void start(Stage stage) {
        // SVG Content Construction
        // Gradient
        Stop[] stops = new Stop[] {
            new Stop(0, Color.web("#0db7ed")),
            new Stop(1, Color.web("#005c99"))
        };
        LinearGradient grad1 = new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE, stops);

        // Shadow
        DropShadow shadow = new DropShadow();
        shadow.setOffsetX(2);
        shadow.setOffsetY(2);
        shadow.setRadius(3); // stdDeviation 3 approx
        shadow.setColor(Color.rgb(0, 0, 0, 0.5));

        Circle background = new Circle(50, 50, 45);
        background.setFill(grad1);
        background.setEffect(shadow);

        // Path
        SVGPath path = new SVGPath();
        path.setContent("M7.05 10.5h4.8v4.8H7.05zM13.95 10.5h4.8v4.8h-4.8zM20.85 10.5h4.8v4.8h-4.8zM20.85 3.6h4.8v4.8h-4.8z"
                + "M27.75 3.6h4.8v4.8h-4.8zM7.05 17.4h4.8v4.8H7.05zM13.95 17.4h4.8v4.8h-4.8zM20.85 17.4h4.8v4.8h-4.8z"
                + "M27.75 17.4h4.8v4.8h-4.8zM27.75 10.5h4.8v4.8h-4.8zM34.65 10.5h4.8v4.8h-4.8zM34.65 17.4h4.8v4.8h-4.8z"
                + "M3.5 24.5c0 8.8 6.4 16.1 14.9 17.6v-6.8h5.3v6.8c8.5-1.5 14.9-8.8 14.9-17.6H3.5z");
        path.setFill(Color.WHITE);

        // Small circles
        Circle c1 = new Circle(7, 38, 1.5, Color.WHITE);
        Circle c2 = new Circle(11, 38, 1.5, Color.WHITE);
        Circle c3 = new Circle(15, 38, 1.5, Color.WHITE);

        Group iconGroup = new Group(path, c1, c2, c3);
        iconGroup.setTranslateX(20);
        iconGroup.setTranslateY(20);
        iconGroup.setScaleX(0.6);
        iconGroup.setScaleY(0.6);

        Group root = new Group(background, iconGroup);
        // Ensure root has a scene for CSS/rendering context if needed, though snapshot handles loose nodes too.
        new Scene(root, 100, 100, Color.TRANSPARENT);

        // Snapshot
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        WritableImage image = root.snapshot(params, null);

        // Convert to BufferedImage (No javafx-swing)
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        PixelReader pixelReader = image.getPixelReader();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = pixelReader.getArgb(x, y);
                bufferedImage.setRGB(x, y, argb);
            }
        }

        // Save
        File outputFile = new File("src/main/resources/app_icon.png");
        try {
            ImageIO.write(bufferedImage, "png", outputFile);
            System.out.println("Icon generated successfully: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        Platform.exit();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
