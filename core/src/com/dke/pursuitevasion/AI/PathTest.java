package com.dke.pursuitevasion.AI;

import com.badlogic.ashley.core.Engine;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.math.collision.Ray;
import com.dke.pursuitevasion.Entities.EntityFactory;
import com.dke.pursuitevasion.Entities.Systems.GraphicsSystem;
import com.dke.pursuitevasion.Entities.Systems.SimulationSystem;
import com.dke.pursuitevasion.PolyMap;
import com.dke.pursuitevasion.TrackingCameraController;
import com.google.gson.Gson;
import com.sun.javafx.geom.Line2D;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Envy on 4/20/2017.
 */
public class PathTest implements Screen, InputProcessor {
    Camera cam;
    TrackingCameraController trackingCameraController;
    InputMultiplexer inputMux;
    Environment environment;
    ModelBuilder modelBuilder;
    ModelBatch modelBatch;
    ModelInstance grid;
    float gapSize;
    int width, height, sIndex, eIndex;
    PolyMap map;
    List<Node> path;
    Mesh mesh;
    boolean[][] nodeGrid;
    boolean startSet, endSet;
    ArrayList<Vector3> walls;
    Vector3 intersection3;
    Model boxx, bbox, wbox, ybox;
    float[] vertices;
    short[] indices;
    ArrayList<ModelInstance> mi = new ArrayList<ModelInstance>();
    ArrayList<ModelInstance> pathAS = new ArrayList<ModelInstance>();
    AStarPathFinder pF;
    Engine engine;
    EntityFactory entityFactory;

    Texture texture;
    ShaderProgram shader;
    String vertexShader = "attribute vec4 a_position;    \n" +
            "attribute vec4 a_color;\n" +
            "attribute vec2 a_texCoord0;\n" +
            "uniform mat4 u_worldView;\n" +
            "varying vec4 v_color;" +
            "varying vec2 v_texCoords;" +
            "void main()                  \n" +
            "{                            \n" +
            "   v_color = vec4(1, 1, 1, 1); \n" +
            "   v_texCoords = a_texCoord0; \n" +
            "   gl_Position =  u_worldView * a_position;  \n"      +
            "}                            \n" ;
    String fragmentShader = "#ifdef GL_ES\n" +
            "precision mediump float;\n" +
            "#endif\n" +
            "varying vec4 v_color;\n" +
            "varying vec2 v_texCoords;\n" +
            "uniform sampler2D u_texture;\n" +
            "void main()                                  \n" +
            "{                                            \n" +
            "  gl_FragColor = v_color * texture2D(u_texture, v_texCoords);\n" +
            "}";

