package com.michelin.Optimization;

import java.util.ArrayList;
import java.util.List;

import com.michelin.utils.Tire;

public class HexagonalOptimization implements AbstractOptimization {
    private final float radius;
    private final float width;
    private final float height;
    private final float distBorder;
    private final float distTire;


    private List<Tire> tires;
    
    public HexagonalOptimization(float radius, float width, float height, float distBorder, float distTire) {
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
        // Calculate effective dimensions accounting for border distance plus radius
        float effectiveWidth = width - 2 * (distBorder );
        float effectiveHeight = height - 2 * (distBorder);

        // Calculate spacing between tire centers
        float horizontalSpacing = 2 * radius + distTire;
        float verticalSpacing = (float)(Math.sqrt(3) * (radius + distTire/2));

        // Calculate number of tires that can fit
        int tiresPerRow = (int)(effectiveWidth / horizontalSpacing);
        int numRows = (int)(effectiveHeight / verticalSpacing);

        // Calculate starting positions to center the pattern
        // Add radius to ensure minimum distance from walls
        float startX = distBorder + radius;
        float startY = distBorder + radius;

        // Create hexagonal arrangement
        for (int row = 0; row < numRows; row++) {
            // Offset every other row by half the horizontal spacing
            float xOffset = (row % 2) * (horizontalSpacing / 2);
            
            for (int col = 0; col < tiresPerRow; col++) {
                float x = startX + col * horizontalSpacing + xOffset;
                float y = startY + row * verticalSpacing;
                
                // Only add tire if it fits within the effective area
                if (x + radius <= width - (distBorder ) && 
                    y + radius <= height - (distBorder)) {
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
