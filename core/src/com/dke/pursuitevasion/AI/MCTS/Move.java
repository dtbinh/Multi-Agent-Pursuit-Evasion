package com.dke.pursuitevasion.AI.MCTS;

import com.badlogic.gdx.math.Vector3;

/**
 * Created by imabeast on 6/13/17.
 */

public class Move {

    private int x,y;
    private boolean[][] bMap;

    public Move (int x, int y, boolean[][] bMap) {
        // x = 0 means center, x = 1 means right, x = -1 means left, y = -1 means down, y = 1 means up
        this.x = x;
        this.y = y;
        this.bMap = bMap;
    }

    public Vector3 translateMovement() {
        Vector3 vector = new Vector3();
        // PathFinder pF = new PathFinder(map)
        // transform using PathFinder.positionFromIndex(int x, int z, AStarPathFinder ASPathFinder) to get the world coordinates corresponding to your b-map coordinates
        return vector;
    }

}
