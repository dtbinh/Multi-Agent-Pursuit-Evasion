package com.dke.pursuitevasion.Simulator;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.ashley.core.Engine;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.dke.pursuitevasion.Entities.EntityFactory;
import com.dke.pursuitevasion.Entities.Systems.GraphicsSystem;
import com.dke.pursuitevasion.Entities.Systems.SimulationSystem;
import com.dke.pursuitevasion.PolyMap;
import com.dke.pursuitevasion.PursuitEvasion;
import com.dke.pursuitevasion.TrackingCameraController;
import com.google.gson.Gson;

/**
 * Created by Nicola Gheza on 08/03/2017.
 */
public class SimulatorScreen implements Screen {

    private PursuitEvasion game;
    private PolyMap map;
    private PerspectiveCamera cam;
    private TrackingCameraController trackingCameraController;
    private InputMultiplexer inputMultiplexer;

    private Engine engine; // move to controller
    private EntityFactory entityFactory; // move to controller

    public SimulatorScreen(PursuitEvasion game, FileHandle mapFile) {

        /* Load the course from a file */
        this.game = game;
        Gson gson = new Gson();
        map = gson.fromJson(mapFile.readString(), PolyMap.class);

        /* Set up the camera */
        cam = new PerspectiveCamera(60f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cam.position.set(0f,90f,0f);
        cam.lookAt(0,0,0);
        cam.near = 1f;
        cam.far = 300f;
        cam.update();

        trackingCameraController = new TrackingCameraController(cam);

        /* Set up the environment */
        Environment env = new Environment();
        env.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.6f, 0.6f, 0.7f, 1f));
        env.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, 0, -0.8f, 0));

        inputMultiplexer = new InputMultiplexer();
        inputMultiplexer.addProcessor(trackingCameraController);

        engine = new Engine();
        engine.addSystem(new GraphicsSystem(cam, env));
        engine.addSystem(new SimulationSystem());

        entityFactory = new EntityFactory();

        engine.addEntity(entityFactory.createTerrain(map.getPolygonMesh()));
        engine.addEntity(entityFactory.testAgent());

        for (int i=0; i<map.getwI().length; i++) {
            engine.addEntity(entityFactory.createWall(map.getwI()[i]));
        }

    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(inputMultiplexer);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

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
