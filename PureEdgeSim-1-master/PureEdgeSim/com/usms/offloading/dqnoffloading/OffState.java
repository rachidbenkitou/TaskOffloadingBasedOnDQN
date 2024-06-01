package com.usms.offloading.dqnoffloading;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OffState implements Serializable {
    private double length;
    private double maxLatency;
    private double requestSize;
    private double localComputationRate;
    private double edgeComputationRate;
    private double remoteComputationRate;
    private long edgeAvailableStorage;
    private int vmId;

    public double[] toArray() {
        return new double[]{
                length, maxLatency, requestSize,
                localComputationRate, edgeComputationRate,
                remoteComputationRate, edgeAvailableStorage * 1.0, vmId * 1.0
        };
    }
}