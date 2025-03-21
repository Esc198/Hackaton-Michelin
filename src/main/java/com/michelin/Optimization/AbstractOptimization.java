package com.michelin.Optimization;

import java.util.List;

import com.michelin.utils.Tire;

public interface AbstractOptimization {
    public void setup();
    public void run();
    public List<Tire> getResult();
    public boolean isFinished();

}
