package com.dke.pursuitevasion.Entities.Components.agents;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.dke.pursuitevasion.CXSearchingAlgorithm.CXAgentState;
import com.dke.pursuitevasion.Entities.Direction;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Created by Nicola Gheza on 23/05/2017.
 */
public class PursuerComponent implements Component {
    public float angularVelocity = 80.0f;
    public float maxAngle = 360.0f;
    public float minAngle = 0.0f;
    public float currentAngle = 0.0f;
    public float waitTimeMaxAngle = 0.5f;
    public float waitTimeMinAngle = 0.5f;
    public float waitTime = 0.0f;
    public Direction direction = Direction.COUNTERCLOCKWISE;
    public boolean patrolStarted = false;
    public boolean alerted = false;
    //public boolean cctvAlerted = false;
    public float detectionTime = 0.0f;
    public Vector2 targetPosition = new Vector2();
    public float moveTime = 0.0f;
    public Vector3 position;
    public ArrayList<Vector3> pursuerPath = new ArrayList<Vector3>();

    private CXAgentState state;
    public LinkedList taskList = new LinkedList();
    public int number;
    public int currentSearchArea;

    public void setState(CXAgentState state){
        System.out.println("Change State from "+ this.state +  " to "+ state);
        this.state = state;
    }
    public CXAgentState getState(){
        return this.state;
    }


}
