package com.dke.pursuitevasion.CXSearchingAlgorithm.CXAgentTaskType;

import com.dke.pursuitevasion.CellDecompose.Graph.CXPoint;

/**
 * Created by chenxi on 5/20/17.
 */
public class CXAgentMovingTask {
    public CXPoint movingDestination;
    public float radius = -1.0f;
    public CXAgentMovingTask(){};
    public CXAgentMovingTask(CXPoint movingDestination){
        this.movingDestination = movingDestination;
    }
    public CXAgentMovingTask(CXPoint movingDestination, float radius){
        this.movingDestination = movingDestination;
        this.radius = radius;
    }
}
