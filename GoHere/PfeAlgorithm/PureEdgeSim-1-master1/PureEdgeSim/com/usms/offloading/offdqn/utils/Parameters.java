package com.usms.offloading.offdqn.utils;

public class Parameters {
    public static final double sigma2 = Math.pow(10, -11);//in mWatt
    public static final double powerTransmission = 100; //in mWatt
    public static final double BwDeviceEdge = 5; //data transmission rate between device and edge server in Mhz
    public static final double BwEdgeCloud = 10; //Mhz

    //public static final double pathLoss = 75.85; //in dB
    public static final double edgeChannelGain = 24.5; //in dBi
    public static final double userChannelGain = 2.15; //in dBi
    public static final double interference = Math.pow(10, -9);//-90; //in dBm

    public static final double localComputationRate = 500;// 0.5Ghz == 500MIPS
    public static final double egdeComputationRate = 10000; //10Ghz == 10000MIPS
    public static final double remoteComputationRate = 50; //50Ghz == 50000MIPS

    public static final double switchedCapacitance = Math.pow(10, -25);
    public static final double unitStorageCost = 0.5;
}
