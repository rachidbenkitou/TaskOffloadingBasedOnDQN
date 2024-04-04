package com.usms.offloading.dqnoffloading;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OffState {
    private double taskDataSize;
    private double taskComputationRequirement;
    private double maximumToleranceDelay;
    private int mdWaitingQueueState;
    private double edgeServerLoad;
    private double bandwidthInformation;


    public static OffState reset() {
        OffState offState = new OffState();
        return offState;
    }

    public double[] toArray() {
        return new double[]{taskDataSize, taskComputationRequirement, maximumToleranceDelay,
                mdWaitingQueueState, edgeServerLoad, bandwidthInformation};
    }
}