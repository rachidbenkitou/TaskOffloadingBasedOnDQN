import numpy as np
from tensorflow.keras.layers import Dense
from tensorflow.keras.models import Sequential
from tensorflow.keras.optimizers import Adam


class DeepQLearningAgent:
    def __init__(self, state_size, action_size, learning_rate=0.001, gamma=0.95, update_target_network_period=10,
                 batch_size=32):
        self.state_size = state_size
        self.action_size = action_size
        self.memory = []
        self.gamma = gamma  # discount rate
        self.learning_rate = learning_rate
        self.epsilon = 1.0  # exploration rate
        self.epsilon_min = 0.01
        self.epsilon_decay = 0.995
        self.update_target_network_period = update_target_network_period
        self.batch_size = batch_size
        self.main_network = self._build_model()
        self.target_network = self._build_model()
        self.update_target_network_counter = 0
        self.update_target_network()

    def _build_model(self):
        """Builds a neural network."""

        model = Sequential()
        model.add(Dense(24, input_dim=self.state_size, activation='relu'))
        model.add(Dense(24, activation='relu'))
        model.add(Dense(self.action_size, activation='linear'))
        model.compile(loss='mse', optimizer=Adam(lr=self.learning_rate))
        return model

    def update_target_network(self):
        """Updates the target network weights."""
        self.target_network.set_weights(self.main_network.get_weights())

    def remember(self, state, action, reward, next_state, done):
        """Stores experiences in memory."""
        self.memory.append((state, action, reward, next_state, done))

    def train(self):
        """Trains the network on a batch of experiences."""
        for state, action, reward, next_state, done in self.memory:  # Corrected this line
            target = self.main_network.predict(np.array([state]))
            if done:
                target[0][action] = reward
            else:
                t = self.target_network.predict(np.array([next_state]))[0]
                target[0][action] = reward + self.gamma * np.amax(t)
            self.main_network.fit(np.array([state]), target, epochs=1, verbose=0)

        self.update_target_network_counter += 1
        if self.update_target_network_counter >= self.update_target_network_period:
            self.update_target_network()
            self.update_target_network_counter = 0

    def choose_best_action(self, state):
        state = np.array([state])
        action_values = self.main_network.predict(state)
        best_action = np.argmax(action_values[0])
        return best_action

    def reduce_epsilon(self):
        """Reduces the epsilon (exploration rate) after each episode."""
        if self.epsilon > self.epsilon_min:
            self.epsilon *= self.epsilon_decay
