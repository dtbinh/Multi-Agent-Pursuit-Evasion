package com.dke.pursuitevasion.AI.PotentialField;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.dke.pursuitevasion.AI.CustomPoint;
import com.dke.pursuitevasion.AI.PathFinder;
import com.dke.pursuitevasion.Entities.Components.StateComponent;
import com.dke.pursuitevasion.Entities.Components.agents.EvaderComponent;
import com.dke.pursuitevasion.Entities.Components.agents.PursuerComponent;
import com.dke.pursuitevasion.Entities.Mappers;
import com.dke.pursuitevasion.PolyMap;

import java.util.ArrayList;

/**
 * Created by imabeast on 6/19/17.
 */

public class PFMap {

    private int[][] obstacleMap;
    private int[][] heatMap;
    private Engine engine;
    private int bestValue;
    private Triplet coords;
    private EvaderComponent evader;
    private PathFinder pathFinder;
    private ImmutableArray<Entity> entities;
    private int heatSize;
    private int updateCounter = 0;
    public int mapSize = 250;
    public float gapSize = 0.1f;

    public PFMap(int[][] o, Engine e, PolyMap map, EvaderComponent evader, int heatSize){
        this.obstacleMap = o;
        this.engine = e;
        this.evader = evader;
        this.heatMap = new int[mapSize][mapSize];
        this.entities = engine.getEntitiesFor(Family.all(StateComponent.class).get());
        this.pathFinder = new PathFinder(map, mapSize, gapSize);
        this.heatSize = heatSize;
        System.out.println("Heatsize = " + heatSize);
    }

    public PFMap() {}

    public void updateEvader(EvaderComponent evader) {
        this.evader = evader;
    }

    public Vector3 getNextMove() {

        Vector3 v3 = evader.position;
        Vector2 v2 = coordsToArray(v3);

        Triplet triplet = findBestMove((int)v2.x, (int)v2.y);

        return new Vector3(arrayToCoords(new Vector2(triplet.getX(),triplet.getY())));
    }

    private void updateEntities() {
        this.entities = engine.getEntitiesFor(Family.all(StateComponent.class).get());
    }

    public void updateMap(){

        for (int a = 0; a < obstacleMap.length; a++) {
            for (int b = 0; b < obstacleMap[0].length; b++) {
                heatMap[a][b] = obstacleMap[a][b];
            }
        }

        ArrayList<Vector2> pursuerPositions = new ArrayList<Vector2>();
        Vector3 tempVector = new Vector3();

        updateEntities();
        for (Entity e : entities) {
            if (Mappers.pursuerMapper.has(e)) {
                PursuerComponent pursuerComponent = Mappers.pursuerMapper.get(e);
                tempVector = pursuerComponent.position;
                pursuerPositions.add(coordsToArray(tempVector));
            }
        }

        int counter = 1;
        for (int i = 0; i < mapSize; i++) {
            for (int j = 0; j < mapSize; j++) {
                if (pursuerPositions.contains(new Vector2((float)i, (float)j))) {
                    generatePotentialField(i,j,heatSize);
                    counter++;
                }
            }
        }

        //System.out.println("Generating " + counter + " potential field(s)");

        /*if (updateCounter % 10 == 0)
            printArray();*/

        updateCounter++;
    }

    private Vector2 coordsToArray(Vector3 vector3){

        Vector2 botCoords = new Vector2();

        float x = vector3.x;
        float y = vector3.z;

        CustomPoint customPoint = PathFinder.getNodeFromWorldCoor(x,y);
        if (customPoint == null)
            customPoint = pathFinder.approximatePosition(x,y);

        botCoords.set(customPoint.x, customPoint.y);

        return botCoords;
    }

    private Vector3 arrayToCoords(Vector2 vector2){
        return PathFinder.positionFromIndex((int)vector2.x, (int)vector2.y, pathFinder.pF);
    }

    private void generatePotentialField(int x, int y, int startValue){

        for (int i = 0; i < startValue; i++) {
            generateColumn(i,x,y,startValue);
            generateColumn(-i,x,y,startValue);
        }

    }

    private void generateColumn(int step, int centerX, int centerY, int startValue) {

        int tempCX = centerX;
        int tempCY = centerY - step;
        int var = startValue - Math.abs(step);

        for (int i = 0; i < var; i++) {
            try {
                if (heatMap[tempCX-i][tempCY] != 1000 && heatMap[tempCX-i][tempCY] < var-i)
                    heatMap[tempCX-i][tempCY] = var-i; } catch (ArrayIndexOutOfBoundsException e) {
            }
            try {
                if (heatMap[tempCX+i][tempCY] != 1000 && heatMap[tempCX+i][tempCY] < var-i)
                    heatMap[tempCX+i][tempCY] = var-i; } catch (ArrayIndexOutOfBoundsException e) {
            }
        }
    }

