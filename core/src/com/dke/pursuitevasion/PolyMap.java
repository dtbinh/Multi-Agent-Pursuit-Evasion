package com.dke.pursuitevasion;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.math.DelaunayTriangulator;
import com.badlogic.gdx.utils.ShortArray;
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
    private int persuer = 1;
    private WallInfo[] wI;
    private AgentInfo[] aI;
    private EvaderInfo[] eI;
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

    public AgentInfo[] getaI() { return aI; }

    public EvaderInfo[] geteI() { return eI; }

    public void setAgentsInfo(ArrayList<AgentInfo> agentsInfo) {
        aI = agentsInfo.toArray(new AgentInfo[agentsInfo.size()]);
    }

    public void setEvaderInfo(ArrayList<EvaderInfo> evaderInfo){
        eI = evaderInfo.toArray(new EvaderInfo[evaderInfo.size()]);
    }

    public Mesh generateMesh(){
        polyVertexArray = generatePolyVertexArray();
        //polyIndexArray = generatePolyIndexArray();
        polyIndexArray = squarePolyIndexArray();
        VertexAttributes attributes = new VertexAttributes(VertexAttribute.Position(), VertexAttribute.TexCoords(0));

        Mesh mesh = new Mesh(true, polyVertexArray.length, polyIndexArray.length, attributes);

        mesh.setVertices(polyVertexArray);
        mesh.setIndices(polyIndexArray);

        System.out.println(polyIndexArray);
        System.out.println(polyVertexArray);
        System.out.println(mesh);

        return mesh;
    }

    private float[] generatePolyVertexArray(){
        float[] polyVertexArray = new float[20];

        for(int i=1; i<polyVertexArray.length;i+=5){
            System.out.println(polyVertexArray.length+"  "+i);
            if(i==1){
                polyVertexArray[i-1] = (float) Math.random()*-10;
                polyVertexArray[i]=0;
                polyVertexArray[i+1] = (float) Math.random()*-10;
                polyVertexArray[i+2] = 0;
                polyVertexArray[i+3] = 0;
            }else if(i==6){
                polyVertexArray[i-1] = (float) Math.random()*-10;
                polyVertexArray[i]=0;
                polyVertexArray[i+1] = (float) Math.random()*10;
                polyVertexArray[i+2] = 0;
                polyVertexArray[i+3] = 0;
            }else if(i==11) {
                polyVertexArray[i-1] = (float) Math.random()*10;
                polyVertexArray[i]=0;
                polyVertexArray[i+1] = (float) Math.random()*10;
                polyVertexArray[i+2] = 0;
                polyVertexArray[i+3] = 0;
            }else if(i==16){
                polyVertexArray[i-1] = (float) Math.random()*10;
                polyVertexArray[i]=0;
                polyVertexArray[i+1] = (float) Math.random()*-10;
                polyVertexArray[i+2] = 0;
                polyVertexArray[i+3] = 0;

            }
        }

        for(int j=0; j<polyVertexArray.length;j++){
            System.out.println(polyVertexArray[j]);
        }

        return polyVertexArray;
    }

 /*   private float[] generatePolyVertexArray(){
        float[] polyVertexArray = new float[20];

        for(int i=0; i<polyVertexArray.length; i++){
            float rand = (float) (Math.random()*3);
            if(i==1 || i==6 || i==11 || i==16){
                polyVertexArray[i] = 0;
            }else if(i==0||i==2||i==3||i==5||i==8||i==9||i==14||i==17){
                if(i==2||i==17){
                    rand = (float) Math.random()+2;
                }
                polyVertexArray[i] = -1*rand;
            }else if(i==10 || i==15){
                polyVertexArray[i] = (float) Math.random()+4;
            }else if(i==7 || i==12){
                polyVertexArray[i] = (float) Math.random()+2;
            }
            else{
                polyVertexArray[i] = rand;
            }

        }
        return polyVertexArray;
    }*/

    private short[] generatePolyIndexArray(){
        short[] polyIndexArray = new short[6];
        for(int i=0; i<polyIndexArray.length; i++){
            int rand = (int) Math.floor((Math.random()*4));
            polyIndexArray[i] = (short) rand;
        }
        return polyIndexArray;
    }

    private short[] squarePolyIndexArray(){
        short[] polyIndexarray = new short[6];
        polyIndexarray[0] = 3;
        polyIndexarray[1] = 0;
        polyIndexarray[2] = 2;
        polyIndexarray[3] = 0;
        polyIndexarray[4] = 1;
        polyIndexarray[5] = 2;
        return polyIndexarray;
    }

    /*

    private int[] indexArrayToInt(short[] polyIndexArray){
        int [] intArray = new int[polyIndexArray.length];
        for (int i=0; i<polyIndexArray.length;i++){
            intArray[i] =(int) polyIndexArray[i];
        }
        return intArray;
    }
  */
}
