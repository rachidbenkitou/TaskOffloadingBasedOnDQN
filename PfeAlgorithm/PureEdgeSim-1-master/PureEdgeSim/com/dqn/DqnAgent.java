//package com.dqn;
//
//import lombok.Data;
//import org.springframework.web.client.RestTemplate;
//
//@Data
//public class DqnAgent {
//    private double gamma; // Discount factor for future rewards
//    private double epsilon = 1.0; // Exploration rate for choosing random actions
//    private final double epsilonMin; // Minimum value for epsilon
//    private final double epsilonDecay; // Decay rate for epsilon after each episode
//
//    private final RestTemplate restTemplate;
//    private final String pythonServiceUrl = "http://localhost:5000"; // URL of the Python DQN service
//
//    public DqnAgent(double gamma, double epsilonMin, double epsilonDecay) {
//        this.gamma = gamma;
//        this.epsilonMin = epsilonMin;
//        this.epsilonDecay = epsilonDecay;
//        this.restTemplate = new RestTemplate();
//    }
//
//    public Action chooseAction(State state) {
//        if (Math.random() < epsilon) {
//            // Explore: choose a random action
//            return Math.random() < 0.5 ? Action.LOCAL_EXECUTION : Action.OFFLOAD_TO_EDGE;
//        } else {
//            // Exploit: choose the best action from the DQN model
//            Action action = restTemplate.postForObject(pythonServiceUrl + "/predict", state, Action.class);
//            return action != null ? action : Action.LOCAL_EXECUTION;
//        }
//    }
//
//    public void train(State currentState, Action action, double reward, State nextState, boolean done) {
//        Experience experience = new Experience(currentState, action, reward, nextState, done);
//        restTemplate.postForObject(pythonServiceUrl + "/train", experience, Void.class);
//        updateEpsilon();
//    }
//
//    private void updateEpsilon() {
//        if (epsilon > epsilonMin) {
//            epsilon *= epsilonDecay;
//        }
//    }
//
//    public Action getBestAction(State state) {
//        // Calls the Python service endpoint dedicated to returning the best action
//        Action bestAction = restTemplate.postForObject(pythonServiceUrl + "/get_best_action", state, Action.class);
//        return bestAction != null ? bestAction : Action.LOCAL_EXECUTION; // Fallback to LOCAL_EXECUTION if null
//    }
//}
