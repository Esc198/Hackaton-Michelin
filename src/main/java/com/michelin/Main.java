package com.michelin;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

import com.michelin.Optimization.AbstractOptimization;
import com.michelin.Optimization.HexagonalOptimization;
import com.michelin.Optimization.MaxForceOptimization;
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
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class Main extends Application {

    private AbstractOptimization optimizationMethod;
    private Label tireCountLabel;

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

            // Create optimization type dropdown with static list of implementations
            ComboBox<Class<? extends AbstractOptimization>> optimizationDropdown = new ComboBox<>();
            List<Class<? extends AbstractOptimization>> optimizationClasses = Arrays.asList(
                    HexagonalOptimization.class,
                    SquareGridOptimization.class,
                    MaxForceOptimization.class

            // Add more optimization classes here as they are implemented
            );

            optimizationDropdown.getItems().addAll(optimizationClasses);
            if (!optimizationClasses.isEmpty()) {
                optimizationDropdown.setValue(optimizationClasses.get(0));
            }

            // Custom cell factory to show simple class names
            optimizationDropdown
                    .setCellFactory(p -> new javafx.scene.control.ListCell<Class<? extends AbstractOptimization>>() {
                        @Override
                        protected void updateItem(Class<? extends AbstractOptimization> item, boolean empty) {
                            super.updateItem(item, empty);
                            if (item != null && !empty) {
                                setText(item.getSimpleName());
                            } else {
                                setText(null);
                            }
                        }
                    });

            optimizationDropdown.setButtonCell(optimizationDropdown.getCellFactory().call(null));

            // Add sliders and text fields for parameters
            Slider radiusSlider = new Slider(20, 200, radius);
            radiusSlider.setShowTickLabels(true);
            radiusSlider.setShowTickMarks(true);
            TextField radiusField = new TextField(String.valueOf(radius));
            radiusField.setMaxWidth(60);

            Slider widthSlider = new Slider(400, 1200, width);
            widthSlider.setShowTickLabels(true);
            widthSlider.setShowTickMarks(true);
            TextField widthField = new TextField(String.valueOf(width));
            widthField.setMaxWidth(60);

            Slider heightSlider = new Slider(300, 900, height);
            heightSlider.setShowTickLabels(true);
            heightSlider.setShowTickMarks(true);
            TextField heightField = new TextField(String.valueOf(height));
            heightField.setMaxWidth(60);

            Slider distBorderSlider = new Slider(0, 200, distBorder);
            distBorderSlider.setShowTickLabels(true);
            distBorderSlider.setShowTickMarks(true);
            TextField distBorderField = new TextField(String.valueOf(distBorder));
            distBorderField.setMaxWidth(60);

            Slider distTireSlider = new Slider(0, 200, distTire);
            distTireSlider.setShowTickLabels(true);
            distTireSlider.setShowTickMarks(true);
            TextField distTireField = new TextField(String.valueOf(distTire));
            distTireField.setMaxWidth(60);

            // Sync sliders with text fields
            radiusSlider.valueProperty()
                    .addListener((obs, oldVal, newVal) -> radiusField.setText(String.valueOf(newVal.intValue())));
            radiusField.setOnAction(e -> radiusSlider.setValue(Double.parseDouble(radiusField.getText())));
            radiusField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal.isEmpty()) {
                    radiusSlider.setValue(Double.parseDouble(newVal));
                }
            });

            widthSlider.valueProperty()
                    .addListener((obs, oldVal, newVal) -> widthField.setText(String.valueOf(newVal.intValue())));
            widthField.setOnAction(e -> widthSlider.setValue(Double.parseDouble(widthField.getText())));
            widthField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal.isEmpty()) {
                    widthSlider.setValue(Double.parseDouble(newVal));
                }
            });

            heightSlider.valueProperty()
                    .addListener((obs, oldVal, newVal) -> heightField.setText(String.valueOf(newVal.intValue())));
            heightField.setOnAction(e -> heightSlider.setValue(Double.parseDouble(heightField.getText())));
            heightField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal.isEmpty()) {
                    heightSlider.setValue(Double.parseDouble(newVal));
                }
            });

            distBorderSlider.valueProperty()
                    .addListener((obs, oldVal, newVal) -> distBorderField.setText(String.valueOf(newVal.intValue())));
            distBorderField.setOnAction(e -> distBorderSlider.setValue(Double.parseDouble(distBorderField.getText())));
            distBorderField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal.isEmpty()) {
                    distBorderSlider.setValue(Double.parseDouble(newVal));
                }
            });

            distTireSlider.valueProperty()
                    .addListener((obs, oldVal, newVal) -> distTireField.setText(String.valueOf(newVal.intValue())));
            distTireField.setOnAction(e -> distTireSlider.setValue(Double.parseDouble(distTireField.getText())));
            distTireField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal.isEmpty()) {
                    distTireSlider.setValue(Double.parseDouble(newVal));
                }
            });

            // Crear el label antes del regenerateBtn
            tireCountLabel = new Label("Neumáticos: 0");
            tireCountLabel.setStyle("-fx-font-weight: bold;");

            Button regenerateBtn = new Button("Regenerate");

            HBox radiusBox = new HBox(10, new Label("Tire Radius:"), radiusField);
            radiusBox.setAlignment(Pos.CENTER_RIGHT);
            HBox widthBox = new HBox(10, new Label("Container Width:"), widthField);
            widthBox.setAlignment(Pos.CENTER_RIGHT);
            HBox heightBox = new HBox(10, new Label("Container Height:"), heightField);
            heightBox.setAlignment(Pos.CENTER_RIGHT);
            HBox distBorderBox = new HBox(10, new Label("Border Distance:"), distBorderField);
            distBorderBox.setAlignment(Pos.CENTER_RIGHT);
            HBox distTireBox = new HBox(10, new Label("Tire Distance:"), distTireField);
            distTireBox.setAlignment(Pos.CENTER_RIGHT);

            controls.getChildren().addAll(
                    new Label("Optimization Type:"), optimizationDropdown,
                    radiusBox,
                    radiusSlider,
                    widthBox,
                    widthSlider,
                    heightBox,
                    heightSlider,
                    distBorderBox,
                    distBorderSlider,
                    distTireBox,
                    distTireSlider,
                    tireCountLabel,
                    regenerateBtn);

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
                // Llamar a resetTireCount antes de cualquier otra acción
                Tire.resetTireCount();
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

                try {
                    // Create optimization based on selected class
                    Class<? extends AbstractOptimization> selectedClass = optimizationDropdown.getValue();
                    optimizationMethod = selectedClass.getDeclaredConstructor(
                            float.class, float.class, float.class, float.class, float.class).newInstance(
                                    (float) radiusSlider.getValue(),
                                    (float) newWidth,
                                    (float) newHeight,
                                    (float) distBorderSlider.getValue(),
                                    (float) distTireSlider.getValue());

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

                            // Draw current state and count valid tires
                            List<Tire> currentTires = optimizationMethod.getResult();
                            int validTires = 0;

                            for (Tire tire : currentTires) {
                                // Verificar si la rueda está completamente dentro del contenedor
                                double x = tire.getPositionX();
                                double y = tire.getPositionY();
                                double r = tire.getRadius();

                                if (x - r >= distBorderSlider.getValue() &&
                                        x + r <= widthSlider.getValue() - distBorderSlider.getValue() &&
                                        y - r >= distBorderSlider.getValue() &&
                                        y + r <= heightSlider.getValue() - distBorderSlider.getValue()) {
                                    validTires++;
                                }

                                tire.draw(gc);
                            }

                            // Actualizar el contador
                            tireCountLabel.setText("Neumáticos válidos: " + validTires);

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

                } catch (IllegalAccessException | IllegalArgumentException | InstantiationException
                        | NoSuchMethodException | SecurityException | InvocationTargetException ex) {
                    ex.printStackTrace();
                }

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
            System.exit(1);
        }
    }
}