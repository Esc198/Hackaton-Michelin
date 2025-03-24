package com.michelin;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Comparator;
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
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class Main extends Application {

    private AbstractOptimization optimizationMethod;
    private Label tireCountLabel;
    private Label occupancyLabel = new Label("Ocupación: 0%");
    private static ListView<String> coordinatesListView = new ListView<>();

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
            gc.setStroke(Color.BLACK); // Asegurar que el borde sea negro
            gc.strokeRect(0, 0, width, height);

            // Create control panel
            VBox controls = new VBox(10);
            controls.setPadding(new Insets(10));
            controls.setStyle("-fx-background-color: rgba(255,255,255,0.8)");
            controls.setMaxWidth(200);
            controls.setAlignment(Pos.TOP_RIGHT);
            controls.getChildren().add(occupancyLabel);

            // Create a TabPane
            TabPane tabPane = new TabPane();

            // Create the controls tab
            Tab controlsTab = new Tab("Controls", controls);
            controlsTab.setClosable(false);

            // Create the coordinates tab
            VBox coordinatesBox = new VBox(5, new Label("Coordenadas de las ruedas:"), coordinatesListView);
            coordinatesBox.setPadding(new Insets(20));
            coordinatesBox.setStyle("-fx-background-color: rgba(255,255,255,0.8)");
            coordinatesBox.setMaxWidth(250);
            coordinatesBox.setAlignment(Pos.TOP_LEFT); // Alinearlo a la izquierda
            Tab coordinatesTab = new Tab("Coordinates", coordinatesBox);
            coordinatesTab.setClosable(false);

            // Add tabs to the TabPane
            tabPane.getTabs().addAll(controlsTab, coordinatesTab);

            // Create optimization type dropdown with static list of implementations
            ComboBox<Class<? extends AbstractOptimization>> optimizationDropdown = new ComboBox<>();
            List<Class<? extends AbstractOptimization>> optimizationClasses = Arrays.asList(
                    HexagonalOptimization.class,
                    SquareGridOptimization.class,
                    MaxForceOptimization.class);

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
                gc.setStroke(Color.BLACK); // Asegurar que el borde sea negro
                gc.strokeRect(0, 0, canvas.getWidth(), canvas.getHeight());
                drawAxes(gc, canvas.getWidth(), canvas.getHeight());
            });

            heightSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                canvas.setHeight(newVal.doubleValue());
                gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
                gc.setFill(Color.LIGHTGRAY);
                gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
                gc.setStroke(Color.BLACK); // Asegurar que el borde sea negro
                gc.strokeRect(0, 0, canvas.getWidth(), canvas.getHeight());
                drawAxes(gc, canvas.getWidth(), canvas.getHeight());
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
                gc.setStroke(Color.BLACK); // Asegurar que el borde sea negro
                gc.strokeRect(0, 0, newWidth, newHeight);

                // Borrar los ejes antes de iniciar la optimización
                clearAxes(gc, newWidth, newHeight);

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
                            gc.setStroke(Color.BLACK); // Asegurar que el borde sea negro
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

                            // Calcular la ocupación del contenedor
                            double totalTireArea = 0;
                            for (Tire tire : currentTires) {
                                double x = tire.getPositionX();
                                double y = tire.getPositionY();
                                double r = tire.getRadius();

                                if (x - r >= distBorderSlider.getValue() &&
                                        x + r <= widthSlider.getValue() - distBorderSlider.getValue() &&
                                        y - r >= distBorderSlider.getValue() &&
                                        y + r <= heightSlider.getValue() - distBorderSlider.getValue()) {
                                    totalTireArea += Math.PI * r * r; // Área de un círculo
                                }
                            }

                            double containerArea = widthSlider.getValue() * heightSlider.getValue();
                            double occupancyPercentage = (totalTireArea / containerArea) * 100;

                            // Actualizar el prcentage de ocupación
                            occupancyLabel.setText(String.format("Ocupación: %.2f%% ", occupancyPercentage));

                            // Check if optimization is complete
                            if (optimizationMethod.isFinished()) {
                                // Dibujar los números en las ruedas
                                drawNumber(gc, currentTires, (int) newWidth, (int) newHeight,
                                        (int) distBorderSlider.getValue());
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
            StackPane root = new StackPane(canvas, tabPane);
            StackPane.setAlignment(tabPane, Pos.CENTER_RIGHT);
            Scene scene = new Scene(root, width, height);

            primaryStage.setTitle("Aplicación Michelin");
            primaryStage.setScene(scene);
            primaryStage.show();

            // Dibujar los ejes después de mostrar la escena para asegurar que estén al
            // frente
            drawAxes(gc, width, height);
        } catch (Exception e) {
            e.printStackTrace();
            Platform.exit();
        }
    }

    // Nueva función para dibujar los ejes
    private void drawAxes(GraphicsContext gc, double width, double height) {
        double arrowLength = 60; // 5 cm en píxeles (asumiendo 10 píxeles por cm)
        double startX = 15; // Ajustar la posición de la línea del borde izquierdo
        double startY = 15; // Ajustar la posición de la línea del borde superior

        gc.setLineWidth(3); // Hacer las flechas más gruesas

        // Eje X
        gc.setStroke(Color.RED);
        gc.strokeLine(startX, startY, startX + arrowLength, startY);
        gc.strokeLine(startX + arrowLength - 10, startY - 5, startX + arrowLength, startY);
        gc.strokeLine(startX + arrowLength - 10, startY + 5, startX + arrowLength, startY);

        // Eje Y
        gc.setStroke(Color.BLUE);
        gc.strokeLine(startX, startY, startX, startY + arrowLength);
        gc.strokeLine(startX - 5, startY + arrowLength - 10, startX, startY + arrowLength);
        gc.strokeLine(startX + 5, startY + arrowLength - 10, startX, startY + arrowLength);
    }

    // Nueva función para borrar los ejes
    private void clearAxes(GraphicsContext gc, double width, double height) {
        double arrowLength = 50; // 5 cm en píxeles (asumiendo 10 píxeles por cm)
        double startX = 10; // Ajustar la posición de la línea del borde izquierdo
        double startY = 10; // Ajustar la posición de la línea del borde superior

        gc.setLineWidth(3); // Hacer las flechas más gruesas

        // Eje X
        gc.setStroke(Color.LIGHTGRAY);
        gc.strokeLine(startX, startY, startX + arrowLength, startY);
        gc.strokeLine(startX + arrowLength - 10, startY - 5, startX + arrowLength, startY);
        gc.strokeLine(startX + arrowLength - 10, startY + 5, startX + arrowLength, startY);

        // Eje Y
        gc.setStroke(Color.LIGHTGRAY);
        gc.strokeLine(startX, startY, startX, startY + arrowLength);
        gc.strokeLine(startX - 5, startY + arrowLength - 10, startX, startY + arrowLength);
        gc.strokeLine(startX + 5, startY + arrowLength - 10, startX, startY + arrowLength);
    }

    // Nueva función para dibujar números en las ruedas
    public static void drawNumber(GraphicsContext gc, List<Tire> tires, int width, int height, int distBorder) {
        if (tires == null || tires.isEmpty()) {
            return;
        }

        // Ordenar las ruedas por posición X y luego por posición Y
        tires.sort(Comparator.comparingDouble(Tire::getPositionY)
                .thenComparingDouble(Tire::getPositionX));

        // Asignar números a las ruedas en orden de preferencia

        StringBuilder coordinates = new StringBuilder();
        for (int i = 0; i < tires.size(); i++) {
            Tire tire = tires.get(i);
            // Verificar la posición de la rueda
            double x = tire.getPositionX();
            double y = tire.getPositionY();
            double r = tire.getRadius();

            if (x - r >= distBorder &&
                    x + r <= width - distBorder &&
                    y - r >= distBorder &&
                    y + r <= height - distBorder) {
                tire.draw(gc);
                gc.setFill(Color.WHITE);
                gc.setStroke(Color.BLACK);
                gc.setLineWidth(1);
                gc.setFont(Font.font("Algerian", FontWeight.BOLD, r / 2)); // Tamaño de fuente dinámico
                double textWidth = gc.getFont().getSize() / 2 * String.valueOf(i + 1).length();
                double textHeight = gc.getFont().getSize() / 2;
                gc.fillText(String.valueOf(i + 1), x - textWidth / 2, y + textHeight / 2);
                coordinates.append("Rueda " + (i + 1) + ": (" + String.format("%.2f", x) + ", "
                        + String.format("%.2f", y) + ")\n");
            }

        }
        // Establecer el texto de la lista de coordenadas
        coordinatesListView.getItems().setAll(coordinates.toString().split("\n"));
    }

    public static void main(String[] args) {
        try {
            launch(args);
        } catch (Exception e) {
            System.exit(1);
        }
    }
}