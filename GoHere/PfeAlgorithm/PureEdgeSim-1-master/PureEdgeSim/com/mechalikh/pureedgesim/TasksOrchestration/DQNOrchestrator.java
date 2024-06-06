package com.mechalikh.pureedgesim.TasksOrchestration;

import com.mechalikh.pureedgesim.DataCentersManager.DataCenter;
import com.mechalikh.pureedgesim.ScenarioManager.SimulationParameters;
import com.mechalikh.pureedgesim.SimulationManager.SimLog;
import com.mechalikh.pureedgesim.SimulationManager.SimulationManager;
import com.mechalikh.pureedgesim.TasksGenerator.Application;
import com.mechalikh.pureedgesim.TasksGenerator.Task;
import com.usms.offloading.dqn.learning.MyActionSpace;
import com.usms.offloading.dqn.learning.MyState;
import com.usms.offloading.dqn.learning.MyStateAction;
import com.usms.offloading.dqn.util.RewardUtil;
import com.usms.offloading.dqnoffloading.OffDqnAgent;
import com.usms.offloading.dqnoffloading.OffExperienceReplay;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.*;

public class DQNOrchestrator extends Orchestrator {
    //    private static Agent dqn;
    private static OffDqnAgent dqn;
    //Current state
    MyState theCurrentState;
    //reward Gt
    double w1 = SimulationParameters.W1;
    double w2 = SimulationParameters.W2;
    double w3 = SimulationParameters.W3;
    private static double computedReward = 0.0;
    //total reward
    double totalReward = 0.0;

    //global network information
    //tasks list
    List<Task> tasks = new ArrayList<>();
    List<DataCenter> datacenters = new ArrayList<>();
    //global network information
    long edgeStorage = 0;
    long edgeAvailableStorage = 0;
    double edgeComputationRate = 0.0;
    double remoteComputationRate = 0.0;

    private Deque<OffExperienceReplay> replayBuffer = new ArrayDeque<>();


    public DQNOrchestrator(SimulationManager simulationManager) {
        super(simulationManager);
        //init the q table
        dqn = new OffDqnAgent(0.1, 0.6, 20);
        dqn.initializeNetworksInPython();

        tasks = simulationManager.getTasksList();
        datacenters = simulationManager.getServersManager().getDatacenterList();
        this.edgeAvailableStorage = getStorage()[1];
        this.remoteComputationRate = getComputationRate()[0];
        this.edgeComputationRate = getComputationRate()[1];
        this.edgeStorage = getStorage()[1];
    }

    @Override
    protected int findVM(String[] architecture, Task task) {

        if ("DQN_OFFLOAD".equals(algorithm)) {
            return dqnTaskOffloading(architecture, task);
        } else if ("TRADE_OFF".equals(algorithm)) {
            return tradeOff(architecture, task);
        } else if ("ROUND_ROBIN".equals(algorithm)) {
            return roundRobin(architecture, task);
        } else {
            SimLog.println("");
            SimLog.println("Default Orchestrator- Unknown orchestration algorithm '" + algorithm
                    + "', please check the simulation parameters file...");
            // Cancel the simulation
            SimulationParameters.STOP = true;
            simulationManager.getSimulation().terminate();
        }
        return -1;
    }

    private MyState buildState(Task task, int defaultVmId) {

        long taskLength = task.getLength();
        double permittedDelay = task.getMaxLatency();
        int app = task.getApplicationID();
        long requestSize = SimulationParameters.APPLICATIONS_LIST.get(app).getRequestSize();
        double localComputationRate = task.getEdgeDevice().getHost(0).getTotalMipsCapacity();
        System.out.println("State : " + "[LR: " + localComputationRate + " ER: " + edgeComputationRate + " RT: " + remoteComputationRate + " AS: " + edgeAvailableStorage + " DVm: " + defaultVmId + "]");
        return new MyState(taskLength, permittedDelay, requestSize, localComputationRate, edgeComputationRate, remoteComputationRate, edgeAvailableStorage, defaultVmId);
    }

    public double[] getComputationRate() {
        double[] rates = {0.0, 0.0};
        double[] x = {0.0, 0.0};

        for (DataCenter dc : datacenters) {

            if (dc.getType() == SimulationParameters.TYPES.CLOUD) {
                for (Vm vm : dc.getVmList()) {
                    rates[0] += vm.getMips();

                    x[0] += vm.getCurrentRequestedMaxMips();

                }
            } else if (dc.getType() == SimulationParameters.TYPES.EDGE_DATACENTER)

                for (Vm vm : dc.getVmList()) {
                    rates[1] += vm.getMips();

                    x[1] += vm.getCurrentRequestedMaxMips();

                }

        }
        System.out.println(" X Total mip for cloud vms : " + x[0]);
        System.out.println(" X request mip for cloud vms : " + x[0]);
        System.out.println(" X Total mip for cloud edges : " + x[1]);
        System.out.println(" X request mip for cloud edges : " + x[1]);

        return rates;
    }

