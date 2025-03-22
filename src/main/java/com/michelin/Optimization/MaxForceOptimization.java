package com.michelin.Optimization;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.michelin.utils.PhysicTire;
import com.michelin.utils.Tire;

public class MaxForceOptimization implements AbstractOptimization {
    private final float tireRadius;
    private final float containerWidth;
    private final float containerHeight;
    private final float distBorder;
    private final float distTire;
    private List<PhysicTire> tires;

    private int iteration;
    private float delta;
    private final float maxIteration;
    private final float minDelta;

    private int tireCount;

    public MaxForceOptimization(float tireRadius, float containerWidth, float containerHeight, float distBorder,
            float distTire, float maxIteration, float minDelta) {
        this.tireRadius = tireRadius;
        this.containerWidth = containerWidth;
        this.containerHeight = containerHeight;
        this.distBorder = distBorder;
        this.distTire = distTire;
        this.maxIteration = maxIteration;
        this.minDelta = minDelta;

    }
    public MaxForceOptimization(float tireRadius, float containerWidth, float containerHeight, float distBorder,
            float distTire) {
        this(tireRadius, containerWidth, containerHeight, distBorder, distTire, 100000, 0.01f);
    }

    @Override
    public void setup() {
        tires = new ArrayList<>();
        iteration = 0;
        delta = 0;
        tireCount = (int) ((containerWidth - distBorder * 2) * (containerHeight - distBorder * 2)
                / (Math.PI * (tireRadius + distTire) * (tireRadius + distTire)));

                
        for (int i = 0; i < tireCount; i++) {
            float randomX = distBorder + tireRadius + (float)(Math.random() * (containerWidth - 2 * (distBorder + tireRadius)));
            float randomY = distBorder + tireRadius + (float)(Math.random() * (containerHeight - 2 * (distBorder + tireRadius)));
            tires.add(new PhysicTire(100, 100, 100, 100, "tire" + i, tireRadius, randomX, randomY));
        }
    }

    @Override
    public void run() {
        iteration++;
        delta = 0;
        
        // Comprobar colisiones entre ruedas
        for (int i = 0; i < tires.size(); i++) {
            PhysicTire tire1 = tires.get(i);
            float fx = 0, fy = 0;
            
            float wallRepulsionForce = 100;
            float dampingFactor = 0.95f; // Factor de amortiguación (1 = sin frenado, 0 = frenado total)
            
            // Aplicar fuerza de frenado proporcional a la velocidad
            float dampingForceX = -tire1.getCurrentSpeedX() * (1 - dampingFactor);
            float dampingForceY = -tire1.getCurrentSpeedY() * (1 - dampingFactor);
            fx += dampingForceX;
            fy += dampingForceY;
            
            // Fuerza de repulsión con otras ruedas
            for (int j = 0; j < tires.size(); j++) {
                if (i == j) continue;
                PhysicTire tire2 = tires.get(j);
                float dx = (float)(tire1.getX() - tire2.getX());
                float dy = (float)(tire1.getY() - tire2.getY());
                float distance = (float)Math.sqrt(dx * dx + dy * dy);
                float minDistance = (float)(tireRadius + tireRadius + distTire);
                if (distance < minDistance) {
                    // Hay superposición, calcular fuerza de repulsión
                    float overlap = minDistance - distance;
                    float forceMagnitude = overlap * wallRepulsionForce; // Factor de repulsión
                    
                    // Normalizar el vector de dirección
                    float nx = dx / distance;
                    float ny = dy / distance;
                    
                    // Acumular fuerzas
                    fx += nx * forceMagnitude;
                    fy += ny * forceMagnitude;
                    delta = Math.max(delta, overlap);
                }
            }
            // Fuerza de contención dentro del contenedor
            float x = tire1.getX();
            float y = tire1.getY();
            float r = tireRadius;
            
            // Comprobar límites del contenedor
            if (x - r < distBorder) {
                float overlap = distBorder - (x - r);
                fx += overlap * wallRepulsionForce;
                delta = Math.max(delta, overlap);
            }
            if (x + r > containerWidth - distBorder) {
                float overlap = (x + r) - (containerWidth - distBorder);
                fx -= overlap * wallRepulsionForce;
                delta = Math.max(delta, overlap);
            }
            if (y - r < distBorder) {
                float overlap = distBorder - (y - r);
                fy += overlap * wallRepulsionForce;
                delta = Math.max(delta, overlap);
            }
            if (y + r > containerHeight - distBorder) {
                float overlap = (y + r) - (containerHeight - distBorder);
                fy -= overlap * wallRepulsionForce;
                delta = Math.max(delta, overlap);
            }
            // Limitar las fuerzas al máximo permitido
            float forceMagnitude = (float)Math.sqrt(fx * fx + fy * fy);
            if (forceMagnitude > tire1.getMaxForce()) {
                float scale = tire1.getMaxForce() / forceMagnitude;
                fx *= scale;
                fy *= scale;
            }
            
            // Actualizar velocidad y posición
            float dt = 0.016f; // Aproximadamente 60 FPS
            
            // Actualizar aceleración
            tire1.setCurrentAccelerationX(fx / tire1.getMaxForce() * tire1.getMaxAcceleration());
            tire1.setCurrentAccelerationY(fy / tire1.getMaxForce() * tire1.getMaxAcceleration());
            
            // Actualizar velocidad con amortiguación adicional
            tire1.setCurrentSpeedX(tire1.getCurrentSpeedX() * dampingFactor + tire1.getCurrentAccelerationX() * dt);
            tire1.setCurrentSpeedY(tire1.getCurrentSpeedY() * dampingFactor + tire1.getCurrentAccelerationY() * dt);
            
            // Si la velocidad es muy pequeña, detener la rueda
            float minSpeed = 0.001f;
            if (Math.abs(tire1.getCurrentSpeedX()) < minSpeed && Math.abs(tire1.getCurrentSpeedY()) < minSpeed) {
                tire1.setCurrentSpeedX(0);
                tire1.setCurrentSpeedY(0);
            }
            
            // Limitar velocidad
            float speedMagnitude = (float)Math.sqrt(
                tire1.getCurrentSpeedX() * tire1.getCurrentSpeedX() + 
                tire1.getCurrentSpeedY() * tire1.getCurrentSpeedY()
            );
            if (speedMagnitude > tire1.getMaxSpeed()) {
                float scale = tire1.getMaxSpeed() / speedMagnitude;
                tire1.setCurrentSpeedX(tire1.getCurrentSpeedX() * scale);
                tire1.setCurrentSpeedY(tire1.getCurrentSpeedY() * scale);
            }
            
            // Actualizar posición
            tire1.setX(tire1.getX() + tire1.getCurrentSpeedX() * dt);
            tire1.setY(tire1.getY() + tire1.getCurrentSpeedY() * dt);
        }
    }

    @Override
    public List<Tire> getResult() {
        List<Tire> result = new LinkedList<>();
        for (PhysicTire tire : tires) {
            result.add(tire);
        }
        return result;
    }

    @Override
    public boolean isFinished() {
        return iteration >= maxIteration || delta < minDelta;
    }

}