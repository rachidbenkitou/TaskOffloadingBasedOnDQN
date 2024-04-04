package com.usms.offloading.dqnoffloading;

import com.mechalikh.pureedgesim.DataCentersManager.DataCenter;
import com.mechalikh.pureedgesim.ScenarioManager.SimulationParameters;
import com.mechalikh.pureedgesim.SimulationManager.SimLog;
import com.mechalikh.pureedgesim.SimulationManager.SimulationManager;
import com.mechalikh.pureedgesim.TasksGenerator.Task;
import com.mechalikh.pureedgesim.TasksOrchestration.Orchestrator;
import com.mechalikh.pureedgesim.TasksOrchestration.VmTaskMapItem;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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

    public OffDqnOrchestrator(SimulationManager simulationManager) {
        super(simulationManager);
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

    private OffState buildState(Task task, long defaultVmId) {
        double taskDataSize = task.getLength();
        double taskComputationRequirement = task.getContainerSize();
        double maximumToleranceDelay = task.getMaxLatency();
        DataCenter device = task.getEdgeDevice();
        int mdWaitingQueueState = calculateMdWaitingQueueState(device);
        double edgeServerLoad = calculateEdgeServerLoad(device.getId());
        DataCenter orchestrator = findDataCenterById(defaultVmId);
        double bandwidthInformation = getBandwidthInformation(device, orchestrator);
        return new OffState(taskDataSize, taskComputationRequirement, maximumToleranceDelay,
                mdWaitingQueueState, (int) edgeServerLoad, bandwidthInformation);
    }

    private int calculateMdWaitingQueueState(DataCenter device) {
        int tasksCount = 0;
        for (VmTaskMapItem mapItem : device.getVmTaskMap()) {
            Vm vm = mapItem.getVm();
            if (device.getVmList().contains(vm)) {
                tasksCount++;
            }
        }
        return tasksCount;
    }

    private double calculateEdgeServerLoad(long dataCenterId) {
        DataCenter edgeServer = findDataCenterById(dataCenterId);
        if (edgeServer == null) {
            return 0.0;
        }
        double totalMipsCapacity = edgeServer.getVmList().stream().mapToDouble(Vm::getMips).sum();
        double allocatedMipsForTasks = 0.0;
        for (VmTaskMapItem mapItem : edgeServer.getVmTaskMap()) {
            Vm vm = mapItem.getVm();
            if (edgeServer.getVmList().contains(vm)) {
                allocatedMipsForTasks += vm.getMips();
            }
        }
        double cpuUtilization = (totalMipsCapacity > 0) ? (allocatedMipsForTasks / totalMipsCapacity) : 0.0;
        return cpuUtilization * 100;
    }

    private DataCenter findDataCenterById(long id) {
        for (DataCenter dc : simulationManager.getServersManager().getDatacenterList()) {
            if (dc.getId() == id) {
                return dc;
            }
        }
        return null;
    }

    private double getBandwidthInformation(DataCenter device, DataCenter orchestrator) {
        double distance = device.getMobilityManager().distanceTo(orchestrator);
        double bandwidth;
        if (distance <= 100) {
            bandwidth = 100;
        } else if (distance <= 1000) {
            bandwidth = 50;
        } else {
            bandwidth = 10;
        }
        return bandwidth;
    }


    private int dqnTaskOffloadingTraining(String[] architecture, Task task) {
        //set the current state and next initial state
        int defaultVmId = pickRandom(vmList);
        if (task.getEdgeDevice().getVmList() != null && !task.getEdgeDevice().getVmList().isEmpty()) {
            defaultVmId = (int) task.getEdgeDevice().getVmList().get(0).getId();
        }
        return 0;
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