    public long[] getStorage() {
        long[] rates = {0, 0};

        for (DataCenter dc : datacenters) {

            if (dc.getType() == SimulationParameters.TYPES.CLOUD) {
                for (Vm vm : dc.getVmList()) rates[0] += vm.getStorage().getCapacity();

            } else if (dc.getType() == SimulationParameters.TYPES.EDGE_DATACENTER) {
                for (Vm vm1 : dc.getVmList()) rates[1] += vm1.getStorage().getCapacity();
            }

        }
        return rates;
    }
int index=0;
    private int dqnTaskOffloading(String[] architecture, Task task) {
        // Set the current state and next initial state
        int defaultVmId = pickRandomVmId(vmList);
        if (task.getEdgeDevice().getVmList() != null && !task.getEdgeDevice().getVmList().isEmpty()) {
            defaultVmId = (int) task.getEdgeDevice().getVmList().get(0).getId();
        }

        theCurrentState = buildState(task, defaultVmId);
        System.out.println("Current state task id " + task.getId());

        // Get the app associated with the current task
        int app = task.getApplicationID();
        Application appInstance = SimulationParameters.APPLICATIONS_LIST.get(app);
        int to = appInstance.getRate();
        int R = tasks.size();
        System.out.println("to == " + to + " R == " + R);

        // Calculate distance (dij) - assume it is half of the area length
        double dij = SimulationParameters.AREA_LENGTH / 2;

        // Prepare lists for VMs based on their types
        List<Vm> vmEdgeServerList = getVmListByType(SimulationParameters.TYPES.EDGE_DATACENTER);
        List<Vm> vmDevices = getVmListByType(SimulationParameters.TYPES.EDGE_DEVICE);

        // Training phase
        index++;
        System.out.println("Episode *************************************************** ***********************************************N: " + index);

        // Select the next state
        MyState nextState = buildState(task, defaultVmId);

        // Choose action
        int actionId = dqn.chooseAction(theCurrentState, index);
        MyActionSpace action = MyActionSpace.values()[actionId];
        System.out.println("Chosen action " + action.name());

        // Compute reward
        RewardUtil reward = new RewardUtil(theCurrentState, action);
        double updatedCpuRate = 0.0;
        double computedReward = 0.0;

        // Handle task execution based on chosen action
        switch (action) {
            case EXECUTE_LOCALLY:
                computedReward = executeLocally(task, vmDevices, reward, to, R, dij);
                break;
            case OFFLOAD_TO_EDGE:
                computedReward = offloadToEdge(task, vmEdgeServerList, reward, to, R, dij);
                break;
        }

        System.out.println("Computed reward " + computedReward);

        // Update next state and total reward
        nextState = buildState(task, defaultVmId);
        totalReward += computedReward;
        System.out.println("Total rewards: " + totalReward);

        // Add to replay buffer
        replayBuffer.addLast(new OffExperienceReplay(theCurrentState, actionId, computedReward, nextState, true));

        // Update current state
        theCurrentState = nextState;

        // Get the best VM
        int bestActionId = dqn.chooseBestAction(theCurrentState);
        MyActionSpace bestAction = MyActionSpace.values()[bestActionId];
        MyStateAction stateAction = new MyStateAction(theCurrentState, bestAction);

        // Reset available storage
        edgeAvailableStorage = edgeStorage;

        if (index==79) index=0;
        return index;
    }

