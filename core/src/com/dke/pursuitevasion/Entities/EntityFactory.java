package com.dke.pursuitevasion.Entities;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.dke.pursuitevasion.EdgeVectors;
import com.dke.pursuitevasion.Entities.Components.*;
import com.dke.pursuitevasion.WallInfo;

import static com.badlogic.gdx.graphics.GL20.GL_TRIANGLES;

/**
 * Created by Nicola Gheza on 20/03/2017.
 */
public class EntityFactory {
    static EntityFactory instance;

    static EntityFactory getInstance() {
        if (instance != null)
            return instance;
        return instance = new EntityFactory();
    }

    public Entity createAgent(Vector3 position, Color color) {
        Entity entity = new Entity();

        StateComponent transformComponent = new StateComponent();
        transformComponent.position = position;
        transformComponent.orientation = new Quaternion(position,0);
        transformComponent.update();
        entity.add(transformComponent);

        //Create a sphere collider component
        SphereColliderComponent sphereColliderComponent = new SphereColliderComponent();
        sphereColliderComponent.radius = 0.15f;
        entity.add(sphereColliderComponent);

        ModelBuilder modelBuilder = new ModelBuilder();
        Model model = modelBuilder.createSphere(0.15f, 0.15f, 0.15f, 20, 20, new Material(ColorAttribute.createDiffuse(color)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);

        ModelInstance agentModel = new ModelInstance(model);

        GraphicsComponent graphicsComponent = new GraphicsComponent();
        graphicsComponent.modelInstance = agentModel;
        entity.add(graphicsComponent);

        VisibleComponent visibleComponent = new VisibleComponent();
        entity.add(visibleComponent);

        MovableComponent movableComponent = new MovableComponent();
        entity.add(movableComponent);

        return entity;
    }

    public Entity createTerrain(Mesh mesh, EdgeVectors[] edgeVectors) {
        Entity entity = new Entity();

        FileHandle img = Gdx.files.internal("wood.jpg");
        Texture texture = new Texture(img, Pixmap.Format.RGB565, false);
        texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        texture.setFilter(Texture.TextureFilter.Linear,
                Texture.TextureFilter.Linear);

        StateComponent transformComponent = new StateComponent();
        transformComponent.position = new Vector3();
        transformComponent.orientation = new Quaternion(new Vector3(0, 0, 0), 0);
        entity.add(transformComponent);

        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.begin();
        modelBuilder.part("1", mesh, GL_TRIANGLES, new Material(new TextureAttribute(TextureAttribute.Diffuse, texture)));
        Model model = modelBuilder.end();


        //ModelInstance polygonModel = new ModelInstance(model);

        //Add model to the entity
        GraphicsComponent graphicsComponent = new GraphicsComponent();
        graphicsComponent.mesh = mesh;
        entity.add(graphicsComponent);

        //Make it visible
        VisibleComponent visibleComponent = new VisibleComponent();
        entity.add(visibleComponent);

        return entity;
    }

    public Entity createBoundary(EdgeVectors eV) {
        Entity entity = new Entity();
        WallComponent wallComponent = new WallComponent();
        wallComponent.eV = eV;

        entity.add(wallComponent);
        return entity;
    }

    public Entity createWall(WallInfo wallInfo) {

        Entity entity = new Entity();
        StateComponent transformComponent = new StateComponent();
        wallInfo.position.y+=wallInfo.height/2;
        transformComponent.transform.setToTranslation(wallInfo.position);
        transformComponent.transform.rotateRad(new Vector3(0, 1, 0), wallInfo.rotAngle);
        transformComponent.autoTransformUpdate = false;
        entity.add(transformComponent);

        ModelBuilder modelBuilder = new ModelBuilder();
        Model wall = modelBuilder.createBox(wallInfo.length-0.05f,wallInfo.height,0.08f,new Material(ColorAttribute.createDiffuse(Color.LIGHT_GRAY)), VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        ModelInstance wallInstance = new ModelInstance(wall);
        wallInstance.transform = transformComponent.transform;

        WallComponent wallComponent = new WallComponent();
        wallComponent.eV = new EdgeVectors(wallInfo.start, wallInfo.end);
        entity.add(wallComponent);

        GraphicsComponent graphicsComponent = new GraphicsComponent();
        graphicsComponent.modelInstance = wallInstance;
        entity.add(graphicsComponent);

        VisibleComponent visibleComponent = new VisibleComponent();
        entity.add(visibleComponent);

        return entity;
    }
}