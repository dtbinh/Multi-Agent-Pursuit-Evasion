package com.dke.pursuitevasion.Entities;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.dke.pursuitevasion.Entities.Components.GraphicsComponent;
import com.dke.pursuitevasion.Entities.Components.VisibleComponent;

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
        //Creating a model builder every time is inefficient, but so is talking about this. (JUST WERKS)
        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.begin();
        modelBuilder.part("1", mesh, GL_TRIANGLES, new Material(ColorAttribute.createDiffuse(Color.GOLD)));
        Model model = modelBuilder.end();

        ModelInstance polygonModel = new ModelInstance(model, 0,0,0);

        GraphicsComponent graphicsComponent = new GraphicsComponent();
        graphicsComponent.modelInstance = polygonModel;
        entity.add(graphicsComponent);

        //Make it visible
        VisibleComponent visibleComponent = new VisibleComponent();
        entity.add(visibleComponent);

        return entity;
    }
}
