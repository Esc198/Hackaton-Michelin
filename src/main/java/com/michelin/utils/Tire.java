package com.michelin.utils;

import java.util.List;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Tire {
    private static int tireCount = 0; // Variable estática para contar las ruedas
    private int tireNumber; // Número de la rueda
    private String model;
    private long radius;
    private long positionX;
    private long positionY;
    private Color color;

    public Tire(String model, long radius, long x, long y) {
        this.model = model;
        this.radius = radius;
        this.positionX = x;
        this.positionY = y;
        this.color = Color.BLACK;
        this.tireNumber = ++tireCount; // Asignar el número de la rueda
    }

    // Getters
    public String getModel() {
        return model;
    }

    public long getRadius() {
        return radius;
    }

    public long getPositionX() {
        return positionX;
    }

    public long getPositionY() {
        return positionY;
    }

    public Color getColor() {
        return color;
    }

    // Setters
    public void setModel(String model) {
        this.model = model;
    }

    public void setRadius(long radius) {
        this.radius = radius;
    }

    public void setPositionX(long x) {
        this.positionX = x;
    }

    public void setPositionY(long y) {
        this.positionY = y;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    // Drawable method
    public void draw(GraphicsContext gc) {
        // Convert from millimeters (stored as long) to pixels (float) by dividing by 1000
        float x = positionX / 1000.0f; // Center X coordinate in pixels
        float y = positionY / 1000.0f; // Center Y coordinate in pixels 
        float r = this.radius / 1000.0f; // Radius in pixels

        // Draw the main black tire circle
        gc.setFill(color);
        gc.fillOval(x - r, y - r, r * 2, r * 2); // Draw centered at (x,y)

        // Draw inner gray ring to create tire rim effect
        gc.setStroke(Color.GRAY);
        gc.setLineWidth(5);
        gc.strokeOval(x - r + 5, y - r + 5, r * 2 - 10, r * 2 - 10); // Slightly smaller than main circle

        // Draw 8 evenly spaced tread marks around the tire
        gc.setLineWidth(2);
        gc.setStroke(Color.GRAY);
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4; // Divide circle into 8 equal segments
            // Calculate start/end points for each tread mark
            double startX = x + Math.cos(angle) * (r - 10); // Outer point
            double startY = y + Math.sin(angle) * (r - 10);
            double endX = x + Math.cos(angle) * (r - 20); // Inner point  
            double endY = y + Math.sin(angle) * (r - 20);
            gc.strokeLine(startX, startY, endX, endY);
        }
    }

    public void drawInvalid(GraphicsContext gc) {
        Color originalColor = color;
        color = Color.RED;
        draw(gc);
        color = originalColor;
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

    // Método para reiniciar el contador de ruedas
    public static void resetTireCount() {
        tireCount = 0;
    }

    public static boolean isValidTire(Tire tire, long width, long height, long distBorder, List<Tire> tires,
            long distTire) {
        long x = tire.getPositionX();
        long y = tire.getPositionY();
        long r = tire.getRadius();
        for (Tire otherTire : tires) {
            if (otherTire == tire) {
                continue;
            }
            long otherX = otherTire.getPositionX();
            long otherY = otherTire.getPositionY();
            long distance = (long) Math.sqrt((x - otherX) * (x - otherX) + (y - otherY) * (y - otherY));
            if (distance < r + otherTire.getRadius() + distTire - 1) {
                return false;
            }
        }
        return x - r >= distBorder && x + r <= width - distBorder && y - r >= distBorder
                && y + r <= height - distBorder;
    }

}