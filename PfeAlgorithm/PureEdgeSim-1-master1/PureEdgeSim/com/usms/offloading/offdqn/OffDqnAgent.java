//package com.usms.offloading.offdqn;
//
//import lombok.AllArgsConstructor;
//import lombok.Data;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Random;
//
//@Data
//@AllArgsConstructor
//public class OffAgent {
//    private static final double LEARNING_RATE = 0.1;
//    private static final double DISCOUNT_FACTOR = 0.9;
//    private static final double EXPLORATION_RATE = 0.1;
//    public static final long EPOCH = 2;
//    private final Random random;
//    private final Map<OffStateAction, Double> qTable;
//
//    public OffAgent(Map<OffStateAction, Double> qTable) {
//        this.random = new Random();
//        this.qTable = (qTable != null) ? new HashMap<>(qTable) : new HashMap<>();
//    }
//
//    public OffAction chooseAction(OffState state) {
//        if (random.nextDouble() < EXPLORATION_RATE) {
//            // Explore: choose a random action
//            OffAction offAction = OffState.getRandomAction();
//            return offAction;
//        } else {
//            // Exploit: choose the action with the highest Q-value
//
//            OffAction offAction = getBestAction(state);
//            return getBestAction(state);
//        }
//    }
//
//    public OffAction getBestAction(OffState state) {
//        OffAction bestAction = null;
//        double maxQValue = Double.NEGATIVE_INFINITY;
//        for (OffAction action : OffAction.values()) {
//            double qValue = getQValue(state, action);
//            if (qValue > maxQValue) {
//                maxQValue = qValue;
//                bestAction = action;
//            }
//        }
//
//        return bestAction;
//    }
//
//    public double getQValue(OffState state, OffAction action) {
//        OffStateAction pair = new OffStateAction(state, action);
//        return qTable.getOrDefault(pair, 0.0);
//    }
//
//    public void updateQValue(OffState state, OffAction action, double newValue) {
//        OffStateAction pair = new OffStateAction(state, action);
//        qTable.put(pair, newValue);
//    }
//
//    public void learn(OffState currentState, OffAction action, double reward, OffState nextState) {
//        double currentQ = getQValue(currentState, action);
//        double maxNextQ = getMaxQValue(nextState);
//        double newQ = currentQ + LEARNING_RATE * (reward + DISCOUNT_FACTOR * maxNextQ - currentQ);
//        updateQValue(currentState, action, newQ);
//    }
//
//    public double getMaxQValue(OffState state) {
//        double maxQValue = Double.NEGATIVE_INFINITY;
//        for (OffAction action : state.getPossibleActions()) {
//            double qValue = getQValue(state, action);
//            if (qValue > maxQValue) {
//                maxQValue = qValue;
//            }
//        }
//        return maxQValue;
//    }
//}


package com.usms.offloading.offdqn;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
public class OffDqnAgent {
    private double gamma;
    private double epsilon;
    private int numberEpisodes;
    private double learningRate = 0.001;
    private int stateDimension = 8;
    private int actionDimension = 2;
    private int replayBufferSize = 300;
    private int batchReplayBufferSize = 100;
    private int updateTargetNetworkPeriod = 100;
    private int counterUpdateTargetNetwork = 0;
    private final RestTemplate restTemplate;
    private final String pythonServiceUrl = "http://localhost:5000"; // URL of the Python DQN service

    public OffDqnAgent(double gamma, double epsilon, int numberEpisodes) {
        this.gamma = gamma;
        this.epsilon = epsilon;
        this.numberEpisodes = numberEpisodes;
        this.restTemplate = new RestTemplate();
        initializeNetworksInPython();
    }

    public void initializeNetworksInPython() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("{}", headers); // Assuming no body is required for initialization
        String response = restTemplate.postForObject(pythonServiceUrl + "/initialize", entity, String.class);
        System.out.println("Response from initializing networks: " + response);
    }

    public OffAction chooseAction(OffState state) {
//        if (index > 1000) {
//            this.epsilon = 0.999 * this.epsilon;
//        }
        int action;
        if (chooseRandomlyDouble() > this.epsilon) {
            action = chooseRandomly();
        } else {
            action = postChooseBestAction(state.toArray());
        }

        if (action == 0) return OffAction.EXECUTE_LOCALLY;
        else return OffAction.OFFLOAD_TO_EDGE;
    }

    public int chooseBestAction(OffState state) {
        return postChooseBestAction(state.toArray());
    }

    private double chooseRandomlyDouble() {
        Random random = new Random();
        return random.nextDouble();
    }

    private int chooseRandomly() {
        Random random = new Random();
        return random.nextInt(2);
    }

    private int postChooseBestAction(Object[] stateArray) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ObjectMapper mapper = new ObjectMapper();

        try {
            String stateJson = mapper.writeValueAsString(Map.of("state", stateArray));
            HttpEntity<String> entity = new HttpEntity<>(stateJson, headers);
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(pythonServiceUrl + "/choose_best_action", entity, JsonNode.class);
            JsonNode responseBody = response.getBody();
            if (responseBody != null && responseBody.has("best_action")) {
                return responseBody.get("best_action").asInt();
            } else {
                throw new RuntimeException("Invalid response structure");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    private void sendTrainingBatchToPython(ArrayList<OffExperienceReplay> trainingBatch) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ObjectMapper mapper = new ObjectMapper();

        List<Map<String, Object>> transformedBatch = trainingBatch.stream().map(exp -> {
            Map<String, Object> expMap = new HashMap<>();
            expMap.put("currentState", exp.getCurrentState().toArray());
            expMap.put("nextState", exp.getNextState().toArray());
            expMap.put("action", exp.getAction());
            expMap.put("reward", exp.getReward());
            expMap.put("terminalState", exp.isDone());
            return expMap;
        }).collect(Collectors.toList());

        try {
            String json = mapper.writeValueAsString(transformedBatch);
            HttpEntity<String> entity = new HttpEntity<>(json, headers);
            String response = restTemplate.postForObject(pythonServiceUrl + "/train_network", entity, String.class);
            System.out.println("Response from server: " + response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void trainNetwork(Deque<OffExperienceReplay> replayBuffer) {
        ArrayList<OffExperienceReplay> replayList = new ArrayList<>(replayBuffer);
        sendTrainingBatchToPython(replayList);
    }


//    public void trainNetwork(Deque<OffExperienceReplay> replayBuffer) {
//        if (replayBuffer.size() > this.batchReplayBufferSize) {
//            ArrayList<OffExperienceReplay> randomSampleBatch = new ArrayList<>(batchReplayBufferSize);
//            List<OffExperienceReplay> replayBufferList = new ArrayList<>(replayBuffer);
//            Collections.shuffle(replayBufferList, new Random());
//            for (int i = 0; i < batchReplayBufferSize && i < replayBufferList.size(); i++) {
//                randomSampleBatch.add(replayBufferList.get(i));
//            }
//            sendTrainingBatchToPython(randomSampleBatch);
//        }
//    }
}
