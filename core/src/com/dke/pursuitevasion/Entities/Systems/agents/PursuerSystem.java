package com.dke.pursuitevasion.Entities.Systems.agents;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.dke.pursuitevasion.AI.CoorExplo.CoordExplor;
import com.dke.pursuitevasion.AI.Node;
import com.dke.pursuitevasion.AI.PathFinder;
import com.dke.pursuitevasion.CXSearchingAlgorithm.CXAgentState;
import com.dke.pursuitevasion.CXSearchingAlgorithm.CXAgentTask;
import com.dke.pursuitevasion.CXSearchingAlgorithm.CXAgentTaskType.CXAgentMovingTask;
import com.dke.pursuitevasion.CXSearchingAlgorithm.CXAgentUtility;
import com.dke.pursuitevasion.CXSearchingAlgorithm.CXMessage.CXMessage;
import com.dke.pursuitevasion.CellDecompose.Graph.*;
import com.dke.pursuitevasion.Entities.Components.agents.EvaderComponent;
import com.dke.pursuitevasion.Entities.Components.ObserverComponent;
import com.dke.pursuitevasion.Entities.Components.StateComponent;
import com.dke.pursuitevasion.Entities.Components.agents.PursuerComponent;
import com.dke.pursuitevasion.Entities.EntityFactory;
import com.dke.pursuitevasion.Entities.Mappers;
import com.dke.pursuitevasion.Entities.Systems.DebugRenderer;
import com.dke.pursuitevasion.Entities.Systems.VisionSystem;
import com.dke.pursuitevasion.PolyMap;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Nicola Gheza on 23/05/2017.
 */
public class PursuerSystem extends IteratingSystem implements DebugRenderer {
    private static final float DETECTION_TIME = 0.0f;

    private ImmutableArray<Entity> evaders;
    private VisionSystem visionSystem;
    private Vector2 position = new Vector2();

    private PathFinder pathFinder;
    private EntityFactory entityFactory;
    List<Node> p;

    CXGraph graph;
    /*
    For saving the message
     */
    private LinkedList messageArrayList = new LinkedList();
    /*
    Saving searched area, <K,V> = <searchedAreaNunber,boolean>
     */
    private HashMap searchedArea = new HashMap();
    /*
    Deal with complex computation process for agent
     */
    private CXAgentUtility agentUtility = new CXAgentUtility();

    private boolean runOnce = false;
    private int messageNumber;
    private Engine engine;
    private String AI;

    boolean completeUpdate = false;
    boolean terribleSolution = false;
    private int pursuerIndex=-1, coordUpdateCounter = 0;
    int discWidth = 100;
    int pursCount, restartCounter=0;
    PolyMap mMap;
    CoordExplor coordExplorer;
    ArrayList<ModelInstance> nodes =  new ArrayList<ModelInstance>();

    public int mapSize = 250;
    public float gapSize = 0.1f;

    public PursuerSystem(VisionSystem visionSystem, CXGraph graph, PolyMap map, int pursuerCount, String AI) {
        super(Family.all(PursuerComponent.class).get());

        this.visionSystem = visionSystem;
        this.graph = graph;
        pathFinder = new PathFinder(map, mapSize, gapSize);
        entityFactory = new EntityFactory();

        mMap = map;
        pursCount = pursuerCount;
        coordExplorer = new CoordExplor(map, 0.2f, discWidth, pursuerCount, pathFinder);

        ModelBuilder modelBuilder = new ModelBuilder();
        Model box = modelBuilder.createBox(0.05f, 0.02f, 0.05f,new Material(ColorAttribute.createDiffuse(Color.RED)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        boolean[][] nodeGrid = pathFinder.getNodeGrid();
        for(int i=0;i<pathFinder.getNodeGrid().length;i++){
            for(int j=0;j<pathFinder.getNodeGrid().length;j++){
                if(nodeGrid[i][j]) {
                    Vector3 pos = PathFinder.positionFromIndex(i, j, pathFinder.pF);
                    pos.y+=0.1f;
                    ModelInstance mBox = new ModelInstance(box, pos);
                    nodes.add(mBox);
                }
            }
        }

        this.AI = AI;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        if (AI.equals("GRAPHSEARCHER")) {
            if (!runOnce) {
                assignInitialTask(entity);
            } else {
                updateTask(entity);
            }
        } else {
            assignTaskorMove(entity);
        }
        updateObserver(entity);
        updateDetection(entity, deltaTime);

    }

    @Override
    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);
        if (!runOnce) this.engine = engine;
        evaders = engine.getEntitiesFor(Family.all(EvaderComponent.class).get());
    }

