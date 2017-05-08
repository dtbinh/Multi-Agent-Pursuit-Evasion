package com.dke.pursuitevasion.Entities.Components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

/**
 * Created by Nicola Gheza on 17/04/2017.
 */
public class ObservableComponent implements Component {
    public Vector2 position = new Vector2();
    public void update(Vector3 position) {
        this.position = new Vector2(position.x, position.z);
    }

}
