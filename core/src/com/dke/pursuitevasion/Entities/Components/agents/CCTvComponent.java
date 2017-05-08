package com.dke.pursuitevasion.Entities.Components.agents;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Vector2;
import com.dke.pursuitevasion.Entities.Direction;

/**
 * Created by Nicola Gheza on 08/05/2017.
 */
public class CCTvComponent implements Component {
    public float angularVelocity = 40.0f;
    public float maxAngle = 360.0f;
    public float minAngle = 0.0f;
    public float currentAngle = 0.0f;
    public float waitTimeMaxAngle = 0.5f;
    public float waitTimeMinAngle = 0.5f;
    public float waitTime = 0.0f;
    public Direction direction = Direction.COUNTERCLOCKWISE;
    public boolean patrolStarted = false;
    public boolean alerted = false;
    public float detectionTime = 0.0f;
    public boolean intruderReported = false;
    public Vector2 targetPosition = new Vector2();
}