    private void switchCase(int firstCheck, int x, int y) {

        try {
            switch (firstCheck) {
                case 0:
                        if (heatMap[x-1][y] < bestValue){
                            bestValue = heatMap[x-1][y];
                            coords.setAll(x-1,y,bestValue);
                            evader.pfDirection = PFDirection.TOP;
                        }
                    break;
                case 1:
                        if (heatMap[x-1][y+1] < bestValue){
                            bestValue = heatMap[x-1][y+1];
                            coords.setAll(x-1,y+1,bestValue);
                            evader.pfDirection = PFDirection.TOP_RIGHT;
                        }
                    break;
                case 2:
                        if (heatMap[x][y+1] < bestValue){
                            bestValue = heatMap[x][y+1];
                            coords.setAll(x,y+1,bestValue);
                            evader.pfDirection = PFDirection.RIGHT;
                        }
                    break;
                case 3:
                        if (heatMap[x+1][y+1] < bestValue){
                            bestValue = heatMap[x+1][y+1];
                            coords.setAll(x+1,y+1,bestValue);
                            evader.pfDirection = PFDirection.BOTTOM_RIGHT;
                        }
                    break;
                case 4:
                        if (heatMap[x+1][y] < bestValue){
                            bestValue = heatMap[x+1][y];
                            coords.setAll(x+1,y,bestValue);
                            evader.pfDirection = PFDirection.DOWN;
                        }
                    break;
                case 5:
                        if (heatMap[x+1][y-1] < bestValue){
                            bestValue = heatMap[x+1][y-1];
                            coords.setAll(x+1,y-1,bestValue);
                            evader.pfDirection = PFDirection.BOTTOM_LEFT;
                        }
                    break;
                case 6:
                        if (heatMap[x][y-1] < bestValue){
                            bestValue = heatMap[x][y-1];
                            coords.setAll(x,y-1,bestValue);
                            evader.pfDirection = PFDirection.LEFT;
                        }
                    break;
                case 7:
                        if (heatMap[x-1][y-1] < bestValue){
                            bestValue = heatMap[x-1][y-1];
                            coords.setAll(x-1,y-1,bestValue);
                            evader.pfDirection = PFDirection.TOP_LEFT;
                        }
                    break;
                default:
                    System.out.println("ERROR");
            }} catch (ArrayIndexOutOfBoundsException e) {
        }
    }

    private Triplet findBestMove(int x, int y) {

        bestValue = 999;
        coords = new Triplet(x,y);

        if (evader.first) {
        regularCheck(x,y);
        evader.first = false;
        evader.previousPosition = coords;
        } else if (evader.previousPosition.getX() == coords.getX() && evader.previousPosition.getY() == coords.getY()) {
            System.out.println("***************************************************** BOT WAS STUCK *****************************************************");
            explore(x,y);
            evader.evaderPath = null;
            evader.position = arrayToCoords(new Vector2(coords.getX(),coords.getY()));
        } else {
            explore(x, y);
        }

        evader.previousPosition = new Triplet(x,y);

        return coords;
    }

    private void explore(int x, int y) {

        boolean set = false;
        if (closeToWall(x,y))
            regularCheck(x,y);
        else {
            try {
                if (evader.pfDirection == PFDirection.TOP && heatMap[x - 1][y] == 0) {
                    coords.setAll(x-1,y,0);
                    set = true;
                }
                } catch (ArrayIndexOutOfBoundsException e) {}
            try {
                if (evader.pfDirection == PFDirection.TOP_RIGHT && heatMap[x-1][y+1] == 0) {
                    coords.setAll(x-1,y+1,0);
                    set = true;
            }
                } catch (ArrayIndexOutOfBoundsException e) {}
            try {if (evader.pfDirection == PFDirection.RIGHT && heatMap[x][y+1] == 0) {
                    coords.setAll(x,y+1,0);
                    set = true;
            }
                } catch (ArrayIndexOutOfBoundsException e) {}
            try {if (evader.pfDirection == PFDirection.BOTTOM_RIGHT && heatMap[x+1][y+1] == 0) {
                    coords.setAll(x+1,y+1,0);
                    set = true;
            }
                } catch (ArrayIndexOutOfBoundsException e) {}
            try {if (evader.pfDirection == PFDirection.DOWN && heatMap[x+1][y] == 0) {
                    coords.setAll(x+1,y,0);
                    set = true;
            }
                } catch (ArrayIndexOutOfBoundsException e) {}
            try {if (evader.pfDirection == PFDirection.BOTTOM_LEFT && heatMap[x+1][y-1] == 0) {
                    coords.setAll(x+1,y-1,0);
                    set = true;
            }
                } catch (ArrayIndexOutOfBoundsException e) {}
            try {if (evader.pfDirection == PFDirection.LEFT && heatMap[x][y-1] == 0) {
                    coords.setAll(x,y-1,0);
                    set = true;
            }
                } catch (ArrayIndexOutOfBoundsException e) {}
            try {if (evader.pfDirection == PFDirection.TOP_LEFT && heatMap[x-1][y-1] == 0) {
                    coords.setAll(x-1,y-1,0);
                    set = true;
            }
                } catch (ArrayIndexOutOfBoundsException e) {}
            if (!set)
                regularCheck(x,y);
        }
    }

