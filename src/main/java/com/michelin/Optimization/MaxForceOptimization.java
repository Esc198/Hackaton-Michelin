package com.michelin.Optimization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import com.michelin.utils.PhysicTire;
import com.michelin.utils.Tire;

public class MaxForceOptimization implements AbstractOptimization {
    private final SimulationConfig config;
    private final AtomicBoolean isRunning;
    private final ExecutorService executor;
    private final Map<Integer, List<PhysicTire>> bestConfiguration = new HashMap<>();

    public MaxForceOptimization(long tireRadius, long containerWidth, long containerHeight,
            long distBorder, long distTire, long maxIteration) {
        this.config = new SimulationConfig(tireRadius, containerWidth, containerHeight,
                distBorder, distTire, maxIteration);
        this.isRunning = new AtomicBoolean(false);
        this.executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        // Agregar shutdown hook para limpieza en caso de interrupción
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

    }

    public MaxForceOptimization(long tireRadius, long containerWidth, long containerHeight,
            long distBorder, long distTire) {
        this(tireRadius, containerWidth, containerHeight, distBorder, distTire, 1_000_000); // Generalmente 10_000_000
    }

    public static PhysicTire generateRandomTire(long tireRadius, long containerWidth, long containerHeight,
            long distBorder, long distTire) {
        long randomX = (long) (Math.random() * (containerWidth - 2 * distBorder)
                + distBorder);
        long randomY = (long) (Math.random() * (containerHeight - 2 * distBorder)
                + distBorder);
        return new PhysicTire("Random", tireRadius, randomX, randomY);
    }

    @Override
    public void setup() {
        isRunning.set(true);

        // Inicializar con optimizaciones básicas
        int bestInitialCount = runInitialOptimizations();
        int maxWheelCount = config.getMaxWheelCount();

        // Inicializar lista de resultados
        for (int i = 0; i < maxWheelCount; i++) {
            bestConfiguration.put(i, new ArrayList<>());
        }

        System.out.println("Iniciando simulaciones...");
        
        // Crear todas las simulaciones de una vez
        for (int i = bestInitialCount; i < maxWheelCount; i++) {
            final int simIndex = i - bestInitialCount;
            final int targetTireCount = i + 1;  // Número de ruedas para esta simulación
            
            // Crear una lista nueva para cada simulación
            List<PhysicTire> simulationTires = new ArrayList<>();
            for (int j = 0; j < targetTireCount; j++) {
                simulationTires.add(generateRandomTire(config.tireRadius, config.containerWidth, 
                    config.containerHeight, config.distBorder, config.distTire));
            }
            
            System.out.println("Simulación " + simIndex + " iniciada con " + simulationTires.size() + " ruedas");
            
            // Crear y ejecutar la simulación en su propio hilo
            final List<PhysicTire> finalTires = new ArrayList<>(simulationTires);
            executor.submit(() -> {
                try {
                    PhysicsEngine physicsEngine = new PhysicsEngine(config, finalTires);
                    runSimulation(physicsEngine, simIndex);
                } catch (InterruptedException e) {
                    System.err.println("Simulación " + simIndex + " interrumpida");
                    Thread.currentThread().interrupt();
                }
            });
        }
    }

    private int runInitialOptimizations() {
        var hexOpt = new HexagonalOptimization(config.tireRadius, config.containerWidth,
                config.containerHeight, config.distBorder, config.distTire);
        var squareOpt = new SquareGridOptimization(config.tireRadius, config.containerWidth,
                config.containerHeight, config.distBorder, config.distTire);

        hexOpt.setup();
        squareOpt.setup();
        hexOpt.run();
        squareOpt.run();

        return Math.max(hexOpt.getResult().size(), squareOpt.getResult().size());
    }

    private void runSimulation(PhysicsEngine physicsEngine, int simIndex) throws InterruptedException {
        try {
            while (isRunning.get() && !physicsEngine.isFinished() && !Thread.currentThread().isInterrupted()) {

                physicsEngine.updateIteration();
                physicsEngine.simulatePhysics();
                int currentValidTires = physicsEngine.countValidTires();
                synchronized (bestConfiguration) {
                    if (currentValidTires >= bestConfiguration.get(simIndex).size()) {
                        bestConfiguration.put(simIndex, physicsEngine.getTires().stream()
                                .map(PhysicTire::clone)
                                .collect(Collectors.toList()));
                    }
                }

            }

        } catch (Exception e) {
            System.out.println("Error en la simulación " + simIndex + ": " + e.getMessage());
        } finally {
            System.out.println("Simulación " + simIndex + " finalizada");
            synchronized (bestConfiguration) {
                try {
                    System.out.println("Resultado simulación " + simIndex + " " + bestConfiguration.get(simIndex).size()
                            + " ruedas");
                } catch (Exception e) {
                    System.out.println("No hay mejor configuración para la simulación " + simIndex);
                }
            }
        }

    }

