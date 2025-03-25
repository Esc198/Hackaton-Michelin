package com.michelin.Optimization;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.michelin.utils.PhysicTire;
import com.michelin.utils.Tire;

public class SinglePhisicOptimization implements  AbstractOptimization{

    private SimulationConfig config;
    private float tireRadius;
    private float containerWidth;
    private float containerHeight;
    private float distBorder;
    private float distTire;
    private float maxIteration;
    private PhysicsEngine physicsEngine;
    private List<PhysicTire> tires;

    private static class SimulationConfig {
                final float WALL_REPULSION_FORCE = 1000000000;
                final float tireRadius;
                final float containerWidth;
                final float containerHeight;
                final float distBorder;
                final float distTire;
                final float maxIteration;
        
                final float REPULSION_FORCE = 1000f;
                final float DAMPING = 0.98f;
                final float DT = 0.016f;
                final float MIN_SPEED = 0.00001f;
        
                SimulationConfig(float tireRadius, float containerWidth, float containerHeight,
                        float distBorder, float distTire, float maxIteration) {
                    this.tireRadius = tireRadius;
                    this.containerWidth = containerWidth;
                    this.containerHeight = containerHeight;
                    this.distBorder = distBorder;
                    this.distTire = distTire;
                    this.maxIteration = maxIteration;
                }
        
                int getMaxWheelCount() {
                    return (int) ((containerWidth - 2 * distBorder) * (containerHeight - 2 * distBorder) /
                            (Math.PI * Math.pow(tireRadius + distTire, 2)));
                }
            }
        
            private static class PhysicsEngine {
                private final SimulationConfig config;
                private final List<PhysicTire> tires;
                private int iteration;
        
                PhysicsEngine(SimulationConfig config, List<PhysicTire> tires) {
                    this.config = config;
                    this.iteration = 0;
                    this.tires = new ArrayList<>(tires);
                }
        
