package com.dke.pursuitevasion.Entities.Systems.agents;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Path;
import com.badlogic.gdx.math.Vector3;
import com.dke.pursuitevasion.AI.CustomPoint;
import com.dke.pursuitevasion.AI.Node;
import com.dke.pursuitevasion.AI.PathFinder;
import com.dke.pursuitevasion.Entities.Components.AgentComponent;
import com.dke.pursuitevasion.Entities.Components.StateComponent;
import com.dke.pursuitevasion.Entities.Mappers;
import com.dke.pursuitevasion.PolyMap;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by Envy on 5/30/2017.
 */
public class EvaderSystem extends IteratingSystem {
    PathFinder pathFinder;
    public EvaderSystem(PolyMap map) {
        super(Family.all(AgentComponent.class).get());
        pathFinder = new PathFinder(map);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        AgentComponent evaderComponent = Mappers.agentMapper.get(entity);
        StateComponent stateComponent = Mappers.stateMapper.get(entity);
        if(evaderComponent.evaderPath==null||evaderComponent.evaderPath.size()==0){
            System.out.println(evaderComponent.position+"    ev pos");
            createPath(evaderComponent, evaderComponent.position);
        }
        if (!evaderComponent.captured) {
            moveEvaders(evaderComponent, stateComponent);
        }
    }

    private void createPath(AgentComponent evader, Vector3 lastPosition){
        System.out.println("damn");
        boolean[][] nodeGrid = pathFinder.getNodeGrid();
        int width = pathFinder.width;
        Random rand = new Random();
        int counter = 0;
        List<Node> p = null;

        while (counter<1){
            int  n = rand.nextInt(width-1) + 1;
            int  m = rand.nextInt(width-1) + 1;
            if(nodeGrid[n][m]){
                CustomPoint endCP = new CustomPoint(n, m);
                Vector3 end = new Vector3(PathFinder.toWorldCoorX(endCP.x), 0, PathFinder.toWorldCoorX(endCP.y));
                p = pathFinder.findPath(lastPosition, end, endCP);
                counter++;
            }
        }
        addAdditionalSteps(evader, p);
    }

    private void moveEvaders(AgentComponent evader, StateComponent stateComponent){
        if(evader.evaderPath!=null && evader.evaderPath.size()>0){
            Vector3 pos = evader.evaderPath.remove(0);
            evader.position = pos;
            stateComponent.position = pos;
            stateComponent.update();
        }

    }
    public static ArrayList<Vector3> addAdditionalSteps(AgentComponent eC, List<Node> p){
        eC.evaderPath = new ArrayList<Vector3>();
        p.size();
        float stepSize = 15;
        if(p.size()>1) {
            for (int i = 0; i < p.size() - 1; i++) {
                Vector3 start = new Vector3(p.get(i).worldX, p.get(i).worldY, p.get(i).worldZ);
                Vector3 end = new Vector3(p.get(i + 1).worldX, p.get(i + 1).worldY, p.get(i + 1).worldZ);
                float adjX = end.x - start.x;
                float adjZ = end.z - start.z;
                for (float j = 1; j < stepSize; j++) {
                    double scale = j/stepSize;
                    BigDecimal sc = BigDecimal.valueOf(scale);
                    BigDecimal xX = BigDecimal.valueOf(adjX);
                    BigDecimal zZ = BigDecimal.valueOf(adjZ);
                    BigDecimal newX = sc.multiply(xX);
                    BigDecimal newZ = sc.multiply(zZ);
                    float bDX = newX.floatValue();
                    float bDZ = newZ.floatValue();
                    Vector3 position = new Vector3(start.x+bDX, 0, start.z+bDZ);
                    eC.evaderPath.add(position);
                }
            }
        }
        return eC.evaderPath;
    }
}
