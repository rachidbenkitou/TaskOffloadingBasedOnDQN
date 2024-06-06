package com.usms.offloading.dqnoffloading;

import com.mechalikh.pureedgesim.DataCentersManager.DataCenter;
import com.mechalikh.pureedgesim.ScenarioManager.SimulationParameters;
import com.mechalikh.pureedgesim.SimulationManager.SimLog;
import com.mechalikh.pureedgesim.SimulationManager.SimulationManager;
import com.mechalikh.pureedgesim.TasksGenerator.Task;
import com.mechalikh.pureedgesim.TasksOrchestration.Orchestrator;
import com.usms.offloading.dqn.learning.MyActionSpace;
import com.usms.offloading.dqn.util.RewardUtil;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.*;
import java.util.stream.Collectors;

public class OffDqnOrchestrator extends Orchestrator {
    private static OffDqnAgent agent;
    private static OffState theCurrentState;
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

    protected Map<Integer, OffStateAction> historyMap = new LinkedHashMap<>();
    private Deque<OffExperienceReplay> replayBuffer = new ArrayDeque<>();

    double updatedCpuRate = 0.0;


    public OffDqnOrchestrator(SimulationManager simulationManager) {
        super(simulationManager);
        agent = new OffDqnAgent(0.1, 0.6, 20);
        agent.initializeNetworksInPython();
        tasks = simulationManager.getTasksList();
        datacenters = simulationManager.getServersManager().getDatacenterList();
        this.edgeAvailableStorage = getStorage()[1];
        this.remoteComputationRate = getComputationRate()[0];
        this.edgeComputationRate = getComputationRate()[1];
        this.edgeStorage = getStorage()[1];
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
        return rates;
    }

    @Override
    protected int findVM(String[] architecture, Task task) {
        if ("DQN_OFFLOAD_TRAINING".equals(algorithm)) {
            return dqnTaskOffloadingTraining(architecture, task);
        }
        if ("DQN_OFFLOAD_LEARN".equals(algorithm)) {
            return learn(architecture, task);
        } else if ("DQN_OFFLOAD_EVALUATION".equals(algorithm)) {
            return dqnTaskOffloadingEvaluation(architecture, task);
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

    private double executeLocalAlgorithm(int deviceSelectedVm, Task task, OffAction action) {
        double dij = 0;
        Vm selectedVm = vmList.get(deviceSelectedVm);
        double percent = selectedVm.getCpuPercentUtilization() + (task.getLength() / selectedVm.getTotalMipsCapacity());
        updatedCpuRate = percent * selectedVm.getTotalMipsCapacity();
        historyMap.put(deviceSelectedVm, new OffStateAction(theCurrentState, action));
        dij = task.getEdgeDevice().getMobilityManager().distanceTo((DataCenter) selectedVm.getHost().getDatacenter());
        if (dij == 0) dij = SimulationParameters.AREA_LENGTH / 2;
//        RewardUtil rewardUtil= new RewardUtil(theCurrentState, MyActionSpace.EXECUTE_LOCALLY)
        return 0.5;
    }

    private double offloadedAlgorithm(int deviceSelectedVm, Task task, OffAction action) {
        double dij = 0;
        Vm selectedVm = vmList.get(deviceSelectedVm);
        dij = task.getEdgeDevice().getMobilityManager().distanceTo((DataCenter) selectedVm.getHost().getDatacenter());
        if (dij == 0) dij = SimulationParameters.AREA_LENGTH / 2;
        updatedCpuRate = getComputationRate()[1];
        edgeComputationRate = updatedCpuRate;
        this.edgeAvailableStorage = this.edgeAvailableStorage - task.getContainerSize();
        if (edgeAvailableStorage < 0) edgeAvailableStorage = 0;
        return 0.7;
    }

    int taskIndex = 1;

    private int dqnTaskOffloadingTraining(String[] architecture, Task task) {
        List<Vm> vmEdgeServerList = vmList.stream()
                .filter(vm -> ((DataCenter) vm.getHost().getDatacenter()).getType() == SimulationParameters.TYPES.EDGE_DATACENTER)
                .collect(Collectors.toList());

        List<Vm> vmDevices = vmList.stream()
                .filter(vm -> ((DataCenter) vm.getHost().getDatacenter()).getType() == SimulationParameters.TYPES.EDGE_DEVICE)
                .collect(Collectors.toList());

        List<Vm> vmsCloud = datacenters.stream()
                .filter(dc -> dc.getType() == SimulationParameters.TYPES.CLOUD)
                .flatMap(dc -> dc.getVmList().stream())
                .collect(Collectors.toList());

        int deviceGeneratedTheTask = pickRandom(vmDevices);
        OffState nextState;

        for (Vm vm : task.getEdgeDevice().getVmList()) {
            if (vm != null) {
                deviceGeneratedTheTask = (int) vm.getId();
                break;
            }
        }

        theCurrentState = OffState.buildState(task, deviceGeneratedTheTask);
        int action = 1;
        OffAction offAction = null;
        if (action == 0) {
            offAction = OffAction.LOCAL_EXECUTION;
        } else if (action == 1) {
            offAction = OffAction.OFFLOAD_TO_EDGE;
        }

        assert offAction != null;

        switch (offAction.name()) {
            case "LOCAL_EXECUTION":
                computedReward = executeLocalAlgorithm(deviceGeneratedTheTask, task, offAction);
                break;
            default:
                computedReward = offloadedAlgorithm(deviceGeneratedTheTask, task, offAction);
                break;
        }

        nextState = OffState.buildState(task, deviceGeneratedTheTask);
        totalReward += computedReward;
        //Add current state, action, reward, next state, and terminal flag to the replay buffer
        //agent.getReplayBuffer().addLast(new OffExperienceReplay(theCurrentState, action, computedReward, nextState, true));
        replayBuffer.addLast(new OffExperienceReplay(theCurrentState, action, computedReward, nextState, true));

        taskIndex++;

        return deviceGeneratedTheTask;
    }

    public int learn(String[] architecture, Task task) {

        this.edgeAvailableStorage = this.edgeStorage;
        return 1;
    }

    private int getDefaultVmIdForTask(Task task) {
        return (int) task.getEdgeDevice().getVmList().get(0).getId();
    }
    private int dqnTaskOffloadingEvaluation(String[] architecture, Task task) {
        return 0;
    }
    int pickRandom(List<Vm> vms) {
        long d = vms.get(0).getId();
        long f = d + vms.size();
        return genererInt((int) d, (int) f);
    }

    int genererInt(int borneInf, int borneSup) {
        if (borneInf == borneSup) return borneInf;
        if (borneInf > borneSup) return -1;
        Random random = new Random();
        int nb;
        nb = borneInf + random.nextInt(borneSup - borneInf);
        return nb;
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
