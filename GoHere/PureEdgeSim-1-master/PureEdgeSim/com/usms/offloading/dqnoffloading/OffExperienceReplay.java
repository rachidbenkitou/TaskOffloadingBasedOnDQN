package com.usms.offloading.dqnoffloading;

import com.usms.offloading.dqn.learning.MyState;
import lombok.Data;

import java.io.Serializable;

@Data
public class OffExperienceReplay implements Serializable {
    private MyState currentState;
    private int action;
    private double reward;
    private MyState nextState;
    private boolean done;

    public OffExperienceReplay(MyState currentState, int action, double reward, MyState nextState, boolean done) {
        this.currentState = currentState;
        this.action = action;
        this.reward = reward;
        this.nextState = nextState;
        this.done = done;
    }

    public OffExperienceReplay(OffState theCurrentState, int action, double computedReward, OffState nextState, boolean done) {
    }
}