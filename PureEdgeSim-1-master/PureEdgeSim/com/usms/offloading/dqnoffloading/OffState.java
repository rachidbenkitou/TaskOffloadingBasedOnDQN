package com.usms.offloading.dqnoffloading;

import com.mechalikh.pureedgesim.TasksGenerator.Task;
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
    private int edgeServerLoad;
    private double bandwidthInformation;

    public static OffState buildState() {
        // Example values for each state attribute
        double taskDataSize = 12;
        double taskComputationRequirement = 12;
        double maximumToleranceDelay = 12;
        int mdWaitingQueueState = 12;
        int edgeServerLoad = 12;
        double bandwidthInformation = 12;

        // Construct and return the State object
        return new OffState(taskDataSize, taskComputationRequirement, maximumToleranceDelay, mdWaitingQueueState, edgeServerLoad, bandwidthInformation);
    }

    public static void buildOffState(Task task) {
    }

    public static OffState reset() {
        OffState offState = new OffState();
        return offState;
    }

    public double[] toArray() {
        return new double[]{taskDataSize, taskComputationRequirement, maximumToleranceDelay,
                mdWaitingQueueState, edgeServerLoad, bandwidthInformation};
    }
}