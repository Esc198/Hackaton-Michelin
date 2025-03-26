package com.michelin.Optimization;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

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

	@Override
	public void setup() {
        if (this.executor == null) {
            this.executor = java.util.concurrent.Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        } else {
            this.executor.shutdown();
            this.executor = java.util.concurrent.Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        }
        this.bestConfiguration.clear();
        this.ValidTires.clear();
        int maxWheelCount = getMaxWheelCount(tireRadius, containerWidth, containerHeight, distBorder, distTire);
        for (int i = 0; i < maxWheelCount; i++) {
            this.bestConfiguration.put(i, new ArrayList<>());
            this.ValidTires.put(i, 0);
            final int threadIndex = i;
            executor.execute(() -> {
                
                Physic physic = new Physic(tireRadius, containerWidth, containerHeight, distBorder, distTire, 1_000_000, threadIndex);
                physic.setup();
                
                while (!physic.isFinished()) {
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
                System.out.println("Best configuration: " + this.bestConfiguration.get(threadIndex));
                System.out.println("--------------------------------");
                physic.stop();
            });
        }



	}

	@Override
	public List<Tire> getResult() {
		return this.bestConfiguration.get(this.ValidTires.entrySet().stream()
				.max(Map.Entry.comparingByValue())
				.map(Map.Entry::getKey)
				.orElse(0));
	}

	@Override
	public boolean isFinished() {
		return this.executor.isTerminated();
	}

	@Override
	public void stop() {
		this.executor.shutdown();
	}


}
