package com.dqn;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class State {
    private double taskDataSize;
    private double taskComputationRequirement;
    private double maximumToleranceDelay;
    private int mdWaitingQueueState;
    private int edgeServerLoad;
    private double bandwidthInformation;

}