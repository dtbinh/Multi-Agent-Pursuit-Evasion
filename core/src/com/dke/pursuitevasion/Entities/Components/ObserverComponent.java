package com.dke.pursuitevasion.Entities.Components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

/**
 * Created by Nicola Gheza on 17/04/2017.
 */
public class ObserverComponent implements Component {
    public Vector2 position = new Vector2();
    public float angle = 0.0f;
    public float distance = 2.0f;
    public float fovAngle = 45.0f;

    public void update(Vector3 position) {
        this.position = new Vector2(position.x, position.z);
    }
}
