package com.dke.pursuitevasion.AI;

import com.badlogic.gdx.ai.pfa.Connection;

/**
 * Created by Envy on 4/19/2017.
 */
public class CustomConnection implements Connection{

    Node fromNode;
    Node toNode;
    float cost;

    public CustomConnection(Node fNode, Node tNode, float Cost){
        fromNode = fNode;
        toNode = tNode;
        cost = Cost;
    }

    @Override
    public float getCost(){
        return cost;
    }

    @Override
    public Node getFromNode(){
        return fromNode;
    }

    @Override
    public Node getToNode(){
        return toNode;
    }
}