package com.dke.pursuitevasion.Editor;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.dke.pursuitevasion.PursuitEvasion;
import com.dke.pursuitevasion.TrackingCameraController;

/**
 * Created by jeeza on 23-2-17.
 */
public class MapEditorScreen implements Screen, InputProcessor {

    private PursuitEvasion pursuitEvasion;

    /* Graphics instances  */
    private Stage stage;
    private Skin skin;
    private Environment environ;
    private ModelBatch modelBatch;

    /* Controller & Others */
    private MapEditorController controller;
    private InputMultiplexer inputMultiplexer;
    private boolean leftPressed;
    private PerspectiveCamera camera;
    private TrackingCameraController trackingCameraController;

    public MapEditorScreen(PursuitEvasion pursuitEvasion) {
        this.pursuitEvasion = pursuitEvasion;
        stage = new Stage();
        controller = new MapEditorController();
        createUI();

        // Setting default camera position
        initCamera();

        //Accept input on stage and screen
        inputMultiplexer = new InputMultiplexer();
        inputMultiplexer.addProcessor(stage);
        inputMultiplexer.addProcessor(trackingCameraController);
        inputMultiplexer.addProcessor(this);

        environ = new Environment();
        environ.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environ.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

        modelBatch = new ModelBatch();
    }

    private void initCamera() {
        //Setting default camera position
        camera = new PerspectiveCamera(35, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(0f, 10f, 0f);
        camera.lookAt(0, 0, 0);
        camera.near = 1f;
        camera.far = 300f;
        trackingCameraController = new TrackingCameraController(camera);
    }

    /**
     * create GUI windows
     */
    private void createUI() {
        skin = new Skin(Gdx.files.internal("uiskin.json"));
        Window windowDesign = new Window("Design", skin);

        TextButton addOuterVertexButton = new TextButton("Add Outer Vertex", skin);

        addOuterVertexButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (controller.getMode() == Mode.DO_NOTHING)
                    controller.setMode(Mode.POINT_EDITOR);
            }
        });

        // Add buttons
        windowDesign.add(addOuterVertexButton);

        // Screen and window variables
        int windowHeight = 100;
        int screenHeight = Gdx.graphics.getHeight();
        int gutter = 10;
        int offset = 15;

        // Dimensions
        windowDesign.setSize(650, windowHeight);
        windowDesign.setPosition(offset, screenHeight - windowHeight - offset);

        // Add windows
        stage.addActor(windowDesign);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(inputMultiplexer);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        Gdx.gl20.glEnable(GL20.GL_DEPTH_TEST);
        camera.update();
        trackingCameraController.update(delta);
        modelBatch.begin(camera);
        modelBatch.render(controller.getPolygonModel(), environ);
        for (ModelInstance instance : controller.getInstances()) {
            modelBatch.render(instance, environ);
        }
        modelBatch.end();
        stage.act(delta);
        stage.draw();
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
        stage.dispose();
        skin.dispose();
        modelBatch.dispose();
        controller.dispose();
    }

    @Override
    public boolean keyDown(int keycode) {
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        if (keycode == Input.Keys.ENTER) {
            controller.remakePolygonMesh();
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
        if (Gdx.input.isButtonPressed(Input.Buttons.LEFT))
            leftPressed = true;
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (leftPressed) {
            switch (controller.getMode()) {
                case POINT_EDITOR:
                    controller.addOuterVertex(screenX, screenY, camera);
                    break;
            }
        }
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
