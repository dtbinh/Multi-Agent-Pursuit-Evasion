package com.dke.pursuitevasion.AI.CoorExplo;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.dke.pursuitevasion.AI.CustomPoint;
import com.dke.pursuitevasion.AI.Node;
import com.dke.pursuitevasion.AI.PathFinder;
import com.dke.pursuitevasion.CellDecompose.Graph.CXPoint;
import com.dke.pursuitevasion.Entities.Components.ObserverComponent;
import com.dke.pursuitevasion.Entities.Components.agents.PursuerComponent;
import com.dke.pursuitevasion.PolyMap;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * Created by Envy on 6/19/2017.
 */
public class CoordExplor {
    public Discretiser discretiser;
    public Cell[][] cellGrid;
    public float gap;
    public int mWidth;
    public ArrayList<ModelInstance> nodes =  new ArrayList<ModelInstance>();
    Model box, expBox;
    Random random = new Random();
    public boolean runOnce = false;
    public HashMap<Cell, Boolean> unexploredFrontier = new HashMap<Cell, Boolean>();
    Vector3[] pursuerPos;
    PathFinder pathFinder;
    double stepSize = 6;
    int pursCount;


    public CoordExplor(PolyMap Map, float Gap, int Width, int pursuerCount, PathFinder pathFinder){
        System.out.println("# of pursuers  " +pursuerCount);
        gap = Gap;
        mWidth = Width;
        cellGrid = new Cell[Width][Width];
        discretiser = new Discretiser(Map, Gap, Width);
        pursCount = pursuerCount;
        pursuerPos = new Vector3[pursuerCount];
        this.pathFinder = pathFinder;
        for(int i=0;i<Width;i++){
            for(int j=0;j<Width;j++){
                cellGrid[i][j] = new Cell(i,j);
                int index = Width*i+j;
                float x = discretiser.pF.allNodes.get(discretiser.pF.CC.get(index)).worldX;
                float z = discretiser.pF.allNodes.get(discretiser.pF.CC.get(index)).worldZ;
                cellGrid[i][j].position = new Vector3(x, 0f, z);
                if(!discretiser.nodeGrid[i][j]){
                    cellGrid[i][j].ignore = true;
                }
            }
        }


        computeOpenness();
        ModelBuilder modelBuilder = new ModelBuilder();
        box = modelBuilder.createBox(0.05f, 0.02f, 0.05f,new Material(ColorAttribute.createDiffuse(Color.RED)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        expBox = modelBuilder.createBox(0.05f, 0.02f, 0.05f,new Material(ColorAttribute.createDiffuse(Color.GREEN)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);

    }

    public void restart(){
        for(int i=0;i<mWidth;i++){
            for(int j=0;j<mWidth;j++){
                Cell cell = cellGrid[i][j];
                cell.frontier = false;
                cell.visibleCounter = 0;
                cell.visibleToPursuers.clear();
                cell.explored = false;
            }
        }

    }

    public void updateGrid(PursuerComponent pursuerComponent, ObserverComponent observerComponent) {
        //System.out.println(pursuerComponent.number);
        if (pursuerComponent.updatePosition && pursuerComponent.position != null) {
            System.out.println(pursuerComponent.number);
            pursuerPos[pursuerComponent.number] = pursuerComponent.position;
        }
        //if approaching a wall, get new task
        if(pursuerComponent.updatePosition && pursuerComponent.targetCell!= null && !pursuerComponent.targetCell.frontier){
            //System.out.println("Target explored before reaching");
            //assignTask(pursuerComponent);
        }

        if (!pursuerComponent.updatePosition) {
            if (pursuerComponent.targetCell != null && !pursuerComponent.targetCell.frontier) {
                assignTask(pursuerComponent);
            }
        }

        if (!pursuerComponent.updatePosition && pursuerComponent.position.dst(pursuerPos[pursuerComponent.number]) < 0.05f) {
            pursuerComponent.updatePosition = true;
        }


        //nodes.clear();
        pursuerComponent.frontierCells.clear();
        CustomPoint pursuerPosition = discretiser.getNodeFromWorldCoor(pursuerComponent.position.x, pursuerComponent.position.z);
        if (pursuerPosition == null) {
            pursuerPosition = pathFinder.approximatePosition(pursuerComponent.position.x, pursuerComponent.position.z);
        }
        int pursuerNodeX = pursuerPosition.x;
        int pursuerNodeY = pursuerPosition.y;
        int viewNode = (int) (Math.ceil(observerComponent.distance*1.5f / discretiser.gap));

        //add new explored nodes to explored
        for (int i = 0; i < mWidth; i++) {
            for (int j = 0; j < mWidth; j++) {
                Cell cell = cellGrid[i][j];
                cell.cost = 0;
                boolean contains = false;
                //REMOVE all visible pursuers from the cell
                int pursuerIndex = -1;
                for (int k = 0; k < cell.visibleToPursuers.size(); k++) {
                    if (pursuerComponent.number == cell.visibleToPursuers.get(k) && !isVisible(cell.position, pursuerComponent, observerComponent, i, j)) {
                        pursuerIndex = k;
                        contains = true;
                        break;
                    }
                }
                if (contains) {
                    cell.visibleToPursuers.remove(pursuerIndex);
                    cell.visibleCounter--;
                }

                //check if a cell is explored
                if (!cell.explored && isVisible(cell.position, pursuerComponent, observerComponent, i, j)) {
                    cell.explored = true;
                    ModelInstance m = new ModelInstance(box, cell.position);
                    nodes.add(m);
                }

                //utility is not changing so we only have to compute it once
                if (!runOnce) {
                    if (cell.distanceToClosestWall < observerComponent.distance) {
                        cell.utility = 1 - cell.distanceToClosestWall / observerComponent.distance;
                        if(cell.utility>0.3){
                            cell.utility = 1-cell.utility;
                        }
                    } else {
                        cell.utility = 0;
                    }
                }
            }
        }
        runOnce = true;
        if(runOnce){
            //utilityToString();
        }

        //searching local within pursuer vision
        for (int i = -viewNode; i < viewNode; i++) {
            for (int j = -viewNode; j < viewNode; j++) {
                //make sure youre are still searching within of the map
                if (i + pursuerNodeX > 0 && i + pursuerNodeX < discretiser.width - 1 && j + pursuerNodeY > 0 && j + pursuerNodeY < discretiser.width - 1) {
                    Cell cell = cellGrid[i + pursuerNodeX][j + pursuerNodeY];
                    //compute new frontier cells
                    if (cell.explored == true) {
                        boolean trigger = false;
                        outerloop:
                        for (int k = -1; k < 2; k++) {
                            for (int l = -1; l < 2; l++) {
                                //within bounds of map
                                if (cell.x+k < discretiser.width && cell.x+k >0 && cell.y+l < discretiser.width && cell.y+l > 0) {
                                    //cell is unexplored and inside map
                                    if ((k != 0 || l != 0) && !cellGrid[cell.x + k][cell.y + l].explored && !cellGrid[cell.x + k][cell.y + l].ignore) {
                                        //if(isVisible(cell.position, pursuerComponent, observerComponent, cell.x,cell.y)){
                                            //if (!discretiser.lineIntersectWall(cell.position, pursuerComponent.position)) {
                                            cell.frontier = true;
                                            if (!unexploredFrontier.containsKey(cell)) {
                                                unexploredFrontier.put(cell, true);
                                            }
                                            trigger = true;
                                            break outerloop;
                                        //}
                                    }
                                }
                            }
                        }
                        //KEEPING track of frontier cells
                        if (!trigger) {
                            if (unexploredFrontier.containsKey(cell)) {
                                unexploredFrontier.remove(cell);
                            }
                            cell.frontier = false;
                        }
                    }

                    if (cell.frontier && isVisible(cell.position, pursuerComponent, observerComponent, cell.x,cell.y)) {
                        pursuerComponent.frontierCells.add(cell);
                        boolean contains = false;
                        for (int k = 0; k < cell.visibleToPursuers.size(); k++) {
                            if (pursuerComponent.number == cell.visibleToPursuers.get(k)) {
                                contains = true;
                                break;
                            }
                        }
                        if (!contains) {
                            cell.visibleToPursuers.add(pursuerComponent.number);
                            cell.visibleCounter++;
                        }

                        float distance = pursuerComponent.position.dst(cell.position);

                        //cost does not exist for a single cell, need to store this in pursuers
                        cell.cost = distance;
                        if (cell.visibleCounter > 1) {
                            cell.cost /= cell.visibleCounter * 2;
                        }

                        pursuerComponent.costs[i + pursuerNodeX][j + pursuerNodeY] = distance;
                        if (pursuerComponent.number != cell.visibleToPursuers.get(0)) {
                            pursuerComponent.costs[i + pursuerNodeX][j + pursuerNodeY] /= cell.visibleCounter;
                        }
                    }
                }
            }
        }
        /*for (Cell key : unexploredFrontier.keySet()) {
            Vector3 position = new Vector3(key.position);
            ModelInstance m = new ModelInstance(expBox, position);
            nodes.add(m);
        }*/
    }

    public boolean isVisible(Vector3 point, PursuerComponent pursuerComponent, ObserverComponent observerComponent, int x, int y){
        //check if a node is visible
        float distance = point.dst(pursuerComponent.position);
        if(discretiser.nodeGrid[x][y] && distance < observerComponent.distance) {
            if(!discretiser.lineIntersectWall(point, pursuerComponent.position)) {
                return true;
            }
        }
        return false;
    }

    public void computeOpenness(){
        //compute openess and closest wall values only once because they can never change
        for(int i = 0; i < cellGrid.length; i++){
            for(int j = 0; j < cellGrid.length; j++){
                cellGrid[i][j].openness = discretiser.calcOpenness(i,j);
                int index = discretiser.width*i+j;
                float x = discretiser.pF.allNodes.get(discretiser.pF.CC.get(index)).worldX;
                float z = discretiser.pF.allNodes.get(discretiser.pF.CC.get(index)).worldZ;
                Vector3 point = new Vector3(x, 0f, z);
                cellGrid[i][j].position = point;
                cellGrid[i][j].distanceToClosestWall = wallDist(point);
            }
        }
    }

    public double wallDist(Vector3 point){
        return discretiser.getDistToWall(point.x, point.z);
    }

    public void assignTask(PursuerComponent pursuerComponent){
        float maxScore = -Float.MAX_VALUE;
        float beta = 1f;
        Cell bestCell = new Cell(0,0);
        if(pursuerComponent.frontierCells.size()>0) {
            for (int i = 0; i < pursuerComponent.frontierCells.size(); i++) {
                //float cost = pursuerComponent.position.dst(pursuerComponent.frontierCells.get(i).position);
                int x = pursuerComponent.frontierCells.get(i).x;
                int y = pursuerComponent.frontierCells.get(i).y;
                float cost = (float) pursuerComponent.costs[x][y];
                cost *= pursuerComponent.frontierCells.get(i).openness;
                float score = (float) (beta*pursuerComponent.frontierCells.get(i).utility + cost);

                if (score > maxScore) {
                    maxScore = score;
                    bestCell = pursuerComponent.frontierCells.get(i);
                }
                //if score is equal, 50% chance to select this cell so path isn't always the same
                if (score == maxScore && random.nextBoolean()) {
                    bestCell = pursuerComponent.frontierCells.get(i);
                }
            }
            CXPoint start = new CXPoint(pursuerComponent.position.x, pursuerComponent.position.z);
            if(bestCell != null && bestCell.x!=0 && bestCell.y!=0) {
                pursuerComponent.targetCell  = bestCell;
                CXPoint end = new CXPoint(bestCell.position.x, bestCell.position.z);
                if(!discretiser.lineIntersectWall(pursuerComponent.position, bestCell.position)){
                    pursuerComponent.pursuerPointPath = discretizePath(start, end);
                }else{
                    Vector3 vStart = new Vector3((float) start.x, 0,(float) start.y);
                    Vector3 vEnd = new Vector3((float) end.x, 0,(float) end.y);
                    pursuerComponent.pursuerPointPath = addAdditionalSteps(pathFinder.findPath(vStart, vEnd, null));
                }
            }
            ModelInstance m = new ModelInstance(expBox, bestCell.position);
            nodes.add(m);
        }else{
            //System.out.println("REQUEST NEW");
            bestCell = requestNewFrontier(pursuerComponent);
            CXPoint start = new CXPoint(pursuerComponent.position.x, pursuerComponent.position.z);
            if(bestCell != null && bestCell.x!=0 && bestCell.y!=0) {
                CXPoint end = new CXPoint(bestCell.position.x, bestCell.position.z);

                if(!discretiser.lineIntersectWall(pursuerComponent.position, bestCell.position)){
                    pursuerComponent.pursuerPointPath = discretizePath(start, end);
                }else{
                    Vector3 vStart = new Vector3((float) start.x, 0,(float) start.y);
                    Vector3 vEnd = new Vector3((float) end.x, 0,(float) end.y);
                    pursuerComponent.pursuerPointPath = addAdditionalSteps(pathFinder.findPath(vStart, vEnd, null));
                }
            }
            if(bestCell!=null && bestCell.position!=null) {
                ModelInstance m = new ModelInstance(expBox, bestCell.position);
                nodes.add(m);
            }
        }
    }

    public Cell requestNewFrontier(PursuerComponent pursuerComponent){
        Cell bestCell = null;
        System.out.println();
        float maxDistFromPursuers = -1*Float.MAX_VALUE;
        for (Cell key : unexploredFrontier.keySet()) {
            float minCellDistToPursuer = Float.MAX_VALUE;
            //Calcing maximum min distance to other pursuers
            for(int i = 0; i < pursuerPos.length; i++){
                float pursuerDistance = key.position.dst(pursuerPos[i]);
                if(pursuerDistance < minCellDistToPursuer){
                    minCellDistToPursuer = pursuerDistance;
                }
            }
            if(minCellDistToPursuer > maxDistFromPursuers){
                maxDistFromPursuers = minCellDistToPursuer;
                bestCell = key;
            }
        }
        if(bestCell!=null) {
            pursuerPos[pursuerComponent.number] = bestCell.position;
        }
        pursuerComponent.updatePosition = false;
        pursuerComponent.targetCell = bestCell;
        return bestCell;
    }

    private ArrayList<CXPoint> discretizePath(CXPoint start, CXPoint end){
        ArrayList<CXPoint> path = new ArrayList<CXPoint>();
        double distX = end.x - start.x;
        double distY = end.y - start.y;
        double distance = Math.sqrt((distX*distX)+(distY*distY));
        double steps = distance/pathFinder.gapSize;

        for(int i=0;i<steps*stepSize;i++){
            double scale = i/(steps*stepSize);
            BigDecimal sc = BigDecimal.valueOf(scale);
            BigDecimal xX = BigDecimal.valueOf(distX);
            BigDecimal zZ = BigDecimal.valueOf(distY);
            BigDecimal newX = sc.multiply(xX);
            BigDecimal newZ = sc.multiply(zZ);
            double bDX = newX.doubleValue();
            double bDZ = newZ.doubleValue();
            CXPoint position = new CXPoint(start.x+bDX, start.y+bDZ);
            path.add(position);
        }
        return path;
    }

    public void costToString(int viewNode, int k, int l, PursuerComponent pursuerComponent){
        for(int i=-viewNode; i < viewNode+1; i++) {
            for (int j = -viewNode; j < viewNode + 2; j++) {
                if(pursuerComponent.costs[j+k][i+l] !=0) {
                    System.out.print(" " + (int) Math.floor(pursuerComponent.costs[j + k][i + l] * 10) + " ");
                }else{
                    System.out.print("   ");
                }
            }
            System.out.println();
        }
    }

   public void utilityToString(){
        for(int i=0; i<cellGrid.length; i++){
            for(int j=0; j<cellGrid.length; j++){
                if(!cellGrid[j][i].ignore && cellGrid[j][i].utility!=-1 ){
                    System.out.print(Math.floor(cellGrid[j][i].utility* 10)+" ");
                }
                else {
                    System.out.print("  ");
                }
            }
            System.out.println();
        }
       System.out.println();
       System.out.println();
    }

    public ArrayList<CXPoint> addAdditionalSteps(List<Node> p){
        ArrayList<CXPoint> pointPath= new ArrayList<CXPoint>();
        //Have to increate stepsize for pathfinder for it to be similar speed, no idea why
        float diagStepSize= (float) Math.floor(2*stepSize);
        int PFStepsize = (int) (stepSize*1.5);
        if(p.size()>1) {
            for (int i = 0; i < p.size() - 1; i++) {
                Vector3 start = new Vector3(p.get(i).worldX, p.get(i).worldY, p.get(i).worldZ);
                Vector3 end = new Vector3(p.get(i + 1).worldX, p.get(i + 1).worldY, p.get(i + 1).worldZ);
                float adjX = end.x - start.x;
                float adjZ = end.z - start.z;
                if(adjX!=0 && adjZ!=0){
                    for (float j = 1; j < diagStepSize; j++) {
                        double scale = j / diagStepSize;
                        BigDecimal sc = BigDecimal.valueOf(scale);
                        BigDecimal xX = BigDecimal.valueOf(adjX);
                        BigDecimal zZ = BigDecimal.valueOf(adjZ);
                        BigDecimal newX = sc.multiply(xX);
                        BigDecimal newZ = sc.multiply(zZ);
                        float bDX = newX.floatValue();
                        float bDZ = newZ.floatValue();
                        CXPoint position = new CXPoint(start.x+bDX, start.z+bDZ);
                        pointPath.add(position);
                    }
                }else {
                    for (float j = 1; j < PFStepsize; j++) {
                        double scale = j / PFStepsize;
                        BigDecimal sc = BigDecimal.valueOf(scale);
                        BigDecimal xX = BigDecimal.valueOf(adjX);
                        BigDecimal zZ = BigDecimal.valueOf(adjZ);
                        BigDecimal newX = sc.multiply(xX);
                        BigDecimal newZ = sc.multiply(zZ);
                        double bDX = newX.doubleValue();
                        double bDZ = newZ.doubleValue();
                        CXPoint position = new CXPoint(start.x+bDX, start.z+bDZ);
                        pointPath.add(position);
                    }
                }
            }
        }
        return pointPath;
    }
}