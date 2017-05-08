package com.dke.pursuitevasion.Entities.Systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.dke.pursuitevasion.Entities.Components.GraphicsComponent;
import com.dke.pursuitevasion.Entities.Components.StateComponent;
import com.dke.pursuitevasion.Entities.Components.VisibleComponent;
import com.dke.pursuitevasion.Entities.Mappers;

/**
 * Created by Nicola Gheza on 17/03/2017.
 */
public class GraphicsSystem extends EntitySystem {
    private ImmutableArray<Entity> entities;

    private ModelBatch modelBatch = new ModelBatch();
    private Camera cam;
    private Environment env;
    private ShaderProgram shader;
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

    public GraphicsSystem(Camera cam, Environment env) {
        this.cam = cam;
        this.env = env;
        shader = new ShaderProgram(vertexShader, fragmentShader);
    }

    /**
     * Get the camera
     *
     * @returns env
     */
    public Camera getCam() {
        return cam;
    }

    /**
     * Set the camera
     * @param cam
     */
    public void setCam(Camera cam) {
        this.cam = cam;
    }

    /**
     * Get the environment
     * @returns env
     */
    public Environment getEnv() {
        return env;
    }

    /**
     * Set the environment
     * @param env
     */
    public void setEnv(Environment env) {
        this.env = env;
    }

    public void addedToEngine(Engine engine) {
        entities = engine.getEntitiesFor(Family.all(StateComponent.class, GraphicsComponent.class, VisibleComponent.class).get());
    }


    private void renderSystems() {
        for (EntitySystem system : getEngine().getSystems()) {
            if (system instanceof DebugRenderer) {
                DebugRenderer renderer = (DebugRenderer) system;
                renderer.render(modelBatch);
            }
        }
    }


    /**
     * Draw all components with GraphicsComponent
     * @param deltaTime
     */
    public void update(float deltaTime) {
        modelBatch.begin(cam);
        for (int i = 0; i < entities.size(); ++i) {
            Entity entity = entities.get(i);
            StateComponent state = Mappers.stateMapper.get(entity);
            GraphicsComponent graphics = Mappers.graphicsMapper.get(entity);

            if (graphics.modelInstance!= null && state.autoTransformUpdate) {
                graphics.modelInstance.transform = state.transform;
            }
            if(graphics.modelInstance!=null)
                modelBatch.render(graphics.modelInstance, env);

            shader.begin();
            shader.setUniformMatrix("u_worldView", cam.combined);
            shader.setUniformi("u_texture", 0);
            if(graphics.modelInstance==null && graphics.mesh!=null) {
                graphics.mesh.render(shader, GL20.GL_TRIANGLES);
            }
            shader.end();
        }
        renderSystems();
        modelBatch.end();
    }
}
