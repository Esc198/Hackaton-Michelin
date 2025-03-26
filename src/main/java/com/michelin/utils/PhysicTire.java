package com.michelin.utils;

import java.util.List;

public class PhysicTire extends Tire{
    private final float maxForce;
    private final float maxSpeed;
    private final float maxAcceleration;
    private final float maxDeceleration;

    private float currentForceX;
    private float currentForceY;
    private float currentSpeedX;
    private float currentSpeedY;
    private float currentAccelerationX;
    private float currentAccelerationY;

    public PhysicTire(float maxAcceleration, float maxDeceleration, float maxForce, float maxSpeed, String model, double radius, double x, double y) {
        super(model, radius, x, y);
        this.maxAcceleration = maxAcceleration;
        this.maxDeceleration = maxDeceleration;
        this.maxForce = maxForce;
        this.maxSpeed = maxSpeed;
    }
    
    // Getters y setters para las variables de física
    public float getCurrentForceX() { return currentForceX; }
    public float getCurrentForceY() { return currentForceY; }
    public float getCurrentSpeedX() { return currentSpeedX; }
    public float getCurrentSpeedY() { return currentSpeedY; }
    public float getCurrentAccelerationX() { return currentAccelerationX; }
    public float getCurrentAccelerationY() { return currentAccelerationY; }
    public float getMaxForce() { return maxForce; }
    public float getMaxSpeed() { return maxSpeed; }
    public float getMaxAcceleration() { return maxAcceleration; }
    public float getMaxDeceleration() { return maxDeceleration; }
    
    public void setCurrentForceX(float force) { this.currentForceX = force; }
    public void setCurrentForceY(float force) { this.currentForceY = force; }
    public void setCurrentSpeedX(float speed) { this.currentSpeedX = speed; }
    public void setCurrentSpeedY(float speed) { this.currentSpeedY = speed; }
    public void setCurrentAccelerationX(float acceleration) { this.currentAccelerationX = acceleration; }
    public void setCurrentAccelerationY(float acceleration) { this.currentAccelerationY = acceleration; }

    public float getX() {
        return super.getPositionX();
    }

    public float getY() {
        return super.getPositionY();
    }

    public void setX(float f) {
        super.setPositionX(f);
    }

    public void setY(float f) {
        super.setPositionY(f);
    }

    public PhysicTire clone() {
        return new PhysicTire(maxAcceleration, maxDeceleration, maxForce, maxSpeed, super.getModel(), super.getRadius(), super.getPositionX(), super.getPositionY());
    }
    
    
    public static boolean isValidTire(PhysicTire tire, double width, double height, double distBorder, List<PhysicTire> tires, double distTire) {
        double x = tire.getPositionX();
        double y = tire.getPositionY();
        double r = tire.getRadius();
        for (PhysicTire otherTire : tires) {
            if (otherTire == tire) {
                continue;
            }
            double otherX = otherTire.getPositionX();
            double otherY = otherTire.getPositionY();
            double distance = Math.sqrt(Math.pow(x - otherX, 2) + Math.pow(y - otherY, 2));
            if (distance < r + otherTire.getRadius() + distTire - 0.0001) {
                return false;
            }
        }
        return x - r >= distBorder && x + r <= width - distBorder && y - r >= distBorder && y + r <= height - distBorder;
    }
}
