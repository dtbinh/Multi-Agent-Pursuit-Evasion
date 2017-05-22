package com.dke.pursuitevasion.AI;

/**
 * Created by Envy on 4/20/2017.
 */
public class NodeState {
    int width, height;
    boolean nodeGrid[][];

    public NodeState(int Width, int Height){
        width = Width;
        height = Height;
        nodeGrid = new boolean[width][height];
    }

    public boolean[][] getNodeGrid(){
        return nodeGrid;
    }
}