                void simulatePhysics() {
                    tires.forEach(tire -> {
                        Vector2D force = calculateForces(tire, tires);
                        updateTirePhysics(tire, force);
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
                    float dx = tire1.getX() - tire2.getX();
                    float dy = tire1.getY() - tire2.getY();
                    float dist = (float) Math.sqrt(dx * dx + dy * dy);
                    float minDist = 2.1f * config.tireRadius + config.distTire;
                    if (dist > 0.0001f && dist < minDist) {  // Solo repeler cuando hay superposición
                        float overlap = minDist - dist;
                        float magnitude = config.REPULSION_FORCE * (overlap / minDist);
                        
                        force.x += (dx / dist) * magnitude;
                        force.y += (dy / dist) * magnitude;
                    }
                }
        
                private void addBorderForces(PhysicTire tire, Vector2D force) {
                    float[] distances = {
                            tire.getX() - config.distBorder - config.tireRadius, // left
                            config.containerWidth - tire.getX() - config.distBorder - config.tireRadius, // right
                            tire.getY() - config.distBorder - config.tireRadius, // top
                            config.containerHeight - tire.getY() - config.distBorder - config.tireRadius // bottom
                    };
        
                    for (int i = 0; i < distances.length; i++) {
                        if (distances[i] < config.distBorder) {
                            float borderForce = config.WALL_REPULSION_FORCE / Math.max(distances[i], 0.0001f);
                    if (i < 2)
                        force.x += i == 0 ? borderForce : -borderForce;
                    else
                        force.y += i == 2 ? borderForce : -borderForce;
                }
            }
        }

        private void updateTirePhysics(PhysicTire tire, Vector2D force) {
            // Actualizar velocidad
            tire.setCurrentSpeedX(tire.getCurrentSpeedX() * config.DAMPING + force.x * config.DT);
            tire.setCurrentSpeedY(tire.getCurrentSpeedY() * config.DAMPING + force.y * config.DT);

            // Aplicar velocidad mínima
            if (Math.abs(tire.getCurrentSpeedX()) < config.MIN_SPEED)
                tire.setCurrentSpeedX(0);
            if (Math.abs(tire.getCurrentSpeedY()) < config.MIN_SPEED)
                tire.setCurrentSpeedY(0);

            // Actualizar posición
            float newX = tire.getX() + tire.getCurrentSpeedX() * config.DT;
            float newY = tire.getY() + tire.getCurrentSpeedY() * config.DT;

            // Mantener dentro de límites
            newX = clamp(newX, config.distBorder + config.tireRadius,
                    config.containerWidth - config.distBorder - config.tireRadius);
            newY = clamp(newY, config.distBorder + config.tireRadius,
                    config.containerHeight - config.distBorder - config.tireRadius);

            tire.setX(newX);
            tire.setY(newY);
        }

        private float clamp(float value, float min, float max) {
            return Math.max(min, Math.min(max, value));
        }

        private void updateIteration() {
            iteration++;
        }

        private int getIteration() {
            return iteration;
        }

        public boolean isFinished() {
            return iteration >= config.maxIteration;
        }

        public List<PhysicTire> getTires() {
            return new ArrayList<>(tires);
        }


        private int countValidTires() {
            return (int) tires.stream().filter(tire -> !isOverlapping(tire, tires)).count();
        }

        private boolean isOverlapping(PhysicTire tire1, List<PhysicTire> others) {
            return others.stream().anyMatch(other -> other != tire1 && isOverlapping(tire1, other));
        }

        private boolean isOverlapping(PhysicTire tire1, PhysicTire tire2) {
            return Math.sqrt(Math.pow(tire1.getX() - tire2.getX(), 2) + Math.pow(tire1.getY() - tire2.getY(), 2)) < 2 * config.tireRadius + config.distTire;
        }

    }

    private static class Vector2D {
        float x, y;
    }

    public SinglePhisicOptimization(float tireRadius, float containerWidth, float containerHeight, 
            float distBorder, float distTire) {
        this(tireRadius, containerWidth, containerHeight, distBorder, distTire, 1_000_000);
    }

    public SinglePhisicOptimization(float tireRadius, float containerWidth, float containerHeight, 
            float distBorder, float distTire, float maxIteration) {
        this.tireRadius = tireRadius;
        this.containerWidth = containerWidth;
        this.containerHeight = containerHeight;
        this.distBorder = distBorder;
        this.distTire = distTire;
        this.maxIteration = maxIteration;
    }

    @Override
    public void setup() {
        this.config = new SimulationConfig(tireRadius, containerWidth, containerHeight, 
            distBorder, distTire, maxIteration);
        this.tires = initializeTires();
        this.physicsEngine = new PhysicsEngine(config, tires);
    }

    private List<PhysicTire> initializeTires() {
        List<PhysicTire> tires = new ArrayList<>();
        System.out.println("Ingrese el número de ruedas:");
        Scanner scanner = new Scanner(System.in);
        int maxWheelCount = scanner.nextInt();
        scanner.close();
        for (int i = 0; i < maxWheelCount; i++) {
            tires.add(createRandomTire(i));
        }
        return tires;
    }

    private PhysicTire createRandomTire(int index) {
        float randomX = generateRandomPosition(containerWidth);
        float randomY = generateRandomPosition(containerHeight);
        return new PhysicTire(100000, 100000, 100000, 100000, String.valueOf(index), 
            tireRadius, randomX, randomY);
    }

    private float generateRandomPosition(float containerSize) {
        return (float) (Math.random() * (containerSize - 2 * distBorder) + distBorder);
    }

    @Override
    public void run() {
        if (!physicsEngine.isFinished()) {
            physicsEngine.simulatePhysics();
            physicsEngine.updateIteration();
            
        }
    }

    @Override
    public List<Tire> getResult() {
        return new ArrayList<>(physicsEngine.getTires());
    }

    @Override
    public void stop() {
        // Implementar limpieza de recursos si es necesario
    }

    @Override
    public boolean isFinished() {
        return physicsEngine.isFinished();
    }
}
