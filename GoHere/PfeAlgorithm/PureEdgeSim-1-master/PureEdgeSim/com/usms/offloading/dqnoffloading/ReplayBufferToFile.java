package com.usms.offloading.dqnoffloading;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Deque;

public class ReplayBufferToFile implements Serializable {
    public void saveReplayBufferToFile(String filePath, Deque<OffExperienceReplay> replayBuffer ) {
        try (ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(filePath))) {
            outputStream.writeObject(replayBuffer);
            System.out.println("Replay buffer saved to file: " + filePath);
        } catch (IOException e) {
            System.err.println("Error occurred while saving replay buffer to file: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
