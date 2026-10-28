package com.xyp.gtneverreset.utils.machine;

public interface QuantumComputerCpuStatus {

    byte NORMAL_CPU = 0;
    byte VIRTUAL_CPU = 1;
    byte SPLIT_CPU = 2;

    byte gtnr$getCPUType();
}
