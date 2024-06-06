package com.codingapi.rl4j.demo;

import lombok.Getter;
import lombok.Value;
import org.deeplearning4j.rl4j.space.Encodable;

@Value
@Getter
public class MyGameState implements Encodable {

    /**
     * 得分（奖励值）
     */
    int reward;

    /**
     * 步数
     */
    int step;

    public MyGameState(int reward, int step) {
        this.reward = reward;
        this.step = step;
    }

    @Override
    public double[] toArray() {
        //组建 游戏状态数据
        double[] ar = new double[2];
        ar[0] = reward;
        ar[1] = step;
        return ar;
    }

}
