package com.dke.pursuitevasion.Editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.EarClippingTriangulator;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.ShortArray;
import com.dke.pursuitevasion.PolyMap;
import com.dke.pursuitevasion.UI.FileSaver;

import java.util.ArrayList;


public class MapEditorController {

    private Mode mode = Mode.DO_NOTHING;

    private ArrayList<ModelInstance> instances;
    private ArrayList<Vector3> instanceVectors;
    private float[] vertList = new float[0];
    private short[] mIndices;
    private int vertListSize;

    private ModelBuilder modelBuilder;
    private Mesh polygonMesh;
    private ModelInstance polygonModel;
    ModelInstance mWall, mWallPerm;

    public MapEditorController() {
        instances= new ArrayList<ModelInstance>();
        instanceVectors = new ArrayList<Vector3>();
        modelBuilder = new ModelBuilder();

        initMesh();
    }

    private void initMesh() {
        polygonMesh = new Mesh(true, 4, 6, new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"));
        //mesh.setVertices(new float[]{-1f, -1f, 0f,0f, -1f, 0f,0f, 0f, 0f,-1f, 0f, 0f,});
        polygonMesh.setVertices(new float[]{0f, 0f, 0f,0f, 0f, 0,0f, 0f, 0f,0f, 0f, 0f,});
        polygonMesh.setIndices(new short[] {0, 1, 2, 2, 3, 0,});
        Material material = new Material(ColorAttribute.createDiffuse(Color.WHITE));
        modelBuilder.begin();
        modelBuilder.part("Temp", polygonMesh, GL20.GL_TRIANGLES, material);
        Model tmp_model = modelBuilder.end();
        polygonModel = new ModelInstance(tmp_model, 0,0,0);
    }

    public void remakePolygonMesh() {
        //Remove y float from vertList
        int newLength = (2*vertList.length)/3;
        int forLoopNum = (vertList.length/3);
        float[] newVertList  = new float[newLength];
        ArrayList<Float> temp = new ArrayList<Float>();
        for (int i=0; i<forLoopNum; i++) {
            temp.add(vertList[i*3]);
            temp.add(vertList[i*3+2]);
        }
        for (int i=0; i<temp.size(); i++)
        {
            newVertList[i] = temp.get(i);
        }

        EarClippingTriangulator triangulator = new EarClippingTriangulator();
        ShortArray meshIndices = triangulator.computeTriangles(newVertList);

        short[] indices = new short[meshIndices.size];
        for (int i=0; i<meshIndices.size;i++)
        {
            indices[i] = meshIndices.get(i);
        }
        mIndices = indices;
        polygonMesh = new Mesh(true, vertList.length, meshIndices.size,
                new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"));
        polygonMesh.setVertices(vertList);
        polygonMesh.setIndices(indices);
        Material material = new Material(ColorAttribute.createDiffuse(Color.GOLD));
        modelBuilder.begin();
        modelBuilder.part("Map", polygonMesh, GL20.GL_TRIANGLES, material);
        Model tmp_model = modelBuilder.end();
        polygonModel = new ModelInstance(tmp_model, 0,0,0);

    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public ModelInstance getPolygonModel() {
        return polygonModel;
    }

    public ArrayList<ModelInstance> getInstances() {
        return instances;
    }

    public ArrayList<Vector3> getInstanceVectors() {
        return instanceVectors;
    }

    private boolean nearNeighbor(Vector3 vec) {
        for (int i=0; i<instanceVectors.size(); i++) {
            Vector3 v = instanceVectors.get(i);
            if (vec.x<v.x+0.5 && vec.x>v.x-0.5 && vec.z<v.z+0.5 && vec.z>v.z-0.5)
                return true;
        }
        return false;
    }

    public void addOuterVertex(int screenX, int screenY, PerspectiveCamera camera) {
        // Translate screen coordinate
        Ray pickRay = camera.getPickRay(screenX, screenY);
        Vector3 intersection = new Vector3();
        Intersector.intersectRayPlane(pickRay, new Plane(new Vector3(0f,1f,0f),0f), intersection);

        String verts = intersection.toString();
        verts = verts.replaceAll("[()]","");
        verts = verts.replaceAll("[,]",", ");
        String[] Array = verts.split(",");

        resizeArray(vertList);
        for(int i = 0; i < 3; i++) {
            vertList[vertListSize+i] = Float.parseFloat(Array[i]);
        }
        vertListSize+=3;

        // Create a model of the vertex at the translated position screenX, screenY
        Model vertexPos = modelBuilder.createSphere(0.15f, 0.15f, 0.15f, 20, 20, new Material(ColorAttribute.createDiffuse(Color.LIGHT_GRAY)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);

        if(!nearNeighbor(intersection)) {
            ModelInstance vPosInst = new ModelInstance(vertexPos, intersection);
            instances.add(vPosInst);
            instanceVectors.add(intersection);
        }
    }

    public void addWall(Vector3 click, Vector3 clickDrag){
        Vector3 cClick = click.cpy();
        Vector3 cClickDrag = clickDrag.cpy();
        float width = Math.abs(click.x - clickDrag.x);
        float depth = Math.abs(click.z - clickDrag.z);
        float distance = click.dst(clickDrag);

        if(width<0.1f)
            width = 0.1f;
        if(depth<0.1f)
            depth = 0.1f;
        float height = 0.125f;
        Vector3 midPoint = ((click.sub(clickDrag)).scl(0.5f)).add(clickDrag);
        midPoint.y +=height/2;

        Model wall = modelBuilder.createBox(distance, height, 0.08f,new Material(ColorAttribute.createDiffuse(Color.SCARLET)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        mWall = new ModelInstance(wall, midPoint);
        Model wallPerm = modelBuilder.createBox(distance, height, 0.08f,new Material(ColorAttribute.createDiffuse(Color.DARK_GRAY)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        mWallPerm = new ModelInstance(wallPerm, midPoint);


        Vector3 difference = (cClick.sub(cClickDrag));
        Vector3 xAxis = new Vector3(1, 0, 0);
        float dotProd = difference.dot(xAxis);
        Vector3 origin = new Vector3(0, 0, 0);
        dotProd = dotProd / (difference.dst(origin) * xAxis.dst(origin));
        double dotResult = (double) dotProd;
        double angle = Math.acos(dotResult);
        float floatAngle = (float) angle;
        if (difference.z > 0)
            floatAngle *= -1;
        mWall.transform.rotateRad(new Vector3(0, 1, 0), floatAngle);
        mWallPerm.transform.rotateRad(new Vector3(0, 1, 0), floatAngle);
    }

    public void addWallToArray(){
        mWall = null;
        if(mWallPerm!=null)
        instances.add(mWallPerm);
    }

    private void resizeArray(float[] oldVertList) {
        // create a new array of size+3
        int newSize = oldVertList.length + 3;
        float[] newArray = new float[newSize];
        System.arraycopy(oldVertList, 0, newArray, 0, oldVertList.length);
        vertList = newArray;
    }

    public void saveFile(Stage stage, Skin skin) {
        FileSaver files = new FileSaver("Save Map File", skin) {
            @Override
            protected void result(Object object) {
                String fileName = getFileName();
                System.out.println("File Name: " + fileName);
                PolyMap map = new PolyMap(fileName);
                map.setPolygonMesh(polygonMesh);
                map.export();
            }
        };
        files.setDirectory(Gdx.files.local("maps/"));
        files.show(stage);
    }


    public void dispose() {
        polygonMesh.dispose();
    }

    public short[] getmIndices(){
        return mIndices;
    }

    public float[] getVertList(){
        return vertList;
    }
}

