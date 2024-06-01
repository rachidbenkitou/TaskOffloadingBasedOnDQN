package com.usms.offloading.offdqn;

import java.util.Random;

public class RandomSelection {

    // Déclaration des tableaux device et edge en tant qu'entiers
    private static int[] device = {
            16, 17, 18, 19
    };
    private static int[] edge = {
            20, 21, 22, 23, 24, 25, 26, 27, 28, 29,
            30, 31, 32, 33, 34, 35, 36, 37, 38, 39,
            40, 41, 42, 43, 44, 45, 46, 47, 48, 49,
            50, 51, 52, 53, 54, 55, 56, 57, 58, 59,
            60, 61, 62, 63, 64, 65, 66, 67, 68, 69,
            70, 71, 72, 73, 74, 75, 76, 77, 78, 79
    };

    private static int[] allDevices = {
//            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
            16,
            17, 18, 19, 20, 21,
            22, 23, 24, 25, 26, 27, 28, 29,
            30, 31, 32, 33, 34, 35, 36, 37, 38, 39,
            40, 41, 42, 43, 44, 45, 46, 47, 48, 49,
            50, 51, 52, 53, 54, 55, 56, 57, 58, 59,
            60, 61, 62, 63, 64, 65, 66, 67, 68, 69,
            70, 71, 72, 73, 74, 75, 76, 77, 78, 79,
    };

    // Fonction pour obtenir un élément aléatoire du tableau device

    public static int getRandom() {
        Random random = new Random();
        int index = random.nextInt(allDevices.length);
        return allDevices[index];
    }

    public static int getRandomDevice() {
        Random random = new Random();
        int index = random.nextInt(device.length);
        return device[index];
    }

    // Fonction pour obtenir un élément aléatoire du tableau edge
    public static int getRandomEdge() {
        Random random = new Random();
        int index = random.nextInt(edge.length);
        return edge[index];
    }

}