package com.usms.offloading.offdqn.orchestrator;

import com.mechalikh.pureedgesim.DataCentersManager.DataCenter;
import com.mechalikh.pureedgesim.ScenarioManager.SimulationParameters;
import com.mechalikh.pureedgesim.SimulationManager.SimLog;
import com.mechalikh.pureedgesim.SimulationManager.SimulationManager;
import com.mechalikh.pureedgesim.TasksGenerator.Task;
import com.usms.offloading.dqnoffloading.OffDqnAgent;
import com.usms.offloading.offdqn.*;
import examples.CustomEdgeOrchestrator;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.*;

public class OffDqnOrchestrator extends CustomEdgeOrchestrator {
    private static OffAgent dqn;
    private static OffDqnAgent offDqnAgent;
    OffState currentState;
    OffState nextState;
    private final double computedReward = 0.0;
    double edgeComputationRate = 0.0;

    List<DataCenter> datacenters = new ArrayList<>();

    public OffDqnOrchestrator(SimulationManager simulationManager) {
        super(simulationManager);
        datacenters = simulationManager.getServersManager().getDatacenterList();
        this.edgeComputationRate = getComputationRate()[1];
        dqn = new OffAgent(new LinkedHashMap<>());
        offDqnAgent = new OffDqnAgent(0.1, 0.1, 20);
        for (double q : dqn.getQTable().values()) {
            q = 0.0;
        }
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

    public int dqnTaskOffloading(String[] architecture, Task task) {
        currentState = OffState.buildState(task, edgeComputationRate);
        Map<OffStateAction, Integer> stateVmMap = new HashMap<>();
        List<Vm> vmEdgeServerList = new ArrayList<>();
        List<Vm> vmCloudList = new ArrayList<>();
        List<Vm> vmDevices = new ArrayList<>();
        for (int i = 0; i < orchestrationHistory.size(); i++) {
            if (((DataCenter) vmList.get(i).getHost().getDatacenter()).getType() == SimulationParameters.TYPES.EDGE_DATACENTER) {
                vmEdgeServerList.add(vmList.get(i));
            }
            if (((DataCenter) vmList.get(i).getHost().getDatacenter()).getType() == SimulationParameters.TYPES.EDGE_DEVICE) {
                vmDevices.add(vmList.get(i));
            }
            if (((DataCenter) vmList.get(i).getHost().getDatacenter()).getType() == SimulationParameters.TYPES.CLOUD) {
                vmCloudList.add(vmList.get(i));
            }
        }

        System.out.println("cloud");
        System.out.println(vmCloudList);

        OffAction action = dqn.chooseAction(currentState);


        switch (action.name()) {
            case "EXECUTE_LOCALLY": {
                vmId = executeLocally(task, vmDevices);
            }
            default: {
                vmId = offloadToEdge(task, vmEdgeServerList);
            }
        }

        nextState = OffState.buildState(task, edgeComputationRate);
        dqn.learn(currentState, action, computedReward, nextState);
        return RandomSelection.getRandom();
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

    private int executeLocally(Task task, List<Vm> vmDevices) {
        int selectedVmId = -1;
        double maxCpuUtilization = -1;
        for (Vm vm : vmDevices) {
            double cpuUtilization = vm.getCpuPercentUtilization();
            if (cpuUtilization > maxCpuUtilization) {
                maxCpuUtilization = cpuUtilization;
                selectedVmId = (int) vm.getId();
            }

        }
        int a = RandomSelection.getRandomDevice();
        System.out.println(a + " Local");
        return a;
    }

    private int offloadToEdge(Task task, List<Vm> vmEdgeServers) {
        int selectedVmId = -1;
        double minCpuUtilization = Double.MAX_VALUE;
        for (Vm vm : vmEdgeServers) {
            double cpuUtilization = vm.getCpuPercentUtilization();
            if (cpuUtilization < minCpuUtilization) {
                minCpuUtilization = cpuUtilization;
                selectedVmId = (int) vm.getId();
            }
        }

        int a = RandomSelection.getRandomEdge();
        System.out.println("Edge " + a);
        return a;
    }

}
