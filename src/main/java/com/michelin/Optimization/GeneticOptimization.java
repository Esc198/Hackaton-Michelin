package com.michelin.Optimization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import com.michelin.utils.Tire;

public class GeneticOptimization implements AbstractOptimization {
    private final float tireRadius;
    private final float containerWidth;
    private final float containerHeight;
    private final float distBorder;
    private final float distTire;
    
    private List<Individual> population;
    private Individual bestSolution;
    private boolean finished;
    
    private static final int POPULATION_SIZE = 10000;
    private static final int MAX_GENERATIONS = 1000000000;
    private static final float MUTATION_RATE = 0.1f;
    private static final int ELITE_SIZE = 5;
    
    private int currentGeneration;
    private final Random random;
    private final ExecutorService executor;

    public GeneticOptimization(float tireRadius, float containerWidth, float containerHeight, float distBorder, float distTire) {
        this.tireRadius = tireRadius;
        this.containerWidth = containerWidth;
        this.containerHeight = containerHeight;
        this.distBorder = distBorder;
        this.distTire = distTire;
        this.random = new Random();
        this.executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        this.finished = false;
    }

    private class Individual implements Comparable<Individual> {
        List<Tire> tires;
        float fitness;

        Individual() {
            this.tires = new ArrayList<>();
        }

        Individual(List<Tire> tires) {
            this.tires = new ArrayList<>();
            for (Tire tire : tires) {
                this.tires.add(new Tire(tire.getModel(), tire.getRadius(), tire.getPositionX(), tire.getPositionY()));
            }
            calculateFitness();
        }

        void calculateFitness() {
            float validTires = 0;
            for (Tire tire : tires) {
                if (isValidPosition(tire, this)) {
                    validTires++;
                }
            }
            this.fitness = validTires;
        }

        @Override
        public int compareTo(Individual other) {
            return Float.compare(other.fitness, this.fitness); // Mayor fitness primero
        }
    }

    private boolean isValidPosition(Tire tire, Individual individual) {
        // Verificar límites del contenedor
        if (tire.getPositionX() - tireRadius < distBorder || 
            tire.getPositionX() + tireRadius > containerWidth - distBorder ||
            tire.getPositionY() - tireRadius < distBorder || 
            tire.getPositionY() + tireRadius > containerHeight - distBorder) {
            return false;
        }

        // Verificar colisiones con otras ruedas en el mismo individuo
        for (Tire other : individual.tires) {
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
        population = new ArrayList<>();
        currentGeneration = 0;
        
        // Crear población inicial con individuos válidos
        while (population.size() < POPULATION_SIZE) {
            Individual individual = createValidIndividual();
            if (individual != null) {
                population.add(individual);
                if (bestSolution == null || individual.fitness > bestSolution.fitness) {
                    bestSolution = new Individual(individual.tires);
                }
            }
        }
    }

    private Individual createValidIndividual() {
        Individual individual = new Individual();
        int maxAttempts = 1000; // Límite de intentos para evitar bucles infinitos
        int attempts = 0;
        
        while (attempts < maxAttempts) {
            // Intentar añadir una nueva rueda
            float x = distBorder + tireRadius + random.nextFloat() * (containerWidth - 2 * (distBorder + tireRadius));
            float y = distBorder + tireRadius + random.nextFloat() * (containerHeight - 2 * (distBorder + tireRadius));
            
            Tire newTire = new Tire("Tire " + individual.tires.size(), tireRadius, x, y);
            
            // Verificar si la posición es válida
            if (isValidPosition(newTire, individual)) {
                individual.tires.add(newTire);
                attempts = 0; // Resetear intentos al añadir una rueda exitosamente
            } else {
                attempts++;
            }
            
            // Si hemos intentado muchas veces sin éxito, probablemente el espacio está lleno
            if (attempts >= 100 && !individual.tires.isEmpty()) {
                break;
            }
        }
        
        // Solo retornar el individuo si tiene al menos una rueda
        if (!individual.tires.isEmpty()) {
            individual.calculateFitness();
            return individual;
        }
        return null;
    }

    @Override
    public void run() {
        if (currentGeneration >= MAX_GENERATIONS) {
            finished = true;
            return;
        }

        try {
            // Evaluar población en paralelo
            List<Future<Individual>> futures = new ArrayList<>();
            for (Individual individual : population) {
                futures.add(executor.submit(() -> {
                    individual.calculateFitness();
                    return individual;
                }));
            }

            // Esperar resultados y ordenar por fitness
            population = futures.stream()
                              .map(f -> {
                                  try { return f.get(); }
                                  catch (Exception e) { return null; }
                              })
                              .collect(Collectors.toList());
            Collections.sort(population);

            // Actualizar mejor solución
            if (population.get(0).fitness > bestSolution.fitness) {
                bestSolution = new Individual(population.get(0).tires);
            }

            // Crear nueva generación
            List<Individual> newPopulation = new ArrayList<>();

            // Mantener elite
            for (int i = 0; i < ELITE_SIZE; i++) {
                newPopulation.add(new Individual(population.get(i).tires));
            }

            // Crossover y mutación
            while (newPopulation.size() < POPULATION_SIZE) {
                Individual parent1 = selectParent();
                Individual parent2 = selectParent();
                Individual child = crossover(parent1, parent2);
                if (random.nextFloat() < MUTATION_RATE) {
                    mutate(child);
                }
                child.calculateFitness();
                newPopulation.add(child);
            }

            population = newPopulation;
            currentGeneration++;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Individual selectParent() {
        // Selección por torneo
        int tournamentSize = 5;
        Individual best = population.get(random.nextInt(population.size()));
        for (int i = 1; i < tournamentSize; i++) {
            Individual contestant = population.get(random.nextInt(population.size()));
            if (contestant.fitness > best.fitness) {
                best = contestant;
            }
        }
        return best;
    }

    private Individual crossover(Individual parent1, Individual parent2) {
        Individual child = new Individual();
        List<Tire> allTires = new ArrayList<>(parent1.tires);
        allTires.addAll(parent2.tires);
        Collections.shuffle(allTires, random);
        
        // Intentar añadir ruedas en orden aleatorio
        for (Tire tire : allTires) {
            Tire newTire = new Tire(tire.getModel(), tire.getRadius(), 
                                  tire.getPositionX(), tire.getPositionY());
            if (isValidPosition(newTire, child)) {
                child.tires.add(newTire);
            }
        }
        
        return child;
    }

    private void mutate(Individual individual) {
        // Lista temporal para almacenar las ruedas mutadas válidas
        List<Tire> newTires = new ArrayList<>();
        
        for (Tire tire : individual.tires) {
            if (random.nextFloat() < MUTATION_RATE) {
                // Intentar nueva posición
                float newX = distBorder + tireRadius + 
                    random.nextFloat() * (containerWidth - 2 * (distBorder + tireRadius));
                float newY = distBorder + tireRadius + 
                    random.nextFloat() * (containerHeight - 2 * (distBorder + tireRadius));
                
                Tire newTire = new Tire(tire.getModel(), tire.getRadius(), newX, newY);
                
                // Solo mantener la nueva posición si es válida
                if (isValidPosition(newTire, individual)) {
                    newTires.add(newTire);
                } else {
                    newTires.add(tire); // Mantener la posición original si la nueva no es válida
                }
            } else {
                newTires.add(tire);
            }
        }
        
        individual.tires.clear();
        individual.tires.addAll(newTires);
    }

    @Override
    public List<Tire> getResult() {
        return bestSolution != null ? bestSolution.tires : new ArrayList<>();
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    protected void finalize() {
        executor.shutdown();
    }
}
