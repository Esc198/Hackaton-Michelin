package com.optimization;

public class CylinderOptimizer {
    private double area;
    private double radius;
    private double height;
    private double volume;

    public CylinderOptimizer(double area) {
        this.area = area;
        optimize();
    }

    private void optimize() {
        // Para un cilindro con área superficial fija, el volumen máximo se alcanza cuando
        // la altura es igual al diámetro (h = 2r)
        // Área superficial = 2πr² + 2πrh
        // Con h = 2r, tenemos: A = 2πr² + 4πr² = 6πr²
        // Por lo tanto: r = √(A/6π)
        
        this.radius = Math.sqrt(area / (6 * Math.PI));
        this.height = 2 * radius;
        this.volume = Math.PI * radius * radius * height;
    }

    public double getRadius() {
        return radius;
    }

    public double getHeight() {
        return height;
    }

    public double getVolume() {
        return volume;
    }

    public double getArea() {
        return area;
    }

    @Override
    public String toString() {
        return String.format("Cilindro optimizado:\n" +
                "Área superficial: %.2f\n" +
                "Radio: %.2f\n" +
                "Altura: %.2f\n" +
                "Volumen: %.2f",
                area, radius, height, volume);
    }

    public static void main(String[] args) {
        // Ejemplo de uso
        double areaSuperficial = 100.0; // Área superficial en unidades cuadradas
        CylinderOptimizer optimizador = new CylinderOptimizer(areaSuperficial);
        System.out.println(optimizador);
    }
} 