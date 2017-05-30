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
import com.dke.pursuitevasion.CXSearchingAlgorithm.CXAgentState;
import com.dke.pursuitevasion.CXSearchingAlgorithm.CXAgentTask;
import com.dke.pursuitevasion.CXSearchingAlgorithm.CXAgentTaskType.CXAgentMovingTask;
import com.dke.pursuitevasion.CXSearchingAlgorithm.CXAgentUtility;
import com.dke.pursuitevasion.CXSearchingAlgorithm.CXMessage.CXMessage;
import com.dke.pursuitevasion.CellDecompose.Graph.*;
import com.dke.pursuitevasion.Entities.Components.ObservableComponent;
import com.dke.pursuitevasion.Entities.Components.ObserverComponent;
import com.dke.pursuitevasion.Entities.Components.StateComponent;
import com.dke.pursuitevasion.Entities.Components.agents.PursuerComponent;
import com.dke.pursuitevasion.Entities.Mappers;
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
public class PursuerSystem extends IteratingSystem {
    private static final float DETECTION_TIME = 0.0f;

    private ImmutableArray<Entity> evaders;
    private VisionSystem visionSystem;
    private Vector2 position = new Vector2();

    private PathFinder pathFinder;
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

    public PursuerSystem(VisionSystem visionSystem, CXGraph graph, PolyMap map) {
        super(Family.all(PursuerComponent.class).get());

        this.visionSystem = visionSystem;
        this.graph = graph;
        pathFinder = new PathFinder(map);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        if (!runOnce) {
            assignInitialTask(entity);
        } else {
            updateTask(entity);
        }
        //movePursuer(entity, deltaTime);
        updateObserver(entity);
        updateDetection(entity, deltaTime);
    }

    @Override
    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);
        evaders = engine.getEntitiesFor(Family.all(ObservableComponent.class).get());
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
        task2.scanTask.scanScope.add((Float)45.0f);
        task2.scanTask.scanScope.add((Float)90.0f);

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
                    break;
                }
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
                this.printStateAndLocation("Moving",new CXPoint(stateC.position.x, stateC.position.z));
                CXAgentTask task = (CXAgentTask) pursuerC.taskList.get(0);
                CXPoint destination = task.movingTask.movingDestination;

                // Get the update location
                float velocity = stateC.velocity.x;
                CXPoint updateLocation = this.agentUtility.getNextMovingPoint(destination,destination,velocity);

                CXPoint newDestination = CXPoint.converToGraphCoordination(destination);
                CXPoint newUpdateLocation = CXPoint.converToGraphCoordination(updateLocation);
                System.out.println("Moving destination x = "+ newDestination.x + " y = "+newDestination.y + " ; UpdateLocation is x = "+ newUpdateLocation.x + " y = " + newUpdateLocation.y);

                stateC.position = stateC.position.set((float)updateLocation.x,stateC.position.y,(float)updateLocation.y);
                if (updateLocation == destination){
                    // Check the angle is correct
                    if (task.movingTask.radius != -1.0 && stateC.angle != task.movingTask.radius){
                        stateC.angle = task.movingTask.radius;
                    }
                    else {
                        pursuerC.taskList.removeFirst();

                        System.out.println("Agent " + pursuerC.number + " is arrived Destination ");
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
                float value =  this.agentUtility.getTheNextScanPosition(pursuerC);
                System.out.println("Current Radius is " + stateC.angle + " TargetRadius is "+ value);
                stateC.angle = value;
                pursuerC.currentAngle = value; // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

                CXAgentTask task = (CXAgentTask) pursuerC.taskList.getFirst();
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
                pursuerC   = this.agentUtility.checkLeftAreaIsBeenSearched(this.graph,pursuerC,this.searchedArea,stateC);
                break;
            }
            case FinishGame:{
                pursuerC.setState(CXAgentState.Free);
                break;
            }
        }
        stateC.update();

    }

    private void printStateAndLocation(String state,CXPoint location){
        CXPoint point = CXPoint.converToGraphCoordination(location);
        System.out.println("State: " + state +  ",Current Location " + point.x + " " +point.y);
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

        pursuer.alerted = false;
        pursuer.targetPosition.set(0.0f, 0.0f);

        if (visionSystem.canSee(entity,target)) {
            pursuer.alerted = true;
            pursuer.targetPosition.set(targetPos);
        }
    }


}
