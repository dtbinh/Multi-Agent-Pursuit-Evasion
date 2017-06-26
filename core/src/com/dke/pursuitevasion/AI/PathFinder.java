package com.dke.pursuitevasion.AI;

import com.badlogic.ashley.core.Engine;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.dke.pursuitevasion.CellDecompose.Graph.CXPoint;
import com.dke.pursuitevasion.Entities.EntityFactory;
import com.dke.pursuitevasion.Entities.Systems.GraphicsSystem;
import com.dke.pursuitevasion.PolyMap;
import com.dke.pursuitevasion.TrackingCameraController;
import com.sun.javafx.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Envy on 4/20/2017.
 */
public class PathFinder implements Screen {
    Camera cam;
    TrackingCameraController trackingCameraController;
    InputMultiplexer inputMux;
    Environment environment;
    public static float gapSize;
    public static int width;
    static int height;
    PolyMap map;
    List<Node> path;
    Mesh mesh;
    static boolean[][] nodeGrid;
    ArrayList<Vector3> walls;
    Vector3 intersection3;
    float[] vertices;
    short[] indices;
    public static AStarPathFinder pF;
    Engine engine;
    EntityFactory entityFactory;
    ArrayList<CustomPoint> CP;

    public void create(){
        //gapSize = 0.1f;
        //width = 100;
        height = width;

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

        entityFactory = new EntityFactory();

        engine.addEntity(entityFactory.createTerrain(map.getPolygonMesh(), map.geteV()));
        for (int i=0; i<map.getwI().length; i++) {
            engine.addEntity(entityFactory.createWall(map.getwI()[i]));
        }
        for (int i=0; i<map.geteV().length; i++) {
            engine.addEntity(entityFactory.createBoundary(map.geteV()[i]));
        }
    }

    public static Vector3 positionFromIndex(int x, int z, AStarPathFinder ASPathFinder){
        int index = height*x+z;
        float worldX = ASPathFinder.allNodes.get(ASPathFinder.CC.get(index)).worldX;
        float worldZ = ASPathFinder.allNodes.get(ASPathFinder.CC.get(index)).worldZ;
        return new Vector3(worldX, 0, worldZ);
    }

