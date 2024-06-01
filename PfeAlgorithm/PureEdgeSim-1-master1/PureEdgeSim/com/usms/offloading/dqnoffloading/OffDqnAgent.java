//package com.usms.offloading.dqnoffloading;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.usms.offloading.dqn.learning.MyState;
//import lombok.AllArgsConstructor;
//import lombok.Data;
//import org.springframework.http.HttpEntity;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.client.RestTemplate;
//
//import java.util.*;
//import java.util.stream.Collectors;
//
//@Data
//@AllArgsConstructor
//public class OffDqnAgent {
//    private double gamma;
//    private double epsilon;
//    private int numberEpisodes;
//    private double learningRate = 0.001;
//    private int stateDimension = 8;
//    private int actionDimension = 2;
//    private int replayBufferSize = 300;
//    private int batchReplayBufferSize = 100;
//    private int updateTargetNetworkPeriod = 100;
//    private int counterUpdateTargetNetwork = 0;
//    private final RestTemplate restTemplate;
//    private final String pythonServiceUrl = "http://localhost:5000"; // URL of the Python DQN service
//
//    public OffDqnAgent(double gamma, double epsilon, int numberEpisodes) {
//        this.gamma = gamma;
//        this.epsilon = epsilon;
//        this.numberEpisodes = numberEpisodes;
//        this.restTemplate = new RestTemplate();
//        initializeNetworksInPython();
//    }
//
//    public void initializeNetworksInPython() {
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        HttpEntity<String> entity = new HttpEntity<>("{}", headers); // Assuming no body is required for initialization
//        String response = restTemplate.postForObject(pythonServiceUrl + "/initialize", entity, String.class);
//        System.out.println("Response from initializing networks: " + response);
//    }
//
//    public int chooseAction(MyState state, int index) {
//        if (index > 1000) {
//            this.epsilon = 0.999 * this.epsilon;
//        }
//        if (chooseRandomlyDouble() > this.epsilon) {
//            return chooseRandomly();
//        } else {
//            return postChooseBestAction(state.toArray());
//        }
//    }
//
//    public int chooseBestAction(MyState state) {
//        return postChooseBestAction(state.toArray());
//    }
//
//    private double chooseRandomlyDouble() {
//        Random random = new Random();
//        return random.nextDouble();
//    }
//
//    private int chooseRandomly() {
//        Random random = new Random();
//        return random.nextInt(2);
//    }
//
//    private int postChooseBestAction(double[] stateArray) {
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        ObjectMapper mapper = new ObjectMapper();
//
//        try {
//            String stateJson = mapper.writeValueAsString(Map.of("state", stateArray));
//            HttpEntity<String> entity = new HttpEntity<>(stateJson, headers);
//            ResponseEntity<JsonNode> response = restTemplate.postForEntity(pythonServiceUrl + "/choose_best_action", entity, JsonNode.class);
//            JsonNode responseBody = response.getBody();
//            if (responseBody != null && responseBody.has("best_action")) {
//                return responseBody.get("best_action").asInt();
//            } else {
//                throw new RuntimeException("Invalid response structure");
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            return -1;
//        }
//    }
//
//    private void sendTrainingBatchToPython(ArrayList<OffExperienceReplay> trainingBatch) {
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        ObjectMapper mapper = new ObjectMapper();
//
//        List<Map<String, Object>> transformedBatch = trainingBatch.stream().map(exp -> {
//            Map<String, Object> expMap = new HashMap<>();
//            expMap.put("currentState", exp.getCurrentState().toArray());
//            expMap.put("nextState", exp.getNextState().toArray());
//            expMap.put("action", exp.getAction());
//            expMap.put("reward", exp.getReward());
//            expMap.put("terminalState", exp.isDone());
//            return expMap;
//        }).collect(Collectors.toList());
//
//        try {
//            String json = mapper.writeValueAsString(transformedBatch);
//            HttpEntity<String> entity = new HttpEntity<>(json, headers);
//            String response = restTemplate.postForObject(pythonServiceUrl + "/train_network", entity, String.class);
//            System.out.println("Response from server: " + response);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    public void trainNetwork(Deque<OffExperienceReplay> replayBuffer) {
//        ArrayList<OffExperienceReplay> replayList = new ArrayList<>(replayBuffer);
//        sendTrainingBatchToPython(replayList);
//    }
//
//
////    public void trainNetwork(Deque<OffExperienceReplay> replayBuffer) {
////        if (replayBuffer.size() > this.batchReplayBufferSize) {
////            ArrayList<OffExperienceReplay> randomSampleBatch = new ArrayList<>(batchReplayBufferSize);
////            List<OffExperienceReplay> replayBufferList = new ArrayList<>(replayBuffer);
////            Collections.shuffle(replayBufferList, new Random());
////            for (int i = 0; i < batchReplayBufferSize && i < replayBufferList.size(); i++) {
////                randomSampleBatch.add(replayBufferList.get(i));
////            }
////            sendTrainingBatchToPython(randomSampleBatch);
////        }
////    }
//}
