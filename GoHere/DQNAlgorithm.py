import random
from collections import deque

import numpy as np
from tensorflow.keras.layers import Dense
from tensorflow.keras.models import Sequential
from tensorflow.keras.optimizers import Adam
from tensorflow.keras.optimizers import RMSprop


class DeepQLearning:

    def __init__(self, gamma, epsilon, numberEpisodes):
        self.gamma = gamma
        self.epsilon = epsilon
        self.numberEpisodes = numberEpisodes

        self.times = 16
        self.learning_rate = 0.001

        self.stateDimension = 6
        self.actionDimension = 2
        self.replayBufferSize = 300
        self.batchReplayBufferSize = 100

        self.updateTargetNetworkPeriod = 100

        self.counterUpdateTargetNetwork = 0

        self.sumRewardsEpisode = []

        self.replayBuffer = deque(maxlen=self.replayBufferSize)

        self.mainNetwork = self.createNetwork()

        self.targetNetwork = self.createNetwork()

        self.targetNetwork.set_weights(self.mainNetwork.get_weights())

        self.actionsAppend = []

    # def my_loss_fn(self, y_true, y_pred, energy):
    #     s1, s2 = y_true.shape
    #     indices = np.zeros(shape=(s1, s2))
    #     indices[:, 0] = np.arange(s1)
    #     indices[:, 1] = self.actionsAppend
    #     loss = mean_squared_error(gather_nd(y_true, indices=indices.astype(int)),
    #                               gather_nd(y_pred, indices=indices.astype(int)))
    #     return loss

    def calculate_cost(tau_MD, tau_EC, E_device, E_edge, x, y, alpha=1.0, beta=1.0):
        """
        Calculate the total cost of processing a task, considering delay and energy consumption.

        Parameters:
        tau_MD (float): Delay incurred when processing the task on the mobile device.
        tau_EC (float): Delay incurred when processing the task on an edge computing resource.
        E_device (float): Energy consumed by the mobile device to process the task locally.
        E_edge (float): Energy consumed when the task is offloaded and processed by an edge computing resource.
        x (int): Binary decision variable, 1 if the task is offloaded, 0 otherwise.
        y (int): Binary decision variable, indicating which edge server the task is offloaded to.
        alpha (float): Weight for the importance of minimizing delays.
        beta (float): Weight for the importance of minimizing energy consumption.

        Returns:
        float: The total cost of processing the task.
        """
        cost = alpha * (tau_MD + tau_EC) + beta * ((1 - x) * E_device + x * y * E_edge)
        return cost

    def createNetwork(self):
        model = Sequential()
        model.add(Dense(128, input_dim=self.stateDimension, activation='relu'))
        model.add(Dense(56, activation='relu'))
        model.add(Dense(self.actionDimension, activation='linear'))
        model.compile(loss='mse', optimizer=Adam(lr=self.learning_rate), metrics=['accuracy'])

        return model

    # def trainingEpisodes(self):
    #     for indexEpisode in range(self.numberEpisodes):
    #         rewardsEpisode = []
    #
    #         print("Simulating episode {}".format(indexEpisode))
    #
    #         (currentState, _) = self.env.reset()
    #
    #         for indexTime in range(self.times):
    #             action = self.selectAction(currentState)
    #
    #             (nextState, reward, terminalState, _, _) = self.env.step(action)
    #             rewardsEpisode.append(reward)
    #
    #             self.replayBuffer.append((currentState, action, reward, nextState, terminalState))
    #
    #             self.trainNetwork()
    #
    #             currentState = nextState
    #
    #         print("Sum of rewards {}".format(np.sum(rewardsEpisode)))
    #         self.sumRewardsEpisode.append(np.sum(rewardsEpisode))

    def trainingEpisodes(self):
        for indexEpisode in range(self.numberEpisodes):
            rewardsEpisode = []

            print("Simulating episode {}".format(indexEpisode))

            # Simulate environment reset with a random state
            currentState = np.random.rand(self.stateDimension).reshape(1, -1)

            for indexTime in range(self.times):
                action = self.selectAction(currentState)

                # Simulate taking an action and getting the next state, reward, and terminal state
                nextState = np.random.rand(self.stateDimension).reshape(1, -1)  # Random next state
                reward = np.random.rand() - 0.5  # Random reward, adjusted to possibly be negative for diversity
                terminalState = np.random.choice([True, False], p=[0.1, 0.9])  # Terminal state with a small probability

                rewardsEpisode.append(reward)

                self.replayBuffer.append((currentState, action, reward, nextState, terminalState))

                self.trainNetwork()

                currentState = nextState

            print("Sum of rewards {}".format(np.sum(rewardsEpisode)))
            self.sumRewardsEpisode.append(np.sum(rewardsEpisode))

    def selectAction(self, state):
        if np.random.rand() <= self.epsilon:
            return np.random.randint(self.actionDimension)
        act_values = self.mainNetwork.predict(state)
        return np.argmax(act_values[0])

    def trainNetwork(self):
        if len(self.replayBuffer) > self.batchReplayBufferSize:
            randomSampleBatch = random.sample(self.replayBuffer, self.batchReplayBufferSize)

            # Initialize currentStateBatch and nextStateBatch with the correct shape based on self.stateDimension
            currentStateBatch = np.zeros((self.batchReplayBufferSize, self.stateDimension))
            nextStateBatch = np.zeros((self.batchReplayBufferSize, self.stateDimension))

            # Initialize arrays to hold rewards, actions, and termination status
            rewards = np.zeros(self.batchReplayBufferSize)
            actions = np.zeros(self.batchReplayBufferSize, dtype=int)
            done_flags = np.zeros(self.batchReplayBufferSize)

            for index, (currentState, action, reward, nextState, terminated) in enumerate(randomSampleBatch):
                currentStateBatch[index] = currentState
                nextStateBatch[index] = nextState
                rewards[index] = reward
                actions[index] = action
                done_flags[index] = terminated

            # Predict Q-values for currentState and nextState
            Q_current = self.mainNetwork.predict(currentStateBatch)
            Q_next = self.targetNetwork.predict(nextStateBatch)

            # Update Q-values based on the action taken
            for i in range(self.batchReplayBufferSize):
                if done_flags[i]:
                    Q_current[i, actions[i]] = rewards[i]
                else:
                    Q_current[i, actions[i]] = rewards[i] + self.gamma * np.max(Q_next[i])

            # Fit the model
            self.mainNetwork.fit(currentStateBatch, Q_current, batch_size=self.batchReplayBufferSize, verbose=0,
                                 epochs=1)

            # Update the target network, if necessary
            self.counterUpdateTargetNetwork += 1
            if self.counterUpdateTargetNetwork >= self.updateTargetNetworkPeriod:
                self.targetNetwork.set_weights(self.mainNetwork.get_weights())
                print("Target network updated!")
                self.counterUpdateTargetNetwork = 0


if __name__ == "__main__":
    dql_model = DeepQLearning(gamma=0.95, epsilon=0.1, numberEpisodes=50)
    dql_model.trainingEpisodes()
