package com.dke.pursuitevasion.CXSearchingAlgorithm;

import com.badlogic.gdx.math.Vector3;
import com.dke.pursuitevasion.AI.Node;
import com.dke.pursuitevasion.AI.PathFinder;
import com.dke.pursuitevasion.CXSearchingAlgorithm.CXAgentTaskType.*;
import com.dke.pursuitevasion.CXSearchingAlgorithm.CXMessage.*;
import com.dke.pursuitevasion.CellDecompose.Graph.*;
import com.dke.pursuitevasion.Entities.Components.StateComponent;
import com.dke.pursuitevasion.Entities.Components.agents.PursuerComponent;
import sun.misc.Queue;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by chenxi on 5/20/17.
 */
public class CXAgentUtility {

    public CXDecomposedGraphNode finalArea;

    public CXAgentUtility(){}


    public PursuerComponent checkMessage(LinkedList messageArrayList, PursuerComponent pursuerComponent){
        if (messageArrayList.isEmpty()) return pursuerComponent;

        for (int i = 0; i < messageArrayList.size() ; i++) {
            CXMessage message = (CXMessage)messageArrayList.get(i);
            if (message.receiver == -1 || message.receiver == pursuerComponent.number){
                System.out.println("Received message " + message.messageType );
                switch (message.messageType){
                    case CallBackUp:
                    {
                        // Transfer SearchingTask to movingTask + messageTask
                        messageArrayList.remove(i);
                        return transferBackupMessageToTask(pursuerComponent,message);
                    }
                }
            }
        }return pursuerComponent;
    }

    private PursuerComponent transferBackupMessageToTask(PursuerComponent pursuerComponent,CXMessage message){

        CXDecomposedGraphNode node = message.messageContent.searchTask.searchArea;
        CXGraphNode topLeftNode = node.getTopLeftNode();
        CXPoint destination = CXPoint.convertToOriginalCoordination(topLeftNode.location);

        // 1. Moving to the area
        CXAgentTask newTask = new CXAgentTask(CXAgentState.Moving);
        CXAgentMovingTask movingTask = new CXAgentMovingTask(destination);
        movingTask.radius = 90.0f;
        newTask.movingTask = movingTask;
        pursuerComponent.taskList.add(newTask);

        // 2. Add Searching Task
        CXAgentTask newTask2 = new CXAgentTask(CXAgentState.Searching);
        CXAgentSearchTask searchTask = new CXAgentSearchTask();
        searchTask.searchArea = node;
        newTask2.searchTask = searchTask;
        pursuerComponent.taskList.add(newTask2);

        return pursuerComponent;
    }

    public PursuerComponent tranferSearchingTaskToMovingTask(PursuerComponent agent){

        // warning bug!!!!!!!!!!!!
        CXAgentTask task = (CXAgentTask) agent.taskList.getFirst();
        CXDecomposedGraphNode searchingArea = task.searchTask.searchArea;
        agent.currentSearchArea = task.searchTask.searchArea.nodeNumber;

        CXGraphNode  topRightNode = searchingArea.getTopRightNode();
        CXPoint destination = topRightNode.location;

        agent.taskList.removeFirst();

        CXAgentTask newTask = new CXAgentTask(CXAgentState.Moving);

        destination =  CXPoint.convertToOriginalCoordination(destination);

        CXAgentMovingTask movingTask = new CXAgentMovingTask(destination);
        newTask.movingTask = movingTask;

        agent.taskList.addFirst(newTask);
        agent.setState(CXAgentState.Moving);

        return agent;
    }

