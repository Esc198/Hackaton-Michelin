package com.michelin.utils;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.stage.Stage;
import java.util.ArrayList;
import java.util.List;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Michelin Tires");

        Group root = new Group();
        Canvas canvas = new Canvas(800, 600);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        root.getChildren().add(canvas);

        List<Tire> tires = new ArrayList<>();
        tires.add(new Tire("Model A", 50, 100, 100));
        tires.add(new Tire("Model B", 50, 200, 150));
        tires.add(new Tire("Model C", 50, 300, 200));
        tires.add(new Tire("Model D", 50, 400, 250));

        // Dibujar las ruedas
        for (Tire tire : tires) {
            tire.draw(gc);
        }
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
