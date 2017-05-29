package com.dke.pursuitevasion.Entities.Components.agents;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.dke.pursuitevasion.Entities.Direction;

/**
 * Created by Nicola Gheza on 23/05/2017.
 */
public class PursuerComponent implements Component {
    public float angularVelocity = 80.0f;
    public float maxAngle = 180.0f;
    public float minAngle = 0.0f;
    public float currentAngle = 0.0f;
    public float waitTimeMaxAngle = 0.5f;
    public float waitTimeMinAngle = 0.5f;
    public float waitTime = 0.0f;
    public Direction direction = Direction.COUNTERCLOCKWISE;
    public boolean patrolStarted = false;
    public boolean alerted = false;
    public float detectionTime = 0.0f;
    public Vector2 targetPosition = new Vector2();
    public float moveTime = 0.0f;
    public Vector3 position;
}