    public void assignTaskorMove(Entity entity){
        PursuerComponent pursuerComponent = Mappers.pursuerMapper.get(entity);
        ObserverComponent observerComponent = Mappers.observerMapper.get(entity);
        StateComponent stateComponent = Mappers.stateMapper.get(entity);
        if(completeUpdate && (terribleSolution ||(coordExplorer.unexploredFrontier.size()==0 && coordExplorer.runOnce))){
            pursuerComponent.updatePosition = true;
            pursuerComponent.pursuerPointPath.clear();
            if(restartCounter==0){
                coordExplorer.nodes.clear();
                coordExplorer.restart();
            }
            restartCounter++;
            coordExplorer.updateGrid(pursuerComponent, observerComponent);
            if(restartCounter<pursCount+1) {
                terribleSolution = true;
                coordExplorer.runOnce = true;
            }else{
                //System.out.println("RESTARTING");
                coordExplorer.runOnce = false;
                restartCounter = 0;
                terribleSolution = false;
            }
            completeUpdate = true;
        } else {
            coordUpdateCounter++;
            if (!completeUpdate && pursuerIndex == pursuerComponent.number) {
                completeUpdate = true;
            }
            //Update grid for every pursuer before assigning any moves
            if (!completeUpdate) {
                pursuerComponent.costs = new double[discWidth][discWidth];
                pursuerIndex = 0;
                coordExplorer.updateGrid(pursuerComponent, observerComponent);
            } else {
                //if there is no path, assign task
                if (pursuerComponent.pursuerPointPath.size() == 0) {
                    coordExplorer.assignTask(pursuerComponent);
                } else {
                    //update once every 10 calls for performance issues
                    if (coordUpdateCounter > 10) {
                        //need to update unexplored frontier cells
                        coordExplorer.updateGrid(pursuerComponent, observerComponent);
                        coordUpdateCounter = 0;
                    }
                    followPathCoordExplo(pursuerComponent, stateComponent);
                }
            }
        }
    }

    private void followPathCoordExplo(PursuerComponent pC, StateComponent sC){
        if(pC.pursuerPointPath!=null && pC.pursuerPointPath.size()>0){
            CXPoint pos = pC.pursuerPointPath.remove(0);
            pC.position = new Vector3((float) pos.x, 0, (float) pos.y);
            sC.position = new Vector3((float) pos.x, 0, (float) pos.y);
            sC.update();
        }
    }

    private void assignInitialTask(Entity entity){
        PursuerComponent pC = Mappers.pursuerMapper.get(entity);
        // The first time assign a task to a random agent.
        CXDecomposedGraphNode searchNode = GraphTool.findTheFarleftNode(graph);
        CXGraphNode graphNode =  searchNode.getTopRightNode();

        CXAgentTask task1 = new CXAgentTask(CXAgentState.Moving);
        CXPoint destination = CXPoint.convertToOriginalCoordination(graphNode.location);
        CXAgentMovingTask movingTask = new CXAgentMovingTask(destination);
        task1.movingTask = movingTask;

        // The first searching area
        CXDecomposedGraphNode node =  GraphTool.findTheFarleftNode(graph);
        CXDecomposedGraphNode finalNode = GraphTool.findTheFarRightNode(graph);
        this.agentUtility.finalArea = finalNode;

        CXAgentTask task2 = new CXAgentTask(CXAgentState.Scanning);
        task2.scanTask.scanScope.add(180.0f);
        task2.scanTask.scanScope.add(90.0f);

        pC.taskList.add(task1);
        pC.setState(CXAgentState.Moving);
        pC.currentSearchArea = node.nodeNumber;
        pC.taskList.add(task2);

        this.runOnce = true;
    }

