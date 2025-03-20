package com.optimization;

import java.util.ArrayList;
import java.util.List;

public class CylinderPlacementOptimizer {
    // Dimensiones del área rectangular
    private double width;
    private double length;
    
    // Características de los cilindros
    private double cylinderRadius;
    
    // Características de la carretilla
    private double forkliftWidth;
    private double forkliftLength;
    private double forkliftTurningAngle; // en grados
    
    // Espacio mínimo requerido entre cilindros
    private double minimumSpaceBetweenCylinders;
    
    // Resultado de la optimización
    private List<CylinderPosition> cylinderPositions;
    private int totalCylinders;
    
    public CylinderPlacementOptimizer(double width, double length, double cylinderRadius,
                                    double forkliftWidth, double forkliftLength, 
                                    double forkliftTurningAngle) {
        this.width = width;
        this.length = length;
        this.cylinderRadius = cylinderRadius;
        this.forkliftWidth = forkliftWidth;
        this.forkliftLength = forkliftLength;
        this.forkliftTurningAngle = forkliftTurningAngle;
        
        // Calculamos el espacio mínimo entre cilindros basado en las dimensiones de la carretilla
        // Esta es una aproximación simple; podría mejorarse con un modelo más sofisticado
        this.minimumSpaceBetweenCylinders = calculateMinimumSpace();
        
        this.cylinderPositions = new ArrayList<>();
    }
    
    private double calculateMinimumSpace() {
        // Una aproximación simple: la carretilla necesita al menos este espacio para maniobrar
        // Se podría hacer un cálculo más preciso basado en el radio de giro
        return Math.max(forkliftWidth, forkliftLength * Math.sin(Math.toRadians(forkliftTurningAngle)));
    }
    
    public void optimize() {
        // Limpiamos resultados previos
        cylinderPositions.clear();
        
        // Calculamos el espacio efectivo necesario para cada cilindro
        double effectiveRadius = cylinderRadius + minimumSpaceBetweenCylinders / 2;
        
        // Calculamos el número óptimo de cilindros en cada dirección
        int[] optimalCounts = calculateOptimalCounts(effectiveRadius);
        int cylindersInWidth = optimalCounts[0];
        int cylindersInLength = optimalCounts[1];
        
        // Calculamos el espaciado para centrar la distribución
        double spacingWidth = (width - cylindersInWidth * 2 * cylinderRadius) / (cylindersInWidth + 1);
        double spacingLength = (length - cylindersInLength * 2 * cylinderRadius) / (cylindersInLength + 1);
        
        // Colocamos los cilindros en una distribución hexagonal
        for (int i = 0; i < cylindersInWidth; i++) {
            for (int j = 0; j < cylindersInLength; j++) {
                // Calculamos la posición base
                double x = spacingWidth + cylinderRadius + i * (2 * cylinderRadius + spacingWidth);
                double y = spacingLength + cylinderRadius + j * (2 * cylinderRadius + spacingLength);
                
                // Ajustamos la posición para crear un patrón hexagonal
                if (j % 2 == 1) {
                    x += cylinderRadius + spacingWidth / 2;
                }
                
                // Verificamos si la carretilla puede acceder a esta posición
                if (canForkliftAccess(x, y)) {
                    cylinderPositions.add(new CylinderPosition(x, y));
                }
            }
        }
        
        totalCylinders = cylinderPositions.size();
    }
    
    private int[] calculateOptimalCounts(double effectiveRadius) {
        // Calculamos el área total disponible
        double totalArea = width * length;
        
        // Calculamos el área de un cilindro (incluyendo el espacio mínimo requerido)
        double cylinderArea = Math.PI * (effectiveRadius * effectiveRadius);
        
        // Calculamos el área mínima necesaria para la carretilla
        double forkliftArea = forkliftWidth * forkliftLength;
        
        // Calculamos el área efectiva disponible (considerando el espacio para la carretilla)
        double effectiveArea = totalArea - (forkliftArea * 2); // Dejamos espacio para dos carretillas
        
        // Calculamos el número teórico máximo de cilindros que cabrían
        int theoreticalMaxCylinders = (int) (effectiveArea / cylinderArea);
        
        // Calculamos cuántos cilindros caben en cada dirección
        int cylindersInWidth = (int) Math.floor(width / (2 * effectiveRadius));
        int cylindersInLength = (int) Math.floor(length / (2 * effectiveRadius));
        
        // Ajustamos el número de cilindros para maximizar el uso del espacio
        while (cylindersInWidth * cylindersInLength < theoreticalMaxCylinders) {
            // Intentamos añadir más cilindros en la dirección que tenga más espacio
            if (width - (cylindersInWidth + 1) * 2 * effectiveRadius > 
                length - (cylindersInLength + 1) * 2 * effectiveRadius) {
                cylindersInWidth++;
            } else {
                cylindersInLength++;
            }
        }
        
        // Aseguramos que no excedamos el número teórico máximo
        while (cylindersInWidth * cylindersInLength > theoreticalMaxCylinders) {
            if (cylindersInWidth > cylindersInLength) {
                cylindersInWidth--;
            } else {
                cylindersInLength--;
            }
        }
        
        return new int[]{cylindersInWidth, cylindersInLength};
    }
    
