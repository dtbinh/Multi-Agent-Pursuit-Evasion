package com.dke.pursuitevasion.Entities.Systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector3;
import com.dke.pursuitevasion.Entities.Components.*;

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
    private ComponentMapper<SphereColliderComponent> scm = ComponentMapper.getFor(SphereColliderComponent.class);
    private ComponentMapper<WallComponent> wm = ComponentMapper.getFor(WallComponent.class);

    public void addedToEngine(Engine engine) {
        entities = engine.getEntitiesFor(Family.all(StateComponent.class, VisibleComponent.class).get());
        bounds = engine.getEntitiesFor(Family.all(WallComponent.class).get());
    }

    public void update(float delta) {
        timeAccumulator += delta;
        for (int i=0; i<(int)(timeAccumulator/STEP_SIZE); i++) {
            stepUpdate(delta);
            timeAccumulator -= STEP_SIZE;
        }
    }

    private void stepUpdate(float delta) {
        for (int i=0; i<entities.size(); i++) {
            if (!mm.has(entities.get(i)))
                continue;
            // Entity is movable
            Random r = new Random();
            float x = sm.get(entities.get(i)).position.x;
            x -= STEP_SIZE;
            Vector3 nextPos = sm.get(entities.get(i)).position.cpy();
            nextPos.mulAdd(sm.get(entities.get(i)).velocity, delta);
            if (!checkForCollisions(nextPos, scm.get(entities.get(i)).radius)) {
                sm.get(entities.get(i)).position.mulAdd(sm.get(entities.get(i)).velocity, delta);
                sm.get(entities.get(i)).update();
            }
        }
    }

    private boolean checkForCollisions(Vector3 position, float radius) {
        Intersector intersector = new Intersector();
        for (int i=0; i<bounds.size(); i++) {
            float distance = intersector.distanceLinePoint(wm.get(bounds.get(i)).eV.Vector1.x,wm.get(bounds.get(i)).eV.Vector1.z,wm.get(bounds.get(i)).eV.Vector2.x,wm.get(bounds.get(i)).eV.Vector2.z,position.x, position.z);
            if (distance - (radius/2) < 0.00005) {
                System.out.println(distance);
                return true;
            }
        }
        return false;
    }
}
