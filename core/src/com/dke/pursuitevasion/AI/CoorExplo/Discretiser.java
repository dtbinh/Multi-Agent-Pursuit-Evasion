package com.dke.pursuitevasion.AI.CoorExplo;

import com.badlogic.ashley.core.Engine;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.dke.pursuitevasion.AI.AStarPathFinder;
import com.dke.pursuitevasion.AI.CustomPoint;
import com.dke.pursuitevasion.AI.NodeState;
import com.dke.pursuitevasion.Entities.EntityFactory;
import com.dke.pursuitevasion.Entities.Systems.GraphicsSystem;
import com.dke.pursuitevasion.Entities.Systems.SimulationSystem;
import com.dke.pursuitevasion.PolyMap;
import com.dke.pursuitevasion.TrackingCameraController;
import com.sun.javafx.geom.Line2D;

import java.util.ArrayList;

/**
 * Created by Envy on 6/19/2017.
 */
public class Discretiser{
    Camera cam;
    TrackingCameraController trackingCameraController;
    Environment environment;
    public static float gapSize;
    public int width;
    static int height;
    public float gap;
    public int mWidth, tCount;
    PolyMap map;
    Mesh mesh;
    public boolean[][] nodeGrid;
    ArrayList<Vector3> walls;
    ArrayList<Vector3> edges;
    Vector3 intersection3;
    float[] vertices;
    short[] indices;
    public static AStarPathFinder pF;
    Engine engine;
    EntityFactory entityFactory;

    public Discretiser(PolyMap Map, float Gap, int Width){
        gapSize = Gap;
        gap = Gap;
        mWidth = Width;
        width = Width;
        height = width;
        map = Map;
        mesh = map.getPolygonMesh();
        indices = new short[mesh.getMaxIndices()];
        mesh.getIndices(indices);
        vertices = new float[mesh.getMaxVertices()];
        mesh.getVertices(vertices);

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


        pF = new AStarPathFinder(width, height, gapSize);
        walls = getWallData(map);
        edges = getEdgeData(map);
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
                tCount++;
                break;
            }
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

    public boolean lineIntersectWall(Vector3 lineStart, Vector3 lineEnd){
        for (int i = 0; i < walls.size() / 2; i++) {
            float x1 = walls.get(i * 2).x;
            float z1 = walls.get(i * 2).z;
            float x2 = walls.get(i * 2 + 1).x;
            float z2 = walls.get(i * 2 + 1).z;

            Line2D line = new Line2D(lineStart.x, lineStart.z, lineEnd.x, lineEnd.z);
            Line2D wall = new Line2D(x1, z1, x2, z2);
            if(wall.intersectsLine(line)){
                return true;
            }
        }
        return false;
    }

    public double calcOpenness(int nodeX, int nodeY){
        float totalDistance = Float.MAX_VALUE;
        if(nodeGrid[nodeX][nodeY] == true) {
            float vectorX = toWorldCoorX(nodeX);
            float vectorY = toWorldCoorY(nodeY);
            totalDistance = (float) getDistToWall(vectorX, vectorY);

            for (int i = 0; i < walls.size() / 2; i++) {
                float x1 = walls.get(i * 2).x;
                float z1 = walls.get(i * 2).z;
                float x2 = walls.get(i * 2 + 1).x;
                float z2 = walls.get(i * 2 + 1).z;

                float vecX = toWorldCoorX(nodeX);
                float vecY = toWorldCoorY(nodeY);

                float distance = Intersector.distanceSegmentPoint(x1, z1, x2, z2, vecX, vecY);
                if((distance)<totalDistance){
                    totalDistance = distance;
                }
            }
            //ret distance to closest wall if node is valid
            return totalDistance;
        }else if (checkInMeshOpenness(nodeX, nodeY) && !nodeGrid[nodeX][nodeY]){
            //ret 0 if wall in is node
            return 0;
        }else {
            //ret -1 if node is outside map
            return -1;
        }
    }

    public boolean checkInMeshOpenness(int nodeX, int nodeY){
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
                return true;
            }
        }
        return false;
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
                    tCount++;
                }
                break;
            }
        }
    }

    public double getDistToWall(float pointX, float pointY){
        double totalDistance = Float.MAX_VALUE;
        for (int i = 0; i < edges.size() / 2; i++) {
            float x1 = edges.get(i * 2).x;
            float z1 = edges.get(i * 2).z;
            float x2 = edges.get(i * 2 + 1).x;
            float z2 = edges.get(i * 2 + 1).z;

            float distance = Intersector.distanceSegmentPoint(x1, z1, x2, z2, pointX, pointY);
            if((distance)<totalDistance){
                totalDistance = distance;
            }
        }
        return totalDistance;
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

    public ArrayList<Vector3> getWallData(PolyMap c){
        ArrayList<Vector3> data = new ArrayList<Vector3>();
        for(int i=0;i<c.getwI().length;i++){
            data.add(c.getwI()[i].start);
            data.add(c.getwI()[i].end);
        }
        return data;
    }

    public ArrayList<Vector3> getEdgeData(PolyMap c){
        ArrayList<Vector3> data = new ArrayList<Vector3>();
        for(int i=0;i<c.geteV().length;i++){
            data.add(c.geteV()[i].Vector1);
            data.add(c.geteV()[i].Vector2);
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
                        return new CustomPoint(j, k);
                    }
                }
            }
        }
        return null;
    }
}