    public void create(){

        cam = new PerspectiveCamera(60f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cam.position.set(0f,90f,0f);
        cam.lookAt(0,0,0);
        cam.near = 1f;
        cam.far = 300f;
        cam.update();

        trackingCameraController = new TrackingCameraController(cam);
        trackingCameraController.setCameraDistance(5f);

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

        engine = new Engine();
        engine.addSystem(new GraphicsSystem(cam, environment));
        engine.addSystem(new SimulationSystem());

        entityFactory = new EntityFactory();

        engine.addEntity(entityFactory.createTerrain(map.getPolygonMesh(), map.geteV()));
        for (int i=0; i<map.getwI().length; i++) {
            engine.addEntity(entityFactory.createWall(map.getwI()[i]));
        }
        for (int i=0; i<map.geteV().length; i++) {
            engine.addEntity(entityFactory.createBoundary(map.geteV()[i]));
        }


        inputMux = new InputMultiplexer();
        inputMux.addProcessor(this);
        inputMux.addProcessor(trackingCameraController);

        modelBatch = new ModelBatch();
        modelBuilder = new ModelBuilder();

        //Make a 32x32 grid composed of 0.35x0.35 squares
        gapSize = 0.2f;
        width = 60;
        height = width;

        Model gridd = modelBuilder.createLineGrid(width, height, gapSize, gapSize, new Material(ColorAttribute.createDiffuse(Color.BLACK)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        boxx = modelBuilder.createBox(0.4f, 0.1f, 0.4f,new Material(ColorAttribute.createDiffuse(Color.BROWN)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        wbox = modelBuilder.createBox(0.1f, 0.01f, 0.1f,new Material(ColorAttribute.createDiffuse(Color.ORANGE)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        bbox = modelBuilder.createBox(0.08f, 0.01f, 0.08f,new Material(ColorAttribute.createDiffuse(Color.GREEN)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        ybox = modelBuilder.createBox(0.1f, 0.01f, 0.1f,new Material(ColorAttribute.createDiffuse(Color.BLUE)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        grid= new ModelInstance(gridd, 0,0,0);
    }

    public PathTest(FileHandle courseF){
        //Load the course from a file
        Gson gson = new Gson();
        map = gson.fromJson(courseF.readString(), PolyMap.class);
        mesh = map.getPolygonMesh();
        indices = new short[mesh.getMaxIndices()];
        mesh.getIndices(indices);
        vertices = new float[mesh.getMaxVertices()];
        mesh.getVertices(vertices);
        create();
        calcPath();
    }

    public List<Node> getPath(){
        return path;
    }

    public void calcPath(){
        pF = new AStarPathFinder(width, height, gapSize);
        walls = getWallData(map);
        NodeState nodeState = new NodeState(width, height);
        nodeGrid = nodeState.getNodeGrid();
        addRepeatVectors();
        updateNodeGrid();
        pF.setNotBlocked(nodeGrid);
        pF.init();
    }
    public void printRes(){
        for(int i=0;i<path.size();i++){
            float x = path.get(i).worldX;
            float y = path.get(i).worldY;
            float z = path.get(i).worldZ;

            ModelInstance pathNode = new ModelInstance(ybox, x,y+0.01f,z);
            pathAS.add(pathNode);
        }
    }
    public void updateNodeGrid(){
        for(int j=0;j<width;j++){
            for(int k=0;k<height;k++){
                checkInMesh(j, k);
                checkInMeshEdges(j, k);
            }
        }
    }


    public void checkInMesh(int nodeX, int nodeY){
        //takes x,y of node and checks if it intersects the mesh
        float Xray = toWorldCoorX(nodeX);
        float Yray = toWorldCoorY(nodeY);
        float offset = gapSize/2;
        Ray ray = new Ray(new Vector3((Xray+offset), 10, (Yray+offset)), new Vector3(0,-10,0));
        intersection3 = new Vector3();

        for (int i = 0; i < indices.length / 3; i++) {
            //vertices array has been reordered already so iterating through goes over all triangles
            Vector3 t1 = new Vector3(vertices[i * 3 * 5], vertices[i * 3 * 5 + 1], vertices[i * 3 * 5 + 2]);
            Vector3 t2 = new Vector3(vertices[(i * 3 + 1) * 5], vertices[(i * 3 + 1) * 5 + 1], vertices[(i * 3 + 1) * 5 + 2]);
            Vector3 t3 = new Vector3(vertices[(i * 3 + 2) * 5], vertices[(i * 3 + 2) * 5 + 1], vertices[(i * 3 + 2) * 5 + 2]);
            if (Intersector.intersectRayTriangle(ray, t1, t2, t3, intersection3)) {
                nodeGrid[nodeX][nodeY] = true;
                checkIntersectWall(nodeX, nodeY);
                if(nodeGrid[nodeX][nodeY]){
                    ModelInstance validNode = new ModelInstance(bbox, intersection3);
                    mi.add(validNode);
                }
                break;
            }
        }
    }

    public void checkInMeshEdges(int nodeX, int nodeY){
        //takes x,y of node and checks if it intersects the mesh
        float Xray = toWorldCoorX(nodeX);
        float Yray = toWorldCoorY(nodeY);
        float offset = gapSize/2;
        float tolerance = offset/1.3f;
        Ray ray1 = new Ray(new Vector3((Xray+offset)+tolerance, 10, (Yray+offset)+tolerance), new Vector3(0,-10,0));
        Ray ray2 = new Ray(new Vector3((Xray+offset)+tolerance, 10, (Yray+offset)-tolerance), new Vector3(0,-10,0));
        Ray ray3 = new Ray(new Vector3((Xray+offset)-tolerance, 10, (Yray+offset)+tolerance), new Vector3(0,-10,0));
        Ray ray4 = new Ray(new Vector3((Xray+offset)-tolerance, 10, (Yray+offset)-tolerance), new Vector3(0,-10,0));

        //Add 4 more rays maybe, top bottom left right

        intersection3 = new Vector3();
        for (int i = 0; i < indices.length / 3; i++) {
            Vector3 t1 = new Vector3(vertices[i * 3 * 5], vertices[i * 3 * 5 + 1], vertices[i * 3 * 5 + 2]);
            Vector3 t2 = new Vector3(vertices[(i * 3 + 1) * 5], vertices[(i * 3 + 1) * 5 + 1], vertices[(i * 3 + 1) * 5 + 2]);
            Vector3 t3 = new Vector3(vertices[(i * 3 + 2) * 5], vertices[(i * 3 + 2) * 5 + 1], vertices[(i * 3 + 2) * 5 + 2]);
            if (!nodeGrid[nodeX][nodeY] && (Intersector.intersectRayTriangle(ray1, t1, t2, t3, intersection3)||Intersector.intersectRayTriangle(ray2, t1, t2, t3, intersection3)||
                Intersector.intersectRayTriangle(ray3, t1, t2, t3, intersection3)||Intersector.intersectRayTriangle(ray4, t1, t2, t3, intersection3))) {
                nodeGrid[nodeX][nodeY] = true;
                checkIntersectWall(nodeX, nodeY);
                if (nodeGrid[nodeX][nodeY]) {
                    ModelInstance validNode = new ModelInstance(bbox, intersection3);
                    int index = height*nodeX+nodeY;
                    pF.allNodes.get(pF.CC.get(index)).worldX = intersection3.x;
                    pF.allNodes.get(pF.CC.get(index)).worldZ = intersection3.z;

                    mi.add(validNode);
                }
                break;
            }
        }
    }

    public void addRepeatVectors(){
        ArrayList<Float> normArray = new ArrayList<Float>();
        for(int i = 0; i<indices.length/3;i++){
            int index1 = indices[i*3];
            int index2 = indices[i*3+1];
            int index3 = indices[i*3+2];

            //Add first vec
            normArray.add(vertices[index1*5]);normArray.add(vertices[index1*5+1]);normArray.add(vertices[index1*5+2]);
            //Add second vec
            normArray.add(vertices[index2*5]);normArray.add(vertices[index2*5+1]);normArray.add(vertices[index2*5+2]);
            //Add third vec
            normArray.add(vertices[index3*5]);normArray.add(vertices[index3*5+1]);normArray.add(vertices[index3*5+2]);
        }
        int size = normArray.size()*3-normArray.size()/3;
        float[] array = new float[size];
        //copy
        for (int i=0;i<normArray.size()/3;i++){
            array[i*5] = normArray.get(i*3);
            array[i*5+1] = normArray.get(i*3+1);
            array[i*5+2] = normArray.get(i*3+2);
        }

        indices = new short[normArray.size()/3];
        for(int i=0;i<normArray.size()/3;i++){
            indices[i] = (short) i;
        }

        //update vertList
        vertices = new float[array.length];
        for(int i=0;i<array.length;i++){
            vertices[i] = array[i];
        }
    }


    public void checkIntersectWall(int nodeX, int nodeY){

        //only bother checking if the node is on the mesh
        //if its not then it can be intersecting any walls anyways
        if(nodeGrid[nodeX][nodeY] == true) {
            for (int i = 0; i < walls.size() / 2; i++) {
                float x1 = walls.get(i * 2).x;
                float z1 = walls.get(i * 2).z;
                float x2 = walls.get(i * 2 + 1).x;
                float z2 = walls.get(i * 2 + 1).z;

                float vecX = toWorldCoorX(nodeX);
                float vecY = toWorldCoorY(nodeY);

                Line2D wall = new Line2D(x1, z1, x2, z2);
                //bottom left to top left
                Line2D l1 = new Line2D(vecX, vecY+gapSize, vecX, vecY);
                //top left to top right
                Line2D l2 = new Line2D(vecX, vecY, vecX + gapSize, vecY);
                //top right to bottom right
                Line2D l3 = new Line2D(vecX + gapSize, vecY, vecX + gapSize, vecY+gapSize);
                //bottom right to bottom left
                Line2D l4 = new Line2D(vecX + gapSize, vecY+gapSize, vecX, vecY+gapSize);
                //top left to bottom right
                Line2D l5 = new Line2D(vecX, vecY, vecX + gapSize, vecY+gapSize);
                //bottom left to top right
                Line2D l6 = new Line2D(vecX, vecY+gapSize, vecX + gapSize, vecY);

                if(wall.intersectsLine(l1)){
                    nodeGrid[nodeX][nodeY] = false;
                }
                if(wall.intersectsLine(l2)){
                    nodeGrid[nodeX][nodeY] = false;
                }
                if(wall.intersectsLine(l3)){
                    nodeGrid[nodeX][nodeY] = false;
                }
                if(wall.intersectsLine(l4)){
                    nodeGrid[nodeX][nodeY] = false;
                }
                if(wall.intersectsLine(l5)){
                    nodeGrid[nodeX][nodeY] = false;
                }
                if(wall.intersectsLine(l6)){
                    nodeGrid[nodeX][nodeY] = false;
                }
            }
        }
    }

    public void displayNodes(){
        for(int i=0;i<width;i++){
            for (int j=0;j<height;j++){
                if(nodeGrid[i][j]==true){
                    float X = toWorldCoorX(i);
                    float Y  = toWorldCoorY(j);
                    float offSet = gapSize/2;
                    //adj position so its in the middle of a node not bottom left pos
                    ModelInstance a = new ModelInstance(boxx,X+offSet,0,Y-offSet);
                    //ModelInstance a = new ModelInstance(boxx,Xray,0,Yray);
                    mi.add(a);
                }
            }
        }
    }

    public float toWorldCoorX(float fX){
        fX*=gapSize;
        float adjW = (width*gapSize)/2;
        return fX-adjW;
    }

    public float toWorldCoorY(float fY){
        fY*=gapSize;
        float adjH = (width*gapSize)/2;
        return fY-adjH;
    }


    public ArrayList<Vector3> getWallData(PolyMap c){
        ArrayList<Vector3> data = new ArrayList<Vector3>();
        for(int i=0;i<c.getwI().length;i++){
            data.add(c.getwI()[i].start);
            data.add(c.getwI()[i].end);
        }
        return data;
    }

    public CustomPoint getNodeFromWorldCoor(float X, float Y){
        for(int j=0;j<width;j++){
            for(int k=0;k<height;k++){
                float offset = gapSize/2;
                float x = toWorldCoorX(j)+offset;
                float y = toWorldCoorY(k)+offset;
                float corner1 = x+gapSize/2;
                float corner2 = x-gapSize/2;
                float corner3 = y+gapSize/2;
                float corner4 = y-gapSize/2;

                if(X < corner1 && X > corner2 && Y < corner3 && Y > corner4){
                    if(nodeGrid[j][k]) {
                        ModelInstance node = new ModelInstance(wbox, new Vector3(x, 0, y));
                        return new CustomPoint(j, k);
                    }
                }
            }
        }
        return null;
    }


    @Override
    public boolean keyDown(int keycode) {
        return false;
    }
    @Override
    public boolean keyUp(int keycode) {
        return false;
    }
    @Override
    public boolean keyTyped(char character) {
        return false;
    }
    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return false;
    }
    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        Ray pickRay = cam.getPickRay(screenX, screenY);
        Vector3 intersection = new Vector3();
        Intersector.intersectRayPlane(pickRay, new Plane(new Vector3(0f, 1f, 0f), 0f), intersection);
        ArrayList<CustomPoint> CP = pF.getCC();
        CustomPoint customPoint = getNodeFromWorldCoor(intersection.x, intersection.z);
        if(customPoint!=null) {
            System.out.println("x: " + customPoint.x + "  y: " + customPoint.y);
        }

        if(!startSet && !endSet && customPoint!= null) {
            for(int i=0;i<CP.size();i++){
                if(CP.get(i).x == customPoint.x && CP.get(i).y == customPoint.y){
                    sIndex = i;
                    //System.out.println(sIndex+ "   start index");
                    startSet = true;
                }
            }
        }
        else if(startSet && !endSet&& customPoint!= null) {
            for(int i=0;i<CP.size();i++){
                if(CP.get(i).x == customPoint.x && CP.get(i).y == customPoint.y){
                    eIndex = i;
                    //System.out.println(eIndex+ "   end index");
                    endSet = true;
                }
            }
            path = pF.findPath(sIndex, eIndex);
            printRes();
        }
        else if(startSet && endSet&& customPoint!= null) {
            endSet = false;
            pathAS.clear();
            for(int i=0;i<CP.size();i++){
                if(CP.get(i).x == customPoint.x && CP.get(i).y == customPoint.y){
                    sIndex = i;
                }
            }
        }

        return true;
    }
    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }
    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }
    @Override
    public boolean scrolled(int amount) {
        return false;
    }
    @Override
    public void show() {
        Gdx.input.setInputProcessor(inputMux);
    }
    @Override
    public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        Gdx.gl20.glEnable(GL20.GL_DEPTH_TEST);
        trackingCameraController.update(delta);
        engine.update(delta);
        modelBatch.begin(cam);
        //modelBatch.render(grid, environment);
        if(mi.size()>0){
            for(int i =0;i<mi.size();i++){
                modelBatch.render(mi.get(i), environment);
            }
        }
        if(pathAS.size()>0){
            for(int i =0;i<pathAS.size();i++){
                modelBatch.render(pathAS.get(i), environment);
            }
        }
        modelBatch.end();
    }
    @Override
    public void resize(int width, int height) {
    }
    @Override
    public void pause() {
    }
    @Override
    public void resume() {
    }
    @Override
    public void hide() {
    }
    @Override
    public void dispose() {
    }
}

