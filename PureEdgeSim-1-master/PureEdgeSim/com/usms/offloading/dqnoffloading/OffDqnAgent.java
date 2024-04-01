package com.usms.offloading.dqnoffloading;

import com.fasterxml.jackson.core.JsonProcessingException;
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

@Data
@AllArgsConstructor
public class OffDqnAgent {
    private double gamma;
    private double epsilon;
    private int numberEpisodes;

    private int times = 16;
    private double learningRate = 0.001;

    private int stateDimension = 6;
    private int actionDimension = 2;
    private int replayBufferSize = 300;
    private int batchReplayBufferSize = 100;

    private int updateTargetNetworkPeriod = 100;

    private int counterUpdateTargetNetwork = 0;

    private List<Double> sumRewardsEpisode = new ArrayList<>();

    private Deque<OffExperienceReplay> replayBuffer = new ArrayDeque<>();
    private List<Integer> actionsAppend = new ArrayList<>();

    private final RestTemplate restTemplate;
    private final String pythonServiceUrl = "http://localhost:5000"; // URL of the Python DQN service

    public OffDqnAgent(double gamma, double epsilon, int numberEpisodes) {
        this.gamma = gamma;
        this.epsilon = epsilon;
        this.numberEpisodes = numberEpisodes;
        this.restTemplate = new RestTemplate();

        // TODO Send API TO PYTHON TO INITIALISE MAIN_NETWORK AND TARGET_NETWORK AND COPY MAIN WEIGHTS IN TARGET NETWORK
        initializeNetworksInPython();
    }

    private void initializeNetworksInPython() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("{}", headers); // Assuming no body is required for initialization

        // Making the POST request to the /initialize endpoint
        String response = restTemplate.postForObject(pythonServiceUrl + "/initialize", entity, String.class);
        System.out.println("Response from initializing networks: " + response);
    }

    public int chooseAction(OffState state, int index) {
        Random random = new Random();

        if (index > 200) {
            this.epsilon = 0.999 * this.epsilon;
        }

        double randomNumber = random.nextDouble();

        if (randomNumber < this.epsilon) {
            return random.nextInt(this.actionDimension);
        } else {
            // TODO This is where your API call or other logic to determine the best action will go
            return postChooseBestAction(state.toArray());
        }
    }

    private int postChooseBestAction(double[] stateArray) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ObjectMapper mapper = new ObjectMapper();

        try {
            String stateJson = mapper.writeValueAsString(Map.of("state", stateArray));
            HttpEntity<String> entity = new HttpEntity<>(stateJson, headers);

            // Use JsonNode for the response
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(pythonServiceUrl + "/choose_best_action", entity, JsonNode.class);

            // Assuming the JSON structure is {"bestAction": value}
            JsonNode responseBody = response.getBody();
            if (responseBody != null && responseBody.has("best_action")) {
                return responseBody.get("best_action").asInt();
            } else {
                throw new RuntimeException("Invalid response structure");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return -1; // Handle error scenario
        }
    }

    public void trainingEpisodes() {

        // here we loop through the episodes
        for (int indexEpisode = 0; indexEpisode < numberEpisodes; indexEpisode++) {

            // list that stores rewards per episode - this is necessary for keeping track of convergence
            List<Double> rewardsEpisode = new ArrayList<>();

            System.out.println("Simulating episode " + indexEpisode);

            OffState currentState = OffState.reset();

            boolean terminalState = false;

            while (!terminalState) {

                int action = chooseAction(currentState, indexEpisode);

                // Make them dynamic in Orchestration
                OffState nextState = new OffState();
                double reward = 12;
                terminalState = true;

                // Add current state, action, reward, next state, and terminal flag to the replay buffer
                replayBuffer.addLast(new OffExperienceReplay(currentState, action, reward, nextState, terminalState));

                // Train network
                trainNetwork();

                // Set the current state for the next step
                currentState = nextState;
            }

        }
    }

    private void sendTrainingBatchToPython(ArrayList<OffExperienceReplay> trainingBatch) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ObjectMapper mapper = new ObjectMapper();

        // Attempt to serialize the trainingBatch to JSON
        String json;
        try {
            json = mapper.writeValueAsString(trainingBatch);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return; // Or handle the error appropriately
        }

        HttpEntity<String> entity = new HttpEntity<>(json, headers);
        String trainNetworkUrl = "http://127.0.0.1:5000/train_network";

        // Using restTemplate to send a POST request
        RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate.postForObject(trainNetworkUrl, entity, String.class);

        // Optional: Do something with the response
        System.out.println("Response from server: " + response);
    }
    public void trainNetwork() {
        if (this.replayBuffer.size() > this.batchReplayBufferSize) {
            ArrayList<OffExperienceReplay> randomSampleBatch = new ArrayList<>(batchReplayBufferSize);
            List<OffExperienceReplay> replayBufferList = new ArrayList<>(replayBuffer);
            Collections.shuffle(replayBufferList, new Random());
            for (int i = 0; i < batchReplayBufferSize && i < replayBufferList.size(); i++) {
                randomSampleBatch.add(replayBufferList.get(i));
            }
            // TODO SEND SAMPLE BATCH TO PYTHON VIA API TO TRAIN THE NETWORK
            sendTrainingBatchToPython(randomSampleBatch);
        }
    }
}
