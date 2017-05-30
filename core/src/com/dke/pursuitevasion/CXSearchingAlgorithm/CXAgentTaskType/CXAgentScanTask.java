package com.dke.pursuitevasion.CXSearchingAlgorithm.CXAgentTaskType;

import sun.misc.Queue;

import java.util.LinkedList;

/**
 * Created by chenxi on 5/24/17.
 */
public class CXAgentScanTask {
    public LinkedList scanScope = new LinkedList();
    public CXAgentScanTask(){}
    public CXAgentScanTask(LinkedList scanScope){
        this.scanScope = scanScope;
    }

}