    private double executeLocally(Task task, List<Vm> vmDevices, RewardUtil reward, int to, int R, double dij) {
        int selectedVmId = task.getEdgeDevice().getVmList() != null && !task.getEdgeDevice().getVmList().isEmpty()
                ? (int) task.getEdgeDevice().getVmList().get(0).getId()
                : pickRandomVmId(vmDevices);

        Vm selectedVm = vmList.get(selectedVmId);
        double percent = selectedVm.getCpuPercentUtilization() + (task.getLength() / selectedVm.getTotalMipsCapacity());
        double updatedCpuRate = percent * selectedVm.getTotalMipsCapacity();

        System.out.println("********** Vm Total Mips Capacity: " + selectedVm.getTotalMipsCapacity());
        System.out.println("********** Vm Percent Cpu Usage: " + percent);
        System.out.println("********** Local computing: updatedCpuRate: " + updatedCpuRate);

        if (((DataCenter) selectedVm.getHost().getDatacenter()).getType() == SimulationParameters.TYPES.EDGE_DATACENTER) {
            dij = task.getEdgeDevice().getMobilityManager().distanceTo((DataCenter) selectedVm.getHost().getDatacenter());
        }
        if (dij == 0) dij = SimulationParameters.AREA_LENGTH / 2;
        System.out.println("Distance - Local computing: " + dij);

        // Compute reward
        return reward.calculateReward(w1, w2, w3, task.getContainerSize(), dij, to, R, edgeAvailableStorage, task.getTime(), edgeStorage);
    }

    private double offloadToEdge(Task task, List<Vm> vmEdgeServerList, RewardUtil reward, int to, int R, double dij) {
        int selectedVmId = pickRandomVmId(vmEdgeServerList);
        Vm selectedVm = vmList.get(selectedVmId);
        double percent = selectedVm.getCpuPercentUtilization() + (task.getLength() / selectedVm.getTotalMipsCapacity());
        double updatedCpuRate = percent * selectedVm.getTotalMipsCapacity();

        System.out.println("********** Vm Total Mips Capacity: " + selectedVm.getTotalMipsCapacity());
        System.out.println("********** Vm Percent Cpu Usage: " + percent);
        System.out.println("********** Edge computing: updatedCpuRate: " + updatedCpuRate);

        if (((DataCenter) selectedVm.getHost().getDatacenter()).getType() == SimulationParameters.TYPES.EDGE_DATACENTER) {
            dij = task.getEdgeDevice().getMobilityManager().distanceTo((DataCenter) selectedVm.getHost().getDatacenter());
        }
        if (dij == 0) dij = SimulationParameters.AREA_LENGTH / 2;
        System.out.println("Distance - Edge computing with offload: " + dij);

        edgeAvailableStorage -= task.getContainerSize();
        if (edgeAvailableStorage < 0) edgeAvailableStorage = 0;

        // Compute reward
        return reward.calculateReward(w1, w2, w3, task.getContainerSize(), dij, to, R, edgeAvailableStorage, task.getTime(), edgeStorage);
    }

    private List<Vm> getVmListByType(SimulationParameters.TYPES type) {
        List<Vm> vmListByType = new ArrayList<>();
        for (int i = 0; i < orchestrationHistory.size(); i++) {
            if (((DataCenter) vmList.get(i).getHost().getDatacenter()).getType() == type) {
                vmListByType.add(vmList.get(i));
            }
        }
        return vmListByType;
    }

