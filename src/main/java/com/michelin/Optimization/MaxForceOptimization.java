package com.michelin.Optimization;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.michelin.utils.PhysicTire;
import com.michelin.utils.Tire;

public class MaxForceOptimization implements AbstractOptimization {
    private final SimulationConfig config;
    private final PhysicsEngine physicsEngine;
    private final SimulationState state;
    private final AtomicBoolean isRunning;
    private final ExecutorService executor;
    private final List<Integer> validTireCounts;
    private List<PhysicTire> bestConfiguration = new ArrayList<>();
    private final List<Thread> activeThreads;
    private volatile boolean shutdownRequested;

    public MaxForceOptimization(float tireRadius, float containerWidth, float containerHeight, 
                              float distBorder, float distTire, float maxIteration) {
        this.config = new SimulationConfig(tireRadius, containerWidth, containerHeight, 
                               distBorder, distTire, maxIteration);
        this.physicsEngine = new PhysicsEngine(config);
        this.state = new SimulationState(config);
        this.isRunning = new AtomicBoolean(false);
        this.executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        this.validTireCounts = new ArrayList<>();
        this.activeThreads = new ArrayList<>();
        this.shutdownRequested = false;
        
        // Agregar shutdown hook para limpieza en caso de interrupción
        Runtime.getRuntime().addShutdownHook(new Thread(this::cleanupResources));
    }

    public MaxForceOptimization(float tireRadius, float containerWidth, float containerHeight, 
                              float distBorder, float distTire) {
        this(tireRadius, containerWidth, containerHeight, distBorder, distTire, 1000000);
    }

