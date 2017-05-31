package com.dke.pursuitevasion.Entities.Components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Vector3;

import java.util.ArrayList;

/**
 * Created by Nicola Gheza on 29/03/2017.
 */
public class AgentComponent implements Component {
    public float radius;
    public ArrayList<Vector3> evaderPath;
    public Vector3 position;
    public boolean captured = false;
}