    public PathFinder(PolyMap Map, int width, float gapSize){
        this.width = width;
        this.gapSize = gapSize;
        map = Map;
        mesh = map.getPolygonMesh();
        indices = new short[mesh.getMaxIndices()];
        mesh.getIndices(indices);
        vertices = new float[mesh.getMaxVertices()];
        mesh.getVertices(vertices);
        create();
        calcPath();
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

    public void updateNodeGrid(){
        for(int j=0;j<width;j++){
            for(int k=0;k<height;k++){
                checkInMesh(j, k);
                checkInMeshEdges(j, k);
            }
        }
    }

    public List<Node> findPath(Vector3 currentPos, Vector3 targetPos, CustomPoint overrideEnd){
        boolean apxStart = false;
        boolean apxEnd = false;
        CP = pF.CC;
        CustomPoint start = getNodeFromWorldCoor(currentPos.x, currentPos.z);
        CustomPoint end = getNodeFromWorldCoor(targetPos.x, targetPos.z);
        if(overrideEnd!=null){
            end = overrideEnd;
        }
        if(start == null){
            start = approximatePosition(currentPos.x, currentPos.z);
            apxStart = true;
        }
        if(end == null){
            end = approximatePosition(targetPos.x, targetPos.z);
            apxEnd = true;
        }
        int sIndex = 1;;
        int eIndex = 1;

        try {
            for (int i = 0; i < CP.size(); i++) {
                if (CP.get(i) != null && CP.get(i).x == start.x && CP.get(i).y == start.y) {
                    sIndex = i;
                }
            }
            for (int i = 0; i < CP.size(); i++) {
                if (CP.get(i) != null && CP.get(i).x == end.x && CP.get(i).y == end.y) {
                    eIndex = i;
                }
            }
        }catch (Exception e){
        }
        path = pF.findPath(sIndex, eIndex);
        if(apxStart){
            Node newStart = new Node(0,0,0,0,0,0);
            newStart.worldX = currentPos.x;
            newStart.worldZ = currentPos.z;
            path.add(0,newStart);
        }
        //add on accurate end if approximation used
        if(apxEnd){
            Node newEnd = new Node (0,0,0,0,0,0);
            newEnd.worldX = targetPos.x;
            newEnd.worldZ = targetPos.z;
            path.add(newEnd);
        }
        return path;
    }
    public static CustomPoint approximatePosition(float X, float Y){
        int xIndex = 1;
        int yIndex = 1;
        int newXIndex = 0;
        int newYIndex = 0;
        outerloop:
        for (int j = 0; j < width; j++) {
            for (int k = 0; k < height; k++) {
                float offset = gapSize / 2;
                float x = toWorldCoorX(j) + offset;
                float y = toWorldCoorY(k) + offset;
                float corner1 = x + gapSize / 2+0.01f;
                float corner2 = x - gapSize / 2-0.01f;
                float corner3 = y + gapSize / 2+0.01f;
                float corner4 = y - gapSize / 2-0.01f;

                if(X < corner1 && X > corner2 && Y < corner3 && Y > corner4){
                    xIndex = j;
                    yIndex = k;
                    break outerloop;
                }
            }
        }
        if(xIndex == 0 && yIndex == 0){
            System.out.println("Approximation failed");
        }
        float distance = Float.MAX_VALUE;
        for (int i=-1;i<2;i++){
            for(int j=-1;j<2;j++){
                if((j!=0 || i!=0) && nodeGrid[xIndex+i][yIndex+j]){
                    Vector3 adjacentPos = positionFromIndex(xIndex+i, yIndex+j, pF);
                    Vector3 position = new Vector3(X, 0, Y);
                    float checkDistance = position.dst(adjacentPos);
                    if(distance>checkDistance){
                        distance = checkDistance;
                        newXIndex = xIndex+i;
                        newYIndex = yIndex+j;
                    }
                }
            }
        }
        return new CustomPoint(newXIndex, newYIndex);
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
                    int index = height*nodeX+nodeY;
                    pF.allNodes.get(pF.CC.get(index)).worldX = intersection3.x;
                    pF.allNodes.get(pF.CC.get(index)).worldZ = intersection3.z;
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

    public boolean checkStraightLinePath(CXPoint start, CXPoint end){
        for (int i = 0; i < walls.size() / 2; i++) {
            Line2D line = new Line2D((float) start.x,(float) start.y,(float) end.x,(float) end.y);
            float x1 = walls.get(i * 2).x;
            float z1 = walls.get(i * 2).z;
            float x2 = walls.get(i * 2 + 1).x;
            float z2 = walls.get(i * 2 + 1).z;

            Line2D wall = new Line2D(x1, z1, x2, z2);

            if(wall.intersectsLine(line)){
                return false;
            }
        }
            return true;
    }

    public static float toWorldCoorX(float fX){
        fX*=gapSize;
        float adjW = (width*gapSize)/2;
        return fX-adjW;
    }

    public static float toWorldCoorY(float fY){
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

    public static CustomPoint getNodeFromWorldCoor(float X, float Y){
        for(int j=0;j<width;j++){
            for(int k=0;k<height;k++){
                float offset = gapSize/2;
                float x = toWorldCoorX(j)+offset;
                float y = toWorldCoorY(k)+offset;
                float corner1 = x+gapSize/2+0.01f;
                float corner2 = x-gapSize/2-0.01f;
                float corner3 = y+gapSize/2+0.01f;
                float corner4 = y-gapSize/2-0.01f;

                if(X <= corner1 && X >= corner2 && Y <= corner3 && Y >= corner4){
                    if(nodeGrid[j][k]) {
                        return new CustomPoint(j, k);
                    }
                }
            }
        }
        return null;
    }

    public boolean[][] getNodeGrid(){
        return nodeGrid;
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

