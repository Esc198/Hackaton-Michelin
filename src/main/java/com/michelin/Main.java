package com.michelin;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import com.michelin.Optimization.AbstractOptimization;
import com.michelin.Optimization.HexagonalOptimization;
import com.michelin.Optimization.MaxForceOptimization;
import com.michelin.Optimization.Physic;
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
import javafx.scene.control.CheckBox;
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

            long radius = 100; // Internamente en base a mil
            long width = 800; // Internamente en base a mil
            long height = 600; // Internamente en base a mil
            long distBorder = 100; // Internamente en base a mil
            long distTire = 100; // Internamente en base a mil

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

            // Crear un label para mostrar el número de neumáticos válidos
            Label validTiresLabel = new Label("Neumáticos válidos: 0");
            validTiresLabel.setStyle("-fx-font-weight: bold;");

            // Create the coordinates tab
            VBox coordinatesBox = new VBox(5, validTiresLabel, new Label("Coordenadas de las ruedas:"),
                    coordinatesListView);
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

            // Modificar sliders y text fields para trabajar en base a mil
            Slider radiusSlider = new Slider(0, 200, radius);
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

            // Sincronizar sliders con text fields en base a mil
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

            // Agregar un checkbox para activar/desactivar las franjas diagonales
            CheckBox showStripesCheckbox = new CheckBox("Mostrar franjas diagonales");
            showStripesCheckbox.setSelected(false); // Por defecto desactivado
            controls.getChildren().add(showStripesCheckbox);

            // Listener para el checkbox de mostrar franjas diagonales
            showStripesCheckbox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
                gc.setFill(Color.LIGHTGRAY);
                gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
                gc.setStroke(Color.BLACK); // Asegurar que el borde sea negro
                gc.strokeRect(0, 0, canvas.getWidth(), canvas.getHeight());

                // Dibujar las franjas diagonales y las esquinas negras
                drawDiagonalStripes(gc, canvas.getWidth(), canvas.getHeight(), distBorderSlider.getValue(), newVal);
                drawCornerSquares(gc, canvas.getWidth(), canvas.getHeight(), distBorderSlider.getValue(), newVal);

                // Redibujar los neumáticos existentes con sus números
                if (optimizationMethod != null) {
                    List<Tire> currentTires = optimizationMethod.getResult();
                    for (int i = 0; i < currentTires.size(); i++) {
                        Tire tire = currentTires.get(i);
                        tire.draw(gc);
                        drawNumber(gc, currentTires, (int) canvas.getWidth(), (int) canvas.getHeight(),
                                (int) distBorderSlider.getValue());
                    }
                }
            });

            // Add listeners to update container size in real-time
            widthSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                canvas.setWidth(newVal.doubleValue());
                gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
                gc.setFill(Color.LIGHTGRAY);
                gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
                gc.setStroke(Color.BLACK); // Asegurar que el borde sea negro
                gc.strokeRect(0, 0, canvas.getWidth(), canvas.getHeight());
                drawAxes(gc);
                drawDiagonalStripes(gc, canvas.getWidth(), canvas.getHeight(), distBorderSlider.getValue(),
                        showStripesCheckbox.isSelected());
            });

            heightSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                canvas.setHeight(newVal.doubleValue());
                gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
                gc.setFill(Color.LIGHTGRAY);
                gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
                gc.setStroke(Color.BLACK); // Asegurar que el borde sea negro
                gc.strokeRect(0, 0, canvas.getWidth(), canvas.getHeight());
                drawAxes(gc);
                drawDiagonalStripes(gc, canvas.getWidth(), canvas.getHeight(), distBorderSlider.getValue(),
                        showStripesCheckbox.isSelected());
            });

            distBorderSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                drawDiagonalStripes(gc, canvas.getWidth(), canvas.getHeight(), newVal.doubleValue(),
                        showStripesCheckbox.isSelected());
            });

            // Reference to current animation timer to be able to stop it
            final AnimationTimer[] currentTimer = new AnimationTimer[1];

            // Add regeneration handler
            regenerateBtn.setOnAction(e -> {
                // Desactivar el checkbox de mostrar franjas diagonales
                showStripesCheckbox.setSelected(false);

                // Llamar a resetTireCount antes de cualquier otra acción
                Tire.resetTireCount();
                if (optimizationMethod != null && !optimizationMethod.isFinished()) {
                    optimizationMethod.stop();
                }
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
                clearAxes(gc);

                // Dibujar las franjas diagonales si están activadas
                drawDiagonalStripes(gc, newWidth, newHeight, distBorderSlider.getValue(),
                        showStripesCheckbox.isSelected());

                try {
                    // Create optimization based on selected class
                    Class<? extends AbstractOptimization> selectedClass = optimizationDropdown.getValue();
                    optimizationMethod = selectedClass.getDeclaredConstructor(
                            long.class, long.class, long.class, long.class, long.class).newInstance(
                                    (long) (radiusSlider.getValue() * 1000),
                                    (long) (newWidth * 1000),
                                    (long) (newHeight * 1000),
                                    (long) (distBorderSlider.getValue() * 1000),
                                    (long) (distTireSlider.getValue() * 1000));

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
                            List<Tire> validTires = new ArrayList<>();
                            
                            for (Tire tire : currentTires) {
                                if (Tire.isValidTire(tire, (long) (widthSlider.getValue() * 1000),
                                        (long) (heightSlider.getValue() * 1000),
                                        (long) (distBorderSlider.getValue() * 1000), currentTires,
                                        (long) (distTireSlider.getValue() * 1000))) {
                                    validTires.add(tire);
                                }
                            }
                        
                            for (Tire tire : validTires) {
                                    tire.draw(gc);

                            }

                            // Actualizar el contador
                            tireCountLabel.setText("Neumáticos válidos: " + validTires.size());
                            validTiresLabel.setText("Neumáticos válidos: " + validTires.size());

                            // Calcular la ocupación del contenedor
                            double totalTireArea = 0;
                            for (Tire tire : validTires) {
                                double r = tire.getRadius();
                                totalTireArea += Math.PI * r * r; // Área de un círculo
                            }

                            double containerArea = widthSlider.getValue() * heightSlider.getValue() * 1000000;
                            double occupancyPercentage = (totalTireArea / containerArea) * 100;

                            // Actualizar el porcentaje de ocupación
                            occupancyLabel.setText(String.format("Ocupación: %.2f%% ", occupancyPercentage));

                            // Check if optimization is complete
                            if (optimizationMethod.isFinished()) {
                                drawNumber(gc, validTires, (int) newWidth, (int) newHeight,
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
                    if (optimizationMethod != null) {
                        optimizationMethod.stop();
                    }
                    
                    ex.printStackTrace();
                }

                // Ensure controls are visible by bringing them to front
                controls.toFront();
            });

            // Create the scene
            StackPane root = new StackPane(canvas, tabPane);
            StackPane.setAlignment(tabPane, Pos.CENTER_RIGHT);
            Scene scene = new Scene(root, width, height);

            // Agregar un manejador para cuando se cierre la ventana
            primaryStage.setOnCloseRequest(event -> {
                if (optimizationMethod != null) {
                    optimizationMethod.stop();
                }
                Platform.exit();
            });

            primaryStage.setTitle("Aplicación Michelin");
            primaryStage.setScene(scene);
            primaryStage.show();

            // Dibujar los ejes después de mostrar la escena para asegurar que estén al
            // frente
            drawAxes(gc);
        } catch (Exception e) {
            if (optimizationMethod != null) {
                optimizationMethod.stop();
            }
            e.printStackTrace();
            Platform.exit();
        }
    }

    // Nueva función para dibujar los ejes
    private void drawAxes(GraphicsContext gc) {
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
    private void clearAxes(GraphicsContext gc) {
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
    private static void drawNumber(GraphicsContext gc, List<Tire> tires, int width, int height, int distBorder) {
        if (tires == null || tires.isEmpty()) {
            return;
        }

        // Ordenar las ruedas por posición Y y luego por posición X
        tires.sort(Comparator.comparingDouble(Tire::getPositionY)
                .thenComparingDouble(Tire::getPositionX));

        // Asignar números a las ruedas en orden de preferencia
        StringBuilder coordinates = new StringBuilder();
        for (int i = 0; i < tires.size(); i++) {
            Tire tire = tires.get(i);
            double x = tire.getPositionX() / 1000.0;
            double y = tire.getPositionY() / 1000.0;
            double r = tire.getRadius() / 1000.0;

            // Verificar si la rueda está dentro de los límites
            if (x - r >= distBorder &&
                    x + r <= width - distBorder &&
                    y - r >= distBorder &&
                    y + r <= height - distBorder) {
                gc.setFill(Color.WHITE);
                gc.setStroke(Color.BLACK);
                gc.setLineWidth(1);
                gc.setFont(Font.font("Algerian", FontWeight.BOLD, r / 2)); // Tamaño de fuente dinámico
                double textWidth = gc.getFont().getSize() / 2 * String.valueOf(i + 1).length();
                double textHeight = gc.getFont().getSize() / 2;
                gc.fillText(String.valueOf(i + 1), x - textWidth / 2, y + textHeight / 2);

                // Agregar coordenadas al texto
                coordinates.append("Rueda ").append(i + 1).append(": (")
                        .append(String.format("%.2f", x)).append(", ")
                        .append(String.format("%.2f", y)).append(")\n");
            }
        }

        // Actualizar la lista de coordenadas
        coordinatesListView.getItems().setAll(coordinates.toString().split("\n"));
    }

    // Nueva función para dibujar las franjas diagonales
    private void drawDiagonalStripes(GraphicsContext gc, double width, double height, double borderDistance,
            boolean showStripes) {
        if (!showStripes) {
            return;
        }

        gc.setLineWidth(2);

        // Dibujar franjas diagonales en el borde superior
        for (double x = 0; x < width; x += 20) {
            gc.setStroke(Color.BLACK);
            gc.strokeLine(x, 0, x - 20, borderDistance);
            gc.setStroke(Color.GRAY);
            gc.strokeLine(x + 10, 0, x - 10, borderDistance);
        }

        // Dibujar franjas diagonales en el borde inferior
        for (double x = 0; x < width; x += 20) {
            gc.setStroke(Color.BLACK);
            gc.strokeLine(x, height, x - 20, height - borderDistance);
            gc.setStroke(Color.GRAY);
            gc.strokeLine(x + 10, height, x - 10, height - borderDistance);
        }

        // Dibujar franjas diagonales en el borde izquierdo
        for (double y = 0; y < height; y += 20) {
            gc.setStroke(Color.BLACK);
            gc.strokeLine(0, y, borderDistance, y - 20);
            gc.setStroke(Color.GRAY);
            gc.strokeLine(0, y + 10, borderDistance, y - 10);
        }

        // Dibujar franjas diagonales en el borde derecho
        for (double y = 0; y < height; y += 20) {
            gc.setStroke(Color.BLACK);
            gc.strokeLine(width, y, width - borderDistance, y - 20);
            gc.setStroke(Color.GRAY);
            gc.strokeLine(width, y + 10, width - borderDistance, y - 10);
        }
    }

    // Nueva función para dibujar las esquinas negras
    private void drawCornerSquares(GraphicsContext gc, double width, double height, double borderDistance,
            boolean showCorners) {
        if (!showCorners) {
            return;
        }

        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, borderDistance, borderDistance); // Esquina superior izquierda
        gc.fillRect(width - borderDistance, 0, borderDistance, borderDistance); // Esquina superior derecha
        gc.fillRect(0, height - borderDistance, borderDistance, borderDistance); // Esquina inferior izquierda
        gc.fillRect(width - borderDistance, height - borderDistance, borderDistance, borderDistance); // Esquina
                                                                                                      // inferior
                                                                                                      // derecha
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