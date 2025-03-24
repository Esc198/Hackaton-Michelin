package com.michelin.Optimization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.michelin.utils.PhysicTire;
import com.michelin.utils.Tire;

public class MaxForceOptimization implements AbstractOptimization {
    private final Config config;
    private final PhysicsEngine physicsEngine;
    private final SimulationState state;
    private Thread simulationThread;
    private volatile boolean isRunning;

    public MaxForceOptimization(float tireRadius, float containerWidth, float containerHeight, 
                              float distBorder, float distTire, float maxIteration, float minDelta) {
        this.config = new Config(tireRadius, containerWidth, containerHeight, 
                               distBorder, distTire, maxIteration, minDelta);
        this.physicsEngine = new PhysicsEngine(config);
        this.state = new SimulationState(config);
    }

    public MaxForceOptimization(float tireRadius, float containerWidth, float containerHeight, 
                              float distBorder, float distTire) {
        this(tireRadius, containerWidth, containerHeight, distBorder, distTire, 1000000, 1f);
    }

    @Override
    public void setup() {
        state.initialize();
        isRunning = true;
        
        simulationThread = new Thread(() -> {
            System.out.println("Iniciando simulación en hilo separado");
            while (isRunning && !isFinished()) {
                state.incrementIteration();
                physicsEngine.simulatePhysics(state.getTires());
                
            }
            System.out.println("Simulación finalizada:");
            System.out.println("Iteraciones totales: " + state.getIteration());
            System.out.println("Ruedas válidas finales: " + state.countValidTires(state.getTires()));
        });
        
        simulationThread.setName("SimulationThread");
        simulationThread.start();
    }

    @Override
    public void run() {
        // Este método ya no hace nada porque la simulación corre en otro hilo
    }

    @Override
    public List<Tire> getResult() {
        // Crear una copia segura del estado actual
        synchronized(state) {
            return new ArrayList<>(state.getTires());
        }
    }

    @Override
    public boolean isFinished() {
        if (state.getIteration() >= config.maxIteration ){
            stop();
            return true;
        }

        return false;
    }

    // Método para detener la simulación de forma segura
    public void stop() {
        isRunning = false;
        if (simulationThread != null) {
            try {
                simulationThread.join(1000); // Esperar hasta 1 segundo a que termine
            } catch (InterruptedException e) {
                System.out.println("Error al esperar a que termine la simulación");
                Thread.currentThread().interrupt();
            }
        }
    }

    // Clase de configuración
    private static class Config {
        final float tireRadius;
        final float containerWidth;
        final float containerHeight;
        final float distBorder;
        final float distTire;
        final float maxIteration;
        final float minDelta;

        Config(float tireRadius, float containerWidth, float containerHeight,
               float distBorder, float distTire, float maxIteration, float minDelta) {
            this.tireRadius = tireRadius;
            this.containerWidth = containerWidth;
            this.containerHeight = containerHeight;
            this.distBorder = distBorder;
            this.distTire = distTire;
            this.maxIteration = maxIteration;
            this.minDelta = minDelta;
        }
    }

    // Motor de física
    private static class PhysicsEngine {
        private final Config config;
        private static final float WALL_REPULSION_FORCE = 200;
        private static final float WHEEL_REPULSION_FORCE = 100;
        private static final float DAMPING_FACTOR = 0.95f;
        private static final float DT = 0.016f;
        private static final float MIN_SPEED = 0.01f;

        PhysicsEngine(Config config) {
            this.config = config;
        }

        void simulatePhysics(List<PhysicTire> tires) {
            
            // Calcular todas las fuerzas primero
            for (int i = 0; i < tires.size(); i++) {
                PhysicTire tire = tires.get(i);
                Forces forces = calculateForces(tire, tires);
                updateTirePhysics(tire, forces);
                
            }
            
        }



