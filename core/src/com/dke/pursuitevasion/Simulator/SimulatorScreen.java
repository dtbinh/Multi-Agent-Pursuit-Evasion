package com.dke.pursuitevasion.Simulator;

import com.badlogic.gdx.*;
import com.badlogic.ashley.core.Engine;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.dke.pursuitevasion.*;
import com.dke.pursuitevasion.CellDecompose.Graph.CXGraph;
import com.dke.pursuitevasion.CellDecompose.Graph.CXGraphNode;
import com.dke.pursuitevasion.CellDecompose.Graph.CXPoint;
import com.dke.pursuitevasion.CellDecompose.Graph.CellDecompositionAlgorithm;
import com.dke.pursuitevasion.Entities.EntityFactory;
import com.dke.pursuitevasion.Entities.Systems.GraphicsSystem;
import com.dke.pursuitevasion.Entities.Systems.VisionSystem;
import com.dke.pursuitevasion.Entities.Systems.agents.CCTvSystem;
import com.dke.pursuitevasion.Entities.Systems.agents.EvaderSystem;
import com.dke.pursuitevasion.Entities.Systems.agents.PursuerSystem;
import com.google.gson.Gson;

/**
 * Created by Nicola Gheza on 08/03/2017.
 */
public class SimulatorScreen implements Screen, InputProcessor{

    private PursuitEvasion game;
    private PolyMap map;
    private PerspectiveCamera cam;
    private TrackingCameraController trackingCameraController;
    private InputMultiplexer inputMultiplexer;

    private Engine engine; // move to controller
    private EntityFactory entityFactory; // move to controller

    public SimulatorScreen(PursuitEvasion game, FileHandle mapFile, PolyMap Map) {

        /* Load the course from a file */
        this.game = game;
        if(Map == null) {
            Gson gson = new Gson();
            map = gson.fromJson(mapFile.readString(), PolyMap.class);
        }else{
            map = Map;
        }

        /* Set up the camera */
        cam = new PerspectiveCamera(60f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cam.position.set(0f,90f,0f);
        cam.lookAt(0,0,0);
        cam.near = 1f;
        cam.far = 300f;
        cam.update();

        trackingCameraController = new TrackingCameraController(cam);
        trackingCameraController.setCameraDistance(5f);

        /* Set up the environment */
        Environment env = new Environment();
        env.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.6f, 0.6f, 0.7f, 1f));
        env.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, 0, -0.8f, 0));

        inputMultiplexer = new InputMultiplexer();
        inputMultiplexer.addProcessor(trackingCameraController);
        inputMultiplexer.addProcessor(this);

        // Convert map into graph
        CXGraph polygon = new CXGraph();
        EdgeVectors[] edge = map.geteV();
        WallInfo[] wI = map.getwI();
        for (int i = 0 ; i < edge.length; i++){
            // 1.1 Transfer the location ( x = x + 5 , y = (y) * (-1) + 5)
            EdgeVectors vectors = edge[i];
            double x1 = vectors.Vector1.x + 5;
            double y1 = vectors.Vector1.z * (-1) + 5;
            double x2 = vectors.Vector2.x + 5;
            double y2 = vectors.Vector2.z * (-1) + 5;
            // 1.2 add Node
            CXGraphNode node1 = new CXGraphNode(new CXPoint(x1,y1));
            CXGraphNode node2 = new CXGraphNode(new CXPoint(x2,y2));
            node1 = polygon.add_vertex(node1);
            node2 = polygon.add_vertex(node2);
            polygon.add_edge(node1,node2,1);
        }

        // 2. Obstacles --> wallInfo
        CXGraph obstacle = new CXGraph();
        // 2.1   add the obstacle nodes
        for (int i = 0; i < wI.length ; i++) {
            WallInfo info = wI[i];
            double x1 = info.start.x + 5;

            double y1 = info.start.z * (-1) + 5;
            double x2 = info.end.x + 5;
            double y2 = info.end.z * (-1) + 5;
            CXGraphNode node1 = new CXGraphNode(new CXPoint(x1,y1));
            CXGraphNode node2 = new CXGraphNode(new CXPoint(x2,y2));
            node1 = obstacle.add_vertex(node1);
            node2 = obstacle.add_vertex(node2);
            obstacle.add_vertex(node1);
            obstacle.add_vertex(node2);
            obstacle.add_edge(node1,node2,1);
        }

        CellDecompositionAlgorithm cellDecomposeAlgorithm = new CellDecompositionAlgorithm(polygon,obstacle);
        CXGraph graph =  cellDecomposeAlgorithm.decomposeGraph();

        engine = new Engine();

        entityFactory = new EntityFactory();

        engine.addEntity(entityFactory.createTerrain(map.getPolygonMesh(), map.geteV()));
        System.out.println("EdgeVectors: " + map.geteV().length);

        for (int i=0; i<map.geteV().length; i++) {
            engine.addEntity(entityFactory.createBoundary(map.geteV()[i]));
        }

        for (int i=0; i<map.getwI().length; i++) {
            engine.addEntity(entityFactory.createWall(map.getwI()[i]));
        }

        engine.addSystem(new GraphicsSystem(cam, env));
        //engine.addSystem(new SimulationSystem());
        VisionSystem visionSystem = new VisionSystem();
        engine.addSystem(visionSystem);
        engine.addSystem(new EvaderSystem(map));
        engine.addSystem(new CCTvSystem(visionSystem, map));
        engine.addSystem(new PursuerSystem(visionSystem, graph, map));

        for (int i=0; i<map.getaI().length; i++) {
            if(map.getaI()[i].isCCTV){
                engine.addEntity(entityFactory.createCCTv(map.getaI()[i].position));
            }else{
                engine.addEntity(entityFactory.createPursuer(map.getaI()[i].position, Color.BLUE));
            }
        }

        for (int i=0; i<map.geteI().length; i++) {
            engine.addEntity(entityFactory.createEvader(map.geteI()[i].position, Color.RED));
        }

    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(inputMultiplexer);
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
        Gdx.gl.glViewport(0, 0, width, height);
        cam.viewportHeight = height;
        cam.viewportWidth = width;
        cam.update();
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

    @Override
    public boolean keyDown(int keycode) {
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        if (keycode == Input.Keys.ESCAPE) {
            game.showPauseMenu();
            return true;
        }
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
        return false;
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
}
