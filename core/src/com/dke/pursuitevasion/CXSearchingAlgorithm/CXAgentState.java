package com.dke.pursuitevasion.CXSearchingAlgorithm;

/**
 * Created by chenxi on 5/20/17.
 */
public enum CXAgentState {
    Free,   // 0
    Hold,    // 1
    Searching, // 2
    FinisihSearching, // 3
    SendMessage, // 4
    Moving,  // 5
    Scanning,
    WaitBackup,
    WaitSearching,
    FinishGame;
}
