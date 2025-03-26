package com.michelin.Optimization;

import java.util.ArrayList;
import java.util.List;

import com.michelin.utils.Tire;

public class SquareGridOptimization implements AbstractOptimization {
    private final long radius;
    private final long width;
    private final long height;
    private final long distBorder;
    private final long distTire;
    private List<Tire> tires;

    public SquareGridOptimization(long radius, long width, long height, long distBorder, long distTire) {
        this.radius = radius;
        this.width = width;
        this.height = height;
        this.distBorder = distBorder;
        this.distTire = distTire;
    }

    @Override
    public void setup() {
        tires = new ArrayList<>();
    }

    @Override
    public void run() {
        // Calculate effective dimensions accounting for border distance
        long effectiveWidth = width - distBorder;
        long effectiveHeight = height - distBorder;

        // Calculate spacing between tire centers (Prueba de cambio)
        long tireSpacing = 2 * radius + distTire;

        // Calculate number of tires that can fit in each direction
        long tiresPerRow = (effectiveWidth / tireSpacing);
        long tiresPerColumn = (effectiveHeight / tireSpacing);

        // Calculate starting position to center the grid
        long startX = distBorder + radius;
        long startY = distBorder + radius;

        // Place tires in a grid pattern
        for (int row = 0; row < tiresPerColumn; row++) {
            for (int col = 0; col < tiresPerRow; col++) {
                long x = startX + col * tireSpacing;
                long y = startY + row * tireSpacing;

                // Validate if the tire fits within the effective dimensions
                if (x + radius <= width - distBorder && y + radius <= height - distBorder) {
                    // Create new tire at calculated position
                    tires.add(new Tire("Michelin Pilot Sport", radius, x, y));
                }
            }
        }
    }

    @Override
    public List<Tire> getResult() {
        return tires;
    }

    @Override
    public boolean isFinished() {
        return true;
    }
}
