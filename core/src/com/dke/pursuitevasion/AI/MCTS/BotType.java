package com.dke.pursuitevasion.AI.MCTS;

/**
 * Created by imabeast on 6/13/17.
 */

public enum BotType {

    EVADER(1), PURSUER(0);

    private final int type;

    BotType (int i) {
        type = i;
    }

    public int getType() {
        return type;
    }

    public BotType otherType(BotType currentType) {
        if (currentType == EVADER)
            return PURSUER;
        else
            return EVADER;
    }
}
