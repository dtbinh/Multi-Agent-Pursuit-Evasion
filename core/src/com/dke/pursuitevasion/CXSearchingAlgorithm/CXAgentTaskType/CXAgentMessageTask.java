package com.dke.pursuitevasion.CXSearchingAlgorithm.CXAgentTaskType;

import com.dke.pursuitevasion.CXSearchingAlgorithm.CXAgentTask;
import com.dke.pursuitevasion.CXSearchingAlgorithm.CXMessage.CXMessageType;

/**
 * Created by chenxi on 5/20/17.
 */
public class CXAgentMessageTask {
    public CXAgentTask messageContent;
    /*
    messageReceiver = -1, means send to everyone.
     */
    public int messageReceiver = -1;
    public CXMessageType messageType;
    public CXAgentMessageTask(){}
    public CXAgentMessageTask(CXAgentTask messageContent,int messageReceiver){
        this.messageContent = messageContent;
        this.messageReceiver = messageReceiver;
    }
    public CXAgentMessageTask(CXAgentTask messageContent){
        this.messageContent = messageContent;
    }
}
