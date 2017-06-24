package com.dke.pursuitevasion.AI.PotentialField;

/**
 * Created by imabeast on 6/24/17.
 */

public enum PFDirection {

    TOP(0), TOP_RIGHT(1), RIGHT(2), BOTTOM_RIGHT(3), DOWN(4), BOTTOM_LEFT(5), LEFT(6), TOP_LEFT(7);

    private final int TYPE;

    PFDirection(int i) {
        TYPE = i;
    }
}
