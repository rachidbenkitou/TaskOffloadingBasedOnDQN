package com.usms.offloading.dqnoffloading;

import com.mechalikh.pureedgesim.ScenarioManager.SimulationParameters;
import com.mechalikh.pureedgesim.TasksGenerator.Task;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
public class OffState implements Serializable {
    private double length;
    private double maxLatency;
    private double requestSize;
    private double localComputationRate;
    private static double edgeComputationRate;
    private static double remoteComputationRate;
    private static long edgeAvailableStorage;
    private int vmId;
    private double taskToleratedDelay;
    private double currentCpu;
    private long ramMemory;

    public OffState(double length, double maxLatency, double requestSize, double localComputationRate,
                         double edgeComputationRate, double remoteComputationRate, long edgeAvailableStorage,
                         int vmId, double taskToleratedDelay, double currentCpu, long ramMemory) {
        this.length = length;
        this.maxLatency = maxLatency;
        this.requestSize = requestSize;
        this.localComputationRate = localComputationRate;
        OffState.edgeComputationRate = edgeComputationRate;
        OffState.remoteComputationRate = remoteComputationRate;
        OffState.edgeAvailableStorage = edgeAvailableStorage;
        this.vmId = vmId;
        this.taskToleratedDelay = taskToleratedDelay;
        this.currentCpu = currentCpu;
        this.ramMemory = ramMemory;
    }
    public double[] toArray() {
        return new double[]{
                length, maxLatency, requestSize,
                localComputationRate, edgeComputationRate,
                remoteComputationRate, edgeAvailableStorage * 1.0, vmId * 1.0,
                taskToleratedDelay, currentCpu, ramMemory
        };
    }


    static   OffState buildState(Task task, long defaultVmId) {
        long taskLength = task.getLength();
        double permittedDelay = task.getMaxLatency();
        int app = task.getApplicationID();
        long requestSize = SimulationParameters.APPLICATIONS_LIST.get(app).getRequestSize();
        double localComputationRate = task.getEdgeDevice().getHost(0).getTotalMipsCapacity();

        double taskToleratedDelay = task.getMaxLatency();
        double currentCpu = task.getEdgeDevice().getHost(0).getTotalMipsCapacity();
        long ramMemory = task.getEdgeDevice().getResources().getRam();
        return new OffState(taskLength, permittedDelay, requestSize, localComputationRate, edgeComputationRate, remoteComputationRate, edgeAvailableStorage, (int) defaultVmId, taskToleratedDelay, currentCpu, ramMemory);
    }
}