package com.dke.pursuitevasion.Editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ShortArray;
import com.dke.pursuitevasion.*;
import com.dke.pursuitevasion.UI.FileChooser;
import com.dke.pursuitevasion.UI.FileSaver;
import sun.management.Agent;

import java.util.*;


public class MapEditorController {

    private Mode mode = Mode.DO_NOTHING;

    private ArrayList<ModelInstance> instancesSpheres;
    public ArrayList<ModelInstance> agentInstances;
    public ArrayList<ModelInstance> evaderInstances;
    public ArrayList<ModelInstance> wallInstances;
    //public ArrayList<ModelInstance> instances;
    private ArrayList<Vector3> instanceVectors;
    public ArrayList<WallInfo> wallInfo;
    public ArrayList<AgentInfo> agentsInfo;
    public ArrayList<EvaderInfo> evaderInfo;
    public ArrayList<EdgeVectors> edges;

    private float[] vertList = new float[0];
    private short[] mIndices;
    private int vertListSize;
    public Texture texture;
    public PolyMap localMap;


    //for wall info
    private float Distance;
    private float Height;
    private Vector3 Midpoint;
    private float Angle;
    private Vector3 currentVector;
    private Vector3 nextVector;


    private ModelBuilder modelBuilder;
    private Mesh polygonMesh;
    private ModelInstance polygonModel;
    public boolean meshRenderable, meshRendered, delauneyTri = true;
    ModelInstance mWall, mWallPerm;

    public MapEditorController() {
        //instances= new ArrayList<ModelInstance>();
        instancesSpheres= new ArrayList<ModelInstance>();
        agentInstances = new ArrayList<ModelInstance>();
        evaderInstances = new ArrayList<ModelInstance>();
        wallInstances = new ArrayList<ModelInstance>();
        instanceVectors = new ArrayList<Vector3>();
        wallInfo = new ArrayList<WallInfo>();
        agentsInfo = new ArrayList<AgentInfo>();
        evaderInfo = new ArrayList<EvaderInfo>();
        modelBuilder = new ModelBuilder();
        edges = new ArrayList<EdgeVectors>();

        initMesh();
        initTexture();

    }

    private void initTexture(){
        FileHandle img = Gdx.files.internal("blueprint-1.jpg");
        texture = new Texture(img, Pixmap.Format.RGB565, false);
        texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        texture.setFilter(Texture.TextureFilter.Linear,
                Texture.TextureFilter.Linear);

    }

    private void setTextureCoords(){
        float[] textureVertList = new float[vertListSize/3*2+vertListSize];
        //copy elements from old vertlist to new vertlist skipping texture positions
        for(int i=0; i<vertList.length/3;i++){
            textureVertList[i*5] = vertList[i*3];
            textureVertList[i*5+1] = vertList[i*3+1];
            textureVertList[i*5+2] = vertList[i*3+2];
        }
        vertList = textureVertList;
        setUV(vertList);
        meshRendered = true;
    }

    //set coordinates for the texture
    private void setUV(float[] vertArray){
        float u = ((float) Gdx.graphics.getWidth()) / texture.getWidth()/3;
        float v = ((float) Gdx.graphics.getHeight()) / texture.getHeight()/3;
        for(int i=0;i<vertList.length/5;i++){
            vertArray[i*5+3] = u*vertArray[i*5];
            vertArray[i*5+4] = -v*vertArray[i*5+2];
        }
    }

