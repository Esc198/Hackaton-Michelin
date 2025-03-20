package com.optimization;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class CylinderPlacementVisualizer extends JPanel {
    private CylinderPlacementOptimizer optimizer;
    private double scale;
    private static final int PANEL_WIDTH = 800;
    private static final int PANEL_HEIGHT = 600;
    private static final int MARGIN = 50;

    public CylinderPlacementVisualizer(CylinderPlacementOptimizer optimizer) {
        this.optimizer = optimizer;
        this.scale = calculateScale();
        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setBackground(Color.WHITE);
    }

    private double calculateScale() {
        // Calculamos la escala para que el área rectangular quepa en el panel
        double areaWidth = optimizer.getWidth();
        double areaLength = optimizer.getLength();
        
        double scaleX = (PANEL_WIDTH - 2 * MARGIN) / areaWidth;
        double scaleY = (PANEL_HEIGHT - 2 * MARGIN) / areaLength;
        
        // Tomamos la escala más pequeña para mantener la proporción
        return Math.min(scaleX, scaleY);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Dibujamos el área rectangular
        g2d.setColor(new Color(240, 240, 240));
        int rectWidth = (int) (optimizer.getWidth() * scale);
        int rectHeight = (int) (optimizer.getLength() * scale);
        g2d.fillRect(MARGIN, MARGIN, rectWidth, rectHeight);
        
        g2d.setColor(Color.BLACK);
        g2d.drawRect(MARGIN, MARGIN, rectWidth, rectHeight);
        
        // Dibujamos los cilindros y sus centros
        List<CylinderPlacementOptimizer.CylinderPosition> positions = optimizer.getCylinderPositions();
        
        // Dibujamos los cilindros
        g2d.setColor(new Color(100, 149, 237)); // Azul más suave
        for (CylinderPlacementOptimizer.CylinderPosition position : positions) {
            double x = position.getX() * scale + MARGIN;
            double y = position.getY() * scale + MARGIN;
            double diameter = optimizer.getCylinderRadius() * 2 * scale;
            
            // Dibujamos el círculo del cilindro
            Ellipse2D.Double circle = new Ellipse2D.Double(
                x - diameter/2, 
                y - diameter/2, 
                diameter, 
                diameter
            );
            g2d.fill(circle);
            
            // Dibujamos el borde del cilindro
            g2d.setColor(new Color(0, 0, 139)); // Azul oscuro para el borde
            g2d.draw(circle);
            
            // Dibujamos el centro del cilindro
            g2d.setColor(Color.RED);
            double crossSize = 5;
            g2d.draw(new Line2D.Double(x - crossSize, y, x + crossSize, y));
            g2d.draw(new Line2D.Double(x, y - crossSize, x, y + crossSize));
            
            // Punto central
            g2d.fill(new Ellipse2D.Double(x - 2, y - 2, 4, 4));
            
            // Número del cilindro
            g2d.setColor(Color.BLACK);
            g2d.drawString(String.valueOf(positions.indexOf(position) + 1), 
                         (float)(x - 3), (float)(y - 5));
        }
        
        // Añadimos información de la optimización
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        g2d.drawString("Total de cilindros: " + optimizer.getTotalCylinders(), MARGIN, MARGIN / 2);
        g2d.drawString("Área: " + optimizer.getWidth() + " x " + optimizer.getLength(), MARGIN, MARGIN / 2 + 15);
        g2d.drawString("Radio de los cilindros: " + optimizer.getCylinderRadius(), MARGIN, MARGIN / 2 + 30);
        
        // Leyenda
        int legendY = MARGIN + rectHeight + 20;
        g2d.setColor(new Color(100, 149, 237));
        g2d.fill(new Ellipse2D.Double(MARGIN, legendY, 20, 20));
        g2d.setColor(Color.BLACK);
        g2d.drawString("Cilindro", MARGIN + 25, legendY + 15);
        
        g2d.setColor(Color.RED);
        g2d.fill(new Ellipse2D.Double(MARGIN, legendY + 25, 4, 4));
        g2d.draw(new Line2D.Double(MARGIN - 2, legendY + 25, MARGIN + 6, legendY + 25));
        g2d.draw(new Line2D.Double(MARGIN + 2, legendY + 21, MARGIN + 2, legendY + 29));
        g2d.setColor(Color.BLACK);
        g2d.drawString("Centro del cilindro", MARGIN + 25, legendY + 30);
    }

    public static void createAndShowGUI(CylinderPlacementOptimizer optimizer) {
        JFrame frame = new JFrame("Optimización de colocación de cilindros");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        CylinderPlacementVisualizer visualizer = new CylinderPlacementVisualizer(optimizer);
        frame.getContentPane().add(visualizer);
        
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
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
        
        SwingUtilities.invokeLater(() -> createAndShowGUI(optimizer));
    }

    public void updateOptimizer(CylinderPlacementOptimizer newOptimizer) {
        this.optimizer = newOptimizer;
        this.scale = calculateScale();
        revalidate();
        repaint();
    }
} 