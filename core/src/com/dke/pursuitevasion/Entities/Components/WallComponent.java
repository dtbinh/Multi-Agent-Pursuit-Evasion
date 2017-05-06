package com.dke.pursuitevasion.Entities.Components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Vector3;
import com.dke.pursuitevasion.EdgeVectors;

/**
 * Created by Nicola Gheza on 28/03/2017.
 */
public class WallComponent implements Component {
    public EdgeVectors eV;
    public boolean innerWall = false;
}
