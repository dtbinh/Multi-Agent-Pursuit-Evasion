package com.dke.pursuitevasion.CXSearchingAlgorithm;

import com.dke.pursuitevasion.CXSearchingAlgorithm.CXAgentTaskType.CXAgentMessageTask;
import com.dke.pursuitevasion.CXSearchingAlgorithm.CXAgentTaskType.CXAgentMovingTask;
import com.dke.pursuitevasion.CXSearchingAlgorithm.CXAgentTaskType.CXAgentScanTask;
import com.dke.pursuitevasion.CXSearchingAlgorithm.CXAgentTaskType.CXAgentSearchTask;

/**
 * Created by chenxi on 5/20/17.
 */
public class CXAgentTask {
    public CXAgentState taskState ;
    public CXAgentMovingTask movingTask = new CXAgentMovingTask();
    public CXAgentMessageTask messageTask = new CXAgentMessageTask();
    public CXAgentSearchTask searchTask = new CXAgentSearchTask();
    public CXAgentScanTask scanTask = new CXAgentScanTask();
    public CXAgentTask(){}
    public CXAgentTask(CXAgentState state){
        this.taskState = state;
    }

}