    private void updateTask(Entity entity) {
        // Get the pursuer current state
        PursuerComponent pursuerC = Mappers.pursuerMapper.get(entity);
        StateComponent stateC = Mappers.stateMapper.get(entity);
        System.out.println();
        System.out.println("------------- Agent: "+ pursuerC.number + " -------------");

        if (pursuerC.number == 0){
            messageNumber = 0;
        }
        if (pursuerC.getState() == CXAgentState.WaitSearching || pursuerC.getState() == CXAgentState.Hold || pursuerC.getState() == CXAgentState.WaitBackup){
            messageNumber ++ ;
        }
        if (messageNumber == EntityFactory.pursuerCounter )
        {
            engine.addEntity(entityFactory.createPursuer(new Vector3(0f,0f,0f), Color.BLUE));
            messageNumber = 0;
        }


        switch (pursuerC.getState()){
            case Free:{
                this.printStateAndLocation("Free",new CXPoint(stateC.position.x, stateC.position.z));
                if (pursuerC.taskList.size() != 0){
                    CXAgentTask task = (CXAgentTask)pursuerC.taskList.getFirst();
                    pursuerC.setState(task.taskState);
                    break;
                }
                else {
                    pursuerC = this.agentUtility.checkMessage(this.messageArrayList, pursuerC);
                    // doesn't get task
                    if (pursuerC.taskList.size() == 0 && pursuerC.pursuerPointPath.size() == 0) {
                        this.agentUtility.randomMovement(stateC, pursuerC, pathFinder);

                    }
                    if(pursuerC.taskList.size() == 0 && pursuerC.pursuerPointPath.size()>0){
                        followPath(pursuerC, stateC);
                    }
                    else {
                        pursuerC.pursuerPointPath.clear();
                    }
                }
                    break;
                }
            case Hold:{
                this.printStateAndLocation("Hold",new CXPoint(stateC.position.x, stateC.position.z));
                pursuerC = this.agentUtility.checkMessage(this.messageArrayList,pursuerC);
                break;
            }
            case Searching:{
                this.printStateAndLocation("Seaching",new CXPoint(stateC.position.x, stateC.position.z));
                pursuerC = this.agentUtility.tranferSearchingTaskToMovingTask(pursuerC);
                this.searchedArea.put(pursuerC.currentSearchArea,Boolean.FALSE);
                break;
            }
            case FinisihSearching:{
                this.printStateAndLocation("FinishSearching",new CXPoint(stateC.position.x, stateC.position.z));
                // Put all searched Area in to map
                searchedArea.put(pursuerC.currentSearchArea,Boolean.TRUE);
                pursuerC = this.agentUtility.findNewSearchingArea(pursuerC,stateC,this.graph,searchedArea,messageArrayList);
                break;
            }
            case SendMessage:{
                this.printStateAndLocation("SendMessage",new CXPoint(stateC.position.x, stateC.position.z));
                // Send Message
                CXAgentTask task = (CXAgentTask) pursuerC.taskList.get(0);
                CXMessage message = new CXMessage();
                message.receiver = task.messageTask.messageReceiver;
                message.messageContent = task.messageTask.messageContent;
                message.messageType = task.messageTask.messageType;
                this.messageArrayList.add(message);

                // Update state
                pursuerC.taskList.removeFirst();
                if (pursuerC.taskList.size() != 0){
                    CXAgentTask newTask = (CXAgentTask)pursuerC.taskList.getFirst();
                    pursuerC.setState(newTask.taskState);
                    break;
                }
                pursuerC.setState(CXAgentState.Free);
                break;
            }
            case Moving:{
                CXPoint curLocation = new CXPoint(stateC.position.x, stateC.position.z);
                this.printStateAndLocation("Moving", curLocation);
                CXAgentTask task = (CXAgentTask) pursuerC.taskList.get(0);
                CXPoint destination = task.movingTask.movingDestination;

                // Get the update location
                float velocity = stateC.velocity.x;

                CXPoint updateLocation = this.agentUtility.getNextMovingPoint(pursuerC, curLocation, destination, pathFinder);

                CXPoint newDestination = CXPoint.converToGraphCoordination(destination);
                CXPoint newUpdateLocation = CXPoint.converToGraphCoordination(updateLocation);
                //System.out.println("Moving destination x = "+ newDestination.x + " y = "+newDestination.y + " ; UpdateLocation is x = "+ newUpdateLocation.x + " y = " + newUpdateLocation.y);

                stateC.position = stateC.position.set((float)updateLocation.x,stateC.position.y,(float)updateLocation.y);
                if (updateLocation == destination){
                    // Check the angle is correct
                    if (task.movingTask.radius != -1.0f && pursuerC.currentAngle != task.movingTask.radius){
                        pursuerC.currentAngle = agentUtility.getNextScanPositionForMoving(pursuerC,task.movingTask.radius);
                        System.out.println("Current angle " + pursuerC.currentAngle + " Target Angle " + task.movingTask.radius);
                    }
                    else {
                        pursuerC.taskList.removeFirst();
                        //System.out.println("Agent " + pursuerC.number + " is arrived Destination ");
                        if (!pursuerC.taskList.isEmpty()){
                            CXAgentTask newTask = (CXAgentTask) pursuerC.taskList.getFirst();
                            pursuerC.setState(newTask.taskState);
                        }
                        else {
                            pursuerC.setState(CXAgentState.FinisihSearching);
                        }
                    }
                }
                break;
            }
            case Scanning:{
                this.printStateAndLocation("Scanning",new CXPoint(stateC.position.x, stateC.position.z));
                CXAgentTask task = (CXAgentTask) pursuerC.taskList.getFirst();
                Float targetRadius = (Float) task.scanTask.scanScope.getFirst();
                float value =  this.agentUtility.getTheNextScanPosition(pursuerC,targetRadius);
                System.out.println("Current Radius is " + stateC.angle + " TargetRadius is "+ value);
                stateC.angle = value;
                pursuerC.currentAngle = value; // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

                if (task.scanTask.scanScope.isEmpty()){
                    pursuerC.taskList.removeFirst();
                    if (pursuerC.taskList.isEmpty()){
                        pursuerC.setState(CXAgentState.FinisihSearching);
                    }
                    else {
                        CXAgentTask agentTask = (CXAgentTask) pursuerC.taskList.getFirst();
                        pursuerC.setState(agentTask.taskState);
                    }
                }
                break;
            }
            case WaitBackup:{
                this.printStateAndLocation("WaitBackup",new CXPoint(stateC.position.x, stateC.position.z));
                // 1. Check the the right area is marked in searched or not ? --> Yes, add searching task.
                pursuerC = this.agentUtility.checkTheBackupLocation(this.graph,pursuerC,this.searchedArea);
                break;
            }
            case WaitSearching:{
                this.printStateAndLocation("WaitSearching",new CXPoint(stateC.position.x, stateC.position.z));
                // 1. Check the left area is been searched or not ? --> Yes, Check the location --> It's in the top-right location? Add Searching task: state = Free;
                pursuerC = this.agentUtility.checkLeftAreaIsBeenSearched(this.graph,pursuerC,this.searchedArea,stateC);
                break;
            }
            case FinishGame:{
                pursuerC.setState(CXAgentState.Free);
                System.out.println("The game is over.");
                break;
            }
        }
        stateC.update();

    }