        private Forces calculateForces(PhysicTire tire1, List<PhysicTire> tires) {
            Forces forces = new Forces();
            
            // Amortiguación (Damping)
            // Aplica una fuerza opuesta al movimiento para simular fricción/resistencia
            // La fuerza es proporcional a la velocidad actual multiplicada por (1-DAMPING_FACTOR)
            // - Si DAMPING_FACTOR es 0, la amortiguación es máxima (la rueda se detiene rápidamente)
            // - Si DAMPING_FACTOR es 1, no hay amortiguación (la rueda mantiene su velocidad)
            forces.fx = -tire1.getCurrentSpeedX() * (1 - DAMPING_FACTOR);
            forces.fy = -tire1.getCurrentSpeedY() * (1 - DAMPING_FACTOR);
            
            // Fuerzas entre ruedas
            for (PhysicTire tire2 : tires) {
                if (tire1 == tire2) continue;
                
                float dx = tire1.getX() - tire2.getX();
                float dy = tire1.getY() - tire2.getY();
                float distance = (float)Math.sqrt(dx * dx + dy * dy);
                
                // Fuerza base inversamente proporcional a la distancia
                float forceMagnitude = WHEEL_REPULSION_FORCE / distance;
                
                float nx = dx / distance;
                float ny = dy / distance;
                forces.fx += nx * forceMagnitude;
                forces.fy += ny * forceMagnitude;
            }
            
            
            // Fuerzas de los bordes más fuertes
            addBoundaryForces(forces, tire1);
            
            return forces;
        }

        private void addBoundaryForces(Forces forces, PhysicTire tire) {
            float x = tire.getX();
            float y = tire.getY();
            float r = config.tireRadius;

            float distance=0;
            // Aplicar fuerzas más fuertes cerca de los bordes
            if ( (distance = config.distBorder - (x - r)) > 0) {
                forces.fx += WALL_REPULSION_FORCE * (1.0f + Math.abs( 1/distance ));
            }
            if ( (distance = config.containerWidth - config.distBorder - (x + r)) > 0) {
                forces.fx -= WALL_REPULSION_FORCE * (1.0f + Math.abs( 1/distance ));
            }
            if ( (distance = config.distBorder - (y - r)) > 0) {
                forces.fy += WALL_REPULSION_FORCE * (1.0f + Math.abs( 1/distance ));
            }
            if ( (distance = config.containerHeight - config.distBorder - (y + r)) > 0) {
                forces.fy -= WALL_REPULSION_FORCE * (1.0f + Math.abs( 1/distance ));
            }
        }

        private void updateTirePhysics(PhysicTire tire, Forces forces) {
            // Limitar fuerzas antes de actualizar
            limitForces(forces, tire.getMaxForce());
            
            // Actualizar aceleración
            tire.setCurrentAccelerationX(forces.fx / tire.getMaxForce() * tire.getMaxAcceleration());
            tire.setCurrentAccelerationY(forces.fy / tire.getMaxForce() * tire.getMaxAcceleration());
            
            // Actualizar velocidad con más amortiguación
            float newSpeedX = tire.getCurrentSpeedX() * DAMPING_FACTOR + tire.getCurrentAccelerationX() * DT;
            float newSpeedY = tire.getCurrentSpeedY() * DAMPING_FACTOR + tire.getCurrentAccelerationY() * DT;
            
            // Detener si la velocidad es muy baja
            if (Math.abs(newSpeedX) < MIN_SPEED && Math.abs(newSpeedY) < MIN_SPEED) {
                newSpeedX = newSpeedY = 0;
            }
            
            // Limitar velocidad
            limitSpeed(tire, newSpeedX, newSpeedY);
            
            // Actualizar posición
            float newX = tire.getX() + tire.getCurrentSpeedX() * DT;
            float newY = tire.getY() + tire.getCurrentSpeedY() * DT;
            
            // Asegurar que las ruedas no salgan del contenedor
            newX = Math.max(config.distBorder + config.tireRadius, 
                          Math.min(config.containerWidth - config.distBorder - config.tireRadius, newX));
            newY = Math.max(config.distBorder + config.tireRadius, 
                          Math.min(config.containerHeight - config.distBorder - config.tireRadius, newY));
            
            tire.setX(newX);
            tire.setY(newY);
        }

