package com.dke.pursuitevasion.Entities.Systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectSet;
import com.dke.pursuitevasion.EdgeVectors;
import com.dke.pursuitevasion.Entities.Components.ObservableComponent;
import com.dke.pursuitevasion.Entities.Components.ObserverComponent;
import com.dke.pursuitevasion.Entities.Components.StateComponent;
import com.dke.pursuitevasion.Entities.Components.WallComponent;


/**
 * Created by Nicola Gheza on 17/04/2017.
 */
public class VisionSystem extends IteratingSystem implements EntityListener, DebugRenderer {

    private ObjectMap<Entity, ObjectSet<Entity>> vision = new ObjectMap();
    private Vector2 toObservable = new Vector2();

    private ComponentMapper<ObservableComponent> observableMapper = ComponentMapper.getFor(ObservableComponent.class);
    private ComponentMapper<ObserverComponent> observerMapper = ComponentMapper.getFor(ObserverComponent.class);
    private ComponentMapper<WallComponent> wallMapper = ComponentMapper.getFor(WallComponent.class);
    private ComponentMapper<StateComponent> stateMapper = ComponentMapper.getFor(StateComponent.class);
    private ImmutableArray<Entity> observables;
    private ImmutableArray<Entity> walls;

    public VisionSystem() {
        super(Family.all(ObserverComponent.class).get());
    }

    /**
     * The addedToEngine() and removedFromEngine() methods are invoked whenever we register the system with the engine.
     */
    @Override
    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);
        observables = engine.getEntitiesFor(
                Family.all(ObservableComponent.class).get()
        );
        walls = engine.getEntitiesFor(
                Family.all(WallComponent.class).get()
        );
        engine.addEntityListener(getFamily(), this);
    }

    @Override
    public void removedFromEngine(Engine engine) {
        super.removedFromEngine(engine);
        engine.removeEntityListener(this);
    }

    @Override
    public void entityAdded(Entity entity) {
        vision.put(entity, new ObjectSet<Entity>());
    }

    @Override
    public void entityRemoved(Entity entity) {
        vision.remove(entity);
    }

    public boolean canSee(Entity observer, Entity observable) {
        ObjectSet<Entity> observables = vision.get(observer);

        if (observable == null)
            return false;
        return observables.contains(observable);
    }

    @Override
    protected void processEntity(Entity observer, float deltaTime) {
        ObserverComponent observerComponent = observerMapper.get(observer);
        StateComponent stateComponent = stateMapper.get(observer);
        observerComponent.update(stateComponent.position);
        updateVision(observer);
    }

    private void updateVision(Entity observer) {
        ObservableComponent observableComponent;
        StateComponent stateComponent;
        for (Entity observable : observables) {
            observableComponent = observableMapper.get(observable);
            stateComponent = stateMapper.get(observable);
            observableComponent.update(stateComponent.position);
            updateVision(observer, observable);
        }
    }

    private void updateVision(Entity observer, Entity observable) {
        if(!inFov(observer, observable))
        {
            removeFromVision(observer,observable);
            return;
        }

        raycast(observer, observable);
    }

    private void raycast(Entity entity, Entity target) {
        ObserverComponent observer = observerMapper.get(entity);
        ObservableComponent observable = observableMapper.get(target);

        //System.out.println("Observer position: " + observer.position );
        //System.out.println("Observable position: " + observable.position);

        for (Entity wallEntity : walls) {
            if (wallMapper.get(wallEntity).innerWall) {
                // It is an inner wall
                Vector2 p1 = observer.position;
                Vector2 p2 = observable.position;
                EdgeVectors eV = wallMapper.get(wallEntity).eV;
                Vector2 p3 = new Vector2(eV.Vector1.x, eV.Vector1.z);
                Vector2 p4 = new Vector2(eV.Vector2.x, eV.Vector2.z);

                Vector2 intersection = new Vector2();

                if (!Intersector.intersectSegments(p1, p2, p3, p4,intersection)) {
                    addToVision(entity, target);
                } else {
                    if (vision.containsKey(entity) && vision.get(entity).contains(target))
                        removeFromVision(entity, target);
                }
            }
        }
    }

    private void addToVision(Entity observer, Entity observable) {
        vision.get(observer).add(observable);
    }

    private void removeFromVision(Entity observer, Entity observable) {
        if (vision.containsKey(observer) && vision.get(observer).contains(observable))
            vision.get(observer).remove(observable);
    }

    private boolean inFov(Entity entity, Entity target) {
        ObserverComponent observer = observerMapper.get(entity);
        ObservableComponent observable = observableMapper.get(target);

        if (observer.position.isZero() || observable.position.isZero() ||
            observer.position.dst2(observable.position) > observer.distance * observer.distance)
            return false;
        toObservable.set(observable.position);
        toObservable.sub(observer.position);

        float toObservableAngle = toObservable.angle();
        float angleDifference = Math.abs(toObservableAngle - observer.angle);

        angleDifference = Math.min(angleDifference, 360.0f - angleDifference);

        if (angleDifference > observer.fovAngle)
            return false;

        return true;
    }


    private Vector2 tmp1 = new Vector2();
    private Vector2 tmp2 = new Vector2();

    @Override
    public void render(ModelBatch modelBatch) {
        ModelBuilder modelBuilder = new ModelBuilder();
        for (Entity entity : getEntities()) {
            modelBuilder.begin();
            MeshPartBuilder builder = modelBuilder.part("triangle", 1, 3, new Material());
            builder.setColor(Color.GREEN);

            ObserverComponent observer = observerMapper.get(entity);
            float halfFov = observer.fovAngle * 0.5f;

            tmp1.set(observer.distance, 0.0f);
            tmp1.rotate(observer.angle);
            tmp1.rotate(halfFov);
            tmp1.add(observer.position);

            tmp2.set(observer.distance, 0.0f);
            tmp2.rotate(observer.angle);
            tmp2.rotate(-halfFov);
            tmp2.add(observer.position);

            Vector3 p1 = new Vector3(observer.position.x, 0, observer.position.y);
            Vector3 p2 = new Vector3(tmp1.x, 0, tmp1.y);
            Vector3 p3 = new Vector3(tmp2.x, 0, tmp2.y);

            builder.triangle(p1, p2, p3);
            Model triangleModel = modelBuilder.end();
            ModelInstance triangleInstance = new ModelInstance(triangleModel);
            modelBatch.render(triangleInstance);
        }
    }
}
