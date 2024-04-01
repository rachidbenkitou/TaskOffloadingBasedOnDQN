import numpy as np
from flask import Flask, request, jsonify
from tensorflow.keras.layers import Dense
from tensorflow.keras.models import Sequential
from tensorflow.keras.optimizers import Adam

app = Flask(__name__)

# Global variables for the main and target networks
main_network = None
target_network = None


@app.route('/initialize', methods=['POST'])
def initialize_networks():
    global main_network, target_network
    state_dimension = request.json['stateDimension']
    action_dimension = request.json['actionDimension']
    learning_rate = request.json['learningRate']

    main_network = Sequential([
        Dense(128, input_dim=state_dimension, activation='relu'),
        Dense(56, activation='relu'),
        Dense(action_dimension, activation='linear')
    ])
    main_network.compile(loss='mse', optimizer=Adam(learning_rate=learning_rate))

    target_network = Sequential([
        Dense(128, input_dim=state_dimension, activation='relu'),
        Dense(56, activation='relu'),
        Dense(action_dimension, activation='linear')
    ])
    target_network.compile(loss='mse', optimizer=Adam(learning_rate=learning_rate))

    # Copy weights from main_network to target_network
    target_network.set_weights(main_network.get_weights())

    return jsonify({'status': 'Networks initialized and synchronized'})


@app.route('/choose_action', methods=['POST'])
def choose_action():
    global main_network
    state = np.array([request.json['state']])
    action_values = main_network.predict(state)
    best_action = np.argmax(action_values[0])
    return jsonify({'action': int(best_action)})


@app.route('/train_network', methods=['POST'])
def train_network():
    experiences = request.json

    return jsonify({'status': 'Training completed'})


if __name__ == '__main__':
    app.run(debug=True)
