package com.dke.pursuitevasion.Entities.Systems.AI;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.dke.pursuitevasion.AI.MCTS.MCTS;
import com.dke.pursuitevasion.Entities.Components.agents.EvaderComponent;
import com.dke.pursuitevasion.PolyMap;

/**
 * Created by Nicola Gheza on 16/06/2017.
 */
public class MCSystem extends IteratingSystem {

    private Engine engine;
    private PolyMap map;

    private MCTS mcts;

    public MCSystem(Engine engine, PolyMap map) {
        super(Family.all(EvaderComponent.class).get());
        this.engine = engine;
        this.map = map;

        initMCTS();
    }

    private void initMCTS() {
        mcts = new MCTS(engine, map, 1000, 6);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {

    }
}
