package com.dke.pursuitevasion.Entities.Systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.math.Vector3;
import com.dke.pursuitevasion.Entities.Components.MovableComponent;
import com.dke.pursuitevasion.Entities.Components.StateComponent;
import com.dke.pursuitevasion.Entities.Components.VisibleComponent;

import java.util.Random;

/**
 * Created by Nicola Gheza on 25/03/2017.
 */
public class SimulationSystem extends EntitySystem {
    private final float STEP_SIZE = 1f/60f;
    private ImmutableArray<Entity> entities;

    private float timeAccumulator = 0f;

    private ComponentMapper<StateComponent> sm = ComponentMapper.getFor(StateComponent.class);
    private ComponentMapper<MovableComponent> mm = ComponentMapper.getFor(MovableComponent.class);

    public void addedToEngine(Engine engine) {
        entities = engine.getEntitiesFor(Family.all(StateComponent.class, VisibleComponent.class).get());
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
            Random r = new Random();
            float x = sm.get(entities.get(i)).position.x;
            x -= STEP_SIZE;
            sm.get(entities.get(i)).position.set(new Vector3(x,0,0));
            sm.get(entities.get(i)).update();
        }
    }
}
