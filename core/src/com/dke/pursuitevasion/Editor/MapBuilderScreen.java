package com.dke.pursuitevasion.Editor;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.EarClippingTriangulator;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.ShortArray;
import com.dke.pursuitevasion.PursuitEvasion;
import com.dke.pursuitevasion.TrackingCameraController;

import java.awt.*;
import java.util.ArrayList;

/**
 * Created by jeeza on 10-2-17.
 */
public class MapBuilderScreen implements Screen, InputProcessor {
    enum Mode {
        DO_NOTHING,
        POINT_EDITOR
    }
    private final PursuitEvasion game;
    Mode mode = Mode.DO_NOTHING;
    PerspectiveCamera camera;
    InputMultiplexer inputMultiplexer;
    Stage stage;
    Skin skin;
    Environment environ;
    ModelBatch modelBatch;
    ModelBuilder modelBuilder;
    ArrayList<ModelInstance> instances;
    ArrayList<Vector3> instanceVectors;
    TrackingCameraController trackingCameraController;
    Boolean leftPressed;
    Mesh mesh;
    Model model;
    ModelInstance meshMap;
    float[] vertList = new float[0];
    int listSize;
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

    public MapBuilderScreen(PursuitEvasion game) {
        this.game = game;
        stage = new Stage();
        //shader = new ShaderProgram(vertexShader, fragmentShader);
        createUI();
        //Setting default camera position
        camera = new PerspectiveCamera(35, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(0f, 10f, 0f);
        camera.lookAt(0, 0, 0);
        camera.near = 1f;
        camera.far = 300f;
        trackingCameraController = new TrackingCameraController(camera);

        //Accept input on stage and screen
        inputMultiplexer = new InputMultiplexer();
        inputMultiplexer.addProcessor(stage);
        inputMultiplexer.addProcessor(trackingCameraController);
        inputMultiplexer.addProcessor(this);

        environ = new Environment();
        environ.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environ.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

        modelBatch = new ModelBatch();
        modelBuilder = new ModelBuilder();

        mesh = new Mesh(true, 4, 6, new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"));
        //mesh.setVertices(new float[]{-1f, -1f, 0f,0f, -1f, 0f,0f, 0f, 0f,-1f, 0f, 0f,});
        mesh.setVertices(new float[]{0f, 0f, 0f,0f, 0f, 0,0f, 0f, 0f,0f, 0f, 0f,});
        mesh.setIndices(new short[] {0, 1, 2, 2, 3, 0,});
        Material material = new Material(ColorAttribute.createDiffuse(Color.WHITE));
        modelBuilder.begin();
        modelBuilder.part("Temp", mesh, GL20.GL_TRIANGLES, material);
        model = modelBuilder.end();
        meshMap = new ModelInstance(model, 0,0,0);

        instances = new ArrayList<ModelInstance>();
        instanceVectors = new ArrayList<Vector3>();
    }

    private void createUI() {
        skin = new Skin(Gdx.files.internal("uiskin.json"));
        Window windowDesign = new Window("Design", skin);

        TextButton addOutVertexButton = new TextButton("Add Outer Vertex", skin);

        addOutVertexButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (mode == Mode.DO_NOTHING)
                    mode = Mode.POINT_EDITOR;
            }
        });

        // Add buttons
        windowDesign.add(addOutVertexButton);

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

    private boolean nearNeighbor(Vector3 vec){
        for(int i=0;i<instanceVectors.size();i++){
            Vector3 v = instanceVectors.get(i);
            if (vec.x<v.x+0.5 && vec.x>v.x-0.5 && vec.z<v.z+0.5 && vec.z>v.z-0.5){
                return true;
            }
        }

        return false;
    }

    private void remakeMesh(){
        //Remove y float from vertList
        int newLength = (2*vertList.length)/3;
        int forLoopNum = (vertList.length/3);
        float[] newVertList  = new float[newLength];
        ArrayList<Float> temp = new ArrayList<Float>();
        //System.out.println(vertList.length);
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
            System.out.print(indices[i]+ " ");
            System.out.println(meshIndices.get(i));
        }

        mesh = new Mesh(true, vertList.length, meshIndices.size,
                new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"));
        mesh.setVertices(vertList);
        mesh.setIndices(indices);
        Material material = new Material(ColorAttribute.createDiffuse(Color.GOLD));
        modelBuilder.begin();
        modelBuilder.part("Course", mesh, GL20.GL_TRIANGLES, material);
        model = modelBuilder.end();
        meshMap = new ModelInstance(model, 0,0,0);
    }

    private void resizeArray(float[] oldVertList) {
        // create a new array of size+3
        int newSize = oldVertList.length + 3;
        float[] newArray = new float[newSize];
        System.arraycopy(oldVertList, 0, newArray, 0, oldVertList.length);
        vertList = newArray;
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
        stage.act(delta);
        stage.draw();

        //Some texture shit
        /*shader.begin();
        shader.setUniformMatrix("u_worldView", camera.combined);
        shader.setUniformi("u_texture", 0);
        shader.end();*/

        modelBatch.begin(camera);
        modelBatch.render(meshMap, environ);
        for (int i=0;i<instances.size();i++)
        {
            modelBatch.render(instances.get(i), environ);
        }
        modelBatch.end();
    }

    @Override
    public void resize(int width, int height) {
        Gdx.gl.glViewport(0, 0, width, height);
        stage.getViewport().update(width, height);
        camera.viewportHeight = height;
        camera.viewportWidth = width;
        camera.update();
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
        if (keycode == Input.Keys.ENTER) {
            remakeMesh();
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
        if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
            leftPressed = true;
        }
            return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (leftPressed) {
            switch (mode) {
                case POINT_EDITOR:
                    //Translate user click to point on the plane
                    Ray pickRay = camera.getPickRay(screenX, screenY);
                    Vector3 intersection = new Vector3();
                    Intersector.intersectRayPlane(pickRay, new Plane(new Vector3(0f, 1f, 0f), 0f), intersection);

                    String verts = intersection.toString();
                    verts = verts.replaceAll("[()]","");
                    verts = verts.replaceAll("[,]",", ");
                    String[] Array = verts.split(",");
                    resizeArray(vertList);
                    for(int i = 0; i < 3; i++) {
                        vertList[listSize+i] = Float.parseFloat(Array[i]);
                    }
                    listSize+=3;

                    //Create a model at position
                    Model vertexPos = modelBuilder.createSphere(0.15f, 0.15f, 0.15f, 20, 20, new Material(ColorAttribute.createDiffuse(Color.LIGHT_GRAY)),
                            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);

                    if(!nearNeighbor(intersection)) {
                        ModelInstance vPosInst = new ModelInstance(vertexPos, intersection);
                        instances.add(vPosInst);
                        instanceVectors.add(intersection);
                    }
            }
        }

        leftPressed = false;
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
