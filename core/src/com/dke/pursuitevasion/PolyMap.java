package com.dke.pursuitevasion;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;

/**
 * Created by jeeza on 23-2-17.
 */
public class PolyMap {
    private String name;

    private float[] polyVertexArray;
    private short[] polyIndexArray;
    private WallInfo[] wI;
    private EdgeVectors[] eV;

    public PolyMap(String name) {
        this.name = name;
    }

    public void setPolygonMesh(Mesh mesh) {
        polyIndexArray = new short[mesh.getNumIndices()];
        mesh.getIndices(polyIndexArray);
        System.out.println(mesh.getNumVertices()+" num verts");
        polyVertexArray = new float[mesh.getNumVertices() * 5];

        System.out.printf("Vertex num %s vertex size %s float size %s", mesh.getNumVertices(), mesh.getVertexSize(), 4);

        mesh.getVertices(polyVertexArray);
    }

    public Mesh getPolygonMesh() {
        VertexAttributes attributes = new VertexAttributes(VertexAttribute.Position(), VertexAttribute.TexCoords(0));
        Mesh mesh = new Mesh(true,polyVertexArray.length, polyIndexArray.length, attributes);
        mesh.setVertices(polyVertexArray);
        mesh.setIndices(polyIndexArray);
        return mesh;
    }

    public void export() {
        GsonBuilder gsonBuilder = new GsonBuilder().setPrettyPrinting();
        Gson gson = gsonBuilder.create();

        System.out.println(gson.toJson(this));
        Gdx.files.local("maps/" + name).writeString(gson.toJson(this),false);
    }

    public void setWalls(ArrayList<WallInfo> wallInfo){
        wI = wallInfo.toArray(new WallInfo[wallInfo.size()]);
    }

    public void setEdgeVectors(ArrayList<EdgeVectors> edgeVectorsInfo){
        eV = edgeVectorsInfo.toArray(new EdgeVectors[edgeVectorsInfo.size()]);
    }

    public WallInfo[] getwI() {
        return wI;
    }

    public EdgeVectors[] geteV(){
        return eV;
    }

}
