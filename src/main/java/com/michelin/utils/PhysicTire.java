package com.michelin.utils;

import java.util.List;

public class PhysicTire extends Tire{
    private long currentForceX;
    private long currentForceY;
    private long currentSpeedX;
    private long currentSpeedY;
    private long currentAccelerationX;
    private long currentAccelerationY;

    public PhysicTire(String model, long radius, long x, long y) {
        super(model, radius, x, y);
    }
    
    // Getters y setters para las variables de física
    public long getCurrentForceX() { return currentForceX; }
    public long getCurrentForceY() { return currentForceY; }
    public long getCurrentSpeedX() { return currentSpeedX; }
    public long getCurrentSpeedY() { return currentSpeedY; }
    public long getCurrentAccelerationX() { return currentAccelerationX; }
    public long getCurrentAccelerationY() { return currentAccelerationY; }
    
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

    @Override
    public PhysicTire clone() {
        PhysicTire clone = new PhysicTire(String.valueOf(super.getModel()), super.getRadius(), super.getPositionX(), super.getPositionY());
        clone.setCurrentForceX(this.currentForceX);
        clone.setCurrentForceY(this.currentForceY);
        clone.setCurrentSpeedX(this.currentSpeedX);
        clone.setCurrentSpeedY(this.currentSpeedY);
        clone.setCurrentAccelerationX(this.currentAccelerationX);
        clone.setCurrentAccelerationY(this.currentAccelerationY);
        return clone;
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
                return false;
            }
        }
        return x - r >= distBorder && x + r <= width - distBorder && y - r >= distBorder && y + r <= height - distBorder;
    }
}