    private void printStateAndLocation(String state,CXPoint location){
//        CXPoint point = CXPoint.converToGraphCoordination(location);
//        System.out.println("State: " + state +  ",Current Location " + point.x + " " +point.y);
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
        moveVision(pursuerComponent, stateComponent, deltaTime);
        limitAngle(pursuerComponent);

    }
    private void followPath(PursuerComponent pC, StateComponent sC){
        if(pC.pursuerPointPath!=null && pC.pursuerPointPath.size()>0){
            CXPoint target = pC.pursuerPointPath.remove(0);
            Vector3 pos = new Vector3((float) target.x, 0, (float) target.y);
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
                pursuerComponent.direction = pursuerComponent.direction.invert(); // Clock
            }
            else if (pursuerComponent.currentAngle >= pursuerComponent.maxAngle) {
                pursuerComponent.waitTime = pursuerComponent.waitTimeMaxAngle;
                pursuerComponent.direction = pursuerComponent.direction.invert(); // Moving direction
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
        /*if (pursuer.cctvAlerted)
            pursuer.alerted = true;
        else*/
        pursuer.alerted = false;

        for (Entity target : evaders) {
            updateDetection(entity, target);
            if (pursuer.alerted)
                break;
        }

        pursuer.detectionTime = pursuer.alerted ? pursuer.detectionTime + deltaTime : 0.0f;

        if (pursuer.detectionTime > DETECTION_TIME) {
            System.out.println("INTRUDER DETECTED");
            /*Vector3 start = new Vector3(pursuer.position.x, 0, pursuer.position.z);
            Vector3 end = new Vector3(pursuer.targetPosition.x, 0, pursuer.targetPosition.y);
            p = pathFinder.findPath(start, end, null);
            //reset start node to current position of pursuer.
            if(p!=null && p.size()<0) {
                p.get(0).worldX = pursuer.position.x;
                p.get(0).worldZ = pursuer.position.z;
            }
            addAdditionalSteps(pursuer, p, start);*/
            pursuer.detectionTime = 0.0f;
        }
    }

    public static ArrayList<Vector3> addAdditionalSteps(PursuerComponent pC, List<Node> p, Vector3 Start){
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
                    Vector3 position = new Vector3(start.x+bDX, 0, start.z+bDZ);
                    pC.pursuerPath.add(position);
                }
            }
        }
        return pC.pursuerPath;
    }

    private void updateDetection(Entity entity, Entity target) {
        Vector2 targetPos = Mappers.observableMapper.get(target).position;
        PursuerComponent pursuer = Mappers.pursuerMapper.get(entity);
        EvaderComponent evader = Mappers.agentMapper.get(target);

        pursuer.alerted = false;
        pursuer.targetPosition.set(0.0f, 0.0f);

        if (visionSystem.canSee(entity,target)) {
            pursuer.alerted = true;
            pursuer.targetPosition.set(targetPos);
            //System.out.println(evader);
            evader.captured = true;
            System.out.println(evader + " is captured.");
            engine.removeEntity(target);
        }

    }


    @Override
    public void render(ModelBatch modelBatch) {
        if(coordExplorer.nodes.size()>0){
            for(int i=0;i<coordExplorer.nodes.size();i++){
                modelBatch.render(coordExplorer.nodes.get(i));
            }
        }
    }
}
