package com.dke.pursuitevasion.CellDecompose.Graph;

import com.dke.pursuitevasion.CellDecompose.Model.CXExtendInfoModel;
import com.dke.pursuitevasion.CellDecompose.Model.CXExtendedModel;
import com.sun.corba.se.impl.orbutil.graph.Graph;
import com.sun.org.apache.bcel.internal.generic.IF_ACMPEQ;
import com.sun.org.apache.bcel.internal.generic.NOP;

import java.awt.*;
import java.awt.image.AreaAveragingScaleFilter;
import java.lang.reflect.Array;
import java.security.PublicKey;
import java.util.ArrayList;

/**
 * Created by chenxi on 4/16/17.
 */
public class CellDecompositionAlgorithm  {

    /*
    1. Original Graph: 1. vertices(location: point) 2. edges
    2. Decomposed Graph: 1. vertices(Vertices: pointList) 2.edges
     */
    public CXGraph polygon;
    public CXGraph obstacles;
    public CXGraph originalGraph;   // OriginalGraph = polygon + obstables
    public CXGraph decomposedGraph = new CXGraph(); // Final graph
    public CXGraph extendedGraph;    // extendedGraph  =  originalGraph + extendlined generated from obstacle
    public ArrayList extendedModelArray = new ArrayList(); // Save all the new extended edges as Point [x,y]
    GraphTool graphTool = new GraphTool();

    // Initialisation
    public CellDecompositionAlgorithm(){

    }

    public CellDecompositionAlgorithm(CXGraph polygon,CXGraph obstacles){
        this.polygon = polygon;
        this.obstacles = obstacles;
        this.originalGraph = GraphTool.combineGraph(polygon,obstacles);
    }

    public CXGraph decomposeGraph(){

        System.out.println("1. Sort the vertices in obstacles based on X value");
        ArrayList sortedVertices =  sortObstacleVertices(obstacles);
        for (int s = 0; s < sortedVertices.size() ; s++) {
            CXGraphNode node = (CXGraphNode) sortedVertices.get(s);
            System.out.println("The " + s + " vertices " +"NodeNumber= " + node.nodeNumber + " x = " + node.location.x  + " y " + node.location.y);
        }

        System.out.println("2.Get the location of new vertices generated from the obstacle.");
        ArrayList[] edgeList = new ArrayList[sortedVertices.size()];
        edgeList = computeTheLocationOfNewVertices(sortedVertices,edgeList);

        System.out.println("3. Extend the vertices in obstacle and generate a new Graph");
        CXGraph extendedGraph = new CXGraph();
        try { extendedGraph = (CXGraph) CXGraph.deepCopy(originalGraph);}
        catch (Exception e){
            System.out.println("Deep copy error" + e);
        }
        this.extendedGraph = extendTheVertices(edgeList,extendedGraph,sortedVertices);

        System.out.println("4. Construct decomposedGraph");
        decomposedGraph = buildTheDecomposedGraph(this.decomposedGraph,extendedGraph,this.extendedModelArray);
        System.out.println(decomposedGraph);
        System.out.println("Finish decomposing map");
        return decomposedGraph;
    }

    private ArrayList sortObstacleVertices(CXGraph graph) {

        ArrayList list = graph.allVertices;
        for (int i = 0; i < list.size(); i++) {
            CXGraphNode nodeI = (CXGraphNode) list.get(i);
            for (int j = i + 1; j < list.size(); j++) {
                CXGraphNode nodeJ = (CXGraphNode) list.get(j);
                if (nodeI.location.x > nodeJ.location.x) {
                    CXGraphNode tempNode = nodeI;
                    list.set(i, nodeJ);
                    list.set(j, tempNode);
                    nodeI = nodeJ;
//                }
                }
            }
        }
        return list;
    }

    private ArrayList[] computeTheLocationOfNewVertices(ArrayList sortedVertices,ArrayList[] edgeList){
        // intersected edgeList
        ArrayList[] graphVerticesList = originalGraph.getVerticesList();

        // Only extended the vertices in obstacle
        for (int i = 0 ; i < sortedVertices.size() ; i++){
            CXGraphNode vertices = (CXGraphNode) sortedVertices.get(i);
            double verticesNodeX = vertices.location.x;
            ArrayList extendEdges = new ArrayList();

            // Put all the intersection edges in (ArrayList)edgeList;
            for (int j = 1 ; j < graphVerticesList.length ; j ++){
                // There is no vertices to check
                if (graphVerticesList[j] == null){
                    break;
                }
                ArrayList list = graphVerticesList[j];
                CXGraphNode firstNode = (CXGraphNode) list.get(0);

                for (int k = 1 ; k < list.size() ; k++){
                    CXGraphNode neighbourNode = (CXGraphNode) list.get(k);
                    double firstNodeX = firstNode.location.x;
                    double neighbourNodeX = neighbourNode.location.x;
                    // Warning: if the intersection is in the vertices, and then it will add twice.
                    if(vertices.nodeNumber != firstNode.nodeNumber && vertices.nodeNumber != neighbourNode.nodeNumber) {
                        if (firstNodeX <= verticesNodeX && verticesNodeX < neighbourNodeX || firstNodeX < verticesNodeX && verticesNodeX <= neighbourNodeX) {
                            extendEdges.add(new Point(firstNode.nodeNumber, neighbourNode.nodeNumber));
                        }
                    }
                }
            }
            edgeList [i] = extendEdges;
        }
        // Find the  closest edge for each vertices in obstacle and save it in edgeList
        System.out.println("2.1 Find the closest edge to connect");
        edgeList = findTheClosestEdgeInConsectionList(edgeList,sortedVertices);
        return edgeList;
    }

