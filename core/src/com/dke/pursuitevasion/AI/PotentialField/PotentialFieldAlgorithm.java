package com.dke.pursuitevasion.AI.PotentialField;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.Vector3;
import com.dke.pursuitevasion.PolyMap;

/**
 * Created by imabeast on 6/19/17.
 */

public class PotentialFieldAlgorithm {

    private Engine engine;
    private boolean[][] bMap;
    private int[][] obstacleMap;
    private Entity evader;
    PolyMap map;
    // add option to change start value at some point if necessary

    public PotentialFieldAlgorithm(Engine e, boolean[][] b, PolyMap map, Entity evader){
        this.engine = e;
        this.bMap = b;
        this.map = map;
        this.evader = evader;
        generateInitialPFMap();
    }

    public void generateInitialPFMap(){

        int[][] map = new int[100][100];

        for (int i = 0; i < 100; i++) {
            for (int j = 0; j < 100; j++) {
                if (bMap[i][j] == false)
                    map[i][j] = 1000; // make sure this value is smaller than the smallest value within any potential field
            }
        }

        this.obstacleMap = map;
    }

    public Vector3 computePositionToMoveTo(){

        PFMap pfMap = new PFMap(obstacleMap, engine, map, evader);
        pfMap.updateMap();

        Vector3 v = pfMap.getNextMove();

        return v;
    }
}
