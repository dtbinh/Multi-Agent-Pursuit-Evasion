package com.dke.pursuitevasion.AI.MCTS;

import java.util.*;

/**
 * Created by imabeast on 6/13/17.
 */

public class TreeNode {
    private State state;
    private Move move;
    private BotType type;
    private TreeNode parent;
    private ArrayList<TreeNode> children;
    private int totalGames, totalWins;
    private boolean winningState;

    public TreeNode(BotType type, State state){
        parent = null;
        this.type = type;
        this.state = state;
        children = new ArrayList<TreeNode>();
    }

    public TreeNode(TreeNode parent){
        this.parent = parent;
        this.state = parent.getState();
        parent.addChildren(this);
        if(parent.getType() == BotType.PURSUER)
            type = BotType.EVADER;
        else
            type = BotType.PURSUER;
        children = new ArrayList<TreeNode>();
    }

    public void setMove(Move move){
        this.move = move;
        this.state = parent.getState().getCopy();
        // call a method on the current state that changes the positions and directions of the bots HERE
        checkWinningState();
    }

    public State getState(){
        return state;
    }

    public Move getMove() {
        return move;
    }

    public TreeNode getParent() {
        return parent;
    }

    public ArrayList<TreeNode> getChildren() {
        return children;
    }

    public void addChildren(TreeNode node){
        children.add(node);
    }

    public int getTotalGames() {
        return totalGames;
    }

    public int getTotalWins() {
        return totalWins;
    }

    public BotType getType(){
        return type;
    }

    public void incrementGames(){
        totalGames++;
        if(parent!=null) // update total games along the branch
            parent.incrementGames();
    }

    public void incrementWins(int wins){
        totalWins+=wins;
        if(parent!=null) // update total wins along the branch
            parent.incrementWins(wins);
    }

    public boolean isWinningState(){
        return winningState;
    }

    public void checkWinningState(){
        if(state.hasWon(type))
            winningState = true;
    }

    public int getDepth(){
        TreeNode p = parent;
        int count = 0;
        while (p != null) {
            count++;
            p = p.parent;
        }
        return count;
    }

    public String toString(){
        String s = type.toString() + " " ;
        if (move != null) s += move.toString();
        s += " " + "Depth: " + getDepth() + " Height: " + getHeight() + " Value: " + totalWins + " Games: " + totalGames + " Parent: ";
        if (parent != null && parent.move!=null)
            s += parent.move.toString();
        return s;
    }

    public boolean isLeaf() {
        if (children.size() == 0)return true;
        return false;
    }

    public void setParent(TreeNode parent){
        this.parent = parent;
    }

    public int getHeight(){
        if (isLeaf()) return 0;
        int maxHeight = -1;
        for (int i = 0; i < children.size(); i++) {
            int childHeight = children.get(i).getHeight();
            if (childHeight>maxHeight) maxHeight = childHeight;
        }

        maxHeight += 1;
        return maxHeight;
    }

}
