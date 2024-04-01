package com.usms.offloading.dqnoffloading;

import com.dqn.State;
import com.usms.offloading.dqn.learning.Agent;

public class OffMain {
    public static void main(String[] args) {
        OffDqnAgent offDqnAgent= new OffDqnAgent(0.01, 0.1, 10);
        System.out.println(offDqnAgent.chooseAction(new OffState(1,2,3,4,5,6),1222));;
    }
}