        private void limitSpeed(PhysicTire tire, float speedX, float speedY) {
            float speedMagnitude = (float)Math.sqrt(speedX * speedX + speedY * speedY);
            if (speedMagnitude > tire.getMaxSpeed()) {
                float scale = tire.getMaxSpeed() / speedMagnitude;
                tire.setCurrentSpeedX(speedX * scale);
                tire.setCurrentSpeedY(speedY * scale);
            } else {
                tire.setCurrentSpeedX(speedX);
                tire.setCurrentSpeedY(speedY);
            }
        }

        private void limitForces(Forces forces, float maxForce) {
            float magnitude = (float)Math.sqrt(forces.fx * forces.fx + forces.fy * forces.fy);
            if (magnitude > maxForce) {
                float scale = maxForce / magnitude;
                forces.fx *= scale;
                forces.fy *= scale;
            }
        }
    }

    // Clase auxiliar para fuerzas
    private static class Forces {
        float fx = 0;
        float fy = 0;
    }

    // Estado de la simulación
    private static class SimulationState {
        private final Config config;
        private final List<PhysicTire> tires;
        private volatile int iteration;
        private List<PhysicTire> bestConfiguration;
        private int bestValidTireCount;
        private boolean testsInProgress;
        private final List<RemovalTest> activeTests;
        

        SimulationState(Config config) {
            this.config = config;
            this.tires = new ArrayList<>();
            this.bestConfiguration = new ArrayList<>();
            this.bestValidTireCount = 0;
            this.testsInProgress = false;
            this.activeTests = new ArrayList<>();
        }   

        synchronized void initialize() {
            tires.clear();
            iteration = 0;
            
            int tireCount = (int) ((config.containerWidth - config.distBorder * 2) * 
                                 (config.containerHeight - config.distBorder * 2) /
                                 (Math.PI * (config.tireRadius + config.distTire) * 
                                          (config.tireRadius + config.distTire)));
            
            System.out.println("Iniciando simulación con " + tireCount + " ruedas");
            
            for (int i = 0; i < tireCount; i++) {
                float randomX = config.distBorder + config.tireRadius + 
                              (float)(Math.random() * (config.containerWidth - 
                                     2 * (config.distBorder + config.tireRadius)));
                float randomY = config.distBorder + config.tireRadius + 
                              (float)(Math.random() * (config.containerHeight - 
                                     2 * (config.distBorder + config.tireRadius)));
                tires.add(new PhysicTire(100, 100, 100, 100, "tire" + i, 
                                       config.tireRadius, randomX, randomY));
            }
        }

        synchronized List<PhysicTire> getTires() { 
            return new ArrayList<>(tires); 
        }
        
        int getIteration() { return iteration; }
        
        synchronized void incrementIteration() { iteration++; }

        private synchronized void startRemovalTests() {
            if (testsInProgress) {
                return;
            }
            
            activeTests.clear();
            testsInProgress = true;
            
            int currentValidTires = countValidTires(tires);
            bestValidTireCount = currentValidTires;
            
            System.out.println("\n----------------------------------------");
            System.out.println("INICIANDO EVALUACIÓN DE ELIMINACIÓN DE RUEDAS");
            System.out.println("Estado actual:");
            System.out.printf("- Iteración: %,d%n", iteration);
            System.out.printf("- Ruedas totales: %d%n", tires.size());
            System.out.printf("- Ruedas válidas: %d%n", currentValidTires);
            System.out.println("----------------------------------------\n");
            
            List<PhysicTire> problematicTires = findProblematicTires();
            if (problematicTires.isEmpty()) {
                System.out.println("No se encontraron ruedas problemáticas para eliminar.");
                testsInProgress = false;
                return;
            }
            
            System.out.println("Ruedas candidatas para eliminación:");
            for (int i = 0; i < problematicTires.size(); i++) {
                PhysicTire tire = problematicTires.get(i);
                float overlapScore = calculateOverlapScore(tire);
                float boundaryPenalty = calculateBoundaryPenalty(tire);
                System.out.printf("%d. Rueda %s:%n", i + 1, tire.getModel());
                System.out.printf("   - Score de solapamiento: %.3f%n", overlapScore);
                System.out.printf("   - Penalización de borde: %.3f%n", boundaryPenalty);
                System.out.printf("   - Score total: %.3f%n", overlapScore + boundaryPenalty);
            }
            System.out.println();
            
            for (PhysicTire tire : problematicTires) {
                RemovalTest test = new RemovalTest(new ArrayList<>(tires), tire);
                activeTests.add(test);
                test.start();
            }
        }

