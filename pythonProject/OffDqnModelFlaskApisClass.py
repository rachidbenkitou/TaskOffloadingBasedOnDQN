from flask import Flask, request, jsonify

from OffDqnModelClass import DeepQLearningAgent

app = Flask(__name__)

# Instantiate the DeepQLearningAgent with default parameters or adjust as necessary.
agent = DeepQLearningAgent(state_size=6, action_size=2)


@app.route('/initialize', methods=['POST'])
def initialize():
    # Assuming the initialization just involves setting up the networks which is done in the constructor.
    # If any other setup is needed, it should be added here.
    global agent
    agent = DeepQLearningAgent(state_size=6, action_size=2)  # Re-instantiate to reset
    return jsonify({'message': 'Networks initialized'}), 200


@app.route('/choose_best_action', methods=['POST'])
def choose_best_action():
    data = request.get_json()
    state = data['state']
    best_action = agent.choose_best_action(state)
    return jsonify({'best_action': int(best_action)}), 200


@app.route('/train_network', methods=['POST'])
def train_network():
    experiences = request.get_json()
    for experience in experiences:
        currentState = experience['currentState']
        action = experience['action']
        reward = experience['reward']
        nextState = experience['nextState']
        terminalState = experience['terminalState']
        agent.remember(currentState, action, reward, nextState, terminalState)
    agent.train()
    return jsonify({'message': 'Training completed'}), 200


if __name__ == '__main__':
    app.run(debug=True)
