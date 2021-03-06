package com.dke.pursuitevasion.Entities.Components.agents;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.dke.pursuitevasion.AI.PotentialField.PFDirection;
import com.dke.pursuitevasion.AI.PotentialField.Triplet;
import com.dke.pursuitevasion.CellDecompose.Graph.CXPoint;
import com.dke.pursuitevasion.Entities.Direction;

import java.util.ArrayList;

/**
 * Created by Nicola Gheza on 29/03/2017.
 */
public class EvaderComponent implements Component {
    public float radius;
    public Vector3 position;
    public PFDirection pfDirection;
    public Triplet previousPosition;
    public boolean first = true;
    public boolean captured = false;
    public ArrayList<CXPoint> evaderPath;
    public boolean alerted = false;
    public float detectionTime = 0.0f;
    public float angularVelocity = 80.0f;
    public float maxAngle = 360.0f;
    public float minAngle = 0.0f;
    public float currentAngle = 0.0f;
    public float waitTimeMaxAngle = 0.5f;
    public float waitTimeMinAngle = 0.5f;
    public float waitTime = 0.0f;
    public Direction direction = Direction.COUNTERCLOCKWISE;
    public Vector2 targetPosition = new Vector2();
}
