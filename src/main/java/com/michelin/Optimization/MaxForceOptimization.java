package com.michelin.Optimization;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.michelin.utils.Tire;

public class MaxForceOptimization implements AbstractOptimization {
    
    private final long tireRadius;
    private final long containerWidth;
    private final long containerHeight;
    private final long distBorder;
    private final long distTire;

    private final ConcurrentHashMap<Integer, List<Tire>> bestConfiguration ;
    private final ConcurrentHashMap<Integer, Integer> ValidTires ;
    private ExecutorService executor;
    private final AtomicBoolean isRunning = new AtomicBoolean(true);
    
    static int getMaxWheelCount(long tireRadius, long containerWidth, long containerHeight, long distBorder, long distTire) {
        return (int) ((containerWidth - 2 * distBorder) * (containerHeight - 2 * distBorder) /
                (Math.PI * Math.pow(tireRadius + distTire, 2)));
    }
    public MaxForceOptimization(long tireRadius, long containerWidth, long containerHeight, long distBorder, long distTire) {
        this.tireRadius = tireRadius;
        this.containerWidth = containerWidth;
        this.containerHeight = containerHeight;
        this.distBorder = distBorder;
        this.distTire = distTire;
        this.bestConfiguration = new ConcurrentHashMap<>();
        this.ValidTires = new ConcurrentHashMap<>();
        

    }


    public int bestBasicMethod(){
        AbstractOptimization genericOptimization = new SquareGridOptimization(tireRadius, containerWidth, containerHeight, distBorder, distTire);
        genericOptimization.setup();
        genericOptimization.run();
        final List<Tire> result = genericOptimization.getResult();
        int squareValidTires = (int) result.stream().filter(tire -> Tire.isValidTire(tire, containerWidth, containerHeight, distBorder, result, distTire)).count();

        genericOptimization.stop();
        genericOptimization = new HexagonalOptimization(tireRadius, containerWidth, containerHeight, distBorder, distTire);
        genericOptimization.setup();
        genericOptimization.run();
        final List<Tire> result2 = genericOptimization.getResult();
        int hexagonalValidTires = (int) result2.stream().filter(tire -> Tire.isValidTire(tire, containerWidth, containerHeight, distBorder, result2, distTire)).count();
        genericOptimization.stop();
        
        
        return Math.max(squareValidTires, hexagonalValidTires);
    }


	@Override
	public void setup() {
        if (this.executor != null) {
            this.executor.shutdown();
        }
        isRunning.set(true);
        this.executor = java.util.concurrent.Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {this.stop();}));
        this.bestConfiguration.clear();
        this.ValidTires.clear();
        
        int minWheelCount = bestBasicMethod();
        int maxWheelCount = getMaxWheelCount(tireRadius, containerWidth, containerHeight, distBorder, distTire);
        for (int i = minWheelCount; i <= maxWheelCount; i++) {
            final int threadIndex = i - minWheelCount;
            
            this.bestConfiguration.put(threadIndex, new ArrayList<>());
            this.ValidTires.put(threadIndex, 0);
            executor.execute(() -> {
                try {
                    Physic physic = new Physic(tireRadius, containerWidth, containerHeight, distBorder, distTire, 1_000_000, threadIndex + minWheelCount);
                    physic.setup();
                    
                    while (!physic.isFinished() && isRunning.get()) {
                        physic.run();
                        List<Tire> result = physic.getResult();
                        int validTires = (int) result.stream().filter(tire -> Tire.isValidTire(tire, containerWidth, containerHeight, distBorder, result, distTire)).count();
                        if (validTires >= this.ValidTires.get(threadIndex)) {
                            this.ValidTires.put(threadIndex, validTires);
                            this.bestConfiguration.put(threadIndex, result);
                        }
                    }
                    System.out.println("Thread " + threadIndex + " finished");
                    System.out.println("Valid tires: " + this.ValidTires.get(threadIndex));
                    System.out.println("--------------------------------");
                    physic.stop();
                } catch (Exception e) {
                    System.out.println("Thread " + threadIndex + " finished with error: " + e.getMessage());
                }
                
            });
        }
    }


	@Override
	public List<Tire> getResult() {
		// Get the entry with the maximum number of valid tires from ValidTires map
		// This finds the thread/configuration that produced the most valid tire placements
		return this.bestConfiguration.get(this.ValidTires.entrySet().stream()
				// Compare entries by their value (number of valid tires)
				.max(Map.Entry.comparingByValue())
				// Extract just the key (thread index) from the max entry
				.map(Map.Entry::getKey)
				// Default to 0 if no entries exist
				.orElse(0));
	}

	@Override
	public boolean isFinished() {
		return this.executor.isTerminated();
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
            System.out.println("Executor cerrado correctamente");
            System.out.println("Best configuration: " + this.getResult());

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


}
