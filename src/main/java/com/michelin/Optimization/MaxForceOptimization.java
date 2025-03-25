package com.michelin.Optimization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
    private final List<Integer> validTireCounts;
    private Map<Integer, List<PhysicTire>> bestConfiguration = new HashMap<>();
    private volatile boolean shutdownRequested;
    private final SimulationProgress progress;

    public MaxForceOptimization(float tireRadius, float containerWidth, float containerHeight,
            float distBorder, float distTire, float maxIteration) {
        this.config = new SimulationConfig(tireRadius, containerWidth, containerHeight,
                distBorder, distTire, maxIteration);
        this.isRunning = new AtomicBoolean(false);
        this.executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        this.validTireCounts = new ArrayList<>();
        this.shutdownRequested = false;
        this.progress = new SimulationProgress();

        // Agregar shutdown hook para limpieza en caso de interrupción
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

    }

    public MaxForceOptimization(float tireRadius, float containerWidth, float containerHeight,
            float distBorder, float distTire) {
        this(tireRadius, containerWidth, containerHeight, distBorder, distTire, 10_000_000);
    }

    @Override
    public void setup() {
        isRunning.set(true);
        shutdownRequested = false;

        // Inicializar con optimizaciones básicas
        int bestInitialCount = runInitialOptimizations();

        // Preparar lista de conteos
        int maxWheelCount = config.getMaxWheelCount();
        validTireCounts.clear();
        for (int i = 0; i < maxWheelCount; i++) {
            validTireCounts.add(i == 0 ? bestInitialCount : 0);
        }
        List<PhysicTire> initialTires = new ArrayList<>();
        for (int i = 0; i < bestInitialCount; i++) {
            float randomX = (float) (Math.random() * (config.containerWidth - 2 * config.distBorder) + config.distBorder);
            float randomY = (float) (Math.random() * (config.containerHeight - 2 * config.distBorder) + config.distBorder);
            initialTires.add(new PhysicTire(randomX, randomY, 0, 0, "" + i, config.tireRadius, i, config.tireRadius));
        }
        
        System.out.println("Iniciando simulaciones...");
        // Iniciar simulaciones
        for (int i = bestInitialCount; i < maxWheelCount; i++) {
            final int simIndex = i - bestInitialCount;

            float randomX = (float) (Math.random() * (config.containerWidth - 2 * config.distBorder) + config.distBorder);
            float randomY = (float) (Math.random() * (config.containerHeight - 2 * config.distBorder) + config.distBorder);
            initialTires.add(new PhysicTire(randomX, randomY, 0, 0, "" + i, config.tireRadius, i, config.tireRadius));
            System.out.println("Simulación " + simIndex + " iniciada con " + initialTires.size() + " ruedas");
            executor.submit(() -> {
                try {
                    PhysicsEngine physicsEngine = new PhysicsEngine(config, initialTires);
                    runSimulation(physicsEngine, simIndex);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("Simulación " + simIndex + " interrumpida");
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
        while (isRunning.get() && !shutdownRequested && physicsEngine.getIteration() < config.maxIteration && !physicsEngine.isFinished()
                && !Thread.currentThread().isInterrupted()) {
            if (shutdownRequested) {
                break;
            }
            physicsEngine.updateIteration();
            physicsEngine.simulatePhysics();
            int currentValidTires = physicsEngine.countValidTires();
            synchronized (bestConfiguration) {
                if (currentValidTires > validTireCounts.get(simIndex)) {
                    validTireCounts.set(simIndex, currentValidTires);
                    bestConfiguration.put(simIndex, new ArrayList<>(physicsEngine.getTires()));
                }
            }

            // Actualizar progreso
            progress.updateSimulationProgress(simIndex, 
                (float) physicsEngine.getIteration() / config.maxIteration);

        }
        System.out.println("Simulación " + simIndex + " finalizada");

        
    }

    private int getBestValidTireCount() {
        synchronized (bestConfiguration) {      
            int maxCount = 0;
            for (List<PhysicTire> tires : bestConfiguration.values()) {
                int validTires = (int) tires.stream()
                    .filter(tire -> !isOverlapping(tire, tires))
                                        .count();
                                    if (validTires > maxCount) {
                                        maxCount = validTires;
                                    }
                                }
                                return maxCount;
                            }
                        }
                    

                    
                        @Override
    public void run() {
        // System.out.println(getStatus());
    }

    
    private boolean isOverlapping(PhysicTire tire, List<PhysicTire> tires) {
        return tires.stream().anyMatch(other -> other != tire && isOverlapping(tire, other));

    }

    private boolean isOverlapping(PhysicTire tire1, PhysicTire tire2) {
        return Math.sqrt(Math.pow(tire1.getX() - tire2.getX(), 2) + Math.pow(tire1.getY() - tire2.getY(), 2)) < 2 * config.tireRadius + config.distTire;
    }

    @Override
    public List<Tire> getResult() {
        synchronized (bestConfiguration) {
            // Crear una copia profunda de la mejor configuración
            List<Tire> result = bestConfiguration.values().stream()
                .flatMap(List::stream)
                .map(tire -> tire.clone())
                .collect(Collectors.toList());
            
            // Si no hay ruedas, usar la configuración de la simulación 1
            if (result.isEmpty() && bestConfiguration.containsKey(0)) {
                result = bestConfiguration.get(0).stream()
                    .map(tire -> tire.clone())
                    .collect(Collectors.toList());
            }
            
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
            shutdownRequested = true;
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
            shutdownRequested = true;
            isRunning.set(false);

            // Asegurar que el executor se cierre
            if (executor != null && !executor.isShutdown()) {
                executor.shutdownNow();
            }

           
            // Limpiar otras estructuras de datos
            validTireCounts.clear();
            bestConfiguration.clear();

        } catch (Exception e) {
            System.err.println("Error durante la limpieza de recursos: " + e.getMessage());
        }
    }

    // Método para obtener el progreso actual
    public SimulationStatus getStatus() {
        return progress.getStatus();
    }
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


    // Clase interna para manejar el progreso
    private static class SimulationProgress {
        private final Map<Integer, Float> simulationProgress = new ConcurrentHashMap<>();
        private volatile int bestTireCount = 0;
        private volatile int bestSimulationIndex = -1;
        private volatile long bestIterationFound = 0;
        private volatile long startTime = System.currentTimeMillis();

        public void updateSimulationProgress(int simIndex, float progress) {
            simulationProgress.put(simIndex, progress);
        }

        public void updateBestConfiguration(int tireCount, int iteration, int simIndex) {
            this.bestTireCount = tireCount;
            this.bestIterationFound = iteration;
            this.bestSimulationIndex = simIndex;
        }

        public SimulationStatus getStatus() {
            float averageProgress = (float) simulationProgress.values().stream()
                .mapToDouble(Float::doubleValue)
                .average()
                .orElse(0.0);

            return new SimulationStatus(
                averageProgress,
                bestTireCount,
                bestSimulationIndex,
                bestIterationFound,
                System.currentTimeMillis() - startTime,
                simulationProgress.size()
            );
        }
    }

    // Clase para representar el estado actual de la simulación
    public static class SimulationStatus {
        private final float overallProgress;
        private final int bestTireCount;
        private final int bestSimulationIndex;
        private final long bestIterationFound;
        private final long elapsedTimeMs;
        private final int activeSimulations;

        public SimulationStatus(float overallProgress, int bestTireCount, 
                              int bestSimulationIndex, long bestIterationFound,
                              long elapsedTimeMs, int activeSimulations) {
            this.overallProgress = overallProgress;
            this.bestTireCount = bestTireCount;
            this.bestSimulationIndex = bestSimulationIndex;
            this.bestIterationFound = bestIterationFound;
            this.elapsedTimeMs = elapsedTimeMs;
            this.activeSimulations = activeSimulations;
        }

        // Getters
        public float getOverallProgress() { return overallProgress; }
        public int getBestTireCount() { return bestTireCount; }
        public int getBestSimulationIndex() { return bestSimulationIndex; }
        public long getBestIterationFound() { return bestIterationFound; }
        public long getElapsedTimeMs() { return elapsedTimeMs; }
        public int getActiveSimulations() { return activeSimulations; }

        @Override
        public String toString() {
            return String.format(
                "Progreso: %.2f%%, Mejor configuración: %d ruedas (Sim #%d, It #%d), " +
                "Tiempo transcurrido: %.2f s, Simulaciones activas: %d",
                overallProgress * 100,
                bestTireCount,
                bestSimulationIndex,
                bestIterationFound,
                elapsedTimeMs / 1000.0,
                activeSimulations
            );
        }
    }
}