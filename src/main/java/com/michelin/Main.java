package com.michelin;

import java.util.List;

import com.michelin.Optimization.AbstractOptimization;
import com.michelin.Optimization.HexagonalOptimization;
import com.michelin.Optimization.SquareGridOptimization;
import com.michelin.utils.Tire;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class Main extends Application {

    private AbstractOptimization optimizationMethod;
    @Override
    public void start(Stage primaryStage) {
        try {
            // Create a canvas
            Canvas canvas = new Canvas(800, 600);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            
            float radius = 100;
            float width = 800;
            float height = 600;
            float distBorder = 100;
            float distTire = 100;

            // Draw container rectangle with light gray fill
            gc.setFill(Color.LIGHTGRAY);
            gc.fillRect(0, 0, width, height);
            gc.strokeRect(0, 0, width, height);

            // Create control panel
            VBox controls = new VBox(10);
            controls.setPadding(new Insets(10));
            controls.setStyle("-fx-background-color: rgba(255,255,255,0.8)");
            controls.setMaxWidth(200);
            controls.setAlignment(Pos.TOP_RIGHT);

            // Create optimization type dropdown
            ComboBox<String> optimizationDropdown = new ComboBox<>();
            optimizationDropdown.getItems().addAll("Hexagonal", "Square Grid");
            optimizationDropdown.setValue("Hexagonal");

            // Add sliders for parameters
            Slider radiusSlider = new Slider(20, 200, radius);
            radiusSlider.setShowTickLabels(true);
            radiusSlider.setShowTickMarks(true);

            Slider widthSlider = new Slider(400, 1200, width);
            widthSlider.setShowTickLabels(true);
            widthSlider.setShowTickMarks(true);

            Slider heightSlider = new Slider(300, 900, height);
            heightSlider.setShowTickLabels(true);
            heightSlider.setShowTickMarks(true);

            Slider distBorderSlider = new Slider(0, 200, distBorder);
            distBorderSlider.setShowTickLabels(true);
            distBorderSlider.setShowTickMarks(true);

            Slider distTireSlider = new Slider(0, 200, distTire);
            distTireSlider.setShowTickLabels(true);
            distTireSlider.setShowTickMarks(true);

            Button regenerateBtn = new Button("Regenerate");

            controls.getChildren().addAll(
                new Label("Optimization Type:"), optimizationDropdown,
                new Label("Tire Radius:"), radiusSlider,
                new Label("Container Width:"), widthSlider,
                new Label("Container Height:"), heightSlider,
                new Label("Border Distance:"), distBorderSlider,
                new Label("Tire Distance:"), distTireSlider,
                regenerateBtn
            );

            // Add listeners to update container size in real-time
            widthSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                canvas.setWidth(newVal.doubleValue());
                gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
                gc.setFill(Color.LIGHTGRAY);
                gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
                gc.strokeRect(0, 0, canvas.getWidth(), canvas.getHeight());
            });

            heightSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                canvas.setHeight(newVal.doubleValue());
                gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
                gc.setFill(Color.LIGHTGRAY);
                gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
                gc.strokeRect(0, 0, canvas.getWidth(), canvas.getHeight());
            });

            // Reference to current animation timer to be able to stop it
            final AnimationTimer[] currentTimer = new AnimationTimer[1];

            // Add regeneration handler
            regenerateBtn.setOnAction(e -> {
                // Stop current optimization if running
                if (currentTimer[0] != null) {
                    currentTimer[0].stop();
                }

                // Update canvas size
                double newWidth = widthSlider.getValue();
                double newHeight = heightSlider.getValue();
                canvas.setWidth(newWidth);
                canvas.setHeight(newHeight);

                gc.clearRect(0, 0, newWidth, newHeight);
                gc.setFill(Color.LIGHTGRAY);
                gc.fillRect(0, 0, newWidth, newHeight);
                gc.strokeRect(0, 0, newWidth, newHeight);
                
                // Create optimization based on selected type
                if (optimizationDropdown.getValue().equals("Hexagonal")) {
                    optimizationMethod = new HexagonalOptimization(
                        (float)radiusSlider.getValue(),
                        (float)newWidth,
                        (float)newHeight, 
                        (float)distBorderSlider.getValue(),
                        (float)distTireSlider.getValue()
                    );
                } else {
                    optimizationMethod = new SquareGridOptimization(
                        (float)radiusSlider.getValue(),
                        (float)newWidth,
                        (float)newHeight,
                        (float)distBorderSlider.getValue(),
                        (float)distTireSlider.getValue()
                    );
                }
                
                optimizationMethod.setup();
                
                // Create animation timer to handle continuous optimization
                AnimationTimer timer = new AnimationTimer() {
                    @Override
                    public void handle(long now) {
                        // Run one optimization step
                        optimizationMethod.run();
                        
                        // Clear and redraw
                        gc.clearRect(0, 0, newWidth, newHeight);
                        gc.setFill(Color.LIGHTGRAY);
                        gc.fillRect(0, 0, newWidth, newHeight);
                        gc.strokeRect(0, 0, newWidth, newHeight);
                        
                        // Draw current state
                        List<Tire> currentTires = optimizationMethod.getResult();
                        for (Tire tire : currentTires) {
                            tire.draw(gc);
                        }

                        // Check if optimization is complete
                        if (optimizationMethod.isFinished()) {
                            this.stop();
                        }
                        
                        // Ensure controls stay visible
                        controls.toFront();
                    }
                };
                
                currentTimer[0] = timer;
                timer.start();
                
                // Ensure controls are visible by bringing them to front
                controls.toFront();
            });
            
            // Create the scene
            StackPane root = new StackPane(canvas, controls);
            StackPane.setAlignment(controls, Pos.CENTER_RIGHT);
            Scene scene = new Scene(root, width, height);
            
            primaryStage.setTitle("Aplicación Michelin");
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
            Platform.exit();
        }
    }

    public static void main(String[] args) {
        try {
            launch(args);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
} 