package com.dke.pursuitevasion.Entities.Systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.dke.pursuitevasion.Entities.Components.*;
import com.dke.pursuitevasion.Entities.Mappers;

import java.util.Random;

/**
 * Created by Nicola Gheza on 25/03/2017.
 */
public class SimulationSystem extends EntitySystem {
    private final float STEP_SIZE = 1f/60f;
    private ImmutableArray<Entity> entities;
    private ImmutableArray<Entity> bounds;
    Vector3 bounce;

    private float timeAccumulator = 0f;

    public void addedToEngine(Engine engine) {
        entities = engine.getEntitiesFor(Family.all(StateComponent.class, VisibleComponent.class).get());
        bounds = engine.getEntitiesFor(Family.all(WallComponent.class).get());
    }

    public void update(float delta) {
        timeAccumulator += delta;
        for (int i=0; i<(int)(timeAccumulator/STEP_SIZE); i++) {
            //stepUpdate(delta);
            timeAccumulator -= STEP_SIZE;
        }
    }

    private float directionChangeFrequency = 2f;
    private float minDirectionChangeAmount = 0.1f;
    private float maxDirectionChangeAmount = 0.5f;

    private Random randomNumberGen = new Random();

    float directionChangetimer = 0;

    private void stepUpdate(float delta) {
        for (int i=0; i<entities.size(); i++) {
            if (!Mappers.agentMapper.has(entities.get(i)))
                continue;

            directionChangetimer += delta;

            Vector3 nextPos = Mappers.stateMapper.get(entities.get(i)).position.cpy();
            nextPos.mulAdd(Mappers.stateMapper.get(entities.get(i)).velocity, delta);
            if (directionChangetimer >= directionChangeFrequency) {
                directionChangetimer -= directionChangeFrequency;
                float directionChangeRange = maxDirectionChangeAmount - minDirectionChangeAmount;
                // calculate a random change amount between the minimum and max
                float directionChangeAmount = randomNumberGen.nextFloat() * directionChangeRange + minDirectionChangeAmount;
                // flip the sign half the time so that the velocity increases and decreases
                Random random = new Random();
                if(random.nextBoolean()){
                    directionChangeAmount  = -directionChangeAmount;
                }
                Mappers.stateMapper.get(entities.get(i)).velocity.z += directionChangeAmount; // apply the change amount to the velocity
            }
            if (!checkForCollisions(nextPos, Mappers.agentMapper.get(entities.get(i)).radius, Mappers.stateMapper.get(entities.get(i)).velocity)) {

                Mappers.stateMapper.get(entities.get(i)).position.mulAdd(Mappers.stateMapper.get(entities.get(i)).velocity, delta);
                Mappers.stateMapper.get(entities.get(i)).update();

            } else {
                Mappers.stateMapper.get(entities.get(i)).velocity = bounce;
            }
        }
    }

    private boolean checkForCollisions(Vector3 position, float radius, Vector3 velo) {
        Intersector intersector = new Intersector();
        for (int i=0; i<bounds.size(); i++) {
            Vector2 v1 = new Vector2(Mappers.wallMapper.get(bounds.get(i)).eV.Vector1.x,Mappers.wallMapper.get(bounds.get(i)).eV.Vector1.z);
            Vector2 v2 = new Vector2(Mappers.wallMapper.get(bounds.get(i)).eV.Vector2.x,Mappers.wallMapper.get(bounds.get(i)).eV.Vector2.z);
            Vector2 point = new Vector2(position.x, position.z);
            float distance = intersector.distanceSegmentPoint(v1,v2, point);
            if (distance - (radius/2) < 0.005) {
                if(velo!=null) {
                    Vector3 one = Mappers.wallMapper.get(bounds.get(i)).eV.Vector1;
                    Vector3 two = Mappers.wallMapper.get(bounds.get(i)).eV.Vector2;
                    Vector3 three = (two.cpy().sub(one.cpy()).nor());

                    Random r = new Random();
                    float chance = r.nextFloat();
                    if (chance <= 0.10f){
                        bounce = velo.scl(-1f);
                    }else{
                        bounce = three.cpy().sub(velo.cpy()).nor();
                    }

                    /*Vector3 three = (one.cpy().sub(two)).nor();
                    bounce = velo.cpy().sub(three).nor();*/

                }

                //System.out.println("trig");
                return true;
            }
        }
        return false;
    }


}
