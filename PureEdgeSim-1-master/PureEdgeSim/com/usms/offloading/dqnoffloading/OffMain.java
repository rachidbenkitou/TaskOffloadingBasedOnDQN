package com.usms.offloading.dqnoffloading;

import com.mechalikh.pureedgesim.MainApplication;

public class OffMain extends MainApplication {
    private static String settingsPath = "PureEdgeSim/off_dqn_learning/settings/";

    private static String outputPath = "PureEdgeSim/off_dqn_learning/outputs/";

    public OffMain(int fromIteration, int step_) {
        super(fromIteration, step_);
    }

    public static void main(String[] args) {
        setCustomOutputFolder(outputPath);
        setCustomSettingsFolder(settingsPath);
        OffMain.launchSimulation();
    }
}
