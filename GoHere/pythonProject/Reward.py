def calculate_reward(state, action, latency, energy_consumption, penalty):
    alpha = 0.5  # Weight for latency
    beta = 0.5  # Weight for energy consumption
    # Assume penalty for exceeding max delay is incorporated into the system's cost function
    reward = - (alpha * latency + beta * energy_consumption - penalty)
    return reward
