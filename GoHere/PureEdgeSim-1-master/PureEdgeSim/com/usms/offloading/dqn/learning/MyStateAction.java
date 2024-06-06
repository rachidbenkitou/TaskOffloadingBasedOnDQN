package com.usms.offloading.dqn.learning;

import lombok.Getter;
import lombok.Setter;
import lombok.Value;

@Value
@Getter
@Setter
public class MyStateAction {
    private MyState state;
    private MyActionSpace action;




    @Override
    public int hashCode() {
        return (int) (state.getRequestSize()+ state.getLength()+ action.getActionCode()[1]) + state.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        MyStateAction other = (MyStateAction) obj;
        return state.equals(other.state) && action.equals(other.action);
    }
}
