package com.michelin.utils;

import java.util.List;

public class PhysicTire extends Tire{
    private final long maxForce;
    private final long maxSpeed;
    private final long maxAcceleration;
    private final long maxDeceleration;

    private long currentForceX;
    private long currentForceY;
    private long currentSpeedX;
    private long currentSpeedY;
    private long currentAccelerationX;
    private long currentAccelerationY;

    public PhysicTire(long maxAcceleration, long maxDeceleration, long maxForce, long maxSpeed, String model, long radius, long x, long y) {
        super(model, radius, x, y);
        this.maxAcceleration = maxAcceleration;
        this.maxDeceleration = maxDeceleration;
        this.maxForce = maxForce;
        this.maxSpeed = maxSpeed;
    }
    
    // Getters y setters para las variables de física
    public long getCurrentForceX() { return currentForceX; }
    public long getCurrentForceY() { return currentForceY; }
    public long getCurrentSpeedX() { return currentSpeedX; }
    public long getCurrentSpeedY() { return currentSpeedY; }
    public long getCurrentAccelerationX() { return currentAccelerationX; }
    public long getCurrentAccelerationY() { return currentAccelerationY; }
    public long getMaxForce() { return maxForce; }
    public long getMaxSpeed() { return maxSpeed; }
    public long getMaxAcceleration() { return maxAcceleration; }
    public long getMaxDeceleration() { return maxDeceleration; }
    
    public void setCurrentForceX(long force) { this.currentForceX = force; }
    public void setCurrentForceY(long force) { this.currentForceY = force; }
    public void setCurrentSpeedX(long speed) { this.currentSpeedX = speed; }
    public void setCurrentSpeedY(long speed) { this.currentSpeedY = speed; }
    public void setCurrentAccelerationX(long acceleration) { this.currentAccelerationX = acceleration; }
    public void setCurrentAccelerationY(long acceleration) { this.currentAccelerationY = acceleration; }

    public long getX() {
        return super.getPositionX();
    }

    public long getY() {
        return super.getPositionY();
    }

    public void setX(long f) {
        super.setPositionX(f);
    }

    public void setY(long f) {
        super.setPositionY(f);
    }

    public PhysicTire clone() {
        return new PhysicTire(maxAcceleration, maxDeceleration, maxForce, maxSpeed, super.getModel(), super.getRadius(), super.getPositionX(), super.getPositionY());
    }
    
    public static boolean isValidTire(PhysicTire tire, long  width, long height, long distBorder, List<PhysicTire> tires, long distTire) {
        long x = tire.getPositionX();
        long y = tire.getPositionY();
        long r = tire.getRadius();
        for (Tire otherTire : tires) {
            if (otherTire == tire) {
                continue;
            }
            long otherX = otherTire.getPositionX();
            long otherY = otherTire.getPositionY();
            long distance = Math.round(Math.sqrt((x - otherX) * (x - otherX) + (y - otherY) * (y - otherY)));
            if (distance < r + otherTire.getRadius() + distTire - 1) {
                System.err.println("distancia" + distance + "r" + r + "otherTire.getRadius()" + otherTire.getRadius() + "distTire" + distTire);
                return false;
            }
        }
        return x - r >= distBorder && x + r <= width - distBorder && y - r >= distBorder && y + r <= height - distBorder;
    }
}
