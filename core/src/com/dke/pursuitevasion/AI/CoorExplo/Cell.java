package com.dke.pursuitevasion.AI.CoorExplo;

import com.badlogic.gdx.math.Vector3;

import java.util.ArrayList;

/**
 * Created by Envy on 6/20/2017.
 */
public class Cell {
    public boolean frontier;
    public boolean ignore;
    public boolean explored;
    public double openness;
    public double cost;
    public double utility;
    public double distanceToClosestWall;
    public int x, y, visibleCounter;
    public Vector3 position;
    //HashMap visibleToPursuers = new HashMap();
    ArrayList<Integer> visibleToPursuers= new ArrayList<Integer>();

    public Cell(int X, int Y){
        x = X;
        y = Y;
        frontier = false;
        explored = false;
        ignore = false;
        openness = -1;
        cost = 0;
        utility = -1;
        visibleCounter = 0;
    }

}
