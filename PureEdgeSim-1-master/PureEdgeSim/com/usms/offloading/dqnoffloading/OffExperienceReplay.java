package com.usms.offloading.dqnoffloading;

import lombok.Data;

@Data
public class OffExperienceReplay {
    private OffState currentState;
    private int action;
    private double reward;
    private OffState nextState;
    private boolean done;

    public OffExperienceReplay(OffState currentState, int action, double reward, OffState nextState, boolean done) {
        this.currentState = currentState;
        this.action = action;
        this.reward = reward;
        this.nextState = nextState;
        this.done = done;
    }
}