        private List<PhysicTire> findProblematicTires() {
            Map<PhysicTire, Float> overlapScores = new HashMap<>();
            
            // Calcular puntuación de solapamiento para cada rueda
            for (PhysicTire tire : tires) {
                float overlapScore = calculateOverlapScore(tire);
                float boundaryPenalty = calculateBoundaryPenalty(tire);
                float totalScore = overlapScore + boundaryPenalty;
                
                // Solo considerar ruedas con problemas significativos
                if (totalScore > 0.1f) {  // Umbral mínimo para considerar una rueda como problemática
                    overlapScores.put(tire, totalScore);
                }
            }
            
            // Seleccionar hasta 3 ruedas más problemáticas
            return overlapScores.entrySet().stream()
                .sorted(Map.Entry.<PhysicTire, Float>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        }

        private float calculateOverlapScore(PhysicTire tire) {
            float score = 0;
            for (PhysicTire other : tires) {
                if (tire == other) continue;
                float distance = calculateDistance(tire, other);
                float minDistance = config.tireRadius * 2 + config.distTire;
                if (distance < minDistance) {
                    score += (minDistance - distance) / minDistance;
                }
            }
            return score;
        }

        private float calculateDistance(PhysicTire t1, PhysicTire t2) {
            float dx = t1.getX() - t2.getX();
            float dy = t1.getY() - t2.getY();
            return (float) Math.sqrt(dx * dx + dy * dy);
        }

        private int countValidTires(List<PhysicTire> tireList) {
            int count = 0;
            for (PhysicTire tire : tireList) {
                boolean isValid = true;
                // Verificar colisiones con otras ruedas
                for (PhysicTire other : tireList) {
                    if (tire == other) continue;
                    float distance = calculateDistance(tire, other);
                    if (distance < config.tireRadius * 2 + config.distTire) {
                        isValid = false;
                        break;
                    }
                }
                // Verificar límites del contenedor
                if (isValid && isWithinBounds(tire)) {
                    count++;
                }
            }
            return count;
        }

        private boolean isWithinBounds(PhysicTire tire) {
            float x = tire.getX();
            float y = tire.getY();
            float r = config.tireRadius;
            return x - r >= config.distBorder &&
                   x + r <= config.containerWidth - config.distBorder &&
                   y - r >= config.distBorder &&
                   y + r <= config.containerHeight - config.distBorder;
        }

        private float calculateBoundaryPenalty(PhysicTire tire) {
            float x = tire.getX();
            float y = tire.getY();
            float r = config.tireRadius;
            float penalty = 0;
            
            // Penalizar ruedas cerca de los bordes
            if (x - r < config.distBorder * 1.5f) {
                penalty += (config.distBorder * 1.5f - (x - r)) / config.distBorder;
            }
            if (x + r > config.containerWidth - config.distBorder * 1.5f) {
                penalty += ((x + r) - (config.containerWidth - config.distBorder * 1.5f)) / config.distBorder;
            }
            if (y - r < config.distBorder * 1.5f) {
                penalty += (config.distBorder * 1.5f - (y - r)) / config.distBorder;
            }
            if (y + r > config.containerHeight - config.distBorder * 1.5f) {
                penalty += ((y + r) - (config.containerHeight - config.distBorder * 1.5f)) / config.distBorder;
            }
            
            return penalty;
        }

        private class RemovalTest extends Thread {
            private final List<PhysicTire> testTires;
            private final PhysicTire removedTire;
            private int validTireCount;
            private static final int TEST_ITERATIONS = 10000;
            private List<PhysicTire> finalConfiguration;

            RemovalTest(List<PhysicTire> originalTires, PhysicTire tireToRemove) {
                this.testTires = new ArrayList<>(originalTires); // Hacer copia explícita
                this.removedTire = tireToRemove;
                this.testTires.remove(tireToRemove);
                setName("RemovalTest-" + removedTire.getModel());
            }

            @Override
            public void run() {
                PhysicsEngine testEngine = new PhysicsEngine(config);
                List<PhysicTire> bestTestConfiguration = null;
                
                System.out.printf("\nIniciando prueba para %s:%n", removedTire.getModel());
                System.out.println("- Configuración inicial: " + testTires.size() + " ruedas");
                
                // Ejecutar simulación de prueba
                for (int i = 0; i < TEST_ITERATIONS; i++) {
                    testEngine.simulatePhysics(testTires);
                    
                    // Guardar la mejor configuración encontrada durante la prueba
                    int currentValidCount = countValidTires(testTires);
                    if (currentValidCount > validTireCount || 
                        (currentValidCount == validTireCount )) {
                        validTireCount = currentValidCount;
                        bestTestConfiguration = new ArrayList<>(testTires);
                        
                        if (i % 1000 == 0) {
                            System.out.printf("  Iteración %d: %d ruedas válidas", 
                                i, currentValidCount);
                        }
                    }
                }
                
                // Usar la mejor configuración encontrada
                if (bestTestConfiguration != null) {
                    testTires.clear();
                    testTires.addAll(bestTestConfiguration);
                }
                
                finalConfiguration = new ArrayList<>(testTires);
                
                synchronized (SimulationState.this) {
                    System.out.printf("\nResultados de prueba para %s:%n", removedTire.getModel());
                    System.out.printf("- Ruedas válidas: %d%n", validTireCount);
                    
                    if (validTireCount > bestValidTireCount) {
                        bestValidTireCount = validTireCount;
                        bestConfiguration = new ArrayList<>(finalConfiguration);
                        System.out.println("¡MEJOR CONFIGURACIÓN ENCONTRADA!");
                    }
                    
                    checkTestsCompletion();
                }
            }
        }

        private synchronized void checkTestsCompletion() {
            boolean allTestsComplete = activeTests.stream()
                .allMatch(test -> !test.isAlive());
                
            if (allTestsComplete && testsInProgress) {
                testsInProgress = false;
                
                System.out.println("\n----------------------------------------");
                System.out.println("RESULTADOS DE LA EVALUACIÓN:");
                
                if (bestConfiguration != null && !bestConfiguration.isEmpty() && 
                    bestValidTireCount > countValidTires(tires)) {
                    
                    int previousValid = countValidTires(tires);
                    System.out.println("SE APLICARÁ NUEVA CONFIGURACIÓN");
                    System.out.println("Motivos:");
                    System.out.printf("- Ruedas válidas anteriores: %d%n", previousValid);
                    System.out.printf("- Ruedas válidas nuevas: %d%n", bestValidTireCount);
                    System.out.printf("- Mejora neta: %d ruedas%n", bestValidTireCount - previousValid);

                    
                    tires.clear();
                    tires.addAll(bestConfiguration);
                } else {
                    System.out.println("NO SE APLICARÁN CAMBIOS");
                    System.out.println("Motivos:");
                    if (bestConfiguration == null || bestConfiguration.isEmpty()) {
                        System.out.println("- No se encontró ninguna configuración válida");
                    } else {
                        System.out.printf("- Ruedas válidas actuales: %d%n", countValidTires(tires));
                        System.out.printf("- Mejor resultado encontrado: %d ruedas%n", bestValidTireCount);
                        System.out.println("- No hay mejora significativa");
                    }
                }
                System.out.println("----------------------------------------\n");
            
            }
        }

    }
}