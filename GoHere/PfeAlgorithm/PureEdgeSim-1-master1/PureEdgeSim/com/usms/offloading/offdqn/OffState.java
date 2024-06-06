package com.usms.offloading.offdqn;

import com.mechalikh.pureedgesim.ScenarioManager.SimulationParameters;
import com.mechalikh.pureedgesim.TasksGenerator.Task;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Random;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OffState {
    private long taskSize;
    private double taskToleratedDelay;
    private double currentCpu;
    private long ramMemory;
    private long availableStorageMemory;
    private double requestSize;
    private double localComputationRate;
    private double edgeComputationRate;

    public static OffAction getRandomAction() {
        int r = new Random().nextInt(2);
        OffAction[] actions = OffAction.values();
        return actions[r];
    }

    public OffAction[] getPossibleActions() {
        return OffAction.values();
    }

    public static OffState buildState(Task task, double edgeComputationRate) {
        long taskSize = task.getLength();
        double taskToleratedDelay = task.getMaxLatency();
        double currentCpu = task.getEdgeDevice().getHost(0).getTotalMipsCapacity();
        long ramMemory = task.getEdgeDevice().getResources().getRam();
        long availableStorageMemory = task.getEdgeDevice().getResources().getAvailableStorage();

        double localComputationRate = task.getEdgeDevice().getHost(0).getTotalMipsCapacity();
        int app = task.getApplicationID();
        long requestSize = SimulationParameters.APPLICATIONS_LIST.get(app).getRequestSize();

        return new OffState(taskSize, taskToleratedDelay, currentCpu, ramMemory, availableStorageMemory, requestSize, localComputationRate, edgeComputationRate);
    }

    public Object[] toArray() {
        return new Object[]{
                taskSize,
                taskToleratedDelay,
                currentCpu,
                ramMemory,
                availableStorageMemory,
                requestSize,
                localComputationRate,
                edgeComputationRate
        };
    }

}