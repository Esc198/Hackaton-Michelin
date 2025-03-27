package com.michelin.Optimization;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.michelin.utils.PhysicTire;
import com.michelin.utils.Tire;
public class Physic implements AbstractOptimization {
    final long WALL_REPULSION_FORCE = 1_000_000_000;
    final long tireRadius;
    final long containerWidth;
    final long containerHeight;
    final long distBorder;
    final long distTire;
    final long maxIteration;
    final long REPULSION_FORCE = 100_000_000;
    final float DAMPING = 0.90f;
    final float DT = 0.016f;
    
    private final List<PhysicTire> tires;
    private final long numTires;
    private int iteration;
    
    public Physic(long tireRadius, long containerWidth, long containerHeight,
    long distBorder, long distTire, long maxIteration, long numTires) {
        this.tireRadius = tireRadius;
        this.containerWidth = containerWidth;
        this.containerHeight = containerHeight;
        this.distBorder = distBorder;
        this.distTire = distTire;
        this.maxIteration = maxIteration;
        this.tires = new ArrayList<>();
        this.numTires = numTires;
    }
    
    @Override
    public void setup() {
        tires.clear();
        for (int i = 0; i < numTires; i++) {
            long randomX = (long) (Math.random() * (containerWidth - 2 * distBorder) + distBorder);
            long randomY = (long) (Math.random() * (containerHeight - 2 * distBorder) + distBorder);
            tires.add(new PhysicTire("Tire" + i, tireRadius, randomX, randomY));
        }
        iteration = 0;
    }
    
    @Override
    public List<Tire> getResult() {
        return tires.stream()
        .map(tire -> tire.clone())
        .collect(Collectors.toList());
    }
    
    @Override
    public boolean isFinished() {
        return iteration >= maxIteration;
    }
    @Override
    public void run() {
        tires.forEach(tire -> {
            updateTirePhysics(tire, calculateForces(tire, tires));
        });
        iteration++;
    }

    private Vector2D calculateForces(PhysicTire tire, List<PhysicTire> others) {
        Vector2D force = new Vector2D();
        
        // Escalar las fuerzas según el radio del neumático
        long scaledRepulsionForce = (long)(REPULSION_FORCE * (tireRadius / 100.0));
        
        // Fuerzas entre ruedas con fuerza escalada
        others.stream()
            .filter(other -> other != tire)
            .forEach(other -> addTireRepulsion(tire, other, force, scaledRepulsionForce));
        
        // Fuerzas de bordes
        addBorderForces(tire, force);
        
        return force;
    }
    
    private void addTireRepulsion(PhysicTire tire1, PhysicTire tire2, Vector2D force, long scaledRepulsionForce) {
        long dx = tire1.getX() - tire2.getX();
        long dy = tire1.getY() - tire2.getY();
        double dist = Math.sqrt(dx * dx + dy * dy);
        double minDist =  1.1 * (2 * tireRadius + distTire);
        
        if (dist < minDist && dist > 0.0001) {
            double magnitude = scaledRepulsionForce * Math.pow((minDist - dist) / minDist, 2);
            force.x += (long)((dx / dist) * magnitude);
            force.y += (long)((dy / dist) * magnitude); 
        }
    }
    
    private void addBorderForces(PhysicTire tire, Vector2D force) {
        long[] distances = {
            tire.getX() - (distBorder + tireRadius), // Left border
            (containerWidth - distBorder - tireRadius) - tire.getX(), // Right border  
            tire.getY() - (distBorder + tireRadius), // Top border
            (containerHeight - distBorder - tireRadius) - tire.getY() // Bottom border
        };
        
        
        for (int i = 0; i < distances.length; i++) {
            if (distances[i] < 0) {
                double borderForce = WALL_REPULSION_FORCE * 
                Math.exp(Math.abs(distances[i]) / (double)distBorder);
                if (i < 2) { 
                    force.x += (i == 0) ? borderForce : -borderForce;
                } else {
                    force.y += (i == 2) ? borderForce : -borderForce;
                }
            }
        }
    }
        
    private void updateTirePhysics(PhysicTire tire, Vector2D force) {
        double newSpeedX = tire.getCurrentSpeedX() + force.x * DT;
        double newSpeedY = tire.getCurrentSpeedY() + force.y * DT;
        
        double speed = Math.sqrt(newSpeedX * newSpeedX + newSpeedY * newSpeedY);
        double maxSpeed = 20000.0;
        
        if (speed > maxSpeed) {
            newSpeedX = (newSpeedX / speed) * maxSpeed;
            newSpeedY = (newSpeedY / speed) * maxSpeed;
        }
        
        
        newSpeedX *= DAMPING;
        newSpeedY *= DAMPING;
        
        tire.setCurrentSpeedX((long)newSpeedX);
        tire.setCurrentSpeedY((long)newSpeedY);
        
        long newX = tire.getX() + (long)(newSpeedX * DT);
        long newY = tire.getY() + (long)(newSpeedY * DT);
        
        /*  newX = Math.max(config.distBorder + config.tireRadius, 
        Math.min(config.containerWidth - config.distBorder - config.tireRadius, newX));
        newY = Math.max(config.distBorder + config.tireRadius, 
        Math.min(config.containerHeight - config.distBorder - config.tireRadius, newY));
        */
        tire.setX(newX);
        tire.setY(newY);
    }
        
        
    
    private static class Vector2D {
        long x, y;
    }
}