    // Waiting to add
    private ArrayList[] findTheClosestEdgeInConsectionList(ArrayList[] edgeList,ArrayList sortedObstacleVertices){

        ArrayList addVerticesInPolygon =  new ArrayList();

        for (int i = 0 ; i < edgeList.length ; i ++){
            ArrayList list = edgeList[i];
            int upperEdgeNumber = 0;
            int lowerEdgeNumber = 0;
            int minUpperDistanceIndex = 0;
            int minLowerDistanceIndex = 0;
            double minUperDistance = 1000000000;
            double minLowerDistance = 1000000000;
            CXGraphNode vertice = (CXGraphNode) sortedObstacleVertices.get(i);

            // The intersected edge is more than 2 which means we need to pick the closest one to add new vertices

                int countYPlus = 0, countYMinus = 0;

                for (int j = 0; j < list.size(); j++) {
                    // 1. distance from vertices to the edge
                    Point edgePoint =  (Point) list.get(j);
                    CXGraphNode X = originalGraph.getVerticesInIndex(edgePoint.x);
                    CXGraphNode Y = originalGraph.getVerticesInIndex(edgePoint.y);
                    double X1 = X.location.x;
                    double Y1 = X.location.y;
                    double X2 = Y.location.x;
                    double Y2 = Y.location.y;

                    double K = (Y2- Y1)/(X2-X1);
                    double b = Y1- K*X1;
                    double horizontalValue = K*vertice.location.x + b;
                    // get the most closest edge from the intersection array.
                    if (horizontalValue == vertice.location.y){
                        if (polygon.containVertices(X.location)!= -1 && polygon.containVertices(Y.location)!= -1){
                            CXGraphNode[] addNode = new CXGraphNode[3];
                            addNode[0] = X;
                            addNode[1] = Y;
                            addNode[2] = vertice;
                            addVerticesInPolygon.add(addNode);
                        }
                        continue;
                    }
                    if (horizontalValue > vertice.location.y){
                        upperEdgeNumber ++;
                            if (minUperDistance > horizontalValue - vertice.location.y){
                                minUperDistance = horizontalValue - vertice.location.y;
                                minUpperDistanceIndex = j;
                        }
                    }
                    else {
                        lowerEdgeNumber ++;
                            if (minLowerDistance > vertice.location.y - horizontalValue){
                                minLowerDistance = vertice.location.y - horizontalValue;
                                minLowerDistanceIndex = j;
                        }
                    }
                }
                // Replace intersection Arraylist if intersected edge is more than 2
                ArrayList newList = new ArrayList();
                if (minUperDistance != 1000000000) {
                    Point minUpperPoint = (Point) list.get(minUpperDistanceIndex);
                    newList.add(minUpperPoint);
                }
                if (minLowerDistance  != 1000000000){
                    Point minLowerPoint = (Point) list.get(minLowerDistanceIndex);
                    newList.add(minLowerPoint);
                }

                // Check the most closest intersection is in the same closed path or not?
                for (int k = 0; k < newList.size();k++){
                    Point point = (Point) newList.get(k);
                    CXGraphNode node = originalGraph.getVerticesInIndex(point.x);
                    if(graphTool.isConnectedInClosedPath(originalGraph,vertice,node)){
                        newList.remove(point);
                        break;
                    }
                }

                edgeList[i] = newList;
            }
        for (int j = 0; j < addVerticesInPolygon.size(); j++) {
            CXGraphNode[] addNode = (CXGraphNode[]) addVerticesInPolygon.get(j);
            originalGraph.remove_edge(addNode[0],addNode[1]);
            originalGraph.add_edge(addNode[0],addNode[2],1);
            originalGraph.add_edge(addNode[1],addNode[2],1);
            System.out.println("Combine the vertices " + addNode[2].nodeNumber + " which in both polygon and obstacle");
        }
        return  edgeList;
    }

