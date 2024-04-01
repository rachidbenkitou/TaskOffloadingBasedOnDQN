package com.usms.offloading.dqn.util;


import com.usms.offloading.dqn.learning.MyActionSpace;
import com.usms.offloading.dqn.learning.MyState;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;

import static com.usms.offloading.dqn.util.Parameters.*;

@Value
@Setter
@Getter
public class RewardUtil {

    private MyState s;
    private MyActionSpace a;

    public double pathLoss(double d){
        //double pl = 80+ 35*Math.log10(d/1000);
        double pl=3.73;
        return pl;

    }

    public  double calculateReward(double w1, double w2, double w3, long containerSize, double dij, int nbReq, int R, long cav, double memoryAccessTime, long c) {

        return -1 * ( w1*calculateLatency(containerSize, dij) + w2*calculateEnergy(containerSize, dij) + w3*calculateCachLoss(nbReq, R, cav, memoryAccessTime, c) );
    }

    public  double calculateLatency(long containerSize, double dij) {
        double v = a.getActionCode()[0] * getLatencyLocal(containerSize, dij) +
                a.getActionCode()[1] * getLatencyEdge(containerSize, dij) +
                a.getActionCode()[2] * getLatencyRemote(containerSize, dij);
        System.out.println("Latency : " + v);

        return v;
    }

    public  double calculateEnergy(long containerSize,
                                         double dij) {

        double v = a.getActionCode()[0] * getEnergyLocal(containerSize, dij)
                + a.getActionCode()[1] * getEnergyEdge(containerSize, dij) +
                a.getActionCode()[2] * getEnergyRemote(dij);
        System.out.println("Energy : " + v);
        return v;
    }

    public  double benefit(int nbReq, int R, long cav, double memoryAccessTime, long c){
        double v = (nbReq / (s.getMaxLatency() * R)) * Math.log10((cav * s.getEdgeComputationRate() * memoryAccessTime) / c);
        System.out.println("Benefit : " + v);
        return v;
    }
    public  double calculateCachLoss(int nbReq, int R, long cav, double memoryAccessTime, long c) {
        System.out.println("Cache Loss To " + R + " nbReq "+ nbReq);
        double v = (double)(a.getActionCode()[1] * s.getRequestSize() * unitStorageCost) - (cav/c);// - (nbReq / (s.getMaxLatency() * R)) * Math.log((cav * s.getEdgeComputationRate() * memoryAccessTime) / c);
        System.out.println("Cache Loss : " + v);
        return v;
    }

    public  double SPDevice(double dij) {
        return powerTransmission * getUserChannelGain(dij) * Math.pow(dij, -pathLoss(dij));
        //return powerTransmission * (140.7+(36.7*Math.log10(dij*1000)))* Math.pow(dij, -pathLoss);
    }

    private double getUserChannelGain(double dij) {
        return 140.7 + 36.7 *Math.log10(dij/1000);
        //return 140.7+(36.7*Math.log10(dij*1000))
    }

    public  double dataRate(double bw, double dij) {
        double spdevice = SPDevice(dij);
        System.out.println("spdevice : " + spdevice);
        double v = bw * Math.log(1 + (spdevice / (interference + sigma2))/Math.log(2));
        System.out.println("dataRate : " + v);
        return v;
        //return 1.0;
    }

    public  double getLatencyLocal(long containerSize, double dij) {
        return ((s.getLength() / s.getLocalComputationRate()) + ((containerSize / dataRate(BwDeviceEdge, dij)) + (containerSize / dataRate(BwEdgeCloud, dij))) * (1 - a.getActionCode()[3]));

    }

    public  double getLatencyEdge(long containerSize, double dij) {
        return ((s.getRequestSize() / dataRate(BwDeviceEdge, dij)) + ((containerSize / dataRate(BwEdgeCloud, dij)) * (1 - a.getActionCode()[3])) + (s.getLength() / s.getEdgeComputationRate()));
    }

    public  double getLatencyRemote(long containerSize, double dij) {
        return ((s.getRequestSize() / dataRate(BwDeviceEdge, dij)) + ((s.getRequestSize() / dataRate(BwEdgeCloud, dij))) + (s.getLength() / s.getRemoteComputationRate()));
    }

    public  double getEnergyLocal(long containerSize, double dij) {
        return ((switchedCapacitance * s.getLength() * Math.pow(s.getLocalComputationRate(), 2)) +
                ((1 - a.getActionCode()[3]) * ((powerTransmission * containerSize / dataRate(BwDeviceEdge, dij)) + (powerTransmission * containerSize / dataRate(BwEdgeCloud, dij))))
        );
    }

    public  double getEnergyEdge(long containerSize, double dij) {

        return ((s.getRequestSize() * powerTransmission / dataRate(BwDeviceEdge, dij))
                + ((powerTransmission * containerSize / dataRate(BwEdgeCloud, dij)) * (1 - a.getActionCode()[3])) +
                (switchedCapacitance * s.getLength() * Math.pow(s.getEdgeComputationRate(), 2))
        );
    }

    public  double getEnergyRemote(double dij) {
        return ((powerTransmission * s.getRequestSize() / dataRate(BwDeviceEdge, dij)) +
                (powerTransmission * s.getRequestSize() / dataRate(BwEdgeCloud, dij)) +
                switchedCapacitance * s.getLength() * Math.pow(s.getRemoteComputationRate(), 2)
        );


    }
}
