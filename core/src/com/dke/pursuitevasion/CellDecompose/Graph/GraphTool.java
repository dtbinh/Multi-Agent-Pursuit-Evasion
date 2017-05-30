package com.dke.pursuitevasion.CellDecompose.Graph;

import sun.plugin2.os.windows.FLASHWINFO;

import java.awt.dnd.DnDConstants;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by chenxi on 4/16/17.
 */
public class GraphTool {
    public GraphTool (){

    }

    public static CXGraph combineGraph(CXGraph graph, CXGraph obstacle){
        CXGraph combinedGraph =  new CXGraph();
       try {
            combinedGraph = (CXGraph) CXGraph.deepCopy(graph);
       }
       catch (Exception e){
           System.out.println(e);
       }

        ArrayList[] graph1VerticesList = obstacle.getVerticesList();
        for (int i = 0; i < graph1VerticesList.length ; i++){
            if (graph1VerticesList[i] != null){
                ArrayList list = graph1VerticesList[i];
                combinedGraph.addVertexes(list,i);
            }
        }
        return combinedGraph;

    }

    public  Boolean isConnectedInClosedPath(CXGraph graph, CXGraphNode originalNode, CXGraphNode searchingNode){

        HashMap map = new HashMap();
        map.put(originalNode.location,originalNode);
        int count = 0;
        count = this.connected(graph,originalNode,searchingNode,map,count);
        if (count == 2)return true;
        return false;
    }

    private int connected(CXGraph graph, CXGraphNode orginalNode, CXGraphNode searchingNode , HashMap map , int count){
        ArrayList neighbours = graph.neighbors(orginalNode);
        for (int i = 1 ; i < neighbours.size(); i++){
            CXGraphNode neighbourNode = (CXGraphNode) neighbours.get(i);
            if (neighbourNode.location == searchingNode.location){
                count++;
                if (count == 2) {
                    return count;
                }
            }
            if (!map.containsKey(neighbourNode.location)){
                map.put(neighbourNode.location,neighbourNode);
              count =  connected(graph,neighbourNode,searchingNode,map,count);
            }

        }
        return count;
    }

    public static CXDecomposedGraphNode findTheFarleftNode(CXGraph graph){
        ArrayList allVertices = graph.allVertices;
        CXDecomposedGraphNode node = (CXDecomposedGraphNode) allVertices.get(0);
        for (int i = 1; i < allVertices.size() ; i++) {
            double nodeX = node.getTopRightNode().location.x;
            CXDecomposedGraphNode node1  = (CXDecomposedGraphNode)allVertices.get(i);
            double nodeX1 = node1.getTopRightNode().location.x;
            node = nodeX>nodeX1?node1:node;
        }
        return node;
    }

    public static CXDecomposedGraphNode findTheFarRightNode(CXGraph graph){
        ArrayList allVertices = graph.allVertices;
        CXDecomposedGraphNode node = (CXDecomposedGraphNode) allVertices.get(0);
        for (int i = 1; i < allVertices.size() ; i++) {
            double nodeX = node.getTopRightNode().location.x;
            CXDecomposedGraphNode node1  = (CXDecomposedGraphNode)allVertices.get(i);
            double nodeX1 = node1.getTopRightNode().location.x;
            node = nodeX<nodeX1?node1:node;
        }
        return node;
    }
    public static ArrayList findTheRightNeighbours(CXGraph graph, CXDecomposedGraphNode currentNode){
        ArrayList neighbours = graph.neighbors(currentNode);
        ArrayList rightNeighbours = new ArrayList();
        double topLeftNodeX =  currentNode.getTopLeftNode().location.x;

        for (int i = 1; i < neighbours.size() ; i++) {
            CXDecomposedGraphNode DNode = (CXDecomposedGraphNode) neighbours.get(i);
            CXGraphNode node = (CXGraphNode) DNode.vertices.get(0);
            if (node.location.x > topLeftNodeX) rightNeighbours.add(DNode);
        }

        return rightNeighbours;
    }
    public static ArrayList findtheLeftNeighbours(CXGraph graph, CXDecomposedGraphNode currentNode){
        ArrayList neighbours = graph.neighbors(currentNode);
        ArrayList rightNeighbours = new ArrayList();
        double topRightNodeX =  currentNode.getTopRightNode().location.x;

        for (int i = 1; i < neighbours.size() ; i++) {
            CXDecomposedGraphNode DNode = (CXDecomposedGraphNode) neighbours.get(i);
            CXGraphNode node = (CXGraphNode) DNode.vertices.get(0);
            if (node.location.x < topRightNodeX) rightNeighbours.add(DNode);
        }
        return rightNeighbours;
    }


}