    private CXGraph extendTheVertices(ArrayList[] edgeList,CXGraph newGraph,ArrayList sortedVertices){

        for (int i = 0; i < sortedVertices.size()  ; i++) {
            CXGraphNode extendVertices = (CXGraphNode) sortedVertices.get(i);
            System.out.println("Extend the " + i + " vertices" + " NodeNumber = " + extendVertices.nodeNumber + "NodeLocation x= " + extendVertices.location.x + " y = " + extendVertices.location.y) ;
            CXExtendedModel extendedModel = new CXExtendedModel();       // Saving all the extended infomation
            ArrayList arrayList = new ArrayList();
            extendedModel.extendedInfor = arrayList;
            extendedModel.orignialVertices = extendVertices;

            ArrayList extendEdgeList = edgeList[i];
            for (int j = 0; j < extendEdgeList.size(); j++) {
                CXExtendInfoModel infoModel = new CXExtendInfoModel();


                // 1. Compute the intersection location
                Point point = (Point) extendEdgeList.get(j);
                CXGraphNode node1 = newGraph.getVerticesInIndex(point.x);
                CXGraphNode node2 = newGraph.getVerticesInIndex(point.y);
                CXGraphNode rightNode1 = node1.location.x > node2.location.x? node1:node2;
                CXGraphNode leftNode1 = node1.location.x < node2.location.x? node1:node2;

                double intersectedX = extendVertices.location.x;
                double X1 = leftNode1.location.x;
                double X2 = rightNode1.location.x;
                double Y1 = leftNode1.location.y;
                double Y2 = rightNode1.location.y;
                double K = (Y2 - Y1)/(X2 - X1);
                double b = Y2 - K*X2;
                double intersectedY = K*intersectedX + b;

                CXPoint newPoint = new CXPoint(intersectedX,intersectedY);
                infoModel.extendedVertices = newPoint;
                infoModel.edgeNumber = point;
                infoModel.isUp = (intersectedY - extendVertices.location.y) > 0;

                // 2.1 if intersected location is vertices(A). Then all we needed is add newEdge(A,B)

                if(newPoint.x == node1.location.x && newPoint.y == node1.location.y){
                    newGraph.add_edge(node1,extendVertices,1);
                    infoModel.extendedVerticesNumber = node1.nodeNumber;

                }
                else if (newPoint.x == node2.location.x  && newPoint.y == node2.location.y){
                    newGraph.add_edge(node2,extendVertices,1);
                    infoModel.extendedVerticesNumber = node2.nodeNumber;

                }
                else {
                    // If the graph changed, so we should find the new intersection area.

                    CXGraphNode rightNode = node1.location.x > node2.location.x? node1:node2;

                    CXGraphNode leftNode = new CXGraphNode();

                    ArrayList neighbours = newGraph.neighbors(rightNode);
                    for (int k = 1; k < neighbours.size() ; k++) {
                        CXGraphNode node = (CXGraphNode)neighbours.get(k);
                        double  K1 = (rightNode.location.y - node.location.y)/(rightNode.location.x - node.location.x);
                        if (K1 - K < 0.001 && K1 - K > -0.001 ) {
                        leftNode = node;
                        break;}
                    }
                    // 2.2 if intersected location is in the edge, then add New Vertices newPoint
                    CXGraphNode newNode = new CXGraphNode(newPoint);
                    newGraph.add_vertex(newNode);
                    infoModel.extendedVerticesNumber =  newGraph.allVertices.size();
                    // 3. Remove the neighbour relationship of old edge (Edge.leftVertice, Edge.rightVertice)
                    newGraph.remove_edge(leftNode, rightNode);
                    // 4. Add neighbour relationship in new edges (vertices, newPoint) (edge.leftVertices newPoint) (newPoint, edge.rightVertices)
                    newGraph.add_edge(extendVertices, newNode, 1);
                    // put all the new edge into ArrayList, prepare for checking the relationship of vertices in decomposed graph
//                this.newEdges.add(new Point(extendVertices.nodeNumber,newNode.nodeNumber));
                    newGraph.add_edge(leftNode, newNode, 1);
                    newGraph.add_edge(newNode, rightNode, 1);
                }
                extendedModel.extendedInfor.add(infoModel);
            }
            this.extendedModelArray.add(extendedModel);
        }

        return  newGraph;
    }

    private CXGraph buildTheDecomposedGraph(CXGraph decomposedGraph, CXGraph baseGraph,ArrayList extendedModelArray){
        /* 1. start from the sortedVertices node
           2. check for up or down
           3. choose up & down loop
           4. Put all the vertices in an ArrayList(),check with previous GraphNode, if it doesn't exist generate a new Graph.CXDecomposedGraphNode
           5. Build the relationship in Graph.CXDecomposedGraphNode
           */
       // 1. Generate component of new Vertices
        System.out.println("4.1 Generate component of new Vertices ");
        ArrayList newVerticesArrayList = new ArrayList();
        newVerticesArrayList =  generateTheNewNodes( decomposedGraph,  baseGraph, extendedModelArray , newVerticesArrayList);
        // 2. Delete the repeted components
        System.out.println("4.2 Delete the repeted components");
        newVerticesArrayList = deleteTheRepetedVertices(newVerticesArrayList);

        // 3. Generate the new graph
        System.out.println("4.3 Generate the new graph");
        decomposedGraph = buildTheNewGraph(decomposedGraph,newVerticesArrayList);

        // 4. Connect all the new vertices
        System.out.println("4.4 Build the connection for the vertices");

        decomposedGraph = buildTheConnectionForTheVertices(decomposedGraph,extendedModelArray);

        return decomposedGraph;
    }

