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

        public MaxForceOptimization(float tireRadius, float containerWidth, float containerHeight,
                float distBorder, float distTire, float maxIteration) {
            this.config = new SimulationConfig(tireRadius, containerWidth, containerHeight,
                    distBorder, distTire, maxIteration);
            this.isRunning = new AtomicBoolean(false);
            this.executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

            // Agregar shutdown hook para limpieza en caso de interrupción
            Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

        }

        public MaxForceOptimization(float tireRadius, float containerWidth, float containerHeight,
                float distBorder, float distTire) {
            this(tireRadius, containerWidth, containerHeight, distBorder, distTire, 1_000_000); // Generalmente 10_000_000
        }
        public static PhysicTire generateRandomTire(float tireRadius, float containerWidth, float containerHeight, float distBorder, float distTire) {
            float randomX = (float) (Math.random() * (containerWidth - 2 * distBorder)
                    + distBorder);
            float randomY = (float) (Math.random() * (containerHeight - 2 * distBorder)
                    + distBorder);
            return new PhysicTire(100, 100, 100, 100, "Random", tireRadius, randomX, randomY);
        }

        @Override
        public void setup() {
            isRunning.set(true);

            // Inicializar con optimizaciones básicas
            int bestInitialCount = runInitialOptimizations();

            // Preparar lista de conteos
            int maxWheelCount = config.getMaxWheelCount();

            // Inicializar lista de resultados
            for (int i = 0; i < maxWheelCount; i++) {
                bestConfiguration.put(i, new ArrayList<>());
            }

            // Inicializar lista de ruedas iniciales
            List<PhysicTire> initialTires = new ArrayList<>();
            for (int i = 0; i < bestInitialCount; i++) {
                initialTires.add(generateRandomTire(config.tireRadius, config.containerWidth, config.containerHeight, config.distBorder, config.distTire));
            }

            System.out.println("Iniciando simulaciones...");
            // Iniciar simulaciones
            for (int i = bestInitialCount; i < maxWheelCount; i++) {
                final int simIndex = i - bestInitialCount;
                initialTires.add(generateRandomTire(config.tireRadius, config.containerWidth, config.containerHeight, config.distBorder, config.distTire));
                System.out.println("Simulación " + simIndex + " iniciada con " + initialTires.size() + " ruedas");

                executor.submit(() -> {
                    try {
                        // Crear engine de física, internamente crea un clon del array de ruedas para no modificar el original
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
            while (isRunning.get() && !physicsEngine.isFinished() && !Thread.currentThread().isInterrupted()) {

                physicsEngine.updateIteration();
                physicsEngine.simulatePhysics();
                int currentValidTires = physicsEngine.countValidTires();
                synchronized (bestConfiguration) {
                    try {
                        if (currentValidTires >= bestConfiguration.get(simIndex).size()) {
                            bestConfiguration.put(simIndex, new ArrayList<>(physicsEngine.getTires()));
                        }
                    } catch (Exception e) {
                        System.err.println("Error al actualizar la mejor configuración: " + e.getMessage());
                        bestConfiguration.put(simIndex, new ArrayList<>());
                    }
                }


            }
            System.out.println("Simulación " + simIndex + " finalizada");
            synchronized (bestConfiguration) {
                try {
                    System.out.println("Mejor configuración: " + bestConfiguration.get(simIndex).size() + " ruedas");
                } catch (Exception e) {
                    System.out.println("No hay mejor configuración para la simulación " + simIndex);
                }
            }

        }

        private int getBestValidTireCount() {
            synchronized (bestConfiguration) {
                int maxCount = 0;
                for (List<PhysicTire> tires : bestConfiguration.values()) {
                    int validTires = (int) tires.stream()
                            .filter(tire -> PhysicTire.isValidTire(tire, config.containerWidth, config.containerHeight, config.distBorder, tires, config.distTire))
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
                            (int)list1.stream().filter(t -> PhysicTire.isValidTire(t, config.containerWidth, config.containerHeight, config.distBorder, list1, config.distTire)).count(),
                            (int)list2.stream().filter(t -> PhysicTire.isValidTire(t, config.containerWidth, config.containerHeight, config.distBorder, list2, config.distTire)).count()
                        ))
                        .map(tires -> tires.stream()
                            .map(tire -> (Tire)tire.clone())
                            .collect(Collectors.toList()))
                        .orElse(new ArrayList<>());
                System.out.println("Mejor configuración: " + result.size() + " ruedas");
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
                float dx = tire1.getX() - tire2.getX();
                float dy = tire1.getY() - tire2.getY();
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                float minDist = 2.1f * config.tireRadius + config.distTire;
                if (dist > 0.0001f && dist < minDist) { // Solo repeler cuando hay superposición
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
            
            public boolean isFinished() {
                return iteration >= config.maxIteration;
            }

            public List<PhysicTire> getTires() {
                return new ArrayList<>(tires);
            }

            private int countValidTires() {
                return (int) tires.stream().filter(tire -> PhysicTire.isValidTire(tire, config.containerWidth, config.containerHeight, config.distBorder, tires, config.distTire)).count();
            }



        }

        private static class Vector2D {
            float x, y;
        }

}