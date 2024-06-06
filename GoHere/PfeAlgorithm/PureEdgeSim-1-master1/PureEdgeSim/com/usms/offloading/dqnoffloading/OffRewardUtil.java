//package com.usms.offloading.dqnoffloading;
//
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//@Data
//@NoArgsConstructor
//public class OffRewardUtil {
//
//    double calculateReward() {
//        return 0.0;
//    }
//
//    public double calculateDeviceProcessingLatency(double dataSize, double computationResourcePerBit, double processingPower) {
//        // Task Complexity
//        double taskComplexity = dataSize * computationResourcePerBit;
//        // Processing Latency
//        double processingLatency = taskComplexity / processingPower;
//        return processingLatency;
//    }
//
//    public double calculateCompletionTime(double timeSlotDuration, double waitingDelay, double dataSize, double computationResourcePerBit, double processingPower, double maxToleratedDelay) {
//        // Calculate processing latency using the provided function
//        double processingLatency = calculateDeviceProcessingLatency(dataSize, computationResourcePerBit, processingPower);
//        // Calculate completion time based on the formula
//        double completionTime = Math.min(timeSlotDuration + waitingDelay + processingLatency, timeSlotDuration + maxToleratedDelay);
//        return completionTime;
//    }
//
//    public double calculateWaitingDelay(double t, double tlComp, double timeSlotDuration, double dataSize, double computationResourcePerBit, double processingPower, double maxToleratedDelay) {
//        // Calculate the completion time of the previous task
//        double previousTaskCompletionTime = calculateCompletionTime(t - 1, tlComp, dataSize, computationResourcePerBit, processingPower, maxToleratedDelay);
//
//        // Calculate waiting delay using the provided equation
//        double waitingDelay = Math.max(previousTaskCompletionTime, t + 1) - t;
//
//        // Ensure waiting delay does not exceed the maximum tolerated delay
//        waitingDelay = Math.min(waitingDelay, maxToleratedDelay);
//
//        return waitingDelay;
//    }
//
//    public double calculateDeviceEnergyConsumption(double dataSize, double computationResourcePerBit, double processingPower, double timeSlotDuration, double t, double tlComp, double waitingPower, double maxToleratedDelay) {
//        // Calculate processing latency using the provided function
//        double processingLatency = calculateDeviceProcessingLatency(dataSize, computationResourcePerBit, processingPower);
//
//        // Calculate waiting delay using the provided function
//        double waitingDelay = calculateWaitingDelay(t, tlComp, timeSlotDuration, dataSize, computationResourcePerBit, processingPower, maxToleratedDelay);
//
//        // Calculate device energy consumption
//        double deviceEnergyConsumption = (processingPower * processingLatency) + (waitingDelay * waitingPower);
//
//        return deviceEnergyConsumption;
//    }
//
//
//    public double calculateDeviceTotalTimeDelay(double t, double tlComp, double timeSlotDuration, double dataSize, double computationResourcePerBit, double processingPower, double maxToleratedDelay) {
//        // Calculate processing latency
//        double processingLatency = calculateDeviceProcessingLatency(dataSize, computationResourcePerBit, processingPower);
//
//        // Calculate waiting delay
//        double waitingDelay = calculateWaitingDelay(t, tlComp, timeSlotDuration, dataSize, computationResourcePerBit, processingPower, maxToleratedDelay);
//
//        // Calculate the total time delay
//        double deviceTotalTimeDelay = Math.min(waitingDelay + processingLatency, maxToleratedDelay);
//
//        return deviceTotalTimeDelay;
//    }
//
//    double edgeServerTotalTimeDelay() {
//        return 0.0;
//    }
//
//    double deviceEnergyConsumption() {
//        return 0.0;
//    }
//
//    double edgeServerEnergyConsumption() {
//        return 0.0;
//    }
//}
