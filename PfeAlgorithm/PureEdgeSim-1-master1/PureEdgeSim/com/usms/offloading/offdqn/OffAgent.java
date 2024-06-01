package com.usms.offloading.offdqn;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Data
@AllArgsConstructor
public class OffAgent {
    private static final double LEARNING_RATE = 0.1;
    private static final double DISCOUNT_FACTOR = 0.9;
    private static final double EXPLORATION_RATE = 0.1;
    public static final long EPOCH = 2;
    private final Random random;
    private final Map<OffStateAction, Double> qTable;

    public OffAgent(Map<OffStateAction, Double> qTable) {
        this.random = new Random();
        this.qTable = (qTable != null) ? new HashMap<>(qTable) : new HashMap<>();
    }

    public OffAction chooseAction(OffState state) {
        if (random.nextDouble() < EXPLORATION_RATE) {
            // Explore: choose a random action
            OffAction offAction = OffState.getRandomAction();
            return offAction;
        } else {
            // Exploit: choose the action with the highest Q-value

            OffAction offAction = getBestAction(state);
            return getBestAction(state);
        }
    }

    public OffAction getBestAction(OffState state) {
        OffAction bestAction = null;
        double maxQValue = Double.NEGATIVE_INFINITY;
        for (OffAction action : OffAction.values()) {
            double qValue = getQValue(state, action);
            if (qValue > maxQValue) {
                maxQValue = qValue;
                bestAction = action;
            }
        }

        return bestAction;
    }

    public double getQValue(OffState state, OffAction action) {
        OffStateAction pair = new OffStateAction(state, action);
        return qTable.getOrDefault(pair, 0.0);
    }

    public void updateQValue(OffState state, OffAction action, double newValue) {
        OffStateAction pair = new OffStateAction(state, action);
        qTable.put(pair, newValue);
    }

    public void learn(OffState currentState, OffAction action, double reward, OffState nextState) {
        double currentQ = getQValue(currentState, action);
        double maxNextQ = getMaxQValue(nextState);
        double newQ = currentQ + LEARNING_RATE * (reward + DISCOUNT_FACTOR * maxNextQ - currentQ);
        updateQValue(currentState, action, newQ);
    }

    public double getMaxQValue(OffState state) {
        double maxQValue = Double.NEGATIVE_INFINITY;
        for (OffAction action : state.getPossibleActions()) {
            double qValue = getQValue(state, action);
            if (qValue > maxQValue) {
                maxQValue = qValue;
            }
        }
        return maxQValue;
    }
}