    private int getBestValidTireCount() {
        synchronized (bestConfiguration) {
            int maxCount = 0;
            for (List<PhysicTire> tires : bestConfiguration.values()) {
                int validTires = (int) tires.stream()
                        .filter(tire -> PhysicTire.isValidTire(tire, config.containerWidth, config.containerHeight,
                                config.distBorder, tires, config.distTire))
                        .count();
                if (validTires > maxCount) {
                    maxCount = validTires;
                }
            }
            return maxCount;
        }
    }

    @Override
    public List<Tire> getResult() {
        synchronized (bestConfiguration) {
            // Find the configuration with the most valid tires and clone it
            List<Tire> result = bestConfiguration.values().stream()
                    .max((list1, list2) -> Integer.compare(
                            (int) list1.stream()
                                    .filter(t -> PhysicTire.isValidTire(t, config.containerWidth,
                                            config.containerHeight, config.distBorder, list1, config.distTire))
                                    .count(),
                            (int) list2.stream()
                                    .filter(t -> PhysicTire.isValidTire(t, config.containerWidth,
                                            config.containerHeight, config.distBorder, list2, config.distTire))
                                    .count()))
                    .map(tires -> tires.stream()
                            .map(tire -> (Tire) tire.clone())
                            .collect(Collectors.toList()))
                    .orElse(new ArrayList<>());
            return result;
        }
    }

    @Override
    public boolean isFinished() {
        if (executor.isShutdown()) {
            isRunning.set(false);
            return true;
        }
        return false;
    }

    @Override
    public void stop() {
        try {
            isRunning.set(false);

            // Apagar el executor service
            executor.shutdownNow();
            if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                System.err.println("El executor no se cerró correctamente");
            }

            // Mostrar resultado final
            int finalBestCount = getBestValidTireCount();
            System.out.println("\n=== Resultado Final ===");
            System.out.println("Mejor cantidad de ruedas válidas: " + finalBestCount);
            System.out.println("====================");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Interrupción durante el cierre de la simulación");
        } finally {
            cleanupResources();
        }
    }

    private void cleanupResources() {
        try {
            isRunning.set(false);

            // Asegurar que el executor se cierre
            if (executor != null && !executor.isShutdown()) {
                executor.shutdownNow();
            }

            // Limpiar otras estructuras de datos
            bestConfiguration.clear();

        } catch (Exception e) {
            System.err.println("Error durante la limpieza de recursos: " + e.getMessage());
        }
    }

    private static class SimulationConfig {
        final long WALL_REPULSION_FORCE = 1_000_000;
        final long tireRadius;
        final long containerWidth;
        final long containerHeight;
        final long distBorder;
        final long distTire;
        final long maxIteration;

        final long REPULSION_FORCE = 100_000;
        final float DAMPING = 0.98f;
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

            return force;
        }

        private void addTireRepulsion(PhysicTire tire1, PhysicTire tire2, Vector2D force) {
            long dx = tire1.getX() - tire2.getX();
            long dy = tire1.getY() - tire2.getY();
            double dist = Math.sqrt(dx * dx + dy * dy);
            double minDist = (2.1 * config.tireRadius + config.distTire);
            
            if (dist > 0.0001 && dist < minDist) {
                double magnitude = config.REPULSION_FORCE * (minDist - dist) / minDist;
                
                force.x += (long)((dx / dist) * magnitude);
                force.y += (long)((dy / dist) * magnitude);
            }
        }

        private void addBorderForces(PhysicTire tire, Vector2D force) {
            long[] distances = {
                    tire.getX() - config.distBorder - config.tireRadius, // left
                    config.containerWidth - tire.getX() - config.distBorder - config.tireRadius, // right
                    tire.getY() - config.distBorder - config.tireRadius, // top
                    config.containerHeight - tire.getY() - config.distBorder - config.tireRadius // bottom
            };

            for (int i = 0; i < distances.length; i++) {
                if (distances[i] < config.distBorder) {
                    double borderForce = config.WALL_REPULSION_FORCE / Math.max(distances[i], 1);
                    if (i < 2)
                        force.x += i == 0 ? borderForce : -borderForce;
                    else
                        force.y += i == 2 ? borderForce : -borderForce;
                }
            }
        }

        private void updateTirePhysics(PhysicTire tire, Vector2D force) {
            double newSpeedX = tire.getCurrentSpeedX() * config.DAMPING + force.x * config.DT;
            double newSpeedY = tire.getCurrentSpeedY() * config.DAMPING + force.y * config.DT;
            
            tire.setCurrentSpeedX((long)newSpeedX);
            tire.setCurrentSpeedY((long)newSpeedY);
            
            long newX = tire.getX() + (long)(tire.getCurrentSpeedX() * config.DT);
            long newY = tire.getY() + (long)(tire.getCurrentSpeedY() * config.DT);
            
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

    private static class Vector2D {
        long x, y;
    }

}