    private boolean canForkliftAccess(double x, double y) {
        // Calculamos el radio de giro mínimo de la carretilla
        double turningRadius = forkliftLength / Math.sin(Math.toRadians(forkliftTurningAngle));
        
        // Verificamos si hay suficiente espacio para la carretilla
        double minDistanceFromEdge = Math.max(forkliftWidth, turningRadius);
        
        // Verificamos si el cilindro está dentro de los límites accesibles
        boolean withinBounds = x >= minDistanceFromEdge && 
                             y >= minDistanceFromEdge && 
                             x <= width - minDistanceFromEdge && 
                             y <= length - minDistanceFromEdge;
        
        // Verificamos si hay suficiente espacio para la maniobra
        if (withinBounds) {
            // Verificamos la distancia a otros cilindros
            for (CylinderPosition other : cylinderPositions) {
                double distance = Math.sqrt(
                    Math.pow(x - other.getX(), 2) + 
                    Math.pow(y - other.getY(), 2)
                );
                if (distance < 2 * cylinderRadius + minimumSpaceBetweenCylinders) {
                    return false;
                }
            }
        }
        
        return withinBounds;
    }
    
    public List<CylinderPosition> getCylinderPositions() {
        return cylinderPositions;
    }
    
    public int getTotalCylinders() {
        return totalCylinders;
    }
    
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append(String.format("Área rectangular: %.2f x %.2f\n", width, length));
        result.append(String.format("Radio de los cilindros: %.2f\n", cylinderRadius));
        result.append(String.format("Dimensiones de la carretilla: %.2f x %.2f\n", forkliftWidth, forkliftLength));
        result.append(String.format("Ángulo de giro de la carretilla: %.2f grados\n", forkliftTurningAngle));
        result.append(String.format("Espacio mínimo entre cilindros: %.2f\n", minimumSpaceBetweenCylinders));
        result.append(String.format("Total de cilindros óptimamente colocados: %d\n", totalCylinders));
        
        if (totalCylinders > 0) {
            result.append("Posiciones de los cilindros:\n");
            for (int i = 0; i < cylinderPositions.size(); i++) {
                CylinderPosition pos = cylinderPositions.get(i);
                result.append(String.format("  Cilindro %d: (%.2f, %.2f)\n", i + 1, pos.getX(), pos.getY()));
            }
        }
        
        return result.toString();
    }
    
    public double getWidth() {
        return width;
    }
    
    public double getLength() {
        return length;
    }
    
    public double getCylinderRadius() {
        return cylinderRadius;
    }
    
    // Clase interna para representar la posición de un cilindro
    public static class CylinderPosition {
        private double x;
        private double y;
        
        public CylinderPosition(double x, double y) {
            this.x = x;
            this.y = y;
        }
        
        public double getX() {
            return x;
        }
        
        public double getY() {
            return y;
        }
    }
    
    public static void main(String[] args) {
        // Ejemplo de uso
        double width = 10.0;            // Ancho del área rectangular
        double length = 15.0;           // Largo del área rectangular
        double cylinderRadius = 0.5;    // Radio de los cilindros
        double forkliftWidth = 1.2;     // Ancho de la carretilla
        double forkliftLength = 2.5;    // Largo de la carretilla
        double turningAngle = 30.0;     // Ángulo de giro de la carretilla en grados
        
        CylinderPlacementOptimizer optimizer = new CylinderPlacementOptimizer(
            width, length, cylinderRadius, forkliftWidth, forkliftLength, turningAngle);
            
        optimizer.optimize();
        System.out.println(optimizer);
    }
} 