    public PursuerComponent findNewSearchingArea(PursuerComponent pursuerComponent, StateComponent stateComponent, CXGraph decomposedGraph, HashMap searchedArea, LinkedList messageLinkedList){

        CXDecomposedGraphNode node = (CXDecomposedGraphNode) decomposedGraph.getVerticesInIndex(pursuerComponent.currentSearchArea);
        ArrayList rightNeighbours = GraphTool.findTheRightNeighbours(decomposedGraph, node);
        ArrayList leftNeighbours = GraphTool.findtheLeftNeighbours(decomposedGraph,node);

        switch (rightNeighbours.size()){
            case 1: {
                CXDecomposedGraphNode rightNode = (CXDecomposedGraphNode)rightNeighbours.get(0);
                ArrayList leftNeighboursOfRightNode = GraphTool.findtheLeftNeighbours(decomposedGraph,rightNode);

                if (this.theAreaIsBeenSearched(leftNeighboursOfRightNode, searchedArea)) {

                    CXDecomposedGraphNode dNode = (CXDecomposedGraphNode) rightNeighbours.get(0);
                    CXPoint agentLocation = new CXPoint(stateComponent.position.x,stateComponent.position.z);

                    agentLocation = CXPoint.converToGraphCoordination(agentLocation);
                    CXPoint newPoint = dNode.getTopLeftNode().location;

                    // The reason use CXPoint.distance instead of  equal because the number has too much decimal
                    if (CXPoint.distance(agentLocation,newPoint) < 0.0001) { // 转变了坐标
                        // If the next search area is the final Area
                        System.out.println(rightNode.nodeNumber);
                        if (rightNode.nodeNumber == this.finalArea.nodeNumber){
                            CXAgentTask newTask = new CXAgentTask(CXAgentState.Scanning);
                            newTask.scanTask.scanScope.add((Float)0f);
                            pursuerComponent.taskList.add(newTask);
                            pursuerComponent.currentSearchArea = dNode.nodeNumber;
                            pursuerComponent.setState(CXAgentState.Scanning);

                            CXAgentTask finishTask = new CXAgentTask(CXAgentState.FinishGame);
                            pursuerComponent.taskList.add(finishTask);
                        }
                        CXAgentTask newTask = new CXAgentTask(CXAgentState.Searching);
                        newTask.searchTask.searchArea = dNode;
                        pursuerComponent.taskList.add(newTask);
                        pursuerComponent.currentSearchArea = dNode.nodeNumber;
                        pursuerComponent.setState(CXAgentState.Searching);

                    }
                    else pursuerComponent.setState(CXAgentState.Free);
                } else {
                    pursuerComponent.setState(CXAgentState.WaitSearching);
                }
                return pursuerComponent;
            }

            default:{

                LinkedList downUpList = new LinkedList();

                CXDecomposedGraphNode dNode = (CXDecomposedGraphNode) rightNeighbours.get(0);
                double y = dNode.getTopLeftNode().location.y;

                CXDecomposedGraphNode dNode1 = (CXDecomposedGraphNode) rightNeighbours.get(1);
                double y1 = dNode1.getTopLeftNode().location.y;

                if (y > y1){
                    downUpList.add(dNode);
                    downUpList.add(dNode1);
                }
                else {
                    downUpList.add(dNode);
                    downUpList.add(dNode1);
                }
                CXMessage message = new CXMessage();
                message.sender = pursuerComponent.number;
                message.messageType = CXMessageType.CallBackUp;

                CXAgentTask messageContent = new CXAgentTask();
                messageContent.taskState = CXAgentState.Searching;

                CXAgentSearchTask searchingTask = new CXAgentSearchTask();
                searchingTask.searchArea = (CXDecomposedGraphNode)downUpList.getLast();
                messageContent.searchTask = searchingTask;

                message.messageContent = messageContent;

                messageLinkedList.add(message);
                pursuerComponent.setState(CXAgentState.WaitBackup);
                System.out.println("Agent " + pursuerComponent.number + " Send a backUp message, Search Area " + searchingTask.searchArea.nodeNumber );

                return pursuerComponent;
            }
        }
    }

    public CXPoint getNextMovingPoint(PursuerComponent pC, CXPoint currentPoint, CXPoint destination, PathFinder pathfinder){
        if (currentPoint.x == destination.x && currentPoint.y == destination.y) return destination;
        if(pC.pursuerPointPath.size()==0){
            pC.pursuerPointPath = discretizePath(currentPoint, destination);
            Vector3 start = new Vector3((float)currentPoint.x, 0, (float)(currentPoint.y));
            Vector3 end = new Vector3((float)destination.x, 0, (float)(destination.y));
            List<Node> path = pathfinder.findPath(start, end, null);
            if(path.size()>0) {
                path.get(0).worldX = (float) currentPoint.x;
                path.get(0).worldZ = (float) currentPoint.y;
            }
            pC.pursuerPointPath = addAdditionalSteps(pC, path);
        }
        if(pC.pursuerPointPath.size()>1){
            return pC.pursuerPointPath.remove(0);
        }else {
            if(pC.pursuerPointPath.size()>0)
                pC.pursuerPointPath.remove(0);
            return destination;
        }
    }

