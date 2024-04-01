package com.usms.offloading.dqn.learning;

import com.mechalikh.pureedgesim.TasksGenerator.Task;
import org.cloudbus.cloudsim.datacenters.Datacenter;

import java.util.*;

public class Agent {
    private static final double learningRate = 0.1;
    private static final double discountFactor = 0.9;
    private static final double explorationRate = 0.1;
    public static final long epoch = 2;
    private final Random random;
    private final Map<MyStateAction, Double> qTable;

    public Map<MyStateAction, Double> getqTable() {
        return qTable;
    }


    public Agent(Map<MyStateAction, Double> qTable) {
        this.random = new Random();
        this.qTable = new HashMap<>();
    }

    public MyActionSpace chooseAction(MyState state) {
        if (random.nextDouble() < explorationRate) {
            // Explore: choose a random action
            return state.getRandomAction();
        } else {
            // Exploit: choose the action with the highest Q-value
            return getBestAction(state);
        }
    }

    public MyActionSpace getBestAction(MyState state) {
        MyActionSpace bestAction = null;
        double maxQValue = Double.NEGATIVE_INFINITY;
        for (MyActionSpace action : MyActionSpace.values()) {
            double qValue = getQValue(state, action);
            if (qValue > maxQValue) {
                maxQValue = qValue;
                bestAction = action;
            }
        }
        System.out.println("Max Q value : " + maxQValue);
        return bestAction;
    }

    public double getQValue(MyState state, MyActionSpace action) {
        MyStateAction pair = new MyStateAction(state, action);
        return qTable.getOrDefault(pair, 0.0);
    }

    public void updateQValue(MyState state, MyActionSpace action, double newValue) {
        MyStateAction pair = new MyStateAction(state, action);
        qTable.put(pair, newValue);
    }

    public void learn(MyState currentState, MyActionSpace action, double reward, MyState nextState) {
        double currentQ = getQValue(currentState, action);
        double maxNextQ = getMaxQValue(nextState);
        double newQ = currentQ + learningRate * (reward + discountFactor * maxNextQ - currentQ);
        updateQValue(currentState, action, newQ);
    }

    public double getMaxQValue(MyState state) {
        double maxQValue = Double.NEGATIVE_INFINITY;
        for (MyActionSpace action : state.getPossibleActions()) {
            double qValue = getQValue(state, action);
            if (qValue > maxQValue) {
                maxQValue = qValue;
            }
        }
        return maxQValue;
    }

}
