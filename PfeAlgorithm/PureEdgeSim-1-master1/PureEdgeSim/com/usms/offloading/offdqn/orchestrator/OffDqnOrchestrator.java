package com.usms.offloading.offdqn.orchestrator;

import com.mechalikh.pureedgesim.DataCentersManager.DataCenter;
import com.mechalikh.pureedgesim.ScenarioManager.SimulationParameters;
import com.mechalikh.pureedgesim.SimulationManager.SimLog;
import com.mechalikh.pureedgesim.SimulationManager.SimulationManager;
import com.mechalikh.pureedgesim.TasksGenerator.Task;
import com.usms.offloading.offdqn.OffAction;
import com.usms.offloading.offdqn.OffDqnAgent;
import com.usms.offloading.offdqn.OffExperienceReplay;
import com.usms.offloading.offdqn.OffState;
import examples.CustomEdgeOrchestrator;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.*;

public class OffDqnOrchestrator extends CustomEdgeOrchestrator {
    protected Map<Integer, Integer> historyMap = new LinkedHashMap<>();
    private Deque<OffExperienceReplay> replayBufferList = new ArrayDeque<>();
    private static OffDqnAgent dqn;
    OffState currentState;
    OffState nextState;
    private final double computedReward = 0.0;
    double edgeComputationRate = 0.0;

    List<DataCenter> datacenters = new ArrayList<>();
    List<Integer> mobileDevices;
    List<Integer> edgeServers;

    public OffDqnOrchestrator(SimulationManager simulationManager) {
        super(simulationManager);

        mobileDevices = new ArrayList<>();
        edgeServers = new ArrayList<>();
        for (int i = 0; i < orchestrationHistory.size(); i++) {
            Vm currentVm = vmList.get(i);
            // Check if the current VM is an EDGE_DEVICE
            if (((DataCenter) currentVm.getHost().getDatacenter()).getType() == SimulationParameters.TYPES.EDGE_DEVICE) {
                mobileDevices.add((int) currentVm.getId());
            }
        }

        for (int i = 0; i < orchestrationHistory.size(); i++) {
            Vm currentVm = vmList.get(i);
            // Check if the current VM is an EDGE_DEVICE
            if (((DataCenter) currentVm.getHost().getDatacenter()).getType() == SimulationParameters.TYPES.EDGE_DATACENTER || ((DataCenter) currentVm.getHost().getDatacenter()).getType() == SimulationParameters.TYPES.CLOUD) {
                edgeServers.add((int) currentVm.getId());
            }
        }
        System.out.println(edgeServers);

        datacenters = simulationManager.getServersManager().getDatacenterList();
        this.edgeComputationRate = getComputationRate()[1];
        dqn = new OffDqnAgent(0.1, 0.1, 20);

//        dqn = new OffAgent(new LinkedHashMap<>());
//        offDqnAgent = new OffDqnAgent(0.1, 0.1, 20);
//        for (double q : dqn.getQTable().values()) {
//            q = 0.0;
//        }
    }