    public ArrayList<CXPoint> addAdditionalSteps(PursuerComponent pC, List<Node> p){
        pC.pursuerPointPath= new ArrayList<CXPoint>();
        float stepSize = 8;
        float diagStepSize = (float) Math.floor(1.4*stepSize);
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
                        pC.pursuerPointPath.add(position);
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
                        pC.pursuerPointPath.add(position);
                    }
                }
            }
        }
        return pC.pursuerPointPath;
    }

    private ArrayList<CXPoint> discretizePath(CXPoint start, CXPoint end){
        ArrayList<CXPoint> path = new ArrayList<CXPoint>();
        double distX = end.x - start.x;
        double distY = end.y - start.y;
        double distance = Math.sqrt((distX*distX)+(distY*distY));
        double steps = distance/0.2;
        double stepSize = 15;

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

    public PursuerComponent checkLeftAreaIsBeenSearched(CXGraph decomposedGraph, PursuerComponent pursuerComponent, HashMap searchedArea, StateComponent stateComponent){

        // Check all left area is been searched or not ? --> Yes, Check the location --> It's in the top-right location? Add Searching task: state = Free;

        // 1. Get left Neighbours
        CXDecomposedGraphNode node = (CXDecomposedGraphNode) decomposedGraph.getVerticesInIndex(pursuerComponent.currentSearchArea);
        //
        ArrayList toBesearchedArea = GraphTool.findTheRightNeighbours(decomposedGraph,node);
        CXDecomposedGraphNode rightNode = (CXDecomposedGraphNode)toBesearchedArea.get(0);

        ArrayList leftNeighbours = GraphTool.findtheLeftNeighbours(decomposedGraph,rightNode);

        // 2. check the area
        if (theAreaIsBeenSearched(leftNeighbours,searchedArea)){
            // 1. Check Location --> Free / New Task

            // Change agentLocation to graph coordination system
            CXPoint agentLocation = new CXPoint(stateComponent.position.x,stateComponent.position.z);
            agentLocation = CXPoint.converToGraphCoordination(agentLocation);

            float  distance =  (float) CXPoint.distance(agentLocation,rightNode.getTopLeftNode().location);
            System.out.println(rightNode.getTopLeftNode().location.x + " " + rightNode.getTopLeftNode().location.y);
            if (CXPoint.distance(agentLocation,rightNode.getTopLeftNode().location) < 0.001) {
                if (rightNode.nodeNumber == this.finalArea.nodeNumber){
                    CXAgentTask newTask = new CXAgentTask(CXAgentState.Scanning);
                    newTask.scanTask.scanScope.add((Float)0.0f);

                    pursuerComponent.taskList.add(newTask);
                    pursuerComponent.currentSearchArea = finalArea.nodeNumber;
                    pursuerComponent.setState(CXAgentState.Scanning);

                    CXAgentTask finishTask = new CXAgentTask(CXAgentState.FinishGame);
                    pursuerComponent.taskList.add(finishTask);
                }
                else pursuerComponent.setState(CXAgentState.FinisihSearching); // Should invoke find new path
            }
            else pursuerComponent.setState(CXAgentState.Free);
        }

        return pursuerComponent;
    }

    public PursuerComponent checkTheBackupLocation(CXGraph decomposedGraph, PursuerComponent pursuerComponent,HashMap searchedArea){
        // 1. Get the right neighbours
        CXDecomposedGraphNode searchNode = (CXDecomposedGraphNode) decomposedGraph.getVerticesInIndex(pursuerComponent.currentSearchArea);
        ArrayList rightNeighbours = GraphTool.findTheRightNeighbours(decomposedGraph,searchNode);

        int count = 0;
        CXDecomposedGraphNode waitSeachNode = new CXDecomposedGraphNode();
        for (int i = 0; i < rightNeighbours.size() ; i++) {
            CXDecomposedGraphNode node = (CXDecomposedGraphNode)rightNeighbours.get(i);
            if (searchedArea.containsKey(node.nodeNumber))count ++;
            else {
                waitSeachNode = node;
            }
        }
        if (count == rightNeighbours.size() -1){
            CXAgentTask searchTask = new CXAgentTask(CXAgentState.Searching);
            searchTask.searchTask.searchArea = waitSeachNode;
            pursuerComponent.taskList.add(searchTask);
            pursuerComponent.setState(CXAgentState.Searching);
        }
        return pursuerComponent;
    }


    public boolean theAreaIsBeenSearched(ArrayList decomposedNodeListArray, HashMap searchedArea){
        int count = 0;
        for (int i = 0; i < decomposedNodeListArray.size() ; i++) {
            CXDecomposedGraphNode leftNode = (CXDecomposedGraphNode)decomposedNodeListArray.get(i);
            if (searchedArea.containsKey(leftNode.nodeNumber) &&  searchedArea.get(leftNode.nodeNumber) == Boolean.TRUE ) count++;
            else return false;
        }
        return true;
    }

    public float getTheNextScanPosition(PursuerComponent pursuerComponent){
        CXAgentTask task = (CXAgentTask) pursuerComponent.taskList.getFirst();
        Float targetRadius = (Float) task.scanTask.scanScope.getFirst();

        // Write a function to decide the next move position
        float nextMovePostion = targetRadius.floatValue();

        if (nextMovePostion == targetRadius.floatValue()){
            task.scanTask.scanScope.removeFirst();
        }

        return nextMovePostion;
    }
}
