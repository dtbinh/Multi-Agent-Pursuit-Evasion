package com.dke.pursuitevasion;

import com.badlogic.gdx.math.Vector3;

import java.util.ArrayList;

public class Edges
{
    public static ArrayList<Edge> edges = new ArrayList<Edge>();
    public static ArrayList<Edge> border = new ArrayList<Edge>();
    public static ArrayList<EdgeVectors> vectorEdges = new ArrayList<EdgeVectors>();

    public static ArrayList<EdgeVectors> computeEdges(int triangleIndices[], float[] vertList)
    {
        for (int i=0; i<triangleIndices.length; i+=3)
        {
            int i0 = triangleIndices[i+0];
            int i1 = triangleIndices[i+1];
            int i2 = triangleIndices[i+2];

            Edge edge1 = new Edge(i0, i1);
            Edge edge2 = new Edge(i1, i2);
            Edge edge3 = new Edge(i2, i0);

            edges.add(edge1);
            edges.add(edge2);
            edges.add(edge3);
        }

        for(int j=0;j<edges.size();j++){
            addIfEdge(edges.get(j));
        }

        for(int i=0;i<border.size();i++){
            int index = border.get(i).index0;
            int index2 = border.get(i).index1;
            Vector3 vertex1 = new Vector3(vertList[index*3], 0, vertList[index*3+2]);
            Vector3 vertex2 = new Vector3(vertList[index2*3], 0, vertList[index2*3+2]);
            EdgeVectors edgeVector = new EdgeVectors();
            edgeVector.Vector1 = vertex1;
            edgeVector.Vector2 = vertex2;

            vectorEdges.add(edgeVector);
        }

        System.out.println(edges.toString());

        /*for(int i=0;i<vectorEdges.size();i++) {
            System.out.println(vectorEdges.get(i).Vector1+"   "+vectorEdges.get(i).Vector2);
        }*/

        return vectorEdges;
    }

    public static void addIfEdge(Edge edge){
        int counter = 0;

        for(int j=0;j<edges.size();j++){
            if (edges.get(j).index0 == edge.index0){
                if (edges.get(j).index1 == edge.index1) {
                    counter++;
                }
            }
                if (edges.get(j).index0 == edge.index1) {
                    if (edges.get(j).index1 == edge.index0) {
                        counter++;
                    }
                }
        }
        if(counter==1){
            border.add(edge);
        }
    }
}