    private boolean closeToWall(int x, int y) {

        try {
            if (heatMap[x-1][y] == 1000)
                return true;
        } catch (ArrayIndexOutOfBoundsException e) {}
        try {
            if (heatMap[x-1][y+1] == 1000)
                return true;
        } catch (ArrayIndexOutOfBoundsException e) {}
        try {
            if (heatMap[x][y+1] == 1000)
                return true;
        } catch (ArrayIndexOutOfBoundsException e) {}
        try {
            if (heatMap[x+1][y+1] == 1000)
                return true;
        } catch (ArrayIndexOutOfBoundsException e) {}
        try {
            if (heatMap[x+1][y] == 1000)
                return true;
        } catch (ArrayIndexOutOfBoundsException e) {}
        try {
            if (heatMap[x+1][y-1] == 1000)
                return true;
        } catch (ArrayIndexOutOfBoundsException e) {}
        try {
            if (heatMap[x][y-1] == 1000)
                return true;
        } catch (ArrayIndexOutOfBoundsException e) {}
        try {
            if (heatMap[x-1][y-1] == 1000)
                return true;
        } catch (ArrayIndexOutOfBoundsException e) {}

        return false;
    }

    private void regularCheck(int x, int y) {

        int firstCheck = (int)(Math.ceil((7*Math.random())));
        switchCase(firstCheck,x,y);
        boolean dangerDetected = false;

        try {
            if (heatMap[x-1][y] < bestValue){
                bestValue = heatMap[x-1][y];
                coords.setAll(x-1,y,bestValue);
                if (heatMap[x-1][y] > 0 && heatMap[x-1][y] < 1000)
                    dangerDetected = true;
                evader.pfDirection = PFDirection.TOP;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
        }

        try {
            if (heatMap[x-1][y+1] < bestValue){
                bestValue = heatMap[x-1][y+1];
                coords.setAll(x-1,y+1,bestValue);
                if (heatMap[x-1][y+1] > 0 && heatMap[x-1][y+1] < 1000)
                    dangerDetected = true;
                evader.pfDirection = PFDirection.TOP_RIGHT;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
        }

        try {
            if (heatMap[x][y+1] < bestValue){
                bestValue = heatMap[x][y+1];
                coords.setAll(x,y+1,bestValue);
                if (heatMap[x][y+1] > 0 && heatMap[x][y+1] < 1000)
                    dangerDetected = true;
                evader.pfDirection = PFDirection.RIGHT;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
        }

        try {
            if (heatMap[x+1][y+1] < bestValue){
                bestValue = heatMap[x+1][y+1];
                coords.setAll(x+1,y+1,bestValue);
                if (heatMap[x+1][y+1] > 0 && heatMap[x+1][y+1] < 1000)
                    dangerDetected = true;
                evader.pfDirection = PFDirection.BOTTOM_RIGHT;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
        }

        try {
            if (heatMap[x+1][y] < bestValue){
                bestValue = heatMap[x+1][y];
                coords.setAll(x+1,y,bestValue);
                if (heatMap[x+1][y] > 0 && heatMap[x+1][y] < 1000)
                    dangerDetected = true;
                evader.pfDirection = PFDirection.DOWN;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
        }

        try {
            if (heatMap[x+1][y-1] < bestValue){
                bestValue = heatMap[x+1][y-1];
                coords.setAll(x+1,y-1,bestValue);
                if (heatMap[x+1][y-1] > 0 && heatMap[x+1][y-1] < 1000)
                    dangerDetected = true;
                evader.pfDirection = PFDirection.BOTTOM_LEFT;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
        }

        try {
            if (heatMap[x][y-1] < bestValue){
                bestValue = heatMap[x][y-1];
                coords.setAll(x,y-1,bestValue);
                if (heatMap[x][y-1] > 0 && heatMap[x][y-1] < 1000)
                    dangerDetected = true;
                evader.pfDirection = PFDirection.LEFT;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
        }

        try {
            if (heatMap[x-1][y-1] < bestValue){
                bestValue = heatMap[x-1][y-1];
                coords.setAll(x-1,y-1,bestValue);
                if (heatMap[x-1][y-1] > 0 && heatMap[x-1][y-1] < 1000)
                    dangerDetected = true;
                evader.pfDirection = PFDirection.TOP_LEFT;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
        }

        if (dangerDetected)
            System.out.println("Danger detected");
    }

    private void printArray() {

        for (int i = 0; i < heatMap.length; i++) {
            for (int j = 0; j < heatMap[0].length; j++) {
                System.out.print(heatMap[j][i] + "  ");
            }
            System.out.println();
        }

    }
}
