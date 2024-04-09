package com.usms.offloading.dqnoffloading;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OffStateAction {
    private OffState state;
    private double action;
}
