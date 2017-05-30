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
import com.dke.pursuitevasion.Entities.Components.agents.PursuerComponent;
import com.dke.pursuitevasion.Entities.Mappers;
import com.dke.pursuitevasion.Entities.Systems.VisionSystem;
import com.dke.pursuitevasion.PolyMap;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nicola Gheza on 23/05/2017.
 */
public class PursuerSystem extends IteratingSystem {
    private static final float DETECTION_TIME = 0.5f;

    private ImmutableArray<Entity> evaders;
    private VisionSystem visionSystem;
    private Vector2 position = new Vector2();

    private PathFinder pathFinder;
    List<Node> p;

    public PursuerSystem(VisionSystem visionSystem, PolyMap map) {
        super(Family.all(PursuerComponent.class).get());

        this.visionSystem = visionSystem;
        pathFinder = new PathFinder(map);
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

        if(pursuerComponent.alerted) {
            followPath(pursuerComponent, stateComponent);
            trackTarget(pursuerComponent, stateComponent);
        }
        else {
            moveVision(pursuerComponent, stateComponent, deltaTime);
        }

        limitAngle(pursuerComponent);

    }
    private void followPath(PursuerComponent pC, StateComponent sC){
        if(pC.pursuerPath!=null && pC.pursuerPath.size()>0){
            Vector3 pos = pC.pursuerPath.remove(0);
            pC.position = pos;
            sC.position = pos;
            sC.update();
        }
    }

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
            Vector3 start = new Vector3(pursuer.position.x, 0, pursuer.position.z);
            Vector3 end = new Vector3(pursuer.targetPosition.x, 0, pursuer.targetPosition.y);
            p = pathFinder.findPath(start, end);
            //reset start node to current position of pursuer.
            if(p!=null && p.size()<0) {
                p.get(0).worldX = pursuer.position.x;
                p.get(0).worldZ = pursuer.position.z;
            }

            addAdditionalSteps(pursuer, p, start);
            pursuer.detectionTime = 0.0f;
        }
    }

    public void addAdditionalSteps(PursuerComponent pC, List<Node> p, Vector3 Start){
        pC.pursuerPath = new ArrayList<Vector3>();
        float stepSize = 15;
        if(p.size()>1) {
            for (int i = 0; i < p.size() - 1; i++) {
                Vector3 start = new Vector3(p.get(i).worldX, p.get(i).worldY, p.get(i).worldZ);
                if(i==0){
                    start = Start;
                }
                Vector3 end = new Vector3(p.get(i + 1).worldX, p.get(i + 1).worldY, p.get(i + 1).worldZ);
                float adjX = end.x - start.x;
                float adjZ = end.z - start.z;
                for (float j = 1; j < stepSize; j++) {
                    double scale = j/stepSize;
                    BigDecimal sc = BigDecimal.valueOf(scale);
                    BigDecimal xX = BigDecimal.valueOf(adjX);
                    BigDecimal zZ = BigDecimal.valueOf(adjZ);
                    BigDecimal newX = sc.multiply(xX);
                    BigDecimal newZ = sc.multiply(zZ);
                    float bDX = newX.floatValue();
                    float bDZ = newZ.floatValue();
                    float x = (float) (scale * adjZ);
                    float z = (float) (scale * adjZ);
                    if(bDX==0 && bDZ==0) {
                        System.out.println(scale+"  "+adjX+"   "+adjZ);
                    }
                    Vector3 position = new Vector3(start.x+bDX, 0, start.z+bDZ);
                    pC.pursuerPath.add(position);
                }
            }
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
