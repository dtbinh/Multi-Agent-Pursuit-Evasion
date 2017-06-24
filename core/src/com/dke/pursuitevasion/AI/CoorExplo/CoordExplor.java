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
    float mViewDistance;
    PathFinder pathFinder;
    double stepSize = 5;
    int pursCount;


    public CoordExplor(PolyMap Map, float Gap, int Width, int pursuerCount, PathFinder pathFinder){
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

    public void updateGrid(PursuerComponent pursuerComponent, ObserverComponent observerComponent){
        if(pursuerComponent.updatePosition && pursuerComponent.position!=null) {
            pursuerPos[pursuerComponent.number] = pursuerComponent.position;
        }
        if(!pursuerComponent.updatePosition && pursuerComponent.position.dst(pursuerPos[pursuerComponent.number])<0.15f){
            pursuerComponent.updatePosition = true;
        }
        if(!pursuerComponent.updatePosition){
            if(pursuerComponent.targetCell!= null && !pursuerComponent.targetCell.frontier){
                requestNewFrontier(pursuerComponent);
            }
        }
        //nodes.clear();
        pursuerComponent.frontierCells.clear();
        CustomPoint pursuerPosition = discretiser.getNodeFromWorldCoor(pursuerComponent.position.x, pursuerComponent.position.z);
        if(pursuerPosition == null){
            pursuerPosition = PathFinder.approximatePosition(pursuerComponent.position.x, pursuerComponent.position.z);
        }
        int pursuerNodeX = pursuerPosition.x;
        int pursuerNodeY = pursuerPosition.y;
        int viewNode = (int) (Math.ceil(observerComponent.distance/discretiser.gap)/2);
        viewNode*=1.5;

        //add new explored nodes to explored
        for(int i=-0; i < mWidth; i++){
            for(int j = 0; j < mWidth; j++){
                if(i>1 && i<discretiser.width-1 && j>1 && j<discretiser.width-1) {
                    Cell cell = cellGrid[i][j];
                    cell.cost = 0;
                    boolean contains = false;
                    int pursuerIndex = -1;
                    for(int k = 0; k<cell.visibleToPursuers.size(); k++){
                        if(!isVisible(cell.position, pursuerComponent, observerComponent, i, j) && pursuerComponent.number == cell.visibleToPursuers.get(k)){
                            pursuerIndex  = k;
                            contains = true;
                            break;
                        }
                    }
                    if(contains){
                        cell.visibleToPursuers.remove(pursuerIndex);
                        cell.visibleCounter--;
                    }

                    //check if a cell is explored
                    if (!cell.explored && isVisible(cell.position, pursuerComponent, observerComponent, i, j)) {
                        cell.explored = true;
                        ModelInstance mBox = new ModelInstance(box, cell.position);
                        nodes.add(mBox);
                    }
                }
            }
        }
        //searching local within pursuer vision
        for(int i=-viewNode; i < viewNode+1; i++) {
            for (int j = -viewNode; j < viewNode + 2; j++) {
                //make sure your are still searching within of the map
                if (i + pursuerNodeX > 0 && i + pursuerNodeX < discretiser.width && j + pursuerNodeY > 0 && j + pursuerNodeY < discretiser.width) {
                    Cell cell = cellGrid[i+pursuerNodeX][j+pursuerNodeY];
                    //compute new frontier cells
                    if (cell.explored == true) {
                        boolean trigger = false;
                        outerloop:
                        for (int k = -1; k < 2; k++) {
                            for (int l = -1; l < 2; l++) {
                                //within bounds of map
                                if (cell.x + k < discretiser.width && cell.x > 1 && cell.y + l < discretiser.width && cell.y + l > 1) {
                                    //cell is unexplored and inside map
                                    if ((k != 0 || l != 0) && !cellGrid[cell.x + k][cell.y + l].explored && !cellGrid[cell.x + k][cell.y + l].ignore) {
                                        if(!discretiser.lineIntersectWall(cell.position, pursuerComponent.position)) {
                                            cell.frontier = true;
                                            if (!unexploredFrontier.containsKey(cell)) {
                                                unexploredFrontier.put(cell, true);
                                            }
                                            trigger = true;
                                            break outerloop;
                                        }
                                    }
                                }
                            }
                        }
                        //KEEPING track of frontier cells
                        if(!trigger){
                            if(unexploredFrontier.containsKey(cell)){
                                unexploredFrontier.remove(cell);
                            }
                            cell.frontier = false;
                        }
                    }

                    if (cell.frontier) {
                        pursuerComponent.frontierCells.add(cell);
                        //ModelInstance mBox = new ModelInstance(expBox, cell.position);
                        //nodes.add(mBox);

                        /*if (!cell.visibleToPursuers.containsKey(pursuerComponent.number)) {
                            cell.visibleToPursuers.put(pursuerComponent.number, true);
                            cell.visibleCounter++;
                        }*/
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

                        //utility is not changing so we only have to compute it once
                        if (!runOnce) {
                            float viewDistance = observerComponent.distance * 0.75f;
                            mViewDistance = observerComponent.distance;
                            if (cell.distanceToClosestWall < viewDistance) {
                                cell.utility = 1 - cell.distanceToClosestWall / viewDistance;
                            } else {
                                cell.utility = 0;
                            }
                            runOnce = true;
                        }
                    }
                }
            }
        }
    }

    public boolean isVisible(Vector3 point, PursuerComponent pursuerComponent, ObserverComponent observerComponent, int x, int y){
        //check if a node is visible
        float distance = point.dst(pursuerComponent.position);
        if(distance < observerComponent.distance/2 && discretiser.nodeGrid[x][y]) {
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
        float beta = 1;
        Cell bestCell = new Cell(0,0);
        if(pursuerComponent.frontierCells.size()>0) {
            for (int i = 0; i < pursuerComponent.frontierCells.size(); i++) {
                //float cost = pursuerComponent.position.dst(pursuerComponent.frontierCells.get(i).position);
                int x = pursuerComponent.frontierCells.get(i).x;
                int y = pursuerComponent.frontierCells.get(i).y;
                float cost = (float) pursuerComponent.costs[x][y];
                cost *= pursuerComponent.frontierCells.get(i).openness;
                float score = (float) (pursuerComponent.frontierCells.get(i).utility + beta * cost);
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
                CXPoint end = new CXPoint(bestCell.position.x, bestCell.position.z);
                pursuerComponent.pursuerPointPath = discretizePath(start, end);
            }
        }else{
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
        }
    }

    public Cell requestNewFrontier(PursuerComponent pursuerComponent){
        Cell bestCell = null;
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
            ModelInstance mBox = new ModelInstance(expBox, bestCell.position);
            nodes.add(mBox);
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
        double steps = distance/gap;

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

    /* public void utilityToString(){
        for(int i=0; i<cellGrid.length; i++){
            for(int j=0; j<cellGrid.length; j++){
                if(cellGrid[j][i].utility!=-1){
                    System.out.print((int) Math.floor(cellGrid[j][i].utility* 10)+" ");
                }
                else {
                    System.out.print("  ");
                }
            }
            System.out.println();
        }
    }*/

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

    public ArrayList<CXPoint> addAdditionalSteps(List<Node> p){
        ArrayList<CXPoint> pointPath= new ArrayList<CXPoint>();
        float diagStepSize= (float) Math.floor(1.4*stepSize);
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
                    for (float j = 1; j < stepSize; j++) {
                        double scale = j / stepSize;
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