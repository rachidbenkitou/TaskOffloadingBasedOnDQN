package com.usms.offloading.offdqn;

import com.usms.offloading.offdqn.OffState;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class OffExperienceReplay implements Serializable {
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