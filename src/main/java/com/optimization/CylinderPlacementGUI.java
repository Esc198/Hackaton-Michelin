package com.optimization;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class CylinderPlacementGUI extends JFrame {
    private CylinderPlacementOptimizer optimizer;
    private CylinderPlacementVisualizer visualizer;
    private JPanel controlPanel;
    
    // Campos de entrada
    private JTextField widthField;
    private JTextField lengthField;
    private JTextField cylinderRadiusField;
    private JTextField forkliftWidthField;
    private JTextField forkliftLengthField;
    private JTextField turningAngleField;
    
    // Sliders
    private JSlider widthSlider;
    private JSlider lengthSlider;
    private JSlider cylinderRadiusSlider;
    private JSlider forkliftWidthSlider;
    private JSlider forkliftLengthSlider;
    private JSlider turningAngleSlider;
    
    // Valores mínimos y máximos para los sliders
    private static final double MIN_WIDTH = 5.0;
    private static final double MAX_WIDTH = 20.0;
    private static final double MIN_LENGTH = 5.0;
    private static final double MAX_LENGTH = 20.0;
    private static final double MIN_RADIUS = 0.2;
    private static final double MAX_RADIUS = 2.0;
    private static final double MIN_FORKLIFT_WIDTH = 0.5;
    private static final double MAX_FORKLIFT_WIDTH = 3.0;
    private static final double MIN_FORKLIFT_LENGTH = 1.0;
    private static final double MAX_FORKLIFT_LENGTH = 5.0;
    private static final double MIN_TURNING_ANGLE = 0.0;
    private static final double MAX_TURNING_ANGLE = 90.0;
    
    public CylinderPlacementGUI() {
        setTitle("Optimización de Colocación de Cilindros");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        // Valores iniciales
        double width = 10.0;
        double length = 15.0;
        double cylinderRadius = 0.5;
        double forkliftWidth = 1.2;
        double forkliftLength = 2.5;
        double turningAngle = 30.0;
        
        // Crear el optimizador inicial
        optimizer = new CylinderPlacementOptimizer(
            width, length, cylinderRadius, forkliftWidth, forkliftLength, turningAngle);
        optimizer.optimize();
        
        // Crear el panel de visualización
        visualizer = new CylinderPlacementVisualizer(optimizer);
        add(visualizer, BorderLayout.CENTER);
        
        // Crear el panel de control
        createControlPanel(width, length, cylinderRadius, forkliftWidth, forkliftLength, turningAngle);
        add(controlPanel, BorderLayout.EAST);
        
        pack();
        setLocationRelativeTo(null);
    }
    
    private void createControlPanel(double width, double length, double cylinderRadius,
                                  double forkliftWidth, double forkliftLength, double turningAngle) {
        controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Crear campos de entrada y sliders
        widthField = createInputFieldWithSlider("Ancho del área:", width, MIN_WIDTH, MAX_WIDTH);
        lengthField = createInputFieldWithSlider("Largo del área:", length, MIN_LENGTH, MAX_LENGTH);
        cylinderRadiusField = createInputFieldWithSlider("Radio de los cilindros:", cylinderRadius, MIN_RADIUS, MAX_RADIUS);
        forkliftWidthField = createInputFieldWithSlider("Ancho de la carretilla:", forkliftWidth, MIN_FORKLIFT_WIDTH, MAX_FORKLIFT_WIDTH);
        forkliftLengthField = createInputFieldWithSlider("Largo de la carretilla:", forkliftLength, MIN_FORKLIFT_LENGTH, MAX_FORKLIFT_LENGTH);
        turningAngleField = createInputFieldWithSlider("Ángulo de giro (grados):", turningAngle, MIN_TURNING_ANGLE, MAX_TURNING_ANGLE);
        
        // Añadir espacio al final
        controlPanel.add(Box.createVerticalStrut(20));
    }
    
    private JTextField createInputFieldWithSlider(String label, double value, double min, double max) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        
        // Panel para el campo de texto
        JPanel textPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        textPanel.add(new JLabel(label));
        JTextField field = new JTextField(String.format("%.2f", value), 8);
        textPanel.add(field);
        panel.add(textPanel);
        
        // Crear y configurar el slider
        JSlider slider = new JSlider(
            JSlider.HORIZONTAL,
            (int)(min * 100), // Convertir a enteros para el slider
            (int)(max * 100),
            (int)(value * 100)
        );
        slider.setMajorTickSpacing((int)((max - min) * 100 / 4));
        slider.setMinorTickSpacing((int)((max - min) * 100 / 8));
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        panel.add(slider);
        
        // Añadir el panel al panel de control
        controlPanel.add(panel);
        controlPanel.add(Box.createVerticalStrut(10));
        
        // Configurar listeners
        setupListeners(field, slider, min, max);
        
        return field;
    }
    
    private void setupListeners(JTextField field, JSlider slider, double min, double max) {
        // Listener para el campo de texto
        field.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { updateValue(); }
            public void insertUpdate(DocumentEvent e) { updateValue(); }
            public void removeUpdate(DocumentEvent e) { updateValue(); }
            
            private void updateValue() {
                try {
                    double value = Double.parseDouble(field.getText());
                    if (value >= min && value <= max) {
                        slider.setValue((int)(value * 100));
                        updateOptimization();
                    }
                } catch (NumberFormatException ex) {
                    // Ignorar valores no numéricos
                }
            }
        });
        
        // Listener para el slider
        slider.addChangeListener(e -> {
            if (!slider.getValueIsAdjusting()) {
                double value = slider.getValue() / 100.0;
                field.setText(String.format("%.2f", value));
                updateOptimization();
            }
        });
    }
    
    private void updateOptimization() {
        try {
            double width = Double.parseDouble(widthField.getText());
            double length = Double.parseDouble(lengthField.getText());
            double cylinderRadius = Double.parseDouble(cylinderRadiusField.getText());
            double forkliftWidth = Double.parseDouble(forkliftWidthField.getText());
            double forkliftLength = Double.parseDouble(forkliftLengthField.getText());
            double turningAngle = Double.parseDouble(turningAngleField.getText());
            
            // Crear nuevo optimizador con los valores actualizados
            optimizer = new CylinderPlacementOptimizer(
                width, length, cylinderRadius, forkliftWidth, forkliftLength, turningAngle);
            optimizer.optimize();
            
            // Actualizar visualización
            visualizer.updateOptimizer(optimizer);
            
        } catch (NumberFormatException ex) {
            // Ignorar valores no numéricos
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            CylinderPlacementGUI gui = new CylinderPlacementGUI();
            gui.setVisible(true);
        });
    }
} 