    private int pickRandomVmId(List<Vm> vmList) {
        return (int) vmList.get(new Random().nextInt(vmList.size())).getId();
    }


//    int index = 0;
//
//    private int dqnTaskOffloading(String[] architecture, Task task) {
//        //set the current state and next initial state
//        int defaultVmId = pickRandom(vmList);
//        if (task.getEdgeDevice().getVmList() != null && !task.getEdgeDevice().getVmList().isEmpty()) {
//            defaultVmId = (int) task.getEdgeDevice().getVmList().get(0).getId();
//        }
//
//        theCurrentState = buildState(task, defaultVmId);
//        System.out.println("current state task id " + task.getId());
//        //get the app associated with the current task
//        int app = task.getApplicationID();
//        //to calculate the benefit of caching we compute to and R
//        int R = tasks.size();
//        Application app1 = SimulationParameters.APPLICATIONS_LIST.get(app);
//        int to = app1.getRate();
//        System.out.println("to == " + to + " R == " + R);
//
//        double dij = SimulationParameters.AREA_LENGTH / 2;
//        //calculate distance from edge device to edge server --> task.vm == the edge server executing the task
//        //task.getedgedevice == the device generating (the origin) the task --> get best vm for this task
//        Map<MyStateAction, Integer> stateVmMap = new HashMap<>();
//        //clustering
//        List<Vm> vmEdgeServerList = new ArrayList<>();
//        List<Vm> vmDevices = new ArrayList<>();
//        for (int i = 0; i < orchestrationHistory.size(); i++) {
//            if (((DataCenter) vmList.get(i).getHost().getDatacenter()).getType() == SimulationParameters.TYPES.EDGE_DATACENTER) {
//                vmEdgeServerList.add(vmList.get(i));
//            }
//            if (((DataCenter) vmList.get(i).getHost().getDatacenter()).getType() == SimulationParameters.TYPES.EDGE_DEVICE) {
//                vmDevices.add(vmList.get(i));
//            }
//        }
//        List<Vm> vmsVloud = new ArrayList<>();
//        for (com.mechalikh.pureedgesim.DataCentersManager.DataCenter dc : datacenters) {
//            if (dc.getType() == SimulationParameters.TYPES.CLOUD) {
//                vmsVloud.addAll(dc.getVmList());
//            }
//        }
//        //training phase
//        index++;
//        System.out.println("Episode *************************************************** ***********************************************N: " + index);
//
//
//        //repeat until terminal state
//
//        // These Infos are already exists.
//        int appT = task.getApplicationID();
//        int selectedVmId;
//        if (task.getEdgeDevice().getVmList() != null && !task.getEdgeDevice().getVmList().isEmpty())
//            selectedVmId = (int) task.getEdgeDevice().getVmList().get(0).getId();
//        else selectedVmId = pickRandom(vmDevices);
//        long requestSize = SimulationParameters.APPLICATIONS_LIST.get(appT).getRequestSize();
//        double localComptationRate = vmList.get(selectedVmId).getTotalMipsCapacity();
//        MyState nextState = buildState(task, selectedVmId);
//
//
//        MyState currentState = theCurrentState;
//
//        //compute dij
//
//        /** ---------------------------------Implementation du DQN -----------------------------*/
//
//        //choose action
//        int actionId = dqn.chooseAction(currentState, index);
//        MyActionSpace action;
//        if (actionId == 0) {
//            action = MyActionSpace.EXECUTE_LOCALLY;
//        } else {
//            action = MyActionSpace.OFFLOAD_TO_EDGE;
//        }
//        System.out.println("chosen action " + action.name());
//        //compute reward
//        RewardUtil reward = new RewardUtil(currentState, action);
//        double updatedCpuRate = 0.0;
//        //next state
//        switch (action.name()) {
//            case "EXECUTE_LOCALLY": {
////                if (task.getEdgeDevice().getVmList() != null && !task.getEdgeDevice().getVmList().isEmpty()) {
////                    int localEdgeDeviceSize = task.getEdgeDevice().getVmList().size();
////                    long initialVmId = task.getEdgeDevice().getVmList().get(0).getId();
////                    //select random vm from the edgeDevice to execute the task
////                    selectedVmId = pickRandom((List<Vm>) task.getEdgeDevice().getVmList());
////                } else {
////                    selectedVmId = pickRandom(vmDevices);
////                }
////                Vm selectedVm = vmList.get(selectedVmId);
////                double percent = selectedVm.getCpuPercentUtilization() + (task.getLength() / selectedVm.getTotalMipsCapacity());
////                updatedCpuRate = percent * selectedVm.getTotalMipsCapacity();
////                System.out.println("********** Vm Total Mips Capacity : " + selectedVm.getTotalMipsCapacity());
////                System.out.println("********** Vm Percent Cpu Usage : " + percent);
////                System.out.println("********** local computing : updatedCpuRate : " + updatedCpuRate);
////                //compute dij
////                if (((DataCenter) selectedVm.getHost().getDatacenter()).getType() == SimulationParameters.TYPES.EDGE_DATACENTER)
////                    dij = task.getEdgeDevice().getMobilityManager().distanceTo((DataCenter) selectedVm.getHost().getDatacenter());
////                if (dij == 0) dij = SimulationParameters.AREA_LENGTH / 2;
////                System.out.println("Distance - Local computing: " + dij);
////                //compute reward and next state
////                computedReward = reward.calculateReward(w1, w2, w3, task.getContainerSize(), dij, to, R, this.edgeAvailableStorage, task.getTime(), this.edgeStorage);
////                break;
//            }
//            case "OFFLOAD_TO_EDGE": {
////                selectedVmId = pickRandom(vmEdgeServerList);
////                Vm selectedVm = vmList.get(selectedVmId);
////                if (((DataCenter) selectedVm.getHost().getDatacenter()).getType() == SimulationParameters.TYPES.EDGE_DATACENTER)
////                    dij = task.getEdgeDevice().getMobilityManager().distanceTo((DataCenter) selectedVm.getHost().getDatacenter());
////                if (dij == 0) dij = SimulationParameters.AREA_LENGTH / 2;
////                updatedCpuRate = getComputationRate()[1];
////                edgeComputationRate = updatedCpuRate;
////                computedReward = reward.calculateReward(w1, w2, w3, task.getContainerSize(), dij, to, R, this.edgeAvailableStorage, task.getTime(), this.edgeStorage);
////                this.edgeAvailableStorage = this.edgeAvailableStorage - task.getContainerSize();
////                if (edgeAvailableStorage < 0) edgeAvailableStorage = 0;
////                System.out.println("Distance - Edge computing with off: " + dij);
////                break;
//            }
//        }
//
//
//        System.out.println("computed reward " + computedReward);
//        System.out.println("Next state");
//        nextState = buildState(task, selectedVmId);
//        //compute the total reward Gt
//        totalReward += computedReward;
//        System.out.println("Total rewards : " + totalReward);
//
//
//        replayBuffer.addLast(new OffExperienceReplay(currentState, actionId, computedReward, nextState, true));
//
//        //update Q
////        dqn.trainNetwork(replayBuffer);
//        //associer action-pair qvalue a une vm
//        stateVmMap.put(new MyStateAction(currentState, action), selectedVmId);
//
//        //get the best VM
//
//        int bestActionId = dqn.chooseBestAction(theCurrentState);
//        MyActionSpace bestAction;
//        if (bestActionId == 0) {
//            bestAction = MyActionSpace.EXECUTE_LOCALLY;
//        } else {
//            bestAction = MyActionSpace.OFFLOAD_TO_EDGE;
//        }
//
//        MyStateAction stateAction = new MyStateAction(theCurrentState, bestAction);
//        //double qValue = dqn.getQValue(theCurrentState, bestAction);
////        int vmId = stateVmMap.get(new MyStateAction(theCurrentState, bestAction));
////        System.out.println("Task : " + task.getId() + " --> Vm: " + vmId);
//        //update availabel storage
//        this.edgeAvailableStorage = this.edgeStorage;
//        //update state
//        theCurrentState = nextState;
//        return 1;
//    }

