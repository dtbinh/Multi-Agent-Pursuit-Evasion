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
    private Engine engine;

    public SimulatorScreen(PursuitEvasion game, FileHandle mapFile) {

        /* Load the course from a file */
        this.game = game;
        Gson gson = new Gson();
        map = gson.fromJson(mapFile.readString(), PolyMap.class);

        /* Set up the camera */
        cam = new PerspectiveCamera(60, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cam.position.set(-10f, 10, 10f);
        cam.lookAt(0,0,0);
        cam.near = 0.1f;
        cam.far = 300f;
        cam.update();

        trackingCameraController = new TrackingCameraController(cam);

        engine = new Engine();


        /* Set up the environment */
        Environment env = new Environment();
        env.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.6f, 0.6f, 0.7f, 1f));
        env.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, 0, -0.8f, 0));

        inputMultiplexer = new InputMultiplexer();
        // inputMultiplexer.addProcessor(this);
        inputMultiplexer.addProcessor(trackingCameraController);

    }

    @Override
    public void show() {

    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        trackingCameraController.update(delta);

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