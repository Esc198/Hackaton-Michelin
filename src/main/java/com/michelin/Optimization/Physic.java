package com.michelin.Optimization;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.michelin.utils.PhysicTire;
import com.michelin.utils.Tire;
public class Physic implements AbstractOptimization {
    
    
    
    
    
    
    private static class Vector2D {
        long x, y;
    }

    private static class SimulationConfig {
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

        SimulationConfig(long tireRadius, long containerWidth, long containerHeight,
                long distBorder, long distTire, long maxIteration) {
            this.tireRadius = tireRadius;
            this.containerWidth = containerWidth;
            this.containerHeight = containerHeight;
            this.distBorder = distBorder;
            this.distTire = distTire;
            this.maxIteration = maxIteration;
        }

    }

    private static class PhysicsEngine {
        private final SimulationConfig config;
        private final List<PhysicTire> tires;
        private int iteration;

        PhysicsEngine(SimulationConfig config, List<PhysicTire> tires) {
            this.config = config;
            this.iteration = 0;
            this.tires = tires;
        }

        void simulatePhysics() {
            tires.forEach(tire -> {
                updateTirePhysics(tire, calculateForces(tire, tires));
            });
        }

        private Vector2D calculateForces(PhysicTire tire, List<PhysicTire> others) {
            Vector2D force = new Vector2D();

            // Fuerzas entre ruedas
            others.stream()
                    .filter(other -> other != tire)
                    .forEach(other -> addTireRepulsion(tire, other, force));

            // Fuerzas de bordes
            addBorderForces(tire, force);

            return force;
        }

        private void addTireRepulsion(PhysicTire tire1, PhysicTire tire2, Vector2D force) {
            long dx = tire1.getX() - tire2.getX();
            long dy = tire1.getY() - tire2.getY();
            double dist = Math.sqrt(dx * dx + dy * dy);
            double minDist =  1.1 * (2 * config.tireRadius + config.distTire);
            
            if (Math.abs(dx) < 1) dx = (long)((Math.random() - 0.5) * 10);
            if (Math.abs(dy) < 1) dy = (long)((Math.random() - 0.5) * 10);
            
            if (dist < minDist && dist > 0.0001) {
                double magnitude = config.REPULSION_FORCE * Math.pow((minDist - dist) / minDist, 2);
                force.x += (long)((dx / dist) * magnitude);
                force.y += (long)((dy / dist) * magnitude);
            }
        }

        private void addBorderForces(PhysicTire tire, Vector2D force) {
            long[] distances = {
                tire.getX() - (config.distBorder + config.tireRadius), // Left border
                (config.containerWidth - 2 * config.distBorder - config.tireRadius) - tire.getX(), // Right border  
                tire.getY() - (config.distBorder + config.tireRadius), // Top border
                (config.containerHeight - 2 * config.distBorder - config.tireRadius) - tire.getY() // Bottom border
            };

            long borderInfluence = config.distBorder * 3;

            for (int i = 0; i < distances.length; i++) {
                if (distances[i] < 0) {
                    // Calculate exponential repulsion force that increases as distance decreases
                    double borderForce = config.WALL_REPULSION_FORCE * 
                        Math.exp(Math.abs(distances[i]) / (double)borderInfluence);
                    
                    // Apply much stronger force if tire crosses boundary
                    if (distances[i] <= 0) {
                        borderForce *= 100.0; // Increased multiplier for stronger boundary enforcement
                    }
                    
                    // Apply force in appropriate direction based on which border
                    if (i < 2) { // Horizontal borders
                        force.x += (i == 0) ? borderForce : -borderForce; // Left vs Right
                    } else { // Vertical borders 
                        force.y += (i == 2) ? borderForce : -borderForce; // Top vs Bottom
                    }
                }
            }
        }

        private void updateTirePhysics(PhysicTire tire, Vector2D force) {
            double newSpeedX = tire.getCurrentSpeedX() + force.x * config.DT;
            double newSpeedY = tire.getCurrentSpeedY() + force.y * config.DT;
            
            newSpeedX *= config.DAMPING;
            newSpeedY *= config.DAMPING;
            
            double speed = Math.sqrt(newSpeedX * newSpeedX + newSpeedY * newSpeedY);
            double maxSpeed = 2000.0;
            
            if (speed > maxSpeed) {
                newSpeedX = (newSpeedX / speed) * maxSpeed;
                newSpeedY = (newSpeedY / speed) * maxSpeed;
            }
            
            tire.setCurrentSpeedX((long)newSpeedX);
            tire.setCurrentSpeedY((long)newSpeedY);
            
            long newX = tire.getX() + (long)(newSpeedX * config.DT);
            long newY = tire.getY() + (long)(newSpeedY * config.DT);
            
           /*  newX = Math.max(config.distBorder + config.tireRadius, 
                            Math.min(config.containerWidth - config.distBorder - config.tireRadius, newX));
            newY = Math.max(config.distBorder + config.tireRadius, 
                            Math.min(config.containerHeight - config.distBorder - config.tireRadius, newY));
            */
            tire.setX(newX);
            tire.setY(newY);
        }

        private void updateIteration() {
            iteration++;
        }

        public boolean isFinished() {
            return iteration >= config.maxIteration;
        }

        public List<PhysicTire> getTires() {
            return tires;
        }

    }

    private final SimulationConfig config;
    private final List<PhysicTire> tires;
    private final PhysicsEngine engine;
    private boolean isRunning;
    private final long numTires;
    public Physic(long tireRadius, long containerWidth, long containerHeight,
            long distBorder, long distTire) {
        this(tireRadius, containerWidth, containerHeight, distBorder, distTire, 1_000_000, 10);
    }

    public Physic(long tireRadius, long containerWidth, long containerHeight,
            long distBorder, long distTire, long maxIteration, long numTires) {
        this.config = new SimulationConfig(tireRadius, containerWidth, containerHeight,
                distBorder, distTire, maxIteration);
        this.tires = new ArrayList<>();
        this.engine = new PhysicsEngine(config, tires);
        this.isRunning = false;
        this.numTires = numTires;
    }

    @Override
    public void setup() {
        isRunning = true;
        for (int i = 0; i < numTires; i++) {
            long randomX = (long) (Math.random() * (config.containerWidth - 2 * config.distBorder) + config.distBorder);
            long randomY = (long) (Math.random() * (config.containerHeight - 2 * config.distBorder) + config.distBorder);
            tires.add(new PhysicTire("Tire" + i, config.tireRadius, randomX, randomY));
        }
    }

    @Override
    public List<Tire> getResult() {
        return engine.getTires().stream()
                .map(tire -> (Tire) tire.clone())
                .collect(Collectors.toList());
    }

    @Override
    public boolean isFinished() {
        return !isRunning || engine.isFinished();
    }

    @Override
    public void run() {
        if (!isRunning) {
            System.out.println("Error: Debes llamar a setup() antes de run()");
            return;
        }
        
        engine.updateIteration();
        engine.simulatePhysics();
        

        if (engine.isFinished()) {
            isRunning = false;
        }
    }

}