    @Override
    public void setup() {
        state.initialize();
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

        // Iniciar simulaciones
        synchronized(activeThreads) {
            activeThreads.clear();
            for (int i = 0; i < maxWheelCount; i++) {
                final int simIndex = i;
                final SimulationState simState = state.clone(); // Cada simulación tiene su propio estado
                
                Thread simulationThread = new Thread(() -> {
                    try {
                        runSimulation(simIndex, simState);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        System.out.println("Simulación " + simIndex + " interrumpida");
                    }
                });
                simulationThread.setName("Simulation-" + i);
                activeThreads.add(simulationThread);
                simulationThread.start();
            }
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

    private void runSimulation(int simIndex, SimulationState simState) throws InterruptedException {
        while (isRunning.get() && !isFinished() && !Thread.currentThread().isInterrupted()) {
            if (shutdownRequested) {
                break;
            }
            
            simState.incrementIteration();
            physicsEngine.simulatePhysics(simState.getTires());
            int currentValidTires = simState.countValidTires();
            
            synchronized(validTireCounts) {
                validTireCounts.set(simIndex, currentValidTires);
                if (currentValidTires > getBestValidTireCount()) {
                    bestConfiguration = new ArrayList<>(simState.getTires());
                    System.out.printf("Nueva mejor configuración encontrada: %d ruedas válidas%n", 
                                    currentValidTires);
                }
                if (simState.getIteration() % 1000 == 0) {
                    System.out.printf("Simulación %d: %d ruedas válidas (Mejor hasta ahora: %d)%n", 
                                    simIndex, currentValidTires, getBestValidTireCount());
                }
            }
            
            Thread.sleep(1);
        }
    }

    private int getBestValidTireCount() {
        int maxCount = 0;
        for (int count : validTireCounts) {
            if (count > maxCount) {
                maxCount = count;
            }
        }
        return maxCount;
    }

    @Override
    public void run() {
        // La simulación corre en hilos separados
    }

    @Override
    public List<Tire> getResult() {
        synchronized(validTireCounts) {
            // Si no hay mejor configuración guardada, devolver la actual
            if (bestConfiguration.isEmpty()) {
                return new ArrayList<>(state.getTires());
            }
            // Devolver la mejor configuración encontrada
            return new ArrayList<>(bestConfiguration);
        }
    }

    @Override
    public boolean isFinished() {
        if (state.getIteration() >= config.maxIteration) {
            System.out.println("Terminado");
                return true;
            }
            return false;
    }

    public void stop() {
        try {
            shutdownRequested = true;
            isRunning.set(false);
            
            // Interrumpir todos los hilos activos
            synchronized(activeThreads) {
                for (Thread thread : activeThreads) {
                    if (thread != null && thread.isAlive()) {
                        thread.interrupt();
                    }
                }
            }
            
            // Esperar a que todos los hilos terminen (con timeout)
            long timeout = System.currentTimeMillis() + 5000; // 5 segundos de timeout
            synchronized(activeThreads) {
                for (Thread thread : activeThreads) {
                    if (thread != null && thread.isAlive()) {
                        long remainingTime = timeout - System.currentTimeMillis();
                        if (remainingTime > 0) {
                            thread.join(remainingTime);
                        }
                    }
                }
            }
            
            // Apagar el executor service
            executor.shutdownNow();
            if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                System.err.println("El executor no se cerró correctamente");
            }
            
            // Mostrar resultado final
            int finalBestCount = getBestValidTireCount();
            System.out.println("\n=== Resultado Final ===");
            System.out.println("Mejor cantidad de ruedas válidas: " + finalBestCount);
            System.out.println("Iteraciones totales: " + state.getIteration());
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
            
            // Interrumpir hilos activos
            synchronized(activeThreads) {
                for (Thread thread : activeThreads) {
                    if (thread != null && thread.isAlive()) {
                        thread.interrupt();
                    }
                }
                activeThreads.clear();
            }
            
            // Limpiar otras estructuras de datos
            validTireCounts.clear();
            bestConfiguration.clear();
            
        } catch (Exception e) {
            System.err.println("Error durante la limpieza de recursos: " + e.getMessage());
        }
    }

    private static class SimulationConfig {
        final float tireRadius;
        final float containerWidth;
        final float containerHeight;
        final float distBorder;
        final float distTire;
        final float maxIteration;

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
        private static final float REPULSION_FORCE = 10f;
        private static final float DAMPING = 0.98f;
        private static final float DT = 0.008f;
        private static final float MIN_SPEED = 0.00001f;
        private final SimulationConfig config;

        PhysicsEngine(SimulationConfig config) {
            this.config = config;
        }

        void simulatePhysics(List<PhysicTire> tires) {
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
            float minDist = 2 * config.tireRadius + config.distTire;

            if (dist < minDist && dist > 0.0001f) {
                float overlap = minDist - dist;
                float magnitude = REPULSION_FORCE * (overlap / minDist);
                force.x += (dx / dist) * magnitude;
                force.y += (dy / dist) * magnitude;
            }
        }

        private void addBorderForces(PhysicTire tire, Vector2D force) {
            float[] distances = {
                tire.getX() - config.distBorder - config.tireRadius,                    // left
                config.containerWidth - tire.getX() - config.distBorder - config.tireRadius,  // right
                tire.getY() - config.distBorder - config.tireRadius,                    // top
                config.containerHeight - tire.getY() - config.distBorder - config.tireRadius  // bottom
            };

            for (int i = 0; i < distances.length; i++) {
                if (distances[i] < config.distBorder) {
                    float borderForce = REPULSION_FORCE / Math.max(distances[i], 0.0001f);
                    if (i < 2) force.x += i == 0 ? borderForce : -borderForce;
                    else force.y += i == 2 ? borderForce : -borderForce;
                }
            }
        }

        private void updateTirePhysics(PhysicTire tire, Vector2D force) {
            // Actualizar velocidad
            tire.setCurrentSpeedX(tire.getCurrentSpeedX() * DAMPING + force.x * DT);
            tire.setCurrentSpeedY(tire.getCurrentSpeedY() * DAMPING + force.y * DT);

            // Aplicar velocidad mínima
            if (Math.abs(tire.getCurrentSpeedX()) < MIN_SPEED) tire.setCurrentSpeedX(0);
            if (Math.abs(tire.getCurrentSpeedY()) < MIN_SPEED) tire.setCurrentSpeedY(0);
            
            // Actualizar posición
            float newX = tire.getX() + tire.getCurrentSpeedX() * DT;
            float newY = tire.getY() + tire.getCurrentSpeedY() * DT;
            
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
    }

    private static class Vector2D {
        float x, y;
    }

    private static class SimulationState {
        private final SimulationConfig config;
        private final List<PhysicTire> tires;
        private final AtomicInteger iteration;

        SimulationState(SimulationConfig config) {
            this.config = config;
            this.tires = new ArrayList<>();
            this.iteration = new AtomicInteger(0);
        }

        void initialize() {
            tires.clear();
            iteration.set(0);
            
            int tireCount = config.getMaxWheelCount();
            float effectiveWidth = config.containerWidth - 2 * config.distBorder;
            float effectiveHeight = config.containerHeight - 2 * config.distBorder;
            
            // Distribuir las ruedas en una cuadrícula inicial aproximada
            int cols = (int) Math.sqrt(tireCount);
            int rows = (tireCount + cols - 1) / cols;
            
            float dx = effectiveWidth / cols;
            float dy = effectiveHeight / rows;
            
            int count = 0;
            for (int i = 0; i < rows && count < tireCount; i++) {
                for (int j = 0; j < cols && count < tireCount; j++) {
                    float x = config.distBorder + config.tireRadius + j * dx + 
                             (float)(Math.random() * dx * 0.5 - dx * 0.25);
                    float y = config.distBorder + config.tireRadius + i * dy + 
                             (float)(Math.random() * dy * 0.5 - dy * 0.25);
                    
                    tires.add(new PhysicTire(100, 100, 100, 100, 
                             "tire" + count, config.tireRadius, x, y));
                    count++;
                }
            }
        }

        List<PhysicTire> getTires() {
            return new ArrayList<>(tires); 
        }
        
        int getIteration() {
            return iteration.get();
        }

        void incrementIteration() {
            iteration.incrementAndGet();
        }

        int countValidTires() {
            return (int) tires.stream()
                            .filter(this::isValidTire)
                            .count();
        }

        private boolean isValidTire(PhysicTire tire) {
            return isWithinBounds(tire) && !hasCollisions(tire);
        }

        private boolean isWithinBounds(PhysicTire tire) {
            return tire.getX() - config.tireRadius >= config.distBorder &&
                   tire.getX() + config.tireRadius <= config.containerWidth - config.distBorder &&
                   tire.getY() - config.tireRadius >= config.distBorder &&
                   tire.getY() + config.tireRadius <= config.containerHeight - config.distBorder;
        }

        private boolean hasCollisions(PhysicTire tire) {
            return tires.stream()
                       .filter(other -> other != tire)
                       .anyMatch(other -> calculateDistance(tire, other) < 
                                        2 * config.tireRadius + config.distTire);
        }

        private float calculateDistance(PhysicTire t1, PhysicTire t2) {
            float dx = t1.getX() - t2.getX();
            float dy = t1.getY() - t2.getY();
            return (float) Math.sqrt(dx * dx + dy * dy);
        }

        public SimulationState clone() {
            SimulationState newState = new SimulationState(this.config);
            newState.iteration.set(this.iteration.get());
            for (PhysicTire tire : this.tires) {
                newState.tires.add(new PhysicTire(
                    tire.getMaxAcceleration(),
                    tire.getMaxDeceleration(),
                    tire.getMaxForce(),
                    tire.getMaxSpeed(),
                    tire.getModel(),
                    tire.getRadius(),
                    tire.getX(),
                    tire.getY()
                ));
            }
            return newState;
        }
    }
}