    @Override
    protected int findVM(String[] architecture, Task task) {

        if ("DQN_OFFLOAD".equals(algorithm)) {
            return dqnTaskOffloading(architecture, task);
        }
//        else if ("TRADE_OFF".equals(algorithm)) {
//            return tradeOff(architecture, task);
//        } else if ("ROUND_ROBIN".equals(algorithm)) {
//            return roundRobin(architecture, task);
//        }
        else {
            SimLog.println("");
            SimLog.println("Default Orchestrator- Unknown orchestration algorithm '" + algorithm
                    + "', please check the simulation parameters file...");
            // Cancel the simulation
            SimulationParameters.STOP = true;
            simulationManager.getSimulation().terminate();
        }
        return -1;
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
            } else if (dc.getType() == SimulationParameters.TYPES.EDGE_DATACENTER) {
                for (Vm vm : dc.getVmList()) {
                    rates[1] += vm.getMips();
                    x[1] += vm.getCurrentRequestedMaxMips();
                }
            }
        }
        return rates;
    }

    int vmId = 0;
    int index = 0;

    public int dqnTaskOffloading(String[] architecture, Task task) {
        index++;
        currentState = OffState.buildState(task, edgeComputationRate);
        OffAction action = dqn.chooseAction(currentState);

//        System.out.println("Choosen Action: " + action);
        double updatedCpuRate = 0.0;
        switch (action.name()) {
            case "EXECUTE_LOCALLY": {
                vmId = executeLocally(task, architecture);
//                vmId = RandomSelection.getRandomDevice();
                System.out.println("Locally: " + vmId);
            }
            default: {
                vmId = offloadToEdge(task, architecture);
//                vmId = RandomSelection.getRandomEdge();
                System.out.println("Offloaded: " + vmId);

            }
        }

        OffState nextState = OffState.buildState(task, edgeComputationRate);

        OffExperienceReplay offExperienceReplay = OffExperienceReplay
                .builder()
                .nextState(nextState)
                .done(true)
                .reward(12)
                .currentState(currentState)
                .build();

        replayBufferList.add(offExperienceReplay);
//        dqn.trainNetwork(replayBufferList);

        currentState = nextState;

//        return RandomSelection.getRandom();
        return vmId;
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


//    private int executeLocally(Task task, String[] architecture) {
//        int vmwareId = -1;
//
////        if (!task.getEdgeDevice().getVmList().isEmpty()) {
//        vmwareId = (int) task.getEdgeDevice().getHost(0).getVmList().get(0).getId();
////        } else {
////            vmwareId = (int) task.getEdgeDevice().getId();
////        }
//
//        for (int i = 0; i < mobileDevices.size(); i++) {
////            Vm currentVm = vmList.get(i);
//            // Check if the current VM is an EDGE_DEVICE
////            if (((DataCenter) currentVm.getHost().getDatacenter()).getType() == SimulationParameters.TYPES.EDGE_DEVICE) {
////                System.out.println(currentVm.getId());
//            if (mobileDevices.get(i) == vmwareId) {
//                return i; // Return the index of the VM in the orchestrationHistory list
//            }
////            }
//        }
//
//        return -1; // Return -1 if the originating VM is not found
//    }
    private int executeLocally(Task task, String[] architecture) {
        int vmwareId = -1;

//        if (!task.getEdgeDevice().getVmList().isEmpty()) {
        vmwareId = (int) task.getEdgeDevice().getHost(0).getVmList().get(0).getId();
//        } else {
//            vmwareId = (int) task.getEdgeDevice().getId();
//        }

        for (int i = 0; i < orchestrationHistory.size(); i++) {
            Vm currentVm = vmList.get(i);
            // Check if the current VM is an EDGE_DEVICE
            if (((DataCenter) currentVm.getHost().getDatacenter()).getType() == SimulationParameters.TYPES.EDGE_DEVICE) {
//                System.out.println(currentVm.getId());
                if (currentVm.getId() == vmwareId) {
                    return i; // Return the index of the VM in the orchestrationHistory list
                }
            }
        }

        return -1; // Return -1 if the originating VM is not found
    }

    private int offloadToEdge(Task task, String[] architecture) {
        int vm = -1;
        double min = -1;
        double new_min;

        // Iterate through the orchestration history
        for (int i = 0; i < orchestrationHistory.size(); i++) {
            Vm currentVm = vmList.get(i);
            if (offloadingIsPossible(task, currentVm, architecture)) {
                // Check if the current VM is an EDGE_DEVICE
                if (((DataCenter) currentVm.getHost().getDatacenter()).getType() == SimulationParameters.TYPES.EDGE_DATACENTER) {

                    double weight = 1.3; // Default weight for edge devices

                    new_min = (orchestrationHistory.get(i).size() + 1) * weight * task.getLength() / currentVm.getMips();

                    if (min == -1 || min > new_min) {
                        min = new_min;
                        vm = i;
                    }
                }
            }
        }

        return vm;
    }
}


