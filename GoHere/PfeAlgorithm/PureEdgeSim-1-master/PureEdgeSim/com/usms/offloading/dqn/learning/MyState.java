package com.usms.offloading.dqn.learning;

import com.mechalikh.pureedgesim.TasksGenerator.Task;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.Value;
import org.deeplearning4j.rl4j.space.Encodable;

import java.util.Random;

@Value
@Getter

public class MyState implements Encodable {
    private double length;
    private double maxLatency;
    private double requestSize;
    private double localComputationRate;
    private double edgeComputationRate;
    private double remoteComputationRate;
    private long edgeAvailableStorage;

    private int vmId;

    @Override
    public double[] toArray() {

        double[] arr = new double[8];
        arr[0] = length;
        arr[1] = maxLatency;
        arr[2] = requestSize;
        arr[3] = this.localComputationRate;
        arr[4] = this.edgeComputationRate;
        arr[5] = this.remoteComputationRate;
        arr[6] = this.edgeAvailableStorage * 1.0;
        arr[7] = (double)this.vmId;
        return arr;
    }


    public static MyActionSpace getRandomAction() {
        int r = new Random().nextInt(5);
        MyActionSpace[] action = MyActionSpace.values();
        return action[r];
    }


    public MyActionSpace[] getPossibleActions() {
        return MyActionSpace.values();

    }


}
