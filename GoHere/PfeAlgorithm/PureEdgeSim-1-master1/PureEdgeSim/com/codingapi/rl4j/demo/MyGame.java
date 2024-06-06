package com.codingapi.rl4j.demo;

import lombok.extern.slf4j.Slf4j;
import org.deeplearning4j.gym.StepReply;
import org.deeplearning4j.rl4j.mdp.MDP;
import org.deeplearning4j.rl4j.space.ArrayObservationSpace;
import org.deeplearning4j.rl4j.space.DiscreteSpace;
import org.deeplearning4j.rl4j.space.ObservationSpace;

/**
 * 游戏控制的对象
 */
@Slf4j
public class MyGame implements MDP<MyGameState, Integer, DiscreteSpace> {


    /**
     * 最大分数
     */
    final private int maxReward;

    /**
     * 游戏的状态信息
     */
    private MyGameState mySimpleState;

    /**
     * 方向 left right
     */
    private DiscreteSpace actionSpace = new DiscreteSpace(2);

    /**
     * 对应 MyGameState 的存储空间数据
     */
    private ObservationSpace<MyGameState> observationSpace = new ArrayObservationSpace(new int[]{2});


    public MyGame(int maxReward) {
        this.maxReward = maxReward;
    }


    @Override
    public ObservationSpace<MyGameState> getObservationSpace() {
        return observationSpace;
    }

    @Override
    public DiscreteSpace getActionSpace() {
        return actionSpace;
    }

    @Override
    public MyGameState reset() {
        //重置参数
        return mySimpleState = new MyGameState(0, 0);
    }


    @Override
    public void close() {
        //释放游戏参数
    }

    @Override
    public StepReply<MyGameState> step(Integer action) {
        //游戏执行一步 action 为动作 0:left 1:right
        int reward = (action == 0 ? -1 : 1);

        //游戏的状态
        mySimpleState = new MyGameState(mySimpleState.getReward() + reward, mySimpleState.getStep() + 1);

        //返回操作响应信息
        return new StepReply<>(mySimpleState, reward, isDone(), null);
    }

    @Override
    public boolean isDone() {
        boolean res = mySimpleState.getReward() == maxReward;

        //当结束时，打印总步数与得分.
        if (res) {
            log.info("step->{},reward->{}", mySimpleState.getStep(), mySimpleState.getReward());
        }
        return res;
    }


    @Override
    public MDP<MyGameState, Integer, DiscreteSpace> newInstance() {
        return new MyGame(maxReward);
    }
}
