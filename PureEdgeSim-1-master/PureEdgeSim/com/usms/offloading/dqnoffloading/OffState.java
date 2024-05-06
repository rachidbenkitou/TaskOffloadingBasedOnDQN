package com.usms.offloading.dqnoffloading;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OffState {
    private double length;
    private double maxLatency;
    private double requestSize;
    private double localComputationRate;
    private double edgeComputationRate;
    private double remoteComputationRate;
    private long edgeAvailableStorage;
    private int vmId;

    public double[] toArray() {

        double[] arr = new double[8];
        arr[0] = length;
        arr[1] = maxLatency;
        arr[2] = requestSize;
        arr[3] = this.localComputationRate;
        arr[4] = this.edgeComputationRate;
        arr[5] = this.remoteComputationRate;
        arr[6] = this.edgeAvailableStorage * 1.0;
        arr[7] = (double) this.vmId;
        return arr;
    }

}