package com.dke.pursuitevasion.Entities.Systems.agents;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.dke.pursuitevasion.Entities.Components.ObservableComponent;
import com.dke.pursuitevasion.Entities.Components.ObserverComponent;
import com.dke.pursuitevasion.Entities.Components.StateComponent;
import com.dke.pursuitevasion.Entities.Components.agents.PursuerComponent;
import com.dke.pursuitevasion.Entities.Mappers;
import com.dke.pursuitevasion.Entities.Systems.VisionSystem;

/**
 * Created by Nicola Gheza on 23/05/2017.
 */
public class PursuerSystem extends IteratingSystem {
    private static final float DETECTION_TIME = 1.0f;
    private Vector2 position = new Vector2();
    private ImmutableArray<Entity> evaders;
    private VisionSystem visionSystem;


    public PursuerSystem(VisionSystem visionSystem) {
        super(Family.all(
                PursuerComponent.class
        ).get());

        this.visionSystem = visionSystem;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        movePursuer(entity, deltaTime);
        updateObserver(entity);
        updateDetection(entity, deltaTime);
    }

    @Override
    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);
        evaders = engine.getEntitiesFor(Family.all(ObservableComponent.class).get());
    }

    private void movePursuer(Entity entity, float deltaTime) {
        PursuerComponent pursuerComponent = Mappers.pursuerMapper.get(entity);
        StateComponent stateComponent = Mappers.stateMapper.get(entity);
        // Movin vision
        if (pursuerComponent.alerted)
            trackTarget(pursuerComponent, stateComponent);
        else
            moveVision(pursuerComponent, stateComponent, deltaTime);
        limitAngle(pursuerComponent);
    }

    // Point camera to target and keep tracks of it
    private void trackTarget(PursuerComponent pursuer, StateComponent state) {
        position.set(pursuer.targetPosition);
        Vector2 cameraPosition = new Vector2(state.position.x, state.position.z);
        position.sub(cameraPosition);
        position.nor();
        float angle = position.angle();
        pursuer.currentAngle = angle;
        pursuer.patrolStarted = false;
        state.angle = angle;
    }

    private void moveVision(PursuerComponent pursuerComponent, StateComponent stateComponent, float deltaTime) {
        if (!pursuerComponent.patrolStarted) {
            pursuerComponent.currentAngle = stateComponent.angle;
            pursuerComponent.patrolStarted = true;
        }

        if (pursuerComponent.waitTime == 0) {
            pursuerComponent.currentAngle += pursuerComponent.angularVelocity * pursuerComponent.direction.value() * deltaTime;
            if (pursuerComponent.currentAngle <= pursuerComponent.minAngle) {
                pursuerComponent.waitTime = pursuerComponent.waitTimeMinAngle;
                pursuerComponent.direction = pursuerComponent.direction.invert();
            }
            else if (pursuerComponent.currentAngle >= pursuerComponent.maxAngle) {
                pursuerComponent.waitTime = pursuerComponent.waitTimeMaxAngle;
                pursuerComponent.direction = pursuerComponent.direction.invert();
            }
        } else {
            pursuerComponent.waitTime = Math.max(pursuerComponent.waitTime - deltaTime, 0.0f);
        }
        stateComponent.angle = pursuerComponent.currentAngle;
    }

    private void limitAngle(PursuerComponent pursuerComponent) {
        pursuerComponent.currentAngle = MathUtils.clamp(
                pursuerComponent.currentAngle,
                pursuerComponent.minAngle,
                pursuerComponent.maxAngle
        );
    }

    private void updateObserver(Entity entity) {
        PursuerComponent pursuerComponent = Mappers.pursuerMapper.get(entity);
        ObserverComponent observerComponent = Mappers.observerMapper.get(entity);
        observerComponent.angle = pursuerComponent.currentAngle;
    }

    private void updateDetection(Entity entity, float deltaTime) {
        PursuerComponent pursuer = Mappers.pursuerMapper.get(entity);

        pursuer.alerted = false;

        for (Entity target : evaders) {
            updateDetection(entity, target);

            if (pursuer.alerted)
                break;
        }

        pursuer.detectionTime = pursuer.alerted ? pursuer.detectionTime + deltaTime : 0.0f;

        if (pursuer.detectionTime > DETECTION_TIME) {
            pursuer.detectionTime = 0.0f;
            System.out.println(" intruder detected" + pursuer.targetPosition);
        }
    }

    private void updateDetection(Entity entity, Entity target) {
        Vector2 targetPos = Mappers.observableMapper.get(target).position;
        PursuerComponent pursuer = Mappers.pursuerMapper.get(entity);

        pursuer.alerted = false;
        pursuer.targetPosition.set(0.0f, 0.0f);

        if (visionSystem.canSee(entity,target)) {
            pursuer.alerted = true;
            pursuer.targetPosition.set(targetPos);
        }

    }

}