    int genererInt(int borneInf, int borneSup) {
        if (borneInf == borneSup) return borneInf;
        if (borneInf > borneSup) return -1;
        Random random = new Random();
        int nb;
        nb = borneInf + random.nextInt(borneSup - borneInf);
        return nb;
    }

    int pickRandom(List<Vm> vms) {
        long d = vms.get(0).getId();
        long f = d + vms.size();
        return genererInt((int) d, (int) f);

    }

    private int tradeOff(String[] architecture, Task task) {
        int vm = -1;
        double min = -1;
        double new_min;// vm with minimum assigned tasks;

        // get best vm for this task
        for (int i = 0; i < orchestrationHistory.size(); i++) {
            if (offloadingIsPossible(task, vmList.get(i), architecture)) {
                // the weight below represent the priority, the less it is, the more it is
                // suitable for offlaoding, you can change it as you want
                double weight = 1.2; // this is an edge server 'cloudlet', the latency is slightly high then edge
                // devices
                if (((DataCenter) vmList.get(i).getHost().getDatacenter())
                        .getType() == SimulationParameters.TYPES.CLOUD) {
                    weight = 1.8; // this is the cloud, it consumes more energy and results in high latency, so
                    // better to avoid it
                } else if (((DataCenter) vmList.get(i).getHost().getDatacenter())
                        .getType() == SimulationParameters.TYPES.EDGE_DEVICE) {
                    weight = 1.3;// this is an edge device, it results in an extremely low latency, but may
                    // consume more energy.
                }
                new_min = (orchestrationHistory.get(i).size() + 1) * weight * task.getLength()
                        / vmList.get(i).getMips();
                if (min == -1 || min > new_min) { // if it is the first iteration, or if this vm has more cpu mips and
                    // less waiting tasks
                    min = new_min;
                    // set the first vm as thebest one
                    vm = i;
                }
            }
        }
        // assign the tasks to the found vm
        return vm;
    }

    private int roundRobin(String[] architecture, Task task) {
        int vm = -1;
        int minTasksCount = -1; // vm with minimum assigned tasks;
        // get best vm for this task
        for (int i = 0; i < orchestrationHistory.size(); i++) {
            if (offloadingIsPossible(task, vmList.get(i), architecture) && minTasksCount == -1
                    || minTasksCount > orchestrationHistory.get(i).size()) {
                minTasksCount = orchestrationHistory.get(i).size();
                // if this is the first time,
                // or new min found, so we choose it as the best VM
                // set the first vm as the best one
                vm = i;
            }
        }
        // assign the tasks to the found vm
        return vm;
    }


    @Override
    protected boolean offloadingIsPossible(Task task, Vm vm, String[] architecture) {
        return super.offloadingIsPossible(task, vm, architecture);
    }

    @Override
    public void resultsReturned(Task task) {

    }
}
