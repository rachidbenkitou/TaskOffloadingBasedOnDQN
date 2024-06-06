//package com.usms.offloading.dqnoffloading;
//
//import com.mechalikh.pureedgesim.DataCentersManager.DataCenter;
//import com.mechalikh.pureedgesim.ScenarioManager.SimulationParameters;
//import com.mechalikh.pureedgesim.SimulationManager.SimLog;
//import com.mechalikh.pureedgesim.SimulationManager.SimulationManager;
//import com.mechalikh.pureedgesim.TasksGenerator.Task;
//import com.mechalikh.pureedgesim.TasksOrchestration.Orchestrator;
//import org.cloudbus.cloudsim.vms.Vm;
//
//import java.util.*;
//import java.util.stream.Collectors;
//
//public class OffDqnOrchestrator extends Orchestrator {
//    private static OffDqnAgent agent;
//    private static OffState theCurrentState;
//    private static double computedReward = 0.0;
//    //total reward
//    double totalReward = 0.0;
//
//    //global network information
//    //tasks list
//    List<Task> tasks = new ArrayList<>();
//    List<DataCenter> datacenters = new ArrayList<>();
//    //global network information
//    long edgeStorage = 0;
//    long edgeAvailableStorage = 0;
//    double edgeComputationRate = 0.0;
//    double remoteComputationRate = 0.0;
//
//    protected Map<Integer, OffStateAction> historyMap = new LinkedHashMap<>();
//    private Deque<OffExperienceReplay> replayBuffer = new ArrayDeque<>();
//
//    double updatedCpuRate = 0.0;
//
//
//    public OffDqnOrchestrator(SimulationManager simulationManager) {
//        super(simulationManager);
//        agent = new OffDqnAgent(0.1, 0.6, 20);
//        agent.initializeNetworksInPython();
//        tasks = simulationManager.getTasksList();
//        datacenters = simulationManager.getServersManager().getDatacenterList();
//        this.edgeAvailableStorage = getStorage()[1];
//        this.remoteComputationRate = getComputationRate()[0];
//        this.edgeComputationRate = getComputationRate()[1];
//        this.edgeStorage = getStorage()[1];
//    }
//
//    public long[] getStorage() {
//        long[] rates = {0, 0};
//
//        for (DataCenter dc : datacenters) {
//
//            if (dc.getType() == SimulationParameters.TYPES.CLOUD) {
//                for (Vm vm : dc.getVmList()) rates[0] += vm.getStorage().getCapacity();
//
//            } else if (dc.getType() == SimulationParameters.TYPES.EDGE_DATACENTER) {
//                for (Vm vm1 : dc.getVmList()) rates[1] += vm1.getStorage().getCapacity();
//            }
//
//        }
//        return rates;
//    }
//
//    public double[] getComputationRate() {
//        double[] rates = {0.0, 0.0};
//        double[] x = {0.0, 0.0};
//
//        for (DataCenter dc : datacenters) {
//
//            if (dc.getType() == SimulationParameters.TYPES.CLOUD) {
//                for (Vm vm : dc.getVmList()) {
//                    rates[0] += vm.getMips();
//
//                    x[0] += vm.getCurrentRequestedMaxMips();
//
//                }
//            } else if (dc.getType() == SimulationParameters.TYPES.EDGE_DATACENTER)
//
//                for (Vm vm : dc.getVmList()) {
//                    rates[1] += vm.getMips();
//
//                    x[1] += vm.getCurrentRequestedMaxMips();
//
//                }
//        }
//        return rates;
//    }
//
//    @Override
//    protected int findVM(String[] architecture, Task task) {
//        if ("DQN_OFFLOAD_TRAINING".equals(algorithm)) {
//            return dqnTaskOffloadingTraining(architecture, task);
//        }
//        if ("DQN_OFFLOAD_LEARN".equals(algorithm)) {
//            return learn(architecture, task);
//        } else if ("DQN_OFFLOAD_EVALUATION".equals(algorithm)) {
//            return dqnTaskOffloadingEvaluation(architecture, task);
//        } else if ("TRADE_OFF".equals(algorithm)) {
//            return tradeOff(architecture, task);
//        } else if ("ROUND_ROBIN".equals(algorithm)) {
//            return roundRobin(architecture, task);
//        } else {
//            SimLog.println("");
//            SimLog.println("Default Orchestrator- Unknown orchestration algorithm '" + algorithm
//                    + "', please check the simulation parameters file...");
//            // Cancel the simulation
//            SimulationParameters.STOP = true;
//            simulationManager.getSimulation().terminate();
//        }
//        return -1;
//    }
//
//    private OffState buildState(Task task, long defaultVmId) {
//        long taskLength = task.getLength();
//        double permittedDelay = task.getMaxLatency();
//        int app = task.getApplicationID();
//        long requestSize = SimulationParameters.APPLICATIONS_LIST.get(app).getRequestSize();
//        double localComputationRate = task.getEdgeDevice().getHost(0).getTotalMipsCapacity();
//        System.out.println("State : " + "[LR: " + localComputationRate + " ER: " + edgeComputationRate + " RT: " + remoteComputationRate + " AS: " + edgeAvailableStorage + " DVm: " + defaultVmId + "]");
//        return new OffState(taskLength, permittedDelay, requestSize, localComputationRate, edgeComputationRate, remoteComputationRate, edgeAvailableStorage, (int) defaultVmId);
//    }
//
//
//    private double executeLocalAlgorithm(int deviceSelectedVm, Task task, OffAction action) {
//        double dij = 0;
//        Vm selectedVm = vmList.get(deviceSelectedVm);
//        double percent = selectedVm.getCpuPercentUtilization() + (task.getLength() / selectedVm.getTotalMipsCapacity());
//        updatedCpuRate = percent * selectedVm.getTotalMipsCapacity();
//        historyMap.put(deviceSelectedVm, new OffStateAction(theCurrentState, action));
//        dij = task.getEdgeDevice().getMobilityManager().distanceTo((DataCenter) selectedVm.getHost().getDatacenter());
//        if (dij == 0) dij = SimulationParameters.AREA_LENGTH / 2;
//        return 0.5;
//    }
//
//    private double offloadedAlgorithm(int deviceSelectedVm, Task task, OffAction action) {
//        double dij = 0;
//        Vm selectedVm = vmList.get(deviceSelectedVm);
//        dij = task.getEdgeDevice().getMobilityManager().distanceTo((DataCenter) selectedVm.getHost().getDatacenter());
//        if (dij == 0) dij = SimulationParameters.AREA_LENGTH / 2;
//        updatedCpuRate = getComputationRate()[1];
//        edgeComputationRate = updatedCpuRate;
//        this.edgeAvailableStorage = this.edgeAvailableStorage - task.getContainerSize();
//        if (edgeAvailableStorage < 0) edgeAvailableStorage = 0;
//        return 0.7;
//    }
//
//    int taskIndex = 1;
//
//    private int dqnTaskOffloadingTraining(String[] architecture, Task task) {
//// The framework is its own loop for each task, so there is no need for us to iterate for tasks
//        System.out.println("****************************");
//        System.out.println("****************************");
//        System.out.println(" **************Task Number: " + taskIndex);
//        System.out.println("****************************");
//        System.out.println("****************************");
//
////        Isolate End Devices, Edge Servers and Cloud Servers
//
//        List<Vm> vmEdgeServerList = vmList.stream()
//                .filter(vm -> ((DataCenter) vm.getHost().getDatacenter()).getType() == SimulationParameters.TYPES.EDGE_DATACENTER)
//                .collect(Collectors.toList());
//
//        List<Vm> vmDevices = vmList.stream()
//                .filter(vm -> ((DataCenter) vm.getHost().getDatacenter()).getType() == SimulationParameters.TYPES.EDGE_DEVICE)
//                .collect(Collectors.toList());
//
//        List<Vm> vmsCloud = datacenters.stream()
//                .filter(dc -> dc.getType() == SimulationParameters.TYPES.CLOUD)
//                .flatMap(dc -> dc.getVmList().stream())
//                .collect(Collectors.toList());
//
//        int deviceGeneratedTheTask = pickRandom(vmDevices);
//        OffState nextState = new OffState();
//
//        for (Vm vm : task.getEdgeDevice().getVmList()) {
//            if (vm != null) {
//                // Found the first non-null and non-empty value
//                deviceGeneratedTheTask = (int) vm.getId();
//                break; // Exit the loop after finding the first value
//            }
//        }
//
//        // We get the current state of our environment
//        theCurrentState = buildState(task, deviceGeneratedTheTask);
//        // Choose action
//        // This function returns two values 0 or 1. 0 means task will be executed locally, and 1 means that the task will be offloaded.
//        int action =1;
////        int action = agent.chooseAction(theCurrentState, taskIndex);
//        OffAction offAction = null;
//        if (action == 0) {
//            offAction = OffAction.LOCAL_EXECUTION;
//        } else if (action == 1) {
//            offAction = OffAction.OFFLOAD_TO_EDGE;
//        }
//
//        assert offAction != null;
//        System.out.println("Chosen action: " + offAction.name());
//
//        switch (offAction.name()) {
//            case "LOCAL_EXECUTION":
//                computedReward = executeLocalAlgorithm(deviceGeneratedTheTask, task, offAction);
//                break;
//            default:
//                computedReward = offloadedAlgorithm(deviceGeneratedTheTask, task, offAction);
//                break;
//        }
//
//        nextState = buildState(task, deviceGeneratedTheTask);
//        totalReward += computedReward;
//        //Add current state, action, reward, next state, and terminal flag to the replay buffer
//        //agent.getReplayBuffer().addLast(new OffExperienceReplay(theCurrentState, action, computedReward, nextState, true));
//        replayBuffer.addLast(new OffExperienceReplay(theCurrentState, action, computedReward, nextState, true));
//
//
////
////
////        // here we loop through the episodes
////        for (int indexEpisode = 0; indexEpisode < agent.getNumberEpisodes(); indexEpisode++) {
////
////            // list that stores rewards per episode - this is necessary for keeping track of convergence
////            List<Double> rewardsEpisode = new ArrayList<>();
////
////            System.out.println("Simulating episode " + indexEpisode);
////
////            boolean terminalState = false;
////
////            for (Task t : tasks) {
////                if (t != task) {
////
////                    int appT = t.getApplicationID();
////                    int selectedVmId;
////                    if (t.getEdgeDevice().getVmList() != null && !t.getEdgeDevice().getVmList().isEmpty())
////                        selectedVmId = (int) t.getEdgeDevice().getVmList().get(0).getId();
////                    else selectedVmId = pickRandom(vmDevices);
////
////                    OffState nextState = buildState(t, selectedVmId);
////                    OffState currentState = theCurrentState;
////
////                    int action = agent.chooseAction(currentState, indexEpisode);
////                    double updatedCpuRate = 0.0;
////
////                    switch (action) {
////                        case 0: // Local computation
////                            selectedVmId = pickRandom(vmDevices);
////                            Vm selectedVmLocal = vmList.get(selectedVmId);
////                            // Compute updated CPU rate for local computation
////                            updatedCpuRate = selectedVmLocal.getCpuPercentUtilization() + (task.getLength() / selectedVmLocal.getTotalMipsCapacity());
////                            updatedCpuRate *= selectedVmLocal.getTotalMipsCapacity();
////                            // Compute distance for local computation
////                            if (((DataCenter) selectedVmLocal.getHost().getDatacenter()).getType() == SimulationParameters.TYPES.EDGE_DATACENTER)
////                                dij = task.getEdgeDevice().getMobilityManager().distanceTo((DataCenter) selectedVmLocal.getHost().getDatacenter());
////                            break;
////                        case 1: // Offload computation
////                            selectedVmId = pickRandom(vmEdgeServerList);
////                            Vm selectedVmEdge = vmList.get(selectedVmId);
////                            // Compute distance for offload computation
////                            if (((DataCenter) selectedVmEdge.getHost().getDatacenter()).getType() == SimulationParameters.TYPES.EDGE_DATACENTER)
////                                dij = task.getEdgeDevice().getMobilityManager().distanceTo((DataCenter) selectedVmEdge.getHost().getDatacenter());
////                            break;
////                    }
////
////                    // Compute reward
//////                    OffRewardUtil reward = new OffRewardUtil(currentState, action);
////                    // TODO
////                    //  double computedReward = reward.calculateReward(w1, w2, w3, task.getContainerSize(), dij, to, R, this.edgeAvailableStorage, task.getTime(), this.edgeStorage);
////
////                    double computedReward = 0.5;
////
////                    // Additional code snippet
////                    System.out.println("computed reward " + computedReward);
////                    System.out.println("Next state");
////                    nextState = buildState(t, selectedVmId);
////                    // Compute the total reward Gt
////                    totalReward += computedReward;
////                    System.out.println("Total rewards : " + totalReward);
////
////                    // Add current state, action, reward, next state, and terminal flag to the replay buffer
////                    agent.getReplayBuffer().addLast(new OffExperienceReplay(currentState, action, computedReward, nextState, true));
////
////                    //associer action-pair qvalue a une vm
////                    stateVmMap.put(new OffStateAction(currentState, action), selectedVmId);
////
////                    // Set the current state for the next step
////                    currentState = nextState;
////                }
////            }
////
////        }
////
////        // Train network
////        agent.trainNetwork();
////
////        int bestAction = agent.chooseBestAction(theCurrentState);
////        int vmId = stateVmMap.get(new OffStateAction(theCurrentState, bestAction));
////        System.out.println("Task : " + task.getId() + " --> Vm: " + vmId);
////
////        //update available storage
////        this.edgeAvailableStorage = this.edgeStorage;
////        return vmId;
//
//        taskIndex++;
//        System.out.println(taskIndex);
//        if (taskIndex == 10) {
//            ReplayBufferToFile instance = new ReplayBufferToFile();
//            instance.saveReplayBufferToFile("C:\\Users\\PC\\Desktop\\MyPfeFolder\\replayBuffer.dat", replayBuffer);
//        }
//        return deviceGeneratedTheTask;
//    }
//
//    public int learn(String[] architecture, Task task) {
//
//        System.out.println("Rachid Bnekitou");
//        System.out.println(this.replayBuffer);
//        //agent.trainNetwork(this.replayBuffer);
//
//        int bestAction = 1;
////        int bestAction = agent.chooseBestAction(theCurrentState);
//        //System.out.println(bestAction);
//        //int vmId = historyMap.get(new OffStateAction(theCurrentState, bestAction));
//        this.edgeAvailableStorage = this.edgeStorage;
//
//        return 1;
//    }
//
//    private int getDefaultVmIdForTask(Task task) {
//        return (int) task.getEdgeDevice().getVmList().get(0).getId();
//    }
//
//    private int dqnTaskOffloadingEvaluation(String[] architecture, Task task) {
//        return 0;
//
//    }
//
//    int pickRandom(List<Vm> vms) {
//        long d = vms.get(0).getId();
//        long f = d + vms.size();
//        return genererInt((int) d, (int) f);
//    }
//
//    int genererInt(int borneInf, int borneSup) {
//        if (borneInf == borneSup) return borneInf;
//        if (borneInf > borneSup) return -1;
//        Random random = new Random();
//        int nb;
//        nb = borneInf + random.nextInt(borneSup - borneInf);
//        return nb;
//    }
//
//    private int tradeOff(String[] architecture, Task task) {
//        int vm = -1;
//        double min = -1;
//        double new_min;// vm with minimum assigned tasks;
//
//        // get best vm for this task
//        for (int i = 0; i < orchestrationHistory.size(); i++) {
//            if (offloadingIsPossible(task, vmList.get(i), architecture)) {
//                // the weight below represent the priority, the less it is, the more it is
//                // suitable for offlaoding, you can change it as you want
//                double weight = 1.2; // this is an edge server 'cloudlet', the latency is slightly high then edge
//                // devices
//                if (((DataCenter) vmList.get(i).getHost().getDatacenter())
//                        .getType() == SimulationParameters.TYPES.CLOUD) {
//                    weight = 1.8; // this is the cloud, it consumes more energy and results in high latency, so
//                    // better to avoid it
//                } else if (((DataCenter) vmList.get(i).getHost().getDatacenter())
//                        .getType() == SimulationParameters.TYPES.EDGE_DEVICE) {
//                    weight = 1.3;// this is an edge device, it results in an extremely low latency, but may
//                    // consume more energy.
//                }
//                new_min = (orchestrationHistory.get(i).size() + 1) * weight * task.getLength()
//                        / vmList.get(i).getMips();
//                if (min == -1 || min > new_min) { // if it is the first iteration, or if this vm has more cpu mips and
//                    // less waiting tasks
//                    min = new_min;
//                    // set the first vm as thebest one
//                    vm = i;
//                }
//            }
//        }
//        // assign the tasks to the found vm
//        return vm;
//    }
//
//    private int roundRobin(String[] architecture, Task task) {
//        int vm = -1;
//        int minTasksCount = -1; // vm with minimum assigned tasks;
//        // get best vm for this task
//        for (int i = 0; i < orchestrationHistory.size(); i++) {
//            if (offloadingIsPossible(task, vmList.get(i), architecture) && minTasksCount == -1
//                    || minTasksCount > orchestrationHistory.get(i).size()) {
//                minTasksCount = orchestrationHistory.get(i).size();
//                // if this is the first time,
//                // or new min found, so we choose it as the best VM
//                // set the first vm as the best one
//                vm = i;
//            }
//        }
//        // assign the tasks to the found vm
//        return vm;
//    }
//
//    @Override
//    protected boolean offloadingIsPossible(Task task, Vm vm, String[] architecture) {
//        return super.offloadingIsPossible(task, vm, architecture);
//    }
//
//    @Override
//    public void resultsReturned(Task task) {
//
//    }
//}