    private void initMesh() {
        polygonMesh = new Mesh(true, 4, 6, new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"));
        polygonMesh.setVertices(new float[]{1f, 1f, 0f, 1f, -1f, 0f, -1f, -1f, 0f, -1f, 1f, 0f,});
        polygonMesh.setIndices(new short[] {0, 1, 2, 2, 3, 0,});
        Material material = new Material(ColorAttribute.createDiffuse(Color.RED));
        modelBuilder.begin();
        modelBuilder.part("Temp", polygonMesh, GL20.GL_TRIANGLES, material);
        Model tmp_model = modelBuilder.end();
        polygonModel = new ModelInstance(tmp_model, 0,0,0);
    }

    public void remakePolygonMesh() {
        meshRenderable = false;
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

        ShortArray meshIndices;
        if(!delauneyTri) {
            EarClippingTriangulator triangulator = new EarClippingTriangulator();
            meshIndices = triangulator.computeTriangles(newVertList);
        }else{
            DelaunayTriangulator triangulator = new DelaunayTriangulator();
            meshIndices = triangulator.computeTriangles(newVertList, false);
        }


        int intArray[] = new int[meshIndices.size];
        for(int i = 0; i < meshIndices.size; i++)
        {
            intArray[i] = (int)meshIndices.get(i);
        }
        edges = Edges.computeEdges(intArray, vertList);

        short[] indices = new short[meshIndices.size];
        for (int i=0; i<meshIndices.size;i++)
        {
            indices[i] = meshIndices.get(i);
        }
        mIndices = indices;
        setTextureCoords();
        polygonMesh = new Mesh(true, vertList.length, meshIndices.size,
                new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
                new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoord0"));
        polygonMesh.setVertices(vertList);
        polygonMesh.setIndices(indices);
        Material material = new Material(ColorAttribute.createDiffuse(Color.GOLD));
        modelBuilder.begin();
        modelBuilder.part("Map", polygonMesh, GL20.GL_TRIANGLES, material);
        Model tmp_model = modelBuilder.end();
        polygonModel = new ModelInstance(tmp_model, 0,0,0);
        meshRenderable = true;
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

    public Mesh getPolygonMesh(){
        return polygonMesh;
    }

    public void setPolygonMesh(Mesh polygonMesh) {
        this.polygonMesh = polygonMesh;
    }

    /*public ArrayList<ModelInstance> getInstances() {
        //return instances;
    }*/

    public ArrayList<Vector3> getInstanceVectors() {
        return instanceVectors;
    }

    private boolean nearNeighbor(Vector3 vec) {
        for (int i=0; i<instanceVectors.size(); i++) {
            Vector3 v = instanceVectors.get(i);
            float tolerance = 0.5f;
            if (vec.x<v.x+tolerance && vec.x>v.x-tolerance && vec.z<v.z+tolerance && vec.z>v.z-tolerance)
                return true;
        }
        return false;
    }

    public void removeObject(int screenX, int screenY, PerspectiveCamera camera){
        Ray pickRay = camera.getPickRay(screenX, screenY);
        Vector3 vec = new Vector3();
        Intersector.intersectRayPlane(pickRay, new Plane(new Vector3(0f,1f,0f),0f), vec);
        boolean bool = true;
        if(bool) {
            for (int i = 0; i < agentsInfo.size(); i++) {
                Vector3 v = agentsInfo.get(i).position;
                float tolerance = 0.1f;
                if (vec.x < v.x + tolerance && vec.x > v.x - tolerance && vec.z < v.z + tolerance && vec.z > v.z - tolerance) {
                    agentsInfo.remove(i);
                    agentInstances.clear();
                    ArrayList<Vector3> tmpCpy = new ArrayList<Vector3>();
                    for(int k=0;k<agentsInfo.size();k++){
                        tmpCpy.add(agentsInfo.get(k).position.cpy());
                    }
                    agentsInfo.clear();
                    for (int j = 0; j < tmpCpy.size(); j++) {
                        addAgent(0, 0, null, false, tmpCpy.get(j));
                    }
                    bool = false;
                    break;
                }

            }
        }

        if(bool) {
            for (int i = 0; i < evaderInfo.size(); i++) {
                Vector3 v = evaderInfo.get(i).position;
                float tolerance = 0.2f;
                if (vec.x < v.x + tolerance && vec.x > v.x - tolerance && vec.z < v.z + tolerance && vec.z > v.z - tolerance) {
                    evaderInfo.remove(i);
                    evaderInstances.clear();
                    ArrayList<Vector3> tmpCpy = new ArrayList<Vector3>();
                    for(int k=0;k<evaderInfo.size();k++){
                        tmpCpy.add(evaderInfo.get(k).position.cpy());
                    }
                    evaderInfo.clear();
                    for (int j = 0; j < tmpCpy.size(); j++) {
                        addEvader(0, 0, null, tmpCpy.get(j));
                    }
                    bool = false;
                    break;
                }
            }
        }

        if(bool) {
            for (int i = 0; i < wallInfo.size(); i++) {
                float tolerance = 0.15f;
                Vector2 point = new Vector2(vec.x, vec.z);
                Vector2 lineStart = new Vector2(wallInfo.get(i).start.x, wallInfo.get(i).start.z);
                Vector2 lineEnd = new Vector2(wallInfo.get(i).end.x, wallInfo.get(i).end.z);
                float dist = Intersector.distanceSegmentPoint(lineStart, lineEnd, point);
                if (dist < tolerance) {
                    wallInfo.remove(i);

                    ArrayList<Vector3> tmpCpy = new ArrayList<Vector3>();

                    for (int k = 0; k < wallInfo.size(); k++) {
                        tmpCpy.add(wallInfo.get(k).start.cpy());
                        tmpCpy.add(wallInfo.get(k).end.cpy());
                    }
                    wallInfo.clear();
                    wallInstances.clear();

                    for (int j = 0; j < tmpCpy.size() / 2; j++) {
                        addWall(tmpCpy.get(j * 2).cpy(), tmpCpy.get(j * 2 + 1).cpy());
                        addWallToArray();
                    }
                    break;
                }
            }
        }

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


        ModelInstance vPosInst = new ModelInstance(vertexPos, intersection);
        instancesSpheres.add(vPosInst);
        instanceVectors.add(intersection);
    }

    public void addAgent(int screenX, int screenY, PerspectiveCamera camera, boolean isCCTV, Vector3 postion) {
        if(postion==null){
            Ray pickRay = camera.getPickRay(screenX, screenY);
            Vector3 intersection = new Vector3();
            Intersector.intersectRayPlane(pickRay, new Plane(new Vector3(0f, 1f, 0f), 0f), intersection);


            setAgentInfo(intersection, isCCTV);

            if(isCCTV){
                Model cctvModel = modelBuilder.createSphere(0.15f, 0.15f, 0.15f, 20, 20, new Material(ColorAttribute.createDiffuse(Color.BLACK)),
                        VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
                if(!nearNeighbor(intersection)) {
                    ModelInstance cctvInstance = new ModelInstance(cctvModel, intersection);
                    //instances.add(cctvInstance);
                    agentInstances.add(cctvInstance);
                }
            }else{
                Model agentModel = modelBuilder.createSphere(0.15f, 0.15f, 0.15f, 20, 20, new Material(ColorAttribute.createDiffuse(Color.BLUE)),
                        VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
                if(!nearNeighbor(intersection)) {
                    ModelInstance agentInstance = new ModelInstance(agentModel, intersection);
                    //instances.add(agentInstance);
                    agentInstances.add(agentInstance);
                }
            }
        }else{

            setAgentInfo(postion, isCCTV);

            if(isCCTV){
                Model cctvModel = modelBuilder.createSphere(0.15f, 0.15f, 0.15f, 20, 20, new Material(ColorAttribute.createDiffuse(Color.BLACK)),
                        VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
                if(!nearNeighbor(postion)) {
                    ModelInstance cctvInstance = new ModelInstance(cctvModel, postion);
                    //instances.add(cctvInstance);
                    agentInstances.add(cctvInstance);

                }
            }else{
                Model agentModel = modelBuilder.createSphere(0.15f, 0.15f, 0.15f, 20, 20, new Material(ColorAttribute.createDiffuse(Color.BLUE)),
                        VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
                if(!nearNeighbor(postion)) {
                    ModelInstance agentInstance = new ModelInstance(agentModel, postion);
                    //instances.add(agentInstance);
                    agentInstances.add(agentInstance);

                }
            }
        }

    }

    public void addEvader(int screenX,int screenY,PerspectiveCamera camera, Vector3 position){
        if(position==null){
            Ray pickRay = camera.getPickRay(screenX, screenY);
            Vector3 intersection = new Vector3();
            Intersector.intersectRayPlane(pickRay, new Plane(new Vector3(0f, 1f, 0f), 0f), intersection);



            setEvaderInfo(intersection);

            Model evaderModel = modelBuilder.createSphere(0.15f, 0.15f, 0.15f, 20, 20, new Material(ColorAttribute.createDiffuse(Color.RED)),
                    VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);

            if(!nearNeighbor(intersection)) {
                ModelInstance evaderInstance = new ModelInstance(evaderModel, intersection);
                //instances.add(evaderInstance);
                evaderInstances.add(evaderInstance);
            }
        }else{
            setEvaderInfo(position);

            Model evaderModel = modelBuilder.createSphere(0.15f, 0.15f, 0.15f, 20, 20, new Material(ColorAttribute.createDiffuse(Color.RED)),
                    VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);

            if(!nearNeighbor(position)) {
                ModelInstance evaderInstance = new ModelInstance(evaderModel, position);
                //instances.add(evaderInstance);
                evaderInstances.add(evaderInstance);
            }
        }

    }


    public void addWall(Vector3 click, Vector3 clickDrag){
        currentVector = click.cpy();
        nextVector = clickDrag.cpy();
        Vector3 cClick = click.cpy();
        Vector3 cClickDrag = clickDrag.cpy();

        float width = Math.abs(click.x - clickDrag.x);
        float depth = Math.abs(click.z - clickDrag.z);
        float distance = click.dst(clickDrag);

        if(width<0.1f)
            width = 0.1f;
        if(depth<0.1f)
            depth = 0.1f;
        float height = 0.06f;
        Vector3 midPoint = ((click.sub(clickDrag)).scl(0.5f)).add(clickDrag);
        midPoint.y +=height/2+0.01f;
        Color wallColor = new Color(156,229,251,0);

        Model wall = modelBuilder.createBox(distance, height, 0.08f,new Material(ColorAttribute.createDiffuse(wallColor)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        mWall = new ModelInstance(wall, midPoint);
        Model wallPerm = modelBuilder.createBox(distance, height, 0.08f,new Material(ColorAttribute.createDiffuse(wallColor)),
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

        Distance = distance;
        Height = height;
        Angle = floatAngle;
        Midpoint = midPoint;
    }

    public void addWallToArray(){
        mWall = null;
        if(mWallPerm!=null)
            wallInstances.add(mWallPerm);
        //instances.add(mWallPerm);
        setWallInfo(Distance, Height, Angle, Midpoint, currentVector, nextVector);
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
                map.setWalls(wallInfo);
                map.setEdgeVectors(edges);
                map.setAgentsInfo(agentsInfo);
                map.setEvaderInfo(evaderInfo);
                localMap = map;
                map.export();
            }
        };
        files.setDirectory(Gdx.files.local("maps/"));
        files.show(stage);
    }

    public AbstractList<AgentInfo> getAgentInfo(){
        return agentsInfo;
    }

    public void setWallInfo(float Length, float Height, float Angle, Vector3 Position, Vector3 Start, Vector3 End){
        WallInfo wI = new WallInfo();
        wI.length = Length;
        wI.height = Height;
        wI.rotAngle = Angle;
        wI.position = Position;
        wI.start = Start;
        wI.end = End;
        wallInfo.add(wI);
    }

    public ArrayList<ModelInstance> getInstancesSpheres(){
        return instancesSpheres;
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

    public void setAgentInfo(Vector3 position, boolean isCCTV) {
        AgentInfo aI = new AgentInfo();
        aI.position = position;
        aI.isCCTV = isCCTV;
        agentsInfo.add(aI);
    }

    public void setEvaderInfo(Vector3 position) {
        EvaderInfo eI = new EvaderInfo();
        eI.position = position;
        evaderInfo.add(eI);
    }
}

