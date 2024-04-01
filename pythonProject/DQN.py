import random
from collections import deque

from tensorflow.keras.layers import Dense
from tensorflow.keras.models import Sequential
from tensorflow.keras.optimizers import Adam


class ReplayMemory:
    def __init__(self, capacity):
        self.capacity = capacity
        self.memory = deque(maxlen=capacity)

    def add(self, experience):
        self.memory.append(experience)

    def sample(self, batch_size):
        return random.sample(self.memory, batch_size)

    def size(self):
        return len(self.memory)


class DQNModel:
    def __init__(self, state_size, action_size, learning_rate=0.001):
        self.model = self._build_model(state_size, action_size, learning_rate)

    def _build_model(self, state_size, action_size, learning_rate):
        model = Sequential([
            Dense(24, input_shape=(state_size,), activation='relu'),
            Dense(24, activation='relu'),
            Dense(action_size, activation='linear')
        ])
        model.compile(loss='mse', optimizer=Adam(learning_rate=learning_rate))
        return model

    def predict(self, state):
        return self.model.predict(state)

    def update(self, state, q_values):
        self.model.fit(state, q_values, verbose=0)
