package com.michelin.Optimization;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.michelin.utils.Tire;

public class GeneticOptimization implements AbstractOptimization {
    private final float tireRadius;
    private final float containerWidth;
    private final float containerHeight;
    private final float distBorder;
    private final float distTire;
    
    private Solution currentSolution;
    private Solution bestSolution;
    private boolean finished;
    
    // Parámetros del recocido simulado
    private static final double INITIAL_TEMPERATURE = 100.0;
    private static final double COOLING_RATE = 0.995;
    private static final int MAX_ITERATIONS = 1000000;
    private double currentTemperature;
    
    private final Random random;
    private int iteration;

    public GeneticOptimization(float tireRadius, float containerWidth, float containerHeight, 
            float distBorder, float distTire) {
        this.tireRadius = tireRadius;
        this.containerWidth = containerWidth;
        this.containerHeight = containerHeight;
        this.distBorder = distBorder;
        this.distTire = distTire;
        this.random = new Random();
        this.currentTemperature = INITIAL_TEMPERATURE;
        this.iteration = 0;
    }

    private class Solution {
        List<Tire> tires;
        double fitness;

        Solution() {
            this.tires = new ArrayList<>();
        }

        Solution(List<Tire> tires) {
            this.tires = new ArrayList<>(tires);
            calculateFitness();
        }

        final void calculateFitness() {
            int totalTires = tires.size();
            int validTires = 0;

            // Contar ruedas válidas
            for (Tire tire : tires) {
                if (isValidPosition(tire, this)) {
                    validTires++;
                }
            }
            
            // El fitness es el número de ruedas válidas
            // Las soluciones con más ruedas válidas tendrán mejor fitness
            this.fitness = validTires;
        }

        Solution createNeighbor() {
            Solution neighbor = new Solution(this.tires);
            
            switch(random.nextInt(4)) {
                case 0: // Mover una rueda
                    if (!neighbor.tires.isEmpty()) {
                        int index = random.nextInt(neighbor.tires.size());
                        Tire tire = neighbor.tires.get(index);
                        float newX = distBorder + tireRadius + 
                            random.nextFloat() * (containerWidth - 2 * (distBorder + tireRadius));
                        float newY = distBorder + tireRadius + 
                            random.nextFloat() * (containerHeight - 2 * (distBorder + tireRadius));
                        // No verificamos validez, simplemente movemos la rueda
                        neighbor.tires.set(index, new Tire(tire.getModel(), tire.getRadius(), newX, newY));
                    }
                    break;
                    
                case 1: // Intercambiar dos ruedas
                    if (neighbor.tires.size() >= 2) {
                        int i1 = random.nextInt(neighbor.tires.size());
                        int i2 = random.nextInt(neighbor.tires.size());
                        Tire t1 = neighbor.tires.get(i1);
                        Tire t2 = neighbor.tires.get(i2);
                        float tempX = t1.getPositionX();
                        float tempY = t1.getPositionY();
                        neighbor.tires.set(i1, new Tire(t1.getModel(), t1.getRadius(), 
                            t2.getPositionX(), t2.getPositionY()));
                        neighbor.tires.set(i2, new Tire(t2.getModel(), t2.getRadius(), 
                            tempX, tempY));
                    }
                    break;
                    
                case 2: // Añadir una rueda
                    float x = distBorder + tireRadius + 
                        random.nextFloat() * (containerWidth - 2 * (distBorder + tireRadius));
                    float y = distBorder + tireRadius + 
                        random.nextFloat() * (containerHeight - 2 * (distBorder + tireRadius));
                    // Añadir la rueda sin verificar validez
                    neighbor.tires.add(new Tire("Tire " + neighbor.tires.size(), tireRadius, x, y));
                    break;

                case 3: // Eliminar una rueda
                    if (neighbor.tires.size() > 1) {
                        // Intentar eliminar primero una rueda inválida si existe
                        int indexToRemove = findInvalidTireIndex(neighbor);
                        if (indexToRemove == -1) {
                            // Si no hay ruedas inválidas, eliminar una al azar
                            indexToRemove = random.nextInt(neighbor.tires.size());
                        }
                        neighbor.tires.remove(indexToRemove);
                    }
                    break;
            }
            
            neighbor.calculateFitness();
            return neighbor;
        }

        private int findInvalidTireIndex(Solution solution) {
            for (int i = 0; i < solution.tires.size(); i++) {
                if (!isValidPosition(solution.tires.get(i), solution)) {
                    return i;
                }
            }
            return -1;
        }
    }

    private boolean isValidPosition(Tire tire, Solution solution) {
        // Verificar límites del contenedor
        if (tire.getPositionX() - tireRadius < distBorder || 
            tire.getPositionX() + tireRadius > containerWidth - distBorder ||
            tire.getPositionY() - tireRadius < distBorder || 
            tire.getPositionY() + tireRadius > containerHeight - distBorder) {
            return false;
        }

        // Verificar colisiones con otras ruedas
        for (Tire other : solution.tires) {
            if (tire == other) continue;
            float dx = tire.getPositionX() - other.getPositionX();
            float dy = tire.getPositionY() - other.getPositionY();
            float distance = (float) Math.sqrt(dx * dx + dy * dy);
            if (distance < (tireRadius * 2 + distTire)) {
                return false;
            }
        }
        return true;
    }



    @Override
    public void setup() {
        // Crear solución inicial con patrón hexagonal
        currentSolution = createHexagonalSolution();
        bestSolution = new Solution(currentSolution.tires);
        currentTemperature = INITIAL_TEMPERATURE;
        iteration = 0;
        finished = false;
    }

    private Solution createHexagonalSolution() {
        Solution solution = new Solution();
        float rowHeight = (float)(Math.sqrt(3) * (tireRadius + distTire/2));
        float colWidth = 2 * (tireRadius + distTire/2);
        
        for (float y = distBorder + tireRadius; y <= containerHeight - distBorder - tireRadius; y += rowHeight) {
            boolean oddRow = (int)((y - distBorder - tireRadius) / rowHeight) % 2 == 1;
            float startX = distBorder + tireRadius + (oddRow ? colWidth/2 : 0);
            
            for (float x = startX; x <= containerWidth - distBorder - tireRadius; x += colWidth) {
                Tire tire = new Tire("Tire " + solution.tires.size(), tireRadius, x, y);
                if (isValidPosition(tire, solution)) {
                    solution.tires.add(tire);
                }
            }
        }        
        solution.calculateFitness();
        return solution;
    }

    @Override
    public void run() {
        if (iteration >= MAX_ITERATIONS || currentTemperature < 0.01) {
            finished = true;
            return;
        }

        // Generar y evaluar solución vecina
        Solution neighbor = currentSolution.createNeighbor();
        double delta = neighbor.fitness - currentSolution.fitness;

        // Criterio de aceptación del recocido simulado
        if (delta > 0 || random.nextDouble() < Math.exp(delta / currentTemperature)) {
            currentSolution = neighbor;
            if (currentSolution.fitness > bestSolution.fitness) {
                bestSolution = new Solution(currentSolution.tires);
                System.out.println("Best solution found: " + bestSolution.fitness);
            }
        }

        // Enfriar temperatura
        currentTemperature *= COOLING_RATE;
        iteration++;
    }

    @Override
    public List<Tire> getResult() {
        return bestSolution != null ? bestSolution.tires : new ArrayList<>();
    }

    @Override
    public boolean isFinished() {
        finished = iteration >= MAX_ITERATIONS || currentTemperature < 0.01;
        return finished;
    }
}
