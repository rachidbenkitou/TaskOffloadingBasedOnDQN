package com.dqn;

import lombok.Data;

@Data
class Experience {
    private State currentState;
    private Action action;
    private double reward;
    private State nextState;
    private boolean done;

    public Experience(State currentState, Action action, double reward, State nextState, boolean done) {
        this.currentState = currentState;
        this.action = action;
        this.reward = reward;
        this.nextState = nextState;
        this.done = done;
    }
}
