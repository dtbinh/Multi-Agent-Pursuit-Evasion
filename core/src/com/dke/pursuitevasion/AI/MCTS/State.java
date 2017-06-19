package com.dke.pursuitevasion.AI.MCTS;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.dke.pursuitevasion.AI.PathFinder;
import com.dke.pursuitevasion.Entities.Components.StateComponent;
import com.dke.pursuitevasion.PolyMap;

import java.util.ArrayList;

/**
 * Created by imabeast on 6/13/17.
 */

public class State {

    /*
    call PathFinder.getNodeGrid() to get the boolean map
    use PathFinder.positionFromIndex(int x, int z, AStarPathFinder ASPathFinder) to get the world coordinates corresponding to your b-map coordinates

    NOTE: whenever the algorithm proceeds to the next stage by performing a move, the position of the bot on the bMap needs to be updated; then call generateAvailableMoves() on the new position
    */

    private Engine engine;
    private PolyMap map;
    private PathFinder pathFinder;
    private boolean[][] bMap;
    private ImmutableArray<Entity> entities;
    private ArrayList<Move> availableMoves;
    int botPosX, botPosY; // these tell you where the bot is on the boolean map

    public State(Engine e, PolyMap map) {
        this.engine = e;
        pathFinder = new PathFinder(map);
        this.bMap = pathFinder.getNodeGrid();
        this.entities = engine.getEntitiesFor(Family.all(StateComponent.class).get()); // stateComponent supposedly contains all positions of each entity, but no viewing angles
        this.availableMoves = generateAvailableMoves();
    }

    public State getCopy() {
        State copy = new State(this.engine, this.map);
        return copy;
    }

    public ArrayList<Move> getAvailableMoves() {
        return this.availableMoves;
    }

    private ArrayList<Move> generateAvailableMoves() {
        ArrayList<Move> aMoves = new ArrayList<Move>();
        // what is the current position of the bot on bMap?
        // these two variables are placeholders
        int iPos = 0;
        int jPos = 0;
        // true = free node
        if (bMap[iPos+1][jPos] == true)
            aMoves.add(new Move(iPos+1,jPos,bMap));
        if (bMap[iPos-1][jPos] == true)
            aMoves.add(new Move(iPos-1,jPos,bMap));
        if (bMap[iPos][jPos+1] == true)
            aMoves.add(new Move(iPos,jPos+1,bMap));
        if (bMap[iPos][jPos-1] == true)
            aMoves.add(new Move(iPos,jPos-1,bMap));
        // check for outOfBounds

        return aMoves;
    }

    public boolean hasWon(BotType type) {
        if (type == BotType.EVADER) {
            // check if evader hasn't been caught in x moves
        } else {
            // check if pursuer has line of sight on invader
        }
        return false;
    }

    public boolean isEqualTo(State initialState) {
        if (!(initialState.entities.equals(this.entities)))
            return false;
        return true;
    }

    public void performMove(int x, int y, BotType type) {
        Move move = new Move(x,y,bMap);
        move.translateMovement();
        // use Vector3 to change bot position
    }
}
