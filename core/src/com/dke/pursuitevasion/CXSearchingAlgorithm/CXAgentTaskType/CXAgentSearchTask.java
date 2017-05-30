package com.dke.pursuitevasion.CXSearchingAlgorithm.CXAgentTaskType;

import com.dke.pursuitevasion.CellDecompose.Graph.CXDecomposedGraphNode;
import org.omg.CORBA.PUBLIC_MEMBER;

/**
 * Created by chenxi on 5/20/17.
 */
public class CXAgentSearchTask {
    public CXDecomposedGraphNode searchArea;

    public CXAgentSearchTask(){};
    public CXAgentSearchTask(CXDecomposedGraphNode searchArea){
        this.searchArea = searchArea;
    }
}
