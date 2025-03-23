package com.michelin.utils;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Tire {
    private String model;
    private float radius;
    private float positionX;
    private float positionY;
    private Color color;

    public Tire(String model, double radius, double x, double y) {
        this.model = model;
        this.radius = (float)radius;
        this.positionX = (float)x;
        this.positionY = (float)y;
        this.color = Color.BLACK;
    }

    // Getters
    public String getModel() {
        return model;
    }

    public float getRadius() {
        return radius;
    }

    public float getPositionX() {
        return positionX;
    }

    public float getPositionY() {
        return positionY;
    }

    public Color getColor() {
        return color;
    }

    // Setters
    public void setModel(String model) {
        this.model = model;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public void setPositionX(float x) {
        this.positionX = x;
    }

    public void setPositionY(float y) {
        this.positionY = y;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    // Drawable method
    public void draw(GraphicsContext gc) {
        gc.setFill(color);
        gc.fillOval(positionX - radius, positionY - radius, radius * 2, radius * 2);
        
        // Add some details to make it look more like a tire
        gc.setStroke(Color.GRAY);
        gc.setLineWidth(5);
        gc.strokeOval(positionX - radius + 5, positionY - radius + 5, radius * 2 - 10, radius * 2 - 10);
        
        // Add tread pattern
        gc.setLineWidth(2);
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4;
            double startX = positionX + Math.cos(angle) * (radius - 10);
            double startY = positionY + Math.sin(angle) * (radius - 10);
            double endX = positionX + Math.cos(angle) * (radius - 20);
            double endY = positionY + Math.sin(angle) * (radius - 20);
            gc.strokeLine(startX, startY, endX, endY);
        }
    }

    @Override
    public String toString() {
        return "Tire{" +
                "model='" + model + '\'' +
                ", radius=" + radius +
                ", positionX=" + positionX +
                ", positionY=" + positionY +
                ", color=" + color +
                '}';
    }
}