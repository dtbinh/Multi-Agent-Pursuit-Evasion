package com.dke.pursuitevasion.CellDecompose.Graph;

import java.util.ArrayList;

/**
 * Created by chenxi on 4/16/17.
 */
public class CXDecomposedGraphNode extends CXGraphNode {
    public  CXDecomposedGraphNode(){
    }
    public ArrayList vertices;

    public boolean isContainTheNodes(ArrayList arrayList){
     return true;
    }
    public boolean isContainTheNodes(CXPoint point){
        int countNumber = 0;

        for (int i = 0; i < vertices.size() ; i++) {
            CXGraphNode node = (CXGraphNode) vertices.get(i);
            if (node.nodeNumber == point.x || node.nodeNumber == point.y){
                countNumber ++;
            }
        }
        return countNumber == 2;
    }

    // Haven't test
    public CXGraphNode getTopRightNode(){
        CXGraphNode topRightNode = (CXGraphNode) vertices.get(0);
        for (int i = 1 ; i < vertices.size();i++){
            CXGraphNode node = (CXGraphNode) vertices.get(i);
            if (node.location.x > topRightNode.location.x){
                topRightNode = node;
            }
            if (node.location.x == topRightNode.location.x && node.location.y > topRightNode.location.y){
                topRightNode = node;
            }
        }
        return topRightNode;
    }

    public CXGraphNode getTopLeftNode(){
        CXGraphNode topLeftNode = (CXGraphNode) vertices.get(0);
        for (int i = 1 ; i < vertices.size();i++){
            CXGraphNode node = (CXGraphNode)vertices.get(i);
            if (node.location.x < topLeftNode.location.x){
                topLeftNode = node;
            }
            if (node.location.x == topLeftNode.location.x && node.location.y > topLeftNode.location.y){
                topLeftNode = node;
            }
        }
        return topLeftNode;
    }

    public CXGraphNode getDownLeftNode(){
        CXGraphNode downLeftNode = (CXGraphNode)vertices.get(0);
        for (int i = 1; i < vertices.size() ; i++) {
            CXGraphNode node = (CXGraphNode)vertices.get(i);
            if (node.location.x < downLeftNode.location.x){
                downLeftNode = node;
            }
            if (node.location.x == downLeftNode.location.x && node.location.y < downLeftNode.location.y){
                downLeftNode = node;
            }
        }
        return downLeftNode;
    }


}
