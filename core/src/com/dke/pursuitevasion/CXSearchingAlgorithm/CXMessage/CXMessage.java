package com.dke.pursuitevasion.CXSearchingAlgorithm.CXMessage;

import com.dke.pursuitevasion.CXSearchingAlgorithm.CXAgentTask;

/**
 * Created by chenxi on 5/20/17.
 */
public class CXMessage {
    public int sender;
    public int receiver = -1;
    public CXMessageType messageType;
    public CXAgentTask messageContent;
    public CXMessage(){};
}