    private ArrayList generateTheNewNodes(CXGraph decomposedGraph, CXGraph baseGraph,ArrayList extendedModelArray, ArrayList newVerticesArrayList){
             /* 1. start from the sortedVertices node
           2. check for up or down
           3. choose up & down loop
           4. Put all the vertices in an ArrayList(),check with previous GraphNode, if it doesn't exist generate a new Graph.CXDecomposedGraphNode
           5. Build the relationship in Graph.CXDecomposedGraphNode
           */

        for (int i = 0; i < extendedModelArray.size() ; i++) {

            CXExtendedModel model = (CXExtendedModel) extendedModelArray.get(i);
            System.out.println( i + " Generating new Vertices, starting number  = "  + model.orignialVertices.nodeNumber  );
            ArrayList extendedList = model.extendedInfor;

            for (int j = 0; j < extendedList.size() ; j++) {

                ArrayList newVertices = new ArrayList();
                newVertices.add(model.orignialVertices);

                CXGraphNode searchingNode = model.orignialVertices;
                int movingDirection = 0;
                CXExtendInfoModel infoModel = (CXExtendInfoModel) extendedList.get(j);
                boolean isSearchingFinished = false;
                // 2. check the extended direction
                if (infoModel.isUp){
                    while (!isSearchingFinished){
                        ArrayList neighbourList = baseGraph.neighbors(searchingNode);
                        switch (movingDirection) {
                            case 0: {
                                // Moving up
                                for (int k = 1; k < neighbourList.size(); k++) {
                                    CXGraphNode neighbourNode = (CXGraphNode) neighbourList.get(k);
                                    if (neighbourNode.location.y > searchingNode.location.y && neighbourNode.location.x == searchingNode.location.x) {
                                        newVertices.add(neighbourNode);
                                        searchingNode = neighbourNode;
                                        movingDirection++;
                                        break;
                                    }
                                }
                                break;
                            }
                            case 1: {
                                // Moving right ( if there is no right node which means the newNode it generated will overlap with the other nodes, so stop this loop)
                                CXGraphNode node = new CXGraphNode();
                                for (int k = 1; k < neighbourList.size(); k++) {
                                    CXGraphNode neighbourNode = (CXGraphNode) neighbourList.get(k);
                                    if (neighbourNode.location.x > searchingNode.location.x) {
                                        if (node.location == null){
                                            node = neighbourNode;
                                        }
                                        else {
                                            if (neighbourNode.location.y < node.location.y){
                                                node = neighbourNode;
                                            }
                                        }
                                    }
                                }
                                if (node.location == null){
                                    isSearchingFinished = true;
                                    newVertices =  null;
                                    break;
                                }
                                newVertices.add(node);
                                movingDirection++;
                                searchingNode = node;
                                break;
                            }
                            case 2: {
                                // moving down
                                boolean finishSearching = false;
                                for (int k = 1; k < neighbourList.size(); k++) {
                                    CXGraphNode neighbourNode = (CXGraphNode) neighbourList.get(k);
                                    if (neighbourNode == model.orignialVertices) {
                                        isSearchingFinished = true;
                                        finishSearching = true;
                                        break;
                                    }
                                }
                                if (!finishSearching) {
                                    boolean moving = false;
                                    for (int k = 1; k < neighbourList.size(); k++) {
                                        CXGraphNode neighbourNode = (CXGraphNode) neighbourList.get(k);
                                        if (neighbourNode.location.y < searchingNode.location.y && neighbourNode.location.x == searchingNode.location.x) {
                                            newVertices.add(neighbourNode);
                                            searchingNode = neighbourNode;
                                            movingDirection++;
                                            moving = true;
                                            break;
                                        }
                                    }
                                    // keep moving right
                                    if (!moving){
                                        if (neighbourList.size() == 3){
                                            for (int k = 1; k < neighbourList.size(); k++) {
                                                CXGraphNode neighbourNode = (CXGraphNode) neighbourList.get(k);
                                                if (!newVertices.contains(neighbourNode)) {
                                                    newVertices.add(neighbourNode);
                                                    searchingNode = neighbourNode;
                                                }
                                            }
                                        }
                                        else {
                                            for (int k = 1; k < neighbourList.size(); k++) {
                                                CXGraphNode neighbourNode = (CXGraphNode) neighbourList.get(k);
                                                if (neighbourNode.location.x > searchingNode.location.x) {
                                                    newVertices.add(neighbourNode);
                                                    searchingNode = neighbourNode;
                                                }
                                            }
                                        }
                                    }

                                }
                                break;
                            }
                            // keeping moving down or moving left
                            case 3: {
                                boolean finishSearching = false;
                                boolean movingDown = false;
                                // check if there any neighbour vertices is the started vertices
                                for (int k = 1; k < neighbourList.size(); k++) {
                                    CXGraphNode neighbourNode = (CXGraphNode) neighbourList.get(k);
                                    if (neighbourNode.nodeNumber == model.orignialVertices.nodeNumber) {
                                        isSearchingFinished = true;
                                        finishSearching = true;
                                        break;
                                    }
                                }
                                if (!finishSearching) {

                                    // moving left

                                        for (int k = 1; k < neighbourList.size(); k++) {
                                            CXGraphNode neighbourNode = (CXGraphNode) neighbourList.get(k);
                                            if (neighbourNode.location.x < searchingNode.location.x) {
                                                newVertices.add(neighbourNode);
                                                searchingNode = neighbourNode;
                                                movingDirection++;
                                            }
                                        }
                                    if (!movingDown){
                                    // moving down
                                    for (int k = 1; k < neighbourList.size(); k++) {
                                        CXGraphNode neighbourNode = (CXGraphNode) neighbourList.get(k);
                                        if (neighbourNode.location.y < searchingNode.location.y && neighbourNode.location.x == searchingNode.location.x) {
                                            newVertices.add(neighbourNode);
                                            searchingNode = neighbourNode;
                                            movingDown = true;
                                            break;
                                        }
                                    }
                                        }
                                }
                                break;
                            }
                            // moving left or moving up
                            case 4:{
                                boolean finishSearching = false;
                                boolean isMoving = false;
                                // Checking any vertices is started vertices
                                for (int k = 1; k < neighbourList.size(); k++) {
                                    CXGraphNode neighbourNode = (CXGraphNode) neighbourList.get(k);
                                    if (neighbourNode.nodeNumber == model.orignialVertices.nodeNumber) {
                                        isSearchingFinished = true;
                                        finishSearching = true;
                                        break;
                                    }
                                }
                                if (!finishSearching)

                                {     // moving up
                                    for (int k = 1; k < neighbourList.size(); k++) {
                                        CXGraphNode neighbourNode = (CXGraphNode) neighbourList.get(k);
                                        if (neighbourNode.location.y > searchingNode.location.y && neighbourNode.location.x == searchingNode.location.x) {
                                            newVertices.add(neighbourNode);
                                            searchingNode = neighbourNode;
                                            movingDirection++;
                                            isMoving = true;
                                            break;
                                        }
                                    }
                                    // keep moving left
                                    if (!isMoving){
                                        for (int k = 1; k < neighbourList.size(); k++) {
                                            CXGraphNode neighbourNode = (CXGraphNode) neighbourList.get(k);
                                            if (neighbourNode.location.x < searchingNode.location.x) {
                                                newVertices.add(neighbourNode);
                                                searchingNode = neighbourNode;
                                                break;
                                            }
                                        }
                                    }
                                    // If not moving left then Moving up
                                }
                                break;
                            }
                            // Keep moving up until reached the start vertices
                            default:{
                                // Checking any vertices is started vertices
                                for (int k = 1; k < neighbourList.size(); k++) {
                                    CXGraphNode neighbourNode = (CXGraphNode) neighbourList.get(k);
                                    if (neighbourNode.nodeNumber == model.orignialVertices.nodeNumber) {
                                        isSearchingFinished = true;

                                        break;
                                    }
                                }
                                if (!isSearchingFinished){
                                    for (int k = 1; k < neighbourList.size(); k++) {
                                        CXGraphNode neighbourNode = (CXGraphNode) neighbourList.get(k);
                                        if (neighbourNode.location.y > searchingNode.location.y && neighbourNode.location.x == searchingNode.location.x) {
                                            newVertices.add(neighbourNode);
                                            searchingNode = neighbourNode;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }}
                else {
                    while (!isSearchingFinished){
                        ArrayList neighbourList = baseGraph.neighbors(searchingNode);
                        switch (movingDirection) {
                            case 0: {
                                // Moving down
                                for (int k = 1; k < neighbourList.size(); k++) {
                                    CXGraphNode neighbourNode = (CXGraphNode) neighbourList.get(k);
                                    if (neighbourNode.location.y < searchingNode.location.y && neighbourNode.location.x == searchingNode.location.x)
                                    {
                                        newVertices.add(neighbourNode);
                                        searchingNode = neighbourNode;
                                        movingDirection++;
                                        break;
                                    }}
                                break;
                            }
                            case 1: {

                                // Moving right
                                CXGraphNode node = new CXGraphNode();
                                for (int k = 1; k < neighbourList.size(); k++) {
                                    CXGraphNode neighbourNode = (CXGraphNode) neighbourList.get(k);
                                    // Only move to the upper node, if it has two neighbour nodes on the right side.
                                    if (neighbourNode.location.x > searchingNode.location.x) {
                                        if (node.location == null){
                                            node = neighbourNode;
                                        }
                                        else {
                                            if (neighbourNode.location.y > node.location.y){
                                                node = neighbourNode;

                                            }
                                        }
                                    }
                                }
                                if (node.location == null){
                                    isSearchingFinished = true;
                                    newVertices = null;
                                    break;

                                }
                                searchingNode = node;
                                newVertices.add(node);
                                movingDirection++;
                                break;
                            }
                            case 2: {
                                // Moving up or keep moving right

                                //2.1 If neighbour nodes contain started node.
                                boolean finishSearching = false;
                                for (int k = 1; k < neighbourList.size(); k++) {
                                    CXGraphNode neighbourNode = (CXGraphNode) neighbourList.get(k);
                                    if (neighbourNode.nodeNumber == model.orignialVertices.nodeNumber) {
                                        isSearchingFinished = true;
                                        finishSearching = true;
                                        break;
                                    }
                                }
                                if (!finishSearching) {
                                    // Moving up
                                    boolean moving = false;
                                    for (int k = 1; k < neighbourList.size(); k++) {
                                        CXGraphNode neighbourNode = (CXGraphNode) neighbourList.get(k);
                                        if (neighbourNode.location.y > searchingNode.location.y && neighbourNode.location.x == searchingNode.location.x) {
                                            newVertices.add(neighbourNode);
                                            searchingNode = neighbourNode;
                                            movingDirection++;
                                            moving = true;
                                            break;
                                        }
                                    }
                                    if (!moving) {
                                        if (neighbourList.size() == 3){
                                            for (int k = 1; k < neighbourList.size(); k++) {
                                                CXGraphNode neighbourNode = (CXGraphNode) neighbourList.get(k);
                                                if (!newVertices.contains(neighbourNode)) {
                                                    newVertices.add(neighbourNode);
                                                    searchingNode = neighbourNode;
                                                }
                                            }
                                        }
                                        else {
                                        for (int k = 1; k < neighbourList.size(); k++) {
                                            CXGraphNode neighbourNode = (CXGraphNode) neighbourList.get(k);
                                            if (neighbourNode.location.x > searchingNode.location.x) {
                                                newVertices.add(neighbourNode);
                                                searchingNode = neighbourNode;
                                            }
                                        }
                                        }
                                    }
                                }
                                break;
                            }
                            // moving left or keep moving up
                            case 3: {
                                boolean finishSearching = false;
                                boolean movingLeft = false;
                                // check if there any neighbour vertices is started vertices
                                for (int k = 1; k < neighbourList.size(); k++) {
                                    CXGraphNode neighbourNode = (CXGraphNode) neighbourList.get(k);
                                    if (neighbourNode.nodeNumber == model.orignialVertices.nodeNumber) {
                                        isSearchingFinished = true;
                                        finishSearching = true;
                                        break;
                                    }
                                }
                                //  moving left
                                if (!finishSearching) {
                                    for (int k = 1; k < neighbourList.size(); k++) {
                                        CXGraphNode neighbourNode = (CXGraphNode) neighbourList.get(k);
                                        if (neighbourNode.location.x < searchingNode.location.x) {
                                            newVertices.add(neighbourNode);
                                            searchingNode = neighbourNode;
                                            movingDirection++;
                                            movingLeft = true;
                                        }
                                    }
                                    if (!movingLeft) {
                                        // keep moving up
                                        for (int k = 1; k < neighbourList.size(); k++) {
                                            CXGraphNode neighbourNode = (CXGraphNode) neighbourList.get(k);
                                            if (neighbourNode.location.y > searchingNode.location.y && neighbourNode.location.x == searchingNode.location.x) {
                                                newVertices.add(neighbourNode);
                                                searchingNode = neighbourNode;
                                                break;
                                            }
                                        }

                                    }
                                }
                                break;
                            }
                            // moving down or keep moving left
                            case 4: {
                                boolean finishSearching = false;
                                boolean isMoving = false;
                                // Checking any vertices is started vertices
                                for (int k = 0; k < neighbourList.size(); k++) {
                                    CXGraphNode neighbourNode = (CXGraphNode) neighbourList.get(k);
                                    if (neighbourNode.nodeNumber == model.orignialVertices.nodeNumber) {
                                        isSearchingFinished = true;
                                        finishSearching = true;
                                        break;
                                    }
                                }
                                if (!finishSearching)
                                // moving down
                                {
                                    for (int k = 1; k < neighbourList.size(); k++) {
                                        CXGraphNode neighbourNode = (CXGraphNode) neighbourList.get(k);
                                        if (neighbourNode.location.y < searchingNode.location.y && neighbourNode.location.x == searchingNode.location.x) {
                                            newVertices.add(neighbourNode);
                                            searchingNode = neighbourNode;
                                            movingDirection++;
                                            isMoving = true;
                                            break;
                                        }
                                    }
                                    if (!isMoving){
                                        for (int k = 1; k < neighbourList.size(); k++) {
                                            CXGraphNode neighbourNode = (CXGraphNode) neighbourList.get(k);
                                            if (neighbourNode.location.x < searchingNode.location.x) {
                                                newVertices.add(neighbourNode);
                                                searchingNode = neighbourNode;
                                                break;
                                            }
                                        }
                                    }
                                }
                                break;
                            }
                            // Keep moving down until reached the started vertices
                            default:{
                                // Checking any vertices is started vertices
                                for (int k = 1; k < neighbourList.size(); k++) {
                                    CXGraphNode neighbourNode = (CXGraphNode) neighbourList.get(k);
                                    if (neighbourNode.nodeNumber == model.orignialVertices.nodeNumber) {
                                        isSearchingFinished = true;

                                        break;
                                    }
                                }
                                if (!isSearchingFinished){
                                    for (int k = 1; k < neighbourList.size(); k++) {
                                        CXGraphNode neighbourNode = (CXGraphNode) neighbourList.get(k);
                                        if (neighbourNode.location.y < searchingNode.location.y && neighbourNode.location.x == searchingNode.location.x) {
                                            newVertices.add(neighbourNode);
                                            searchingNode = neighbourNode;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }}
                    if (newVertices != null) {
                        newVerticesArrayList.add(newVertices);
                    }
            }
        }
        return newVerticesArrayList;
}
    private ArrayList deleteTheRepetedVertices(ArrayList arrayList){
        for (int i = 0; i < arrayList.size() ; i++) {
            ArrayList compareNodedList = (ArrayList) arrayList.get(i);
            for (int j = i+1; j < arrayList.size() ; j++) {
                ArrayList searchingNodeList = (ArrayList) arrayList.get(j);
                if (compareTwoArrayListIsTheSame(compareNodedList,searchingNodeList)){
                    arrayList.remove(searchingNodeList);
                    break;
                }
            }
        }
        return arrayList;
    }
    private  boolean compareTwoArrayListIsTheSame(ArrayList list1, ArrayList list2){
        if (list1.size() != list2.size()){
            return false;
        }
        int count = 0;
        for (int i = 0; i < list1.size() ; i++) {
            CXGraphNode compareNode = (CXGraphNode)list1.get(i);
            for (int j = 0; j < list2.size() ; j++) {
                CXGraphNode node = (CXGraphNode)list2.get(j);
                if (compareNode.nodeNumber == node.nodeNumber){
                    count++;
                    break;
                }
            }
        }
        return count == list1.size();
    }

    private CXGraph buildTheNewGraph(CXGraph decomposedGraph,ArrayList newVerticesArrayList){

        for (int i = 0; i < newVerticesArrayList.size() ; i++) {
            CXDecomposedGraphNode node = new CXDecomposedGraphNode();
            node.vertices = (ArrayList) newVerticesArrayList.get(i);
            // Need optimize the algorithm
            node.location = new CXPoint(decomposedGraph.allVertices.size()+1,decomposedGraph.allVertices.size()+1);
            decomposedGraph.add_vertex(node);
        }
        return decomposedGraph;
    }
    private CXGraph buildTheConnectionForTheVertices(CXGraph decomposedGraph,ArrayList extendedModelArray){
        // 1. Get all the edges from the extendedModelArray
        // There have some overlapped edge in extendedModelArray
        ArrayList edgeArrayList = new ArrayList();
        for (int i = 0 ; i <extendedModelArray.size();i++ ){
            CXExtendedModel model = (CXExtendedModel) extendedModelArray.get(i);
            for (int j = 0 ; j < model.extendedInfor.size(); j++ ){
                CXExtendInfoModel infoModel = (CXExtendInfoModel) model.extendedInfor.get(j);
                edgeArrayList.add(new CXPoint(model.orignialVertices.nodeNumber,infoModel.extendedVerticesNumber ));
            }
        }
        // 2. Get the all verticesArrray from decomposedGraph
        ArrayList newVertices = decomposedGraph.allVertices;
        // 3. for (i = 0 ; i < edgesArray.count; i ++ ) search all the verticesArray
        for (int i = 0; i < edgeArrayList.size() ; i++) {
            CXPoint point = (CXPoint) edgeArrayList.get(i);
            ArrayList neighbourVertices = new ArrayList();
            for (int j = 0; j < newVertices.size() ; j++) {
                   CXDecomposedGraphNode node = (CXDecomposedGraphNode) newVertices.get(j);
                   if(node.isContainTheNodes(point)){
                       neighbourVertices.add(node);
                   }
            }

            if (neighbourVertices.size() == 1) {
                System.out.println("Node " + point.x + " Node " + point.y +  " Left side of the vertices is left out" );
                decomposedGraph = addExtraVertices(decomposedGraph,this.extendedGraph,point);
                newVertices = decomposedGraph.allVertices;
                i--;
            }
            else {
                for (int j = 0; j < neighbourVertices.size()-1 ; j++) {
                    CXDecomposedGraphNode neighbourNode = (CXDecomposedGraphNode)neighbourVertices.get(j);
                    CXDecomposedGraphNode neighbourNode1 = (CXDecomposedGraphNode)neighbourVertices.get(j + 1);
                    System.out.println("Build neighbour relationship in DecomposeNode "+ neighbourNode.nodeNumber+ " " + neighbourNode1.nodeNumber );
                    decomposedGraph.add_edge( neighbourNode, neighbourNode1,1 );
                }
            }
        }

        return decomposedGraph;
    }


    private CXGraph addExtraVertices(CXGraph decomposedGraph,CXGraph extendedGraph, CXPoint edge){

        // Two kinds of scenario
        CXGraphNode node1 = extendedGraph.getVerticesInIndex((int)edge.x);
        CXGraphNode node2 = extendedGraph.getVerticesInIndex((int)edge.y);
        ArrayList node1Neighbours = extendedGraph.neighbors(node1);
        ArrayList node2Neighbours = extendedGraph.neighbors(node2);
        ArrayList decomposedGraphNodeVertices = new ArrayList();
        decomposedGraphNodeVertices.add(node1);
        decomposedGraphNodeVertices.add(node2);

        boolean isFinishSearching = false;
        // 1. if unsearched area is an angle
        for (int i = 1; i < node1Neighbours.size() ; i++) {
            CXGraphNode node =  (CXGraphNode) node1Neighbours.get(i);
            for (int j = 1; j < node2Neighbours.size() ; j++) {
                CXGraphNode node3 = (CXGraphNode) node2Neighbours.get(j);
                if (node.nodeNumber == node3.nodeNumber){
                    decomposedGraphNodeVertices.add(node);
                    isFinishSearching = true;
                }
            }
        }

        // 2. If unsearched area is an rectangular
        if (!isFinishSearching){

            // 1. choose the up vertices
            CXGraphNode searchingNode = node1.location.y > node2.location.y? node1:node2;

            CXGraphNode finishNode = node1.location.y < node2.location.y? node1:node2;

            // 2. moving up or left

            boolean movingLeft = false;
            while (!movingLeft) {
                ArrayList searchingNodeNeighbour = extendedGraph.neighbors(searchingNode);
                for (int i = 1; i < searchingNodeNeighbour.size(); i++) {
                    CXGraphNode neighbourNode = (CXGraphNode) searchingNodeNeighbour.get(i);
                    if (neighbourNode.location.x < searchingNode.location.x) {
                        decomposedGraphNodeVertices.add(neighbourNode);
                        searchingNode = neighbourNode;
                        movingLeft = true;
                    }
                }
                if (!movingLeft) {
                    for (int i = 1; i < searchingNodeNeighbour.size(); i++) {
                        CXGraphNode neighbourNode = (CXGraphNode) searchingNodeNeighbour.get(i);
                        if (neighbourNode.location.x == searchingNode.location.x && neighbourNode.location.y > searchingNode.location.y) {
                            decomposedGraphNodeVertices.add(neighbourNode);
                            searchingNode = neighbourNode;
                        }
                    }
                }
            }

            ArrayList list = extendedGraph.neighbors(searchingNode);

            while (!isFinishSearching){
                ArrayList neighbours = extendedGraph.neighbors(searchingNode);
                for (int i = 1; i < neighbours.size(); i++) {
                    CXGraphNode node = (CXGraphNode)neighbours.get(i);
                    if (node.nodeNumber == finishNode.nodeNumber){
                        isFinishSearching = true;
                        break;
                    }
                }
                if (!isFinishSearching) {
                    CXGraphNode nextSearchingNode = new CXGraphNode();
                    nextSearchingNode.location = new CXPoint(1000000, 1000000);
                    for (int i = 1; i < neighbours.size(); i++) {
                        CXGraphNode node = (CXGraphNode) neighbours.get(i);
                        if (!decomposedGraphNodeVertices.contains(node)) {
                            double shortestDistance = CXPoint.distance(nextSearchingNode.location, finishNode.location);
                            double currentDistance = CXPoint.distance(node.location, finishNode.location);
                            if ( shortestDistance > currentDistance ) {
                                nextSearchingNode = node;
                            }
                        }
                    }
                    searchingNode = nextSearchingNode;

                    decomposedGraphNodeVertices.add(nextSearchingNode);
                }
            }
        }
        CXDecomposedGraphNode node = new CXDecomposedGraphNode();
        node.location = new  CXPoint(decomposedGraph.allVertices.size()+1,decomposedGraph.allVertices.size()+1);
        node.vertices = decomposedGraphNodeVertices;
        decomposedGraph.add_vertex(node);
        System.out.println("DecomposedGraph add New Vertices" + node.nodeNumber);

        return decomposedGraph;
    }
}
