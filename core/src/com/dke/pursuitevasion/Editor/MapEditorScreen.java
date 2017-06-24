package com.dke.pursuitevasion.Editor;

import com.badlogic.gdx.*;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.dke.pursuitevasion.*;
import com.dke.pursuitevasion.Menu.MenuScreen;
import com.dke.pursuitevasion.Menu.NewSimulationWindow;
import com.dke.pursuitevasion.Simulator.SimulatorScreen;
import com.dke.pursuitevasion.UI.FileChooser;
import com.google.gson.Gson;


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
    private ShaderProgram shader;

    /* Controller & Others */
    private MapEditorController controller;
    private InputMultiplexer inputMultiplexer;
    private boolean leftPressed;
    private PerspectiveCamera camera;
    private TrackingCameraController trackingCameraController;
    private Vector3 wallVec;

    private PursuitEvasion game;
    private NewSimulationWindow newSimulationWindow;
    private FileHandle file;

    boolean isBuilt;

    private ModelBuilder modelBuilder;

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

    public MapEditorScreen(PursuitEvasion pursuitEvasion) {
        this.pursuitEvasion = pursuitEvasion;
        stage = new Stage();
        controller = new MapEditorController();
        shader = new ShaderProgram(vertexShader, fragmentShader);
        this.game = pursuitEvasion;
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
        final MenuScreen simMenu = new MenuScreen(pursuitEvasion);
        Table table=new Table();
        table.setSize(800, 480);

        Label mapBuilderText = new Label("---Map builder---", skin);
        Label objectsText = new Label("---Place objects and agents---", skin);
        Label exportText = new Label("---Export for simulation---", skin);

        final TextButton addOuterVertexButton = new TextButton("Add Outer Vertex", skin);
        TextButton loadMapButton = new TextButton("Load Map", skin);
        final TextButton makePolygonButton = new TextButton("Apply Triangulation", skin);
        final TextButton exportMapButton = new TextButton("Export Map", skin);
        TextButton wallEditorButton = new TextButton("Add Walls", skin);
        TextButton addPursuerButton = new TextButton("Add Pursuer", skin);
        TextButton addEvaderButton = new TextButton("Add Evader", skin);
        TextButton addCCTVButton = new TextButton("Add CCTV", skin);
        TextButton simulatorButton = new TextButton("Simulator", skin);
        TextButton backButton = new TextButton("Back", skin);




        addOuterVertexButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (controller.getMode() == Mode.DO_NOTHING)
                    controller.setMode(Mode.POINT_EDITOR);
            }
        });

        makePolygonButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                controller.setMode(Mode.DO_NOTHING);
                if(!controller.meshRendered)
                    controller.remakePolygonMesh();
                isBuilt = true;
            }
        });

        exportMapButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                controller.saveFile(stage, skin);
            }
        });

        loadMapButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                loadFile();
                isBuilt=true;
                addOuterVertexButton.setDisabled(true);
                makePolygonButton.setDisabled(true);
                controller.setMode(Mode.DO_NOTHING);
            }
        });

        wallEditorButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if(isBuilt && (controller.getMode() == Mode.DO_NOTHING || controller.getMode() == Mode.EVADER_EDITOR || controller.getMode() == Mode.PURSUER_EDITOR || controller.getMode()==Mode.CCTV_EDITOR ))
                    controller.setMode(Mode.WALL_EDITOR);
            }
        });

       addPursuerButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (isBuilt && (controller.getMode() == Mode.DO_NOTHING || controller.getMode() == Mode.WALL_EDITOR || controller.getMode()==Mode.EVADER_EDITOR || controller.getMode()==Mode.CCTV_EDITOR))
                    controller.setMode(Mode.PURSUER_EDITOR);
            }
        });

        addEvaderButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (isBuilt && (controller.getMode() == Mode.DO_NOTHING || controller.getMode() == Mode.WALL_EDITOR || controller.getMode()==Mode.PURSUER_EDITOR || controller.getMode()==Mode.CCTV_EDITOR))
                   controller.setMode(Mode.EVADER_EDITOR);
            }
        });

        addCCTVButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (isBuilt && (controller.getMode() == Mode.DO_NOTHING || controller.getMode() == Mode.WALL_EDITOR || controller.getMode()==Mode.PURSUER_EDITOR || controller.getMode()==Mode.EVADER_EDITOR))
                    controller.setMode(Mode.CCTV_EDITOR);
            }
        });

        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
               game.setScreen(new MenuScreen(pursuitEvasion));
            }
        });

        simulatorButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {

                //NewSimulationWindow sim = new NewSimulationWindow(skin, game, simMenu);
                //stage.addActor(sim);
                if(controller.localMap!=null) {
                    game.setScreen(new SimulatorScreen(game, null, controller.localMap));
                }

            }
        });




        // Add buttons
        table.add(mapBuilderText);
        table.row();
        table.add(addOuterVertexButton).width(150);
        table.row();
        table.add(makePolygonButton).width(150);
        table.row();
        table.add(objectsText);
        table.row();
        table.add(wallEditorButton).width(150);
        table.row();
        table.add(addPursuerButton).width(150);
        table.row();
        table.add(addEvaderButton).width(150);
        table.row();
        table.add(addCCTVButton).width(150);
        table.row();
        table.add(exportText);
        table.row();
        table.add(exportMapButton).width(150);
        table.row();
        table.add(loadMapButton).width(150);
        table.row();
        table.add(simulatorButton).width(150);
        table.row();
        table.add(backButton).width(150);
        windowDesign.add(table);


        // Screen and window variables
        int windowHeight = (int) stage.getHeight();
        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();
        int gutter = 20;
        int offset = 0;

        windowDesign.setMovable(false);

        // Dimensions
        windowDesign.setSize(screenWidth/4, windowHeight);
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

        //rendering mesh
        controller.texture.bind();
        shader.begin();
        shader.setUniformMatrix("u_worldView", camera.combined);
        shader.setUniformi("u_texture", 0);
        if(controller.meshRenderable) {
            controller.getPolygonMesh().render(shader, GL20.GL_TRIANGLES);
        }
        shader.end();


        modelBatch.begin(camera);
        //modelBatch.render(controller.getPolygonModel(), environ);
        if(controller.getMode() == Mode.POINT_EDITOR){
            for (ModelInstance instance : controller.getInstancesSpheres()) {
                modelBatch.render(instance, environ);
            }
        }else{
            for (ModelInstance instance : controller.getInstances()) {
                modelBatch.render(instance, environ);
            }
        }
        if(controller.mWall!=null){
            //lets the user see the wall he is drawing
            modelBatch.render(controller.mWall);
        }
        modelBatch.end();
        stage.act(delta);
        stage.draw();
    }
    public void loadFile() {
        FileChooser files = new FileChooser("Select Course File", skin) {
            @Override
            protected void result(Object object) {
                if (object.equals("OK")) {
                    file = getFile();
                    System.out.println(file.read(56));

                    Gson gson = new Gson();
                    PolyMap map = gson.fromJson(file.readString(), PolyMap.class);
                    //Add mesh
                    controller.setPolygonMesh(map.getPolygonMesh());
                    controller.meshRenderable = true;
                    //Add walls
                    WallInfo[] wallInfo = map.getwI();
                    for (int i=0; i<map.getwI().length;i++){
                        controller.addWall(wallInfo[i].start, wallInfo[i].end);
                        controller.addWallToArray();
                    }
                    AgentInfo[] agentInfo = map.getaI();
                    for (int i=0; i<map.getaI().length;i++){
                        //controller.setAgentInfo(agentInfo[i].position,false);

                        controller.addAgent(0,0,camera,false, agentInfo[i].position);
                        System.out.print(controller.getAgentInfo().get(i).position);
                    }
                    EvaderInfo[] evaderInfo = map.geteI();
                    for (int i=0; i<map.geteI().length;i++){
                        controller.addEvader(0,0,camera,evaderInfo[i].position);
                    }

                }
            }
        };
        files.setDirectory(Gdx.files.local("maps/"));
        files.show(stage);
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
        if (keycode == Input.Keys.ESCAPE) {
            pursuitEvasion.showPauseMenu();
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
            Ray pickRay = camera.getPickRay(screenX, screenY);
            if(controller.getMode() == Mode.WALL_EDITOR) {
                if (wallVec == null) {
                    wallVec = new Vector3();
                    Intersector.intersectRayTriangles(pickRay,controller.getVertList(), controller.getmIndices(), 5, wallVec);
                }
            }
        }
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (leftPressed) {
            switch (controller.getMode()) {
                case POINT_EDITOR:
                    controller.addOuterVertex(screenX, screenY, camera);
                    leftPressed = false;
                    break;
                case WALL_EDITOR:
                    controller.addWallToArray();
                    leftPressed = false;
                    wallVec = null;
                    break;
                case PURSUER_EDITOR:
                    controller.addAgent(screenX,screenY,camera,false,null);
                    leftPressed = false;
                    break;
                case CCTV_EDITOR:
                    controller.addAgent(screenX,screenY,camera,true,null);
                    leftPressed = false;
                    break;
                case EVADER_EDITOR:
                    controller.addEvader(screenX,screenY,camera,null);
                    leftPressed = false;
                    break;
                case DO_NOTHING:
                    leftPressed = false;
                    break;
            }
        }
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (leftPressed) {
            switch (controller.getMode()) {
                case WALL_EDITOR:
                    Ray pickRay = camera.getPickRay(screenX, screenY);
                    Vector3 intersection = new Vector3();
                    Intersector.intersectRayTriangles(pickRay,controller.getVertList(), controller.getmIndices(), 5, intersection);
                    if(wallVec.x !=0 && wallVec.z!=0 && intersection.x!=0 && intersection.z!=0) {
                        controller.addWall(wallVec.cpy(), intersection.cpy());
                    }
                    break;
            }
        }
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
