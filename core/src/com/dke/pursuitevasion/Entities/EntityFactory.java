package com.dke.pursuitevasion.Entities;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Mesh;

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

        return null;
    }
}
