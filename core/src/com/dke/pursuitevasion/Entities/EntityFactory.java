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
import com.badlogic.gdx.math.Vector3;
import com.dke.pursuitevasion.Entities.Components.GraphicsComponent;
import com.dke.pursuitevasion.Entities.Components.StateComponent;
import com.dke.pursuitevasion.Entities.Components.VisibleComponent;
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

    public Entity createTerrain(Mesh mesh) {
        Entity entity = new Entity();

        FileHandle img = Gdx.files.internal("wood.jpg");
        Texture texture = new Texture(img, Pixmap.Format.RGB565, false);
        texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        texture.setFilter(Texture.TextureFilter.Linear,
                Texture.TextureFilter.Linear);

        //Creating a model builder every time is inefficient, but so is talking about this. (JUST WERKS)
        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.begin();
        modelBuilder.part("1", mesh, GL_TRIANGLES, new Material(new TextureAttribute(TextureAttribute.Diffuse, texture)));
        Model model = modelBuilder.end();

        ModelInstance polygonModel = new ModelInstance(model, 0,0,0);

        //Add model to the entity
        GraphicsComponent graphicsComponent = new GraphicsComponent();
        graphicsComponent.modelInstance = polygonModel;
        entity.add(graphicsComponent);

        //Make it visible
        VisibleComponent visibleComponent = new VisibleComponent();
        entity.add(visibleComponent);
        return entity;
    }

    public Entity createWall(WallInfo wallInfo) {
        Entity entity = new Entity();

        StateComponent transformComponent = new StateComponent();
        transformComponent.transform.setToTranslation(wallInfo.position);
        transformComponent.transform.rotateRad(new Vector3(0, 1, 0), wallInfo.rotAngle);
        transformComponent.autoTransformUpdate = false;
        entity.add(transformComponent);

        ModelBuilder modelBuilder = new ModelBuilder();
        Model wall = modelBuilder.createBox(wallInfo.length,wallInfo.height,0.08f,new Material(ColorAttribute.createDiffuse(Color.LIGHT_GRAY)), VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        ModelInstance wallInstance = new ModelInstance(wall);
        wallInstance.transform = transformComponent.transform;

        GraphicsComponent graphicsComponent = new GraphicsComponent();
        graphicsComponent.modelInstance = wallInstance;
        entity.add(graphicsComponent);

        VisibleComponent visibleComponent = new VisibleComponent();
        entity.add(visibleComponent);

        return entity;
    }
}
