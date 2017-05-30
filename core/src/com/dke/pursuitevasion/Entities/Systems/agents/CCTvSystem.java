package com.dke.pursuitevasion.Entities.Systems.agents;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.dke.pursuitevasion.AI.Node;
import com.dke.pursuitevasion.AI.PathFinder;
import com.dke.pursuitevasion.Entities.Components.ObservableComponent;
import com.dke.pursuitevasion.Entities.Components.ObserverComponent;
import com.dke.pursuitevasion.Entities.Components.StateComponent;
import com.dke.pursuitevasion.Entities.Components.agents.CCTvComponent;
import com.dke.pursuitevasion.Entities.Components.agents.PursuerComponent;
import com.dke.pursuitevasion.Entities.Mappers;
import com.dke.pursuitevasion.Entities.Systems.VisionSystem;
import com.dke.pursuitevasion.PolyMap;

import java.util.List;

/**
 * Created by Nicola Gheza on 08/05/2017.
 */
public class CCTvSystem extends IteratingSystem {
    private static final float DETECTION_TIME = 0.5f;

    private ImmutableArray<Entity> evaders;
    private ImmutableArray<Entity> pursuers;

    private VisionSystem visionSystem;
    private Vector2 position = new Vector2();

    private PathFinder pathFinder;

    public CCTvSystem(VisionSystem visionSystem, PolyMap map) {
        super(Family.all(
                CCTvComponent.class,
                StateComponent.class
        ).get());

        this.visionSystem = visionSystem;
        this.pathFinder = new PathFinder(map);
    }

    @Override
    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);
        evaders = engine.getEntitiesFor(Family.all(ObservableComponent.class).get());
        pursuers = engine.getEntitiesFor(Family.all(PursuerComponent.class).get());
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        moveCamera(entity, deltaTime);
        updateObserver(entity);
        updateDetection(entity, deltaTime);
    }

    private void updateDetection(Entity entity, float deltaTime) {
        CCTvComponent cctv = Mappers.cctvMapper.get(entity);

        cctv.alerted = false;

        for (Entity target : evaders) {
            updateDetection(entity, target);

            if (cctv.alerted)
                break;
            else {
                clearPursuerVision();
            }

        }

        cctv.detectionTime = cctv.alerted ? cctv.detectionTime + deltaTime : 0.0f;

        if (cctv.detectionTime > DETECTION_TIME) {
            cctv.detectionTime = 0.0f;
            //System.out.println(/*cctv +*/ " intruder detected" + cctv.targetPosition);
            updatePursuerPath(cctv.targetPosition);
        }
    }

    private void clearPursuerVision() {
        for (Entity e : pursuers) {
            PursuerComponent pC = Mappers.pursuerMapper.get(e);
          //  pC.cctvAlerted = false;
        }
    }

    private void updatePursuerPath(Vector2 targetPosition) {
        for (Entity e : pursuers) {
            PursuerComponent pC = Mappers.pursuerMapper.get(e);
            Vector3 start = new Vector3(pC.position.x, 0, pC.position.z);
            Vector3 end = new Vector3(targetPosition.x, 0, targetPosition.y);
            List<Node> path = pathFinder.findPath(start, end);
            if (path!=null && path.size()>0) {
                path.get(0).worldX = pC.position.x;
                path.get(0).worldZ = pC.position.z;
            }
            //pC.cctvAlerted = true;
            pC.targetPosition = targetPosition.cpy();
            pC.pursuerPath = PursuerSystem.addAdditionalSteps(pC, path, start);
        }
    }

    private void updateDetection(Entity entity, Entity target) {
        Vector2 targetPos = Mappers.observableMapper.get(target).position;
        CCTvComponent cctv = Mappers.cctvMapper.get(entity);

        cctv.alerted = false;
        cctv.targetPosition.set(0.0f, 0.0f);

        if (visionSystem.canSee(entity,target)) {
            cctv.alerted = true;
            cctv.targetPosition.set(targetPos);
        }
    }


    private void updateObserver(Entity entity) {
        CCTvComponent cctv = Mappers.cctvMapper.get(entity);
        ObserverComponent observer = Mappers.observerMapper.get(entity);

        observer.angle = cctv.currentAngle;
    }

    private void moveCamera(Entity entity, float deltaTime) {
        CCTvComponent cctv = Mappers.cctvMapper.get(entity);
        StateComponent state = Mappers.stateMapper.get(entity);

        if(cctv.alerted) {
            trackTarget(cctv, state);
        }
        else {
            movePatrol(cctv, state, deltaTime);
        }

        limitAngle(cctv);
    }

    // Point camera to target and keep tracks of it
    private void trackTarget(CCTvComponent cctv, StateComponent state) {
        position.set(cctv.targetPosition);
        Vector2 cameraPosition = new Vector2(state.position.x, state.position.z);
        position.sub(cameraPosition);
        position.nor();
        float angle = position.angle();
        cctv.currentAngle = angle;
        cctv.patrolStarted = false;
        state.angle = angle;
    }

    // Move camera in patrol
    private void movePatrol(CCTvComponent cctv, StateComponent state, float deltaTime) {
        if (!cctv.patrolStarted) {
            cctv.currentAngle = state.angle;
            cctv.patrolStarted = true;
        }

        if (cctv.waitTime == 0) {
            cctv.currentAngle += cctv.angularVelocity * cctv.direction.value() * deltaTime;
            if (cctv.currentAngle <= cctv.minAngle) {
                cctv.waitTime = cctv.waitTimeMinAngle;
                cctv.direction = cctv.direction.invert();
            }
            else if (cctv.currentAngle >= cctv.maxAngle) {
                cctv.waitTime = cctv.waitTimeMaxAngle;
                cctv.direction = cctv.direction.invert();
            }
        }
        else {
            cctv.waitTime = Math.max(cctv.waitTime - deltaTime, 0.0f);
        }
        state.angle = cctv.currentAngle;
    }

    private void limitAngle(CCTvComponent cctv) {
        cctv.currentAngle = MathUtils.clamp(
                cctv.currentAngle,
                cctv.minAngle,
                cctv.maxAngle
        );
    }
}
