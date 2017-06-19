package com.dke.pursuitevasion.CellDecompose.Graph;

import java.io.*;
import java.util.ArrayList;

/**
 * Created by chenxi on 3/7/17.
 */
public class CXGraph<E> implements Serializable{


    // The vertices have to be add in order, can't remove any vertices otherwise the graph doesn't work properly
    // only if you write your own remove method
    private int [][] edgeValueArray = new int[1000][1000];
    private ArrayList[] verticesList  = new ArrayList[1000];  // can use Dictionary instead of ArrayList
    public ArrayList allVertices = new ArrayList();

    /*
    verticesList structure
    1 . [] - [node,next] - [neithbourNode,next] - [neithbourNode,next]
    2.  [] - [node,next] - [neithbourNode,next] - [neithbourNode,next]
    .
     */

    public  CXGraph(){
    }

    public ArrayList[] getVerticesList() {
        return verticesList;
    }

    public CXGraphNode getVerticesInIndex(int index){
        ArrayList list = this.verticesList[index];
        return  (CXGraphNode) list.get(0);
    }

    public int[][] getEdgeValueArray() {
        return edgeValueArray;
    }

    public  CXGraph(int[][] edgeValueArray , ArrayList[] verticeList){
        this.edgeValueArray = edgeValueArray;
        this.verticesList = verticeList;
    }

    public ArrayList neighbors(CXGraphNode x) {
        return verticesList[x.nodeNumber];
    }

    public boolean add_edge(CXGraphNode x, CXGraphNode y , int value) {
        this.add_vertex(x);
        this.add_vertex(y);
        if (adjacency(x,y)){
            return true;
        }
        if (verticesList == null){
            System.out.print("vertices is empty");
        }
        verticesList[x.nodeNumber].add(y);
        verticesList[y.nodeNumber].add(x);
        edgeValueArray[x.nodeNumber][y.nodeNumber] = value;
        edgeValueArray[y.nodeNumber][x.nodeNumber] = value;
        return true;
    }
    public boolean adjacency(CXGraphNode x, CXGraphNode y) {
        if(edgeValueArray[x.nodeNumber][y.nodeNumber] == 0){
            return false;
        }
        return true;
    }

    public CXGraphNode add_vertex(CXGraphNode x) {
        int value = this.containVertices(x.location);
        if (value != -1)
        {
            return (CXGraphNode) this.allVertices.get(value);
        }
            x.nodeNumber = this.allVertices.size()+1;
            this.allVertices.add(x);
            ArrayList list = new ArrayList();
            list.add(x);
            verticesList[x.nodeNumber] = list;
            return x;

    }

    // For combine graph
    public boolean addVertexes(ArrayList list, int index){
        if (list == null || index == 0){
            return  false;
        }
//        verticesList[index] = list;
        CXGraphNode node = (CXGraphNode) list.get(0);
        this.add_vertex(node);
        for (int i = 1 ; i < list.size(); i++){
            CXGraphNode neighbourNode = (CXGraphNode) list.get(i);
            this.add_edge(node,neighbourNode,1);
        }
        return true;
    }

    public boolean remove_edge(CXGraphNode x, CXGraphNode y) {
        ArrayList listX = verticesList[x.nodeNumber];

        if (listX.contains(y)){
          edgeValueArray[x.nodeNumber][y.nodeNumber] = 0;
          listX.remove(y);
          ArrayList listY = verticesList[y.nodeNumber];
          listY.remove(x);
        }
        return true;
    }



    public boolean remove_vertex(CXGraphNode x) {
        verticesList[x.nodeNumber] = null;

        for (int i = 1; i < edgeValueArray.length+1 ; i++) {
            edgeValueArray[x.nodeNumber][i] = 0;
            edgeValueArray[i][x.nodeNumber] = 0;
        }
        return true;
    }

    public int get_edge_value(CXGraphNode x, CXGraphNode y) {
        return edgeValueArray[x.nodeNumber][y.nodeNumber];
    }

    public boolean set_edge_value(CXGraphNode x, CXGraphNode y, int v) {
        edgeValueArray[x.nodeNumber][y.nodeNumber] = edgeValueArray[y.nodeNumber][x.nodeNumber] =  v;
        return true;
    }

    public static int getVerticesNumberInGraph(CXGraph graph){
        // The Graph has to be add Vertices in the sequence, and can't remove any vertices.
        int sumOfVertices = 0;
        for (int i = 0; i < graph.getVerticesList().length ; i++) {
            ArrayList list = graph.getVerticesList()[i];
            if (list != null){
                sumOfVertices++;
                continue;
            }
            return sumOfVertices;
        }
        return sumOfVertices;
    }

    public int containVertices(CXPoint point){
        // Use dictionary to save
        for (int i = 0; i < this.allVertices.size(); i++) {
            CXGraphNode node = (CXGraphNode) this.allVertices.get(i);
            if (node.location.y == point.y && node.location.x    == point.x ){
                return i;
            }
        }
        return -1;
    }

    // Deep copy
    public static Object deepCopy(Object o) throws IOException, ClassNotFoundException {
//      //先序列化，写入到流里
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        ObjectOutputStream oo = new ObjectOutputStream(bo);
        oo.writeObject(o);
        //然后反序列化，从流里读取出来，即完成复制
        ByteArrayInputStream bi = new ByteArrayInputStream(bo.toByteArray());
        ObjectInputStream oi = new ObjectInputStream(bi);
        return oi.readObject();
    }


}
