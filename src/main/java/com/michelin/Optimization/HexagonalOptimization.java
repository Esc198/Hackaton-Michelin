package com.michelin.Optimization;

import java.util.ArrayList;
import java.util.List;

import com.michelin.utils.Tire;
//Mario maricon
public class HexagonalOptimization implements AbstractOptimization {
    private final long radius;
    private final long width;
    private final long height;
    private final long distBorder;
    private final long distTire;


    private List<Tire> tires;
    
    public HexagonalOptimization(long radius, long  width, long height, long distBorder, long distTire) {
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
        float effectiveWidth = width - distBorder;
        float effectiveHeight = height - distBorder;

        // Calculate spacing between tire centers
        long horizontalSpacing = 2 * radius + distTire;
        long verticalSpacing = Math.round(Math.sqrt(3) * (radius + distTire/2.0));

        // Calculate number of tires that can fit
        int tiresPerRow = (int)(effectiveWidth / horizontalSpacing);
        int numRows = (int)(effectiveHeight / verticalSpacing);

        // Calculate starting positions to center the pattern
        // Add radius to ensure minimum distance from walls
        long startX = distBorder + radius;
        long startY = distBorder + radius;

        // Create hexagonal arrangement
        for (int row = 0; row < numRows; row++) {
            // Offset every other row by half the horizontal spacing
            long xOffset = (long) ((row % 2) * (horizontalSpacing / 2));
            
            for (int col = 0; col < tiresPerRow; col++) {
                long x = startX + col * horizontalSpacing + xOffset;
                long y = startY + row * verticalSpacing;
                
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
