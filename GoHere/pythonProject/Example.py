import numpy as np
import tensorflow as tf
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import Dense
from tensorflow.keras.optimizers import Adam


class OPOAgent:
    def __init__(self, state_size, action_size):
        self.state_size = state_size
        self.action_size = action_size
        self.memory = []  # For storing experiences
        self.gamma = 0.95  # Discount rate
        self.epsilon = 1.0  # Exploration rate
        self.epsilon_min = 0.01
        self.epsilon_decay = 0.995
        self.learning_rate = 0.001
        self.model = self._build_model()

    def _build_model(self):
        model = Sequential()
        model.add(Dense(24, input_dim=self.state_size, activation='relu'))
        model.add(Dense(24, activation='relu'))
        model.add(Dense(self.action_size, activation='linear'))
        model.compile(loss='mse', optimizer=Adam(lr=self.learning_rate))
        return model

    def remember(self, state, action, reward, next_state, done):
        self.memory.append((state, action, reward, next_state, done))

    def act(self, state):
        if np.random.rand() <= self.epsilon:
            return np.random.randint(self.action_size)
        act_values = self.model.predict(state)
        return np.argmax(act_values[0])

    def replay(self, batch_size):
        minibatch_indices = np.random.choice(len(self.memory), batch_size, replace=False)
        minibatch = [self.memory[i] for i in minibatch_indices]
        for state, action, reward, next_state, done in minibatch:
            target = reward
            if not done:
                target = (reward + self.gamma * np.amax(self.model.predict(next_state)[0]))
            target_f = self.model.predict(state)
            target_f[0][action] = target
            self.model.fit(state, target_f, epochs=1, verbose=0)
        if self.epsilon > self.epsilon_min:
            self.epsilon *= self.epsilon_decay


# Define environment and agent
state_size = 4  # Assuming a simplified state representation
action_size = 2  # Assuming two possible actions: offload to edge or process locally
agent = OPOAgent(state_size, action_size)
episodes = 100  # Number of episodes to run the simulation

# Simulate environment interaction
for e in range(episodes):
    state = np.random.rand(1, state_size)  # Simulate initial state
    for time in range(50):  # Assume a maximum of 50 steps per episode
        action = agent.act(state)

        # Here, insert logic to determine next_state, reward, and done based on the action
        # For this simulation, we use random values to represent this logic
        next_state = np.random.rand(1, state_size)
        reward = np.random.rand()  # Placeholder for actual reward calculation
        done = np.random.choice([True, False])  # Randomly end the episode

        # Store the experience
        agent.remember(state, action, reward, next_state, done)

        state = next_state
        if done:
            print(f"episode: {e}/{episodes}, score: {time}, e: {agent.epsilon:.2}")
            break

    # Train the agent with experiences sampled from memory
    if len(agent.memory) > 32:
        agent.replay(32)
