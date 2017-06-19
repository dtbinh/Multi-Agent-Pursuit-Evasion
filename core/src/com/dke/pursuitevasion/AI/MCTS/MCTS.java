package com.dke.pursuitevasion.AI.MCTS;

import com.badlogic.ashley.core.Engine;
import com.dke.pursuitevasion.PolyMap;

import java.util.ArrayList;

/**
 * Created by imabeast on 6/13/17.
 */

public class MCTS {

    int maxTime, depthLevel;
    State initialState;
    BotType type;
    BotType enemy;
    TreeNode root;
    TreeNode lastNode;
    final double a = 0.85;

    /*

    To do:
    - apply depth limit / keep track of moves during computation
    - keep track of bot position on boolean map
    - check if a state is a winning state (vision vs touch?)

    MCTS loop:

        Selection
        Expansion
        Simulation
        Backpropagation

    call chain:

        start()
            '- setNewRoot() if a previous root exists
               selection() - called inside expand()
               expand()
                    '- simulate() - if current node and child node don't contain a winning state
               selectBestMove()

    */

    public MCTS(Engine engine, PolyMap map, int maxTime, int depthLevel){

        this.maxTime = maxTime;
        this.depthLevel = depthLevel;
        this.type = BotType.EVADER;
        enemy = BotType.PURSUER;
        State state = new State(engine,map);
        this.initialState = state;
    }

    public Move start(){

        // if there is no root, create a new node with the initial game state and the enemy as the current player
        if (root == null)
            root = new TreeNode(enemy, initialState);
        else
            setNewRoot();

        int count = 0;

        // performs expansion until time limit is reached
        double startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime <= maxTime){
            expand(selection(root));
            count++;
        }

        printTree(root);
        System.out.println(count);
        TreeNode node = selectBestMoveNode(root);
        lastNode = node;
        return node.getMove();
    }

    // this is called from start() when root != null
    public void setNewRoot(){

        State copy = lastNode.getState().getCopy();
        for(TreeNode treeNode : lastNode.getChildren()){ // lastNode is the one that was chosen as the best option for the next move
            // perform treeNode's move for enemy in copy HERE
            if(copy.isEqualTo(initialState)){
                root = treeNode;
                root.setParent(null);
                return;
            }
            // undo previous move HERE
        }
    }

    private TreeNode selectBestMoveNode(TreeNode root){

        int nWins = root.getChildren().get(0).getTotalWins(); // set highest number of wins to the first child's number, then check for better options
        TreeNode bestMove = root.getChildren().get(0);

        for (TreeNode nodeTree: root.getChildren()){
            if(nodeTree.getTotalWins() > nWins){
                nWins = nodeTree.getTotalWins();
                bestMove = nodeTree;
            }
        }
        return bestMove;
    }

    public TreeNode selection(TreeNode node){

        if(node.getState().getAvailableMoves().size() > 0)
            return node;

        double score = -1000000;
        TreeNode result = node;

        // selects node with highest UCT score among children of "node"
        for (TreeNode nodeChild: node.getChildren()){
            double newScore = calcUCT(nodeChild);
            if(newScore > score){
                score = newScore;
                result = nodeChild;
            }
        }
        return selection(result); // calls itself recursively until a leaf node is reached
    }

    public void printTree(TreeNode root){
        printChild(root);
        System.out.println("Root: " + root.getTotalGames());
    }

    public void printChild(TreeNode node){
        for(TreeNode treeNode: node.getChildren()) {
            for (int i = 0; i < treeNode.getDepth(); i++) {
                System.out.print(" -- ");
            }
            System.out.println(" WINS/TOTAL GAMES: " + treeNode.getTotalWins() + "/" + treeNode.getTotalGames());
            if(treeNode.getChildren().size() > 0){
                printChild(treeNode);
            }
        }
    }

    public void expand(TreeNode node){

        if(node.isWinningState()) {
            if (node.getType() == type)
                node.incrementWins(10);
            else
                node.incrementWins(-100);
        }else{
            TreeNode child = new TreeNode(node);
            ArrayList<Move> freeMoves = node.getState().getAvailableMoves();
            child.setMove(freeMoves.remove((int)(Math.random()*freeMoves.size())));
            child.incrementGames();

            if(child.isWinningState()) {
                if (child.getType() == type)
                    child.incrementWins(10);
                else
                    child.incrementWins(-100);
            }else {
                simulate(child);
            }
        }
    }

    public void simulate(TreeNode nodeTree){

        State copy = nodeTree.getState().getCopy();
        BotType startType;
        if (nodeTree.getType() == BotType.PURSUER)
            startType = BotType.EVADER;
        else
            startType = BotType.PURSUER;

        /*
        Every bot should have an individual set of available moves
         */
        ArrayList<Move> availableMoves = copy.getAvailableMoves();
        int size = availableMoves.size();

        for (int i = 0; i < size/2; i++){
            Move move = availableMoves.remove((int)(Math.random()*availableMoves.size())); // removes a random move from remaining available moves
            // perform a move / state change for the startType bot HERE
        }

        if (startType == BotType.PURSUER)
            startType = BotType.EVADER;
        else
            startType = BotType.PURSUER;

        size = availableMoves.size();
        for (int i = 0; i < size; i++){
            Move move = availableMoves.get(i);
            // perform a move / state change for the startType bot HERE
        }

        if(copy.hasWon(type)) // get game over info from Engine, refactor if necessary
            nodeTree.incrementWins(1);
        else
            nodeTree.incrementWins(-1);

    }

    // upper confidence bounds applied to trees (UCT)
    public double calcUCT(TreeNode node){

        float winRatio = node.getTotalWins()/node.getTotalGames();
        int currentTotalGames = node.getTotalGames();
        int parentTotalGames = node.getParent().getTotalGames();
        double c = Math.sqrt(2);
        if(winRatio > a) // a = 0.85
            c = 0; // when c = 0 the algorithm gathers samples greedily
        return winRatio + c * Math.sqrt(Math.log(currentTotalGames)/parentTotalGames);
    }

    public Move getMove() {
        return start();
    }
}

