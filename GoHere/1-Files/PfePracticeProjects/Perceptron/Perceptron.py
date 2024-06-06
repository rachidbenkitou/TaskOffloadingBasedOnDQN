import math

import numpy as np


def sigmoid(x):
    return 1. / (1 + math.exp(-x))


w = np.random.uniform(-6 / math.sqrt(3), 6 / math.sqrt(3), (1, 2))
b = 0
x = [[0, 0], [0, 1], [1, 0], [1, 1]]
y = [1, 1, 1, 0]

# x = [ [2,0], [0,3], [3,0], [1,1]]
# y = [1,0,0,1]
max_it = 10000
epoch = 1
learning_rate = 0.01

while epoch < max_it:
    error_s = 0
    for i in range(len(x)):
        y_hat = sigmoid(np.dot(w, np.asarray(x[i])) + b)
        error = y[i] - y_hat
        error_s += error * error * 0.5
        w = w + learning_rate * error * (1 - y_hat) * np.asarray(x[i], dtype=np.float32)
        b = b + learning_rate * error * (1 - y_hat)
        print('le vrai est', y[i], ' le prédit est', y_hat, ' la loss est', error_s)
    epoch = epoch + 1

proba = sigmoid(np.dot(w, np.asarray([2, 0])) + b)
print('la proba est', proba)
