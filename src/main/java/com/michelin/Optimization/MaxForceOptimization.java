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
        this(tireRadius, containerWidth, containerHeight, distBorder, distTire, 1000000, 0.0001f);
    }

    @Override
    public void setup() {
        state.initialize();
        isRunning = true;
        
        simulationThread = new Thread(() -> {
            try {
                System.out.println("Iniciando simulación en hilo separado");
                while (isRunning && !isFinished()) {
                    state.incrementIteration();
                    float newDelta = physicsEngine.simulatePhysics(state.getTires());
                    state.setDelta(newDelta);
                    
                    // Verificar estancamiento
                    state.checkForStagnation();
                    
                    // Añadir un pequeño sleep para no saturar el CPU
                    if (state.getIteration() % 100 == 0) {
                        Thread.sleep(1);
                    }
                }
                System.out.println("Simulación finalizada:");
                System.out.println("Iteraciones totales: " + state.getIteration());
                System.out.println("Delta final: " + state.getDelta());
                System.out.println("Ruedas válidas finales: " + state.countValidTires(state.getTires()));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Simulación interrumpida");
            }
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
        return (state.getIteration() >= config.maxIteration) || 
               (state.getIteration() > 10000 && state.getDelta() < config.minDelta);
    }

    // Método para detener la simulación de forma segura
    public void stop() {
        isRunning = false;
        if (simulationThread != null) {
            try {
                simulationThread.join(1000); // Esperar hasta 1 segundo a que termine
            } catch (InterruptedException e) {
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
        private static final float DAMPING_FACTOR = 0.95f;
        private static final float DT = 0.016f;
        private static final float MIN_SPEED = 0.0001f;

        PhysicsEngine(Config config) {
            this.config = config;
        }

        float simulatePhysics(List<PhysicTire> tires) {
            float maxDelta = 0;
            
            // Calcular todas las fuerzas primero
            for (int i = 0; i < tires.size(); i++) {
                PhysicTire tire = tires.get(i);
                Forces forces = calculateForces(tire, tires);
                updateTirePhysics(tire, forces);
                
                // Actualizar delta considerando todas las colisiones
                for (int j = i + 1; j < tires.size(); j++) {
                    PhysicTire other = tires.get(j);
                    float distance = calculateDistance(tire, other);
                    float minDistance = config.tireRadius * 2 + config.distTire;
                    
                    if (distance < minDistance) {
                        float overlap = minDistance - distance;
                        maxDelta = Math.max(maxDelta, overlap);
                    }
                }
                
                // Verificar colisiones con los bordes
                maxDelta = Math.max(maxDelta, calculateBoundaryDelta(tire));
            }
            
            return maxDelta;
        }

        private float calculateDistance(PhysicTire t1, PhysicTire t2) {
            float dx = t1.getX() - t2.getX();
            float dy = t1.getY() - t2.getY();
            return (float) Math.sqrt(dx * dx + dy * dy);
        }

        private float calculateBoundaryDelta(PhysicTire tire) {
            float maxBoundaryDelta = 0;
            float x = tire.getX();
            float y = tire.getY();
            float r = config.tireRadius;
            
            // Verificar distancia a cada borde
            if (x - r < config.distBorder) {
                maxBoundaryDelta = Math.max(maxBoundaryDelta, config.distBorder - (x - r));
            }
            if (x + r > config.containerWidth - config.distBorder) {
                maxBoundaryDelta = Math.max(maxBoundaryDelta, (x + r) - (config.containerWidth - config.distBorder));
            }
            if (y - r < config.distBorder) {
                maxBoundaryDelta = Math.max(maxBoundaryDelta, config.distBorder - (y - r));
            }
            if (y + r > config.containerHeight - config.distBorder) {
                maxBoundaryDelta = Math.max(maxBoundaryDelta, (y + r) - (config.containerHeight - config.distBorder));
            }
            
            return maxBoundaryDelta;
        }

        private Forces calculateForces(PhysicTire tire1, List<PhysicTire> tires) {
            Forces forces = new Forces();
            
            // Amortiguación
            forces.fx = -tire1.getCurrentSpeedX() * (1 - DAMPING_FACTOR);
            forces.fy = -tire1.getCurrentSpeedY() * (1 - DAMPING_FACTOR);
            
            // Fuerzas entre ruedas
            for (PhysicTire tire2 : tires) {
                if (tire1 == tire2) continue;
                
                float dx = tire1.getX() - tire2.getX();
                float dy = tire1.getY() - tire2.getY();
                float distance = (float)Math.sqrt(dx * dx + dy * dy);
                float minDistance = config.tireRadius * 2 + config.distTire;
                
                if (distance < minDistance * 2) {  // Aumentar rango de interacción
                    // Fuerza base inversamente proporcional a la distancia
                    float forceMagnitude = WALL_REPULSION_FORCE;
                    
                    // Fuerza adicional si hay solapamiento
                    if (distance < minDistance) {
                        float overlap = minDistance - distance;
                        forceMagnitude += overlap * WALL_REPULSION_FORCE * 4;  // Aumentar fuerza de repulsión
                    }
                    
                    if (distance > 0.0001f) {  // Evitar división por cero
                        float nx = dx / distance;
                        float ny = dy / distance;
                        forces.fx += nx * forceMagnitude;
                        forces.fy += ny * forceMagnitude;
                    }
                }
            }
            
            // Fuerzas de los bordes más fuertes
            addBoundaryForces(forces, tire1);
            
            return forces;
        }

        private void addBoundaryForces(Forces forces, PhysicTire tire) {
            float x = tire.getX();
            float y = tire.getY();
            float r = config.tireRadius;
            float borderForce = WALL_REPULSION_FORCE * 5;  // Aumentar significativamente la fuerza de los bordes
            
            // Aplicar fuerzas más fuertes cerca de los bordes
            if (x - r < config.distBorder * 2) {
                float dist = x - r - config.distBorder;
                forces.fx += borderForce * (1.0f + Math.abs(dist / config.distBorder));
            }
            if (x + r > config.containerWidth - config.distBorder * 2) {
                float dist = config.containerWidth - config.distBorder - (x + r);
                forces.fx -= borderForce * (1.0f + Math.abs(dist / config.distBorder));
            }
            if (y - r < config.distBorder * 2) {
                float dist = y - r - config.distBorder;
                forces.fy += borderForce * (1.0f + Math.abs(dist / config.distBorder));
            }
            if (y + r > config.containerHeight - config.distBorder * 2) {
                float dist = config.containerHeight - config.distBorder - (y + r);
                forces.fy -= borderForce * (1.0f + Math.abs(dist / config.distBorder));
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
        float delta = 0;
    }

    // Estado de la simulación
    private static class SimulationState {
        private final Config config;
        private final List<PhysicTire> tires;
        private volatile int iteration;
        private volatile float delta;
        private float lastDelta;
        private int stagnationCounter;
        private static final int STAGNATION_CHECK_INTERVAL = 5000;
        private static final int STAGNATION_THRESHOLD = 5;
        private static final float DELTA_CHANGE_THRESHOLD = 0.01f;
        private List<RemovalTest> activeTests;
        private List<PhysicTire> bestConfiguration;
        private int bestValidTireCount;
        private float bestValidDelta;
        private boolean testsInProgress;

        SimulationState(Config config) {
            this.config = config;
            this.tires = new ArrayList<>();
            this.activeTests = new ArrayList<>();
            this.bestConfiguration = new ArrayList<>();
            this.bestValidTireCount = 0;
            this.bestValidDelta = Float.MAX_VALUE;
            this.testsInProgress = false;
        }

        synchronized void initialize() {
            tires.clear();
            iteration = 0;
            delta = 0;
            
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
        float getDelta() { return delta; }
        
        synchronized void incrementIteration() { iteration++; }
        synchronized void setDelta(float newDelta) { 
            delta = newDelta;
            // Mostrar progreso cada 10000 iteraciones en lugar de 100
            if (iteration % 10000 == 0) {
                System.out.printf("Iteración %,d: Delta=%.6f, Ruedas válidas=%d/%d%n", 
                    iteration, delta, countValidTires(tires), tires.size());
            }
        }

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
            System.out.printf("- Delta actual: %.6f%n", delta);
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
            private float testDelta;
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
                float bestTestDelta = Float.MAX_VALUE;
                List<PhysicTire> bestTestConfiguration = null;
                
                System.out.printf("\nIniciando prueba para %s:%n", removedTire.getModel());
                System.out.println("- Configuración inicial: " + testTires.size() + " ruedas");
                
                // Ejecutar simulación de prueba
                for (int i = 0; i < TEST_ITERATIONS; i++) {
                    testDelta = testEngine.simulatePhysics(testTires);
                    
                    // Guardar la mejor configuración encontrada durante la prueba
                    int currentValidCount = countValidTires(testTires);
                    if (currentValidCount > validTireCount || 
                        (currentValidCount == validTireCount && testDelta < bestTestDelta)) {
                        validTireCount = currentValidCount;
                        bestTestDelta = testDelta;
                        bestTestConfiguration = new ArrayList<>(testTires);
                        
                        if (i % 1000 == 0) {
                            System.out.printf("  Iteración %d: %d ruedas válidas, delta=%.6f%n", 
                                i, currentValidCount, testDelta);
                        }
                    }
                }
                
                // Usar la mejor configuración encontrada
                if (bestTestConfiguration != null) {
                    testDelta = bestTestDelta;
                    testTires.clear();
                    testTires.addAll(bestTestConfiguration);
                }
                
                finalConfiguration = new ArrayList<>(testTires);
                
                synchronized (SimulationState.this) {
                    System.out.printf("\nResultados de prueba para %s:%n", removedTire.getModel());
                    System.out.printf("- Delta final: %.6f%n", testDelta);
                    System.out.printf("- Ruedas válidas: %d%n", validTireCount);
                    
                    if (validTireCount > bestValidTireCount ||
                        (validTireCount == bestValidTireCount && testDelta < bestValidDelta)) {
                        bestValidTireCount = validTireCount;
                        bestValidDelta = testDelta;
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
                    System.out.printf("- Delta anterior: %.6f%n", delta);
                    System.out.printf("- Delta nueva: %.6f%n", bestValidDelta);
                    
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
                
                stagnationCounter = 0;
                lastDelta = delta;
            }
        }

        synchronized void checkForStagnation() {
            if (!testsInProgress && iteration % STAGNATION_CHECK_INTERVAL == 0) {
                // Verificar si el sistema está lo suficientemente estable (delta < 0.5)
                System.out.printf("\nVerificando estabilidad (Iteración %,d):%n", iteration);
                System.out.printf("- Delta actual: %.6f%n", delta);
                
                if (delta < 0.5f) {
                    System.out.println("Sistema estable (delta < 0.5). Iniciando pruebas de eliminación.");
                    startRemovalTests();
                } else {
                    System.out.println("Sistema aún inestable. Continuando optimización.");
                }
                
                lastDelta = delta;
            }
        }
    }
}
