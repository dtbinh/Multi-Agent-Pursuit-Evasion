package com.dke.pursuitevasion.Entities.Systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector3;
import com.dke.pursuitevasion.Entities.Components.MovableComponent;
import com.dke.pursuitevasion.Entities.Components.StateComponent;
import com.dke.pursuitevasion.Entities.Components.VisibleComponent;
import com.dke.pursuitevasion.Entities.Components.WallComponent;

import java.util.Random;

/**
 * Created by Nicola Gheza on 25/03/2017.
 */
public class SimulationSystem extends EntitySystem {
    private final float STEP_SIZE = 1f/200f;
    private ImmutableArray<Entity> entities;
    private ImmutableArray<Entity> bounds;

    private float timeAccumulator = 0f;

    private ComponentMapper<StateComponent> sm = ComponentMapper.getFor(StateComponent.class);
    private ComponentMapper<MovableComponent> mm = ComponentMapper.getFor(MovableComponent.class);
    private ComponentMapper<WallComponent> wm = ComponentMapper.getFor(WallComponent.class);

    public void addedToEngine(Engine engine) {
        entities = engine.getEntitiesFor(Family.all(StateComponent.class, VisibleComponent.class).get());
        bounds = engine.getEntitiesFor(Family.all(WallComponent.class).get());
    }

    public void update(float delta) {
        timeAccumulator += delta;
        for (int i=0; i<(int)(timeAccumulator/STEP_SIZE); i++) {
            stepUpdate();
            timeAccumulator -= STEP_SIZE;
        }
    }

    private void stepUpdate() {
        for (int i=0; i<entities.size(); i++) {
            if (!mm.has(entities.get(i)))
                continue;
            // Entity is movable
            Random r = new Random();
            float x = sm.get(entities.get(i)).position.x;
            x -= STEP_SIZE;
            Vector3 position = new Vector3(x, 0, 0);
            checkForCollisions(position);
            /*
            System.out.println("All bounds in map");
            for(int j=0;j<bounds.size();j++){
                System.out.println(wm.get(bounds.get(j)).eV.Vector1.x+"   "+wm.get(bounds.get(j)).eV.Vector1.z);
                System.out.println(wm.get(bounds.get(j)).eV.Vector2.x+"   "+wm.get(bounds.get(j)).eV.Vector2.z);
            }*/
            if(!checkForCollisions(position)) {
                sm.get(entities.get(i)).position.set(new Vector3(x, 0, 0));
                sm.get(entities.get(i)).update();
            }
        }
    }

    private boolean checkForCollisions(Vector3 position) {
        Intersector intersector = new Intersector();
        for (int i=0; i<bounds.size(); i++) {
            Vector3 v1 = wm.get(bounds.get(i)).eV.Vector1;
            Vector3 v2 = wm.get(bounds.get(i)).eV.Vector2;
            float startX = v1.x;
            float startY = v1.z;
            float endX = v2.x;
            float endY = v2.z;
            //float distance = intersector.distanceLinePoint(wm.get(bounds.get(i)).eV.Vector1.x,wm.get(bounds.get(i)).eV.Vector1.z,wm.get(bounds.get(i)).eV.Vector2.x,wm.get(bounds.get(i)).eV.Vector2.z,position.x, position.z);
            float distance = intersector.distanceLinePoint(startX, startY, endX, endY,position.x, position.z);
            if (distance < 0.003) {
                //System.out.println(distance);
                return true;
            }
        }
        return false;
    }
}
