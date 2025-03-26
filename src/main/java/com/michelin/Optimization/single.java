package com.michelin.Optimization;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import com.michelin.utils.PhysicTire;
import com.michelin.utils.Tire;
public class single implements AbstractOptimization {
    private static class Vector2D {
        long x, y;
    }

    private static class SimulationConfig {
        final long WALL_REPULSION_FORCE = 1000_000_000;
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
            System.out.println("Fuerza: " + force.x + " " + force.y);

            return force;
        }

        private void addTireRepulsion(PhysicTire tire1, PhysicTire tire2, Vector2D force) {
            long dx = tire1.getX() - tire2.getX();
            long dy = tire1.getY() - tire2.getY();
            double dist = Math.sqrt(dx * dx + dy * dy);
            double minDist = (2.1 * config.tireRadius + config.distTire);
            
            if (Math.abs(dx) < 0.0001) dx = (long)((Math.random() - 0.5) * 0.1);
            if (Math.abs(dy) < 0.0001) dy = (long)((Math.random() - 0.5) * 0.1);
            
            if (dist < minDist && dist > 0.0001) {
                double magnitude = config.REPULSION_FORCE * Math.pow((minDist - dist) / minDist, 2);
                force.x += (long)((dx / dist) * magnitude);
                force.y += (long)((dy / dist) * magnitude);
            }
        }

        private void addBorderForces(PhysicTire tire, Vector2D force) {
            long[] distances = {
                tire.getX() - (config.distBorder + config.tireRadius),
                (config.containerWidth - config.distBorder - config.tireRadius) - tire.getX(),
                tire.getY() - (config.distBorder + config.tireRadius),
                (config.containerHeight - config.distBorder - config.tireRadius) - tire.getY()
            };

            long borderInfluence = config.distBorder * 3;

            for (int i = 0; i < distances.length; i++) {
                if (distances[i] < borderInfluence) {
                    double borderForce = config.WALL_REPULSION_FORCE * 
                        Math.exp(-distances[i] / (double)borderInfluence);
                    
                    if (distances[i] <= 0) {
                        borderForce *= 10.0;
                    }
                    
                    if (i < 2) {
                        force.x += i == 0 ? borderForce : -borderForce;
                    } else {
                        force.y += i == 2 ? -borderForce : borderForce;
                    }
                }
            }
            
            if (Math.abs(force.x) > 0 || Math.abs(force.y) > 0) {
                System.out.println("Rueda en (" + tire.getX() + "," + tire.getY() + "):");
                System.out.println("  Distancias [izq, der, sup, inf]: " + 
                                 distances[0] + ", " + distances[1] + ", " + 
                                 distances[2] + ", " + distances[3]);
                System.out.println("  Fuerzas resultantes: Fx=" + force.x + " Fy=" + force.y);
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
            
            newX = Math.max(config.distBorder + config.tireRadius, 
                            Math.min(config.containerWidth - config.distBorder - config.tireRadius, newX));
            newY = Math.max(config.distBorder + config.tireRadius, 
                            Math.min(config.containerHeight - config.distBorder - config.tireRadius, newY));
            
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

        private int countValidTires() {
            return (int) tires.stream().filter(tire -> PhysicTire.isValidTire(tire, config.containerWidth,
                    config.containerHeight, config.distBorder, tires, config.distTire)).count();
        }
    }

    private final SimulationConfig config;
    private final List<PhysicTire> tires;
    private final PhysicsEngine engine;
    private boolean isRunning;
    private Scanner scanner;

    public single(long tireRadius, long containerWidth, long containerHeight,
            long distBorder, long distTire) {
        this(tireRadius, containerWidth, containerHeight, distBorder, distTire, 1_000_000);
    }

    public single(long tireRadius, long containerWidth, long containerHeight,
            long distBorder, long distTire, long maxIteration) {
        this.config = new SimulationConfig(tireRadius, containerWidth, containerHeight,
                distBorder, distTire, maxIteration);
        this.tires = new ArrayList<>();
        this.engine = new PhysicsEngine(config, tires);
        this.isRunning = false;
        this.scanner = new Scanner(System.in);
    }

    @Override
    public void setup() {
        isRunning = true;
        int numTires = 10;
        for (int i = 0; i < numTires; i++) {
            long randomX = (long) (Math.random() * (config.containerWidth - 2 * config.distBorder) + config.distBorder);
            long randomY = (long) (Math.random() * (config.containerHeight - 2 * config.distBorder) + config.distBorder);
            tires.add(new PhysicTire("Tire" + i, config.tireRadius, randomX, randomY));
        }
        System.out.println("Iniciando simulación con " + numTires + " ruedas");
        System.out.println("Presiona Enter para continuar...");
        scanner.nextLine();
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
        
        if (engine.iteration % 10 == 0) {
            System.out.println("Iteración " + engine.iteration + 
                             " - Ruedas válidas: " + engine.countValidTires());
            for (PhysicTire tire : engine.getTires()) {
                System.out.printf("Rueda %s: pos=(%d,%d) vel=(%d,%d)%n", 
                    tire.getModel(), 
                    tire.getX(), 
                    tire.getY(),
                    tire.getCurrentSpeedX(),
                    tire.getCurrentSpeedY());
            }
        }

        if (engine.isFinished()) {
            System.out.println("\n=== Resultado Final ===");
            System.out.println("Total de ruedas: " + tires.size());
            System.out.println("Ruedas válidas: " + engine.countValidTires());
            System.out.println("====================");
            System.out.println("Presiona Enter para continuar...");
            scanner.nextLine();
            isRunning = false;
        }
    }

    public static void main(String[] args) {
        long tireRadius = 50;
        long containerWidth = 1000;
        long containerHeight = 1000;
        long distBorder = 100;
        long distTire = 20;
        long maxIteration = 10000;

        single simulation = new single(tireRadius, containerWidth, containerHeight,
                distBorder, distTire, maxIteration);
        
        simulation.setup();
        simulation.run();
    }
}
