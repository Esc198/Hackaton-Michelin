package com.michelin.Optimization;

import java.util.ArrayList;
import java.util.List;

import com.michelin.utils.Tire;

public class SquareGridOptimization implements AbstractOptimization {
    private final float radius;
    private final float width;
    private final float height;
    private final float distBorder;
    private final float distTire;
    private List<Tire> tires;

    public SquareGridOptimization(float radius, float width, float height, float distBorder, float distTire) {
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
        float effectiveWidth = width - 2 * (distBorder);
        float effectiveHeight = height - 2 * ( distBorder);
        
        // Calculate spacing between tire centers
        float tireSpacing = 2 * radius + distTire;
        
        // Calculate number of tires that can fit in each direction
        int tiresPerRow = (int)(effectiveWidth / tireSpacing);
        int tiresPerColumn = (int)(effectiveHeight / tireSpacing);
        
        // Calculate starting position to center the grid
        float startX = distBorder;
        float startY = distBorder;

        // Place tires in a grid pattern
        for (int row = 0; row < tiresPerColumn; row++) {
            for (int col = 0; col < tiresPerRow; col++) {
                float x = startX + col * tireSpacing;
                float y = startY + row * tireSpacing;
                
                // Create new tire at calculated position
                tires.add(new Tire("Michelin Pilot Sport", radius, x, y));
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
