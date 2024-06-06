import os

from flask import Flask, request, jsonify
from tensorflow.keras.models import load_model as keras_load_model

from OffDqnModelClass import DeepQLearningAgent

app = Flask(__name__)

# Instantiate the DeepQLearningAgent with default parameters or adjust as necessary.
agent = DeepQLearningAgent(state_size=19, action_size=2)

# Path to save and load the model
MODEL_PATH = "main_network_model.h5"


@app.route('/choose_best_action', methods=['POST'])
def choose_best_action():
    data = request.get_json()
    print(data)
    state = data['state']
    best_action = agent.choose_best_action(state)
    print(best_action)
    return jsonify({'best_action': int(best_action)}), 200


@app.route('/train_network', methods=['POST'])
def train_network():
    experiences = request.get_json()
    print(experiences)
    for experience in experiences:
        currentState = experience['currentState']
        action = experience['action']
        reward = experience['reward']
        nextState = experience['nextState']
        agent.remember(currentState, action, reward, nextState)
    agent.train()
    return jsonify({'message': 'Training completed'}), 200


@app.route('/save_model', methods=['POST'])
def save_model():
    agent.main_network.save(MODEL_PATH)
    return jsonify({'message': 'Model saved successfully'}), 200


@app.route('/load_model', methods=['POST'])
def load_model():
    if os.path.exists(MODEL_PATH):
        agent.main_network = keras_load_model(MODEL_PATH)
        return jsonify({'message': 'Model loaded successfully'}), 200
    else:
        return jsonify({'message': 'Model file not found'}), 404


if __name__ == '__main__':
    app.run(debug=True)

# @app.route('/get_best_action', methods=['POST'])
# def get_best_action():
#     data = request.get_json()
#     state = data['state']
#     best_action = agent.choose_best_action(state)
#     return jsonify({'best_action': int(best_action)}), 200
#
#
# @app.route('/initialize', methods=['POST'])
# def initialize():
#     # Assuming the initialization just involves setting up the networks which is done in the constructor.
#     # If any other setup is needed, it should be added here.
#     # global agent
#     # agent = DeepQLearningAgent(state_size=3, action_size=2)  # Re-instantiate to reset
#     return jsonify({'message': 'Networks initialized'}), 200
